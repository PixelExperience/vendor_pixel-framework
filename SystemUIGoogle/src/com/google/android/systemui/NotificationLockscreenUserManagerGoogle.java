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

package com.google.android.systemui;

import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Handler;
import android.os.UserManager;
import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.dump.DumpManager;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.NotificationClickNotifier;
import com.android.systemui.statusbar.NotificationLockscreenUserManagerImpl;
import com.android.systemui.statusbar.notification.collection.notifcollection.CommonNotifCollection;
import com.android.systemui.statusbar.notification.collection.render.NotificationVisibilityProvider;
import com.android.systemui.statusbar.phone.KeyguardBypassController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.util.settings.SecureSettings;
import com.google.android.systemui.smartspace.SmartSpaceController;
import dagger.Lazy;
import javax.inject.Inject;

@SysUISingleton
public final class NotificationLockscreenUserManagerGoogle extends NotificationLockscreenUserManagerImpl {
    public final Lazy<KeyguardBypassController> mKeyguardBypassControllerLazy;
    public final KeyguardStateController mKeyguardStateController;
    public final KeyguardStateController.Callback mKeyguardVisibilityCallback;
    public final SmartSpaceController mSmartSpaceController;

    @Inject
    public NotificationLockscreenUserManagerGoogle(Context context, BroadcastDispatcher broadcastDispatcher, DevicePolicyManager devicePolicyManager, UserManager userManager, Lazy<NotificationVisibilityProvider> visibilityProviderLazy, Lazy<CommonNotifCollection> commonNotifCollectionLazy, NotificationClickNotifier clickNotifier, KeyguardManager keyguardManager, StatusBarStateController statusBarStateController, @Main Handler mainHandler, DeviceProvisionedController deviceProvisionedController, KeyguardStateController keyguardStateController, Lazy<KeyguardBypassController> keyguardBypassController, SmartSpaceController smartSpaceController, SecureSettings secureSettings, DumpManager dumpManager) {
        super(context, broadcastDispatcher, devicePolicyManager, userManager, visibilityProviderLazy, commonNotifCollectionLazy, clickNotifier, keyguardManager, statusBarStateController, mainHandler, deviceProvisionedController, keyguardStateController, secureSettings, dumpManager);
        KeyguardStateController.Callback callback = new KeyguardStateController.Callback() {
            public void onKeyguardShowingChanged() {
                NotificationLockscreenUserManagerGoogle.this.updateSmartSpaceVisibilitySettings();
            }
        };
        this.mKeyguardVisibilityCallback = callback;
        this.mKeyguardBypassControllerLazy = keyguardBypassController;
        this.mSmartSpaceController = smartSpaceController;
        this.mKeyguardStateController = keyguardStateController;
        this.mKeyguardStateController.addCallback(callback);
    }

    public void updateSmartSpaceVisibilitySettings() {
        boolean hideNotifs = false;
        boolean hideSensitive = !userAllowsPrivateNotificationsInPublic(this.mCurrentUserId) && (isAnyProfilePublicMode() || !this.mKeyguardStateController.isShowing());
        boolean hideWork = !allowsManagedPrivateNotificationsInPublic();
        if (!((KeyguardBypassController) this.mKeyguardBypassControllerLazy.get()).getBypassEnabled()) {
            if (hideWork && (isAnyManagedProfilePublicMode() || !this.mKeyguardStateController.isShowing())) {
                hideNotifs = true;
            }
            hideWork = hideNotifs;
        }
        this.mSmartSpaceController.setHideSensitiveData(hideSensitive, hideWork);
    }

    public void updatePublicMode() {
        super.updatePublicMode();
        updateSmartSpaceVisibilitySettings();
    }
}