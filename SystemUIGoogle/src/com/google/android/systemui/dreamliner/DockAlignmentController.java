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

package com.google.android.systemui.dreamliner;

import android.util.Log;

import com.google.android.systemui.dreamliner.WirelessCharger;

public class DockAlignmentController {
    private static final boolean DEBUG = Log.isLoggable("DockAlignmentController", 3);
    private final DockObserver mDockObserver;
    private final WirelessCharger mWirelessCharger;
    private int mAlignmentState = 0;

    public DockAlignmentController(WirelessCharger wirelessCharger, DockObserver dockObserver) {
        mWirelessCharger = wirelessCharger;
        mDockObserver = dockObserver;
    }

    void registerAlignInfoListener() {
        WirelessCharger wirelessCharger = mWirelessCharger;
        if (wirelessCharger == null) {
            Log.w("DockAlignmentController", "wirelessCharger is null");
        } else {
            wirelessCharger.registerAlignInfo(new RegisterAlignInfoListener());
        }
    }

    private void onAlignInfoCallBack(DockAlignInfo dockAlignInfo) {
        int i = mAlignmentState;
        int alignmentState = getAlignmentState(dockAlignInfo);
        mAlignmentState = alignmentState;
        if (i != alignmentState) {
            mDockObserver.onAlignStateChanged(alignmentState);
            if (DEBUG) {
                Log.d("DockAlignmentController", "onAlignStateChanged, state: " + mAlignmentState);
            }
        }
        mDockObserver.onFanLevelChange();
    }

    private int getAlignmentState(DockAlignInfo dockAlignInfo) {
        if (DEBUG) {
            Log.d("DockAlignmentController", "onAlignInfo, state: " + dockAlignInfo.getAlignState() + ", alignPct: " + dockAlignInfo.getAlignPct());
        }
        int i = mAlignmentState;
        int alignState = dockAlignInfo.getAlignState();
        if (alignState != 0) {
            if (alignState == 1) {
                return 2;
            }
            if (alignState == 2) {
                int alignPct = dockAlignInfo.getAlignPct();
                if (alignPct >= 0) {
                    return alignPct < 100 ? 1 : 0;
                }
            } else if (alignState != 3) {
                Log.w("DockAlignmentController", "Unexpected state: " + dockAlignInfo.getAlignState());
            }
            return -1;
        }
        return i;
    }

    private final class RegisterAlignInfoListener implements WirelessCharger.AlignInfoListener {
        private RegisterAlignInfoListener() {
        }

        @Override
        public void onAlignInfoChanged(DockAlignInfo dockAlignInfo) {
            onAlignInfoCallBack(dockAlignInfo);
        }
    }
}
