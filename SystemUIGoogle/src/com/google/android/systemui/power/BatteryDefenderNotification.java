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

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.UiEventLogger;
import com.android.systemui.R;
import com.android.systemui.util.NotificationChannels;

import java.text.NumberFormat;
import java.time.Clock;
import java.util.NoSuchElementException;

import vendor.google.google_battery.IGoogleBattery;

class BatteryDefenderNotification {

    private static final String TAG = "BatteryDefenderNotification";

    private final Context mContext;
    private final NotificationManager mNotificationManager;
    private final UiEventLogger mUiEventLogger;
    @VisibleForTesting
    boolean mDefenderEnabled;
    @VisibleForTesting
    boolean mPostNotificationVisible;
    @VisibleForTesting
    boolean mPrvPluggedState;
    @VisibleForTesting
    boolean mRunBypassActionTask = true;
    private int mBatteryLevel;
    private SharedPreferences mSharedPreferences;
    private boolean mHasSystemFeature = false;

    BatteryDefenderNotification(Context context, UiEventLogger uiEventLogger) {
        mContext = context;
        mUiEventLogger = uiEventLogger;
        mNotificationManager = context.getSystemService(NotificationManager.class);
        mHasSystemFeature = mContext.getPackageManager().hasSystemFeature("com.google.android.feature.ADAPTIVE_CHARGING");
    }

    void dispatchIntent(Intent intent) {
        String action = intent.getAction();
        if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
            resolveBatteryChangedIntent(intent);
        } else if ("PNW.defenderResumeCharging".equals(action)) {
            resumeCharging(BatteryDefenderEvent.BATTERY_DEFENDER_BYPASS_LIMIT);
        } else if ("PNW.defenderResumeCharging.settings".equals(action)) {
            resumeCharging(BatteryDefenderEvent.BATTERY_DEFENDER_BYPASS_LIMIT_FOR_TIPS);
        }
    }

    private void resolveBatteryChangedIntent(Intent intent) {
        mBatteryLevel = PowerUtils.getBatteryLevel(intent);
        boolean z = false;
        boolean z2 = intent.getIntExtra("plugged", 0) != 0;
        if (intent.getIntExtra("health", 1) == 3) {
            z = true;
        }
        boolean isFullyCharged = PowerUtils.isFullyCharged(intent);
        Log.d(TAG, "isPlugged: " + z2 + " | isOverheated: " + z + " | defenderEnabled: " + mDefenderEnabled + " | isCharged: " + isFullyCharged);
        if (isFullyCharged && mPostNotificationVisible) {
            cancelPostNotification();
        }
        if (z) {
            sendNotification(z2);
        } else if (mDefenderEnabled) {
            cancelNotificationAndSendPostNotification();
        }
    }

    private void sendNotification(boolean z) {
        if (!mDefenderEnabled) {
            if (mPostNotificationVisible) {
                cancelPostNotification();
            }
            getSharedPreferences().edit().putLong("trigger_time", Clock.systemUTC().millis()).apply();
        }
        if (!mDefenderEnabled || mPrvPluggedState != z) {
            mPrvPluggedState = z;
            sendNotificationInternal(z);
        }
    }

    private void sendNotificationInternal(boolean z) {
        NotificationCompat.Builder addAction = new NotificationCompat.Builder(mContext, NotificationChannels.BATTERY).setSmallIcon(17303566).setContentTitle(mContext.getString(R.string.defender_notify_title)).setContentText(mContext.getString(R.string.defender_notify_des)).addAction(0, mContext.getString(R.string.battery_health_notify_learn_more), PowerUtils.createHelpArticlePendingIntent(mContext, R.string.defender_notify_help_url));
        if (z) {
            addAction.addAction(0, mContext.getString(R.string.defender_notify_resume_charge), PowerUtils.createNormalChargingIntent(mContext, "PNW.defenderResumeCharging"));
        }
        PowerUtils.overrideNotificationAppName(mContext, addAction, 17040783);
        mNotificationManager.notifyAsUser("battery_defender", PowerUtils.NOTIFICATION_ID, addAction.build(), UserHandle.ALL);
        if (!mDefenderEnabled) {
            mDefenderEnabled = true;
            if (mUiEventLogger == null) {
                return;
            }
            mUiEventLogger.log(BatteryDefenderEvent.BATTERY_DEFENDER_NOTIFICATION);
        }
    }

    private void cancelNotificationAndSendPostNotification() {
        cancelNotification();
        if (getSharedPreferences().contains("trigger_time")) {
            sendPostNotification();
        }
    }

    private void sendPostNotification() {
        long j = getSharedPreferences().getLong("trigger_time", -1L);
        String currentTime = j > 0 ? PowerUtils.getCurrentTime(mContext, j) : null;
        if (currentTime != null && PowerUtils.postNotificationThreshold(j)) {
            NotificationCompat.Builder addAction = new NotificationCompat.Builder(mContext, NotificationChannels.BATTERY).setSmallIcon(17303566).setContentTitle(mContext.getString(R.string.defender_post_notify_title)).setContentText(mContext.getString(R.string.defender_post_notify_des, NumberFormat.getPercentInstance().format(0.800000011920929d), currentTime, PowerUtils.getCurrentTime(mContext, Clock.systemUTC().millis()))).addAction(0, mContext.getString(R.string.battery_health_notify_learn_more), PowerUtils.createHelpArticlePendingIntent(mContext, R.string.defender_notify_help_url));
            PowerUtils.overrideNotificationAppName(mContext, addAction, 17040783);
            mNotificationManager.notifyAsUser("battery_defender", PowerUtils.POST_NOTIFICATION_ID, addAction.build(), UserHandle.ALL);
            mPostNotificationVisible = true;
        } else {
            Log.w(TAG, "error getting trigger time");
        }
        clearDefenderStartRecord();
    }

    private void resumeCharging(BatteryDefenderEvent batteryDefenderEvent) {
        if (mUiEventLogger != null) {
            mUiEventLogger.logWithPosition(batteryDefenderEvent, 0, (String) null, mBatteryLevel);
        }
        Log.d(TAG, "resume charging: " + batteryDefenderEvent.mId);
        executeBypassActionWithAsync();
        mNotificationManager.cancelAsUser("battery_defender", PowerUtils.NOTIFICATION_ID, UserHandle.ALL);
        clearDefenderStartRecord();
    }

    private void executeBypassActionWithAsync() {
        if (!mRunBypassActionTask) {
            return;
        }
        AsyncTask.execute(() -> {
            IBinder.DeathRecipient cbRecipient = new IBinder.DeathRecipient() {
                @Override
                public final void binderDied() {
                    Log.d(TAG, "serviceDied");
                }
            };
            if(!mHasSystemFeature) {
                Log.d(TAG, "Device does not support Google Battery");
                return;
            }
            IGoogleBattery initHalInterface = initHalInterface(cbRecipient);
            if (initHalInterface == null) {
                Log.d(TAG, "Can not init hal interface");
            }
            try {
                try {
                    initHalInterface.setProperty(2, 17, 1);
                    initHalInterface.setProperty(3, 17, 1);
                    initHalInterface.setProperty(1, 17, 1);
                } catch (RemoteException e) {
                    Log.e(TAG, "setProperty error: " + e);
                }
            } finally {
                destroyHalInterface(initHalInterface, cbRecipient);
            }
        });
    }

    private static IGoogleBattery initHalInterface(IBinder.DeathRecipient deathReceiver) {
        Log.d(TAG, "initHalInterface");
        try {
            IBinder binder = Binder.allowBlocking(ServiceManager.waitForDeclaredService("vendor.google.google_battery.IGoogleBattery/default"));
            IGoogleBattery batteryInterface = null;
            if (binder != null) {
                batteryInterface = IGoogleBattery.Stub.asInterface(binder);
                if (batteryInterface != null && deathReceiver != null) {
                    binder.linkToDeath(deathReceiver, 0);
                }
            }
            return batteryInterface;
        } catch (RemoteException | NoSuchElementException | SecurityException e) {
            Log.e(TAG, "failed to get Google Battery HAL: ", e);
            return null;
        }
    }

    private static void destroyHalInterface(IGoogleBattery iGoogleBattery, IBinder.DeathRecipient deathRecipient) {
        Log.d(TAG, "destroyHalInterface");
        if (deathRecipient != null && iGoogleBattery != null) {
            iGoogleBattery.asBinder().unlinkToDeath(deathRecipient, 0);
        }
    }

    private SharedPreferences getSharedPreferences() {
        if (mSharedPreferences == null) {
            mSharedPreferences = mContext.getApplicationContext().getSharedPreferences("defender_shared_prefs", 0);
        }
        return mSharedPreferences;
    }

    private void cancelNotification() {
        mDefenderEnabled = false;
        mNotificationManager.cancelAsUser("battery_defender", PowerUtils.NOTIFICATION_ID, UserHandle.ALL);
    }

    private void cancelPostNotification() {
        mPostNotificationVisible = false;
        clearDefenderStartRecord();
        mNotificationManager.cancelAsUser("battery_defender", PowerUtils.POST_NOTIFICATION_ID, UserHandle.ALL);
    }

    private void clearDefenderStartRecord() {
        getSharedPreferences().edit().clear().apply();
    }

    enum BatteryDefenderEvent implements UiEventLogger.UiEventEnum {
        BATTERY_DEFENDER_NOTIFICATION(876),
        BATTERY_DEFENDER_BYPASS_LIMIT(877),
        BATTERY_DEFENDER_BYPASS_LIMIT_FOR_TIPS(878);

        private final int mId;

        BatteryDefenderEvent(int i) {
            mId = i;
        }

        public int getId() {
            return mId;
        }
    }
}
