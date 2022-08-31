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

package com.google.android.systemui.reversecharging.dagger;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.IThermalService;
import android.os.ServiceManager;

import com.android.systemui.R;
import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.statusbar.phone.CentralSurfaces;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.google.android.systemui.reversecharging.ReverseChargingViewController;
import com.google.android.systemui.reversecharging.ReverseWirelessCharger;
import com.google.android.systemui.statusbar.KeyguardIndicationControllerGoogle;

import java.util.Optional;
import java.util.concurrent.Executor;

import dagger.Lazy;
import dagger.Module;
import dagger.Provides;

@Module
public abstract class ReverseChargingModule {
    @Provides
    @SysUISingleton
    static Optional<ReverseWirelessCharger> provideReverseWirelessCharger(Context context) {
        return context.getResources().getBoolean(R.bool.config_wlc_support_enabled) ? Optional.of(new ReverseWirelessCharger(context)) : Optional.empty();
    }

    @Provides
    @SysUISingleton
    static Optional<UsbManager> provideUsbManager(Context context) {
        return Optional.ofNullable(context.getSystemService(UsbManager.class));
    }

    @Provides
    @SysUISingleton
    static IThermalService provideIThermalService() {
        return IThermalService.Stub.asInterface(ServiceManager.getService("thermalservice"));
    }

    @Provides
    @SysUISingleton
    static Optional<ReverseChargingViewController> provideReverseChargingViewController(Context context, BatteryController batteryController, Lazy<CentralSurfaces> lazy, StatusBarIconController statusBarIconController, BroadcastDispatcher broadcastDispatcher, @Main Executor executor, KeyguardIndicationControllerGoogle keyguardIndicationControllerGoogle) {
        if (batteryController.isReverseSupported()) {
            return Optional.of(new ReverseChargingViewController(context, batteryController, lazy, statusBarIconController, broadcastDispatcher, executor, keyguardIndicationControllerGoogle));
        }
        return Optional.empty();
    }
}
