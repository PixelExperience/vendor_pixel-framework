/*
 * Copyright (C) 2022 The PixelExperience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.systemui.reversecharging;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.nfc.IAppCallback;
import android.nfc.INfcAdapter;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IThermalEventListener;
import android.os.IThermalService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Temperature;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.systemui.BootCompleteCache;
import com.android.systemui.R;
import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.CallbackController;
import com.google.android.systemui.reversecharging.ReverseWirelessCharger;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

@SysUISingleton
public class ReverseChargingController extends BroadcastReceiver implements CallbackController<ReverseChargingChangeCallback> {
    private static final boolean DEBUG = Log.isLoggable("ReverseChargingControl", 3);
    private static final long DURATION_TO_ADVANCED_ACCESSORY_DEVICE_RECONNECTED_TIME_OUT;
    private static final long DURATION_TO_ADVANCED_PHONE_RECONNECTED_TIME_OUT;
    private static final long DURATION_TO_ADVANCED_PLUS_ACCESSORY_DEVICE_RECONNECTED_TIME_OUT;
    private static final long DURATION_TO_REVERSE_AC_TIME_OUT;
    private static final long DURATION_TO_REVERSE_RX_REMOVAL_TIME_OUT;
    private static final long DURATION_TO_REVERSE_TIME_OUT;
    private static final long DURATION_WAIT_NFC_SERVICE;

    static {
        TimeUnit timeUnit = TimeUnit.MINUTES;
        DURATION_TO_REVERSE_TIME_OUT = timeUnit.toMillis(1L);
        DURATION_TO_REVERSE_AC_TIME_OUT = timeUnit.toMillis(1L);
        TimeUnit timeUnit2 = TimeUnit.SECONDS;
        DURATION_TO_REVERSE_RX_REMOVAL_TIME_OUT = timeUnit2.toMillis(30L);
        DURATION_TO_ADVANCED_ACCESSORY_DEVICE_RECONNECTED_TIME_OUT = timeUnit2.toMillis(120L);
        DURATION_TO_ADVANCED_PHONE_RECONNECTED_TIME_OUT = timeUnit2.toMillis(120L);
        DURATION_TO_ADVANCED_PLUS_ACCESSORY_DEVICE_RECONNECTED_TIME_OUT = timeUnit2.toMillis(120L);
        DURATION_WAIT_NFC_SERVICE = timeUnit2.toMillis(10L);
    }

    final boolean mDoesNfcConflictWithUsbAudio;
    final int[] mNfcUsbProductIds;
    final int[] mNfcUsbVendorIds;
    private final AlarmManager mAlarmManager;
    private final Executor mBgExecutor;
    private final BootCompleteCache mBootCompleteCache;
    private final BroadcastDispatcher mBroadcastDispatcher;
    private final Context mContext;
    private final boolean mDoesNfcConflictWithWlc;
    private final Executor mMainExecutor;
    private final Optional<ReverseWirelessCharger> mRtxChargerManagerOptional;
    private final IThermalService mThermalService;
    private final Optional<UsbManager> mUsbManagerOptional;
    private final IBinder mNfcInterfaceToken = new Binder();
    private final ArrayList<ReverseChargingChangeCallback> mChangeCallbacks = new ArrayList<>();
    boolean mBootCompleted;
    int mLevel;
    boolean mRestoreUsbNfcPollingMode;
    boolean mReverse;
    IThermalEventListener mSkinThermalEventListener;
    int mCurrentRtxMode = 0;
    boolean mIsUsbPlugIn = false;
    private boolean mCacheIsReverseSupported;
    private boolean mIsReverseSupported;
    private String mName;
    private boolean mPluggedAc;
    private boolean mPowerSave;
    private boolean mRestoreWlcNfcPollingMode;
    private final AlarmManager.OnAlarmListener mCheckNfcConflictWithUsbAudioAlarmAction = new AlarmManager.OnAlarmListener() {
        @Override
        public void onAlarm() {
            if (mUsbManagerOptional.isPresent()) {
                for (UsbDevice usbDevice : mUsbManagerOptional.get().getDeviceList().values()) {
                    checkAndChangeNfcPollingAgainstUsbAudioDevice(false, usbDevice);
                }
            }
        }
    };
    private int mRtxLevel;
    private boolean mStartReconnected;
    private boolean mStopReverseAtAcUnplug;
    private boolean mUseRxRemovalTimeOut;
    private boolean mWirelessCharging;
    final BatteryController.BatteryStateChangeCallback mBatteryStateChangeCallback = new BatteryController.BatteryStateChangeCallback() {
        @Override
        public void onPowerSaveChanged(boolean z) {
            mPowerSave = z;
        }

        @Override
        public void onWirelessChargingChanged(boolean z) {
            mWirelessCharging = z;
        }
    };

    private final BootCompleteCache.BootCompleteListener mBootCompleteListener = new BootCompleteCache.BootCompleteListener() {
        @Override
        public void onBootComplete() {
            if (DEBUG) {
                Log.d("ReverseChargingControl", "onBootComplete(): ACTION_BOOT_COMPLETED");
            }
            mBootCompleted = true;
            setRtxTimer(2, DURATION_WAIT_NFC_SERVICE);
        }
    };
    private int mCurrentRtxReceiverType = 0;

    private final AlarmManager.OnAlarmListener mRtxFinishAlarmAction = new AlarmManager.OnAlarmListener() {
        @Override
        public void onAlarm() {
            onAlarmRtxFinish(5);
        }
    };
    private boolean mProvidingBattery = false;

    private final AlarmManager.OnAlarmListener mRtxFinishRxFullAlarmAction = new AlarmManager.OnAlarmListener() {
        @Override
        public void onAlarm() {
            onAlarmRtxFinish(R.styleable.Constraint_layout_goneMarginTop);
        }
    };

    @Inject
    public ReverseChargingController(Context context, BroadcastDispatcher broadcastDispatcher, Optional<ReverseWirelessCharger> optional, AlarmManager alarmManager, Optional<UsbManager> optional2, Executor executor, Executor executor2, BootCompleteCache bootCompleteCache, IThermalService iThermalService) {
        mContext = context;
        mBroadcastDispatcher = broadcastDispatcher;
        mRtxChargerManagerOptional = optional;
        mAlarmManager = alarmManager;
        mDoesNfcConflictWithWlc = context.getResources().getBoolean(R.bool.config_nfc_conflict_with_wlc);
        mUsbManagerOptional = optional2;
        mMainExecutor = executor;
        mBgExecutor = executor2;
        mBootCompleteCache = bootCompleteCache;
        mThermalService = iThermalService;
        int[] intArray = context.getResources().getIntArray(R.array.config_nfc_conflict_with_usb_audio_vendorid);
        mNfcUsbVendorIds = intArray;
        int[] intArray2 = context.getResources().getIntArray(R.array.config_nfc_conflict_with_usb_audio_productid);
        mNfcUsbProductIds = intArray2;
        if (intArray.length != intArray2.length) {
            throw new IllegalStateException("VendorIds and ProductIds must be the same length");
        }
        mDoesNfcConflictWithUsbAudio = context.getResources().getBoolean(R.bool.config_nfc_conflict_with_usb_audio);
    }

    private boolean shouldEnableAccessoryReconnect(int i) {
        return i == 16 || i == 90 || i == 114;
    }

    private final AlarmManager.OnAlarmListener mReconnectedTimeoutAlarmAction = new AlarmManager.OnAlarmListener() {
        @Override
        public void onAlarm() {
            if (DEBUG) {
                Log.w("ReverseChargingControl", "mReConnectedTimeoutAlarmAction() timeout");
            }
            mStartReconnected = false;
            onAlarmRtxFinish(6);
        }
    };

    private void checkAndChangeNfcPollingAgainstUsbAudioDevice(boolean z, UsbDevice usbDevice) {
        boolean z2 = false;
        for (int i = 0; i < mNfcUsbVendorIds.length; i++) {
            if (usbDevice.getVendorId() == mNfcUsbVendorIds[i] && usbDevice.getProductId() == mNfcUsbProductIds[i]) {
                mRestoreUsbNfcPollingMode = !z;
                if (!mRestoreWlcNfcPollingMode && z) {
                    z2 = true;
                }
                enableNfcPollingMode(z2);
                return;
            }
        }
    }

    private final AlarmManager.OnAlarmListener mAccessoryDeviceRemovedTimeoutAlarmAction = new AlarmManager.OnAlarmListener() {
        @Override
        public void onAlarm() {
            if (DEBUG) {
                Log.w("ReverseChargingControl", "mAccessoryDeviceRemovedTimeoutAlarmAction() timeout");
            }
            onAlarmRtxFinish(6);
        }
    };

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        mBroadcastDispatcher.registerReceiver(this, intentFilter);
    }

    public void init(BatteryController batteryController) {
        batteryController.addCallback(mBatteryStateChangeCallback);
        mCacheIsReverseSupported = false;
        resetReverseInfo();
        registerReceiver();
        mBootCompleteCache.addListener(mBootCompleteListener);
        if (mRtxChargerManagerOptional.isPresent()) {
            setRtxMode(false);
            mRtxChargerManagerOptional.get().addIsDockPresentChangeListener(new ReverseWirelessCharger.IsDockPresentChangeListener() {
                @Override
                public void onDockPresentChanged(Bundle bundle) {
                    onDockPresentChanged(bundle);
                }
            });
            mRtxChargerManagerOptional.get().addReverseChargingInformationChangeListener(new ReverseWirelessCharger.ReverseChargingInformationChangeListener() {
                @Override
                public void onReverseInformationChanged(Bundle bundle) {
                    onReverseInformationChanged(bundle);
                }
            });
            mRtxChargerManagerOptional.get().addReverseChargingChangeListener(new ReverseWirelessCharger.ReverseChargingChangeListener() {
                @Override
                public void onReverseStatusChanged(Bundle bundle) {
                    onReverseStateChanged(bundle);
                }
            });
            try {
                if (mSkinThermalEventListener == null) {
                    mSkinThermalEventListener = new SkinThermalEventListener();
                }
                mThermalService.registerThermalEventListenerWithType(mSkinThermalEventListener, 3);
            } catch (RemoteException e) {
                Log.e("ReverseChargingControl", "Could not register thermal event listener, exception: " + e);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        handleIntentForReverseCharging(intent);
    }

    public void handleIntentForReverseCharging(Intent intent) {
        UsbDevice usbDevice;
        Object objArr;
        Object objArr2;
        if (!isReverseSupported()) {
            return;
        }
        String action = intent.getAction();
        boolean z = true;
        if (action.equals("android.intent.action.BATTERY_CHANGED")) {
            boolean z2 = mPluggedAc;
            mLevel = (int) ((intent.getIntExtra("level", 0) * 100.0f) / intent.getIntExtra("scale", 100));
            int intExtra = intent.getIntExtra("plugged", 0);
            mPluggedAc = intExtra == 1;
            Log.i("ReverseChargingControl", "handleIntentForReverseCharging(): rtx=" + (mReverse ? 1 : 0) + " wlc=" + (mWirelessCharging ? 1 : 0) + " plgac=" + (z2 ? 1 : 0) + " ac=" + (mPluggedAc ? 1 : 0) + " acrtx=" + (mStopReverseAtAcUnplug ? 1 : 0) + " extra=" + intExtra + " this=" + this);
            boolean z3 = mReverse;
            if (z3 && mWirelessCharging) {
                if (DEBUG) {
                    Log.d("ReverseChargingControl", "handleIntentForReverseCharging(): wireless charging, stop");
                }
                setReverseStateInternal(false, R.styleable.Constraint_layout_goneMarginStart);
            } else if (z3 && z2 && !mPluggedAc && mStopReverseAtAcUnplug) {
                if (DEBUG) {
                    Log.d("ReverseChargingControl", "handleIntentForReverseCharging(): wired charging, stop");
                }
                mStopReverseAtAcUnplug = false;
                setReverseStateInternal(false, 106);
            } else if (!z3 && !z2 && mPluggedAc) {
                if (!mBootCompleted) {
                    Log.i("ReverseChargingControl", "skip auto turn on");
                    return;
                }
                if (DEBUG) {
                    Log.d("ReverseChargingControl", "handleIntentForReverseCharging(): wired charging, start");
                }
                mStopReverseAtAcUnplug = true;
                setReverseStateInternal(true, 3);
            } else if (!z3 || !isLowBattery()) {
            } else {
                if (DEBUG) {
                    Log.d("ReverseChargingControl", "handleIntentForReverseCharging(): lower then battery threshold, stop");
                }
                setReverseStateInternal(false, 4);
            }
        } else if (action.equals("android.os.action.POWER_SAVE_MODE_CHANGED")) {
            if (!mReverse || !mPowerSave) {
                return;
            }
            Log.i("ReverseChargingControl", "handleIntentForReverseCharging(): power save, stop");
            setReverseStateInternal(false, R.styleable.Constraint_pathMotionArc);
        } else if (TextUtils.equals(action, "android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
            UsbDevice usbDevice2 = (UsbDevice) intent.getParcelableExtra("device");
            if (usbDevice2 == null) {
                Log.w("ReverseChargingControl", "handleIntentForReverseCharging() UsbDevice is null!");
                mIsUsbPlugIn = false;
                return;
            }
            if (mDoesNfcConflictWithUsbAudio) {
                checkAndChangeNfcPollingAgainstUsbAudioDevice(false, usbDevice2);
            }
            int i = 0;
            while (true) {
                if (i >= usbDevice2.getInterfaceCount()) {
                    objArr = null;
                    break;
                } else if (usbDevice2.getInterface(i).getInterfaceClass() == 1) {
                    objArr = 1;
                    break;
                } else {
                    i++;
                }
            }
            int i2 = 0;
            while (true) {
                if (i2 >= usbDevice2.getConfigurationCount()) {
                    objArr2 = null;
                    break;
                } else if (usbDevice2.getConfiguration(i2).getMaxPower() < 100) {
                    objArr2 = 1;
                    break;
                } else {
                    i2++;
                }
            }
            if (objArr != null && objArr2 != null) {
                z = false;
            }
            mIsUsbPlugIn = z;
            if (!mReverse || !z) {
                return;
            }
            setReverseStateInternal(false, R.styleable.Constraint_transitionEasing);
            Log.d("ReverseChargingControl", "handleIntentForReverseCharging(): stop reverse charging because USB-C plugin!");
        } else if (!TextUtils.equals(action, "android.hardware.usb.action.USB_DEVICE_DETACHED")) {
        } else {
            if (mDoesNfcConflictWithUsbAudio && (usbDevice = (UsbDevice) intent.getParcelableExtra("device")) != null) {
                checkAndChangeNfcPollingAgainstUsbAudioDevice(true, usbDevice);
            }
            mIsUsbPlugIn = false;
        }
    }

    private boolean isLowBattery() {
        int i = Settings.Global.getInt(mContext.getContentResolver(), "advanced_battery_usage_amount", 2) * 5;
        if (mLevel <= i) {
            Log.w("ReverseChargingControl", "The battery is lower than threshold turn off reverse charging ! level : " + mLevel + ", threshold : " + i);
            return true;
        }
        return false;
    }

    public boolean isReverseSupported() {
        if (mCacheIsReverseSupported) {
            return mIsReverseSupported;
        }
        if (mRtxChargerManagerOptional.isPresent()) {
            boolean isRtxSupported = mRtxChargerManagerOptional.get().isRtxSupported();
            mIsReverseSupported = isRtxSupported;
            mCacheIsReverseSupported = true;
            return isRtxSupported;
        } else if (!DEBUG) {
            return false;
        } else {
            Log.d("ReverseChargingControl", "isReverseSupported(): mRtxChargerManagerOptional is not present!");
            return false;
        }
    }

    public boolean isReverseOn() {
        return mReverse;
    }

    public void setReverseState(boolean z) {
        if (!isReverseSupported()) {
            return;
        }
        if (DEBUG) {
            Log.d("ReverseChargingControl", "setReverseState(): rtx=" + (z ? 1 : 0));
        }
        mStopReverseAtAcUnplug = false;
        setReverseStateInternal(z, 2);
    }

    private void setReverseStateInternal(boolean z, int i) {
        if (!isReverseSupported()) {
            return;
        }
        Log.i("ReverseChargingControl", "setReverseStateInternal(): rtx=" + (z ? 1 : 0) + ",reason=" + i);
        if (z && !isReverseOn()) {
            if (mPowerSave) {
                return;
            } else if (isLowBattery()) {
                return;
            } else if (mIsUsbPlugIn) {
                return;
            }
        }
        if (z == isReverseOn()) {
            return;
        }
        if (z && mDoesNfcConflictWithWlc && !mRestoreWlcNfcPollingMode) {
            enableNfcPollingMode(false);
            mRestoreWlcNfcPollingMode = true;
        }
        mReverse = z;
        if (z) {
            setRtxTimer(0, DURATION_TO_REVERSE_TIME_OUT);
        }
        setRtxMode(z);
    }

    private void enableNfcPollingMode(boolean z) {
        int i = z ? 0 : 4096;
        if (DEBUG) {
            Log.d("ReverseChargingControl", "Change NFC reader mode to flags: " + i);
        }
        try {
            INfcAdapter.Stub.asInterface(ServiceManager.getService("nfc")).setReaderMode(mNfcInterfaceToken, (IAppCallback) null, i, (Bundle) null);
        } catch (Exception e) {
            Log.e("ReverseChargingControl", "Could not change NFC reader mode, exception: " + e);
        }
    }

    private long getRtxTimeOut(int i) {
        long j;
        if (mStartReconnected) {
            j = getAccessoryReconnectDuration(i);
        } else if (mStopReverseAtAcUnplug) {
            j = DURATION_TO_REVERSE_AC_TIME_OUT;
        } else if (mUseRxRemovalTimeOut) {
            j = DURATION_TO_REVERSE_RX_REMOVAL_TIME_OUT;
        } else {
            j = DURATION_TO_REVERSE_TIME_OUT;
        }
        String str = SystemProperties.get(mStopReverseAtAcUnplug ? "rtx.ac.timeout" : "rtx.timeout");
        if (!TextUtils.isEmpty(str)) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                Log.w("ReverseChargingControl", "getRtxTimeOut(): invalid timeout, " + e);
                return j;
            }
        }
        return j;
    }

    private long getAccessoryReconnectDuration(int i) {
        if (i == 16) {
            return DURATION_TO_ADVANCED_ACCESSORY_DEVICE_RECONNECTED_TIME_OUT;
        }
        if (i == 114) {
            return DURATION_TO_ADVANCED_PHONE_RECONNECTED_TIME_OUT;
        }
        return DURATION_TO_ADVANCED_PLUS_ACCESSORY_DEVICE_RECONNECTED_TIME_OUT;
    }

    private void onDockPresentChanged(final Bundle bundle) {
        if (DEBUG) {
            Log.d("ReverseChargingControl", "onDockPresentChanged(): rtx =" + (mReverse ? 1 : 0) + " type=" + ((int) bundle.getByte("key_dock_present_type")) + " bundle=" + bundle + " this=" + this);
        }
        if (bundle.getByte("key_dock_present_type") != 4) {
            return;
        }
        mBgExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean z = DEBUG;
                if (z) {
                    Log.d("ReverseChargingControl", "requestReverseInformation()");
                }
                if (mRtxChargerManagerOptional.isPresent()) {
                    mRtxChargerManagerOptional.get().getRtxInformation();
                } else if (!z) {
                } else {
                    Log.d("ReverseChargingControl", "requestReverseInformation(): mRtxChargerManagerOptional is not present!");
                }
            }
        });
        mMainExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (DEBUG) {
                    Log.d("ReverseChargingControl", "onDockPresentChangedOnMainThread(): rtx =" + (mReverse ? 1 : 0) + " type=" + ((int) bundle.getByte("key_dock_present_type")) + " bundle=" + bundle + " this=" + this);
                }
                mName = bundle.getByte("key_dock_present_type") == 4 ? mContext.getString(R.string.reverse_charging_device_name_text) : null;
            }
        });
    }

    private void onReverseInformationChanged(final Bundle bundle) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onReverseInformationChanged(): rtx=");
            int i = 1;
            if (bundle.getInt("key_rtx_mode") != 1) {
                i = 0;
            }
            sb.append(i);
            sb.append(" wlc=");
            sb.append(mWirelessCharging ? 1 : 0);
            sb.append(" mName=");
            sb.append(mName);
            sb.append(" bundle=");
            sb.append(bundle);
            sb.append(" this=");
            sb.append(this);
            Log.d("ReverseChargingControl", sb.toString());
        }
        if (bundle.getInt("key_rtx_level") <= 0) {
            return;
        }
        mMainExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean z = false;
                if (DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onReverseInformationChangedOnMainThread(): rtx=");
                    sb.append(bundle.getInt("key_rtx_mode") == 1 ? 1 : 0);
                    sb.append(" wlc=");
                    sb.append(mWirelessCharging ? 1 : 0);
                    sb.append(" mName=");
                    sb.append(mName);
                    sb.append(" bundle=");
                    sb.append(bundle);
                    sb.append(" this=");
                    sb.append(this);
                    Log.d("ReverseChargingControl", sb.toString());
                }
                if (!mWirelessCharging || mName == null) {
                    return;
                }
                if (bundle.getInt("key_rtx_mode") == 1) {
                    z = true;
                }
                mReverse = z;
                mRtxLevel = bundle.getInt("key_rtx_level");
                fireReverseChanged();
            }
        });
    }

    void onReverseStateChanged(final Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        sb.append("onReverseStateChanged(): rtx=");
        int i = 1;
        if (bundle.getInt("key_rtx_mode") != 1) {
            i = 0;
        }
        sb.append(i);
        sb.append(" bundle=");
        sb.append(bundle);
        sb.append(" this=");
        sb.append(this);
        Log.i("ReverseChargingControl", sb.toString());
        mMainExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean z = DEBUG;
                int i = 0;
                if (z) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onReverseStateChangedOnMainThread(): rtx=");
                    sb.append(bundle.getInt("key_rtx_mode") == 1 ? 1 : 0);
                    sb.append(" bundle=");
                    sb.append(bundle);
                    sb.append(" this=");
                    sb.append(this);
                    Log.d("ReverseChargingControl", sb.toString());
                }
                int i2 = bundle.getInt("key_rtx_mode");
                int i3 = bundle.getInt("key_reason_type");
                boolean z2 = bundle.getBoolean("key_rtx_connection");
                int i4 = bundle.getInt("key_accessory_type");
                int i5 = bundle.getInt("key_rtx_level");
                if (!mReverse && mWirelessCharging && i2 == 0 && i5 > 0) {
                    mRtxLevel = i5;
                    if (TextUtils.isEmpty(mName)) {
                        mName = mContext.getString(R.string.reverse_charging_device_name_text);
                    }
                    fireReverseChanged();
                } else if (!isReverseSupported()) {
                    resetReverseInfo();
                    fireReverseChanged();
                } else {
                    if (mCurrentRtxMode != 1 && i2 == 1 && !mReverse && mDoesNfcConflictWithWlc && !mRestoreWlcNfcPollingMode) {
                        enableNfcPollingMode(false);
                        mRestoreWlcNfcPollingMode = true;
                    }
                    mCurrentRtxMode = i2;
                    resetReverseInfo();
                    if (i2 == 1) {
                        playSoundIfNecessary(z2, i4);
                        mProvidingBattery = z2;
                        mReverse = true;
                        if (!z2) {
                            if (z) {
                                Log.d("ReverseChargingControl", "receiver is not available");
                            }
                            mRtxLevel = -1;
                            mCurrentRtxReceiverType = 0;
                        } else {
                            mStopReverseAtAcUnplug = false;
                            mRtxLevel = i5;
                            mUseRxRemovalTimeOut = true;
                            if (mCurrentRtxReceiverType != i4) {
                                if (z) {
                                    Log.d("ReverseChargingControl", "receiver type updated: " + mCurrentRtxReceiverType + " " + i4);
                                }
                                mCurrentRtxReceiverType = i4;
                            }
                        }
                    } else {
                        mStopReverseAtAcUnplug = false;
                        mProvidingBattery = false;
                        mUseRxRemovalTimeOut = false;
                        mStartReconnected = false;
                        if (mDoesNfcConflictWithWlc && mRestoreWlcNfcPollingMode) {
                            mRestoreWlcNfcPollingMode = false;
                            enableNfcPollingMode(!mRestoreUsbNfcPollingMode);
                        }
                    }
                    fireReverseChanged();
                    cancelRtxTimer(0);
                    cancelRtxTimer(1);
                    cancelRtxTimer(4);
                    if (!mStartReconnected) {
                        cancelRtxTimer(3);
                    }
                    boolean z3 = mReverse;
                    if (z3 && mRtxLevel == -1) {
                        long rtxTimeOut = getRtxTimeOut(i4);
                        if (z) {
                            Log.d("ReverseChargingControl", "onReverseStateChangedOnMainThread(): time out, setRtxTimer, duration=" + rtxTimeOut);
                        }
                        if (mStartReconnected) {
                            i = 3;
                        } else if (mUseRxRemovalTimeOut && !mStopReverseAtAcUnplug) {
                            i = 4;
                        }
                        setRtxTimer(i, rtxTimeOut);
                    } else if (!z3 || mRtxLevel < 100) {
                    } else {
                        if (z) {
                            Log.d("ReverseChargingControl", "onReverseStateChangedOnMainThread(): rtx=" + (mReverse ? 1 : 0) + ", Rx fully charged, setRtxTimer, REVERSE_FINISH_RX_FULL");
                        }
                        setRtxTimer(1, 0L);
                    }
                }
            }
        });
    }

    private void setRtxMode(final boolean z) {
        if (mRtxChargerManagerOptional.isPresent()) {
            mBgExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    Log.i("ReverseChargingControl", "setRtxMode(): rtx=" + (z ? 1 : 0));
                    mRtxChargerManagerOptional.get().setRtxMode(z);
                }
            });
        } else {
            Log.i("ReverseChargingControl", "setRtxMode(): rtx not available");
        }
    }

    private void resetReverseInfo() {
        mReverse = false;
        mRtxLevel = -1;
        mName = null;
    }

    private void fireReverseChanged() {
        synchronized (mChangeCallbacks) {
            ArrayList arrayList = new ArrayList(mChangeCallbacks);
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                ((ReverseChargingChangeCallback) arrayList.get(i)).onReverseChargingChanged(mReverse, mRtxLevel, mName);
            }
        }
    }

    private void setRtxTimer(int i, long j) {
        if (i == 0) {
            mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + j, "ReverseChargingControl", mRtxFinishAlarmAction, null);
        } else if (i == 1) {
            mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + j, "ReverseChargingControl", mRtxFinishRxFullAlarmAction, null);
        } else if (i == 2) {
            mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + j, "ReverseChargingControl", mCheckNfcConflictWithUsbAudioAlarmAction, null);
        } else if (i == 3) {
            mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + j, "ReverseChargingControl", mReconnectedTimeoutAlarmAction, null);
        } else if (i != 4) {
        } else {
            mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + j, "ReverseChargingControl", mAccessoryDeviceRemovedTimeoutAlarmAction, null);
        }
    }

    private void onAlarmRtxFinish(int i) {
        Log.i("ReverseChargingControl", "onAlarmRtxFinish(): rtx=0, reason: " + i);
        setReverseStateInternal(false, i);
    }

    private void cancelRtxTimer(int i) {
        if (i == 0) {
            mAlarmManager.cancel(mRtxFinishAlarmAction);
        } else if (i == 1) {
            mAlarmManager.cancel(mRtxFinishRxFullAlarmAction);
        } else if (i == 3) {
            mAlarmManager.cancel(mReconnectedTimeoutAlarmAction);
        } else if (i != 4) {
        } else {
            mAlarmManager.cancel(mAccessoryDeviceRemovedTimeoutAlarmAction);
        }
    }

    private void playSoundIfNecessary(boolean z, int i) {
        boolean z2 = mProvidingBattery;
        String str = null;
        if (!z2 && z) {
            if (DEBUG) {
                Log.d("ReverseChargingControl", "playSoundIfNecessary() play start charging sound: " + z + ", accType : " + i + ", mStartReconnected : " + mStartReconnected);
            }
            if (!mStartReconnected || !shouldEnableAccessoryReconnect(i)) {
                str = mContext.getString(R.string.reverse_charging_started_sound);
            }
            mStartReconnected = false;
        } else if (z2 && !z) {
            boolean z3 = DEBUG;
            if (z3) {
                Log.d("ReverseChargingControl", "playSoundIfNecessary() play end charging sound: " + z + ", accType : " + i + ", mStartReConnected : " + mStartReconnected);
            }
            if (!mStartReconnected && shouldEnableAccessoryReconnect(i)) {
                mStartReconnected = true;
                if (z3) {
                    Log.w("ReverseChargingControl", "playSoundIfNecessary() start reconnected");
                }
            }
        }
        if (!TextUtils.isEmpty(str)) {
            playSound(RingtoneManager.getRingtone(mContext, new Uri.Builder().scheme("file").appendPath(str).build()));
        }
    }

    void playSound(Ringtone ringtone) {
        if (ringtone != null) {
            ringtone.setStreamType(1);
            ringtone.play();
        }
    }

    @Override
    public void addCallback(ReverseChargingChangeCallback reverseChargingChangeCallback) {
        synchronized (mChangeCallbacks) {
            mChangeCallbacks.add(reverseChargingChangeCallback);
        }
        reverseChargingChangeCallback.onReverseChargingChanged(mReverse, mRtxLevel, mName);
    }

    @Override
    public void removeCallback(ReverseChargingChangeCallback reverseChargingChangeCallback) {
        synchronized (mChangeCallbacks) {
            mChangeCallbacks.remove(reverseChargingChangeCallback);
        }
    }

    final class SkinThermalEventListener extends IThermalEventListener.Stub {
        SkinThermalEventListener() {
        }

        public void notifyThrottling(Temperature temperature) {
            int status = temperature.getStatus();
            Log.i("ReverseChargingControl", "notifyThrottling(): thermal status=" + status);
            ReverseChargingController reverseChargingController = ReverseChargingController.this;
            if (!reverseChargingController.mReverse || status < 5) {
                return;
            }
            reverseChargingController.setReverseStateInternal(false, 3);
        }
    }
}
