/*
 * Copyright (C) 2023 The PixelExperience Project
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
@file:Suppress("DEPRECATION")
package com.google.android.systemui.keyguard

import android.graphics.*
import android.graphics.Bitmap.Config
import android.graphics.BlurMaskFilter.Blur
import android.graphics.PorterDuff.Mode
import android.net.Uri
import android.os.AsyncTask
import android.text.TextUtils
import android.util.Log
import androidx.core.graphics.drawable.IconCompat
import androidx.slice.Slice
import androidx.slice.builders.ListBuilder
import androidx.slice.builders.ListBuilder.*
import androidx.slice.builders.SliceAction
import com.android.systemui.R
import com.android.systemui.keyguard.KeyguardSliceProvider
import com.google.android.systemui.smartspace.SmartSpaceCard
import com.google.android.systemui.smartspace.SmartSpaceController
import com.google.android.systemui.smartspace.SmartSpaceData
import com.google.android.systemui.smartspace.SmartSpaceUpdateListener
import java.lang.ref.WeakReference
import javax.inject.Inject

class KeyguardSliceProviderGoogle : KeyguardSliceProvider(), SmartSpaceUpdateListener {

    @Inject lateinit var smartSpaceController: SmartSpaceController
    private var smartSpaceData: SmartSpaceData? = null
    private var hideSensitiveContent = false
    private var hideWorkContent = true
    override fun updateClockLocked() = notifyChange()

    override fun onCreateSliceProvider(): Boolean {
        val created = super.onCreateSliceProvider()
        smartSpaceData = SmartSpaceData()
        smartSpaceController.addListener(this)
        return created
    }

    override fun onDestroy() {
        super.onDestroy()
        smartSpaceController.removeListener(this)
    }

    override fun onBindSlice(sliceUri: Uri): Slice {
        val builder = ListBuilder(context!!, mSliceUri, INFINITY)
        synchronized(this) {
            var hasAction = false
            val currentCard = smartSpaceData!!.currentCard
            if (
                currentCard != null &&
                    !currentCard.isExpired &&
                    !TextUtils.isEmpty(currentCard.title)
            ) {
                if (
                    !currentCard.isSensitive ||
                        !hideSensitiveContent && !currentCard.isWorkProfile ||
                        !hideWorkContent && currentCard.isWorkProfile
                ) hasAction = true
            }
            if (hasAction) {
                var action: SliceAction? = null
                val icon: IconCompat? =
                    when (currentCard.icon) {
                        null -> null
                        else -> IconCompat.createWithBitmap(currentCard.icon)
                    }
                val intent = currentCard.pendingIntent
                if (icon != null && intent != null) {
                    action = SliceAction(
                        intent,
                        icon,
                        SMALL_IMAGE,
                        currentCard.title
                    )
                }
                val headerBuilder = HeaderBuilder(mHeaderUri)
                    .setTitle(currentCard.formattedTitle)
                if (action != null) {
                    headerBuilder.setPrimaryAction(action)
                }
                builder.setHeader(headerBuilder)
                val subtitle = currentCard.subTitle
                if (subtitle != null) {
                    val calendarBuilder = RowBuilder(calendarUri).setTitle(subtitle)
                    if (icon != null) {
                        calendarBuilder.addEndItem(icon, SMALL_IMAGE)
                    }
                    if (action != null) {
                        calendarBuilder.setPrimaryAction(action)
                    }
                    builder.addRow(calendarBuilder)
                }
                addZenModeLocked(builder)
                addPrimaryActionLocked(builder)
                return builder.build()
            }
            if (needsMediaLocked()) {
                addMediaLocked(builder)
            } else {
                builder.addRow(
                    RowBuilder(mDateUri).setTitle(formattedDateLocked)
                )
            }
            addWeather(builder)
            addNextAlarmLocked(builder)
            addZenModeLocked(builder)
            addPrimaryActionLocked(builder)
            val slice = builder.build()
            if (isDebug) Log.d(logTag, "Binding slice: $slice")
            return slice
        }
    }

    private fun addWeather(listBuilder: ListBuilder) {
        val weatherCard = smartSpaceData!!.weatherCard
        if (weatherCard != null && !weatherCard.isExpired) {
            val builder = RowBuilder(weatherUri).setTitle(weatherCard.title)
            if (weatherCard.icon != null) {
                builder.addEndItem(
                    IconCompat.createWithBitmap(weatherCard.icon).setTintMode(Mode.DST),
                    SMALL_IMAGE
                )
            }
            listBuilder.addRow(builder)
        }
    }

    override fun onSensitiveModeChanged(hideSensitive: Boolean, hideWork: Boolean) {
        var notify = false
        synchronized(this) {
            if (hideSensitiveContent != hideSensitive) {
                hideSensitiveContent = hideSensitive
                if (isDebug) Log.d(logTag, "Public mode changed, hide data: $hideSensitive")
                notify = true
            }
            if (hideWorkContent != hideWork) {
                hideWorkContent = hideWork
                if (isDebug) Log.d(logTag, "Public work mode changed, hide data: $hideWork")
                notify = true
            }
        }
        when { notify -> notifyChange() }
    }

    override fun onSmartSpaceUpdated(data: SmartSpaceData) {
        synchronized(this) { smartSpaceData = data }
        val weatherCard = data.weatherCard
        when {
            weatherCard?.icon != null && !weatherCard.isIconProcessed -> {
                weatherCard.isIconProcessed = true
                AddShadowTask(this, weatherCard).execute(weatherCard.icon)
            }
            else -> notifyChange()
        }
    }

    class AddShadowTask(
        keyguardSliceProviderGoogle: KeyguardSliceProviderGoogle,
        card: SmartSpaceCard
    ) : AsyncTask<Bitmap, Void?, Bitmap>() {
        private val blurRadius: Float
        private val providerReference: WeakReference<KeyguardSliceProviderGoogle>
        private val weatherCard: SmartSpaceCard

        @Deprecated("Deprecated in Java")
        override fun doInBackground(bitmaps: Array<Bitmap>): Bitmap {
            val bitmap = bitmaps[0]
            val blurPaint = Paint()
            blurPaint.maskFilter = BlurMaskFilter(blurRadius, Blur.NORMAL)
            val offset = IntArray(2)
            val alpha = bitmap.extractAlpha(blurPaint, offset)
            val result = Bitmap.createBitmap(
                bitmap.width,
                bitmap.height,
                Config.ARGB_8888
            )
            with(Canvas(result)) {
                val paint = Paint()
                paint.alpha = 70
                drawBitmap(
                    alpha,
                    offset[0].toFloat(),
                    offset[1].toFloat().plus(blurRadius / 2.0f),
                    paint
                )
                alpha.recycle()
                paint.alpha = 255
                drawBitmap(bitmap, 0.0f, 0.0f, paint)
            }
            return result
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(bitmap: Bitmap) {
            var keyguardSliceProvider: KeyguardSliceProviderGoogle?
            synchronized(this) {
                weatherCard.icon = bitmap
                keyguardSliceProvider = providerReference.get()
            }
            if (keyguardSliceProvider != null) {
                keyguardSliceProvider!!.notifyChange()
            }
        }

        init {
            blurRadius =
                keyguardSliceProviderGoogle.context!!
                    .resources
                    .getDimension(R.dimen.smartspace_icon_shadow)
            providerReference = WeakReference(keyguardSliceProviderGoogle)
            weatherCard = card
        }
    }

    companion object {
        private const val logTag = "KeyguardSliceProviderGoogle"
        private val isDebug = Log.isLoggable(logTag, Log.DEBUG)
        private val calendarUri =
            Uri.parse("content://com.android.systemui.keyguard/smartSpace/calendar")
        private val weatherUri =
            Uri.parse("content://com.android.systemui.keyguard/smartSpace/weather")
    }
}