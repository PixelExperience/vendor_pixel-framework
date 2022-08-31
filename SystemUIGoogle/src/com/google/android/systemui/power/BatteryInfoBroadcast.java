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

package com.google.android.systemui.power;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.UserHandle;
import android.util.Log;

class BatteryInfoBroadcast {
    private final Context mContext;
    private final PowerManager mPowerManager;

    BatteryInfoBroadcast(Context context) {
        mContext = context;
        mPowerManager = context.getSystemService(PowerManager.class);
    }

    private static Intent createIntent(String str) {
        return new Intent(str).setPackage("com.google.android.settings.intelligence");
    }

    void notifyBatteryStatusChanged(Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.intent.action.BATTERY_CHANGED") || action.equals("android.os.action.POWER_SAVE_MODE_CHANGED")) {
            Intent newIntent = createIntent("PNW.batteryStatusChanged");
            if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                newIntent.putExtra("battery_changed_intent", intent);
            }
            boolean isPowerSaveMode = mPowerManager.isPowerSaveMode();
            newIntent.putExtra("battery_save", isPowerSaveMode);
            mContext.sendBroadcastAsUser(newIntent, UserHandle.ALL);
            Log.d("BatteryInfoBroadcast", "onReceive: " + action + " isPowerSaveMode: " + isPowerSaveMode);
        } else if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED") || action.equals("android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED") || action.equals("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED") || action.equals("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED") || action.equals("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED") || action.equals("android.bluetooth.device.action.ALIAS_CHANGED") || action.equals("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")) {
            Intent newIntent = createIntent("PNW.bluetoothStatusChanged");
            newIntent.putExtra(action, intent);
            mContext.sendBroadcastAsUser(newIntent, UserHandle.ALL);
        }
    }
}
