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

package com.google.android.systemui.statusbar.phone;

import static com.android.systemui.Dependency.TIME_TICK_HANDLER_NAME;

import android.app.WallpaperManager;
import android.content.Context;
import android.hardware.devicestate.DeviceStateManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.dreams.IDreamManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.internal.jank.InteractionJankMonitor;
import com.android.internal.logging.MetricsLogger;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.Dependency;
import com.android.systemui.InitController;
import com.android.systemui.R;
import com.android.systemui.accessibility.floatingmenu.AccessibilityFloatingMenuController;
import com.android.systemui.animation.ActivityLaunchAnimator;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.charging.WiredChargingRippleController;
import com.android.systemui.classifier.FalsingCollector;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.dagger.qualifiers.UiBackground;
import com.android.systemui.demomode.DemoModeController;
import com.android.systemui.dock.DockManager;
import com.android.systemui.flags.FeatureFlags;
import com.android.systemui.fragments.FragmentService;
import com.android.systemui.keyguard.KeyguardUnlockAnimationController;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.keyguard.ui.viewmodel.LightRevealScrimViewModel;
import com.android.systemui.navigationbar.NavigationBarController;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.PluginDependencyProvider;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.recents.ScreenPinningRequest;
import com.android.systemui.settings.brightness.BrightnessSliderController;
import com.android.systemui.shade.CameraLauncher;
import com.android.systemui.shade.ShadeController;
import com.android.systemui.shade.ShadeExpansionStateManager;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.LockscreenShadeTransitionController;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.NotificationShadeDepthController;
import com.android.systemui.statusbar.NotificationShadeWindowController;
import com.android.systemui.statusbar.PulseExpansionHandler;
import com.android.systemui.statusbar.SysuiStatusBarStateController;
import com.android.systemui.statusbar.notification.DynamicPrivacyController;
import com.android.systemui.statusbar.notification.NotificationWakeUpCoordinator;
import com.android.systemui.statusbar.notification.init.NotificationsController;
import com.android.systemui.statusbar.notification.interruption.NotificationInterruptStateProvider;
import com.android.systemui.statusbar.notification.logging.NotificationLogger;
import com.android.systemui.statusbar.notification.row.NotificationGutsManager;
import com.android.systemui.statusbar.phone.*;
import com.android.systemui.statusbar.phone.AutoHideController;
import com.android.systemui.statusbar.phone.BiometricUnlockController;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.statusbar.phone.DozeScrimController;
import com.android.systemui.statusbar.phone.DozeServiceHost;
import com.android.systemui.statusbar.phone.HeadsUpManagerPhone;
import com.android.systemui.statusbar.phone.KeyguardBypassController;
import com.android.systemui.statusbar.phone.KeyguardDismissUtil;
import com.android.systemui.statusbar.phone.LightBarController;
import com.android.systemui.statusbar.phone.LockscreenWallpaper;
import com.android.systemui.statusbar.phone.NotificationIconAreaController;
import com.android.systemui.statusbar.phone.PhoneStatusBarPolicy;
import com.android.systemui.statusbar.phone.StatusBarHideIconsForBouncerManager;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.phone.StatusBarSignalPolicy;
import com.android.systemui.statusbar.phone.StatusBarTouchableRegionManager;
import com.android.systemui.statusbar.phone.dagger.CentralSurfacesComponent;
import com.android.systemui.statusbar.phone.ongoingcall.OngoingCallController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.statusbar.policy.UserInfoControllerImpl;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.window.StatusBarWindowController;
import com.android.systemui.statusbar.window.StatusBarWindowStateController;
import com.android.systemui.util.WallpaperController;
import com.android.systemui.util.concurrency.DelayableExecutor;
import com.android.systemui.util.concurrency.MessageRouter;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.volume.VolumeComponent;
import com.android.wm.shell.bubbles.Bubbles;
import com.android.wm.shell.startingsurface.StartingSurface;
import com.google.android.systemui.NotificationLockscreenUserManagerGoogle;
import com.google.android.systemui.dreamliner.DockIndicationController;
import com.google.android.systemui.dreamliner.DockObserver;
import com.google.android.systemui.reversecharging.ReverseChargingViewController;
import com.google.android.systemui.smartspace.SmartSpaceController;
import com.google.android.systemui.statusbar.KeyguardIndicationControllerGoogle;

import java.util.Optional;
import java.util.concurrent.Executor;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;

@SysUISingleton
public class CentralSurfacesGoogle extends CentralSurfacesImpl {

    private static final boolean DEBUG = Log.isLoggable("CentralSurfacesGoogle", 3);
    private final BatteryController.BatteryStateChangeCallback mBatteryStateChangeCallback;
    private final KeyguardIndicationControllerGoogle mKeyguardIndicationController;
    private final WallpaperNotifier mWallpaperNotifier;
    private final Optional<ReverseChargingViewController> mReverseChargingViewControllerOptional;
    private final SysuiStatusBarStateController mStatusBarStateController;
    private final SmartSpaceController mSmartSpaceController;
    private final NotificationLockscreenUserManagerGoogle mNotificationLockscreenUserManagerGoogle;

    private long mAnimStartTime;
    private int mReceivingBatteryLevel;
    private boolean mReverseChargingAnimShown;
    private boolean mChargingAnimShown;
    private Context mContext;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject
    public CentralSurfacesGoogle(
            Context context,
            NotificationsController notificationsController,
            FragmentService fragmentService,
            LightBarController lightBarController,
            AutoHideController autoHideController,
            StatusBarWindowController statusBarWindowController,
            StatusBarWindowStateController statusBarWindowStateController,
            KeyguardUpdateMonitor keyguardUpdateMonitor,
            StatusBarSignalPolicy statusBarSignalPolicy,
            PulseExpansionHandler pulseExpansionHandler,
            NotificationWakeUpCoordinator notificationWakeUpCoordinator,
            KeyguardBypassController keyguardBypassController,
            KeyguardStateController keyguardStateController,
            HeadsUpManagerPhone headsUpManagerPhone,
            DynamicPrivacyController dynamicPrivacyController,
            FalsingManager falsingManager,
            FalsingCollector falsingCollector,
            BroadcastDispatcher broadcastDispatcher,
            NotificationGutsManager notificationGutsManager,
            NotificationLogger notificationLogger,
            NotificationInterruptStateProvider notificationInterruptStateProvider,
            ShadeExpansionStateManager shadeExpansionStateManager,
            KeyguardViewMediator keyguardViewMediator,
            DisplayMetrics displayMetrics,
            MetricsLogger metricsLogger,
            @UiBackground Executor uiBgExecutor,
            NotificationMediaManager notificationMediaManager,
            NotificationLockscreenUserManagerGoogle notificationLockscreenUserManagerGoogle,
            NotificationRemoteInputManager remoteInputManager,
            UserSwitcherController userSwitcherController,
            BatteryController batteryController,
            SysuiColorExtractor colorExtractor,
            ScreenLifecycle screenLifecycle,
            WakefulnessLifecycle wakefulnessLifecycle,
            SysuiStatusBarStateController statusBarStateController,
            Optional<Bubbles> bubblesOptional,
            DeviceProvisionedController deviceProvisionedController,
            NavigationBarController navigationBarController,
            AccessibilityFloatingMenuController accessibilityFloatingMenuController,
            Lazy<AssistManager> assistManagerLazy,
            ConfigurationController configurationController,
            NotificationShadeWindowController notificationShadeWindowController,
            DozeParameters dozeParameters,
            ScrimController scrimController,
            Lazy<LockscreenWallpaper> lockscreenWallpaperLazy,
            Lazy<BiometricUnlockController> biometricUnlockControllerLazy,
            DozeServiceHost dozeServiceHost,
            PowerManager powerManager,
            ScreenPinningRequest screenPinningRequest,
            DozeScrimController dozeScrimController,
            VolumeComponent volumeComponent,
            CommandQueue commandQueue,
            CentralSurfacesComponent.Factory centralSurfacesComponentFactory,
            PluginManager pluginManager,
            ShadeController shadeController,
            StatusBarKeyguardViewManager statusBarKeyguardViewManager,
            ViewMediatorCallback viewMediatorCallback,
            InitController initController,
            @Named(TIME_TICK_HANDLER_NAME) Handler timeTickHandler,
            PluginDependencyProvider pluginDependencyProvider,
            KeyguardDismissUtil keyguardDismissUtil,
            ExtensionController extensionController,
            UserInfoControllerImpl userInfoControllerImpl,
            PhoneStatusBarPolicy phoneStatusBarPolicy,
            KeyguardIndicationController keyguardIndicationController,
            DemoModeController demoModeController,
            Lazy<NotificationShadeDepthController> notificationShadeDepthControllerLazy,
            StatusBarTouchableRegionManager statusBarTouchableRegionManager,
            NotificationIconAreaController notificationIconAreaController,
            BrightnessSliderController.Factory brightnessSliderFactory,
            ScreenOffAnimationController screenOffAnimationController,
            WallpaperController wallpaperController,
            OngoingCallController ongoingCallController,
            StatusBarHideIconsForBouncerManager statusBarHideIconsForBouncerManager,
            LockscreenShadeTransitionController lockscreenShadeTransitionController,
            FeatureFlags featureFlags,
            KeyguardUnlockAnimationController keyguardUnlockAnimationController,
            @Main DelayableExecutor delayableExecutor,
            @Main MessageRouter messageRouter,
            WallpaperManager wallpaperManager,
            Optional<StartingSurface> startingSurfaceOptional,
            ActivityLaunchAnimator activityLaunchAnimator,
            InteractionJankMonitor jankMonitor,
            DeviceStateManager deviceStateManager,
            WiredChargingRippleController wiredChargingRippleController,
            IDreamManager dreamManager,
            Lazy<CameraLauncher> cameraLauncherLazy,
            Lazy<LightRevealScrimViewModel> lightRevealScrimViewModelLazy,
            WallpaperNotifier wallpaperNotifier,
            SmartSpaceController smartSpaceController,
            Optional<ReverseChargingViewController> reverseChargingViewControllerOptional,
            KeyguardIndicationControllerGoogle keyguardIndicationControllerGoogle,
            TunerService tunerService) {
        super(context, notificationsController, fragmentService, lightBarController,
                autoHideController, statusBarWindowController, statusBarWindowStateController,
                keyguardUpdateMonitor, statusBarSignalPolicy, pulseExpansionHandler,
                notificationWakeUpCoordinator, keyguardBypassController, keyguardStateController,
                headsUpManagerPhone, dynamicPrivacyController, falsingManager, falsingCollector,
                broadcastDispatcher, notificationGutsManager, notificationLogger, notificationInterruptStateProvider,
                shadeExpansionStateManager, keyguardViewMediator,
                displayMetrics, metricsLogger, uiBgExecutor, notificationMediaManager,
                notificationLockscreenUserManagerGoogle, remoteInputManager, userSwitcherController,
                batteryController, colorExtractor, screenLifecycle,
                wakefulnessLifecycle, statusBarStateController,
                bubblesOptional, deviceProvisionedController,
                navigationBarController, accessibilityFloatingMenuController, assistManagerLazy,
                configurationController, notificationShadeWindowController, dozeParameters,
                scrimController, lockscreenWallpaperLazy,
                biometricUnlockControllerLazy, dozeServiceHost, powerManager, screenPinningRequest,
                dozeScrimController, volumeComponent, commandQueue, centralSurfacesComponentFactory,
                pluginManager, shadeController, statusBarKeyguardViewManager, viewMediatorCallback,
                initController, timeTickHandler, pluginDependencyProvider, keyguardDismissUtil,
                extensionController, userInfoControllerImpl, phoneStatusBarPolicy,
                keyguardIndicationController, demoModeController,
                notificationShadeDepthControllerLazy, statusBarTouchableRegionManager,
                notificationIconAreaController, brightnessSliderFactory,
                screenOffAnimationController, wallpaperController, ongoingCallController,
                statusBarHideIconsForBouncerManager, lockscreenShadeTransitionController,
                featureFlags, keyguardUnlockAnimationController, delayableExecutor,
                messageRouter, wallpaperManager, startingSurfaceOptional, activityLaunchAnimator,
                jankMonitor, deviceStateManager, wiredChargingRippleController,
                dreamManager, cameraLauncherLazy, lightRevealScrimViewModelLazy, tunerService);
        mContext = context;
        mBatteryStateChangeCallback = new BatteryController.BatteryStateChangeCallback() {
            @Override
            public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
                mReceivingBatteryLevel = i;
                if (!mBatteryController.isWirelessCharging()) {
                    if (SystemClock.uptimeMillis() - mAnimStartTime > 1500) {
                        mChargingAnimShown = false;
                    }
                    mReverseChargingAnimShown = false;
                }
                if (DEBUG) {
                    Log.d("CentralSurfacesGoogle", "onBatteryLevelChanged(): level=" + i + ",wlc=" + (mBatteryController.isWirelessCharging() ? 1 : 0) + ",wlcs=" + mChargingAnimShown + ",rtxs=" + mReverseChargingAnimShown + ",this=" + this);
                }
            }

            @Override
            public void onReverseChanged(boolean z, int i, String str) {
                if (!z && i >= 0 && !TextUtils.isEmpty(str) && mBatteryController.isWirelessCharging() && mChargingAnimShown && !mReverseChargingAnimShown) {
                    mReverseChargingAnimShown = true;
                    long uptimeMillis = SystemClock.uptimeMillis() - mAnimStartTime;
                    long j = uptimeMillis > 1500 ? 0L : 1500 - uptimeMillis;
                    showChargingAnimation(mReceivingBatteryLevel, i, j);
                }
                if (DEBUG) {
                    Log.d("CentralSurfacesGoogle", "onReverseChanged(): rtx=" + (z ? 1 : 0) + ",rxlevel=" + mReceivingBatteryLevel + ",level=" + i + ",name=" + str + ",wlc=" + (mBatteryController.isWirelessCharging() ? 1 : 0) + ",wlcs=" + mChargingAnimShown + ",rtxs=" + mReverseChargingAnimShown + ",this=" + this);
                }
            }
        };
        mReverseChargingViewControllerOptional = reverseChargingViewControllerOptional;
        mKeyguardIndicationController = keyguardIndicationControllerGoogle;
        mStatusBarStateController = statusBarStateController;
        mWallpaperNotifier = wallpaperNotifier;
        mSmartSpaceController = smartSpaceController;
        mNotificationLockscreenUserManagerGoogle = notificationLockscreenUserManagerGoogle;
    }

    @Override
    public void start() {
        super.start();
        mWallpaperNotifier.attach();
        mBatteryController.observe(getLifecycle(), mBatteryStateChangeCallback);
        DockObserver dockObserver = (DockObserver) Dependency.get(DockManager.class);
        dockObserver.setDreamlinerGear((ImageView) mNotificationShadeWindowView.findViewById(R.id.dreamliner_gear));
        dockObserver.setPhotoPreview((FrameLayout) mNotificationShadeWindowView.findViewById(R.id.photo_preview));
        dockObserver.setIndicationController(new DockIndicationController(mContext, mKeyguardIndicationController, mStatusBarStateController, this));
        dockObserver.registerDockAlignInfo();
        if (mReverseChargingViewControllerOptional.isPresent()) {
            mReverseChargingViewControllerOptional.get().initialize();
        }
        mNotificationLockscreenUserManagerGoogle.updateSmartSpaceVisibilitySettings();
    }

    @Override
    public void showWirelessChargingAnimation(int i) {
        if (DEBUG) {
            Log.d("CentralSurfacesGoogle", "showWirelessChargingAnimation()");
        }
        mChargingAnimShown = true;
        super.showWirelessChargingAnimation(i);
        mAnimStartTime = SystemClock.uptimeMillis();
    }

    @Override
    public void setLockscreenUser(int i) {
        super.setLockscreenUser(i);
        mSmartSpaceController.reloadData();
    }
}
