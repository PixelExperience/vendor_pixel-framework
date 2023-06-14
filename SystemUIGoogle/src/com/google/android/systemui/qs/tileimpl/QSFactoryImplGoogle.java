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

package com.google.android.systemui.qs.tileimpl;

import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.tileimpl.QSFactoryImpl;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.util.leak.GarbageMonitor;

import javax.inject.Inject;
import javax.inject.Provider;

import dagger.Lazy;
import java.util.Map;

/**
 * A factory that creates Quick Settings tiles based on a tileSpec
 *
 * To create a new tile within SystemUI, the tile class should extend {@link QSTileImpl} and have
 * a public static final TILE_SPEC field which serves as a unique key for this tile. (e.g. {@link
 * com.android.systemui.qs.tiles.DreamTile#TILE_SPEC})
 *
 * After, create or find an existing Module class to house the tile's binding method (e.g. {@link
 * com.android.systemui.accessibility.AccessibilityModule}). If creating a new module, add your
 * module to the SystemUI dagger graph by including it in an appropriate module.
 */

@SysUISingleton
public class QSFactoryImplGoogle extends QSFactoryImpl {

    @Inject
    public QSFactoryImplGoogle(
            Lazy<QSHost> qsHostLazy,
            Provider<CustomTile.Builder> customTileBuilderProvider,
            Map<String, Provider<QSTileImpl<?>>> tileMap) {
        super(qsHostLazy,
                customTileBuilderProvider, tileMap);
   }
}
