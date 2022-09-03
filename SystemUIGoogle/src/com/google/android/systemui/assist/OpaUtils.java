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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.util.ArraySet;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

import com.android.internal.app.AssistUtils;
import com.android.systemui.R;

public final class OpaUtils {
    static final Interpolator INTERPOLATOR_40_40 = new PathInterpolator(0.4f, 0.0f, 0.6f, 1.0f);
    static final Interpolator INTERPOLATOR_40_OUT = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);

    static float getDeltaDiamondPositionBottomX() {
        return 0.0f;
    }

    static float getDeltaDiamondPositionLeftY() {
        return 0.0f;
    }

    static float getDeltaDiamondPositionRightY() {
        return 0.0f;
    }

    static float getDeltaDiamondPositionTopX() {
        return 0.0f;
    }

    static Animator getTranslationAnimatorX(View view, Interpolator interpolator, int i) {
        RenderNodeAnimator renderNodeAnimator = new RenderNodeAnimator(0, 0.0f);
        renderNodeAnimator.setTarget(view);
        renderNodeAnimator.setInterpolator(interpolator);
        renderNodeAnimator.setDuration(i);
        return renderNodeAnimator;
    }

    static ObjectAnimator getAlphaObjectAnimator(View view, float f, int i, int i2, Interpolator interpolator) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, View.ALPHA, f);
        ofFloat.setInterpolator(interpolator);
        ofFloat.setDuration(i);
        ofFloat.setStartDelay(i2);
        return ofFloat;
    }

    static Animator getLongestAnim(ArraySet<Animator> arraySet) {
        long j = Long.MIN_VALUE;
        Animator animator = null;
        for (int size = arraySet.size() - 1; size >= 0; size--) {
            Animator valueAt = arraySet.valueAt(size);
            if (valueAt.getTotalDuration() > j) {
                j = valueAt.getTotalDuration();
                animator = valueAt;
            }
        }
        return animator;
    }

    static ObjectAnimator getScaleObjectAnimator(View view, float f, int i, Interpolator interpolator) {
        ObjectAnimator ofPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(view, PropertyValuesHolder.ofFloat(View.SCALE_X, f), PropertyValuesHolder.ofFloat(View.SCALE_Y, f));
        ofPropertyValuesHolder.setDuration(i);
        ofPropertyValuesHolder.setInterpolator(interpolator);
        return ofPropertyValuesHolder;
    }

    static ObjectAnimator getTranslationObjectAnimatorY(View view, Interpolator interpolator, float f, float f2, int i) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, View.Y, f2, f2 + f);
        ofFloat.setInterpolator(interpolator);
        ofFloat.setDuration(i);
        return ofFloat;
    }

    static ObjectAnimator getTranslationObjectAnimatorX(View view, Interpolator interpolator, float f, float f2, int i) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, View.X, f2, f2 + f);
        ofFloat.setInterpolator(interpolator);
        ofFloat.setDuration(i);
        return ofFloat;
    }

    static float getPxVal(Resources resources, int i) {
        return resources.getDimensionPixelOffset(i);
    }

    static boolean isAGSACurrentAssistant(Context context) {
        ComponentName assistComponentForUser = new AssistUtils(context).getAssistComponentForUser(-2);
        return assistComponentForUser != null && "com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService".equals(assistComponentForUser.flattenToString());
    }

    static float getDeltaDiamondPositionTopY(Resources resources) {
        return -getPxVal(resources, R.dimen.opa_diamond_translation);
    }

    static float getDeltaDiamondPositionLeftX(Resources resources) {
        return -getPxVal(resources, R.dimen.opa_diamond_translation);
    }

    static float getDeltaDiamondPositionRightX(Resources resources) {
        return getPxVal(resources, R.dimen.opa_diamond_translation);
    }

    static float getDeltaDiamondPositionBottomY(Resources resources) {
        return getPxVal(resources, R.dimen.opa_diamond_translation);
    }
}
