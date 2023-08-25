package com.google.android.settings.overlay;

import android.content.Context;

import com.android.settings.accounts.AccountFeatureProvider;
import com.android.settings.fuelgauge.BatteryStatusFeatureProvider;
import com.android.settings.fuelgauge.BatteryStatusFeatureProviderImpl;
import com.android.settings.fuelgauge.BatterySettingsFeatureProvider;
import com.android.settings.fuelgauge.BatterySettingsFeatureProviderImpl;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl;
import com.android.settings.overlay.FeatureFactoryImpl;

import com.google.android.settings.accounts.AccountFeatureProviderGoogleImpl;
import com.google.android.settings.fuelgauge.BatterySettingsFeatureProviderGoogleImpl;
import com.google.android.settings.fuelgauge.BatteryStatusFeatureProviderGoogleImpl;
import com.google.android.settings.fuelgauge.PowerUsageFeatureProviderGoogleImpl;

import android.os.UserHandle;
import android.provider.Settings;

public final class FeatureFactoryImplGoogle extends FeatureFactoryImpl {

    private AccountFeatureProvider mAccountFeatureProvider;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    private BatteryStatusFeatureProvider mBatteryStatusFeatureProvider;
    private BatterySettingsFeatureProvider mBatterySettingsFeatureProvider;

    public boolean isChartGraphEnabled (Context context) {
        int getChartGraphSettings = Settings.System.getIntForUser(context.getContentResolver(),
        "battery_24_hrs_stats", 0, UserHandle.USER_CURRENT);

        return getChartGraphSettings != 0;
    }

    @Override
    public AccountFeatureProvider getAccountFeatureProvider() {
        if (mAccountFeatureProvider == null) {
            mAccountFeatureProvider = new AccountFeatureProviderGoogleImpl();
        }
        return mAccountFeatureProvider;
    }

    @Override
    public BatterySettingsFeatureProvider getBatterySettingsFeatureProvider(Context context) {
        if (mBatterySettingsFeatureProvider == null) {
            mBatterySettingsFeatureProvider = isChartGraphEnabled(context) ? new BatterySettingsFeatureProviderImpl(context) : new BatterySettingsFeatureProviderGoogleImpl(
                    context);
        }
        return mBatterySettingsFeatureProvider;
    }

    @Override
    public BatteryStatusFeatureProvider getBatteryStatusFeatureProvider(Context context) {
        if (mBatteryStatusFeatureProvider == null) {
           mBatteryStatusFeatureProvider = isChartGraphEnabled(context) ? new BatteryStatusFeatureProviderImpl(
                context) : new BatteryStatusFeatureProviderGoogleImpl(context);
        }
        return mBatteryStatusFeatureProvider;
    }

    @Override
    public PowerUsageFeatureProvider getPowerUsageFeatureProvider(Context context) {
        if (mPowerUsageFeatureProvider == null) {
                mPowerUsageFeatureProvider = isChartGraphEnabled(context) ? new PowerUsageFeatureProviderImpl(
                        context) : new PowerUsageFeatureProviderGoogleImpl(
                            context);
        }
        return mPowerUsageFeatureProvider;
    }

}
