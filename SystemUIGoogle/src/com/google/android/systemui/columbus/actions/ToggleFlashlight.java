/*
 * Copyright (C) 2022 The PixelExperience Project
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

package com.google.android.systemui.columbus.actions;

import android.content.Context;
import android.os.Handler;

import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.google.android.systemui.columbus.ColumbusEvent;
import com.google.android.systemui.columbus.sensors.GestureSensor;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

@SysUISingleton
public final class ToggleFlashlight extends UserAction {
    public static final long FLASHLIGHT_TIMEOUT = TimeUnit.MINUTES.toMillis(2);
    public final FlashlightController mFlashlightController;
    public final Handler mHandler;
    public final String mTag;
    public final Runnable mTurnOffFlashlightRunnable = new Runnable() {
        @Override
        public void run() {
            mFlashlightController.setFlashlight(false);
        }
    };

    @Inject
    public ToggleFlashlight(Context context, FlashlightController flashlightController, @Main Handler handler) {
        super(context, null, 2, null);
        mFlashlightController = flashlightController;
        mHandler = handler;
        mTag = "ToggleFlashlight";
        FlashlightController.FlashlightListener flashlightListener = new FlashlightController.FlashlightListener() {
            @Override
            public void onFlashlightAvailabilityChanged(boolean z) {
                if (!z) {
                    mHandler.removeCallbacks(mTurnOffFlashlightRunnable);
                }
                updateAvailable();
            }

            @Override
            public void onFlashlightChanged(boolean z) {
                if (!z) {
                    mHandler.removeCallbacks(mTurnOffFlashlightRunnable);
                }
                updateAvailable();
            }

            @Override
            public void onFlashlightError() {
                mHandler.removeCallbacks(mTurnOffFlashlightRunnable);
                updateAvailable();
            }
        };
        mFlashlightController.addCallback(flashlightListener);
        updateAvailable();
    }

    @Override
    public boolean availableOnLockscreen() {
        return true;
    }

    @Override
    public void onTrigger(GestureSensor.DetectionProperties detectionProperties) {
        mHandler.removeCallbacks(mTurnOffFlashlightRunnable);
        boolean z = !mFlashlightController.isEnabled();
        mFlashlightController.setFlashlight(z);
        if (z) {
            mHandler.postDelayed(mTurnOffFlashlightRunnable, FLASHLIGHT_TIMEOUT);
        }
    }

    public void updateAvailable() {
        setAvailable(mFlashlightController.hasFlashlight() && mFlashlightController.isAvailable());
    }

    @Override
    public String getTag$vendor__unbundled_google__packages__SystemUIGoogle__android_common__sysuig() {
        return mTag;
    }
}
