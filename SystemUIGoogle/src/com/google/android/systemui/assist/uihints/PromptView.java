package com.google.android.systemui.assist.uihints;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import com.android.wm.shell.R;

public class PromptView extends TextView {
    private final DecelerateInterpolator mDecelerateInterpolator;
    private boolean mEnabled;
    private String mHandleString;
    private boolean mHasDarkBackground;
    private final Configuration mLastConfig;
    private int mLastInvocationType;
    private final float mRiseDistance;
    private String mSqueezeString;
    private final int mTextColorDark;
    private final int mTextColorLight;

    public PromptView(Context context) {
        this(context, null);
    }

    @Override
    public final void onConfigurationChanged(Configuration configuration) {
        boolean z;
        super.onConfigurationChanged(configuration);
        mHandleString = getResources().getString(R.string.handle_invocation_prompt);
        mSqueezeString = getResources().getString(R.string.squeeze_invocation_prompt);
        int updateFrom = mLastConfig.updateFrom(configuration);
        boolean z2 = true;
        if ((updateFrom & 4096) != 0) {
            z = true;
        } else {
            z = false;
        }
        if ((updateFrom & 1073741824) == 0) {
            z2 = false;
        }
        if (z || z2) {
            setTextSize(0, getContext().getResources().getDimension(R.dimen.transcription_text_size));
            updateViewHeight();
        }
    }

    @Override
    public final void onFinishInflate() {
        updateViewHeight();
    }

    public final void updateViewHeight() {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            layoutParams.height = (int) (getContext().getResources().getDimension(R.dimen.transcription_text_size) + getResources().getDimension(R.dimen.assist_prompt_start_height) + mRiseDistance);
        }
        requestLayout();
    }

    private void setAlphaProgress(int i, float f) {
        setAlpha((i == 2 || f <= 0.8f) ? f > 0.32000002f ? 1.0f : mDecelerateInterpolator.getInterpolation(f / 0.32000002f) : 0.0f);
    }

    private void setTranslationYProgress(float f) {
        setTranslationY((-mRiseDistance) * f);
    }

    public void disable() {
        mEnabled = false;
        setVisibility(8);
    }

    public void enable() {
        mEnabled = true;
    }

    public void updateDimens() {
        setTextSize(0, getContext().getResources().getDimension(R.dimen.transcription_text_size));
        updateViewHeight();
    }

    public void onInvocationProgress(int i, float f) {
        String str;
        if (f > 1.0f) {
            return;
        }
        if (f == 0.0f) {
            setVisibility(8);
            setAlpha(0.0f);
            setTranslationY(0.0f);
            mLastInvocationType = 0;
        } else if (mEnabled) {
            if (i != 1) {
                if (i != 2) {
                    mLastInvocationType = 0;
                    setText("");
                } else if (mLastInvocationType != i) {
                    mLastInvocationType = i;
                    setText(mSqueezeString);
                    str = mSqueezeString;
                    announceForAccessibility(str);
                }
            } else if (mLastInvocationType != i) {
                mLastInvocationType = i;
                setText(mHandleString);
                str = mHandleString;
                announceForAccessibility(str);
            }
            setVisibility(0);
            setTranslationYProgress(f);
            setAlphaProgress(i, f);
        }
    }

    public PromptView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public PromptView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public PromptView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        mDecelerateInterpolator = new DecelerateInterpolator(2.0f);
        mLastConfig = new Configuration();
        mHasDarkBackground = false;
        mEnabled = false;
        mLastInvocationType = 0;
        int color = getContext().getColor(R.color.transcription_text_dark);
        mTextColorDark = color;
        int color2 = getContext().getColor(R.color.transcription_text_light);
        mTextColorLight = color2;
        mRiseDistance = getResources().getDimension(R.dimen.assist_prompt_rise_distance);
        mHandleString = getResources().getString(R.string.handle_invocation_prompt);
        mSqueezeString = getResources().getString(R.string.squeeze_invocation_prompt);
        boolean z = mHasDarkBackground;
        boolean z2 = !z;
        if (z2 != z) {
            setTextColor(z2 ? color : color2);
            mHasDarkBackground = z2;
        }
    }
    
    public void setHasDarkBackground(boolean z) {
        if (z != mHasDarkBackground) {
            setTextColor(z ? mTextColorDark : mTextColorLight);
            mHasDarkBackground = z;
        }
    }
}

