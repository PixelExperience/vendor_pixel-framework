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

package com.google.android.systemui.smartspace.filters

import android.app.smartspace.SmartspaceTarget
import android.app.smartspace.SmartspaceTarget.FEATURE_CALENDAR
import android.app.smartspace.SmartspaceTarget.FEATURE_WEATHER
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.smartspace.SmartspaceTargetFilter

@SysUISingleton
class DreamTargetFilter(
    private val lockscreenFilter: SmartspaceTargetFilter
) : SmartspaceTargetFilter {
    override fun addListener(listener: SmartspaceTargetFilter.Listener) {
        lockscreenFilter.addListener(listener)
    }

    override fun removeListener(listener: SmartspaceTargetFilter.Listener) {
        lockscreenFilter.removeListener(listener)
    }

    override fun filterSmartspaceTarget(t: SmartspaceTarget): Boolean {
        val featureType = t.featureType
        return when {
            featureType != FEATURE_WEATHER && featureType != FEATURE_CALENDAR -> {
                lockscreenFilter.filterSmartspaceTarget(t)
            }
            else -> false
        }
    }
}