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

package com.google.android.systemui.statusbar.policy;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;

import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.demomode.DemoModeController;
import com.android.systemui.power.EnhancedEstimates;
import com.android.systemui.settings.UserContentResolverProvider;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryControllerImpl;
import com.google.android.systemui.reversecharging.ReverseChargingController;
import com.google.android.systemui.reversecharging.ReverseChargingChangeCallback;

import java.util.ArrayList;

public class BatteryControllerImplGoogle extends BatteryControllerImpl implements ReverseChargingChangeCallback {
    static final String EBS_STATE_AUTHORITY = "com.google.android.flipendo.api";
    static final Uri IS_EBS_ENABLED_OBSERVABLE_URI = Uri.parse("content://com.google.android.flipendo.api/get_flipendo_state");
    private static final boolean DEBUG = Log.isLoggable("BatteryControllerGoogle", 3);
    protected final ContentObserver mContentObserver;
    private final UserContentResolverProvider mContentResolverProvider;
    private final ReverseChargingController mReverseChargingController;
    private boolean mExtremeSaver;
    private String mName;
    private boolean mReverse;
    private int mRtxLevel;

    public BatteryControllerImplGoogle(Context context, EnhancedEstimates enhancedEstimates, PowerManager powerManager, BroadcastDispatcher broadcastDispatcher, DemoModeController demoModeController, Handler handler, Handler handler2, UserContentResolverProvider userContentResolverProvider, ReverseChargingController reverseChargingController) {
        super(context, enhancedEstimates, powerManager, broadcastDispatcher, demoModeController, handler, handler2);
        mReverseChargingController = reverseChargingController;
        mContentResolverProvider = userContentResolverProvider;
        mContentObserver = new ContentObserver(handler2) {
            @Override
            public void onChange(boolean z, Uri uri) {
                if (BatteryControllerImplGoogle.DEBUG) {
                    Log.d("BatteryControllerGoogle", "Change in EBS value " + uri.toSafeString());
                }
                setExtremeSaver(isExtremeBatterySaving());
            }
        };
    }

    @Override
    public void init() {
        super.init();
        resetReverseInfo();
        mReverseChargingController.init(this);
        mReverseChargingController.addCallback(this);
        try {
            ContentResolver userContentResolver = mContentResolverProvider.getUserContentResolver();
            Uri uri = IS_EBS_ENABLED_OBSERVABLE_URI;
            userContentResolver.registerContentObserver(uri, DEBUG, mContentObserver, -1);
            mContentObserver.onChange(DEBUG, uri);
        } catch (Exception e) {
            Log.w("BatteryControllerGoogle", "Couldn't register to observe provider", e);
        }
    }

    @Override
    public void onReverseChargingChanged(boolean z, int i, String str) {
        mReverse = z;
        mRtxLevel = i;
        mName = str;
        if (DEBUG) {
            Log.d("BatteryControllerGoogle", "onReverseChargingChanged(): rtx=" + (z ? 1 : 0) + " level=" + i + " name=" + str + " this=" + this);
        }
        fireReverseChanged();
    }

    @Override
    public void addCallback(BatteryController.BatteryStateChangeCallback batteryStateChangeCallback) {
        super.addCallback(batteryStateChangeCallback);
        batteryStateChangeCallback.onReverseChanged(mReverse, mRtxLevel, mName);
        batteryStateChangeCallback.onExtremeBatterySaverChanged(mExtremeSaver);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        mReverseChargingController.handleIntentForReverseCharging(intent);
    }

    @Override
    public boolean isReverseSupported() {
        return mReverseChargingController.isReverseSupported();
    }

    @Override
    public boolean isReverseOn() {
        return mReverse;
    }

    @Override
    public void setReverseState(boolean z) {
        mReverseChargingController.setReverseState(z);
    }

    private void resetReverseInfo() {
        mReverse = false;
        mRtxLevel = -1;
        mName = null;
    }

    private void setExtremeSaver(boolean z) {
        if (z == mExtremeSaver) {
            return;
        }
        mExtremeSaver = z;
        fireExtremeSaverChanged();
    }

    private void fireExtremeSaverChanged() {
        synchronized (mChangeCallbacks) {
            int size = mChangeCallbacks.size();
            for (int i = 0; i < size; i++) {
                mChangeCallbacks.get(i).onExtremeBatterySaverChanged(mExtremeSaver);
            }
        }
    }

    private void fireReverseChanged() {
        synchronized (mChangeCallbacks) {
            ArrayList arrayList = new ArrayList(mChangeCallbacks);
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                ((BatteryController.BatteryStateChangeCallback) arrayList.get(i)).onReverseChanged(mReverse, mRtxLevel, mName);
            }
        }
    }

    private boolean isExtremeBatterySaving() {
        Bundle bundle;
        try {
            bundle = mContentResolverProvider.getUserContentResolver().call(EBS_STATE_AUTHORITY, "get_flipendo_state", (String) null, new Bundle());
        } catch (IllegalArgumentException unused) {
            bundle = new Bundle();
        }
        return bundle.getBoolean("flipendo_state", DEBUG);
    }
}
