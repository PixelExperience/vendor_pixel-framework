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

package com.google.android.systemui.ambientmusic;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.media.MediaMetadata;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.animation.Interpolators;
import com.android.systemui.doze.DozeReceiver;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.phone.CentralSurfaces;
import com.android.systemui.util.wakelock.DelayedWakeLock;
import com.android.systemui.util.wakelock.WakeLock;

import java.util.Objects;

public class AmbientIndicationContainer extends AutoReinflateContainer implements DozeReceiver, StatusBarStateController.StateListener, NotificationMediaManager.MediaListener {
    private final Handler mHandler;
    private final Rect mIconBounds;
    private final WakeLock mWakeLock;
    private Drawable mAmbientIconOverride;
    private int mAmbientIndicationIconSize;
    private Drawable mAmbientMusicAnimation;
    private Drawable mAmbientMusicNoteIcon;
    private int mAmbientMusicNoteIconIconSize;
    private CharSequence mAmbientMusicText;
    private boolean mAmbientSkipUnlock;
    private int mBottomMarginPx;
    private boolean mDozing;
    private PendingIntent mFavoritingIntent;
    private String mIconDescription;
    private int mIconOverride;
    private ImageView mIconView;
    private int mIndicationTextMode;
    private int mMediaPlaybackState;
    private PendingIntent mOpenIntent;
    private Drawable mReverseChargingAnimation;
    private CharSequence mReverseChargingMessage;
    private CentralSurfaces mCentralSurfaces;
    private int mCentralSurfacesState;
    private int mTextColor;
    private ValueAnimator mTextColorAnimator;
    private TextView mTextView;
    private CharSequence mWirelessChargingMessage;

    public AmbientIndicationContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mIconBounds = new Rect();
        mIconOverride = -1;
        Handler handler = new Handler(Looper.getMainLooper());
        mHandler = handler;
        mWakeLock = createWakeLock(mContext, handler);
    }

    @VisibleForTesting
    WakeLock createWakeLock(Context context, Handler handler) {
        return new DelayedWakeLock(handler, WakeLock.createPartial(context, "AmbientIndication"));
    }

    public void initializeView(CentralSurfaces centralSurfaces) {
        mCentralSurfaces = centralSurfaces;
        addInflateListener(new AutoReinflateContainer.InflateListener() {
            @Override
            public void onInflated(View view) {
                mTextView = (TextView) findViewById(R.id.ambient_indication_text);
                mIconView = (ImageView) findViewById(R.id.ambient_indication_icon);
                mAmbientMusicAnimation = mContext.getDrawable(R.anim.audioanim_animation);
                mAmbientMusicNoteIcon = mContext.getDrawable(R.drawable.ic_music_note);
                mReverseChargingAnimation = mContext.getDrawable(R.anim.reverse_charging_animation);
                mTextColor = mTextView.getCurrentTextColor();
                mAmbientIndicationIconSize = getResources().getDimensionPixelSize(R.dimen.ambient_indication_icon_size);
                mAmbientMusicNoteIconIconSize = getResources().getDimensionPixelSize(R.dimen.ambient_indication_note_icon_size);
                mTextView.setEnabled(!mDozing);
                updateColors();
                updatePill();
                mTextView.setOnClickListener((v) -> onTextClick(v));
                mIconView.setOnClickListener((v) -> onIconClick(v));
            }
        });
        addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    updateBottomSpacing();
                });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this);
        ((NotificationMediaManager) Dependency.get(NotificationMediaManager.class)).addCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).removeCallback(this);
        ((NotificationMediaManager) Dependency.get(NotificationMediaManager.class)).removeCallback(this);
        mMediaPlaybackState = 0;
    }

    public void setAmbientMusic(CharSequence charSequence, PendingIntent pendingIntent, PendingIntent pendingIntent2, int i, boolean z, String str) {
        if (!Objects.equals(mAmbientMusicText, charSequence) || !Objects.equals(mOpenIntent, pendingIntent) || !Objects.equals(mFavoritingIntent, pendingIntent2) || mIconOverride != i || !Objects.equals(mIconDescription, str) || mAmbientSkipUnlock != z) {
            mAmbientMusicText = charSequence;
            mOpenIntent = pendingIntent;
            mFavoritingIntent = pendingIntent2;
            mAmbientSkipUnlock = z;
            mIconOverride = i;
            mIconDescription = str;
            mAmbientIconOverride = getAmbientIconOverride(i);
            updatePill();
        }
    }

    private Drawable getAmbientIconOverride(int i) {
        switch (i) {
            case 1:
                return mContext.getDrawable(R.drawable.ic_music_search);
            case 2:
            default:
                return null;
            case 3:
                return mContext.getDrawable(R.drawable.ic_music_not_found);
            case 4:
                return mContext.getDrawable(R.drawable.ic_cloud_off);
            case 5:
                return mContext.getDrawable(R.drawable.ic_favorite);
            case 6:
                return mContext.getDrawable(R.drawable.ic_favorite_border);
            case 7:
                return mContext.getDrawable(R.drawable.ic_error);
            case 8:
                return mContext.getDrawable(R.drawable.ic_favorite_note);
        }
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

    private void updatePill() {
        int indicationTextMode = mIndicationTextMode;
        boolean updatePill = true;
        mIndicationTextMode = 1;
        CharSequence text = mAmbientMusicText;
        boolean textVisible = mTextView.getVisibility() == View.VISIBLE;
        Drawable icon = textVisible ? mAmbientMusicNoteIcon : mAmbientMusicAnimation;
        if (mAmbientIconOverride != null) {
            icon = mAmbientIconOverride;
        }
        boolean showAmbientMusicText = mAmbientMusicText != null && mAmbientMusicText.length() == 0;
        mTextView.setClickable(mOpenIntent != null);
        mIconView.setClickable(mFavoritingIntent != null || mOpenIntent != null);
        CharSequence iconDescription = TextUtils.isEmpty(mIconDescription) ? text : mIconDescription;
        if (!TextUtils.isEmpty(mReverseChargingMessage)) {
            mIndicationTextMode = 2;
            text = mReverseChargingMessage;
            icon = mReverseChargingAnimation;
            mTextView.setClickable(false);
            mIconView.setClickable(false);
            showAmbientMusicText = false;
            iconDescription = null;
        } else if (!TextUtils.isEmpty(mWirelessChargingMessage)) {
            mIndicationTextMode = 3;
            text = mWirelessChargingMessage;
            mTextView.setClickable(false);
            mIconView.setClickable(false);
            showAmbientMusicText = false;
            icon = null;
            iconDescription = null;
        }
        mTextView.setText(text);
        mTextView.setContentDescription(text);
        mIconView.setContentDescription(iconDescription);
        Drawable drawableWrapper = null;
        if (icon != null) {
            mIconBounds.set(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
            MathUtils.fitRect(mIconBounds, icon == mAmbientMusicNoteIcon ? mAmbientMusicNoteIconIconSize : mAmbientIndicationIconSize);
            drawableWrapper = new DrawableWrapper(icon) {
                @Override
                public int getIntrinsicWidth() {
                    return mIconBounds.width();
                }

                @Override
                public int getIntrinsicHeight() {
                    return mIconBounds.height();
                }
            };
            int i3 = !TextUtils.isEmpty(text) ? (int) (getResources().getDisplayMetrics().density * 24.0f) : 0;
            mTextView.setPaddingRelative(mTextView.getPaddingStart(), mTextView.getPaddingTop(), i3, mTextView.getPaddingBottom());
        } else {
            mTextView.setPaddingRelative(mTextView.getPaddingStart(), mTextView.getPaddingTop(), 0, mTextView.getPaddingBottom());
        }
        mIconView.setImageDrawable(drawableWrapper);
        if ((TextUtils.isEmpty(text) && !showAmbientMusicText)) {
            updatePill = false;
        }
        int vis = View.VISIBLE;
        if (!updatePill) {
            vis = View.GONE;
        }
        mTextView.setVisibility(vis);
        if (icon == null) {
            mIconView.setVisibility(View.GONE);
        } else {
            mIconView.setVisibility(vis);
        }
        if (!updatePill) {
            mTextView.animate().cancel();
            if (icon instanceof AnimatedVectorDrawable) {
                ((AnimatedVectorDrawable) icon).reset();
            }
            mHandler.post(mWakeLock.wrap(() -> {}));
        } else if (!textVisible) {
            mWakeLock.acquire("AmbientIndication");
            if (icon instanceof AnimatedVectorDrawable) {
                ((AnimatedVectorDrawable) icon).start();
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
        } else if (indicationTextMode != mIndicationTextMode) {
            if (icon instanceof AnimatedVectorDrawable) {
                mWakeLock.acquire("AmbientIndication");
                ((AnimatedVectorDrawable) icon).start();
                mWakeLock.release("AmbientIndication");
            }
        } else {
            mHandler.post(mWakeLock.wrap(() -> {}));
        }
        updateBottomSpacing();
    }

    private void updateBottomSpacing() {
        int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.ambient_indication_margin_bottom);
        if (mBottomMarginPx != dimensionPixelSize) {
            mBottomMarginPx = dimensionPixelSize;
            ((FrameLayout.LayoutParams) getLayoutParams()).bottomMargin = mBottomMarginPx;
        }
        mCentralSurfaces.getPanelController().setAmbientIndicationTop(getTop(), mTextView.getVisibility() == View.VISIBLE);
    }

    public void hideAmbientMusic() {
        setAmbientMusic(null, null, null, 0, false, null);
    }

    private void onTextClick(View view) {
        if (mOpenIntent != null) {
            mCentralSurfaces.wakeUpIfDozing(SystemClock.uptimeMillis(), view, "AMBIENT_MUSIC_CLICK");
            if (mAmbientSkipUnlock) {
                sendBroadcastWithoutDismissingKeyguard(mOpenIntent);
            } else {
                mCentralSurfaces.startPendingIntentDismissingKeyguard(mOpenIntent);
            }
        }
    }

    private void onIconClick(View view) {
        if (mFavoritingIntent != null) {
            mCentralSurfaces.wakeUpIfDozing(SystemClock.uptimeMillis(), view, "AMBIENT_MUSIC_CLICK");
            sendBroadcastWithoutDismissingKeyguard(mFavoritingIntent);
            return;
        }
        onTextClick(view);
    }

    @Override
    public void onDozingChanged(boolean z) {
        mDozing = z;
        updateVisibility();
        if (mTextView != null) {
            mTextView.setEnabled(!z);
            updateColors();
        }
    }

    @Override
    public void dozeTimeTick() {
        updatePill();
    }

    private void updateColors() {
        ValueAnimator valueAnimator = mTextColorAnimator;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            mTextColorAnimator.cancel();
        }
        int defaultColor = mTextView.getTextColors().getDefaultColor();
        int i = mDozing ? -1 : mTextColor;
        if (defaultColor == i) {
            mTextView.setTextColor(i);
            mIconView.setImageTintList(ColorStateList.valueOf(i));
            return;
        }
        ValueAnimator ofArgb = ValueAnimator.ofArgb(defaultColor, i);
        mTextColorAnimator = ofArgb;
        ofArgb.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        mTextColorAnimator.setDuration(500L);
        mTextColorAnimator.addUpdateListener(valueAnimator2 -> {
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

    @Override
    public void onStateChanged(int i) {
        mCentralSurfacesState = i;
        updateVisibility();
    }

    private void sendBroadcastWithoutDismissingKeyguard(PendingIntent pendingIntent) {
        if (pendingIntent.isActivity()) {
            return;
        }
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            Log.w("AmbientIndication", "Sending intent failed: " + e);
        }
    }

    private void updateVisibility() {
        if (mCentralSurfacesState == 1) {
            setVisibility(0);
        } else {
            setVisibility(4);
        }
    }

    @Override
    public void onPrimaryMetadataOrStateChanged(MediaMetadata mediaMetadata, int i) {
        if (mMediaPlaybackState != i) {
            mMediaPlaybackState = i;
            if (!isMediaPlaying()) {
                return;
            }
            hideAmbientMusic();
        }
    }

    protected boolean isMediaPlaying() {
        return NotificationMediaManager.isPlayingState(mMediaPlaybackState);
    }
}
