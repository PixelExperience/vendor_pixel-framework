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

import android.content.Context;
import android.os.Bundle;
import android.os.IHwBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import vendor.google.wireless_charger.V1_2.IWirelessCharger;
import vendor.google.wireless_charger.V1_2.IWirelessChargerRtxStatusCallback;
import vendor.google.wireless_charger.V1_2.RtxStatusInfo;


public class ReverseWirelessCharger extends IWirelessChargerRtxStatusCallback.Stub implements IHwBinder.DeathRecipient {
    private static final boolean DEBUG = Log.isLoggable("ReverseWirelessCharger", 3);
    private Context mContext;
    private IWirelessCharger mWirelessCharger;
    private final ArrayList<IsDockPresentCallback> mIsDockPresentCallbacks = new ArrayList<>();
    private final ArrayList<RtxInformationCallback> mRtxInformationCallbacks = new ArrayList<>();
    private final ArrayList<RtxStatusCallback> mRtxStatusCallbacks = new ArrayList<>();
    private final Object mLock = new Object();
    private final LocalRtxInformationCallback mLocalRtxInformationCallback = new LocalRtxInformationCallback();

    public interface IsDockPresentCallback {
        void onIsDockPresentChanged(boolean z, byte b, byte b2, boolean z2, int i);
    }

    public interface RtxInformationCallback {
        void onRtxInformationChanged(RtxStatusInfo rtxStatusInfo);
    }

    public interface RtxStatusCallback {
        void onRtxStatusChanged(RtxStatusInfo rtxStatusInfo);
    }

    public ReverseWirelessCharger(Context context) {
        mContext = context;
    }

    private static Bundle buildDockPresentBundle(boolean z, byte b, byte b2, boolean z2, int i) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("key_dock_present_docked", z);
        bundle.putByte("key_dock_present_type", b);
        bundle.putByte("key_dock_present_orientation", b2);
        bundle.putBoolean("key_dock_present_get_info", z2);
        bundle.putInt("key_dock_present_id", i);
        return bundle;
    }

    private static Bundle buildReverseStatusBundle(RtxStatusInfo rtxStatusInfo) {
        Bundle bundle = new Bundle();
        bundle.putInt("key_rtx_mode", rtxStatusInfo.mode);
        bundle.putInt("key_accessory_type", rtxStatusInfo.acctype);
        bundle.putBoolean("key_rtx_connection", rtxStatusInfo.chg_s);
        bundle.putInt("key_rtx_iout", rtxStatusInfo.iout);
        bundle.putInt("key_rtx_vout", rtxStatusInfo.vout);
        bundle.putInt("key_rtx_level", rtxStatusInfo.level);
        bundle.putInt("key_reason_type", rtxStatusInfo.reason);
        return bundle;
    }

    public void serviceDied(long j) {
        Log.i("ReverseWirelessCharger", "serviceDied");
        mWirelessCharger = null;
    }

    private void initHALInterface() {
        if (mWirelessCharger == null) {
            try {
                mWirelessCharger = IWirelessCharger.getService();
                mWirelessCharger.linkToDeath(this, 0L);
                mWirelessCharger.registerRtxCallback(this);
            } catch (Exception e) {
                Log.i("ReverseWirelessCharger", "no wireless charger hal found: " + e.getMessage(), e);
                mWirelessCharger = null;
            }
        }
    }

    public boolean isRtxSupported() {
        initHALInterface();
        if (mWirelessCharger != null) {
            try {
                return mWirelessCharger.isRtxSupported();
            } catch (Exception e) {
                Log.i("ReverseWirelessCharger", "isRtxSupported fail: ", e);
            }
        }
        return false;
    }

    public void addIsDockPresentCallback(IsDockPresentCallback isDockPresentCallback) {
        synchronized (mLock) {
            mIsDockPresentCallbacks.add(isDockPresentCallback);
        }
    }

    public void addIsDockPresentChangeListener(IsDockPresentChangeListener isDockPresentChangeListener) {
        addIsDockPresentCallback(isDockPresentChangeListener);
    }

    private void dispatchIsDockPresentCallbacks(boolean z, byte b, byte b2, boolean z2, int i) {
        ArrayList arrayList;
        synchronized (mLock) {
            arrayList = new ArrayList(mIsDockPresentCallbacks);
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            ((IsDockPresentCallback) it.next()).onIsDockPresentChanged(z, b, b2, z2, i);
        }
    }

    public void getRtxInformation() {
        initHALInterface();
        if (mWirelessCharger != null) {
            try {
                mWirelessCharger.getRtxInformation(mLocalRtxInformationCallback);
            } catch (Exception e) {
                Log.i("ReverseWirelessCharger", "getRtxInformation fail: ", e);
            }
        }
    }

    public void addRtxInformationCallback(RtxInformationCallback rtxInformationCallback) {
        synchronized (mLock) {
            mRtxInformationCallbacks.add(rtxInformationCallback);
        }
    }

    public void addReverseChargingInformationChangeListener(ReverseChargingInformationChangeListener reverseChargingInformationChangeListener) {
        addRtxInformationCallback(reverseChargingInformationChangeListener);
    }

    private void dispatchRtxInformationCallbacks(RtxStatusInfo rtxStatusInfo) {
        ArrayList arrayList;
        synchronized (mLock) {
            arrayList = new ArrayList(mRtxInformationCallbacks);
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            ((RtxInformationCallback) it.next()).onRtxInformationChanged(rtxStatusInfo);
        }
    }

    public void setRtxMode(boolean z) {
        initHALInterface();
        if (mWirelessCharger != null) {
            try {
                mWirelessCharger.setRtxMode(z);
            } catch (Exception e) {
                Log.i("ReverseWirelessCharger", "setRtxMode fail: ", e);
            }
        }
    }

    public void addRtxStatusCallback(RtxStatusCallback rtxStatusCallback) {
        synchronized (mLock) {
            mRtxStatusCallbacks.add(rtxStatusCallback);
        }
    }

    public void addReverseChargingChangeListener(ReverseChargingChangeListener reverseChargingChangeListener) {
        addRtxStatusCallback(reverseChargingChangeListener);
    }

    private void dispatchRtxStatusCallbacks(RtxStatusInfo rtxStatusInfo) {
        ArrayList arrayList;
        synchronized (mLock) {
            arrayList = new ArrayList(mRtxStatusCallbacks);
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            ((RtxStatusCallback) it.next()).onRtxStatusChanged(rtxStatusInfo);
        }
    }

    @Override
    public void rtxStatusInfoChanged(RtxStatusInfo rtxStatusInfo) throws RemoteException {
        dispatchRtxStatusCallbacks(rtxStatusInfo);
    }

    public interface IsDockPresentChangeListener extends IsDockPresentCallback {
        void onDockPresentChanged(Bundle bundle);

        @Override
        default void onIsDockPresentChanged(boolean z, byte b, byte b2, boolean z2, int i) {
            if (DEBUG) {
                Log.d("ReverseWirelessCharger", "onIsDockPresentChanged(): docked=" + (z ? 1 : 0) + " type=" + ((int) b) + " orient=" + ((int) b2) + " isGetI=" + (z2 ? 1 : 0) + " id=" + i);
            }
            onDockPresentChanged(buildDockPresentBundle(z, b, b2, z2, i));
        }
    }

    public interface ReverseChargingInformationChangeListener extends RtxInformationCallback {
        void onReverseInformationChanged(Bundle bundle);

        @Override
        default void onRtxInformationChanged(RtxStatusInfo rtxStatusInfo) {
            if (DEBUG) {
                Log.d("ReverseWirelessCharger", "onRtxInformationChanged() RtxStatusInfo : " + rtxStatusInfo.toString());
            }
            onReverseInformationChanged(buildReverseStatusBundle(rtxStatusInfo));
        }
    }

    public interface ReverseChargingChangeListener extends RtxStatusCallback {
        void onReverseStatusChanged(Bundle bundle);

        @Override
        default void onRtxStatusChanged(RtxStatusInfo rtxStatusInfo) {
            if (DEBUG) {
                Log.d("ReverseWirelessCharger", "onRtxStatusChanged() RtxStatusInfo : " + rtxStatusInfo.toString());
            }
            onReverseStatusChanged(buildReverseStatusBundle(rtxStatusInfo));
        }
    }

    class LocalIsDockPresentCallback implements vendor.google.wireless_charger.V1_0.IWirelessCharger.isDockPresentCallback {
        LocalIsDockPresentCallback() {
        }

        @Override
        public void onValues(boolean z, byte b, byte b2, boolean z2, int i) {
            if (DEBUG) {
                Log.d("ReverseWirelessCharger", "LocalIsDockPresentCallback::onValues(): docked=" + (z ? 1 : 0) + " type=" + ((int) b) + " orient=" + ((int) b2) + " isGetI=" + (z2 ? 1 : 0) + " id=" + i);
            }
            dispatchIsDockPresentCallbacks(z, b, b2, z2, i);
        }
    }

    class LocalRtxInformationCallback implements IWirelessCharger.getRtxInformationCallback {
        LocalRtxInformationCallback() {
        }

        @Override
        public void onValues(byte b, RtxStatusInfo rtxStatusInfo) {
            dispatchRtxInformationCallbacks(rtxStatusInfo);
        }
    }
}
