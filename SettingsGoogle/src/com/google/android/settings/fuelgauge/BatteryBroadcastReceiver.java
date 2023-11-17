package com.google.android.settings.fuelgauge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/* loaded from: classes2.dex */
public final class BatteryBroadcastReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        Log.d("BatteryBroadcastReceiver", "onReceive:" + intent.getAction());
        String action = intent.getAction();
        action.hashCode();
        if (action.equals("settings.intelligence.battery.action.FETCH_BLUETOOTH_BATTERY_DATA")) {
            try {
                BluetoothBatteryMetadataFetcher.returnBluetoothDevices(context, intent);
            } catch (Exception e) {
                Log.e("BatteryBroadcastReceiver", "returnBluetoothDevices() error", e);
            }
        }
    }
}
