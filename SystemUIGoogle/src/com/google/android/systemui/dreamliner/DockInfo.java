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

package com.google.android.systemui.dreamliner;

import android.os.Bundle;

public class DockInfo {
    private int accessoryType;
    private String manufacturer;
    private String model;
    private String serialNumber;

    public DockInfo(String str, String str2, String str3, int i) {
        manufacturer = "";
        model = "";
        serialNumber = "";
        accessoryType = -1;
        manufacturer = str;
        model = str2;
        serialNumber = str3;
        accessoryType = i;
    }

    Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("manufacturer", manufacturer);
        bundle.putString("model", model);
        bundle.putString("serialNumber", serialNumber);
        bundle.putInt("accessoryType", accessoryType);
        return bundle;
    }

    public String toString() {
        return manufacturer + ", " + model + ", " + serialNumber + ", " + accessoryType;
    }
}
