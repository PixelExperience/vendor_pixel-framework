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

package com.google.android.systemui.power.dagger;

import android.content.Context;

import com.android.internal.logging.UiEventLogger;
import com.android.systemui.animation.DialogLaunchAnimator;
import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.broadcast.BroadcastSender;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.power.EnhancedEstimates;
import com.android.systemui.power.PowerUI;
import com.android.systemui.power.data.repository.PowerRepositoryModule;
import com.android.systemui.statusbar.policy.BatteryController;
import com.google.android.systemui.power.EnhancedEstimatesGoogleImpl;
import com.google.android.systemui.power.PowerNotificationWarningsGoogleImpl;

import dagger.Binds;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;


/**
 * Dagger Module for code in the power package.
 */
@Module(
    includes = {
            PowerRepositoryModule.class,
    }
)
public interface PowerModuleGoogle {
    @Provides
    @SysUISingleton
    static PowerNotificationWarningsGoogleImpl providePowerNotificationWarningsGoogleImpl(Context context, ActivityStarter activityStarter,
                                                                                          BroadcastSender broadcastSender, Lazy<BatteryController> batteryControllerLazy,
                                                                                          DialogLaunchAnimator dialogLaunchAnimator, UiEventLogger uiEventLogger,
                                                                                          BroadcastDispatcher broadcastDispatcher) {
        return new PowerNotificationWarningsGoogleImpl(context, activityStarter, broadcastSender, batteryControllerLazy, dialogLaunchAnimator, uiEventLogger, broadcastDispatcher);
    }

    /**
     *
     */
    @Binds
    EnhancedEstimates bindEnhancedEstimates(EnhancedEstimatesGoogleImpl enhancedEstimates);

    /**
     *
     */
    @Binds
    PowerUI.WarningsUI provideWarningsUi(PowerNotificationWarningsGoogleImpl controllerImpl);
}
