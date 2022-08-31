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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.UserHandle;
import android.text.format.DateFormat;
import androidx.core.app.NotificationCompat;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.R;
import java.time.Clock;
import java.util.Locale;

public final class PowerUtils {
    public static final int NOTIFICATION_ID = R.string.defender_notify_title;
    public static final int POST_NOTIFICATION_ID = R.string.defender_post_notify_title;
    public static final int AC_NOTIFICATION_ID = R.string.adaptive_charging_notify_title;

    static PendingIntent createHelpArticlePendingIntent(Context context, int i) {
        return PendingIntent.getActivity(context, 0, new Intent("android.intent.action.VIEW", Uri.parse(context.getString(i))), 67108864);
    }

    static PendingIntent createNormalChargingIntent(Context context, String str) {
        return PendingIntent.getBroadcastAsUser(context, 0, new Intent(str).setPackage(context.getPackageName()).setFlags(1342177280), 67108864, UserHandle.CURRENT);
    }

    static void overrideNotificationAppName(Context context, NotificationCompat.Builder builder, int i) {
        Bundle bundle = new Bundle();
        bundle.putString("android.substName", context.getString(i));
        builder.addExtras(bundle);
    }

    static boolean postNotificationThreshold(long j) {
        return j > 0 && Clock.systemUTC().millis() - j >= 600000;
    }

    static int getBatteryLevel(Intent intent) {
        int intExtra = intent.getIntExtra("level", -1);
        int intExtra2 = intent.getIntExtra("scale", 0);
        if (intExtra2 == 0) {
            return -1;
        }
        return Math.round((intExtra / intExtra2) * 100.0f);
    }

    static boolean isFullyCharged(Intent intent) {
        return (intent.getIntExtra("status", 1) == 5) || getBatteryLevel(intent) >= 100;
    }

    static String getCurrentTime(Context context, long j) {
        Locale locale = getLocale(context);
        return DateFormat.format(DateFormat.getBestDateTimePattern(locale, DateFormat.is24HourFormat(context) ? "HH:mm" : "h:m"), j).toString().toUpperCase(locale);
    }

    @VisibleForTesting
    static Locale getLocale(Context context) {
        LocaleList locales = context.getResources().getConfiguration().getLocales();
        return (locales == null || locales.isEmpty()) ? Locale.getDefault() : locales.get(0);
    }
}
