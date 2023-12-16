/*
 * Copyright (C) 2022 The PixelExperience Project
 * Copyright (C) 2023 The risingOS Android Project
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
package com.google.android.systemui.ambientmusic;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.media.MediaMetadata;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import com.android.app.animation.Interpolators;
import com.android.keyguard.KeyguardUpdateMonitor;

import com.android.internal.util.ArrayUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.Dependency;
import com.android.systemui.doze.DozeReceiver;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.shade.NotificationPanelViewController;
import com.android.systemui.shade.ShadeViewController;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.phone.CentralSurfaces;
import com.android.systemui.statusbar.phone.CentralSurfacesImpl;
import com.android.systemui.util.wakelock.DelayedWakeLock;
import com.android.systemui.util.wakelock.WakeLock;
import com.android.wm.shell.R;
import java.util.Objects;

public class AmbientIndicationContainer extends AutoReinflateContainer implements DozeReceiver, StatusBarStateController.StateListener, NotificationMediaManager.MediaListener {
    public Drawable mAmbientIconOverride;
    public ConstraintLayout mAmbientIndicationContainer;
    public int mAmbientIndicationIconSize;
    public Drawable mAmbientMusicAnimation;
    public Drawable mAmbientMusicNoteIcon;
    public int mAmbientMusicNoteIconIconSize;
    public CharSequence mAmbientMusicText;
    public boolean mAmbientSkipUnlock;
    public int mBottomMarginPx;
    public CentralSurfaces mCentralSurfaces;
    public boolean mDozing;
    public PendingIntent mFavoritingIntent;
    public final Handler mHandler;
    public final Rect mIconBounds;
    public String mIconDescription;
    public int mIconOverride;
    public ImageView mIconView;
    public int mIndicationTextMode;
    public boolean mInflated;
    public int mMediaPlaybackState;
    public PendingIntent mOpenIntent;
    public Drawable mReverseChargingAnimation;
    public CharSequence mReverseChargingMessage;
    public int mStatusBarState;
    public int mTextColor;
    public ValueAnimator mTextColorAnimator;
    public TextView mTextView;
    public final WakeLock mWakeLock;
    public CharSequence mWirelessChargingMessage;

    public AmbientIndicationContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mIconBounds = new Rect();
        mIconOverride = -1;
        Handler handler = new Handler(Looper.getMainLooper());
        mHandler = handler;
        mWakeLock = createWakeLock(context, handler);
        mContext = context;
    }

    @VisibleForTesting
    WakeLock createWakeLock(Context context, Handler handler) {
        return new DelayedWakeLock(handler, WakeLock.createPartial(context, null, "AmbientIndication"));
    }

    public void initializeView(Context context, CentralSurfaces centralSurfaces, AmbientIndicationContainer ambientIndicationContainer) {
        mCentralSurfaces = centralSurfaces;
        addInflateListener(new AutoReinflateContainer.InflateListener() {
          @Override
            public void onInflated(View view) {
                mTextView = (TextView) findViewById(R.id.ambient_indication_text);
                mIconView = (ImageView) findViewById(R.id.ambient_indication_icon);
                if (mTextView == null || mIconView == null || mContext == null) {
                    return;
                }
                mAmbientIndicationContainer = (ConstraintLayout) findViewById(R.id.ambient_indication);
                ConstraintSet constraintSet = new ConstraintSet();
                int[] udfpsProps = context.getResources().getIntArray(
                        com.android.internal.R.array.config_udfps_sensor_props);
                if (!ArrayUtils.isEmpty(udfpsProps)) {
                    constraintSet.load(context, R.xml.ambient_indication_inner_downwards);
                } else {
                    constraintSet.load(context, R.xml.ambient_indication_inner_upwards);
                }
                constraintSet.applyTo(mAmbientIndicationContainer);
                mAmbientMusicAnimation = null;
                mAmbientMusicNoteIcon = null;
                mReverseChargingAnimation = null;
                mTextColor = mTextView.getCurrentTextColor();
                mAmbientIndicationIconSize = getResources().getDimensionPixelSize(R.dimen.ambient_indication_icon_size);
                mAmbientMusicNoteIconIconSize = getResources().getDimensionPixelSize(R.dimen.ambient_indication_note_icon_size);
                mTextView.setEnabled(!mDozing);
                updateColors();
                updatePill();
                mTextView.setOnClickListener((v) -> onTextClick());
                mIconView.setOnClickListener((v) -> onIconClick());
                mInflated = true;
            }
       });
       addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    updateBottomSpacing();
                });
    }

    public static void sendBroadcastWithoutDismissingKeyguard(PendingIntent pendingIntent) {
        if (pendingIntent.isActivity()) {
            return;
        }
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            Log.w("AmbientIndication", "Sending intent failed: " + e);
        }
    }

    @Override
    public void dozeTimeTick() {
        updatePill();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this);
        ((NotificationMediaManager) Dependency.get(NotificationMediaManager.class)).addCallback(this);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateBottomSpacing();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).removeCallback(this);
        ((NotificationMediaManager) Dependency.get(NotificationMediaManager.class)).removeCallback(this);
        mMediaPlaybackState = 0;
    }

    @Override
    public void onDozingChanged(boolean z) {
        mDozing = z;
        if (mStatusBarState == 1) {
            setVisibility(0);
        } else {
            setVisibility(4);
        }
        TextView textView = mTextView;
        if (textView != null) {
            textView.setEnabled(!z);
            updateColors();
        }
    }

    @Override
    public void onPrimaryMetadataOrStateChanged(MediaMetadata mediaMetadata, int i) {
        if (mMediaPlaybackState != i) {
            mMediaPlaybackState = i;
            if (NotificationMediaManager.isPlayingState(i)) {
                setAmbientMusic(null, null, null, 0, false, null);
            }
        }
    }

    @Override
    public void onStateChanged(int i) {
        mStatusBarState = i;
        if (i == 1) {
            setVisibility(0);
        } else {
            setVisibility(4);
        }
    }

    public void hideAmbientMusic() {
        setAmbientMusic(null, null, null, 0, false, null);
    }

    public void onTextClick() {
        if (mOpenIntent != null) {
            mCentralSurfaces.wakeUpDeviceifDozing();
            if (mAmbientSkipUnlock) {
                sendBroadcastWithoutDismissingKeyguard(mOpenIntent);
            } else {
                mCentralSurfaces.startPendingIntentDismissingKeyguard(mOpenIntent);
            }
        }
    }

    private void onIconClick() {
        if (mFavoritingIntent != null) {
            mCentralSurfaces.wakeUpDeviceifDozing();
            sendBroadcastWithoutDismissingKeyguard(mFavoritingIntent);
            return;
        }
        onTextClick();
    }

    public void setAmbientMusic(CharSequence charSequence, PendingIntent pendingIntent, PendingIntent pendingIntent2, int i, boolean z, String str) {
        Drawable drawable;
        if (Objects.equals(mAmbientMusicText, charSequence) && Objects.equals(mOpenIntent, pendingIntent) && Objects.equals(mFavoritingIntent, pendingIntent2) && mIconOverride == i && Objects.equals(mIconDescription, str) && mAmbientSkipUnlock == z) {
            return;
        }
        mAmbientMusicText = charSequence;
        mOpenIntent = pendingIntent;
        mFavoritingIntent = pendingIntent2;
        mAmbientSkipUnlock = z;
        mIconOverride = i;
        mIconDescription = str;
        switch (i) {
            case 1:
                drawable = mContext.getDrawable(R.drawable.ic_music_search);
                break;
            case 2:
            default:
                drawable = null;
                break;
            case 3:
                drawable = mContext.getDrawable(R.drawable.ic_music_not_found);
                break;
            case 4:
                drawable = mContext.getDrawable(R.drawable.ic_cloud_off);
                break;
            case 5:
                drawable = mContext.getDrawable(R.drawable.ic_favorite);
                break;
            case 6:
                drawable = mContext.getDrawable(R.drawable.ic_favorite_border);
                break;
            case 7:
                drawable = mContext.getDrawable(R.drawable.ic_error);
                break;
            case 8:
                drawable = mContext.getDrawable(R.drawable.ic_favorite_note);
                break;
        }
        mAmbientIconOverride = drawable;
        updatePill();
    }

    public void updateBottomSpacing() {
        boolean z;
        if (!mInflated) {
            return;
        }
        int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.ambient_indication_margin_bottom);
        if (mBottomMarginPx != dimensionPixelSize) {
            mBottomMarginPx = dimensionPixelSize;
            ((FrameLayout.LayoutParams) getLayoutParams()).bottomMargin = mBottomMarginPx;
        }
        int i = 0;
        if (mTextView.getVisibility() == 0) {
            z = true;
        } else {
            z = false;
        }
        ShadeViewController shadeViewController = mCentralSurfaces.getNotificationPanelViewController();
        int top = getTop();
        NotificationPanelViewController notificationPanelViewController = (NotificationPanelViewController) shadeViewController;
        if (z) {
            i = notificationPanelViewController.getScrollerLayoutController().getNotificationStackScrollLayoutView().getBottom() - top;
        }
        if (notificationPanelViewController.getAmbientIndicationBottomPadding() != i) {
            notificationPanelViewController.setAmbientIndicationBottomPadding(i);
            notificationPanelViewController.updateMaxDisplayedNotificationsWrapper(true);
        }
    }

    private void updateColors() {
        if (mTextColorAnimator != null && mTextColorAnimator.isRunning()) {
            mTextColorAnimator.cancel();
        }
        int defaultColor = mTextView.getTextColors().getDefaultColor();
        int i = mDozing ? -1 : mTextColor;
        if (defaultColor == i) {
            mTextView.setTextColor(i);
            mIconView.setImageTintList(ColorStateList.valueOf(i));
            return;
        }
        mTextColorAnimator = ValueAnimator.ofArgb(defaultColor, i);
        mTextColorAnimator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        mTextColorAnimator.setDuration(500L);
        mTextColorAnimator.addUpdateListener(valueAnimator -> {
            int intValue = (Integer) valueAnimator.getAnimatedValue();
            mTextView.setTextColor(intValue);
            mIconView.setImageTintList(ColorStateList.valueOf(intValue));
        });
        mTextColorAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                mTextColorAnimator = null;
            }
        });
        mTextColorAnimator.start();
    }

    public void setWirelessChargingMessage(CharSequence charSequence) {
        if (!Objects.equals(mWirelessChargingMessage, charSequence) || mReverseChargingMessage != null) {
            mWirelessChargingMessage = charSequence;
            mReverseChargingMessage = null;
            updatePill();
        }
    }

    public void setReverseChargingMessage(CharSequence charSequence) {
        if (!Objects.equals(mReverseChargingMessage, charSequence) || mWirelessChargingMessage != null) {
            mWirelessChargingMessage = null;
            mReverseChargingMessage = charSequence;
            updatePill();
        }
    }

    public void updatePill() {
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        CharSequence charSequence;
        Drawable drawable;
        Drawable drawable2;
        boolean z5;
        int i;
        int i2;
        int i3;
        if (mTextView == null || mIconView == null) {
            return;
        }
        TextView textView2 = mTextView;
        int i4 = mIndicationTextMode;
        mIndicationTextMode = 1;
        CharSequence charSequence2 = mAmbientMusicText;
        if (textView2.getVisibility() == 0) {
            z = true;
        } else {
            z = false;
        }
        CharSequence charSequence3 = mAmbientMusicText;
        if (charSequence3 != null && charSequence3.length() == 0) {
            z2 = true;
        } else {
            z2 = false;
        }
        TextView textView3 = mTextView;
        if (mOpenIntent != null) {
            z3 = true;
        } else {
            z3 = false;
        }
        textView3.setClickable(z3);
        ImageView imageView = mIconView;
        if (mFavoritingIntent == null && mOpenIntent == null) {
            z4 = false;
        } else {
            z4 = true;
        }
        imageView.setClickable(z4);
        if (TextUtils.isEmpty(mIconDescription)) {
            charSequence = charSequence2;
        } else {
            charSequence = mIconDescription;
        }
        Drawable drawable3 = null;
        if (!TextUtils.isEmpty(mReverseChargingMessage)) {
            mIndicationTextMode = 2;
            charSequence2 = mReverseChargingMessage;
            if (mReverseChargingAnimation == null) {
                mReverseChargingAnimation = mContext.getDrawable(R.anim.reverse_charging_animation);
            }
            Drawable drawable4 = mReverseChargingAnimation;
            mTextView.setClickable(false);
            mIconView.setClickable(false);
            charSequence = null;
            drawable3 = drawable4;
            z2 = false;
        } else if (!TextUtils.isEmpty(mWirelessChargingMessage)) {
            mIndicationTextMode = 3;
            charSequence2 = mWirelessChargingMessage;
            mTextView.setClickable(false);
            mIconView.setClickable(false);
            z2 = false;
            charSequence = null;
        } else if ((!TextUtils.isEmpty(charSequence2) || z2) && (drawable3 = mAmbientIconOverride) == null) {
            if (z) {
                if (mAmbientMusicNoteIcon == null) {
                    mAmbientMusicNoteIcon = mContext.getDrawable(R.drawable.ic_music_note);
                }
                drawable = mAmbientMusicNoteIcon;
            } else {
                if (mAmbientMusicAnimation == null) {
                    mAmbientMusicAnimation = mContext.getDrawable(R.anim.audioanim_animation);
                }
                drawable = mAmbientMusicAnimation;
            }
            drawable3 = drawable;
        }
        mTextView.setText(charSequence2);
        mTextView.setContentDescription(charSequence2);
        mIconView.setContentDescription(charSequence);
        if (drawable3 != null) {
            mIconBounds.set(0, 0, drawable3.getIntrinsicWidth(), drawable3.getIntrinsicHeight());
            Rect rect = mIconBounds;
            if (drawable3 == mAmbientMusicNoteIcon) {
                i2 = mAmbientMusicNoteIconIconSize;
            } else {
                i2 = mAmbientIndicationIconSize;
            }
            MathUtils.fitRect(rect, i2);
            drawable2 = new DrawableWrapper(drawable3) { 
                @Override
                public final int getIntrinsicHeight() {
                    return mIconBounds.height();
                }

                @Override
                public final int getIntrinsicWidth() {
                    return mIconBounds.width();
                }
            };
            if (!TextUtils.isEmpty(charSequence2)) {
                i3 = (int) (getResources().getDisplayMetrics().density * 24.0f);
            } else {
                i3 = 0;
            }
            TextView textView4 = mTextView;
            textView4.setPaddingRelative(textView4.getPaddingStart(), mTextView.getPaddingTop(), i3, mTextView.getPaddingBottom());
        } else {
            TextView textView5 = mTextView;
            textView5.setPaddingRelative(textView5.getPaddingStart(), mTextView.getPaddingTop(), 0, mTextView.getPaddingBottom());
            drawable2 = drawable3;
        }
        mIconView.setImageDrawable(drawable2);
        if (TextUtils.isEmpty(charSequence2) && !z2) {
            z5 = false;
        } else {
            z5 = true;
        }
        if (z5) {
            i = 0;
        } else {
            i = 8;
        }
        mTextView.setVisibility(i);
        if (drawable3 == null) {
            mIconView.setVisibility(8);
        } else {
            mIconView.setVisibility(i);
        }
        if (z5) {
            if (!z) {
                mWakeLock.acquire("AmbientIndication");
                if (drawable3 != null && (drawable3 instanceof AnimatedVectorDrawable)) {
                    ((AnimatedVectorDrawable) drawable3).start();
                }
                mTextView.setTranslationY(mTextView.getHeight() / 2);
                mTextView.setAlpha(0.0f);
                mTextView.animate().alpha(1.0f).translationY(0.0f).setStartDelay(150L).setDuration(100L).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        mWakeLock.release("AmbientIndication");
                        mTextView.animate().setListener(null);
                    }
                }).setInterpolator(Interpolators.DECELERATE_QUINT).start();
            } else if (i4 != mIndicationTextMode) {
                if (drawable3 != null && (drawable3 instanceof AnimatedVectorDrawable)) {
                    mWakeLock.acquire("AmbientIndication");
                    ((AnimatedVectorDrawable) drawable3).start();
                    mWakeLock.release("AmbientIndication");
                }
            } else {
                mHandler.post(mWakeLock.wrap(() -> {}));
            }
        } else {
            mTextView.animate().cancel();
            if (drawable3 != null && (drawable3 instanceof AnimatedVectorDrawable)) {
                ((AnimatedVectorDrawable) drawable3).reset();
            }
            mHandler.post(mWakeLock.wrap(() -> {}));
        }
        updateBottomSpacing();
    }
}
