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
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.os.Trace;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.animation.Interpolators;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.navigationbar.buttons.ButtonInterface;
import com.android.systemui.navigationbar.buttons.KeyButtonDrawable;
import com.android.systemui.navigationbar.buttons.KeyButtonView;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.shared.system.QuickStepContract;
import com.google.android.systemui.elmyra.feedback.FeedbackEffect;
import com.google.android.systemui.elmyra.sensors.GestureSensor;

import java.util.ArrayList;


public class OpaLayout extends FrameLayout implements ButtonInterface, FeedbackEffect {
    private final Interpolator HOME_DISAPPEAR_INTERPOLATOR;
    private final ArrayList<View> mAnimatedViews;
    private final ArraySet<Animator> mCurrentAnimators;
    private final Runnable mDiamondAnimation;
    private final Interpolator mDiamondInterpolator;
    private final OverviewProxyService.OverviewProxyListener mOverviewProxyListener;
    private final Runnable mRetract;
    private int mAnimationState;
    private View mBlue;
    private View mBottom;
    private boolean mDelayTouchFeedback;
    private boolean mDiamondAnimationDelayed;
    private long mGestureAnimationSetDuration;
    private AnimatorSet mGestureAnimatorSet;
    private AnimatorSet mGestureLineSet;
    private int mGestureState;
    private View mGreen;
    private ImageView mHalo;
    private KeyButtonView mHome;
    private int mHomeDiameter;
    private boolean mIsPressed;
    private boolean mIsVertical;
    private View mLeft;
    private boolean mOpaEnabled;
    private boolean mOpaEnabledNeedsUpdate;
    private OverviewProxyService mOverviewProxyService;
    private View mRed;
    private Resources mResources;
    private View mRight;
    private long mStartTime;
    private View mTop;
    private int mTouchDownX;
    private int mTouchDownY;
    private ImageView mWhite;
    private ImageView mWhiteCutout;
    private boolean mWindowVisible;
    private View mYellow;

    public OpaLayout(Context context) {
        this(context, null);
    }

    public OpaLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public OpaLayout(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        HOME_DISAPPEAR_INTERPOLATOR = new PathInterpolator(0.65f, 0.0f, 1.0f, 1.0f);
        mDiamondInterpolator = new PathInterpolator(0.2f, 0.0f, 0.2f, 1.0f);
        mCurrentAnimators = new ArraySet<>();
        mAnimatedViews = new ArrayList<>();
        mAnimationState = 0;
        mGestureState = 0;
        mRetract = () -> {
            cancelCurrentAnimation("retract");
            startRetractAnimation();
        };
        mOverviewProxyListener = new OverviewProxyService.OverviewProxyListener() {
            @Override
            public void onConnectionChanged(boolean z) {
                updateOpaLayout();
            }
        };
        mDiamondAnimation = () -> {
            if (mCurrentAnimators.isEmpty()) {
                startDiamondAnimation();
            }
        };
    }

    public OpaLayout(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mResources = getResources();
        mBlue = findViewById(R.id.blue);
        mRed = findViewById(R.id.red);
        mYellow = findViewById(R.id.yellow);
        mGreen = findViewById(R.id.green);
        mWhite = (ImageView) findViewById(R.id.white);
        mWhiteCutout = (ImageView) findViewById(R.id.white_cutout);
        mHalo = (ImageView) findViewById(R.id.halo);
        mHome = (KeyButtonView) findViewById(R.id.home_button);
        mHalo.setImageDrawable(KeyButtonDrawable.create(new ContextThemeWrapper(getContext(), R.style.DualToneLightTheme), new ContextThemeWrapper(getContext(), R.style.DualToneDarkTheme), R.drawable.halo, true, null));
        mHomeDiameter = mResources.getDimensionPixelSize(R.dimen.opa_disabled_home_diameter);
        Paint paint = new Paint();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        mWhiteCutout.setLayerType(2, paint);
        mAnimatedViews.add(mBlue);
        mAnimatedViews.add(mRed);
        mAnimatedViews.add(mYellow);
        mAnimatedViews.add(mGreen);
        mAnimatedViews.add(mWhite);
        mAnimatedViews.add(mWhiteCutout);
        mAnimatedViews.add(mHalo);
        mOverviewProxyService = (OverviewProxyService) Dependency.get(OverviewProxyService.class);
    }

    @Override
    public void onWindowVisibilityChanged(int i) {
        super.onWindowVisibilityChanged(i);
        mWindowVisible = i == 0;
        if (i == 0) {
            updateOpaLayout();
            return;
        }
        cancelCurrentAnimation("winVis=" + i);
        skipToStartingValue();
    }

    @Override
    public void setOnLongClickListener(final View.OnLongClickListener onLongClickListener) {
        if (onLongClickListener == null) {
            mHome.setLongClickable(false);
            return;
        }
        mHome.setLongClickable(true);
        mHome.setOnLongClickListener((OnLongClickListener) view -> onLongClickListener.onLongClick(mHome));
    }

    @Override
    public void setOnTouchListener(View.OnTouchListener onTouchListener) {
        mHome.setOnTouchListener(onTouchListener);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        boolean z;
        if (getOpaEnabled() && ValueAnimator.areAnimatorsEnabled() && mGestureState == 0) {
            int action = motionEvent.getAction();
            if (action == 0) {
                mTouchDownX = (int) motionEvent.getRawX();
                mTouchDownY = (int) motionEvent.getRawY();
                if (mCurrentAnimators.isEmpty()) {
                    z = false;
                } else if (mAnimationState != 2) {
                    return false;
                } else {
                    endCurrentAnimation("touchDown");
                    z = true;
                }
                mStartTime = SystemClock.elapsedRealtime();
                mIsPressed = true;
                removeCallbacks(mDiamondAnimation);
                removeCallbacks(mRetract);
                if (!mDelayTouchFeedback || z) {
                    mDiamondAnimationDelayed = false;
                    startDiamondAnimation();
                } else {
                    mDiamondAnimationDelayed = true;
                    postDelayed(mDiamondAnimation, ViewConfiguration.getTapTimeout());
                }
            } else {
                if (action != 1) {
                    if (action == 2) {
                        float quickStepTouchSlopPx = QuickStepContract.getQuickStepTouchSlopPx(getContext());
                        if (Math.abs(motionEvent.getRawX() - mTouchDownX) > quickStepTouchSlopPx || Math.abs(motionEvent.getRawY() - mTouchDownY) > quickStepTouchSlopPx) {
                            abortCurrentGesture();
                            return false;
                        }
                    }
                    return false;
                }
                if (mDiamondAnimationDelayed) {
                    if (mIsPressed) {
                        postDelayed(mRetract, 200L);
                    }
                } else if (mAnimationState == 1) {
                    removeCallbacks(mRetract);
                    postDelayed(mRetract, 100 - (SystemClock.elapsedRealtime() - mStartTime));
                    removeCallbacks(mDiamondAnimation);
                    cancelLongPress();
                    return false;
                } else if (mIsPressed) {
                    mRetract.run();
                }
                mIsPressed = false;
            }
        }
        return false;
    }

    @Override
    public void setAccessibilityDelegate(View.AccessibilityDelegate accessibilityDelegate) {
        super.setAccessibilityDelegate(accessibilityDelegate);
        mHome.setAccessibilityDelegate(accessibilityDelegate);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        mWhite.setImageDrawable(drawable);
        mWhiteCutout.setImageDrawable(drawable);
    }

    @Override
    public void abortCurrentGesture() {
        Trace.beginSection("OpaLayout.abortCurrentGesture: animState=" + mAnimationState);
        Trace.endSection();
        mHome.abortCurrentGesture();
        mIsPressed = false;
        mDiamondAnimationDelayed = false;
        removeCallbacks(mDiamondAnimation);
        cancelLongPress();
        int i = mAnimationState;
        if (i == 3 || i == 1) {
            mRetract.run();
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateOpaLayout();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mOverviewProxyService.addCallback(mOverviewProxyListener);
        mOpaEnabledNeedsUpdate = true;
        post(this::getOpaEnabled);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mOverviewProxyService.removeCallback(mOverviewProxyListener);
    }

    private void startDiamondAnimation() {
        if (allowAnimations()) {
            mCurrentAnimators.clear();
            setDotsVisible();
            mCurrentAnimators.addAll((ArraySet<? extends Animator>) getDiamondAnimatorSet());
            mAnimationState = 1;
            startAll(mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private void startRetractAnimation() {
        if (allowAnimations()) {
            mCurrentAnimators.clear();
            mCurrentAnimators.addAll((ArraySet<? extends Animator>) getRetractAnimatorSet());
            mAnimationState = 2;
            startAll(mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private void startLineAnimation() {
        if (allowAnimations()) {
            mCurrentAnimators.clear();
            mCurrentAnimators.addAll((ArraySet<? extends Animator>) getLineAnimatorSet());
            mAnimationState = 3;
            startAll(mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private void startCollapseAnimation() {
        if (allowAnimations()) {
            mCurrentAnimators.clear();
            mCurrentAnimators.addAll((ArraySet<? extends Animator>) getCollapseAnimatorSet());
            mAnimationState = 3;
            startAll(mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private void startAll(ArraySet<Animator> arraySet) {
        for (int size = arraySet.size() - 1; size >= 0; size--) {
            arraySet.valueAt(size).start();
        }
        for (int size2 = mAnimatedViews.size() - 1; size2 >= 0; size2--) {
            mAnimatedViews.get(size2).invalidate();
        }
    }

    private boolean allowAnimations() {
        return isAttachedToWindow() && mWindowVisible;
    }

    private ArraySet<Animator> getDiamondAnimatorSet() {
        ArraySet<Animator> arraySet = new ArraySet<>();
        arraySet.add(getPropertyAnimator(mTop, View.Y, (-OpaUtils.getPxVal(mResources, R.dimen.opa_diamond_translation)) + mTop.getY(), 200, mDiamondInterpolator));
        Interpolator interpolator = Interpolators.FAST_OUT_SLOW_IN;
        arraySet.add(getPropertyAnimator(mTop, FrameLayout.SCALE_X, 0.8f, 200, interpolator));
        arraySet.add(getPropertyAnimator(mTop, FrameLayout.SCALE_Y, 0.8f, 200, interpolator));
        arraySet.add(getPropertyAnimator(mBottom, View.Y, mBottom.getY() + OpaUtils.getPxVal(mResources, R.dimen.opa_diamond_translation), 200, mDiamondInterpolator));
        arraySet.add(getPropertyAnimator(mBottom, FrameLayout.SCALE_X, 0.8f, 200, interpolator));
        arraySet.add(getPropertyAnimator(mBottom, FrameLayout.SCALE_Y, 0.8f, 200, interpolator));
        arraySet.add(getPropertyAnimator(mLeft, View.X, mLeft.getX() + (-OpaUtils.getPxVal(mResources, R.dimen.opa_diamond_translation)), 200, mDiamondInterpolator));
        arraySet.add(getPropertyAnimator(mLeft, FrameLayout.SCALE_X, 0.8f, 200, interpolator));
        arraySet.add(getPropertyAnimator(mLeft, FrameLayout.SCALE_Y, 0.8f, 200, interpolator));
        arraySet.add(getPropertyAnimator(mRight, View.X, mRight.getX() + OpaUtils.getPxVal(mResources, R.dimen.opa_diamond_translation), 200, mDiamondInterpolator));
        arraySet.add(getPropertyAnimator(mRight, FrameLayout.SCALE_X, 0.8f, 200, interpolator));
        arraySet.add(getPropertyAnimator(mRight, FrameLayout.SCALE_Y, 0.8f, 200, interpolator));
        arraySet.add(getPropertyAnimator(mWhite, FrameLayout.SCALE_X, 0.625f, 200, interpolator));
        arraySet.add(getPropertyAnimator(mWhite, FrameLayout.SCALE_Y, 0.625f, 200, interpolator));
        arraySet.add(getPropertyAnimator(mWhiteCutout, FrameLayout.SCALE_X, 0.625f, 200, interpolator));
        arraySet.add(getPropertyAnimator(mWhiteCutout, FrameLayout.SCALE_Y, 0.625f, 200, interpolator));
        arraySet.add(getPropertyAnimator(mHalo, FrameLayout.SCALE_X, 0.47619048f, 100, interpolator));
        arraySet.add(getPropertyAnimator(mHalo, FrameLayout.SCALE_Y, 0.47619048f, 100, interpolator));
        arraySet.add(getPropertyAnimator(mHalo, View.ALPHA, 0.0f, 100, interpolator));
        getLongestAnim(arraySet).addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                Trace.beginSection("OpaLayout.start.diamond");
                Trace.endSection();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                Trace.beginSection("OpaLayout.cancel.diamond");
                Trace.endSection();
                mCurrentAnimators.clear();
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                Trace.beginSection("OpaLayout.end.diamond");
                Trace.endSection();
                startLineAnimation();
            }
        });
        return arraySet;
    }

    private ArraySet<Animator> getRetractAnimatorSet() {
        ArraySet<Animator> arraySet = new ArraySet<>();
        Interpolator interpolator = OpaUtils.INTERPOLATOR_40_OUT;
        arraySet.add(getPropertyAnimator(mRed, FrameLayout.TRANSLATION_X, 0.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mRed, FrameLayout.TRANSLATION_Y, 0.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mRed, FrameLayout.SCALE_X, 1.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mRed, FrameLayout.SCALE_Y, 1.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mBlue, FrameLayout.TRANSLATION_X, 0.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mBlue, FrameLayout.TRANSLATION_Y, 0.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mBlue, FrameLayout.SCALE_X, 1.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mBlue, FrameLayout.SCALE_Y, 1.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mGreen, FrameLayout.TRANSLATION_X, 0.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mGreen, FrameLayout.TRANSLATION_Y, 0.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mGreen, FrameLayout.SCALE_X, 1.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mGreen, FrameLayout.SCALE_Y, 1.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mYellow, FrameLayout.TRANSLATION_X, 0.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mYellow, FrameLayout.TRANSLATION_Y, 0.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mYellow, FrameLayout.SCALE_X, 1.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mYellow, FrameLayout.SCALE_Y, 1.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mWhite, FrameLayout.SCALE_X, 1.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mWhite, FrameLayout.SCALE_Y, 1.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mWhiteCutout, FrameLayout.SCALE_X, 1.0f, 190, interpolator));
        arraySet.add(getPropertyAnimator(mWhiteCutout, FrameLayout.SCALE_Y, 1.0f, 190, interpolator));
        Interpolator interpolator2 = Interpolators.FAST_OUT_SLOW_IN;
        arraySet.add(getPropertyAnimator(mHalo, FrameLayout.SCALE_X, 1.0f, 190, interpolator2));
        arraySet.add(getPropertyAnimator(mHalo, FrameLayout.SCALE_Y, 1.0f, 190, interpolator2));
        arraySet.add(getPropertyAnimator(mHalo, FrameLayout.ALPHA, 1.0f, 190, interpolator2));
        getLongestAnim(arraySet).addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                Trace.beginSection("OpaLayout.start.retract");
                Trace.endSection();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                Trace.beginSection("OpaLayout.cancel.retract");
                Trace.endSection();
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                Trace.beginSection("OpaLayout.end.retract");
                Trace.endSection();
                mCurrentAnimators.clear();
                skipToStartingValue();
            }
        });
        return arraySet;
    }

    private ArraySet<Animator> getCollapseAnimatorSet() {
        Animator propertyAnimator;
        Animator propertyAnimator2;
        Animator propertyAnimator3;
        Animator propertyAnimator4;
        ArraySet<Animator> arraySet = new ArraySet<>();
        if (mIsVertical) {
            propertyAnimator = getPropertyAnimator(mRed, FrameLayout.TRANSLATION_Y, 0.0f, 133, OpaUtils.INTERPOLATOR_40_OUT);
        } else {
            propertyAnimator = getPropertyAnimator(mRed, FrameLayout.TRANSLATION_X, 0.0f, 133, OpaUtils.INTERPOLATOR_40_OUT);
        }
        arraySet.add(propertyAnimator);
        Interpolator interpolator = OpaUtils.INTERPOLATOR_40_OUT;
        arraySet.add(getPropertyAnimator(mRed, FrameLayout.SCALE_X, 1.0f, 200, interpolator));
        arraySet.add(getPropertyAnimator(mRed, FrameLayout.SCALE_Y, 1.0f, 200, interpolator));
        if (mIsVertical) {
            propertyAnimator2 = getPropertyAnimator(mBlue, FrameLayout.TRANSLATION_Y, 0.0f, 150, interpolator);
        } else {
            propertyAnimator2 = getPropertyAnimator(mBlue, FrameLayout.TRANSLATION_X, 0.0f, 150, interpolator);
        }
        arraySet.add(propertyAnimator2);
        arraySet.add(getPropertyAnimator(mBlue, FrameLayout.SCALE_X, 1.0f, 200, interpolator));
        arraySet.add(getPropertyAnimator(mBlue, FrameLayout.SCALE_Y, 1.0f, 200, interpolator));
        if (mIsVertical) {
            propertyAnimator3 = getPropertyAnimator(mYellow, FrameLayout.TRANSLATION_Y, 0.0f, 133, interpolator);
        } else {
            propertyAnimator3 = getPropertyAnimator(mYellow, FrameLayout.TRANSLATION_X, 0.0f, 133, interpolator);
        }
        arraySet.add(propertyAnimator3);
        arraySet.add(getPropertyAnimator(mYellow, FrameLayout.SCALE_X, 1.0f, 200, interpolator));
        arraySet.add(getPropertyAnimator(mYellow, FrameLayout.SCALE_Y, 1.0f, 200, interpolator));
        if (mIsVertical) {
            propertyAnimator4 = getPropertyAnimator(mGreen, FrameLayout.TRANSLATION_Y, 0.0f, 150, interpolator);
        } else {
            propertyAnimator4 = getPropertyAnimator(mGreen, FrameLayout.TRANSLATION_X, 0.0f, 150, interpolator);
        }
        arraySet.add(propertyAnimator4);
        arraySet.add(getPropertyAnimator(mGreen, FrameLayout.SCALE_X, 1.0f, 200, interpolator));
        arraySet.add(getPropertyAnimator(mGreen, FrameLayout.SCALE_Y, 1.0f, 200, interpolator));
        Interpolator interpolator2 = Interpolators.FAST_OUT_SLOW_IN;
        Animator propertyAnimator5 = getPropertyAnimator(mWhite, FrameLayout.SCALE_X, 1.0f, 150, interpolator2);
        Animator propertyAnimator6 = getPropertyAnimator(mWhite, FrameLayout.SCALE_Y, 1.0f, 150, interpolator2);
        Animator propertyAnimator7 = getPropertyAnimator(mWhiteCutout, FrameLayout.SCALE_X, 1.0f, 150, interpolator2);
        Animator propertyAnimator8 = getPropertyAnimator(mWhiteCutout, FrameLayout.SCALE_Y, 1.0f, 150, interpolator2);
        Animator propertyAnimator9 = getPropertyAnimator(mHalo, FrameLayout.SCALE_X, 1.0f, 150, interpolator2);
        Animator propertyAnimator10 = getPropertyAnimator(mHalo, FrameLayout.SCALE_Y, 1.0f, 150, interpolator2);
        Animator propertyAnimator11 = getPropertyAnimator(mHalo, FrameLayout.ALPHA, 1.0f, 150, interpolator2);
        propertyAnimator5.setStartDelay(33L);
        propertyAnimator6.setStartDelay(33L);
        propertyAnimator7.setStartDelay(33L);
        propertyAnimator8.setStartDelay(33L);
        propertyAnimator9.setStartDelay(33L);
        propertyAnimator10.setStartDelay(33L);
        propertyAnimator11.setStartDelay(33L);
        arraySet.add(propertyAnimator5);
        arraySet.add(propertyAnimator6);
        arraySet.add(propertyAnimator7);
        arraySet.add(propertyAnimator8);
        arraySet.add(propertyAnimator9);
        arraySet.add(propertyAnimator10);
        arraySet.add(propertyAnimator11);
        getLongestAnim(arraySet).addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                Trace.beginSection("OpaLayout.start.collapse");
                Trace.endSection();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                Trace.beginSection("OpaLayout.cancel.collapse");
                Trace.endSection();
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                Trace.beginSection("OpaLayout.end.collapse");
                Trace.endSection();
                mCurrentAnimators.clear();
                skipToStartingValue();
            }
        });
        return arraySet;
    }

    private ArraySet<Animator> getLineAnimatorSet() {
        ArraySet<Animator> arraySet = new ArraySet<>();
        if (mIsVertical) {
            Interpolator interpolator = Interpolators.FAST_OUT_SLOW_IN;
            arraySet.add(getPropertyAnimator(mRed, View.Y, mRed.getY() + OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), 225, interpolator));
            arraySet.add(getPropertyAnimator(mRed, View.X, mRed.getX() + OpaUtils.getPxVal(mResources, R.dimen.opa_line_y_translation), 133, interpolator));
            arraySet.add(getPropertyAnimator(mBlue, View.Y, mBlue.getY() + OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), 225, interpolator));
            arraySet.add(getPropertyAnimator(mYellow, View.Y, mYellow.getY() + (-OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry)), 225, interpolator));
            arraySet.add(getPropertyAnimator(mYellow, View.X, mYellow.getX() + (-OpaUtils.getPxVal(mResources, R.dimen.opa_line_y_translation)), 133, interpolator));
            arraySet.add(getPropertyAnimator(mGreen, View.Y, mGreen.getY() + (-OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg)), 225, interpolator));
        } else {
            Interpolator interpolator = Interpolators.FAST_OUT_SLOW_IN;
            arraySet.add(getPropertyAnimator(mRed, View.X, mRed.getX() + (-OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry)), 225, interpolator));
            arraySet.add(getPropertyAnimator(mRed, View.Y, mRed.getY() + OpaUtils.getPxVal(mResources, R.dimen.opa_line_y_translation), 133, interpolator));
            arraySet.add(getPropertyAnimator(mBlue, View.X, mBlue.getX() + (-OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg)), 225, interpolator));
            arraySet.add(getPropertyAnimator(mYellow, View.X, mYellow.getX() + OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), 225, interpolator));
            arraySet.add(getPropertyAnimator(mYellow, View.Y, mYellow.getY() + (-OpaUtils.getPxVal(mResources, R.dimen.opa_line_y_translation)), 133, interpolator));
            arraySet.add(getPropertyAnimator(mGreen, View.X, mGreen.getX() + OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), 225, interpolator));
        }
        arraySet.add(getPropertyAnimator(mWhite, FrameLayout.SCALE_X, 0.0f, 83, HOME_DISAPPEAR_INTERPOLATOR));
        arraySet.add(getPropertyAnimator(mWhite, FrameLayout.SCALE_Y, 0.0f, 83, HOME_DISAPPEAR_INTERPOLATOR));
        arraySet.add(getPropertyAnimator(mWhiteCutout, FrameLayout.SCALE_X, 0.0f, 83, HOME_DISAPPEAR_INTERPOLATOR));
        arraySet.add(getPropertyAnimator(mWhiteCutout, FrameLayout.SCALE_Y, 0.0f, 83, HOME_DISAPPEAR_INTERPOLATOR));
        arraySet.add(getPropertyAnimator(mHalo, FrameLayout.SCALE_X, 0.0f, 83, HOME_DISAPPEAR_INTERPOLATOR));
        arraySet.add(getPropertyAnimator(mHalo, FrameLayout.SCALE_Y, 0.0f, 83, HOME_DISAPPEAR_INTERPOLATOR));
        getLongestAnim(arraySet).addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                Trace.beginSection("OpaLayout.start.line");
                Trace.endSection();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                Trace.beginSection("OpaLayout.cancel.line");
                Trace.endSection();
                mCurrentAnimators.clear();
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                Trace.beginSection("OpaLayout.end.line");
                Trace.endSection();
                startCollapseAnimation();
            }
        });
        return arraySet;
    }

    public boolean getOpaEnabled() {
        if (mOpaEnabledNeedsUpdate) {
            ((AssistManagerGoogle) Dependency.get(AssistManager.class)).dispatchOpaEnabledState();
            if (mOpaEnabledNeedsUpdate) {
                Log.w("OpaLayout", "mOpaEnabledNeedsUpdate not cleared by AssistManagerGoogle!");
            }
        }
        return mOpaEnabled;
    }

    public void setOpaEnabled(boolean z) {
        Log.i("OpaLayout", "Setting opa enabled to " + z);
        mOpaEnabled = z;
        mOpaEnabledNeedsUpdate = false;
        updateOpaLayout();
    }

    public void updateOpaLayout() {
        boolean shouldShowSwipeUpUI = mOverviewProxyService.shouldShowSwipeUpUI();
        boolean z = true;
        boolean z2 = mOpaEnabled && !shouldShowSwipeUpUI;
        mHalo.setVisibility(z2 ? 0 : 4);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mHalo.getLayoutParams();
        if (z2 || shouldShowSwipeUpUI) {
            z = false;
        }
        int i = z ? mHomeDiameter : -1;
        layoutParams.width = i;
        layoutParams.height = i;
        mWhite.setLayoutParams(layoutParams);
        mWhiteCutout.setLayoutParams(layoutParams);
        ImageView.ScaleType scaleType = z ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.CENTER;
        mWhite.setScaleType(scaleType);
        mWhiteCutout.setScaleType(scaleType);
    }

    private void cancelCurrentAnimation(String str) {
        Trace.beginSection("OpaLayout.cancelCurrentAnimation: reason=" + str);
        Trace.endSection();
        if (!mCurrentAnimators.isEmpty()) {
            for (int size = mCurrentAnimators.size() - 1; size >= 0; size--) {
                Animator valueAt = mCurrentAnimators.valueAt(size);
                valueAt.removeAllListeners();
                valueAt.cancel();
            }
            mCurrentAnimators.clear();
            mAnimationState = 0;
        }
        if (mGestureAnimatorSet != null) {
            mGestureAnimatorSet.cancel();
            mGestureState = 0;
        }
    }

    private void endCurrentAnimation(String str) {
        Trace.beginSection("OpaLayout.endCurrentAnimation: reason=" + str);
        if (!mCurrentAnimators.isEmpty()) {
            for (int size = mCurrentAnimators.size() - 1; size >= 0; size--) {
                Animator valueAt = mCurrentAnimators.valueAt(size);
                valueAt.removeAllListeners();
                valueAt.end();
            }
            mCurrentAnimators.clear();
        }
        mAnimationState = 0;
    }

    private Animator getLongestAnim(ArraySet<Animator> arraySet) {
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

    private void setDotsVisible() {
        int size = mAnimatedViews.size();
        for (int i = 0; i < size; i++) {
            mAnimatedViews.get(i).setAlpha(1.0f);
        }
    }

    private void skipToStartingValue() {
        int size = mAnimatedViews.size();
        for (int i = 0; i < size; i++) {
            View view = mAnimatedViews.get(i);
            view.setScaleY(1.0f);
            view.setScaleX(1.0f);
            view.setTranslationY(0.0f);
            view.setTranslationX(0.0f);
            view.setAlpha(0.0f);
        }
        mHalo.setAlpha(1.0f);
        mWhite.setAlpha(1.0f);
        mWhiteCutout.setAlpha(1.0f);
        mAnimationState = 0;
        mGestureState = 0;
    }

    @Override
    public void setVertical(boolean z) {
        if (mIsVertical != z && mGestureAnimatorSet != null) {
            mGestureAnimatorSet.cancel();
            mGestureAnimatorSet = null;
            skipToStartingValue();
        }
        mIsVertical = z;
        mHome.setVertical(z);
        if (mIsVertical) {
            mTop = mGreen;
            mBottom = mBlue;
            mRight = mYellow;
            mLeft = mRed;
            return;
        }
        mTop = mRed;
        mBottom = mYellow;
        mLeft = mBlue;
        mRight = mGreen;
    }

    @Override
    public void setDarkIntensity(float f) {
        if (mWhite.getDrawable() instanceof KeyButtonDrawable) {
            ((KeyButtonDrawable) mWhite.getDrawable()).setDarkIntensity(f);
        }
        ((KeyButtonDrawable) mHalo.getDrawable()).setDarkIntensity(f);
        mWhite.invalidate();
        mHalo.invalidate();
        mHome.setDarkIntensity(f);
    }

    @Override
    public void setDelayTouchFeedback(boolean z) {
        mHome.setDelayTouchFeedback(z);
        mDelayTouchFeedback = z;
    }

    @Override
    public void onRelease() {
        if (mAnimationState == 0 && mGestureState == 1) {
            if (mGestureAnimatorSet != null) {
                mGestureAnimatorSet.cancel();
            }
            mGestureState = 0;
            startRetractAnimation();
        }
    }

    @Override
    public void onProgress(float f, int i) {
        if (mGestureState == 2 || !allowAnimations()) {
            return;
        }
        if (mAnimationState == 2) {
            endCurrentAnimation("progress=" + f);
        }
        if (mAnimationState != 0) {
            return;
        }
        if (mGestureAnimatorSet == null) {
            mGestureAnimatorSet = getGestureAnimatorSet();
            mGestureAnimationSetDuration = mGestureAnimatorSet.getTotalDuration();
        }
        mGestureAnimatorSet.setCurrentPlayTime((long) (((float) (mGestureAnimationSetDuration - 1)) * f));
        if (f == 0.0f) {
            mGestureState = 0;
        } else {
            mGestureState = 1;
        }
    }

    @Override
    public void onResolve(GestureSensor.DetectionProperties detectionProperties) {
        if (mAnimationState != 0) {
            return;
        }
        if (mGestureState == 1 && mGestureAnimatorSet != null && !mGestureAnimatorSet.isStarted()) {
            mGestureAnimatorSet.start();
            mGestureState = 2;
            return;
        }
        skipToStartingValue();
    }

    private AnimatorSet getGestureAnimatorSet() {
        if (mGestureLineSet != null) {
            mGestureLineSet.removeAllListeners();
            mGestureLineSet.cancel();
            return mGestureLineSet;
        }
        mGestureLineSet = new AnimatorSet();
        Interpolator interpolator = OpaUtils.INTERPOLATOR_40_OUT;
        ObjectAnimator scaleObjectAnimator = OpaUtils.getScaleObjectAnimator(mWhite, 0.0f, 100, interpolator);
        ObjectAnimator scaleObjectAnimator2 = OpaUtils.getScaleObjectAnimator(mWhiteCutout, 0.0f, 100, interpolator);
        ObjectAnimator scaleObjectAnimator3 = OpaUtils.getScaleObjectAnimator(mHalo, 0.0f, 100, interpolator);
        scaleObjectAnimator.setStartDelay(50L);
        scaleObjectAnimator2.setStartDelay(50L);
        mGestureLineSet.play(scaleObjectAnimator).with(scaleObjectAnimator2).with(scaleObjectAnimator3);
        Interpolator interpolator2 = Interpolators.FAST_OUT_SLOW_IN;
        AnimatorSet.Builder with = mGestureLineSet.play(OpaUtils.getScaleObjectAnimator(mTop, 0.8f, 200, interpolator2)).with(scaleObjectAnimator);
        Interpolator interpolator3 = Interpolators.LINEAR;
        with.with(OpaUtils.getAlphaObjectAnimator(mRed, 1.0f, 50, 130, interpolator3)).with(OpaUtils.getAlphaObjectAnimator(mYellow, 1.0f, 50, 130, interpolator3)).with(OpaUtils.getAlphaObjectAnimator(mBlue, 1.0f, 50, 113, interpolator3)).with(OpaUtils.getAlphaObjectAnimator(mGreen, 1.0f, 50, 113, interpolator3)).with(OpaUtils.getScaleObjectAnimator(mBottom, 0.8f, 200, interpolator2)).with(OpaUtils.getScaleObjectAnimator(mLeft, 0.8f, 200, interpolator2)).with(OpaUtils.getScaleObjectAnimator(mRight, 0.8f, 200, interpolator2));
        if (mIsVertical) {
            Interpolator interpolator4 = OpaUtils.INTERPOLATOR_40_40;
            ObjectAnimator translationObjectAnimatorY = OpaUtils.getTranslationObjectAnimatorY(mRed, interpolator4, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), mRed.getY() + OpaUtils.getDeltaDiamondPositionLeftY(), 350);
            translationObjectAnimatorY.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    startCollapseAnimation();
                }
            });
            mGestureLineSet.play(translationObjectAnimatorY).with(scaleObjectAnimator3).with(OpaUtils.getTranslationObjectAnimatorY(mBlue, interpolator4, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), mBlue.getY() + OpaUtils.getDeltaDiamondPositionBottomY(mResources), 350)).with(OpaUtils.getTranslationObjectAnimatorY(mYellow, interpolator4, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), mYellow.getY() + OpaUtils.getDeltaDiamondPositionRightY(), 350)).with(OpaUtils.getTranslationObjectAnimatorY(mGreen, interpolator4, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), mGreen.getY() + OpaUtils.getDeltaDiamondPositionTopY(mResources), 350));
        } else {
            Interpolator interpolator5 = OpaUtils.INTERPOLATOR_40_40;
            ObjectAnimator translationObjectAnimatorX = OpaUtils.getTranslationObjectAnimatorX(mRed, interpolator5, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), mRed.getX() + OpaUtils.getDeltaDiamondPositionTopX(), 350);
            translationObjectAnimatorX.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    startCollapseAnimation();
                }
            });
            mGestureLineSet.play(translationObjectAnimatorX).with(scaleObjectAnimator).with(OpaUtils.getTranslationObjectAnimatorX(mBlue, interpolator5, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), mBlue.getX() + OpaUtils.getDeltaDiamondPositionLeftX(mResources), 350)).with(OpaUtils.getTranslationObjectAnimatorX(mYellow, interpolator5, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), mYellow.getX() + OpaUtils.getDeltaDiamondPositionBottomX(), 350)).with(OpaUtils.getTranslationObjectAnimatorX(mGreen, interpolator5, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), mGreen.getX() + OpaUtils.getDeltaDiamondPositionRightX(mResources), 350));
        }
        return mGestureLineSet;
    }

    private Animator getPropertyAnimator(View view, Property<View, Float> property, float f, int i, Interpolator interpolator) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, property, f);
        ofFloat.setDuration(i);
        ofFloat.setInterpolator(interpolator);
        return ofFloat;
    }
}
