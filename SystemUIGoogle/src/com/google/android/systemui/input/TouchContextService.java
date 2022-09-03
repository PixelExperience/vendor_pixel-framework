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

package com.google.android.systemui.input;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.Display;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.BackgroundThread;
import com.google.input.ContextPacket;
import com.google.input.ITouchContextService;

public class TouchContextService implements DisplayManager.DisplayListener {
    private static final String INTERFACE = ITouchContextService.DESCRIPTOR + "/default";
    private final DisplayManager mDm;
    private final Object mLock = new Object();
    @GuardedBy({"mLock"})
    private ITouchContextService mService;
    private int mLastRotation = -1;

    public TouchContextService(Context context) {
        DisplayManager displayManager = (DisplayManager) context.getSystemService(DisplayManager.class);
        mDm = displayManager;
        if (!ServiceManager.isDeclared(INTERFACE)) {
            Log.d("TouchContextService", "No ITouchContextService declared in manifest, not sending input context");
            return;
        }
        Handler handler = BackgroundThread.getHandler();
        displayManager.registerDisplayListener(this, handler);
        handler.post((Runnable) () -> onDisplayChanged(0));
    }

    private static byte toOrientation(int i, int i2, int i3) {
        byte b = (byte) i;
        return i2 > i3 ? (byte) ((b + 1) % 4) : b;
    }

    @Override
    public void onDisplayAdded(int i) {
    }

    @Override
    public void onDisplayRemoved(int i) {
    }

    @Override
    public void onDisplayChanged(int i) {
        Display display;
        int rotation;
        if (i != 0 || (display = mDm.getDisplay(i)) == null || (rotation = display.getRotation()) == mLastRotation) {
            return;
        }
        Display.Mode mode = display.getMode();
        ContextPacket contextPacket = new ContextPacket();
        contextPacket.orientation = toOrientation(rotation, mode.getPhysicalWidth(), mode.getPhysicalHeight());
        ITouchContextService touchContextService = getTouchContextService();
        if (touchContextService == null) {
            Log.e("TouchContextService", "Failed to get touch context service, dropping context packet.");
            return;
        }
        try {
            touchContextService.updateContext(contextPacket);
            mLastRotation = rotation;
        } catch (RemoteException e) {
            Log.e("TouchContextService", "Failed to send input context packet.", e);
        }
    }

    private ITouchContextService getTouchContextService() {
        if (mService != null) {
            return mService;
        }
        final IBinder service = ServiceManager.getService(INTERFACE);
        if (service == null) {
            Log.e("TouchContextService", "Failed to get ITouchContextService despite being declared.");
            return null;
        }
        try {
            service.linkToDeath(() -> {
                synchronized (mLock) {
                    if (mService.asBinder() == service) {
                        mService = null;
                    }
                }
            }, 0);
            mService = ITouchContextService.Stub.asInterface(service);
            return mService;
        } catch (RemoteException e) {
            Log.e("TouchContextService", "Failed to link to death on ITouchContextService. Binder is probably dead.", e);
            return null;
        }
    }
}
