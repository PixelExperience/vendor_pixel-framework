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
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

import com.android.systemui.R;
import com.android.systemui.animation.Interpolators;
import com.google.android.systemui.elmyra.feedback.FeedbackEffect;
import com.google.android.systemui.elmyra.sensors.GestureSensor;

import java.util.ArrayList;

public class LockscreenOpaLayout extends FrameLayout implements FeedbackEffect {
    private final Interpolator INTERPOLATOR_5_100;
    private final int RED_YELLOW_START_DELAY;
    private final ArrayList<View> mAnimatedViews;
    private final ArraySet<Animator> mCurrentAnimators;
    private View mBlue;
    private AnimatorSet mCannedAnimatorSet;
    private AnimatorSet mGestureAnimatorSet;
    private int mGestureState;
    private View mGreen;
    private AnimatorSet mLineAnimatorSet;
    private View mRed;
    private Resources mResources;
    private View mYellow;

    public LockscreenOpaLayout(Context context) {
        super(context);
        RED_YELLOW_START_DELAY = 17;
        INTERPOLATOR_5_100 = new PathInterpolator(1.0f, 0.0f, 0.95f, 1.0f);
        mGestureState = 0;
        mCurrentAnimators = new ArraySet<>();
        mAnimatedViews = new ArrayList<>();
    }

    public LockscreenOpaLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        RED_YELLOW_START_DELAY = 17;
        INTERPOLATOR_5_100 = new PathInterpolator(1.0f, 0.0f, 0.95f, 1.0f);
        mGestureState = 0;
        mCurrentAnimators = new ArraySet<>();
        mAnimatedViews = new ArrayList<>();
    }

    public LockscreenOpaLayout(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        RED_YELLOW_START_DELAY = 17;
        INTERPOLATOR_5_100 = new PathInterpolator(1.0f, 0.0f, 0.95f, 1.0f);
        mGestureState = 0;
        mCurrentAnimators = new ArraySet<>();
        mAnimatedViews = new ArrayList<>();
    }

    public LockscreenOpaLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        RED_YELLOW_START_DELAY = 17;
        INTERPOLATOR_5_100 = new PathInterpolator(1.0f, 0.0f, 0.95f, 1.0f);
        mGestureState = 0;
        mCurrentAnimators = new ArraySet<>();
        mAnimatedViews = new ArrayList<>();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mResources = getResources();
        mBlue = findViewById(R.id.blue);
        mRed = findViewById(R.id.red);
        mYellow = findViewById(R.id.yellow);
        mGreen = findViewById(R.id.green);
        mAnimatedViews.add(mBlue);
        mAnimatedViews.add(mRed);
        mAnimatedViews.add(mYellow);
        mAnimatedViews.add(mGreen);
    }

    private void startCannedAnimation() {
        if (isAttachedToWindow()) {
            skipToStartingValue();
            mGestureState = 3;
            mGestureAnimatorSet = getCannedAnimatorSet();
            mGestureAnimatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    mGestureState = 1;
                    mGestureAnimatorSet = getLineAnimatorSet();
                    mGestureAnimatorSet.setCurrentPlayTime(0L);
                }
            });
            mGestureAnimatorSet.start();
            return;
        }
        skipToStartingValue();
    }

    private void startRetractAnimation() {
        if (isAttachedToWindow()) {
            if (mGestureAnimatorSet != null) {
                mGestureAnimatorSet.removeAllListeners();
                mGestureAnimatorSet.cancel();
            }
            mCurrentAnimators.clear();
            mCurrentAnimators.addAll((ArraySet<? extends Animator>) getRetractAnimatorSet());
            startAll(mCurrentAnimators);
            mGestureState = 4;
            return;
        }
        skipToStartingValue();
    }

    private void startCollapseAnimation() {
        if (isAttachedToWindow()) {
            mCurrentAnimators.clear();
            mCurrentAnimators.addAll((ArraySet<? extends Animator>) getCollapseAnimatorSet());
            startAll(mCurrentAnimators);
            mGestureState = 2;
            return;
        }
        skipToStartingValue();
    }

    private void startAll(ArraySet<Animator> arraySet) {
        for (int size = arraySet.size() - 1; size >= 0; size--) {
            arraySet.valueAt(size).start();
        }
    }

    private ArraySet<Animator> getRetractAnimatorSet() {
        ArraySet<Animator> arraySet = new ArraySet<>();
        Interpolator interpolator = OpaUtils.INTERPOLATOR_40_OUT;
        arraySet.add(OpaUtils.getTranslationAnimatorX(mRed, interpolator, 190));
        arraySet.add(OpaUtils.getTranslationAnimatorX(mBlue, interpolator, 190));
        arraySet.add(OpaUtils.getTranslationAnimatorX(mGreen, interpolator, 190));
        arraySet.add(OpaUtils.getTranslationAnimatorX(mYellow, interpolator, 190));
        OpaUtils.getLongestAnim(arraySet).addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                mCurrentAnimators.clear();
                skipToStartingValue();
                mGestureState = 0;
                mGestureAnimatorSet = null;
            }
        });
        return arraySet;
    }

    private ArraySet<Animator> getCollapseAnimatorSet() {
        ArraySet<Animator> arraySet = new ArraySet<>();
        Interpolator interpolator = OpaUtils.INTERPOLATOR_40_OUT;
        arraySet.add(OpaUtils.getTranslationAnimatorX(mRed, interpolator, 133));
        arraySet.add(OpaUtils.getTranslationAnimatorX(mBlue, interpolator, 150));
        arraySet.add(OpaUtils.getTranslationAnimatorX(mYellow, interpolator, 133));
        arraySet.add(OpaUtils.getTranslationAnimatorX(mGreen, interpolator, 150));
        OpaUtils.getLongestAnim(arraySet).addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                mCurrentAnimators.clear();
                mGestureAnimatorSet = null;
                mGestureState = 0;
                skipToStartingValue();
            }
        });
        return arraySet;
    }

    private void skipToStartingValue() {
        int size = mAnimatedViews.size();
        for (int i = 0; i < size; i++) {
            View view = mAnimatedViews.get(i);
            view.setAlpha(0.0f);
            view.setTranslationX(0.0f);
        }
    }

    @Override
    public void onRelease() {
        if (mGestureState == 2 || mGestureState == 4) {
            return;
        }
        if (mGestureState != 3) {
            if (mGestureState != 1) {
                return;
            }
            startRetractAnimation();
        } else if (mGestureAnimatorSet.isRunning()) {
            mGestureAnimatorSet.removeAllListeners();
            mGestureAnimatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    startRetractAnimation();
                }
            });
        } else {
            mGestureState = 4;
            startRetractAnimation();
        }
    }

    @Override
    public void onProgress(float f, int i) {
        if (mGestureState == 2) {
            return;
        }
        if (mGestureState == 4) {
            endCurrentAnimation();
        }
        if (f == 0.0f) {
            mGestureState = 0;
            return;
        }
        long max = (long) Math.max(0L, (f * 533.0f) - 167);
        int i3 = mGestureState;
        if (i3 == 0) {
            startCannedAnimation();
        } else if (i3 == 1) {
            mGestureAnimatorSet.setCurrentPlayTime(max);
        } else if (i3 != 3 || max < 167) {
        } else {
            mGestureAnimatorSet.end();
            if (mGestureState != 1) {
                return;
            }
            mGestureAnimatorSet.setCurrentPlayTime(max);
        }
    }

    @Override
    public void onResolve(GestureSensor.DetectionProperties detectionProperties) {
        if (mGestureState == 4 || mGestureState == 2) {
            return;
        }
        if (mGestureState == 3) {
            mGestureState = 2;
            mGestureAnimatorSet.removeAllListeners();
            mGestureAnimatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    mGestureAnimatorSet = getLineAnimatorSet();
                    mGestureAnimatorSet.removeAllListeners();
                    mGestureAnimatorSet.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animator2) {
                            startCollapseAnimation();
                        }
                    });
                    mGestureAnimatorSet.end();
                }
            });
            return;
        }
        if (mGestureAnimatorSet == null) {
            return;
        }
        mGestureState = 2;
        mGestureAnimatorSet.removeAllListeners();
        mGestureAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                startCollapseAnimation();
            }
        });
        if (mGestureAnimatorSet.isStarted()) {
            return;
        }
        mGestureAnimatorSet.start();
    }

    private AnimatorSet getCannedAnimatorSet() {
        if (mCannedAnimatorSet != null) {
            mCannedAnimatorSet.removeAllListeners();
            mCannedAnimatorSet.cancel();
            return mCannedAnimatorSet;
        }
        mCannedAnimatorSet = new AnimatorSet();
        Interpolator interpolator = OpaUtils.INTERPOLATOR_40_40;
        ObjectAnimator translationObjectAnimatorX = OpaUtils.getTranslationObjectAnimatorX(mRed, interpolator, -OpaUtils.getPxVal(mResources, R.dimen.opa_lockscreen_canned_ry), mRed.getX(), 83);
        translationObjectAnimatorX.setStartDelay(RED_YELLOW_START_DELAY);
        ObjectAnimator translationObjectAnimatorX2 = OpaUtils.getTranslationObjectAnimatorX(mYellow, interpolator, OpaUtils.getPxVal(mResources, R.dimen.opa_lockscreen_canned_ry), mYellow.getX(), 83);
        translationObjectAnimatorX2.setStartDelay(RED_YELLOW_START_DELAY);
        AnimatorSet.Builder with = mCannedAnimatorSet.play(translationObjectAnimatorX).with(translationObjectAnimatorX2);
        AnimatorSet.Builder with2 = with.with(OpaUtils.getTranslationObjectAnimatorX(mBlue, interpolator, -OpaUtils.getPxVal(mResources, R.dimen.opa_lockscreen_canned_bg), mBlue.getX(), 167)).with(OpaUtils.getTranslationObjectAnimatorX(mGreen, interpolator, OpaUtils.getPxVal(mResources, R.dimen.opa_lockscreen_canned_bg), mGreen.getX(), 167));
        Interpolator interpolator2 = Interpolators.LINEAR;
        with2.with(OpaUtils.getAlphaObjectAnimator(mRed, 1.0f, 50, 130, interpolator2)).with(OpaUtils.getAlphaObjectAnimator(mYellow, 1.0f, 50, 130, interpolator2)).with(OpaUtils.getAlphaObjectAnimator(mBlue, 1.0f, 50, 113, interpolator2)).with(OpaUtils.getAlphaObjectAnimator(mGreen, 1.0f, 50, 113, interpolator2));
        return mCannedAnimatorSet;
    }

    private AnimatorSet getLineAnimatorSet() {
        if (mLineAnimatorSet != null) {
            mLineAnimatorSet.removeAllListeners();
            mLineAnimatorSet.cancel();
            return mLineAnimatorSet;
        }
        mLineAnimatorSet = new AnimatorSet();
        AnimatorSet.Builder with = mLineAnimatorSet.play(OpaUtils.getTranslationObjectAnimatorX(mRed, INTERPOLATOR_5_100, -OpaUtils.getPxVal(mResources, R.dimen.opa_lockscreen_translation_ry), mRed.getX(), 366)).with(OpaUtils.getTranslationObjectAnimatorX(mYellow, INTERPOLATOR_5_100, OpaUtils.getPxVal(mResources, R.dimen.opa_lockscreen_translation_ry), mYellow.getX(), 366));
        with.with(OpaUtils.getTranslationObjectAnimatorX(mGreen, INTERPOLATOR_5_100, OpaUtils.getPxVal(mResources, R.dimen.opa_lockscreen_translation_bg), mGreen.getX(), 366)).with(OpaUtils.getTranslationObjectAnimatorX(mBlue, INTERPOLATOR_5_100, -OpaUtils.getPxVal(mResources, R.dimen.opa_lockscreen_translation_bg), mBlue.getX(), 366));
        return mLineAnimatorSet;
    }

    private void endCurrentAnimation() {
        if (!mCurrentAnimators.isEmpty()) {
            for (int size = mCurrentAnimators.size() - 1; size >= 0; size--) {
                Animator valueAt = mCurrentAnimators.valueAt(size);
                valueAt.removeAllListeners();
                valueAt.end();
            }
            mCurrentAnimators.clear();
        }
        mGestureState = 0;
    }
}
