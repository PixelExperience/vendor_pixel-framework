/*
 * Copyright (C) 2022 The PixelExperience Project
 *               2023 The RisingOS Android Project
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

package com.google.android.systemui.power

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Settings
import android.util.KeyValueListParser
import android.util.Log
import com.android.settingslib.fuelgauge.Estimate
import com.android.settingslib.utils.PowerUtil
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.power.EnhancedEstimates
import java.time.Duration
import javax.inject.Inject

@SysUISingleton
class EnhancedEstimatesGoogleImpl @Inject constructor(private val mContext: Context) : EnhancedEstimates {
    private val mParser = KeyValueListParser(',')

    override fun isHybridNotificationEnabled(): Boolean {
        return try {
            mContext.packageManager.getPackageInfo("com.google.android.apps.turbo", 512).applicationInfo.enabled &&
                    updateFlags()
            mParser.getBoolean("hybrid_enabled", true)
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun getEstimate(): Estimate {
        var query: Cursor? = null
        return try {
            query = mContext.contentResolver.query(
                Uri.Builder()
                    .scheme("content")
                    .authority("com.google.android.apps.turbo.estimated_time_remaining")
                    .appendPath("time_remaining")
                    .build(),
                null,
                null,
                null,
                null
            )
            if (query != null && query.moveToFirst()) {
                val isBasedOnUsage = query.getColumnIndex("is_based_on_usage") == -1 || query.getInt(query.getColumnIndex("is_based_on_usage")) != 0
                val columnIndex = query.getColumnIndex("average_battery_life")
                val j: Long = if (columnIndex != -1) {
                    val j2 = query.getLong(columnIndex)
                    if (j2 != -1L) {
                        val millis = if (Duration.ofMillis(j2).compareTo(Duration.ofDays(1L)) >= 0) {
                            Duration.ofHours(1L).toMillis()
                        } else {
                            Duration.ofMinutes(15L).toMillis()
                        }
                        PowerUtil.roundTimeToNearestThreshold(j2, millis)
                    } else {
                        -1L
                    }
                } else {
                    -1L
                }
                val estimate = Estimate(query.getLong(query.getColumnIndex("battery_estimate")), isBasedOnUsage, j)
                Estimate.storeCachedEstimate(mContext, estimate)
                estimate
            } else {
                Estimate(-1L, false, -1L)
            }
        } catch (e: Exception) {
            Log.d("EnhancedEstimates", "Something went wrong when getting an estimate from Turbo", e)
            Estimate(-1L, false, -1L)
        } finally {
            query?.close()
        }
    }

    override fun getLowWarningThreshold(): Long {
        updateFlags()
        return mParser.getLong("low_threshold", Duration.ofHours(3L).toMillis())
    }

    override fun getSevereWarningThreshold(): Long {
        updateFlags()
        return mParser.getLong("severe_threshold", Duration.ofHours(1L).toMillis())
    }

    override fun getLowWarningEnabled(): Boolean {
        updateFlags()
        return mParser.getBoolean("low_warning_enabled", false)
    }

    private fun updateFlags(): Boolean {
        return try {
            mParser.setString(Settings.Global.getString(mContext.contentResolver, "hybrid_sysui_battery_warning_flags"))
            true
        } catch (e: IllegalArgumentException) {
            Log.e("EnhancedEstimates", "Bad hybrid sysui warning flags")
            false
        }
    }
}
