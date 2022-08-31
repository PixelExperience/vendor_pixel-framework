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

import android.os.Bundle;
import android.os.Handler;
import android.os.IHwBinder;
import android.os.Looper;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.google.android.systemui.dreamliner.WirelessCharger;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import vendor.google.wireless_charger.V1_0.KeyExchangeResponse;
import vendor.google.wireless_charger.V1_1.AlignInfo;
import vendor.google.wireless_charger.V1_1.IWirelessChargerInfoCallback;
import vendor.google.wireless_charger.V1_3.FanDetailedInfo;
import vendor.google.wireless_charger.V1_3.FanInfo;
import vendor.google.wireless_charger.V1_3.IWirelessCharger;

public class WirelessChargerImpl extends WirelessCharger implements IHwBinder.DeathRecipient, IWirelessCharger.isDockPresentCallback {
    private static final boolean DEBUG = Log.isLoggable("Dreamliner-WLC_HAL", 3);
    private static final long MAX_POLLING_TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private IWirelessCharger.isDockPresentCallback mCallback;
    private long mPollingStartedTimeNs;
    private vendor.google.wireless_charger.V1_3.IWirelessCharger mWirelessCharger;
    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            isDockPresentInternal(mCallback);
        }
    };

    private static Bundle convertFanInfo(byte b, FanInfo fanInfo) {
        Bundle bundle = new Bundle();
        bundle.putByte("fan_id", b);
        bundle.putByte("fan_mode", fanInfo.fanMode);
        bundle.putInt("fan_current_rpm", fanInfo.currentRpm);
        return bundle;
    }

    private static Bundle convertFanDetailedInfo(byte b, FanDetailedInfo fanDetailedInfo) {
        Bundle bundle = new Bundle();
        bundle.putByte("fan_id", b);
        bundle.putByte("fan_mode", fanDetailedInfo.fanMode);
        bundle.putInt("fan_current_rpm", fanDetailedInfo.currentRpm);
        bundle.putInt("fan_min_rpm", fanDetailedInfo.minimumRpm);
        bundle.putInt("fan_max_rpm", fanDetailedInfo.maximumRpm);
        bundle.putByte("fan_type", fanDetailedInfo.type);
        bundle.putByte("fan_count", fanDetailedInfo.count);
        return bundle;
    }

    @Override
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void asyncIsDockPresent(WirelessCharger.IsDockPresentCallback isDockPresentCallback) {
        initHALInterface();
        if (mWirelessCharger != null) {
            mPollingStartedTimeNs = System.nanoTime();
            mCallback = new IsDockPresentCallbackWrapper(isDockPresentCallback);
            mHandler.removeCallbacks(mRunnable);
            mHandler.postDelayed(mRunnable, 100L);
        }
    }

    @Override
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void getInformation(WirelessCharger.GetInformationCallback getInformationCallback) {
        initHALInterface();
        if (mWirelessCharger != null) {
            try {
                mWirelessCharger.getInformation(new GetInformationCallbackWrapper(getInformationCallback));
            } catch (Exception e) {
                Log.i("Dreamliner-WLC_HAL", "getInformation fail: " + e.getMessage());
            }
        }
    }

    @Override
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void keyExchange(byte[] bArr, WirelessCharger.KeyExchangeCallback keyExchangeCallback) {
        initHALInterface();
        if (mWirelessCharger != null) {
            try {
                mWirelessCharger.keyExchange(convertPrimitiveArrayToArrayList(bArr), new KeyExchangeCallbackWrapper(keyExchangeCallback));
            } catch (Exception e) {
                Log.i("Dreamliner-WLC_HAL", "keyExchange fail: " + e.getMessage());
            }
        }
    }

    @Override
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void challenge(byte b, byte[] bArr, WirelessCharger.ChallengeCallback challengeCallback) {
        initHALInterface();
        if (mWirelessCharger != null) {
            try {
                mWirelessCharger.challenge(b, convertPrimitiveArrayToArrayList(bArr), new ChallengeCallbackWrapper(challengeCallback));
            } catch (Exception e) {
                Log.i("Dreamliner-WLC_HAL", "challenge fail: " + e.getMessage());
            }
        }
    }

    @Override
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void registerAlignInfo(WirelessCharger.AlignInfoListener alignInfoListener) {
        initHALInterface();
        if (mWirelessCharger != null) {
            try {
                mWirelessCharger.registerCallback(new WirelessChargerInfoCallback(alignInfoListener));
            } catch (Exception e) {
                Log.i("Dreamliner-WLC_HAL", "register alignInfo callback fail: " + e.getMessage());
            }
        }
    }

    @Override
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void getFanSimpleInformation(byte b, WirelessCharger.GetFanSimpleInformationCallback getFanSimpleInformationCallback) {
        initHALInterface();
        Log.d("Dreamliner-WLC_HAL", "command=3");
        if (mWirelessCharger != null) {
            try {
                mWirelessCharger.getFan(b, new GetFanSimpleInformationCallbackWrapper(b, getFanSimpleInformationCallback));
            } catch (Exception e) {
                Log.i("Dreamliner-WLC_HAL", "command=3 fail: " + e.getMessage());
            }
        }
    }

    @Override
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void getFanInformation(byte b, WirelessCharger.GetFanInformationCallback getFanInformationCallback) {
        initHALInterface();
        Log.d("Dreamliner-WLC_HAL", "command=0");
        if (mWirelessCharger != null) {
            try {
                mWirelessCharger.getFanInformation(b, new GetFanInformationCallbackWrapper(b, getFanInformationCallback));
            } catch (Exception e) {
                Log.i("Dreamliner-WLC_HAL", "command=0 fail: " + e.getMessage());
            }
        }
    }

    @Override
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void setFan(byte b, byte b2, int i, WirelessCharger.SetFanCallback setFanCallback) {
        initHALInterface();
        Log.d("Dreamliner-WLC_HAL", "command=1, i=" + ((int) b) + ", m=" + ((int) b2) + ", r=" + i);
        if (mWirelessCharger != null) {
            try {
                long currentTimeMillis = System.currentTimeMillis();
                mWirelessCharger.setFan(b, b2, (short) i, new SetFanCallbackWrapper(b, setFanCallback));
                if (!DEBUG) {
                    return;
                }
                Log.d("Dreamliner-WLC_HAL", "command=1 spending time: " + (System.currentTimeMillis() - currentTimeMillis));
            } catch (Exception e) {
                Log.i("Dreamliner-WLC_HAL", "command=1 fail: " + e.getMessage());
            }
        }
    }

    @Override
    public void getWpcAuthDigests(byte b, WirelessCharger.GetWpcAuthDigestsCallback getWpcAuthDigestsCallback) {
        initHALInterface();
        if (mWirelessCharger != null) {
            try {
                mWirelessCharger.getWpcAuthDigests(b, new GetWpcAuthDigestsCallbackWrapper(getWpcAuthDigestsCallback));
            } catch (Exception e) {
                Log.i("Dreamliner-WLC_HAL", "get wpc digests fail: " + e.getMessage());
            }
        }
    }

    @Override
    public void getWpcAuthCertificate(byte b, short s, short s2, WirelessCharger.GetWpcAuthCertificateCallback getWpcAuthCertificateCallback) {
        initHALInterface();
        if (mWirelessCharger != null) {
            try {
                mWirelessCharger.getWpcAuthCertificate(b, s, s2, new GetWpcAuthCertificateCallbackWrapper(getWpcAuthCertificateCallback));
            } catch (Exception e) {
                Log.i("Dreamliner-WLC_HAL", "get wpc cert fail: " + e.getMessage());
            }
        }
    }

    @Override
    public void getWpcAuthChallengeResponse(byte b, byte[] bArr, WirelessCharger.GetWpcAuthChallengeResponseCallback getWpcAuthChallengeResponseCallback) {
        initHALInterface();
        if (mWirelessCharger != null) {
            try {
                mWirelessCharger.getWpcAuthChallengeResponse(b, convertPrimitiveArrayToArrayList(bArr), new GetWpcAuthChallengeResponseCallbackWrapper(getWpcAuthChallengeResponseCallback));
            } catch (Exception e) {
                Log.i("Dreamliner-WLC_HAL", "get wpc challenge response fail: " + e.getMessage());
            }
        }
    }

    @Override
    public void setFeatures(long j, long j2, WirelessCharger.SetFeaturesCallback setFeaturesCallback) {
        initHALInterface();
        if (mWirelessCharger != null) {
            try {
                setFeaturesCallback.onCallback(mWirelessCharger.setFeatures(j, j2));
            } catch (Exception e) {
                Log.i("Dreamliner-WLC_HAL", "set features fail: " + e.getMessage());
            }
        }
    }

    @Override
    public void getFeatures(long j, WirelessCharger.GetFeaturesCallback getFeaturesCallback) {
        initHALInterface();
        if (mWirelessCharger != null) {
            try {
                mWirelessCharger.getFeatures(j, new GetFeaturesCallbackWrapper(getFeaturesCallback));
            } catch (Exception e) {
                Log.i("Dreamliner-WLC_HAL", "get features fail: " + e.getMessage());
            }
        }
    }

    @Override
    int getFanLevel() {
        initHALInterface();
        Log.d("Dreamliner-WLC_HAL", "command=2");
        if (mWirelessCharger != null) {
            try {
                return mWirelessCharger.getFanLevel();
            } catch (Exception e) {
                Log.i("Dreamliner-WLC_HAL", "command=2 fail: " + e.getMessage());
                return -1;
            }
        }
        return -1;
    }

    public void serviceDied(long j) {
        Log.i("Dreamliner-WLC_HAL", "serviceDied");
        mWirelessCharger = null;
    }

    private ArrayList<Byte> convertPrimitiveArrayToArrayList(byte[] bArr) {
        if (bArr == null || bArr.length <= 0) {
            return null;
        }
        ArrayList<Byte> arrayList = new ArrayList<>();
        for (byte b : bArr) {
            arrayList.add(b);
        }
        return arrayList;
    }

    private void isDockPresentInternal(IWirelessCharger.isDockPresentCallback callback) {
        initHALInterface();
        if (mWirelessCharger != null) {
            try {
                mWirelessCharger.isDockPresent(callback);
            } catch (Exception e) {
                Log.i("Dreamliner-WLC_HAL", "isDockPresent fail: " + e.getMessage());
            }
        }
    }

    @Override
    public void onValues(boolean z, byte b, byte b2, boolean z2, int i) {
        if (System.nanoTime() < mPollingStartedTimeNs + MAX_POLLING_TIMEOUT_NS && i == 0) {
            mHandler.postDelayed(mRunnable, 100L);
            return;
        }
        if (mCallback == null) {
            return;
        }
        mCallback.onValues(z, b, b2, z2, i);
        mCallback = null;
    }

    private void initHALInterface() {
        if (mWirelessCharger == null) {
            try {
                mWirelessCharger = vendor.google.wireless_charger.V1_3.IWirelessCharger.getService();
                mWirelessCharger.linkToDeath(this, 0L);
            } catch (Exception e) {
                Log.i("Dreamliner-WLC_HAL", "no wireless charger hal found: " + e.getMessage());
                mWirelessCharger = null;
            }
        }
    }

    static final class GetFanSimpleInformationCallbackWrapper implements IWirelessCharger.getFanCallback {
        private final WirelessCharger.GetFanSimpleInformationCallback mCallback;
        private final byte mFanId;

        GetFanSimpleInformationCallbackWrapper(byte b, WirelessCharger.GetFanSimpleInformationCallback getFanSimpleInformationCallback) {
            mFanId = b;
            mCallback = getFanSimpleInformationCallback;
        }

        @Override
        public void onValues(byte b, FanInfo fanInfo) {
            Log.d("Dreamliner-WLC_HAL", "command=3, result=" + Byte.valueOf(b).intValue() + ", i=" + ((int) mFanId) + ", m=" + fanInfo.fanMode + ", cr=" + fanInfo.currentRpm);
            mCallback.onCallback(Byte.valueOf(b).intValue(), WirelessChargerImpl.convertFanInfo(mFanId, fanInfo));
        }
    }

    static final class GetFanInformationCallbackWrapper implements IWirelessCharger.getFanInformationCallback {
        private final WirelessCharger.GetFanInformationCallback mCallback;
        private final byte mFanId;

        public GetFanInformationCallbackWrapper(byte b, WirelessCharger.GetFanInformationCallback getFanInformationCallback) {
            mFanId = b;
            mCallback = getFanInformationCallback;
        }

        @Override
        public void onValues(byte b, FanDetailedInfo fanDetailedInfo) {
            Log.d("Dreamliner-WLC_HAL", "command=0, result=" + Byte.valueOf(b).intValue() + ", i=" + ((int) mFanId) + ", m=" + fanDetailedInfo.fanMode + ", cr=" + fanDetailedInfo.currentRpm + ", mir=" + fanDetailedInfo.minimumRpm + ", mxr=" + fanDetailedInfo.maximumRpm + ", t=" + fanDetailedInfo.type + ", c=" + fanDetailedInfo.count);
            mCallback.onCallback(Byte.valueOf(b).intValue(), WirelessChargerImpl.convertFanDetailedInfo(mFanId, fanDetailedInfo));
        }
    }

    static final class SetFanCallbackWrapper implements IWirelessCharger.setFanCallback {
        private final WirelessCharger.SetFanCallback mCallback;
        private final byte mFanId;

        public SetFanCallbackWrapper(byte b, WirelessCharger.SetFanCallback setFanCallback) {
            mFanId = b;
            mCallback = setFanCallback;
        }

        @Override
        public void onValues(byte b, FanInfo fanInfo) {
            Log.d("Dreamliner-WLC_HAL", "command=1, result=" + Byte.valueOf(b).intValue() + ", i=" + ((int) mFanId) + ", m=" + fanInfo.fanMode + ", cr=" + fanInfo.currentRpm);
            mCallback.onCallback(Byte.valueOf(b).intValue(), WirelessChargerImpl.convertFanInfo(mFanId, fanInfo));
        }
    }

    static final class GetWpcAuthDigestsCallbackWrapper implements IWirelessCharger.getWpcAuthDigestsCallback {
        private final WirelessCharger.GetWpcAuthDigestsCallback mCallback;

        GetWpcAuthDigestsCallbackWrapper(WirelessCharger.GetWpcAuthDigestsCallback getWpcAuthDigestsCallback) {
            mCallback = getWpcAuthDigestsCallback;
        }

        @Override
        public void onValues(byte b, byte b2, byte b3, ArrayList<byte[]> arrayList) {
            mCallback.onCallback(Byte.valueOf(b).intValue(), b2, b3, arrayList);
        }
    }

    static final class GetWpcAuthCertificateCallbackWrapper implements IWirelessCharger.getWpcAuthCertificateCallback {
        private final WirelessCharger.GetWpcAuthCertificateCallback mCallback;

        GetWpcAuthCertificateCallbackWrapper(WirelessCharger.GetWpcAuthCertificateCallback getWpcAuthCertificateCallback) {
            mCallback = getWpcAuthCertificateCallback;
        }

        @Override
        public void onValues(byte b, ArrayList<Byte> arrayList) {
            mCallback.onCallback(Byte.valueOf(b).intValue(), arrayList);
        }
    }

    static final class GetWpcAuthChallengeResponseCallbackWrapper implements IWirelessCharger.getWpcAuthChallengeResponseCallback {
        private final WirelessCharger.GetWpcAuthChallengeResponseCallback mCallback;

        GetWpcAuthChallengeResponseCallbackWrapper(WirelessCharger.GetWpcAuthChallengeResponseCallback getWpcAuthChallengeResponseCallback) {
            mCallback = getWpcAuthChallengeResponseCallback;
        }

        @Override
        public void onValues(byte b, byte b2, byte b3, byte b4, ArrayList<Byte> arrayList, ArrayList<Byte> arrayList2) {
            mCallback.onCallback(Byte.valueOf(b).intValue(), b2, b3, b4, arrayList, arrayList2);
        }
    }

    static final class GetFeaturesCallbackWrapper implements IWirelessCharger.getFeaturesCallback {
        private final WirelessCharger.GetFeaturesCallback mCallback;

        GetFeaturesCallbackWrapper(WirelessCharger.GetFeaturesCallback getFeaturesCallback) {
            mCallback = getFeaturesCallback;
        }

        @Override
        public void onValues(byte b, long j) {
            mCallback.onCallback(Byte.valueOf(b).intValue(), j);
        }
    }

    final class IsDockPresentCallbackWrapper implements IWirelessCharger.isDockPresentCallback {
        private final WirelessCharger.IsDockPresentCallback mCallback;

        public IsDockPresentCallbackWrapper(WirelessCharger.IsDockPresentCallback isDockPresentCallback) {
            mCallback = isDockPresentCallback;
        }

        @Override
        public void onValues(boolean z, byte b, byte b2, boolean z2, int i) {
            mCallback.onCallback(z, b, b2, z2, i);
        }
    }

    final class GetInformationCallbackWrapper implements IWirelessCharger.getInformationCallback {
        private final WirelessCharger.GetInformationCallback mCallback;

        public GetInformationCallbackWrapper(WirelessCharger.GetInformationCallback getInformationCallback) {
            mCallback = getInformationCallback;
        }

        @Override
        public void onValues(byte b, vendor.google.wireless_charger.V1_0.DockInfo dockInfo) {
            mCallback.onCallback(Byte.valueOf(b).intValue(), convertDockInfo(dockInfo));
        }

        private DockInfo convertDockInfo(vendor.google.wireless_charger.V1_0.DockInfo dockInfo) {
            return new DockInfo(dockInfo.manufacturer, dockInfo.model, dockInfo.serial, Byte.valueOf(dockInfo.type).intValue());
        }
    }

    final class KeyExchangeCallbackWrapper implements IWirelessCharger.keyExchangeCallback {
        private final WirelessCharger.KeyExchangeCallback mCallback;

        public KeyExchangeCallbackWrapper(WirelessCharger.KeyExchangeCallback keyExchangeCallback) {
            mCallback = keyExchangeCallback;
        }

        @Override
        public void onValues(byte b, KeyExchangeResponse keyExchangeResponse) {
            if (keyExchangeResponse != null) {
                mCallback.onCallback(Byte.valueOf(b).intValue(), keyExchangeResponse.dockId, keyExchangeResponse.dockPublicKey);
            } else {
                mCallback.onCallback(Byte.valueOf(b).intValue(), (byte) -1, null);
            }
        }
    }

    final class ChallengeCallbackWrapper implements IWirelessCharger.challengeCallback {
        private final WirelessCharger.ChallengeCallback mCallback;

        public ChallengeCallbackWrapper(WirelessCharger.ChallengeCallback challengeCallback) {
            mCallback = challengeCallback;
        }

        @Override
        public void onValues(byte b, ArrayList<Byte> arrayList) {
            mCallback.onCallback(Byte.valueOf(b).intValue(), arrayList);
        }
    }

    final class WirelessChargerInfoCallback extends IWirelessChargerInfoCallback.Stub {
        private final WirelessCharger.AlignInfoListener mListener;

        public WirelessChargerInfoCallback(WirelessCharger.AlignInfoListener alignInfoListener) {
            mListener = alignInfoListener;
        }

        @Override
        public void alignInfoChanged(AlignInfo alignInfo) {
            mListener.onAlignInfoChanged(convertAlignInfo(alignInfo));
        }

        private DockAlignInfo convertAlignInfo(AlignInfo alignInfo) {
            return new DockAlignInfo(Byte.valueOf(alignInfo.alignState).intValue(), Byte.valueOf(alignInfo.alignPct).intValue());
        }
    }
}
