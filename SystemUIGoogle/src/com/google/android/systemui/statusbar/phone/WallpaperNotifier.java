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

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.notification.NotificationEntryListener;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.google.android.collect.Sets;

import java.util.HashSet;

import javax.inject.Inject;


@SysUISingleton
public class WallpaperNotifier {
    private static final String[] NOTIFYABLE_WALLPAPERS = {"com.breel.wallpapers.imprint", "com.breel.wallpapers18.tactile", "com.breel.wallpapers18.delight", "com.breel.wallpapers18.miniman", "com.google.pixel.livewallpaper.imprint", "com.google.pixel.livewallpaper.tactile", "com.google.pixel.livewallpaper.delight", "com.google.pixel.livewallpaper.miniman"};
    private static final HashSet<String> NOTIFYABLE_PACKAGES = Sets.newHashSet(new String[]{"com.breel.wallpapers", "com.breel.wallpapers18", "com.google.pixel.livewallpaper"});
    private final Context mContext;
    private final NotificationEntryManager mEntryManager;
    private final CurrentUserTracker mUserTracker;
    private boolean mShouldBroadcastNotifications;
    private String mWallpaperPackage;
    private final NotificationEntryListener mEntryListener = new NotificationEntryListener() {
        @Override
        public void onNotificationAdded(NotificationEntry notificationEntry) {
            boolean z = mUserTracker.getCurrentUserId() == 0;
            if (!mShouldBroadcastNotifications || !z) {
                return;
            }
            Intent intent = new Intent();
            intent.setPackage(mWallpaperPackage);
            intent.setAction("com.breel.wallpapers.NOTIFICATION_RECEIVED");
            intent.putExtra("notification_color", notificationEntry.getSbn().getNotification().color);
            mContext.sendBroadcast(intent, "com.breel.wallpapers.notifications");
        }
    };
    private final BroadcastReceiver mWallpaperChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.WALLPAPER_CHANGED")) {
                checkNotificationBroadcastSupport();
            }
        }
    };

    @Inject
    public WallpaperNotifier(Context context, NotificationEntryManager notificationEntryManager, BroadcastDispatcher broadcastDispatcher) {
        mContext = context;
        mEntryManager = notificationEntryManager;
        mUserTracker = new CurrentUserTracker(broadcastDispatcher) {
            @Override
            public void onUserSwitched(int i) {
            }
        };
    }

    public void attach() {
        mEntryManager.addNotificationEntryListener(mEntryListener);
        mContext.registerReceiver(mWallpaperChangedReceiver, new IntentFilter("android.intent.action.WALLPAPER_CHANGED"));
        checkNotificationBroadcastSupport();
    }

    private void checkNotificationBroadcastSupport() {
        WallpaperInfo wallpaperInfo;
        mShouldBroadcastNotifications = false;
        WallpaperManager wallpaperManager = (WallpaperManager) mContext.getSystemService(WallpaperManager.class);
        if (wallpaperManager == null || (wallpaperInfo = wallpaperManager.getWallpaperInfo()) == null) {
            return;
        }
        ComponentName component = wallpaperInfo.getComponent();
        String packageName = component.getPackageName();
        if (!NOTIFYABLE_PACKAGES.contains(packageName)) {
            return;
        }
        mWallpaperPackage = packageName;
        String className = component.getClassName();
        for (String str : NOTIFYABLE_WALLPAPERS) {
            if (className.startsWith(str)) {
                mShouldBroadcastNotifications = true;
                return;
            }
        }
    }
}
