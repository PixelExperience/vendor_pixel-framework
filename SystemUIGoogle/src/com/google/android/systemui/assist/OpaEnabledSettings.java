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

package com.google.android.systemui.assist;

import android.app.ActivityManager;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.widget.ILockSettings;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.util.Assert;

import javax.inject.Inject;

@SysUISingleton
public class OpaEnabledSettings {
    private final Context mContext;
    private final ILockSettings mLockSettings = ILockSettings.Stub.asInterface(ServiceManager.getService("lock_settings"));

    @Inject
    public OpaEnabledSettings(Context context) {
        mContext = context;
    }

    public boolean isOpaEligible() {
        Assert.isNotMainThread();
        return Settings.Secure.getIntForUser(mContext.getContentResolver(), "systemui.google.opa_enabled", 0, ActivityManager.getCurrentUser()) != 0;
    }

    public void setOpaEligible(boolean z) {
        Assert.isNotMainThread();
        Settings.Secure.putIntForUser(mContext.getContentResolver(), "systemui.google.opa_enabled", z ? 1 : 0, ActivityManager.getCurrentUser());
    }

    public boolean isOpaEnabled() {
        Assert.isNotMainThread();
        try {
            return mLockSettings.getBoolean("systemui.google.opa_user_enabled", false, ActivityManager.getCurrentUser());
        } catch (RemoteException e) {
            Log.e("OpaEnabledSettings", "isOpaEnabled RemoteException", e);
            return false;
        }
    }

    public void setOpaEnabled(boolean z) {
        Assert.isNotMainThread();
        try {
            mLockSettings.setBoolean("systemui.google.opa_user_enabled", z, ActivityManager.getCurrentUser());
        } catch (RemoteException e) {
            Log.e("OpaEnabledSettings", "RemoteException on OPA_USER_ENABLED", e);
        }
    }

    public boolean isAgsaAssistant() {
        Assert.isNotMainThread();
        return OpaUtils.isAGSACurrentAssistant(mContext);
    }

    public boolean isLongPressHomeEnabled() {
        Assert.isNotMainThread();
        return Settings.Secure.getInt(mContext.getContentResolver(), "assist_long_press_home_enabled", mContext.getResources().getBoolean(com.android.internal.R.bool.config_assistLongPressHomeEnabledDefault) ? 1 : 0) != 0;
    }
}
