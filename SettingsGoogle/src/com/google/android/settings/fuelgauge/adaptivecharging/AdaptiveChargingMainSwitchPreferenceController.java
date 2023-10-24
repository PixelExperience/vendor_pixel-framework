package com.google.android.settings.fuelgauge.adaptivecharging;

import android.content.Context;
import android.content.IntentFilter;
import android.os.UserHandle;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.SettingsMainSwitchPreferenceController;
import com.google.android.systemui.googlebattery.AdaptiveChargingManager;

public class AdaptiveChargingMainSwitchPreferenceController extends SettingsMainSwitchPreferenceController {
    @VisibleForTesting
    AdaptiveChargingManager mAdaptiveChargingManager;
    private boolean mChecked;

    @Override 
    public Class getBackgroundWorkerClass() {
        return super.getBackgroundWorkerClass();
    }

    @Override 
    public IntentFilter getIntentFilter() {
        return super.getIntentFilter();
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_battery;
    }

    @Override 
    public boolean hasAsyncUpdate() {
        return super.hasAsyncUpdate();
    }

    @Override 
    public boolean useDynamicSliceSummary() {
        return super.useDynamicSliceSummary();
    }

    public AdaptiveChargingMainSwitchPreferenceController(Context context, String str) {
        super(context, str);
        mAdaptiveChargingManager = new AdaptiveChargingManager(context.getApplicationContext());
    }

    @Override
    public int getAvailabilityStatus() {
        if (UserHandle.myUserId() == 0) {
            return mAdaptiveChargingManager.isAvailable() ? 0 : 3;
        }
        return 4;
    }

    @Override 
    public boolean isChecked() {
        return mAdaptiveChargingManager.isEnabled();
    }

    @Override 
    public boolean setChecked(boolean z) {
        mAdaptiveChargingManager.setEnabled(z);
        if (!z) {
            mAdaptiveChargingManager.setAdaptiveChargingDeadline(-1);
        }
        if (mChecked != z) {
            mChecked = z;
            FeatureFactory.getFactory(mContext).getMetricsFeatureProvider().action(mContext, 1781, z);
            return true;
        }
        return true;
    }
}
