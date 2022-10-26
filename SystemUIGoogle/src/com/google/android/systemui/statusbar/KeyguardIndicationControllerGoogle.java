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

package com.google.android.systemui.statusbar;

import android.app.IActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.DeviceConfig;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.settingslib.fuelgauge.BatteryStatus;
import com.android.systemui.R;
import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.dock.DockManager;
import com.android.systemui.keyguard.KeyguardIndication;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.phone.KeyguardBypassController;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.DeviceConfigProxy;
import com.android.systemui.util.concurrency.DelayableExecutor;
import com.android.systemui.util.time.DateFormatUtil;
import com.android.systemui.util.wakelock.WakeLock;
import com.google.android.systemui.adaptivecharging.AdaptiveChargingManager;

import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

@SysUISingleton
public class KeyguardIndicationControllerGoogle extends KeyguardIndicationController {
    private final IBatteryStats mBatteryInfo;
    private final BroadcastDispatcher mBroadcastDispatcher;
    private final BroadcastReceiver mBroadcastReceiver;
    private final Context mContext;
    private final DateFormatUtil mDateFormatUtil;
    private final DeviceConfigProxy mDeviceConfig;
    private final TunerService mTunerService;
    @VisibleForTesting
    protected AdaptiveChargingManager mAdaptiveChargingManager;
    @VisibleForTesting
    protected AdaptiveChargingManager.AdaptiveChargingStatusReceiver mAdaptiveChargingStatusReceiver;
    private boolean mAdaptiveChargingActive;
    private boolean mAdaptiveChargingEnabledInSettings;
    private int mBatteryLevel;
    private long mEstimatedChargeCompletion;
    private boolean mInited;
    private boolean mIsCharging;
    private KeyguardUpdateMonitorCallback mUpdateMonitorCallback;

    @Inject
    public KeyguardIndicationControllerGoogle(
            Context context,
            @Main Looper mainLooper,
            WakeLock.Builder wakeLockBuilder,
            KeyguardStateController keyguardStateController,
            StatusBarStateController statusBarStateController,
            KeyguardUpdateMonitor keyguardUpdateMonitor,
            DockManager dockManager,
            BroadcastDispatcher broadcastDispatcher,
            DevicePolicyManager devicePolicyManager,
            IBatteryStats iBatteryStats,
            UserManager userManager,
            @Main DelayableExecutor executor,
            @Background DelayableExecutor bgExecutor,
            FalsingManager falsingManager,
            LockPatternUtils lockPatternUtils,
            ScreenLifecycle screenLifecycle,
            IActivityManager iActivityManager,
            KeyguardBypassController keyguardBypassController,
            AccessibilityManager accessibilityManager,
            TunerService tunerService,
            DeviceConfigProxy deviceConfigProxy) {
        super(context, mainLooper, wakeLockBuilder, keyguardStateController, statusBarStateController, keyguardUpdateMonitor, dockManager, broadcastDispatcher, devicePolicyManager, iBatteryStats, userManager, executor, bgExecutor, falsingManager, lockPatternUtils, screenLifecycle, iActivityManager, keyguardBypassController, accessibilityManager);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("com.google.android.systemui.adaptivecharging.ADAPTIVE_CHARGING_DEADLINE_SET".equals(intent.getAction())) {
                    triggerAdaptiveChargingStatusUpdate();
                }
            }
        };
        mAdaptiveChargingStatusReceiver = new AdaptiveChargingManager.AdaptiveChargingStatusReceiver() {
            @Override
            public void onDestroyInterface() {
            }

            @Override
            public void onReceiveStatus(String str, int i) {
                boolean z = mAdaptiveChargingActive;
                mAdaptiveChargingActive = AdaptiveChargingManager.isActive(str, i);
                long j = mEstimatedChargeCompletion;
                long currentTimeMillis = System.currentTimeMillis();
                TimeUnit timeUnit = TimeUnit.SECONDS;
                mEstimatedChargeCompletion = currentTimeMillis + timeUnit.toMillis(i + 29);
                long abs = Math.abs(mEstimatedChargeCompletion - j);
                if (z != mAdaptiveChargingActive || (mAdaptiveChargingActive && abs > timeUnit.toMillis(30L))) {
                    updateDeviceEntryIndication(true);
                }
            }
        };
        mContext = context;
        mBroadcastDispatcher = broadcastDispatcher;
        mTunerService = tunerService;
        mDeviceConfig = deviceConfigProxy;
        mAdaptiveChargingManager = new AdaptiveChargingManager(context);
        mBatteryInfo = iBatteryStats;
        mDateFormatUtil = new DateFormatUtil(context);
    }

    @Override
    public void init() {
        super.init();
        if (mInited) {
            return;
        }
        mInited = true;
        mTunerService.addTunable(new TunerService.Tunable() {
            @Override
            public final void onTuningChanged(String str, String str2) {
                refreshAdaptiveChargingEnabled();
            }
        }, "adaptive_charging_enabled");
        mDeviceConfig.addOnPropertiesChangedListener("adaptive_charging", mContext.getMainExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.google.android.systemui.statusbar.KeyguardIndicationControllerGoogle$$ExternalSyntheticLambda0
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                if (properties.getKeyset().contains("adaptive_charging_enabled")) {
                    triggerAdaptiveChargingStatusUpdate();
                }
            }
        });
        triggerAdaptiveChargingStatusUpdate();
        mBroadcastDispatcher.registerReceiver(mBroadcastReceiver, new IntentFilter("com.google.android.systemui.adaptivecharging.ADAPTIVE_CHARGING_DEADLINE_SET"), null, UserHandle.ALL);
    }

    private void refreshAdaptiveChargingEnabled() {
        mAdaptiveChargingEnabledInSettings = mAdaptiveChargingManager.isAvailable() && mAdaptiveChargingManager.isEnabled();
    }

    @Override
    protected String computePowerIndication() {
        if (mIsCharging && mAdaptiveChargingEnabledInSettings && mAdaptiveChargingActive) {
            String formatTimeToFull = mAdaptiveChargingManager.formatTimeToFull(mEstimatedChargeCompletion);
            return mContext.getResources().getString(R.string.adaptive_charging_time_estimate, NumberFormat.getPercentInstance().format(mBatteryLevel / 100.0f), formatTimeToFull);
        }
        return super.computePowerIndication();
    }

    @Override
    protected KeyguardUpdateMonitorCallback getKeyguardCallback() {
        if (mUpdateMonitorCallback == null) {
            mUpdateMonitorCallback = new GoogleKeyguardCallback();
        }
        return mUpdateMonitorCallback;
    }

    public void setReverseChargingMessage(CharSequence charSequence) {
        if (TextUtils.isEmpty(charSequence)) {
            mRotateTextViewController.hideIndication(10);
        } else {
            mRotateTextViewController.updateIndication(10, new KeyguardIndication.Builder().setMessage(charSequence).setIcon(mContext.getDrawable(R.anim.reverse_charging_animation)).setTextColor(mInitialTextColorState).build(), false);
        }
    }

    private void triggerAdaptiveChargingStatusUpdate() {
        refreshAdaptiveChargingEnabled();
        if (mAdaptiveChargingEnabledInSettings) {
            mAdaptiveChargingManager.queryStatus(mAdaptiveChargingStatusReceiver);
        } else {
            mAdaptiveChargingActive = false;
        }
    }

    protected class GoogleKeyguardCallback extends KeyguardIndicationController.BaseKeyguardCallback {
        protected GoogleKeyguardCallback() {
            super();
        }

        @Override
        public void onRefreshBatteryInfo(BatteryStatus batteryStatus) {
            mIsCharging = batteryStatus.status == 2;
            mBatteryLevel = batteryStatus.level;
            super.onRefreshBatteryInfo(batteryStatus);
            if (mIsCharging) {
                triggerAdaptiveChargingStatusUpdate();
            } else {
                mAdaptiveChargingActive = false;
            }
        }
    }
}
