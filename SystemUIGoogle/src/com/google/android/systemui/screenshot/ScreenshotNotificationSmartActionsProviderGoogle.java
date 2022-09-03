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

package com.google.android.systemui.screenshot;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import com.android.systemui.screenshot.ScreenshotNotificationSmartActionsProvider;
import com.google.android.apps.miphone.aiai.matchmaker.overview.api.generatedv2.FeedbackParcelables_ScreenshotOp;
import com.google.android.apps.miphone.aiai.matchmaker.overview.api.generatedv2.FeedbackParcelables_ScreenshotOpStatus;
import com.google.android.apps.miphone.aiai.matchmaker.overview.api.generatedv2.SuggestParcelables_InteractionType;
import com.google.android.apps.miphone.aiai.matchmaker.overview.ui.ContentSuggestionsServiceClient;
import com.google.android.apps.miphone.aiai.matchmaker.overview.ui.ContentSuggestionsServiceWrapper;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class ScreenshotNotificationSmartActionsProviderGoogle extends ScreenshotNotificationSmartActionsProvider {
    private final ContentSuggestionsServiceClient mClient;
    private static final ImmutableMap SCREENSHOT_OP_MAP = ImmutableMap.builder().put(ScreenshotNotificationSmartActionsProvider.ScreenshotOp.RETRIEVE_SMART_ACTIONS, FeedbackParcelables_ScreenshotOp.RETRIEVE_SMART_ACTIONS).put(ScreenshotNotificationSmartActionsProvider.ScreenshotOp.REQUEST_SMART_ACTIONS, FeedbackParcelables_ScreenshotOp.REQUEST_SMART_ACTIONS).put(ScreenshotNotificationSmartActionsProvider.ScreenshotOp.WAIT_FOR_SMART_ACTIONS, FeedbackParcelables_ScreenshotOp.WAIT_FOR_SMART_ACTIONS).build();
    private static final ImmutableMap SCREENSHOT_OP_STATUS_MAP = ImmutableMap.builder().put(ScreenshotNotificationSmartActionsProvider.ScreenshotOpStatus.SUCCESS, FeedbackParcelables_ScreenshotOpStatus.SUCCESS).put(ScreenshotNotificationSmartActionsProvider.ScreenshotOpStatus.ERROR, FeedbackParcelables_ScreenshotOpStatus.ERROR).put(ScreenshotNotificationSmartActionsProvider.ScreenshotOpStatus.TIMEOUT, FeedbackParcelables_ScreenshotOpStatus.TIMEOUT).build();
    private static final ImmutableMap SCREENSHOT_INTERACTION_TYPE_MAP = ImmutableMap.builder().put(ScreenshotNotificationSmartActionsProvider.ScreenshotSmartActionType.REGULAR_SMART_ACTIONS, SuggestParcelables_InteractionType.SCREENSHOT_NOTIFICATION).put(ScreenshotNotificationSmartActionsProvider.ScreenshotSmartActionType.QUICK_SHARE_ACTION, SuggestParcelables_InteractionType.QUICK_SHARE).build();

    public ScreenshotNotificationSmartActionsProviderGoogle(Context context, Executor executor, Handler handler) {
        mClient = new ContentSuggestionsServiceClient(context, executor, handler);
    }

    @Override
    public CompletableFuture<List<Notification.Action>> getActions(final String str, Uri uri, Bitmap bitmap, ComponentName componentName, ScreenshotNotificationSmartActionsProvider.ScreenshotSmartActionType screenshotSmartActionType, UserHandle userHandle) {
        final CompletableFuture<List<Notification.Action>> completableFuture = new CompletableFuture<>();
        if (bitmap.getConfig() != Bitmap.Config.HARDWARE) {
            Log.e("ScreenshotActionsGoogle", String.format("Bitmap expected: Hardware, Bitmap found: %s. Returning empty list.", bitmap.getConfig()));
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        final long uptimeMillis = SystemClock.uptimeMillis();
        Log.d("ScreenshotActionsGoogle", "Calling AiAi to obtain screenshot notification smart actions.");
        mClient.provideScreenshotActions(bitmap, uri, componentName.getPackageName(), componentName.getClassName(), userHandle, (SuggestParcelables_InteractionType) SCREENSHOT_INTERACTION_TYPE_MAP.getOrDefault(screenshotSmartActionType, SuggestParcelables_InteractionType.SCREENSHOT_NOTIFICATION), new ContentSuggestionsServiceWrapper.BundleCallback() {
            @Override
            public void onResult(Bundle bundle) {
                completeFuture(bundle, completableFuture);
                long uptimeMillis2 = SystemClock.uptimeMillis() - uptimeMillis;
                Log.d("ScreenshotActionsGoogle", String.format("Total time taken to get smart actions: %d ms", Long.valueOf(uptimeMillis2)));
                notifyOp(str, ScreenshotNotificationSmartActionsProvider.ScreenshotOp.RETRIEVE_SMART_ACTIONS, ScreenshotNotificationSmartActionsProvider.ScreenshotOpStatus.SUCCESS, uptimeMillis2);
            }
        });
        return completableFuture;
    }

    @Override
    public void notifyOp(String str, ScreenshotNotificationSmartActionsProvider.ScreenshotOp screenshotOp, ScreenshotNotificationSmartActionsProvider.ScreenshotOpStatus screenshotOpStatus, long j) {
        mClient.notifyOp(str,
            (FeedbackParcelables_ScreenshotOp) SCREENSHOT_OP_MAP.getOrDefault(screenshotOp, FeedbackParcelables_ScreenshotOp.OP_UNKNOWN),
            (FeedbackParcelables_ScreenshotOpStatus) SCREENSHOT_OP_STATUS_MAP.getOrDefault(screenshotOpStatus, FeedbackParcelables_ScreenshotOpStatus.OP_STATUS_UNKNOWN), j);
    }

    @Override
    public void notifyAction(String str, String str2, boolean z, Intent intent) {
        mClient.notifyAction(str, str2, z, intent);
    }

    void completeFuture(Bundle bundle, CompletableFuture<List<Notification.Action>> completableFuture) {
        if (bundle.containsKey("ScreenshotNotificationActions")) {
            completableFuture.complete(bundle.getParcelableArrayList("ScreenshotNotificationActions"));
        } else {
            completableFuture.complete(Collections.emptyList());
        }
    }
}
