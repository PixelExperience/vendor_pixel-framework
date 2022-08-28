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

package com.google.android.systemui.power;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Log;

import com.android.settingslib.fuelgauge.Estimate;
import com.android.settingslib.utils.PowerUtil;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.power.EnhancedEstimates;

import java.time.Duration;

import javax.inject.Inject;

@SysUISingleton
public class EnhancedEstimatesGoogleImpl implements EnhancedEstimates {
    private final KeyValueListParser mParser = new KeyValueListParser(',');
    private final Context mContext;

    @Inject
    public EnhancedEstimatesGoogleImpl(Context context) {
        mContext = context;
    }

    @Override
    public boolean isHybridNotificationEnabled() {
        try {
            if (!mContext.getPackageManager().getPackageInfo("com.google.android.apps.turbo", 512).applicationInfo.enabled) {
                return false;
            }
            updateFlags();
            return mParser.getBoolean("hybrid_enabled", true);
        } catch (PackageManager.NameNotFoundException unused) {
            return false;
        }
    }

    @Override
    public Estimate getEstimate() {
        Cursor query = null;
        try {
            query = mContext.getContentResolver().query(new Uri.Builder().scheme("content").authority("com.google.android.apps.turbo.estimated_time_remaining").appendPath("time_remaining").build(), null, null, null, null);
        } catch (Exception e) {
            Log.d("EnhancedEstimates", "Something went wrong when getting an estimate from Turbo", e);
        }
        if (query != null && query.moveToFirst()) {
            boolean z = query.getColumnIndex("is_based_on_usage") == -1 || query.getInt(query.getColumnIndex("is_based_on_usage")) != 0;
            boolean z2 = z;
            int columnIndex = query.getColumnIndex("average_battery_life");
            long j = -1;
            if (columnIndex != -1) {
                long j2 = query.getLong(columnIndex);
                if (j2 != -1) {
                    long millis = Duration.ofMinutes(15L).toMillis();
                    if (Duration.ofMillis(j2).compareTo(Duration.ofDays(1L)) >= 0) {
                        millis = Duration.ofHours(1L).toMillis();
                    }
                    j = PowerUtil.roundTimeToNearestThreshold(j2, millis);
                }
            }
            Estimate estimate = new Estimate(query.getLong(query.getColumnIndex("battery_estimate")), z2, j);
            query.close();
            return estimate;
        }
        if (query != null) {
            query.close();
        }
        return new Estimate(-1L, false, -1L);
    }

    @Override
    public long getLowWarningThreshold() {
        updateFlags();
        return mParser.getLong("low_threshold", Duration.ofHours(3L).toMillis());
    }

    @Override
    public long getSevereWarningThreshold() {
        updateFlags();
        return mParser.getLong("severe_threshold", Duration.ofHours(1L).toMillis());
    }

    @Override
    public boolean getLowWarningEnabled() {
        updateFlags();
        return mParser.getBoolean("low_warning_enabled", false);
    }

    protected void updateFlags() {
        try {
            mParser.setString(Settings.Global.getString(mContext.getContentResolver(), "hybrid_sysui_battery_warning_flags"));
        } catch (IllegalArgumentException unused) {
            Log.e("EnhancedEstimates", "Bad hybrid sysui warning flags");
        }
    }
}
