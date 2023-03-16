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

package com.google.android.systemui;

import android.app.Application;
import android.content.Context;
import com.android.systemui.SystemUIInitializer;
import com.android.systemui.dagger.GlobalRootComponent;
import com.google.android.systemui.dagger.DaggerSystemUIGoogleGlobalRootComponent;

public class SystemUIGoogleInitializer extends SystemUIInitializer {

    private static final String SCREENSHOT_CROSS_PROFILE_PROCESS = "com.android.systemui:screenshot_cross_profile";

    public SystemUIGoogleInitializer(Context context) {
        super(context);
    }

    @Override
    protected GlobalRootComponent.Builder getGlobalRootComponentBuilder() {
        if (SCREENSHOT_CROSS_PROFILE_PROCESS.equals(Application.getProcessName())) {
            return null;
        }
        return DaggerSystemUIGoogleGlobalRootComponent.builder();
    }
}
