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

package com.google.android.systemui.dreamliner;

import androidx.annotation.NonNull;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.service.dreams.IDreamManager;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Dependency;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.dock.DockManager;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.interruption.NotificationInterruptStateProvider;
import com.android.systemui.statusbar.notification.interruption.NotificationInterruptSuppressor;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.util.concurrency.DelayableExecutor;
import com.google.android.systemui.dreamliner.WirelessCharger;
import com.google.android.systemui.elmyra.gates.KeyguardVisibility;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DockObserver extends BroadcastReceiver implements DockManager {
    @VisibleForTesting
    static final String ACTION_ALIGN_STATE_CHANGE = "com.google.android.systemui.dreamliner.ALIGNMENT_CHANGE";
    @VisibleForTesting
    static final String ACTION_CHALLENGE = "com.google.android.systemui.dreamliner.ACTION_CHALLENGE";
    @VisibleForTesting
    static final String ACTION_DOCK_UI_ACTIVE = "com.google.android.systemui.dreamliner.ACTION_DOCK_UI_ACTIVE";
    @VisibleForTesting
    static final String ACTION_DOCK_UI_IDLE = "com.google.android.systemui.dreamliner.ACTION_DOCK_UI_IDLE";
    @VisibleForTesting
    static final String ACTION_GET_DOCK_INFO = "com.google.android.systemui.dreamliner.ACTION_GET_DOCK_INFO";
    @VisibleForTesting
    static final String ACTION_KEY_EXCHANGE = "com.google.android.systemui.dreamliner.ACTION_KEY_EXCHANGE";
    @VisibleForTesting
    static final String ACTION_REBIND_DOCK_SERVICE = "com.google.android.systemui.dreamliner.ACTION_REBIND_DOCK_SERVICE";
    @VisibleForTesting
    static final String ACTION_START_DREAMLINER_CONTROL_SERVICE = "com.google.android.apps.dreamliner.START";
    @VisibleForTesting
    static final String COMPONENTNAME_DREAMLINER_CONTROL_SERVICE = "com.google.android.apps.dreamliner/.DreamlinerControlService";
    @VisibleForTesting
    static final String EXTRA_ALIGN_STATE = "align_state";
    @VisibleForTesting
    static final String EXTRA_CHALLENGE_DATA = "challenge_data";
    @VisibleForTesting
    static final String EXTRA_CHALLENGE_DOCK_ID = "challenge_dock_id";
    @VisibleForTesting
    static final String EXTRA_PUBLIC_KEY = "public_key";
    @VisibleForTesting
    static final String KEY_SHOWING = "showing";
    @VisibleForTesting
    static final String PERMISSION_WIRELESS_CHARGER_STATUS = "com.google.android.systemui.permission.WIRELESS_CHARGER_STATUS";
    @VisibleForTesting
    static final int RESULT_NOT_FOUND = 1;
    @VisibleForTesting
    static final int RESULT_OK = 0;
    private static final boolean DEBUG = Log.isLoggable("DLObserver", 3);
    @VisibleForTesting
    static volatile ExecutorService mSingleThreadExecutor;
    private static boolean sIsDockingUiShowing = DEBUG;
    @VisibleForTesting
    final DreamlinerBroadcastReceiver mDreamlinerReceiver = new DreamlinerBroadcastReceiver();
    private final List<DockManager.AlignmentStateListener> mAlignmentStateListeners;
    private final List<DockManager.DockEventListener> mClients;
    private final ConfigurationController mConfigurationController;
    private final Context mContext;
    private final DockAlignmentController mDockAlignmentController;
    private final DelayableExecutor mMainExecutor;
    private final StatusBarStateController mStatusBarStateController;
    private final WirelessCharger mWirelessCharger;
    @VisibleForTesting
    DockGestureController mDockGestureController;
    @VisibleForTesting
    DreamlinerServiceConn mDreamlinerServiceConn;
    DockIndicationController mIndicationController;
    @VisibleForTesting
    int mDockState = 0;
    @VisibleForTesting
    int mLastAlignState = -1;
    private ImageView mDreamlinerGear;
    private Runnable mPhotoAction;
    private FrameLayout mPhotoPreview;
    private int mFanLevel = -1;

    private final Handler mMainHandler;
    private final UserTracker mUserTracker;

    private final UserTracker.Callback mUserChangedCallback =
            new UserTracker.Callback() {
                @Override
                public void onUserChanged(int newUser, @NonNull Context userContext) {
                    stopDreamlinerService(mContext);
                    updateCurrentDockingStatus(mContext);
                }
            };

    public DockObserver(final Context context, WirelessCharger wirelessCharger, StatusBarStateController statusBarStateController,
        NotificationInterruptStateProvider notificationInterruptStateProvider, ConfigurationController configurationController,
        DelayableExecutor delayableExecutor, @NonNull UserTracker userTracker, @Main Handler mainHandler) {
        NotificationInterruptSuppressor notificationInterruptSuppressor = new NotificationInterruptSuppressor() {
            @Override
            public String getName() {
                return "DLObserver";
            }

            @Override
            public boolean suppressInterruptions(NotificationEntry notificationEntry) {
                return DockObserver.isDockingUiShowing();
            }
        };
        mMainExecutor = delayableExecutor;
        mContext = context;
        mClients = new ArrayList();
        mAlignmentStateListeners = new ArrayList();
        mWirelessCharger = wirelessCharger;
        if (wirelessCharger == null) {
            Log.i("DLObserver", "wireless charger is null, check dock component.");
        }
        mStatusBarStateController = statusBarStateController;
        context.registerReceiver(this, getDockIntentFilter(), PERMISSION_WIRELESS_CHARGER_STATUS, null);
        mDockAlignmentController = new DockAlignmentController(wirelessCharger, this);
        notificationInterruptStateProvider.addSuppressor(notificationInterruptSuppressor);
        mConfigurationController = configurationController;
        refreshFanLevel(null);
        mUserTracker = userTracker;
        mMainHandler = mainHandler;
    }

    private static void runOnBackgroundThread(Runnable runnable) {
        if (mSingleThreadExecutor == null) {
            mSingleThreadExecutor = Executors.newSingleThreadExecutor();
        }
        mSingleThreadExecutor.execute(runnable);
    }

    public static boolean isDockingUiShowing() {
        return sIsDockingUiShowing;
    }

    public void registerDockAlignInfo() {
        mDockAlignmentController.registerAlignInfoListener();
    }

    public void setDreamlinerGear(ImageView imageView) {
        mDreamlinerGear = imageView;
    }

    public void setPhotoPreview(FrameLayout frameLayout) {
        mPhotoPreview = frameLayout;
    }

    public void setIndicationController(DockIndicationController dockIndicationController) {
        mIndicationController = dockIndicationController;
        mConfigurationController.addCallback(dockIndicationController);
    }

    @Override
    public void addListener(final DockManager.DockEventListener dockEventListener) {
        if (DEBUG) {
            Log.d("DLObserver", "add listener: " + dockEventListener);
        }
        if (!mClients.contains(dockEventListener)) {
            mClients.add(dockEventListener);
        }
        mMainExecutor.execute((Runnable) () -> dockEventListener.onEvent(mDockState));
    }

    @Override
    public void removeListener(DockManager.DockEventListener dockEventListener) {
        if (DEBUG) {
            Log.d("DLObserver", "remove listener: " + dockEventListener);
        }
        mClients.remove(dockEventListener);
    }

    @Override
    public boolean isDocked() {
        int i = mDockState;
        if (i == 1 || i == 2) {
            return true;
        }
        return DEBUG;
    }

    @Override
    public boolean isHidden() {
        if (mDockState == 2) {
            return true;
        }
        return DEBUG;
    }

    @Override
    public void addAlignmentStateListener(DockManager.AlignmentStateListener alignmentStateListener) {
        if (DEBUG) {
            Log.d("DLObserver", "add alignment listener: " + alignmentStateListener);
        }
        if (!mAlignmentStateListeners.contains(alignmentStateListener)) {
            mAlignmentStateListeners.add(alignmentStateListener);
        }
    }

    @Override
    public void removeAlignmentStateListener(DockManager.AlignmentStateListener alignmentStateListener) {
        if (DEBUG) {
            Log.d("DLObserver", "remove alignment listener: " + alignmentStateListener);
        }
        if (mAlignmentStateListeners.contains(alignmentStateListener)) {
            mAlignmentStateListeners.remove(alignmentStateListener);
        }
    }

    private void tryTurnScreenOff(Context context) {
        PowerManager powerManager = context.getSystemService(PowerManager.class);
        if (powerManager.isScreenOn()) {
            powerManager.goToSleep(SystemClock.uptimeMillis());
        }
    }

    private void onDockStateChanged(int i) {
        if (mDockState == i) {
            return;
        }
        if (DEBUG) {
            Log.d("DLObserver", "dock state changed from " + mDockState + " to " + i);
        }
        int i2 = mDockState;
        mDockState = i;
        for (int i3 = 0; i3 < mClients.size(); i3++) {
            mClients.get(i3).onEvent(mDockState);
        }
        if (mIndicationController != null) {
            mIndicationController.setDocking(isDocked());
        }
        if (i2 != 0 || i != 1) {
            return;
        }
        notifyDreamlinerAlignStateChanged(mLastAlignState);
        onFanLevelChange();
    }

    void onAlignStateChanged(int i) {
        if (DEBUG) {
            Log.d("DLObserver", "onAlignStateChanged alignState = " + i);
        }
        mLastAlignState = i;
        for (DockManager.AlignmentStateListener alignmentStateListener : mAlignmentStateListeners) {
            alignmentStateListener.onAlignmentStateChanged(i);
        }
        runPhotoAction();
        notifyDreamlinerAlignStateChanged(i);
    }

    private void notifyDreamlinerAlignStateChanged(int i) {
        if (isDocked()) {
            mContext.sendBroadcastAsUser(new Intent(ACTION_ALIGN_STATE_CHANGE).putExtra(EXTRA_ALIGN_STATE, i).addFlags(1073741824), UserHandle.CURRENT);
        }
    }

    private void refreshFanLevel(final Runnable runnable) {
        Log.d("DLObserver", "command=2");
        runOnBackgroundThread(() -> {
            if (mWirelessCharger == null) {
                Log.i("DLObserver", "hint is UNKNOWN for null wireless charger HAL");
                mFanLevel = -1;
            } else {
                long currentTimeMillis = System.currentTimeMillis();
                mFanLevel = mWirelessCharger.getFanLevel();
                if (DEBUG) {
                    Log.d("DLObserver", "command=2, l=" + mFanLevel + ", spending time=" + (System.currentTimeMillis() - currentTimeMillis));
                }
            }
            if (runnable != null) {
                runnable.run();
            }
        });
    }

    void onFanLevelChange() {
        refreshFanLevel(() -> {
            Log.d("DLObserver", "notify l=" + mFanLevel + ", isDocked=" + isDocked());
            if (isDocked()) {
                mContext.sendBroadcastAsUser(new Intent("com.google.android.systemui.dreamliner.ACTION_UPDATE_FAN_LEVEL").putExtra("fan_level", mFanLevel).addFlags(1073741824), UserHandle.CURRENT);
            }
        });
    }

    private boolean isWirelessCharging(Context context) {
        Intent registerReceiver = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        if (registerReceiver == null) {
            if (DEBUG) {
                Log.d("DLObserver", "null battery intent when checking plugged status");
            }
            return DEBUG;
        }
        int intExtra = registerReceiver.getIntExtra("plugged", -1);
        if (DEBUG) {
            Log.d("DLObserver", "plugged = " + intExtra);
        }
        if (intExtra != 4) {
            return DEBUG;
        }
        return true;
    }

    @VisibleForTesting
    void updateCurrentDockingStatus(Context context) {
        notifyForceEnabledAmbientDisplay(DEBUG);
        checkIsDockPresentIfNeeded(context);
    }

    private void checkIsDockPresentIfNeeded(Context context) {
        if (mWirelessCharger == null || !isWirelessCharging(context)) {
            return;
        }
        runOnBackgroundThread(new IsDockPresent(context));
    }

    private void getFeatures(Intent intent) {
        long longExtra = intent.getLongExtra("charger_id", -1L);
        if (DEBUG) {
            Log.d("DLObserver", "gF, id=" + longExtra);
        }
        ResultReceiver resultReceiver = intent.getParcelableExtra("android.intent.extra.RESULT_RECEIVER");
        if (resultReceiver != null) {
            if (longExtra == -1) {
                resultReceiver.send(1, null);
            } else {
                runOnBackgroundThread(new GetFeatures(resultReceiver, longExtra));
            }
        }
    }

    private void setFeatures(Intent intent) {
        long longExtra = intent.getLongExtra("charger_id", -1L);
        long longExtra2 = intent.getLongExtra("charger_feature", -1L);
        ResultReceiver resultReceiver = intent.getParcelableExtra("android.intent.extra.RESULT_RECEIVER");
        if (DEBUG) {
            Log.d("DLObserver", "sF, id=" + longExtra + ", feature=" + longExtra2);
        }
        if (resultReceiver != null) {
            if (longExtra == -1 || longExtra2 == -1) {
                resultReceiver.send(1, null);
            } else {
                runOnBackgroundThread(new SetFeatures(resultReceiver, longExtra, longExtra2));
            }
        }
    }

    private IntentFilter getDockIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        intentFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        intentFilter.addAction(ACTION_REBIND_DOCK_SERVICE);
        intentFilter.addAction("com.google.android.systemui.dreamliner.ACTION_GET_FEATURES");
        intentFilter.addAction("com.google.android.systemui.dreamliner.ACTION_SET_FEATURES");
        intentFilter.setPriority(1000);
        return intentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        if (DEBUG) {
            Log.i("DLObserver", "onReceive(); " + intent.getAction());
        }
        String action = intent.getAction();
        switch (action) {
            case "android.intent.action.ACTION_POWER_DISCONNECTED":
                stopDreamlinerService(context);
                sIsDockingUiShowing = DEBUG;
                break;
            case "com.google.android.systemui.dreamliner.ACTION_SET_FEATURES":
                setFeatures(intent);
                break;
            case "android.intent.action.BOOT_COMPLETED":
                break;
            case "com.google.android.systemui.dreamliner.ACTION_GET_FEATURES":
                getFeatures(intent);
                break;
            case "android.intent.action.ACTION_POWER_CONNECTED":
                checkIsDockPresentIfNeeded(context);
                break;
            case ACTION_REBIND_DOCK_SERVICE:
                updateCurrentDockingStatus(context);
                break;
        }
    }

    private synchronized void startDreamlinerService(Context context, int i, int i2, int i3) {
        notifyForceEnabledAmbientDisplay(true);
        if (mDreamlinerServiceConn == null) {
            mDreamlinerReceiver.registerReceiver(context);
            ImageView imageView = mDreamlinerGear;
            DockGestureController dockGestureController = new DockGestureController(context, imageView, mPhotoPreview, (View) imageView.getParent(), mIndicationController, mStatusBarStateController, (KeyguardStateController) Dependency.get(KeyguardStateController.class));
            mDockGestureController = dockGestureController;
            mConfigurationController.addCallback(dockGestureController);
            Intent intent = new Intent(ACTION_START_DREAMLINER_CONTROL_SERVICE);
            intent.setComponent(ComponentName.unflattenFromString(COMPONENTNAME_DREAMLINER_CONTROL_SERVICE));
            intent.putExtra("type", i);
            intent.putExtra("orientation", i2);
            intent.putExtra("id", i3);
            intent.putExtra("occluded", new KeyguardVisibility(context).isKeyguardOccluded());
            try {
                DreamlinerServiceConn dreamlinerServiceConn = new DreamlinerServiceConn(context);
                mDreamlinerServiceConn = dreamlinerServiceConn;
                if (context.bindServiceAsUser(intent, dreamlinerServiceConn, 1, new UserHandle(mUserTracker.getUserId()))) {
                    mUserTracker.addCallback(mUserChangedCallback, new HandlerExecutor(mMainHandler));
                    return;
                }
            } catch (SecurityException e) {
                Log.e("DLObserver", e.getMessage(), e);
            }
            mDreamlinerServiceConn = null;
            Log.w("DLObserver", "Unable to bind Dreamliner service: " + intent);
        }
    }

    private void stopDreamlinerService(Context context) {
        notifyForceEnabledAmbientDisplay(DEBUG);
        onDockStateChanged(0);
        try {
            if (mDreamlinerServiceConn == null) {
                return;
            }
            if (assertNotNull(mDockGestureController, DockGestureController.class.getSimpleName())) {
                mConfigurationController.removeCallback(mDockGestureController);
                mDockGestureController.stopMonitoring();
                mDockGestureController = null;
            }
            mUserTracker.removeCallback(mUserChangedCallback);
            mDreamlinerReceiver.unregisterReceiver(context);
            context.unbindService(mDreamlinerServiceConn);
            mDreamlinerServiceConn = null;
        } catch (IllegalArgumentException e) {
            Log.e("DLObserver", e.getMessage(), e);
        }
    }

    private boolean assertNotNull(Object obj, String str) {
        if (obj == null) {
            Log.w("DLObserver", str + " is null");
            return DEBUG;
        }
        return true;
    }

    private void notifyForceEnabledAmbientDisplay(boolean z) {
        IDreamManager dreamManagerInstance = getDreamManagerInstance();
        if (dreamManagerInstance != null) {
            try {
                dreamManagerInstance.forceAmbientDisplayEnabled(z);
                return;
            } catch (RemoteException unused) {
                return;
            }
        }
        Log.e("DLObserver", "DreamManager not found");
    }

    private IDreamManager getDreamManagerInstance() {
        return IDreamManager.Stub.asInterface(ServiceManager.checkService("dreams"));
    }

    private void sendDockIdleIntent(Context context) {
        if (DEBUG) {
            Log.d("DLObserver", "sendDockIdleIntent()");
        }
        context.sendBroadcast(new Intent("android.intent.action.DOCK_IDLE").addFlags(1073741824));
    }

    private void sendDockActiveIntent(Context context) {
        if (DEBUG) {
            Log.d("DLObserver", "sendDockActiveIntent()");
        }
        context.sendBroadcast(new Intent("android.intent.action.DOCK_ACTIVE").addFlags(1073741824));
    }

    private void triggerKeyExchangeWithDock(Intent intent) {
        ResultReceiver resultReceiver;
        if (DEBUG) {
            Log.d("DLObserver", "triggerKeyExchangeWithDock");
        }
        if (intent == null || (resultReceiver = intent.getParcelableExtra("android.intent.extra.RESULT_RECEIVER")) == null) {
            return;
        }
        byte[] byteArrayExtra = intent.getByteArrayExtra(EXTRA_PUBLIC_KEY);
        if (byteArrayExtra == null || byteArrayExtra.length <= 0) {
            resultReceiver.send(1, null);
        } else {
            runOnBackgroundThread(new KeyExchangeWithDock(resultReceiver, byteArrayExtra));
        }
    }

    private void triggerChallengeWithDock(Intent intent) {
        ResultReceiver resultReceiver;
        if (DEBUG) {
            Log.d("DLObserver", "triggerChallengeWithDock");
        }
        if (intent == null || (resultReceiver = intent.getParcelableExtra("android.intent.extra.RESULT_RECEIVER")) == null) {
            return;
        }
        byte byteExtra = intent.getByteExtra(EXTRA_CHALLENGE_DOCK_ID, (byte) -1);
        byte[] byteArrayExtra = intent.getByteArrayExtra(EXTRA_CHALLENGE_DATA);
        if (byteArrayExtra == null || byteArrayExtra.length <= 0 || byteExtra < 0) {
            resultReceiver.send(1, null);
        } else {
            runOnBackgroundThread(new ChallengeWithDock(resultReceiver, byteExtra, byteArrayExtra));
        }
    }

    private void configPhotoAction(Intent intent) {
        if (DEBUG) {
            Log.d("DLObserver", "handlePhotoAction");
        }
        if (intent == null) {
            return;
        }
        final ResultReceiver resultReceiver = intent.getParcelableExtra("android.intent.extra.RESULT_RECEIVER");
        boolean booleanExtra = intent.getBooleanExtra("enabled", DEBUG);
        if (mDockGestureController != null) {
            mDockGestureController.setPhotoEnabled(booleanExtra);
        }
        if (resultReceiver == null || mIndicationController == null) {
            return;
        }
        mPhotoAction = () -> mIndicationController.showPromo(resultReceiver);
    }

    private void runPhotoAction() {
        if (mLastAlignState != 0 || mPhotoAction == null || mIndicationController.isPromoShowing()) {
            return;
        }
        mMainExecutor.executeDelayed(mPhotoAction, Duration.ofSeconds(3L).toMillis());
    }

    private void handlePhotoFailure() {
        Log.w("DLObserver", "Fail to launch photo");
        if (mDockGestureController != null) {
            mDockGestureController.handlePhotoFailure();
        }
    }

    private byte[] convertArrayListToPrimitiveArray(ArrayList<Byte> arrayList) {
        if (arrayList == null || arrayList.isEmpty()) {
            return null;
        }
        int size = arrayList.size();
        byte[] bArr = new byte[size];
        for (int i = 0; i < size; i++) {
            bArr[i] = arrayList.get(i);
        }
        return bArr;
    }

    private Bundle createKeyExchangeResponseBundle(byte b, ArrayList<Byte> arrayList) {
        if (arrayList == null || arrayList.isEmpty()) {
            return null;
        }
        byte[] convertArrayListToPrimitiveArray = convertArrayListToPrimitiveArray(arrayList);
        Bundle bundle = new Bundle();
        bundle.putByte("dock_id", b);
        bundle.putByteArray("dock_public_key", convertArrayListToPrimitiveArray);
        return bundle;
    }

    private Bundle createChallengeResponseBundle(ArrayList<Byte> arrayList) {
        if (arrayList == null || arrayList.isEmpty()) {
            return null;
        }
        byte[] convertArrayListToPrimitiveArray = convertArrayListToPrimitiveArray(arrayList);
        Bundle bundle = new Bundle();
        bundle.putByteArray("challenge_response", convertArrayListToPrimitiveArray);
        return bundle;
    }

    private Bundle createWpcAuthDigestsResponseBundle(byte b, byte b2, ArrayList<byte[]> arrayList) {
        Bundle bundle = new Bundle();
        bundle.putByte("slot_populated_mask", b);
        bundle.putByte("slot_returned_mask", b2);
        final ArrayList<Bundle> arrayList2 = new ArrayList<>();
        if (arrayList != null) {
            arrayList.forEach(bytes -> {
                Bundle bundle1 = new Bundle();
                bundle1.putByteArray("wpc_digest", bytes);
                arrayList2.add(bundle1);
            });
        }
        bundle.putParcelableArrayList("wpc_digests", arrayList2);
        return bundle;
    }

    private Bundle createWpcAuthCertificateResponseBundle(ArrayList<Byte> arrayList) {
        if (arrayList == null || arrayList.isEmpty()) {
            return null;
        }
        byte[] convertArrayListToPrimitiveArray = convertArrayListToPrimitiveArray(arrayList);
        Bundle bundle = new Bundle();
        bundle.putByteArray("wpc_cert", convertArrayListToPrimitiveArray);
        return bundle;
    }

    private Bundle createWpcAuthChallengeResponseBundle(byte b, byte b2, byte b3, ArrayList<Byte> arrayList, ArrayList<Byte> arrayList2) {
        Bundle bundle = new Bundle();
        bundle.putByte("max_protocol_ver", b);
        bundle.putByte("slot_populated_mask", b2);
        bundle.putByte("cert_lsb", b3);
        bundle.putByteArray("signature_r", convertArrayListToPrimitiveArray(arrayList));
        bundle.putByteArray("signature_s", convertArrayListToPrimitiveArray(arrayList2));
        return bundle;
    }

    private Bundle createGetFeatureResponse(long j) {
        Bundle bundle = new Bundle();
        bundle.putLong("charger_feature", j);
        return bundle;
    }

    @VisibleForTesting
    static final class GetFanSimpleInformationCallback implements WirelessCharger.GetFanSimpleInformationCallback {
        private final byte mFanId;
        private final ResultReceiver mResultReceiver;

        GetFanSimpleInformationCallback(byte b, ResultReceiver resultReceiver) {
            mFanId = b;
            mResultReceiver = resultReceiver;
        }

        @Override
        public void onCallback(int i, Bundle bundle) {
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "Callback of command=3, result=" + i + ", i=" + ((int) mFanId));
            }
            if (i == 0) {
                if (DockObserver.DEBUG) {
                    Log.d("DLObserver", "Callback of command=3, i=" + bundle.getByte("fan_id", (byte) -1) + ", m=" + bundle.getByte("fan_mode", (byte) -1) + ", cr=" + bundle.getInt("fan_current_rpm", -1));
                }
                mResultReceiver.send(0, bundle);
                return;
            }
            mResultReceiver.send(1, null);
        }
    }

    @VisibleForTesting
    static final class GetFanInformationCallback implements WirelessCharger.GetFanInformationCallback {
        private final byte mFanId;
        private final ResultReceiver mResultReceiver;

        GetFanInformationCallback(byte b, ResultReceiver resultReceiver) {
            mFanId = b;
            mResultReceiver = resultReceiver;
        }

        @Override
        public void onCallback(int i, Bundle bundle) {
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "Callback of command=0, result=" + i + ", i=" + ((int) mFanId));
            }
            if (i == 0) {
                if (DockObserver.DEBUG) {
                    Log.d("DLObserver", "Callback of command=0, i=" + bundle.getByte("fan_id", (byte) -1) + ", m=" + bundle.getByte("fan_mode", (byte) -1) + ", cr=" + bundle.getInt("fan_current_rpm", -1) + ", mir=" + bundle.getInt("fan_min_rpm", -1) + ", mxr=" + bundle.getInt("fan_max_rpm", -1) + ", t=" + bundle.getByte("fan_type", (byte) -1) + ", c=" + bundle.getByte("fan_count", (byte) -1));
                }
                mResultReceiver.send(0, bundle);
                return;
            }
            mResultReceiver.send(1, null);
        }
    }

    @VisibleForTesting
    static final class SetFanCallback implements WirelessCharger.SetFanCallback {
        SetFanCallback() {
        }

        @Override
        public void onCallback(int i, Bundle bundle) {
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "Callback of command=1, i=" + bundle.getByte("fan_id", (byte) -1) + ", m=" + bundle.getByte("fan_mode", (byte) -1) + ", cr=" + bundle.getInt("fan_current_rpm", -1));
            }
        }
    }

    private class IsDockPresent implements Runnable {
        final Context context;

        public IsDockPresent(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            if (mWirelessCharger == null) {
                return;
            }
            mWirelessCharger.asyncIsDockPresent(new IsDockPresentCallback(context));
        }
    }

    private class GetDockInfo implements Runnable {
        final Context context;
        final ResultReceiver resultReceiver;

        public GetDockInfo(ResultReceiver resultReceiver, Context context) {
            this.resultReceiver = resultReceiver;
            this.context = context;
        }

        @Override
        public void run() {
            if (mWirelessCharger == null) {
                return;
            }
            mWirelessCharger.getInformation(new GetInformationCallback(resultReceiver));
        }
    }

    private class KeyExchangeWithDock implements Runnable {
        final byte[] publicKey;
        final ResultReceiver resultReceiver;

        public KeyExchangeWithDock(ResultReceiver resultReceiver, byte[] bArr) {
            publicKey = bArr;
            this.resultReceiver = resultReceiver;
        }

        @Override
        public void run() {
            if (mWirelessCharger == null) {
                return;
            }
            mWirelessCharger.keyExchange(publicKey, new KeyExchangeCallback(resultReceiver));
        }
    }

    private class GetFanSimpleInformation implements Runnable {
        final byte mFanId;
        final ResultReceiver mResultReceiver;

        GetFanSimpleInformation(byte b, ResultReceiver resultReceiver) {
            mFanId = b;
            mResultReceiver = resultReceiver;
        }

        @Override
        public void run() {
            if (mWirelessCharger == null) {
                return;
            }
            byte b = mFanId;
            mWirelessCharger.getFanSimpleInformation(b, new GetFanSimpleInformationCallback(b, mResultReceiver));
        }
    }

    private class GetFanInformation implements Runnable {
        final byte mFanId;
        final ResultReceiver mResultReceiver;

        public GetFanInformation(byte b, ResultReceiver resultReceiver) {
            mFanId = b;
            mResultReceiver = resultReceiver;
        }

        @Override
        public void run() {
            if (mWirelessCharger == null) {
                return;
            }
            byte b = mFanId;
            mWirelessCharger.getFanInformation(b, new GetFanInformationCallback(b, mResultReceiver));
        }
    }

    private class SetFan implements Runnable {
        final byte mFanId;
        final byte mFanMode;
        final int mFanRpm;

        public SetFan(byte b, byte b2, int i) {
            mFanId = b;
            mFanMode = b2;
            mFanRpm = i;
        }

        @Override
        public void run() {
            if (mWirelessCharger == null) {
                return;
            }
            mWirelessCharger.setFan(mFanId, mFanMode, mFanRpm, new SetFanCallback());
        }
    }

    private class ChallengeWithDock implements Runnable {
        final byte[] challengeData;
        final byte dockId;
        final ResultReceiver resultReceiver;

        public ChallengeWithDock(ResultReceiver resultReceiver, byte b, byte[] bArr) {
            dockId = b;
            challengeData = bArr;
            this.resultReceiver = resultReceiver;
        }

        @Override
        public void run() {
            if (mWirelessCharger == null) {
                return;
            }
            mWirelessCharger.challenge(dockId, challengeData, new ChallengeCallback(resultReceiver));
        }
    }

    private class GetWpcAuthDigests implements Runnable {
        final ResultReceiver mResultReceiver;
        final byte mSlotMask;

        GetWpcAuthDigests(ResultReceiver resultReceiver, byte b) {
            mResultReceiver = resultReceiver;
            mSlotMask = b;
        }

        @Override
        public void run() {
            if (mWirelessCharger == null) {
                return;
            }
            mWirelessCharger.getWpcAuthDigests(mSlotMask, new GetWpcAuthDigestsCallback(mResultReceiver));
        }
    }

    private class GetWpcAuthCertificate implements Runnable {
        final short mLength;
        final short mOffset;
        final ResultReceiver mResultReceiver;
        final byte mSlotNum;

        GetWpcAuthCertificate(ResultReceiver resultReceiver, byte b, short s, short s2) {
            mResultReceiver = resultReceiver;
            mSlotNum = b;
            mOffset = s;
            mLength = s2;
        }

        @Override
        public void run() {
            if (mWirelessCharger == null) {
                return;
            }
            mWirelessCharger.getWpcAuthCertificate(mSlotNum, mOffset, mLength, new GetWpcAuthCertificateCallback(mResultReceiver));
        }
    }

    private class GetWpcAuthChallengeResponse implements Runnable {
        final ResultReceiver mResultReceiver;
        final byte mSlotNum;
        final byte[] mWpcNonce;

        GetWpcAuthChallengeResponse(ResultReceiver resultReceiver, byte b, byte[] bArr) {
            mResultReceiver = resultReceiver;
            mSlotNum = b;
            mWpcNonce = bArr;
        }

        @Override
        public void run() {
            if (mWirelessCharger == null) {
                return;
            }
            mWirelessCharger.getWpcAuthChallengeResponse(mSlotNum, mWpcNonce, new GetWpcAuthChallengeResponseCallback(mResultReceiver));
        }
    }

    private class SetFeatures implements Runnable {
        final long mChargerId;
        final long mFeature;
        final ResultReceiver mResultReceiver;

        SetFeatures(ResultReceiver resultReceiver, long j, long j2) {
            mResultReceiver = resultReceiver;
            mChargerId = j;
            mFeature = j2;
        }

        @Override
        public void run() {
            if (mWirelessCharger == null) {
                return;
            }
            mWirelessCharger.setFeatures(mChargerId, mFeature, new WirelessCharger.SetFeaturesCallback() {
                @Override
                public void onCallback(int i) {
                    mResultReceiver.send(i, null);
                }
            });
        }
    }

    private class GetFeatures implements Runnable {
        final long mChargerId;
        final ResultReceiver mResultReceiver;

        GetFeatures(ResultReceiver resultReceiver, long j) {
            mResultReceiver = resultReceiver;
            mChargerId = j;
        }

        @Override
        public void run() {
            if (mWirelessCharger == null) {
                return;
            }
            mWirelessCharger.getFeatures(mChargerId, new GetFeaturesCallback(mResultReceiver));
        }
    }

    @VisibleForTesting
    final class DreamlinerServiceConn implements ServiceConnection {
        final Context mContext;

        public DreamlinerServiceConn(Context context) {
            mContext = context;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            sendDockActiveIntent(mContext);
        }

        @Override
        public void onBindingDied(ComponentName componentName) {
            stopDreamlinerService(mContext);
        }
    }

    @VisibleForTesting
    final class IsDockPresentCallback implements WirelessCharger.IsDockPresentCallback {
        private final Context mContext;

        IsDockPresentCallback(Context context) {
            mContext = context;
        }

        @Override
        public void onCallback(boolean z, byte b, byte b2, boolean z2, int i) {
            if (DockObserver.DEBUG) {
                Log.i("DLObserver", "isDockPresent() docked: " + z + ", id: " + i + ", type: " + ((int) b) + ", orientation: " + ((int) b2) + ", support GetInfo: " + z2);
            }
            if (z) {
                startDreamlinerService(mContext, b, b2, i);
            }
        }
    }

    @VisibleForTesting
    final class GetInformationCallback implements WirelessCharger.GetInformationCallback {
        private final ResultReceiver mResultReceiver;

        GetInformationCallback(ResultReceiver resultReceiver) {
            mResultReceiver = resultReceiver;
        }

        @Override
        public void onCallback(int i, DockInfo dockInfo) {
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "getInformation() Result: " + i);
            }
            if (i != 0) {
                if (i == 1) {
                    return;
                }
                mResultReceiver.send(1, null);
                return;
            }
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "getInformation() DockInfo: " + dockInfo.toString());
            }
            mResultReceiver.send(0, dockInfo.toBundle());
        }
    }

    @VisibleForTesting
    final class KeyExchangeCallback implements WirelessCharger.KeyExchangeCallback {
        private final ResultReceiver mResultReceiver;

        KeyExchangeCallback(ResultReceiver resultReceiver) {
            mResultReceiver = resultReceiver;
        }

        @Override
        public void onCallback(int i, byte b, ArrayList<Byte> arrayList) {
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "keyExchange() Result: " + i);
            }
            if (i == 0) {
                if (DockObserver.DEBUG) {
                    Log.d("DLObserver", "keyExchange() key: " + arrayList);
                }
                mResultReceiver.send(0, createKeyExchangeResponseBundle(b, arrayList));
                return;
            }
            mResultReceiver.send(1, null);
        }
    }

    @VisibleForTesting
    final class ChallengeCallback implements WirelessCharger.ChallengeCallback {
        private final ResultReceiver mResultReceiver;

        ChallengeCallback(ResultReceiver resultReceiver) {
            mResultReceiver = resultReceiver;
        }

        @Override
        public void onCallback(int i, ArrayList<Byte> arrayList) {
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "challenge() Result: " + i);
            }
            if (i == 0) {
                if (DockObserver.DEBUG) {
                    Log.d("DLObserver", "challenge() response: " + arrayList);
                }
                mResultReceiver.send(0, createChallengeResponseBundle(arrayList));
                return;
            }
            mResultReceiver.send(1, null);
        }
    }

    @VisibleForTesting
    final class GetWpcAuthDigestsCallback implements WirelessCharger.GetWpcAuthDigestsCallback {
        private final ResultReceiver mResultReceiver;

        GetWpcAuthDigestsCallback(ResultReceiver resultReceiver) {
            mResultReceiver = resultReceiver;
        }

        @Override
        public void onCallback(int i, byte b, byte b2, ArrayList<byte[]> arrayList) {
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "GWAD() result: " + i);
            }
            if (i == 0) {
                if (DockObserver.DEBUG) {
                    Log.d("DLObserver", "GWAD() response: pm=" + ((int) b) + ", rm=" + ((int) b2) + ", d=" + arrayList);
                }
                mResultReceiver.send(0, createWpcAuthDigestsResponseBundle(b, b2, arrayList));
                return;
            }
            mResultReceiver.send(1, null);
        }
    }

    @VisibleForTesting
    final class GetWpcAuthCertificateCallback implements WirelessCharger.GetWpcAuthCertificateCallback {
        private final ResultReceiver mResultReceiver;

        GetWpcAuthCertificateCallback(ResultReceiver resultReceiver) {
            mResultReceiver = resultReceiver;
        }

        @Override
        public void onCallback(int i, ArrayList<Byte> arrayList) {
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "GWAC() result: " + i);
            }
            if (i == 0) {
                if (DockObserver.DEBUG) {
                    Log.d("DLObserver", "GWAC() response: c=" + arrayList);
                }
                mResultReceiver.send(0, createWpcAuthCertificateResponseBundle(arrayList));
                return;
            }
            mResultReceiver.send(1, null);
        }
    }

    @VisibleForTesting
    final class GetWpcAuthChallengeResponseCallback implements WirelessCharger.GetWpcAuthChallengeResponseCallback {
        private final ResultReceiver mResultReceiver;

        GetWpcAuthChallengeResponseCallback(ResultReceiver resultReceiver) {
            mResultReceiver = resultReceiver;
        }

        @Override
        public void onCallback(int i, byte b, byte b2, byte b3, ArrayList<Byte> arrayList, ArrayList<Byte> arrayList2) {
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "GWACR() result: " + i);
            }
            if (i == 0) {
                if (DockObserver.DEBUG) {
                    Log.d("DLObserver", "GWACR() response: mpv=" + ((int) b) + ", pm=" + ((int) b2) + ", chl=" + ((int) b3) + ", rv=" + arrayList + ", sv=" + arrayList2);
                }
                mResultReceiver.send(0, createWpcAuthChallengeResponseBundle(b, b2, b3, arrayList, arrayList2));
                return;
            }
            mResultReceiver.send(1, null);
        }
    }

    @VisibleForTesting
    final class GetFeaturesCallback implements WirelessCharger.GetFeaturesCallback {
        private final ResultReceiver mResultReceiver;

        GetFeaturesCallback(ResultReceiver resultReceiver) {
            mResultReceiver = resultReceiver;
        }

        @Override
        public void onCallback(int i, long j) {
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "GF() result: " + i);
            }
            if (i == 0) {
                if (DockObserver.DEBUG) {
                    Log.d("DLObserver", "GF() response: f=" + j);
                }
                mResultReceiver.send(0, createGetFeatureResponse(j));
                return;
            }
            mResultReceiver.send(1, null);
        }
    }

    @VisibleForTesting
    class DreamlinerBroadcastReceiver extends BroadcastReceiver {
        private boolean mListening;

        DreamlinerBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "Dock Receiver.onReceive(): " + intent.getAction());
            }
            String action = intent.getAction();
            switch (action) {
                case "com.google.android.systemui.dreamliner.ACTION_GET_FAN_LEVEL":
                    onFanLevelChange();
                    return;
                case "com.google.android.systemui.dreamliner.ACTION_SET_FAN":
                    setFan(intent);
                    return;
                case DockObserver.ACTION_GET_DOCK_INFO:
                    ResultReceiver resultReceiver = intent.getParcelableExtra("android.intent.extra.RESULT_RECEIVER");
                    if (resultReceiver == null) {
                        return;
                    }
                    DockObserver.runOnBackgroundThread(new GetDockInfo(resultReceiver, context));
                    return;
                case DockObserver.ACTION_KEY_EXCHANGE:
                    triggerKeyExchangeWithDock(intent);
                    return;
                case "com.google.android.systemui.dreamliner.photo_error":
                    handlePhotoFailure();
                    return;
                case DockObserver.ACTION_DOCK_UI_IDLE:
                    sendDockIdleIntent(context);
                    return;
                case DockObserver.ACTION_CHALLENGE:
                    triggerChallengeWithDock(intent);
                    return;
                case "com.google.android.systemui.dreamliner.ACTION_GET_FAN_SIMPLE_INFO":
                    getFanSimpleInformation(intent);
                    return;
                case "com.google.android.systemui.dreamliner.ACTION_GET_WPC_DIGESTS":
                    getWpcAuthDigests(intent);
                    return;
                case "com.google.android.systemui.dreamliner.paired":
                    if (assertNotNull(mDockGestureController, DockGestureController.class.getSimpleName())) {
                        mDockGestureController.setTapAction((PendingIntent) intent.getParcelableExtra("single_tap_action"));
                    }
                    break;
                case "com.google.android.systemui.dreamliner.resume":
                    break;
                case "com.google.android.systemui.dreamliner.undock":
                    onDockStateChanged(0);
                    if (!assertNotNull(mDockGestureController, DockGestureController.class.getSimpleName())) {
                        return;
                    }
                    mDockGestureController.stopMonitoring();
                    return;
                case "com.google.android.systemui.dreamliner.dream":
                    tryTurnScreenOff(context);
                    return;
                case "com.google.android.systemui.dreamliner.pause":
                    onDockStateChanged(2);
                    if (!assertNotNull(mDockGestureController, DockGestureController.class.getSimpleName())) {
                        return;
                    }
                    mDockGestureController.stopMonitoring();
                    return;
                case "com.google.android.systemui.dreamliner.photo":
                    configPhotoAction(intent);
                    runPhotoAction();
                    return;
                case "com.google.android.systemui.dreamliner.assistant_poodle":
                    DockIndicationController dockIndicationController = mIndicationController;
                    if (dockIndicationController == null) {
                        return;
                    }
                    dockIndicationController.setShowing(intent.getBooleanExtra(DockObserver.KEY_SHOWING, DockObserver.DEBUG));
                    return;
                case "com.google.android.systemui.dreamliner.ACTION_GET_WPC_CERTIFICATE":
                    getWpcAuthCertificate(intent);
                    return;
                case DockObserver.ACTION_DOCK_UI_ACTIVE:
                    sendDockActiveIntent(context);
                    return;
                case "com.google.android.systemui.dreamliner.ACTION_GET_FAN_INFO":
                    getFanInformation(intent);
                    return;
                case "com.google.android.systemui.dreamliner.ACTION_GET_WPC_CHALLENGE_RESPONSE":
                    getWpcAuthChallengeResponse(intent);
                    return;
            }
            onDockStateChanged(1);
            if (!assertNotNull(mDockGestureController, DockGestureController.class.getSimpleName())) {
                return;
            }
            mDockGestureController.startMonitoring();
        }

        private void getFanSimpleInformation(Intent intent) {
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "command=3, i=" + ((int) intent.getByteExtra("fan_id", (byte) -1)));
            }
            ResultReceiver resultReceiver = intent.getParcelableExtra("android.intent.extra.RESULT_RECEIVER");
            if (resultReceiver != null) {
                DockObserver.runOnBackgroundThread(new GetFanSimpleInformation(intent.getByteExtra("fan_id", (byte) 0), resultReceiver));
            }
        }

        private void getFanInformation(Intent intent) {
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "command=0, i=" + ((int) intent.getByteExtra("fan_id", (byte) -1)));
            }
            ResultReceiver resultReceiver = intent.getParcelableExtra("android.intent.extra.RESULT_RECEIVER");
            if (resultReceiver != null) {
                DockObserver.runOnBackgroundThread(new GetFanInformation(intent.getByteExtra("fan_id", (byte) 0), resultReceiver));
            }
        }

        private void setFan(Intent intent) {
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "command=1, i=" + ((int) intent.getByteExtra("fan_id", (byte) -1)) + ", m=" + ((int) intent.getByteExtra("fan_mode", (byte) -1)) + ", r=" + intent.getIntExtra("fan_rpm", -1));
            }
            byte byteExtra = intent.getByteExtra("fan_id", (byte) 0);
            byte byteExtra2 = intent.getByteExtra("fan_mode", (byte) 0);
            int intExtra = intent.getIntExtra("fan_rpm", -1);
            if (byteExtra2 != 1 || intExtra != -1) {
                DockObserver.runOnBackgroundThread(new SetFan(byteExtra, byteExtra2, intExtra));
            } else {
                Log.e("DLObserver", "Failed to get r.");
            }
        }

        private void getWpcAuthDigests(Intent intent) {
            byte byteExtra = intent.getByteExtra("slot_mask", (byte) -1);
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "gWAD, mask=" + ((int) byteExtra));
            }
            ResultReceiver resultReceiver = intent.getParcelableExtra("android.intent.extra.RESULT_RECEIVER");
            if (resultReceiver != null) {
                if (byteExtra != -1) {
                    DockObserver.runOnBackgroundThread(new GetWpcAuthDigests(resultReceiver, byteExtra));
                } else {
                    resultReceiver.send(1, null);
                }
            }
        }

        private void getWpcAuthCertificate(Intent intent) {
            byte byteExtra = intent.getByteExtra("slot_number", (byte) -1);
            short shortExtra = intent.getShortExtra("cert_offset", (short) -1);
            short shortExtra2 = intent.getShortExtra("cert_length", (short) -1);
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "gWAC, num=" + ((int) byteExtra) + ", offset=" + ((int) shortExtra) + ", length=" + ((int) shortExtra2));
            }
            ResultReceiver resultReceiver = intent.getParcelableExtra("android.intent.extra.RESULT_RECEIVER");
            if (resultReceiver != null) {
                if (byteExtra != -1 && shortExtra != -1 && shortExtra2 != -1) {
                    DockObserver.runOnBackgroundThread(new GetWpcAuthCertificate(resultReceiver, byteExtra, shortExtra, shortExtra2));
                } else {
                    resultReceiver.send(1, null);
                }
            }
        }

        private void getWpcAuthChallengeResponse(Intent intent) {
            byte byteExtra = intent.getByteExtra("slot_number", (byte) -1);
            if (DockObserver.DEBUG) {
                Log.d("DLObserver", "gWACR, num=" + ((int) byteExtra));
            }
            ResultReceiver resultReceiver = intent.getParcelableExtra("android.intent.extra.RESULT_RECEIVER");
            if (resultReceiver != null) {
                byte[] byteArrayExtra = intent.getByteArrayExtra("wpc_nonce");
                if (byteArrayExtra != null && byteArrayExtra.length > 0) {
                    DockObserver.runOnBackgroundThread(new GetWpcAuthChallengeResponse(resultReceiver, byteExtra, byteArrayExtra));
                } else {
                    resultReceiver.send(1, null);
                }
            }
        }

        private IntentFilter getIntentFilter() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(DockObserver.ACTION_GET_DOCK_INFO);
            intentFilter.addAction(DockObserver.ACTION_DOCK_UI_IDLE);
            intentFilter.addAction(DockObserver.ACTION_DOCK_UI_ACTIVE);
            intentFilter.addAction(DockObserver.ACTION_KEY_EXCHANGE);
            intentFilter.addAction(DockObserver.ACTION_CHALLENGE);
            intentFilter.addAction("com.google.android.systemui.dreamliner.dream");
            intentFilter.addAction("com.google.android.systemui.dreamliner.paired");
            intentFilter.addAction("com.google.android.systemui.dreamliner.pause");
            intentFilter.addAction("com.google.android.systemui.dreamliner.resume");
            intentFilter.addAction("com.google.android.systemui.dreamliner.undock");
            intentFilter.addAction("com.google.android.systemui.dreamliner.assistant_poodle");
            intentFilter.addAction("com.google.android.systemui.dreamliner.photo");
            intentFilter.addAction("com.google.android.systemui.dreamliner.photo_error");
            intentFilter.addAction("com.google.android.systemui.dreamliner.ACTION_GET_FAN_INFO");
            intentFilter.addAction("com.google.android.systemui.dreamliner.ACTION_GET_FAN_SIMPLE_INFO");
            intentFilter.addAction("com.google.android.systemui.dreamliner.ACTION_SET_FAN");
            intentFilter.addAction("com.google.android.systemui.dreamliner.ACTION_GET_FAN_LEVEL");
            intentFilter.addAction("com.google.android.systemui.dreamliner.ACTION_GET_WPC_DIGESTS");
            intentFilter.addAction("com.google.android.systemui.dreamliner.ACTION_GET_WPC_CERTIFICATE");
            intentFilter.addAction("com.google.android.systemui.dreamliner.ACTION_GET_WPC_CHALLENGE_RESPONSE");
            return intentFilter;
        }

        public void registerReceiver(Context context) {
            if (!mListening) {
                context.registerReceiverAsUser(this, UserHandle.ALL, getIntentFilter(), DockObserver.PERMISSION_WIRELESS_CHARGER_STATUS, null);
                mListening = true;
            }
        }

        public void unregisterReceiver(Context context) {
            if (mListening) {
                context.unregisterReceiver(this);
                mListening = DockObserver.DEBUG;
            }
        }
    }
}
