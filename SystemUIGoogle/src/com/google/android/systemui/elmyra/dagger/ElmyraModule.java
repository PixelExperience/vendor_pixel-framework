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

package com.google.android.systemui.elmyra.dagger;

import android.content.Context;

import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.navigationbar.NavigationModeController;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.telephony.TelephonyListenerManager;
import com.google.android.systemui.assist.AssistManagerGoogle;
import com.google.android.systemui.elmyra.ServiceConfigurationGoogle;
import com.google.android.systemui.elmyra.actions.CameraAction;
import com.google.android.systemui.elmyra.actions.LaunchOpa;
import com.google.android.systemui.elmyra.actions.SettingsAction;
import com.google.android.systemui.elmyra.actions.SetupWizardAction;
import com.google.android.systemui.elmyra.actions.SilenceCall;
import com.google.android.systemui.elmyra.actions.UnpinNotifications;
import com.google.android.systemui.elmyra.feedback.AssistInvocationEffect;
import com.google.android.systemui.elmyra.feedback.OpaHomeButton;
import com.google.android.systemui.elmyra.feedback.OpaLockscreen;
import com.google.android.systemui.elmyra.feedback.SquishyNavigationButtons;
import com.google.android.systemui.elmyra.gates.TelephonyActivity;
import com.google.android.systemui.statusbar.phone.CentralSurfacesGoogle;

import java.util.Optional;

import dagger.Module;
import dagger.Provides;

@Module
public class ElmyraModule {

    @Provides
    @SysUISingleton
    static ServiceConfigurationGoogle provideServiceConfigurationGoogle(Context context, AssistInvocationEffect assistInvocationEffect, LaunchOpa.Builder builder, SettingsAction.Builder builderB, CameraAction.Builder builderC, SetupWizardAction.Builder builderD, SquishyNavigationButtons squishyNavigationButtons, UnpinNotifications unpinNotifications, SilenceCall silenceCall, TelephonyActivity telephonyActivity) {
        return new ServiceConfigurationGoogle(context, assistInvocationEffect, builder, builderB, builderC, builderD, squishyNavigationButtons, unpinNotifications, silenceCall, telephonyActivity);
    }

    @Provides
    @SysUISingleton
    static AssistInvocationEffect provideAssistInvocationEffectElmyra(AssistManagerGoogle assistManagerGoogle, OpaHomeButton opaHomeButton, OpaLockscreen opaLockscreen) {
        return new AssistInvocationEffect(assistManagerGoogle, opaHomeButton, opaLockscreen);
    }

    @Provides
    @SysUISingleton
    static OpaHomeButton provideOpaHomeButton(KeyguardViewMediator keyguardViewMediator, CentralSurfacesGoogle centralSurfacesGoogle, NavigationModeController navigationModeController) {
        return new OpaHomeButton(keyguardViewMediator, centralSurfacesGoogle, navigationModeController);
    }

    @Provides
    @SysUISingleton
    static OpaLockscreen provideOpaLockscreen(CentralSurfacesGoogle centralSurfacesGoogle, KeyguardStateController keyguardStateController) {
        return new OpaLockscreen(centralSurfacesGoogle, keyguardStateController);
    }

    @Provides
    @SysUISingleton
    static SquishyNavigationButtons provideSquishyNavigationButtons(Context context, KeyguardViewMediator keyguardViewMediator, CentralSurfacesGoogle centralSurfacesGoogle, NavigationModeController navigationModeController) {
        return new SquishyNavigationButtons(context, keyguardViewMediator, centralSurfacesGoogle, navigationModeController);
    }

    @Provides
    @SysUISingleton
    static TelephonyActivity provideTelephonyActivityElmyra(Context context, TelephonyListenerManager telephonyListenerManager) {
        return new TelephonyActivity(context, telephonyListenerManager);
    }

    @Provides
    @SysUISingleton
    static SetupWizardAction.Builder provideSetupWizardAction(Context context, CentralSurfacesGoogle centralSurfacesGoogle) {
        return new SetupWizardAction.Builder(context, centralSurfacesGoogle);
    }

    @Provides
    @SysUISingleton
    static UnpinNotifications provideUnpinNotificationsElmyra(Context context, Optional<HeadsUpManager> optional) {
        return new UnpinNotifications(context, optional);
    }

    @Provides
    @SysUISingleton
    static LaunchOpa.Builder provideLaunchOpaElmyra(Context context, CentralSurfacesGoogle centralSurfacesGoogle) {
        return new LaunchOpa.Builder(context, centralSurfacesGoogle);
    }

    @Provides
    @SysUISingleton
    static SilenceCall provideSilenceCallElmyra(Context context, TelephonyListenerManager telephonyListenerManager) {
        return new SilenceCall(context, telephonyListenerManager);
    }

    @Provides
    @SysUISingleton
    static SettingsAction.Builder provideSettingsActionElmyra(Context context, CentralSurfacesGoogle centralSurfacesGoogle) {
        return new SettingsAction.Builder(context, centralSurfacesGoogle);
    }

    @Provides
    @SysUISingleton
    static CameraAction.Builder provideCameraAction(Context context, CentralSurfacesGoogle centralSurfacesGoogle) {
        return new CameraAction.Builder(context, centralSurfacesGoogle);
    }
}