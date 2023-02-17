/*
 * Copyright (C) 2023 The PixelExperience Project
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

package com.google.android.systemui.smartspace;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Log;
import androidx.annotation.NonNull;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.Dumpable;
import com.android.systemui.broadcast.BroadcastSender;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dump.DumpManager;
import com.android.systemui.smartspace.nano.SmartspaceProto;
import com.android.systemui.util.Assert;

import javax.inject.Inject;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

@SysUISingleton
public class SmartSpaceController implements Dumpable {
    static final String TAG = "SmartSpaceController";
    static final boolean DEBUG = Log.isLoggable(TAG, 3);
    public final AlarmManager mAlarmManager;
    public boolean mAlarmRegistered;
    public final Handler mBackgroundHandler;
    public final BroadcastSender mBroadcastSender;
    public final Context mContext;
    public int mCurrentUserId;
    public final SmartSpaceData mData;
    public boolean mHidePrivateData;
    public boolean mHideWorkData;
    public final ProtoStore mStore;
    public final ArrayList<SmartSpaceUpdateListener> mListeners = new ArrayList<>();
    public final AlarmManager.OnAlarmListener mExpireAlarmAction = () -> {
        onExpire(false);
    };
    public final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SmartSpaceController.this.onGsaChanged();
        }
    };
    public final KeyguardUpdateMonitorCallback mKeyguardMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void onTimeChanged() {
            if (SmartSpaceController.this.mData.hasCurrent() || SmartSpaceController.this.mData.getExpirationRemainingMillis() > 0) {
                SmartSpaceController.this.update();
            }
        }
    };
    public final Handler mUiHandler = new Handler(Looper.getMainLooper());

    @Inject
    public SmartSpaceController(Context context, KeyguardUpdateMonitor keyguardUpdateMonitor, @Background Handler backgroundHandler, AlarmManager alarmManager, BroadcastSender broadcastSender, DumpManager dumpManager) {
        this.mContext = context;
        this.mStore = new ProtoStore(context);
        new HandlerThread("smartspace-background").start();
        this.mBackgroundHandler = backgroundHandler;
        this.mCurrentUserId = UserHandle.myUserId();
        this.mAlarmManager = alarmManager;
        this.mBroadcastSender = broadcastSender;
        this.mData = new SmartSpaceData();
        if (isSmartSpaceDisabledByExperiments()) {
            return;
        }
        keyguardUpdateMonitor.registerCallback(this.mKeyguardMonitorCallback);
        reloadData();
        onGsaChanged();
        context.registerReceiver(this.mBroadcastReceiver, GSAIntents.getGsaPackageFilter("android.intent.action.PACKAGE_ADDED", "android.intent.action.PACKAGE_CHANGED", "android.intent.action.PACKAGE_REMOVED", "android.intent.action.PACKAGE_DATA_CLEARED"), 2);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        context.registerReceiver(new UserSwitchReceiver(), intentFilter);
        context.registerReceiver(new SmartSpaceBroadcastReceiver(this, this.mBroadcastSender), new IntentFilter("com.google.android.apps.nexuslauncher.UPDATE_SMARTSPACE"), "android.permission.CAPTURE_AUDIO_HOTWORD", this.mUiHandler, 2);
        dumpManager.registerDumpable(SmartSpaceController.class.getName(), this);
    }

    private SmartSpaceCard loadSmartSpaceData(boolean isCurrent) {
        SmartspaceProto.CardWrapper wrapper = new SmartspaceProto.CardWrapper();
        if (this.mStore.load("smartspace_" + this.mCurrentUserId + "_" + isCurrent, wrapper)) {
            return SmartSpaceCard.fromWrapper(this.mContext, wrapper, !isCurrent);
        }
        return null;
    }

    public void onNewCard(final NewCardInfo newCardInfo) {
        String str = "SmartSpaceController";
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onNewCard: ");
            sb.append(newCardInfo);
            Log.d(str, sb.toString());
        }
        if (newCardInfo != null) {
            if (newCardInfo.getUserId() != mCurrentUserId) {
                if (DEBUG) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("Ignore card that belongs to another user target: ");
                    sb2.append(mCurrentUserId);
                    sb2.append(" current: ");
                    sb2.append(mCurrentUserId);
                    Log.d(str, sb2.toString());
                }
                return;
            }
            mBackgroundHandler.post(new Runnable() {
                @Override
                public final void run() {
                    final SmartspaceProto.CardWrapper wrapper = newCardInfo.toWrapper(mContext);
                    if (!mHidePrivateData) {
                        ProtoStore protoStore = mStore;
                        StringBuilder sb = new StringBuilder();
                        sb.append("smartspace_");
                        sb.append(mCurrentUserId);
                        sb.append("_");
                        sb.append(newCardInfo.isPrimary());
                        protoStore.store(wrapper, sb.toString());
                    }
                    mUiHandler.post(new Runnable() {
                        @Override
                        public final void run() {
                            SmartSpaceCard smartSpaceCard = newCardInfo.shouldDiscard() ? null :
                                SmartSpaceCard.fromWrapper(mContext, wrapper, newCardInfo.isPrimary());
                            if (newCardInfo.isPrimary()) {
                                mData.mCurrentCard = smartSpaceCard;
                            } else {
                                mData.mWeatherCard = smartSpaceCard;
                            }
                            mData.handleExpire();
                            update();

                        }
                    });
                }
            });
        }
    }

    private void clearStore() {
        this.mStore.store(null, "smartspace_" + this.mCurrentUserId + "_true");
        this.mStore.store(null, "smartspace_" + this.mCurrentUserId + "_false");
    }

    private void update() {
        Assert.isMainThread();
        if (DEBUG) {
            Log.d(TAG, "update");
        }
        if (this.mAlarmRegistered) {
            this.mAlarmManager.cancel(this.mExpireAlarmAction);
            this.mAlarmRegistered = false;
        }
        long expiresAtMillis = this.mData.getExpiresAtMillis();
        if (expiresAtMillis > 0) {
            this.mAlarmManager.set(0, expiresAtMillis, "SmartSpace", this.mExpireAlarmAction, this.mUiHandler);
            this.mAlarmRegistered = true;
        }
        if (this.mListeners != null) {
            if (DEBUG) {
                Log.d(TAG, "notifying listeners data=" + this.mData);
            }
            ArrayList<SmartSpaceUpdateListener> listeners = new ArrayList<>(this.mListeners);
            int size = listeners.size();
            for (int i = 0; i < size; i++) {
                listeners.get(i).onSmartSpaceUpdated(this.mData);
            }
        }
    }

    private void onExpire(boolean z) {
        Assert.isMainThread();
        this.mAlarmRegistered = false;
        if (this.mData.handleExpire() || z) {
            update();
        } else if (DEBUG) {
            Log.d(TAG, "onExpire - cancelled");
        }
    }

    public void setHideSensitiveData(boolean z, boolean z2) {
        if (this.mHidePrivateData == z && this.mHideWorkData == z2) {
            return;
        }
        this.mHidePrivateData = z;
        this.mHideWorkData = z2;
        ArrayList<SmartSpaceUpdateListener> listeners = new ArrayList<>(this.mListeners);
        boolean z3 = false;
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onSensitiveModeChanged(z, z2);
        }
        if (this.mData.getCurrentCard() != null) {
            boolean z4 = this.mHidePrivateData && !this.mData.getCurrentCard().isWorkProfile();
            if (this.mHideWorkData && this.mData.getCurrentCard().isWorkProfile()) {
                z3 = true;
            }
            if (z4 || z3) {
                clearStore();
            }
        }
    }

    private void onGsaChanged() {
        if (DEBUG) {
            Log.d(TAG, "onGsaChanged");
        }
        ArrayList<SmartSpaceUpdateListener> listeners = new ArrayList<>(this.mListeners);
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onGsaChanged();
        }
    }

    public void reloadData() {
        this.mData.mCurrentCard = loadSmartSpaceData(true);
        this.mData.mWeatherCard = loadSmartSpaceData(false);
        update();
    }

    private boolean isSmartSpaceDisabledByExperiments() {
        boolean z;
        String string = Settings.Global.getString(this.mContext.getContentResolver(), "always_on_display_constants");
        KeyValueListParser keyValueListParser = new KeyValueListParser(',');
        try {
            keyValueListParser.setString(string);
            z = keyValueListParser.getBoolean("smart_space_enabled", true);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Bad AOD constants");
            z = true;
        }
        return !z;
    }

    public void dump(@NonNull PrintWriter pw, @NonNull String[] args) {
        pw.println();
        pw.println(TAG);
        pw.println("  weather " + this.mData.mWeatherCard);
        pw.println("  current " + this.mData.mCurrentCard);
        pw.println("serialized:");
        pw.println("  weather " + loadSmartSpaceData(false));
        pw.println("  current " + loadSmartSpaceData(true));
        pw.println("disabled by experiment: " + isSmartSpaceDisabledByExperiments());
    }

    public void addListener(SmartSpaceUpdateListener smartSpaceUpdateListener) {
        Assert.isMainThread();
        this.mListeners.add(smartSpaceUpdateListener);
        if (this.mData != null && smartSpaceUpdateListener != null) {
            smartSpaceUpdateListener.onSmartSpaceUpdated(this.mData);
        }
        if (smartSpaceUpdateListener != null) {
            smartSpaceUpdateListener.onSensitiveModeChanged(this.mHidePrivateData, this.mHideWorkData);
        }
    }

    public void removeListener(SmartSpaceUpdateListener smartSpaceUpdateListener) {
        Assert.isMainThread();
        this.mListeners.remove(smartSpaceUpdateListener);
    }

    private class UserSwitchReceiver extends BroadcastReceiver {
        private UserSwitchReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (SmartSpaceController.DEBUG) {
                Log.d(SmartSpaceController.TAG, "Switching user: " + intent.getAction() + " uid: " + UserHandle.myUserId());
            }
            if (intent.getAction().equals("android.intent.action.USER_SWITCHED")) {
                SmartSpaceController.this.mCurrentUserId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                SmartSpaceController.this.mData.clear();
                SmartSpaceController.this.onExpire(true);
            }
            SmartSpaceController.this.onExpire(true);
        }
    }

    private static class GSAIntents {
        private GSAIntents() {
        }

        public static IntentFilter getGsaPackageFilter(String... intents) {
            return getPackageFilter("com.google.android.googlequicksearchbox", intents);
        }

        public static IntentFilter getPackageFilter(String packageName, String... intents) {
            IntentFilter intentFilter = new IntentFilter();
            for (String action : intents) {
                intentFilter.addAction(action);
            }
            intentFilter.addDataScheme("package");
            intentFilter.addDataSchemeSpecificPart(packageName, 0);
            return intentFilter;
        }
    }
}