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

package com.google.android.systemui.smartspace.dagger;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Handler;

import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.flags.FeatureFlags;
import com.android.systemui.plugins.BcSmartspaceDataPlugin;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.util.concurrency.DelayableExecutor;
import com.google.android.systemui.smartspace.BcSmartspaceDataProvider;
import com.google.android.systemui.smartspace.KeyguardMediaViewController;
import com.google.android.systemui.smartspace.KeyguardSmartspaceController;
import com.google.android.systemui.smartspace.KeyguardZenAlarmViewController;

import dagger.Module;
import dagger.Provides;

@Module
public abstract class SmartspaceModule {
    @Provides
    @SysUISingleton
    static KeyguardSmartspaceController provideKeyguardSmartspaceController(FeatureFlags featureFlags,
                                                                            KeyguardZenAlarmViewController keyguardZenAlarmViewController, KeyguardMediaViewController keyguardMediaViewController) {
        return new KeyguardSmartspaceController(featureFlags, keyguardZenAlarmViewController, keyguardMediaViewController);
    }

    @Provides
    @SysUISingleton
    static KeyguardZenAlarmViewController provideKeyguardZenAlarmViewController(Context context, BcSmartspaceDataPlugin bcSmartspaceDataPlugin, ZenModeController zenModeController,
                                                                                AlarmManager alarmManager, NextAlarmController nextAlarmController, Handler handler) {
        return new KeyguardZenAlarmViewController(context, bcSmartspaceDataPlugin, zenModeController, alarmManager, nextAlarmController, handler);
    }

    @Provides
    @SysUISingleton
    static KeyguardMediaViewController provideKeyguardMediaViewController(Context context, BcSmartspaceDataPlugin bcSmartspaceDataPlugin,
                                                                          @Main DelayableExecutor delayableExecutor, NotificationMediaManager notificationMediaManager, BroadcastDispatcher broadcastDispatcher) {
        return new KeyguardMediaViewController(context, bcSmartspaceDataPlugin, delayableExecutor, notificationMediaManager, broadcastDispatcher);
    }

    @Provides
    @SysUISingleton
    static BcSmartspaceDataPlugin provideBcSmartspaceDataPlugin() {
        return new BcSmartspaceDataProvider();
    }
}
