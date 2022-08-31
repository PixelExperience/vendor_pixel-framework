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

import android.content.Context;
import android.content.Intent;
import android.graphics.RectF;
import android.os.ResultReceiver;
import android.os.UserHandle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.R;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.SysuiStatusBarStateController;
import com.android.systemui.statusbar.phone.KeyguardIndicationTextView;
import com.android.systemui.statusbar.phone.NotificationShadeWindowView;
import com.android.systemui.statusbar.phone.CentralSurfaces;
import com.android.systemui.statusbar.policy.ConfigurationController;

import java.util.concurrent.TimeUnit;

public class DockIndicationController implements StatusBarStateController.StateListener, View.OnClickListener, View.OnAttachStateChangeListener, ConfigurationController.ConfigurationListener {
    @VisibleForTesting
    static final String ACTION_ASSISTANT_POODLE = "com.google.android.systemui.dreamliner.ASSISTANT_POODLE";
    private static final long KEYGUARD_INDICATION_TIMEOUT_MILLIS;
    private static final long PROMO_SHOWING_TIME_MILLIS;

    static {
        TimeUnit timeUnit = TimeUnit.SECONDS;
        PROMO_SHOWING_TIME_MILLIS = timeUnit.toMillis(2L);
        KEYGUARD_INDICATION_TIMEOUT_MILLIS = timeUnit.toMillis(15L);
    }

    private final AccessibilityManager mAccessibilityManager;
    private final Context mContext;
    private final Runnable mDisableLiveRegionRunnable;
    private final Animation mHidePromoAnimation;
    private final Runnable mHidePromoRunnable;
    private final KeyguardIndicationController mKeyguardIndicationController;
    private final Animation mShowPromoAnimation;
    private final CentralSurfaces mCentralSurfaces;
    @VisibleForTesting
    FrameLayout mDockPromo;
    @VisibleForTesting
    ImageView mDockedTopIcon;
    @VisibleForTesting
    boolean mIconViewsValidated;
    private boolean mDocking;
    private boolean mDozing;
    private TextView mPromoText;
    private boolean mShowPromo;
    private int mShowPromoTimes;
    private int mStatusBarState;
    private boolean mTopIconShowing;
    private KeyguardIndicationTextView mTopIndicationView;

    public DockIndicationController(Context context, KeyguardIndicationController keyguardIndicationController, SysuiStatusBarStateController sysuiStatusBarStateController, CentralSurfaces centralSurfaces) {
        mContext = context;
        mCentralSurfaces = centralSurfaces;
        mKeyguardIndicationController = keyguardIndicationController;
        sysuiStatusBarStateController.addCallback(this);
        mHidePromoRunnable = this::hidePromo;
        mDisableLiveRegionRunnable = this::disableLiveRegion;
        Animation loadAnimation = AnimationUtils.loadAnimation(context, R.anim.dock_promo_animation);
        mShowPromoAnimation = loadAnimation;
        loadAnimation.setAnimationListener(new PhotoAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mDockPromo.postDelayed(mHidePromoRunnable, getRecommendedTimeoutMillis(DockIndicationController.PROMO_SHOWING_TIME_MILLIS));
            }
        });
        Animation loadAnimation2 = AnimationUtils.loadAnimation(context, R.anim.dock_promo_fade_out);
        mHidePromoAnimation = loadAnimation2;
        loadAnimation2.setAnimationListener(new PhotoAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                if (mShowPromoTimes < 3) {
                    showPromoInner();
                    return;
                }
                mKeyguardIndicationController.setVisible(true);
                mDockPromo.setVisibility(8);
            }
        });
        mAccessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
    }

    @Override
    public void onViewAttachedToWindow(View view) {
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.docked_top_icon) {
            Intent intent = new Intent(ACTION_ASSISTANT_POODLE);
            intent.addFlags(1073741824);
            try {
                mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            } catch (SecurityException e) {
                Log.w("DLIndicator", "Cannot send event for intent= " + intent, e);
            }
        }
    }

    @Override
    public void onDozingChanged(boolean z) {
        mDozing = z;
        updateVisibility();
        updateLiveRegionIfNeeded();
        if (!mDozing) {
            mShowPromo = false;
        } else {
            showPromoInner();
        }
    }

    @Override
    public void onStateChanged(int i) {
        mStatusBarState = i;
        updateVisibility();
    }

    @Override
    public void onViewDetachedFromWindow(View view) {
        view.removeOnAttachStateChangeListener(this);
        mIconViewsValidated = false;
        mDockedTopIcon = null;
    }

    @Override
    public void onLocaleListChanged() {
        if (!mIconViewsValidated) {
            initializeIconViews();
        }
        mPromoText.setText(mContext.getResources().getString(R.string.dock_promo_text));
    }

    public void setShowing(boolean z) {
        mTopIconShowing = z;
        updateVisibility();
    }

    public void setDocking(boolean z) {
        mDocking = z;
        if (!z) {
            mTopIconShowing = false;
            mShowPromo = false;
        }
        updateVisibility();
        updateLiveRegionIfNeeded();
    }

    @VisibleForTesting
    void initializeIconViews() {
        NotificationShadeWindowView notificationShadeWindowView = mCentralSurfaces.getNotificationShadeWindowView();
        ImageView imageView = (ImageView) notificationShadeWindowView.findViewById(R.id.docked_top_icon);
        mDockedTopIcon = imageView;
        imageView.setImageResource(R.drawable.ic_assistant_logo);
        ImageView imageView2 = mDockedTopIcon;
        int i = R.string.accessibility_assistant_poodle;
        imageView2.setContentDescription(mContext.getString(i));
        mDockedTopIcon.setTooltipText(mContext.getString(i));
        mDockedTopIcon.setOnClickListener(this);
        mDockPromo = (FrameLayout) notificationShadeWindowView.findViewById(R.id.dock_promo);
        TextView textView = (TextView) notificationShadeWindowView.findViewById(R.id.photo_promo_text);
        mPromoText = textView;
        textView.setAutoSizeTextTypeUniformWithConfiguration(10, 16, 1, 2);
        notificationShadeWindowView.findViewById(R.id.ambient_indication).addOnAttachStateChangeListener(this);
        mTopIndicationView = (KeyguardIndicationTextView) notificationShadeWindowView.findViewById(R.id.keyguard_indication_text);
        mIconViewsValidated = true;
    }

    public void showPromo(ResultReceiver resultReceiver) {
        mShowPromoTimes = 0;
        mShowPromo = true;
        if (mDozing && mDocking) {
            showPromoInner();
            resultReceiver.send(0, null);
            return;
        }
        resultReceiver.send(1, null);
    }

    public boolean isPromoShowing() {
        return mDockPromo.getVisibility() == 0;
    }

    boolean isDockedTopIconTouched(MotionEvent motionEvent) {
        ImageView imageView;
        if (motionEvent == null || (imageView = mDockedTopIcon) == null) {
            return false;
        }
        int[] iArr = new int[2];
        imageView.getLocationOnScreen(iArr);
        boolean contains = new RectF(iArr[0], iArr[1], iArr[0] + mDockedTopIcon.getWidth(), iArr[1] + mDockedTopIcon.getHeight()).contains(motionEvent.getRawX(), motionEvent.getRawY());
        Log.d("DLIndicator", "dockedTopIcon touched=" + contains);
        return contains;
    }

    private void showPromoInner() {
        if (!mDozing || !mDocking || !mShowPromo) {
            return;
        }
        mKeyguardIndicationController.setVisible(false);
        mDockPromo.setVisibility(0);
        mDockPromo.startAnimation(mShowPromoAnimation);
        mShowPromoTimes++;
    }

    private void hidePromo() {
        if (!mDozing || !mDocking) {
            return;
        }
        mDockPromo.startAnimation(mHidePromoAnimation);
    }

    private void updateVisibility() {
        if (!mIconViewsValidated) {
            initializeIconViews();
        }
        boolean z = false;
        if (!mDozing || !mDocking) {
            mDockPromo.setVisibility(8);
            mDockedTopIcon.setVisibility(8);
            int i = mStatusBarState;
            if (i == 1 || i == 2) {
                z = true;
            }
            mKeyguardIndicationController.setVisible(z);
        } else if (!mTopIconShowing) {
            mDockedTopIcon.setVisibility(8);
        } else {
            mDockedTopIcon.setVisibility(0);
        }
    }

    private void updateLiveRegionIfNeeded() {
        int accessibilityLiveRegion = mTopIndicationView.getAccessibilityLiveRegion();
        if (mDozing && mDocking) {
            mTopIndicationView.removeCallbacks(mDisableLiveRegionRunnable);
            mTopIndicationView.postDelayed(mDisableLiveRegionRunnable, getRecommendedTimeoutMillis(KEYGUARD_INDICATION_TIMEOUT_MILLIS));
        } else if (accessibilityLiveRegion != 1) {
            mTopIndicationView.setAccessibilityLiveRegion(1);
        }
    }

    private void disableLiveRegion() {
        if (!mDocking || !mDozing) {
            return;
        }
        mTopIndicationView.setAccessibilityLiveRegion(0);
    }

    private long getRecommendedTimeoutMillis(long j) {
        return mAccessibilityManager == null ? j : mAccessibilityManager.getRecommendedTimeoutMillis(Math.toIntExact(j), 2);
    }

    private static class PhotoAnimationListener implements Animation.AnimationListener {
        private PhotoAnimationListener() {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        }
    }
}
