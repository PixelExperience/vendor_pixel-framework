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

package com.google.android.systemui.smartspace.dagger

import com.android.systemui.plugins.BcSmartspaceDataPlugin
import com.android.systemui.smartspace.SmartspaceTargetFilter
import com.android.systemui.smartspace.dagger.SmartspaceModule.Companion.DREAM_SMARTSPACE_DATA_PLUGIN
import com.android.systemui.smartspace.dagger.SmartspaceModule.Companion.DREAM_SMARTSPACE_TARGET_FILTER
import com.google.android.systemui.smartspace.*
import com.google.android.systemui.smartspace.filters.DreamTargetFilter
import dagger.Binds
import dagger.Module
import javax.inject.Named

@Module
abstract class SmartspaceGoogleModule {
    @Binds
    @Named(DREAM_SMARTSPACE_DATA_PLUGIN)
    abstract fun bindsDreamBcSmartspaceDataPlugin(
        plugin: BcSmartspaceDataProvider
    ): BcSmartspaceDataPlugin

    @Binds
    @Named(DREAM_SMARTSPACE_TARGET_FILTER)
    abstract fun bindsDreamSmartspaceTargetFilter(
        filter: DreamTargetFilter
    ): SmartspaceTargetFilter
}
