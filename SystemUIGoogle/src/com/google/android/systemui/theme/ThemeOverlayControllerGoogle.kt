/*
 * Copyright (C) 2022 The PixelExperience Project
 * Copyright (C) 2023 The risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.systemui.theme

import android.app.UiModeManager
import android.app.WallpaperManager
import android.content.Context
import android.content.res.Resources
import android.os.Handler
import android.os.UserManager
import android.util.Log
import com.android.systemui.broadcast.BroadcastDispatcher
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.dump.DumpManager
import com.android.systemui.flags.FeatureFlags
import com.android.systemui.flags.SystemPropertiesHelper
import com.android.systemui.keyguard.WakefulnessLifecycle
import com.android.systemui.settings.UserTracker
import com.android.systemui.statusbar.policy.ConfigurationController
import com.android.systemui.statusbar.policy.DeviceProvisionedController
import com.android.systemui.theme.ThemeOverlayApplier
import com.android.systemui.theme.ThemeOverlayController
import com.android.systemui.util.settings.SecureSettings
import java.util.concurrent.Executor
import javax.inject.Inject

@SysUISingleton
class ThemeOverlayControllerGoogle @Inject constructor(
    private val context: Context,
    broadcastDispatcher: BroadcastDispatcher,
    @Background bgHandler: Handler,
    @Main mainExecutor: Executor,
    @Background bgExecutor: Executor,
    themeOverlayApplier: ThemeOverlayApplier,
    secureSettings: SecureSettings,
    wallpaperManager: WallpaperManager,
    userManager: UserManager,
    deviceProvisionedController: DeviceProvisionedController,
    userTracker: UserTracker,
    dumpManager: DumpManager,
    featureFlags: FeatureFlags,
    @Main resources: Resources,
    wakefulnessLifecycle: WakefulnessLifecycle,
    uiModeManager: UiModeManager,
    @param:Main private val mainResources: Resources,
    private val systemPropertiesHelper: SystemPropertiesHelper,
    configurationController: ConfigurationController
) : ThemeOverlayController(
    context,
    broadcastDispatcher,
    bgHandler,
    mainExecutor,
    bgExecutor,
    themeOverlayApplier,
    secureSettings,
    wallpaperManager,
    userManager,
    deviceProvisionedController,
    userTracker,
    dumpManager,
    featureFlags,
    resources,
    wakefulnessLifecycle,
    uiModeManager,
    configurationController
) {
    init {
        configurationController.addCallback(object :
            ConfigurationController.ConfigurationListener {
            override fun onThemeChanged() {
                setBootColorSystemProps()
            }
        })

        val bootColors = getBootColors()
        for (i in bootColors.indices) {
            Log.d("ThemeOverlayController", "Boot animation colors ${i + 1}: ${bootColors[i]}")
        }
    }

    private fun setBootColorSystemProps() {
        try {
            val bootColors = getBootColors()
            for (i in bootColors.indices) {
                systemPropertiesHelper.set("persist.bootanim.color${i + 1}", bootColors[i])
                Log.d("ThemeOverlayController", "Writing boot animation colors ${i + 1}: ${bootColors[i]}")
            }
        } catch (e: RuntimeException) {
            Log.w("ThemeOverlayController", "Cannot set sysprop. Look for 'init' and 'dmesg' logs for more info.")
        }
    }

    private fun getBootColors(): IntArray {
        return intArrayOf(
            mainResources.getColor(android.R.color.system_accent3_100),
            mainResources.getColor(android.R.color.system_accent1_300),
            mainResources.getColor(android.R.color.system_accent2_500),
            mainResources.getColor(android.R.color.system_accent1_100)
        )
    }
}
