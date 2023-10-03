/*
 * Copyright (C) 2022 The PixelExperience Project
 *               2023 RisingTechOSS
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

package com.google.android.systemui.power;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.broadcast.BroadcastSender;

class BatteryInfoBroadcast {
    @VisibleForTesting
    private static long sBroadcastDelayFromBoot = 2400000;
    private final BroadcastSender mBroadcastSender;
    @VisibleForTesting
    private final ContentObserver mContentObserver;
    private final Context mContext;
    @VisibleForTesting
    private final BluetoothAdapter.OnMetadataChangedListener mMetadataListener;
    private final PowerManager mPowerManager;
    @VisibleForTesting
    private long mLastFullChargeHour = -1;
    private final Handler mHandler = new Handler();

    BatteryInfoBroadcast(Context context, BroadcastSender broadcastSender) {
        mContext = context;
        mPowerManager = context.getSystemService(PowerManager.class);
        ContentObserver contentObserver = new ContentObserver(mHandler) {
            @Override
            public final void onChange(boolean selfChange) {
                sendBroadcast(createIntent("PNW.batteryStatusChanged"));
            }
        };
        mContentObserver = contentObserver;
        mMetadataListener = new BluetoothAdapter.OnMetadataChangedListener() {
            public final void onMetadataChanged(BluetoothDevice bluetoothDevice, int i, byte[] bArr) {
                sendBroadcast(createIntent("PNW.bluetoothStatusChanged"));
            }
        };
        mBroadcastSender = broadcastSender;
    }

    private static Intent createIntent(String str) {
        return new Intent(str).setPackage("com.google.android.settings.intelligence");
    }

    @VisibleForTesting
    public boolean isInTheDifferentInterval(long j) {
        long j2 = mLastFullChargeHour;
        if (j2 != -1 && j2 == j) {
            return false;
        }
        return true;
    }

    public void sendBatteryChangeIntent(Intent intent, String str) {
        // log possible intents sent from ASI etc.
        if (!str.equals("notifyBatteryStatusChanged")) {
            Log.d("sendBatteryChangeIntent", "Intent: " + intent + " Intent string: " + str);
        }
        String action = intent.getAction();
        if (action.equals("android.intent.action.BATTERY_CHANGED") || action.equals("android.os.action.POWER_SAVE_MODE_CHANGED")) {
            Intent createIntent = createIntent("PNW.batteryStatusChanged");
            if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                createIntent.putExtra("battery_changed_intent", intent);
            }
            boolean z = true;
            int intExtra = intent.getIntExtra("status", 1);
            int intExtra2 = intent.getIntExtra("level", 0);
            if (intExtra != 5 && intExtra2 < 100) {
                z = false;
            }
            if (z) {
                long elapsedRealtime = sBroadcastDelayFromBoot - SystemClock.elapsedRealtime();
                if (elapsedRealtime > 0) {
                    Log.d("BatteryInfoBroadcast", "cancel sendBroadcastToFetchUsageData when broadcastDelay is" + elapsedRealtime + "ms.");
                } else {
                    long currentTimeMillis = System.currentTimeMillis() / 3600000;
                    if (isInTheDifferentInterval(currentTimeMillis)) {
                        sendBroadcast(new Intent("settings.intelligence.battery.action.FETCH_BATTERY_USAGE_DATA").setComponent(new ComponentName("com.android.settings", "com.google.android.settings.fuelgauge.BatteryBroadcastReceiver")));
                        mLastFullChargeHour = currentTimeMillis;
                        Log.d("BatteryInfoBroadcast", "Fetch battery usage data for full charge status.");
                    }
                }
            }
            boolean isPowerSaveMode = mPowerManager.isPowerSaveMode();
            createIntent.putExtra("battery_save", isPowerSaveMode);
            sendBroadcast(createIntent);
            Log.d("BatteryInfoBroadcast", "onReceive: " + action + " isPowerSaveMode: " + isPowerSaveMode);
        } else if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED") 
            || action.equals("android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED") 
            || action.equals("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED") 
            || action.equals("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED") 
            || action.equals("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED") 
            || action.equals("android.bluetooth.device.action.ALIAS_CHANGED") 
            || action.equals("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")) {
                Intent btStatusChangeIntent = createIntent("PNW.bluetoothStatusChanged");
                btStatusChangeIntent.putExtra(action, intent);
                sendBroadcast(btStatusChangeIntent);
        }
    }

    public void sendBroadcast(Intent intent) {
        BroadcastSender broadcastSender = mBroadcastSender;
        if (broadcastSender != null && intent != null) {
            broadcastSender.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    void notifyBatteryStatusChanged(Intent intent) {
        sendBatteryChangeIntent(intent, "notifyBatteryStatusChanged");
    }
}
