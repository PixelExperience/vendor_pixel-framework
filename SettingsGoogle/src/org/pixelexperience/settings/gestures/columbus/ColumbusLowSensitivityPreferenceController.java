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

package org.pixelexperience.settings.gestures.columbus;

import android.content.Context;
import android.content.IntentFilter;
import com.android.settings.slices.SliceBackgroundWorker;
import com.google.android.settings.gestures.columbus.ColumbusTogglePreferenceController;

import android.os.SystemProperties;

public class ColumbusLowSensitivityPreferenceController extends ColumbusTogglePreferenceController {
    public ColumbusLowSensitivityPreferenceController(Context context, String str) {
        super(context, str, 1747);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (SystemProperties.getBoolean("persist.columbus.use_ap_sensor", true)){
            return UNSUPPORTED_ON_DEVICE;
        }
        return super.getAvailabilityStatus();
    }
}