package com.google.android.systemui.googlebattery;

import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import java.util.NoSuchElementException;
import vendor.google.google_battery.IGoogleBattery;

public final class GoogleBatteryManager {
    public static final boolean DEBUG = Log.isLoggable("GoogleBatteryManager", 3);

    public static void destroyHalInterface(IGoogleBattery iGoogleBattery, IBinder.DeathRecipient deathRecipient) {
        if (DEBUG) {
            Log.d("GoogleBatteryManager", "destroyHalInterface");
        }
        if (deathRecipient != null && iGoogleBattery != null) {
            iGoogleBattery.asBinder().unlinkToDeath(deathRecipient, 0);
        }
    }

    public static IGoogleBattery initHalInterface(IBinder.DeathRecipient deathRecipient) {
        if (DEBUG) {
            Log.d("GoogleBatteryManager", "initHalInterface");
        }
        try {
            IBinder allowBlocking = Binder.allowBlocking(ServiceManager.waitForDeclaredService("vendor.google.google_battery.IGoogleBattery/default"));
            if (allowBlocking == null) {
                return null;
            }
            IGoogleBattery asInterface = IGoogleBattery.Stub.asInterface(allowBlocking);
            if (asInterface != null && deathRecipient != null) {
                allowBlocking.linkToDeath(deathRecipient, 0);
            }
            return asInterface;
        } catch (RemoteException | SecurityException | NoSuchElementException e) {
            Log.e("GoogleBatteryManager", "failed to get Google Battery HAL: ", e);
            return null;
        }
    }
}