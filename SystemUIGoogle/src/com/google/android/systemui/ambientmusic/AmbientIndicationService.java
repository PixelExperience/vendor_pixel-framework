/*
 * Copyright (C) 2022 The PixelExperience Project
 * Copyright (C) 2023 The risingOS Android Project
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
package com.google.android.systemui.ambientmusic;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.os.SystemClock;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.Dependency;

public final class AmbientIndicationService extends BroadcastReceiver {
    public AlarmManager mAlarmManager;
    public AmbientIndicationContainer mAmbientIndicationContainer;
    public Context mContext;
    public boolean mStarted = false;
    public KeyguardUpdateMonitorCallback mCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public final void onUserSwitchComplete(int i) {
            onUserSwitched();
        }
    };
    public AlarmManager.OnAlarmListener mHideIndicationListener = new AlarmManager.OnAlarmListener() {
        @Override
        public final void onAlarm() {
            mAmbientIndicationContainer.setAmbientMusic(null, null, null, 0, false, null);
        }
    };

    public AmbientIndicationService(Context context, AmbientIndicationContainer ambientIndicationContainer, AlarmManager alarmManager) {
        mContext = context;
        mAmbientIndicationContainer = ambientIndicationContainer;
        mAlarmManager = alarmManager;
        start();
    }

    void start() {
        if (!mStarted) {
            mStarted = true;
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("com.google.android.ambientindication.action.AMBIENT_INDICATION_SHOW");
            intentFilter.addAction("com.google.android.ambientindication.action.AMBIENT_INDICATION_HIDE");
            mContext.registerReceiverAsUser(this, UserHandle.ALL, intentFilter, "com.google.android.ambientindication.permission.AMBIENT_INDICATION", null, 2);
            ((KeyguardUpdateMonitor) Dependency.get(KeyguardUpdateMonitor.class)).registerCallback(mCallback);
        }
    }

    @Override
    public final void onReceive(Context context, Intent intent) {
        if (!isForCurrentUser()) {
            Log.i("AmbientIndication", "Suppressing ambient, not for this user.");
            return;
        }
        int intExtra = intent.getIntExtra("com.google.android.ambientindication.extra.VERSION", 0);
        boolean z = true;
        if (intExtra != 1) {
            Log.e("AmbientIndication", "AmbientIndicationApi.EXTRA_VERSION is 1, but received an intent with version " + intExtra + ", dropping intent.");
            z = false;
        }
        if (!z) {
            return;
        }
        String action = intent.getAction();
        action.getClass();
        if (!action.equals("com.google.android.ambientindication.action.AMBIENT_INDICATION_HIDE")) {
            if (action.equals("com.google.android.ambientindication.action.AMBIENT_INDICATION_SHOW")) {
                long min = Math.min(Math.max(intent.getLongExtra("com.google.android.ambientindication.extra.TTL_MILLIS", 180000L), 0L), 180000L);
                boolean booleanExtra = intent.getBooleanExtra("com.google.android.ambientindication.extra.SKIP_UNLOCK", false);
                int intExtra2 = intent.getIntExtra("com.google.android.ambientindication.extra.ICON_OVERRIDE", 0);
                String stringExtra = intent.getStringExtra("com.google.android.ambientindication.extra.ICON_DESCRIPTION");
                mAmbientIndicationContainer.setAmbientMusic(intent.getCharSequenceExtra("com.google.android.ambientindication.extra.TEXT"), (PendingIntent) intent.getParcelableExtra("com.google.android.ambientindication.extra.OPEN_INTENT"), (PendingIntent) intent.getParcelableExtra("com.google.android.ambientindication.extra.FAVORITING_INTENT"), intExtra2, booleanExtra, stringExtra);
                mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + min, "AmbientIndication", mHideIndicationListener, null);
                Log.i("AmbientIndication", "Showing ambient indication.");
                return;
            }
            return;
        }
        mAlarmManager.cancel(mHideIndicationListener);
        mAmbientIndicationContainer.setAmbientMusic(null, null, null, 0, false, null);
        Log.i("AmbientIndication", "Hiding ambient indication.");
    }

    boolean isForCurrentUser() {
        return getSendingUserId() == getCurrentUser() || getSendingUserId() == -1;
    }

    int getCurrentUser() {
        return KeyguardUpdateMonitor.getCurrentUser();
    }

    void onUserSwitched() {
        mAmbientIndicationContainer.hideAmbientMusic();
    }
}
