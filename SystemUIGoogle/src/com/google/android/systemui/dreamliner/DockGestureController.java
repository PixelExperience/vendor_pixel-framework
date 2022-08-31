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

package com.google.android.systemui.dreamliner;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.dynamicanimation.animation.DynamicAnimation;

import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.R;
import com.android.systemui.animation.Interpolators;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.wm.shell.animation.PhysicsAnimator;

import java.util.concurrent.TimeUnit;

public class DockGestureController extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener, StatusBarStateController.StateListener, KeyguardStateController.Callback, ConfigurationController.ConfigurationListener {
    private static final long GEAR_VISIBLE_TIME_MILLIS;
    private static final long PREVIEW_DELAY_MILLIS;

    static {
        TimeUnit timeUnit = TimeUnit.SECONDS;
        GEAR_VISIBLE_TIME_MILLIS = timeUnit.toMillis(15L);
        PREVIEW_DELAY_MILLIS = timeUnit.toMillis(1L);
    }

    final DockIndicationController mDockIndicationController;
    private final AccessibilityManager mAccessibilityManager;
    private final Context mContext;
    private final KeyguardStateController mKeyguardStateController;
    private final int mPhotoDiffThreshold;
    private final FrameLayout mPhotoPreview;
    private final TextView mPhotoPreviewText;
    private final ImageView mSettingsGear;
    private final StatusBarStateController mStatusBarStateController;
    private final View mTouchDelegateView;
    private final PhysicsAnimator.SpringConfig mTargetSpringConfig = new PhysicsAnimator.SpringConfig(1500.0f, 1.0f);
    private final PhysicsAnimator<View> mPreviewTargetAnimator;
    @VisibleForTesting
    GestureDetector mGestureDetector;
    private float mDiffX;
    private float mFirstTouchX;
    private float mFirstTouchY;
    private boolean mFromRight;
    private float mLastTouchX;
    private boolean mLaunchedPhoto;
    private boolean mPhotoEnabled;
    private boolean mShouldConsumeTouch;
    private PendingIntent mTapAction;
    private boolean mTriggerPhoto;
    private VelocityTracker mVelocityTracker;
    private float mVelocityX;
    private final Runnable mHideGearRunnable = this::hideGear;
    private final KeyguardStateController.Callback mKeyguardMonitorCallback = new KeyguardStateController.Callback() {
        @Override
        public void onKeyguardShowingChanged() {
            if (mKeyguardStateController.isOccluded()) {
                hidePhotoPreview(false);
            }
        }
    };

    DockGestureController(Context context, ImageView imageView, FrameLayout frameLayout, View view, DockIndicationController dockIndicationController, StatusBarStateController statusBarStateController, KeyguardStateController keyguardStateController) {
        mDockIndicationController = dockIndicationController;
        mContext = context;
        mGestureDetector = new GestureDetector(context, this);
        mTouchDelegateView = view;
        mSettingsGear = imageView;
        mPhotoPreview = frameLayout;
        TextView textView = frameLayout.findViewById(R.id.photo_preview_text);
        mPhotoPreviewText = textView;
        textView.setText(context.getResources().getString(R.string.dock_photo_preview_text));
        imageView.setOnClickListener(v -> {
            hideGear();
            sendProtectedBroadcast(new Intent("com.google.android.apps.dreamliner.SETTINGS"));
        });
        mAccessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
        mPhotoDiffThreshold = context.getResources().getDimensionPixelSize(R.dimen.dock_photo_diff);
        mStatusBarStateController = statusBarStateController;
        mKeyguardStateController = keyguardStateController;
        mPreviewTargetAnimator = PhysicsAnimator.getInstance(frameLayout);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        initVelocityTracker();
        float x = motionEvent.getX();
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            mVelocityTracker.clear();
            mFirstTouchX = x;
            mFirstTouchY = motionEvent.getY();
            mLaunchedPhoto = false;
            mFromRight = false;
            DockIndicationController dockIndicationController = mDockIndicationController;
            boolean z = dockIndicationController == null || !dockIndicationController.isDockedTopIconTouched(motionEvent);
            mShouldConsumeTouch = z;
            if (z && x > mPhotoPreview.getRight() - mPhotoDiffThreshold) {
                mFromRight = true;
                if (mPhotoEnabled) {
                    mPhotoPreview.setVisibility(0);
                    CrossFadeHelper.fadeIn(mPhotoPreview, 100L, 0);
                }
            }
        } else if (actionMasked == 1) {
            mVelocityTracker.computeCurrentVelocity(1000);
            mVelocityX = mVelocityTracker.getXVelocity();
            if (Math.signum(mDiffX) == Math.signum(mVelocityX) || mLastTouchX > mPhotoPreview.getRight() - mPhotoDiffThreshold) {
                mTriggerPhoto = false;
            }
            if (mTriggerPhoto && mPhotoPreview.getVisibility() == 0) {
                sendProtectedBroadcast(new Intent("com.google.android.systemui.dreamliner.PHOTO_EVENT"));
                mPhotoPreview.post(() -> mPreviewTargetAnimator.spring(DynamicAnimation.TRANSLATION_X, 0.0f, mVelocityX, mTargetSpringConfig).start());
                mLaunchedPhoto = true;
                mTriggerPhoto = false;
            } else {
                hidePhotoPreview(true);
            }
        } else if (actionMasked == 2) {
            handleMoveEvent(motionEvent);
        }
        mLastTouchX = x;
        if (mShouldConsumeTouch) {
            mGestureDetector.onTouchEvent(motionEvent);
        }
        return mShouldConsumeTouch;
    }

    private void handleMoveEvent(MotionEvent motionEvent) {
        if (!mFromRight || !mPhotoEnabled) {
            return;
        }
        float x = motionEvent.getX();
        float f = mLastTouchX;
        mPhotoPreview.setTranslationX(f + (x - f));
        mVelocityTracker.addMovement(motionEvent);
        mDiffX = mFirstTouchX - x;
        if (Math.abs(mDiffX) <= Math.abs(mFirstTouchY - motionEvent.getY()) || Math.abs(mDiffX) <= mPhotoDiffThreshold) {
            return;
        }
        mTriggerPhoto = true;
    }

    private void hidePhotoPreview(boolean z) {
        if (mPhotoPreview.getVisibility() != 0) {
            return;
        }
        if (z) {
            mPhotoPreview.post(() -> mPreviewTargetAnimator.spring(DynamicAnimation.TRANSLATION_X, mPhotoPreview.getRight(), mVelocityX, mTargetSpringConfig).withEndActions(() -> {
                mPhotoPreview.setAlpha(0.0f);
                mPhotoPreview.setVisibility(4);
            }).start());
            return;
        }
        mPhotoPreview.setAlpha(0.0f);
        mPhotoPreview.setVisibility(4);
    }

    private void initVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        PendingIntent pendingIntent = mTapAction;
        if (pendingIntent != null) {
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.w("DLGestureController", "Tap action pending intent cancelled", e);
            }
        }
        showGear();
        return false;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        sendProtectedBroadcast(new Intent("com.google.android.systemui.dreamliner.TOUCH_EVENT"));
        return false;
    }

    @Override
    public void onDozingChanged(boolean z) {
        if (z) {
            mTouchDelegateView.setOnTouchListener(this);
            showGear();
            return;
        }
        mTouchDelegateView.setOnTouchListener(null);
        hideGear();
        if (!mLaunchedPhoto) {
            hidePhotoPreview(true);
        } else {
            mPhotoPreview.postDelayed(() -> hidePhotoPreview(false), PREVIEW_DELAY_MILLIS);
        }
    }

    @Override
    public void onLocaleListChanged() {
        mPhotoPreviewText.setText(mContext.getResources().getString(R.string.dock_photo_preview_text));
    }

    void setPhotoEnabled(boolean z) {
        mPhotoEnabled = z;
    }

    void handlePhotoFailure() {
        hidePhotoPreview(false);
    }

    void setTapAction(PendingIntent pendingIntent) {
        mTapAction = pendingIntent;
    }

    void startMonitoring() {
        mSettingsGear.setVisibility(4);
        onDozingChanged(mStatusBarStateController.isDozing());
        mStatusBarStateController.addCallback(this);
        mKeyguardStateController.addCallback(mKeyguardMonitorCallback);
    }

    void stopMonitoring() {
        mStatusBarStateController.removeCallback(this);
        mKeyguardStateController.removeCallback(mKeyguardMonitorCallback);
        onDozingChanged(false);
        mSettingsGear.setVisibility(8);
    }

    private void showGear() {
        if (mTapAction != null) {
            return;
        }
        if (!mSettingsGear.isVisibleToUser()) {
            mSettingsGear.setVisibility(0);
            mSettingsGear.animate().setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN).alpha(1.0f).start();
        }
        mSettingsGear.removeCallbacks(mHideGearRunnable);
        mSettingsGear.postDelayed(mHideGearRunnable, getRecommendedTimeoutMillis());
    }

    private void hideGear() {
        if (mSettingsGear.isVisibleToUser()) {
            mSettingsGear.removeCallbacks(mHideGearRunnable);
            mSettingsGear.animate().setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN).alpha(0.0f).withEndAction(() -> mSettingsGear.setVisibility(4)).start();
        }
    }

    private void sendProtectedBroadcast(Intent intent) {
        try {
            mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        } catch (SecurityException e) {
            Log.w("DLGestureController", "Cannot send event", e);
        }
    }

    private long getRecommendedTimeoutMillis() {
        AccessibilityManager accessibilityManager = mAccessibilityManager;
        return accessibilityManager == null ? GEAR_VISIBLE_TIME_MILLIS : accessibilityManager.getRecommendedTimeoutMillis(Math.toIntExact(GEAR_VISIBLE_TIME_MILLIS), 5);
    }
}
