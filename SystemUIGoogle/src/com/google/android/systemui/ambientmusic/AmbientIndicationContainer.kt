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

package com.google.android.systemui.ambientmusic

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.PendingIntent
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.PowerManager
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.MathUtils
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.android.internal.annotations.VisibleForTesting
import com.android.systemui.AutoReinflateContainer
import com.android.systemui.Dependency
import com.android.systemui.R
import com.android.systemui.animation.Interpolators
import com.android.systemui.doze.DozeReceiver
import com.android.systemui.plugins.statusbar.StatusBarStateController
import com.android.systemui.statusbar.NotificationMediaManager
import com.android.systemui.statusbar.phone.CentralSurfaces
import com.android.systemui.util.wakelock.DelayedWakeLock
import com.android.systemui.util.wakelock.WakeLock
import java.util.Objects

class AmbientIndicationContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AutoReinflateContainer(context, attrs), DozeReceiver,
    StatusBarStateController.StateListener,
    NotificationMediaManager.MediaListener {
    private val mHandler: Handler
    private val mIconBounds: Rect
    private val mWakeLock: WakeLock
    private var mAmbientIconOverride: Drawable? = null
    private var mAmbientIndicationIconSize = 0
    private var mAmbientMusicAnimation: Drawable? = null
    private var mAmbientMusicNoteIcon: Drawable? = null
    private var mAmbientMusicNoteIconIconSize = 0
    private var mAmbientMusicText: CharSequence? = null
    private var mAmbientSkipUnlock = false
    private var mBottomMarginPx = 0
    private var mDozing = false
    private var mFavoritingIntent: PendingIntent? = null
    private var mIconDescription: String? = null
    private var mIconOverride = -1
    private var mIconView: ImageView? = null
    private var mIndicationTextMode = 0
    private var mMediaPlaybackState = 0
    private var mOpenIntent: PendingIntent? = null
    private var mReverseChargingAnimation: Drawable? = null
    private var mReverseChargingMessage: CharSequence? = null
    private var mCentralSurfaces: CentralSurfaces? = null
    private var mCentralSurfacesState = 0
    private var mTextColor = 0
    private var mTextColorAnimator: ValueAnimator? = null
    private var mTextView: TextView? = null
    private var mWirelessChargingMessage: CharSequence? = null

    init {
        mIconBounds = Rect()
        mIconOverride = -1
        val handler = Handler(Looper.getMainLooper())
        mHandler = handler
        mWakeLock = createWakeLock(mContext, handler)
    }

    @VisibleForTesting
    fun createWakeLock(context: Context, handler: Handler): WakeLock {
        return DelayedWakeLock(handler, WakeLock.createPartial(context, "AmbientIndication"))
    }

    fun initializeView(centralSurfaces: CentralSurfaces) {
        mCentralSurfaces = centralSurfaces
        addInflateListener(object : AutoReinflateContainer.InflateListener {
            override fun onInflated(view: View) {
                mTextView = findViewById<View>(R.id.ambient_indication_text) as TextView
                mIconView = findViewById<View>(R.id.ambient_indication_icon) as ImageView
                if (mTextView == null || mIconView == null || mContext == null) {
                    return
                }
                mAmbientMusicAnimation = mContext.getDrawable(R.anim.audioanim_animation)
                mAmbientMusicNoteIcon = mContext.getDrawable(R.drawable.ic_music_note)
                mReverseChargingAnimation = mContext.getDrawable(R.anim.reverse_charging_animation)
                mTextColor = mTextView!!.currentTextColor
                mAmbientIndicationIconSize =
                    resources.getDimensionPixelSize(R.dimen.ambient_indication_icon_size)
                mAmbientMusicNoteIconIconSize =
                    resources.getDimensionPixelSize(R.dimen.ambient_indication_note_icon_size)
                mTextView!!.isEnabled = !mDozing
                updateColors()
                updatePill()
                mTextView!!.setOnClickListener { v: View? -> onTextClick(v) }
                mIconView!!.setOnClickListener { v: View? -> onIconClick(v) }
            }
        })
        addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            updateBottomSpacing()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Dependency.get(StatusBarStateController::class.java).addCallback(this)
        Dependency.get(NotificationMediaManager::class.java).addCallback(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Dependency.get(StatusBarStateController::class.java).removeCallback(this)
        Dependency.get(NotificationMediaManager::class.java).removeCallback(this)
        mMediaPlaybackState = 0
    }

    fun setAmbientMusic(
        charSequence: CharSequence?,
        pendingIntent: PendingIntent?,
        pendingIntent2: PendingIntent?,
        i: Int,
        z: Boolean,
        str: String?
    ) {
        if (!Objects.equals(mAmbientMusicText, charSequence) ||
            !Objects.equals(mOpenIntent, pendingIntent) ||
            !Objects.equals(mFavoritingIntent, pendingIntent2) ||
            mIconOverride != i ||
            !Objects.equals(mIconDescription, str) ||
            mAmbientSkipUnlock != z
        ) {
            mAmbientMusicText = charSequence
            mOpenIntent = pendingIntent
            mFavoritingIntent = pendingIntent2
            mAmbientSkipUnlock = z
            mIconOverride = i
            mIconDescription = str
            mAmbientIconOverride = getAmbientIconOverride(i)
            updatePill()
        }
    }

    private fun getAmbientIconOverride(i: Int): Drawable? {
        return when (i) {
            1 -> mContext.getDrawable(R.drawable.ic_music_search)
            3 -> mContext.getDrawable(R.drawable.ic_music_not_found)
            4 -> mContext.getDrawable(R.drawable.ic_cloud_off)
            5 -> mContext.getDrawable(R.drawable.ic_favorite)
            6 -> mContext.getDrawable(R.drawable.ic_favorite_border)
            7 -> mContext.getDrawable(R.drawable.ic_error)
            8 -> mContext.getDrawable(R.drawable.ic_favorite_note)
            else -> null
        }
    }

    fun setWirelessChargingMessage(charSequence: CharSequence?) {
        if (!Objects.equals(mWirelessChargingMessage, charSequence) ||
            mReverseChargingMessage != null
        ) {
            mWirelessChargingMessage = charSequence
            mReverseChargingMessage = null
            updatePill()
        }
    }

    fun setReverseChargingMessage(charSequence: CharSequence?) {
        if (!Objects.equals(mReverseChargingMessage, charSequence) ||
            mWirelessChargingMessage != null
        ) {
            mWirelessChargingMessage = null
            mReverseChargingMessage = charSequence
            updatePill()
        }
    }

    private fun updatePill() {
        if (isMediaPlaying()) {
            hideAmbientMusic()
            return
        }
        val indicationTextMode = mIndicationTextMode
        var updatePill = true
        mIndicationTextMode = 1
        var text = mAmbientMusicText
        if (mTextView == null || mIconView == null) {
            return
        }
        var textVisible = mTextView!!.visibility == View.VISIBLE
        var icon = if (textVisible) mAmbientMusicNoteIcon else mAmbientMusicAnimation
        if (mAmbientIconOverride != null) {
            icon = mAmbientIconOverride
        }
        var showAmbientMusicText = mAmbientMusicText != null && mAmbientMusicText!!.length == 0
        mTextView!!.isClickable = mOpenIntent != null
        mIconView!!.isClickable = mFavoritingIntent != null || mOpenIntent != null
        var iconDescription =
            if (TextUtils.isEmpty(mIconDescription)) text else mIconDescription
        if (!TextUtils.isEmpty(mReverseChargingMessage)) {
            mIndicationTextMode = 2
            text = mReverseChargingMessage
            icon = mReverseChargingAnimation
            mTextView!!.isClickable = false
            mIconView!!.isClickable = false
            showAmbientMusicText = false
            iconDescription = null
            updatePill = false
        } else if (!TextUtils.isEmpty(mWirelessChargingMessage)) {
            mIndicationTextMode = 3
            text = mWirelessChargingMessage
            mTextView!!.isClickable = false
            mIconView!!.isClickable = false
            showAmbientMusicText = false
            icon = null
            iconDescription = null
            updatePill = false
        }
        mTextView!!.text = text
        mTextView!!.contentDescription = text
        val drawableWrapper: Drawable?
        if (icon != null) {
            mIconBounds.set(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
            MathUtils.fitRect(
                mIconBounds,
                if (icon === mAmbientMusicNoteIcon) mAmbientMusicNoteIconIconSize else mAmbientIndicationIconSize
            )
            drawableWrapper = object : DrawableWrapper(icon) {
                override fun getIntrinsicWidth(): Int {
                    return mIconBounds.width()
                }

                override fun getIntrinsicHeight(): Int {
                    return mIconBounds.height()
                }
            }
            val i3 =
                if (!TextUtils.isEmpty(text)) (resources.displayMetrics.density * 24.0f).toInt() else 0
            mTextView!!.setPaddingRelative(
                mTextView!!.paddingStart,
                mTextView!!.paddingTop,
                i3,
                mTextView!!.paddingBottom
            )
        } else {
            mTextView!!.setPaddingRelative(
                mTextView!!.paddingStart,
                mTextView!!.paddingTop,
                0,
                mTextView!!.paddingBottom
            )
            drawableWrapper = null
        }
        mIconView!!.setImageDrawable(drawableWrapper)
        if (TextUtils.isEmpty(text) && !showAmbientMusicText) {
            updatePill = false
        }
        val vis: Int
        vis = if (!updatePill) {
            View.GONE
        } else {
            View.VISIBLE
        }
        mTextView!!.visibility = vis
        if (icon == null) {
            mIconView!!.visibility = View.GONE
        } else {
            mIconView!!.visibility = vis
        }
        if (!updatePill) {
            mTextView!!.animate().cancel()
            if (icon is AnimatedVectorDrawable) {
                icon.reset()
            }
            mHandler.post(mWakeLock.wrap { })
        } else if (!textVisible) {
            mWakeLock.acquire("AmbientIndication")
            if (icon is AnimatedVectorDrawable) {
                icon.start()
            }
            mTextView!!.translationY = mTextView!!.height / 2.toFloat()
            mTextView!!.alpha = 0.0f
            mTextView!!.animate().alpha(1.0f).translationY(0.0f).setStartDelay(150L)
                .setDuration(100L)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animator: Animator) {
                        mWakeLock.release("AmbientIndication")
                        mTextView!!.animate().setListener(null)
                    }
                })
                .setInterpolator(Interpolators.DECELERATE_QUINT).start()
        } else if (indicationTextMode != mIndicationTextMode) {
            if (icon is AnimatedVectorDrawable) {
                mWakeLock.acquire("AmbientIndication")
                icon.start()
                mWakeLock.release("AmbientIndication")
            }
        } else {
            mHandler.post(mWakeLock.wrap { })
        }
        updateBottomSpacing()
    }

    private fun updateBottomSpacing() {
        val dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.ambient_indication_margin_bottom)
        if (mBottomMarginPx != dimensionPixelSize) {
            mBottomMarginPx = dimensionPixelSize
            (layoutParams as FrameLayout.LayoutParams).bottomMargin = mBottomMarginPx
        }
        mCentralSurfaces!!.notificationPanelViewController.setAmbientIndicationTop(top, mTextView!!.visibility == View.VISIBLE)
    }

    fun hideAmbientMusic() {
        setAmbientMusic(null, null, null, 0, false, null)
    }

    private fun onTextClick(view: View?) {
        if (mOpenIntent != null) {
            mCentralSurfaces!!.wakeUpIfDozing(
                SystemClock.uptimeMillis(), view, "AMBIENT_MUSIC_CLICK",
                PowerManager.WAKE_REASON_GESTURE
            )
            if (mAmbientSkipUnlock) {
                sendBroadcastWithoutDismissingKeyguard(mOpenIntent!!)
            } else {
                mCentralSurfaces!!.startPendingIntentDismissingKeyguard(mOpenIntent!!)
            }
        }
    }

    private fun onIconClick(view: View?) {
        if (mFavoritingIntent != null) {
            mCentralSurfaces!!.wakeUpIfDozing(
                SystemClock.uptimeMillis(), view, "AMBIENT_MUSIC_CLICK",
                PowerManager.WAKE_REASON_GESTURE
            )
            sendBroadcastWithoutDismissingKeyguard(mFavoritingIntent!!)
            return
        }
        onTextClick(view)
    }

    override fun onDozingChanged(z: Boolean) {
        mDozing = z
        updateVisibility()
        if (mTextView != null) {
            mTextView!!.isEnabled = !z
            updateColors()
        }
    }

    override fun dozeTimeTick() {
        updatePill()
    }

    private fun updateColors() {
        if (mTextColorAnimator != null && mTextColorAnimator!!.isRunning) {
            mTextColorAnimator!!.cancel()
        }
        val defaultColor = mTextView!!.textColors.defaultColor
        val i = if (mDozing) -1 else mTextColor
        if (defaultColor == i) {
            mTextView!!.setTextColor(i)
            mIconView!!.imageTintList = ColorStateList.valueOf(i)
            return
        }
        mTextColorAnimator = ValueAnimator.ofArgb(defaultColor, i)
        mTextColorAnimator!!.interpolator = Interpolators.LINEAR_OUT_SLOW_IN
        mTextColorAnimator!!.duration = 500L
        mTextColorAnimator!!.addUpdateListener { valueAnimator: ValueAnimator ->
            val intValue = valueAnimator.animatedValue as Int
            mTextView!!.setTextColor(intValue)
            mIconView!!.imageTintList = ColorStateList.valueOf(intValue)
        }
        mTextColorAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
                mTextColorAnimator = null
            }
        })
        mTextColorAnimator!!.start()
    }

    override fun onStateChanged(i: Int) {
        mCentralSurfacesState = i
        updateVisibility()
    }

    private fun sendBroadcastWithoutDismissingKeyguard(pendingIntent: PendingIntent) {
        if (pendingIntent.isActivity) {
            return
        }
        try {
            pendingIntent.send()
        } catch (e: PendingIntent.CanceledException) {
            Log.w("AmbientIndication", "Sending intent failed: $e")
        }
    }

    private fun updateVisibility() {
        if (mCentralSurfacesState == 1) {
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }

    override fun onPrimaryMetadataOrStateChanged(mediaMetadata: MediaMetadata?, i: Int) {
        if (mMediaPlaybackState != i) {
            mMediaPlaybackState = i
            updatePill()
        }
    }

    protected fun isMediaPlaying(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        if (sessionManager != null) {
            val controllers: List<MediaController> = sessionManager.getActiveSessions(null)
            for (controller in controllers) {
                val state: PlaybackState? = controller.playbackState
                if (state != null && state.state == PlaybackState.STATE_PLAYING) {
                    return true
                }
            }
        }
        return audioManager?.isMusicActive == true
    }
}
