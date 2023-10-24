package com.google.android.settings.fuelgauge.adaptivecharging;

import android.content.Context;
import android.content.IntentFilter;
import androidx.preference.Preference;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.google.android.systemui.googlebattery.AdaptiveChargingManager;

public class AdaptiveChargingPreferenceController extends BasePreferenceController {
    @VisibleForTesting
    AdaptiveChargingManager mAdaptiveChargingManager;

    @Override 
    public  Class getBackgroundWorkerClass() {
        return super.getBackgroundWorkerClass();
    }

    @Override 
    public  IntentFilter getIntentFilter() {
        return super.getIntentFilter();
    }

    @Override 
    public  int getSliceHighlightMenuRes() {
        return super.getSliceHighlightMenuRes();
    }

    @Override 
    public  boolean hasAsyncUpdate() {
        return super.hasAsyncUpdate();
    }

    @Override 
    public  boolean isPublicSlice() {
        return super.isPublicSlice();
    }

    @Override 
    public  boolean isSliceable() {
        return super.isSliceable();
    }

    @Override 
    public  boolean useDynamicSliceSummary() {
        return super.useDynamicSliceSummary();
    }

    public AdaptiveChargingPreferenceController(Context context, String str) {
        super(context, str);
        mAdaptiveChargingManager = new AdaptiveChargingManager(context.getApplicationContext());
    }

    @Override
    public int getAvailabilityStatus() {
        return mAdaptiveChargingManager.isAvailable() ? 1 : 3;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(mAdaptiveChargingManager.isEnabled() ? R.string.battery_saver_on_summary : R.string.battery_saver_off_summary);
    }
}
