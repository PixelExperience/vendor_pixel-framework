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

package com.google.android.systemui.smartspace;

import android.util.Log;
import androidx.annotation.NonNull;

public class SmartSpaceData {
    public SmartSpaceCard mCurrentCard;
    public SmartSpaceCard mWeatherCard;

    public boolean hasCurrent() {
        return this.mCurrentCard != null;
    }

    public boolean hasWeather() {
        return this.mWeatherCard != null;
    }

    public SmartSpaceCard getCurrentCard() {
        return this.mCurrentCard;
    }

    public SmartSpaceCard getWeatherCard() {
        return this.mWeatherCard;
    }

    public long getExpirationRemainingMillis() {
        long expiration;
        long currentTimeMillis = System.currentTimeMillis();
        if (hasCurrent() && hasWeather()) {
            expiration = Math.min(this.mCurrentCard.getExpiration(), this.mWeatherCard.getExpiration());
        } else if (hasCurrent()) {
            expiration = this.mCurrentCard.getExpiration();
        } else if (!hasWeather()) {
            return 0L;
        } else {
            expiration = this.mWeatherCard.getExpiration();
        }
        return expiration - currentTimeMillis;
    }

    public long getExpiresAtMillis() {
        if (hasCurrent() && hasWeather()) {
            return Math.min(this.mCurrentCard.getExpiration(), this.mWeatherCard.getExpiration());
        }
        if (hasCurrent()) {
            return this.mCurrentCard.getExpiration();
        }
        if (hasWeather()) {
            return this.mWeatherCard.getExpiration();
        }
        return 0L;
    }

    public void clear() {
        this.mWeatherCard = null;
        this.mCurrentCard = null;
    }

    public boolean handleExpire() {
        boolean z;
        if (hasWeather() && this.mWeatherCard.isExpired()) {
            if (SmartSpaceController.DEBUG) {
                Log.d("SmartspaceData", "weather expired " + this.mWeatherCard.getExpiration());
            }
            this.mWeatherCard = null;
            z = true;
        } else {
            z = false;
        }
        if (hasCurrent() && this.mCurrentCard.isExpired()) {
            if (SmartSpaceController.DEBUG) {
                Log.d("SmartspaceData", "current expired " + this.mCurrentCard.getExpiration());
            }
            this.mCurrentCard = null;
            return true;
        }
        return z;
    }

    @NonNull
    public String toString() {
        return "{" + this.mCurrentCard + "," + this.mWeatherCard + "}";
    }
}