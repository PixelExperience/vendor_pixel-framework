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

import android.content.Context;
import android.os.UserManager;
import android.view.View;

import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.statusbar.phone.CentralSurfaces;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.Lazy;

@SysUISingleton
public class OpaEnabledDispatcher implements OpaEnabledListener {
    private final Lazy<CentralSurfaces> mCentralSurfacesLazy;

    @Inject
    public OpaEnabledDispatcher(Lazy<CentralSurfaces> centralSurfacesLazy) {
        mCentralSurfacesLazy = centralSurfacesLazy;
    }

    @Override
    public void onOpaEnabledReceived(Context context, boolean z, boolean z2, boolean z3, boolean z4) {
        dispatchUnchecked((z4 && z && z2) || UserManager.isDeviceInDemoMode(context));
    }

    private void dispatchUnchecked(boolean z) {
        CentralSurfaces centralSurfaces = this.mCentralSurfacesLazy.get();
        if (centralSurfaces.getNavigationBarView() != null) {
            ArrayList<View> views = centralSurfaces.getNavigationBarView().getHomeButton().getViews();
            for (int i = 0; i < views.size(); i++) {
                ((OpaLayout) views.get(i)).setOpaEnabled(z);
            }
        }
    }
}
