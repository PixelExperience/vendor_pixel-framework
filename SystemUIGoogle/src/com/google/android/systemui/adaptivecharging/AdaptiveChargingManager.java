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

package com.google.android.systemui.adaptivecharging;

import android.content.Context;
import android.os.IHwBinder;
import android.os.LocaleList;
import android.os.RemoteException;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;

import java.util.Locale;
import java.util.NoSuchElementException;

import vendor.google.google_battery.V1_1.IGoogleBattery;

public class AdaptiveChargingManager {
    private static final boolean DEBUG = Log.isLoggable("AdaptiveChargingManager", 3);
    Context mContext;

    public AdaptiveChargingManager(Context context) {
        mContext = context;
    }

    private static IGoogleBattery initHalInterface(IHwBinder.DeathRecipient deathRecipient) {
        if (DEBUG) {
            Log.d("AdaptiveChargingManager", "initHalInterface");
        }
        try {
            IGoogleBattery service = IGoogleBattery.getService();
            if (service != null && deathRecipient != null) {
                service.linkToDeath(deathRecipient, 0L);
            }
            return service;
        } catch (RemoteException | NoSuchElementException e) {
            Log.e("AdaptiveChargingManager", "failed to get Google Battery HAL: ", e);
            return null;
        }
    }

    public static boolean isStageActive(String str) {
        return "Active".equals(str);
    }

    public static boolean isStageEnabled(String str) {
        return "Enabled".equals(str);
    }

    public static boolean isStageActiveOrEnabled(String str) {
        return isStageActive(str) || isStageEnabled(str);
    }

    public static boolean isActive(String str, int i) {
        return isStageActiveOrEnabled(str) && i > 0;
    }

    private void destroyHalInterface(IGoogleBattery iGoogleBattery, IHwBinder.DeathRecipient deathRecipient) {
        if (DEBUG) {
            Log.d("AdaptiveChargingManager", "destroyHalInterface");
        }
        if (deathRecipient != null) {
            try {
                iGoogleBattery.unlinkToDeath(deathRecipient);
            } catch (RemoteException e) {
                Log.e("AdaptiveChargingManager", "unlinkToDeath failed: ", e);
            }
        }
    }

    boolean hasAdaptiveChargingFeature() {
        return mContext.getPackageManager().hasSystemFeature("com.google.android.feature.ADAPTIVE_CHARGING");
    }

    public boolean isAvailable() {
        return hasAdaptiveChargingFeature() && DeviceConfig.getBoolean("adaptive_charging", "adaptive_charging_enabled", true);
    }

    public boolean shouldShowNotification() {
        return DeviceConfig.getBoolean("adaptive_charging", "adaptive_charging_notification", false);
    }

    public boolean isEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(), "adaptive_charging_enabled", 1) == 1;
    }

    private Locale getLocale() {
        LocaleList locales = mContext.getResources().getConfiguration().getLocales();
        return (locales == null || locales.isEmpty()) ? Locale.getDefault() : locales.get(0);
    }

    public String formatTimeToFull(long j) {
        return DateFormat.format(DateFormat.getBestDateTimePattern(getLocale(), DateFormat.is24HourFormat(mContext) ? "Hm" : "hma"), j).toString();
    }

    public boolean setAdaptiveChargingDeadline(int i) {
        IGoogleBattery initHalInterface = initHalInterface(null);
        boolean z = false;
        if (initHalInterface == null) {
            return false;
        }
        try {
            if (initHalInterface.setChargingDeadline(i) == 0) {
                z = true;
            }
        } catch (RemoteException e) {
            Log.e("AdaptiveChargingManager", "setChargingDeadline failed: ", e);
        }
        destroyHalInterface(initHalInterface, null);
        return z;
    }

    public void queryStatus(final AdaptiveChargingStatusReceiver adaptiveChargingStatusReceiver) {
        final IHwBinder.DeathRecipient deathRecipient = new IHwBinder.DeathRecipient() {
            public void serviceDied(long j) {
                if (AdaptiveChargingManager.DEBUG) {
                    Log.d("AdaptiveChargingManager", "serviceDied");
                }
                adaptiveChargingStatusReceiver.onDestroyInterface();
            }
        };
        final IGoogleBattery initHalInterface = initHalInterface(deathRecipient);
        if (initHalInterface == null) {
            adaptiveChargingStatusReceiver.onDestroyInterface();
            return;
        }
        try {
            initHalInterface.getChargingStageAndDeadline(new IGoogleBattery.getChargingStageAndDeadlineCallback() {
                @Override
                public void onValues(byte b, String str, int i) {
                    if (AdaptiveChargingManager.DEBUG) {
                        Log.d("AdaptiveChargingManager", "getChargingStageDeadlineCallback result: " + ((int) b) + ", stage: \"" + str + "\", seconds: " + i);
                    }
                    if (b == 0) {
                        adaptiveChargingStatusReceiver.onReceiveStatus(str, i);
                    }
                    destroyHalInterface(initHalInterface, deathRecipient);
                    adaptiveChargingStatusReceiver.onDestroyInterface();
                }
            });
        } catch (RemoteException e) {
            Log.e("AdaptiveChargingManager", "Failed to get Adaptive Chaging status: ", e);
            destroyHalInterface(initHalInterface, deathRecipient);
            adaptiveChargingStatusReceiver.onDestroyInterface();
        }
    }

    public interface AdaptiveChargingStatusReceiver {
        void onDestroyInterface();

        void onReceiveStatus(String str, int i);
    }
}
