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

package com.google.android.systemui.assist.dagger;

import static org.pixelexperience.systemui.Dependency.*;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.android.internal.app.AssistUtils;
import com.android.systemui.assist.AssistLogger;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.navigationbar.NavigationBarController;
import com.android.systemui.navigationbar.NavigationModeController;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.phone.CentralSurfaces;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.google.android.systemui.assist.uihints.AssistantPresenceHandler;
import com.google.android.systemui.assist.uihints.AssistantWarmer;
import com.google.android.systemui.assist.uihints.ColorChangeHandler;
import com.google.android.systemui.assist.uihints.ConfigurationHandler;
import com.google.android.systemui.assist.uihints.FlingVelocityWrapper;
import com.google.android.systemui.assist.uihints.GlowController;
import com.google.android.systemui.assist.uihints.GoBackHandler;
import com.google.android.systemui.assist.uihints.GoogleDefaultUiController;
import com.google.android.systemui.assist.uihints.IconController;
import com.google.android.systemui.assist.uihints.KeyboardMonitor;
import com.google.android.systemui.assist.uihints.LightnessProvider;
import com.google.android.systemui.assist.uihints.NavBarFader;
import com.google.android.systemui.assist.uihints.NgaMessageHandler;
import com.google.android.systemui.assist.uihints.NgaUiController;
import com.google.android.systemui.assist.uihints.OverlappedElementController;
import com.google.android.systemui.assist.uihints.OverlayUiHost;
import com.google.android.systemui.assist.uihints.ScrimController;
import com.google.android.systemui.assist.uihints.TakeScreenshotHandler;
import com.google.android.systemui.assist.uihints.TaskStackNotifier;
import com.google.android.systemui.assist.uihints.TimeoutManager;
import com.google.android.systemui.assist.uihints.TouchInsideHandler;
import com.google.android.systemui.assist.uihints.TouchOutsideHandler;
import com.google.android.systemui.assist.uihints.TranscriptionController;
import com.google.android.systemui.assist.uihints.edgelights.EdgeLightsController;
import com.google.android.systemui.assist.uihints.input.NgaInputHandler;
import com.google.android.systemui.assist.uihints.input.TouchActionRegion;
import com.google.android.systemui.assist.uihints.input.TouchInsideRegion;
import com.google.android.systemui.statusbar.phone.CentralSurfacesGoogle;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Named;

import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;

@Module
public abstract class AssistModule {
    @Provides
    @SysUISingleton
    static NavBarFader provideNavBarFader(NavigationBarController navigationBarController, Handler handler) {
        return new NavBarFader(navigationBarController, handler);
    }

    @Provides
    @SysUISingleton
    static FlingVelocityWrapper provideFlingVelocityWrapper() {
        return new FlingVelocityWrapper();
    }

    @Provides
    @SysUISingleton
    static TouchInsideHandler provideTouchInsideHandler(Lazy<AssistManager> lazy, NavigationModeController navigationModeController, AssistLogger assistLogger) {
        return new TouchInsideHandler(lazy, navigationModeController, assistLogger);
    }

    @Provides
    @SysUISingleton
    static OverlappedElementController provideOverlappedElementController(Lazy<CentralSurfacesGoogle> lazy) {
        return new OverlappedElementController(lazy);
    }

    @Provides
    @SysUISingleton
    static ColorChangeHandler provideColorChangeHandler(Context context) {
        return new ColorChangeHandler(context);
    }

    @Provides
    @SysUISingleton
    static IconController provideIconController(LayoutInflater layoutInflater, @Named(OVERLAY_UI_HOST_PARENT_VIEW_GROUP) ViewGroup viewGroup, ConfigurationController configurationController) {
        return new IconController(layoutInflater, viewGroup, configurationController);
    }


    @Provides
    @SysUISingleton
    static AssistantWarmer provideAssistantWarmer(Context context) {
        return new AssistantWarmer(context);
    }

    @Provides
    @SysUISingleton
    static TranscriptionController provideTranscriptionController(@Named(OVERLAY_UI_HOST_PARENT_VIEW_GROUP) ViewGroup viewGroup, TouchInsideHandler touchInsideHandler, FlingVelocityWrapper flingVelocityWrapper, ConfigurationController configurationController) {
        return new TranscriptionController(viewGroup, touchInsideHandler, flingVelocityWrapper, configurationController);
    }

    @Provides
    @SysUISingleton
    static TouchOutsideHandler provideTouchOutsideHandler() {
        return new TouchOutsideHandler();
    }

    @Provides
    @SysUISingleton
    static ConfigurationHandler provideConfigurationHandler(Context context) {
        return new ConfigurationHandler(context);
    }

    @Provides
    @SysUISingleton
    static KeyboardMonitor provideKeyboardMonitor(Context context, Optional<CommandQueue> optional) {
        return new KeyboardMonitor(context, optional);
    }

    @Provides
    @SysUISingleton
    static TaskStackNotifier provideTaskStackNotifier() {
        return new TaskStackNotifier();
    }

    @Provides
    @SysUISingleton
    static TakeScreenshotHandler provideTakeScreenshotHandler(Context context) {
        return new TakeScreenshotHandler(context);
    }

    @Provides
    @SysUISingleton
    static GoBackHandler provideGoBackHandler() {
        return new GoBackHandler();
    }

    @Provides
    @SysUISingleton
    static NgaUiController provideNgaUiController(Context context, TimeoutManager timeoutManager, AssistantPresenceHandler assistantPresenceHandler, TouchInsideHandler touchInsideHandler, ColorChangeHandler colorChangeHandler, OverlayUiHost overlayUiHost, EdgeLightsController edgeLightsController, GlowController glowController, ScrimController scrimController, TranscriptionController transcriptionController, IconController iconController, LightnessProvider lightnessProvider, StatusBarStateController statusBarStateController, Lazy<AssistManager> lazy, FlingVelocityWrapper flingVelocityWrapper, AssistantWarmer assistantWarmer, NavBarFader navBarFader, AssistLogger assistLogger) {
        return new NgaUiController(context, timeoutManager, assistantPresenceHandler, touchInsideHandler, colorChangeHandler, overlayUiHost, edgeLightsController, glowController, scrimController, transcriptionController, iconController, lightnessProvider, statusBarStateController, lazy, flingVelocityWrapper, assistantWarmer, navBarFader, assistLogger);
    }

    @Provides
    @SysUISingleton
    static GlowController provideGlowController(Context context, @Named(OVERLAY_UI_HOST_PARENT_VIEW_GROUP) ViewGroup viewGroup, TouchInsideHandler touchInsideHandler) {
        return new GlowController(context, viewGroup, touchInsideHandler);
    }

    @Provides
    @SysUISingleton
    static NgaMessageHandler provideNgaMessageHandler(NgaUiController ngaUiController, AssistantPresenceHandler assistantPresenceHandler, NavigationModeController navigationModeController, Set<NgaMessageHandler.KeepAliveListener> set, Set<NgaMessageHandler.AudioInfoListener> setB, Set<NgaMessageHandler.CardInfoListener> setC, Set<NgaMessageHandler.ConfigInfoListener> setD, Set<NgaMessageHandler.EdgeLightsInfoListener> setE, Set<NgaMessageHandler.TranscriptionInfoListener> setF, Set<NgaMessageHandler.GreetingInfoListener> setG, Set<NgaMessageHandler.ChipsInfoListener> setH, Set<NgaMessageHandler.ClearListener> setI, Set<NgaMessageHandler.StartActivityInfoListener> setJ, Set<NgaMessageHandler.KeyboardInfoListener> setK, Set<NgaMessageHandler.ZerostateInfoListener> set1B, Set<NgaMessageHandler.GoBackListener> set1C, Set<NgaMessageHandler.TakeScreenshotListener> set1D, Set<NgaMessageHandler.WarmingListener> set1E, Set<NgaMessageHandler.NavBarVisibilityListener> set1F, @Main Handler handler) {
        return new NgaMessageHandler(ngaUiController, assistantPresenceHandler, navigationModeController, set, setB, setC, setD, setE, setF, setG, setH, setI, setJ, setK, set1B, set1C, set1D, set1E, set1F, handler);
    }

    @Provides
    @SysUISingleton
    static ScrimController provideScrimController(@Named(OVERLAY_UI_HOST_PARENT_VIEW_GROUP) ViewGroup viewGroup, OverlappedElementController overlappedElementController, LightnessProvider lightnessProvider, TouchInsideHandler touchInsideHandler) {
        return new ScrimController(viewGroup, overlappedElementController, lightnessProvider, touchInsideHandler);
    }

    @Provides
    @SysUISingleton
    static TimeoutManager provideTimeoutManager(Lazy<AssistManager> lazy) {
        return new TimeoutManager(lazy);
    }

    @Provides
    @SysUISingleton
    static OverlayUiHost provideOverlayUiHost(Context context, TouchOutsideHandler touchOutsideHandler) {
        return new OverlayUiHost(context, touchOutsideHandler);
    }

    @Provides
    @SysUISingleton
    @Named(OVERLAY_UI_HOST_PARENT_VIEW_GROUP)
    static ViewGroup provideParentViewGroup(OverlayUiHost overlayUiHost) {
        return overlayUiHost.getParent();
    }

    @Provides
    @SysUISingleton
    static LightnessProvider provideLightnessProvider() {
        return new LightnessProvider();
    }

    @Provides
    @SysUISingleton
    static EdgeLightsController provideEdgeLightsController(Context context, @Named(OVERLAY_UI_HOST_PARENT_VIEW_GROUP) ViewGroup viewGroup, AssistLogger assistLogger) {
        return new EdgeLightsController(context, viewGroup, assistLogger);
    }

    @Provides
    @SysUISingleton
    static NgaInputHandler provideNgaInputHandler(TouchInsideHandler touchInsideHandler, Set<TouchActionRegion> set, Set<TouchInsideRegion> setB) {
        return new NgaInputHandler(touchInsideHandler, set, setB);
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.AudioInfoListener> provideAudioInfoListeners(EdgeLightsController edgeLightsController, GlowController glowController) {
        return new HashSet(Arrays.asList(edgeLightsController, glowController));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.CardInfoListener> provideCardInfoListeners(GlowController glowController, ScrimController scrimController, TranscriptionController transcriptionController, LightnessProvider lightnessProvider) {
        return new HashSet(Arrays.asList(glowController, scrimController, transcriptionController, lightnessProvider));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.TranscriptionInfoListener> provideTranscriptionInfoListener(TranscriptionController transcriptionController) {
        return new HashSet(Arrays.asList(transcriptionController));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.GreetingInfoListener> provideGreetingInfoListener(TranscriptionController transcriptionController) {
        return new HashSet(Arrays.asList(transcriptionController));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.ChipsInfoListener> provideChipsInfoListener(TranscriptionController transcriptionController) {
        return new HashSet(Arrays.asList(transcriptionController));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.ClearListener> provideClearListener(TranscriptionController transcriptionController) {
        return new HashSet(Arrays.asList(transcriptionController));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.KeyboardInfoListener> provideKeyboardInfoListener(IconController iconController) {
        return new HashSet(Arrays.asList(iconController));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.ZerostateInfoListener> provideZerostateInfoListener(IconController iconController) {
        return new HashSet(Arrays.asList(iconController));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.GoBackListener> provideGoBackListener(GoBackHandler goBackHandler) {
        return new HashSet(Arrays.asList(goBackHandler));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.TakeScreenshotListener> provideTakeScreenshotListener(TakeScreenshotHandler takeScreenshotHandler) {
        return new HashSet(Arrays.asList(takeScreenshotHandler));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.WarmingListener> provideWarmingListener(AssistantWarmer assistantWarmer) {
        return new HashSet(Arrays.asList(assistantWarmer));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.NavBarVisibilityListener> provideNavBarVisibilityListener(NavBarFader navBarFader) {
        return new HashSet(Arrays.asList(navBarFader));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.ConfigInfoListener> provideConfigInfoListeners(AssistantPresenceHandler assistantPresenceHandler, TouchInsideHandler touchInsideHandler, TouchOutsideHandler touchOutsideHandler, TaskStackNotifier taskStackNotifier, KeyboardMonitor keyboardMonitor, ColorChangeHandler colorChangeHandler, ConfigurationHandler configurationHandler) {
        return new HashSet(Arrays.asList(assistantPresenceHandler, touchInsideHandler, touchOutsideHandler, taskStackNotifier, keyboardMonitor, colorChangeHandler, configurationHandler));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.EdgeLightsInfoListener> provideEdgeLightsInfoListeners(EdgeLightsController edgeLightsController, NgaInputHandler ngaInputHandler) {
        return new HashSet(Arrays.asList(edgeLightsController, ngaInputHandler));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.KeepAliveListener> provideKeepAliveListener(TimeoutManager timeoutManager) {
        return new HashSet(Arrays.asList(timeoutManager));
    }

    @Provides
    @ElementsIntoSet
    static Set<NgaMessageHandler.StartActivityInfoListener> provideActivityStarter(final Lazy<CentralSurfacesGoogle> lazy) {
        return new HashSet(Collections.singletonList((NgaMessageHandler.StartActivityInfoListener) (intent, z) -> {
            if (intent == null) {
                Log.e("ActivityStarter", "Null intent; cannot start activity");
            } else {
                lazy.get().startActivity(intent, z);
            }
        }));
    }

    @Provides
    @ElementsIntoSet
    static Set<TouchActionRegion> provideTouchActionRegions(IconController iconController, TranscriptionController transcriptionController) {
        return new HashSet(Arrays.asList(iconController, transcriptionController));
    }

    @Provides
    @ElementsIntoSet
    static Set<TouchInsideRegion> provideTouchInsideRegions(GlowController glowController, ScrimController scrimController, TranscriptionController transcriptionController) {
        return new HashSet(Arrays.asList(glowController, scrimController, transcriptionController));
    }
}
