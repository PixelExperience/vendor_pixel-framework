package com.google.android.systemui.elmyra.feedback;

import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.CentralSurfaces;
import com.android.systemui.statusbar.phone.KeyguardBottomAreaView;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.google.android.systemui.elmyra.sensors.GestureSensor;

public class OpaLockscreen implements FeedbackEffect {
    private static final Interpolator LOCK_ICON_HIDE_INTERPOLATOR = new DecelerateInterpolator();
    private static final Interpolator LOCK_ICON_SHOW_INTERPOLATOR = new AccelerateInterpolator();

    private KeyguardBottomAreaView mKeyguardBottomAreaView;
    private final KeyguardStateController mKeyguardStateController;
    private FeedbackEffect mLockscreenOpaLayout;
    private final CentralSurfaces mCentralSurfaces;

    public OpaLockscreen(CentralSurfaces centralSurfaces, KeyguardStateController keyguardStateController) {
        mCentralSurfaces = centralSurfaces;
        mKeyguardStateController = keyguardStateController;
        refreshLockscreenOpaLayout();
    }

    private void refreshLockscreenOpaLayout() {
        KeyguardBottomAreaView keyguardBottomAreaView = mCentralSurfaces.getNotificationPanelViewController().mKeyguardBottomArea;
        if (keyguardBottomAreaView != null && mKeyguardStateController.isShowing()) {
            if (mLockscreenOpaLayout == null || !keyguardBottomAreaView.equals(mKeyguardBottomAreaView)) {
                mKeyguardBottomAreaView = keyguardBottomAreaView;
                FeedbackEffect feedbackEffect = mLockscreenOpaLayout;
                if (feedbackEffect != null) {
                    feedbackEffect.onRelease();
                }
                mLockscreenOpaLayout = keyguardBottomAreaView.findViewById(R.id.lockscreen_opa);
                return;
            }
            return;
        }
        mKeyguardBottomAreaView = null;
        mLockscreenOpaLayout = null;
    }

    @Override
    public void onProgress(float f, int i) {
        refreshLockscreenOpaLayout();
        FeedbackEffect feedbackEffect = mLockscreenOpaLayout;
        if (feedbackEffect != null) {
            feedbackEffect.onProgress(f, i);
        }
    }

    @Override
    public void onRelease() {
        refreshLockscreenOpaLayout();
        FeedbackEffect feedbackEffect = mLockscreenOpaLayout;
        if (feedbackEffect != null) {
            feedbackEffect.onRelease();
        }
    }

    @Override
    public void onResolve(GestureSensor.DetectionProperties detectionProperties) {
        refreshLockscreenOpaLayout();
        FeedbackEffect feedbackEffect = mLockscreenOpaLayout;
        if (feedbackEffect != null) {
            feedbackEffect.onResolve(detectionProperties);
        }
    }
}
