package com.google.android.settings.fuelgauge;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import com.android.settings.R;
import com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl;
import com.android.settingslib.fuelgauge.Estimate;
import com.android.settingslib.utils.PowerUtil;
import com.google.android.settings.experiments.PhenotypeProxy;
import com.google.android.systemui.googlebattery.AdaptiveChargingManager;
import com.google.android.systemui.googlebattery.GoogleBatteryManager;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import vendor.google.google_battery.IGoogleBattery;

/* loaded from: classes2.dex */
public class PowerUsageFeatureProviderGoogleImpl extends PowerUsageFeatureProviderImpl {
    static final String ACTION_RESUME_CHARGING = "PNW.defenderResumeCharging.settings";
    static final String AVERAGE_BATTERY_LIFE_COL = "average_battery_life";
    static final String BATTERY_ESTIMATE_BASED_ON_USAGE_COL = "is_based_on_usage";
    static final String BATTERY_ESTIMATE_COL = "battery_estimate";
    static final String BATTERY_LEVEL_COL = "battery_level";
    static final int CUSTOMIZED_TO_USER = 1;
    static final String EXTRA_IS_DOCK_DEFENDER = "is_dock_defender";
    private static final String[] PACKAGES_SERVICE = {"com.google.android.gms", "com.google.android.apps.gcs"};
    static final String PACKAGE_NAME_SYSTEMUI = "com.android.systemui";
    static final String SETTINGS_GLOBAL_BATTERY_MANAGER_DISABLED = "settingsgoogle:battery_manager_disabled";
    static final String TIMESTAMP_COL = "timestamp_millis";
    AdaptiveChargingManager mAdaptiveChargingManager;
    private boolean mBatteryUsageEnabled;
    private double mBatteryUsageListConsumePowerThreshold;
    private double mBatteryUsageListScreenOnTimeThresholdInMs;
    private Set<String> mHideApplicationSet;
    private Set<String> mHideBackgroundUsageTimeSet;
    private Set<Integer> mHideSystemComponentSet;
    private Set<String> mIgnoreScreenOnTimeTaskRootSet;
    private Set<String> mOthersCustomComponentNameSet;
    private Set<Integer> mOthersSystemComponentSet;
    boolean mSettingsIntelligenceConfigurationLoaded;
    private List<String> mSystemAppsAllowlist;

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public boolean delayHourlyJobWhenBooting() {
        return false;
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public String getFullChargeIntentAction() {
        return "android.intent.action.ACTION_POWER_DISCONNECTED";
    }

    public PowerUsageFeatureProviderGoogleImpl(Context context) {
        super(context);
        mSettingsIntelligenceConfigurationLoaded = false;
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public boolean isBatteryUsageEnabled() {
        loadSettingsIntelligenceConfiguration();
        return mBatteryUsageEnabled;
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public double getBatteryUsageListScreenOnTimeThresholdInMs() {
        loadSettingsIntelligenceConfiguration();
        return mBatteryUsageListScreenOnTimeThresholdInMs;
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public double getBatteryUsageListConsumePowerThreshold() {
        loadSettingsIntelligenceConfiguration();
        return mBatteryUsageListConsumePowerThreshold;
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public List<String> getSystemAppsAllowlist() {
        loadSettingsIntelligenceConfiguration();
        return mSystemAppsAllowlist;
    }

    void setPackageManager(PackageManager packageManager) {
        mPackageManager = packageManager;
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public Estimate getEnhancedBatteryPrediction(Context context) {
        long j;
        Cursor query = context.getContentResolver().query(getEnhancedBatteryPredictionUri(), null, null, null, null);
        if (query != null) {
            try {
                if (query.moveToFirst()) {
                    int columnIndex = query.getColumnIndex(BATTERY_ESTIMATE_BASED_ON_USAGE_COL);
                    boolean z = true;
                    if (columnIndex != -1 && 1 != query.getInt(columnIndex)) {
                        z = false;
                    }
                    boolean z2 = z;
                    int columnIndex2 = query.getColumnIndex(AVERAGE_BATTERY_LIFE_COL);
                    if (columnIndex2 != -1) {
                        long j2 = query.getLong(columnIndex2);
                        if (j2 != -1) {
                            long millis = Duration.ofMinutes(15L).toMillis();
                            if (Duration.ofMillis(j2).compareTo(Duration.ofDays(1L)) >= 0) {
                                millis = Duration.ofHours(1L).toMillis();
                            }
                            j = PowerUtil.roundTimeToNearestThreshold(j2, millis);
                            Estimate estimate = new Estimate(query.getLong(query.getColumnIndex(BATTERY_ESTIMATE_COL)), z2, j);
                            query.close();
                            return estimate;
                        }
                    }
                    j = -1;
                    Estimate estimate2 = new Estimate(query.getLong(query.getColumnIndex(BATTERY_ESTIMATE_COL)), z2, j);
                    query.close();
                    return estimate2;
                }
            } catch (Throwable th) {
                try {
                    query.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
        if (query != null) {
            query.close();
            return null;
        }
        return null;
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public boolean isEnhancedBatteryPredictionEnabled(Context context) {
        if (isTurboEnabled(context)) {
            try {
                return mPackageManager.getPackageInfo("com.google.android.apps.turbo", 512).applicationInfo.enabled;
            } catch (PackageManager.NameNotFoundException unused) {
                return false;
            }
        }
        return false;
    }

    private Uri getEnhancedBatteryPredictionUri() {
        return new Uri.Builder().scheme("content").authority("com.google.android.apps.turbo.estimated_time_remaining").appendPath("time_remaining").build();
    }

    boolean isTurboEnabled(Context context) {
        return PhenotypeProxy.getBooleanFlagByPackageAndKey(context, "com.google.android.apps.turbo", "NudgesBatteryEstimates__estimated_time_remaining_provider_enabled", false);
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public Intent getResumeChargeIntent(boolean z) {
        return new Intent(ACTION_RESUME_CHARGING).setPackage(PACKAGE_NAME_SYSTEMUI).addFlags(1342177280).putExtra(EXTRA_IS_DOCK_DEFENDER, z);
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public synchronized Set<Integer> getOthersSystemComponentSet() {
        if (mOthersSystemComponentSet == null) {
            mOthersSystemComponentSet = new ArraySet();
            for (int i : mContext.getResources().getIntArray(R.array.allowlist_others_system_compenents_in_battery_usage)) {
                mOthersSystemComponentSet.add(Integer.valueOf(i));
            }
        }
        return mOthersSystemComponentSet;
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public synchronized Set<String> getOthersCustomComponentNameSet() {
        if (mOthersCustomComponentNameSet == null) {
            mOthersCustomComponentNameSet = new ArraySet();
            for (CharSequence charSequence : mContext.getResources().getTextArray(R.array.allowlist_others_custom_compenent_names_in_battery_usage)) {
                mOthersCustomComponentNameSet.add(charSequence.toString());
            }
        }
        return mOthersCustomComponentNameSet;
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public synchronized Set<Integer> getHideSystemComponentSet() {
        if (mHideSystemComponentSet == null) {
            mHideSystemComponentSet = new ArraySet();
            for (int i : mContext.getResources().getIntArray(R.array.allowlist_hide_system_compenents_in_battery_usage)) {
                mHideSystemComponentSet.add(Integer.valueOf(i));
            }
        }
        return mHideSystemComponentSet;
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public synchronized Set<String> getHideApplicationSet() {
        if (mHideApplicationSet == null) {
            mHideApplicationSet = new ArraySet();
            for (CharSequence charSequence : mContext.getResources().getTextArray(R.array.allowlist_hide_entry_in_battery_usage)) {
                mHideApplicationSet.add(charSequence.toString());
            }
        }
        return mHideApplicationSet;
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public synchronized Set<String> getHideBackgroundUsageTimeSet() {
        if (mHideBackgroundUsageTimeSet == null) {
            mHideBackgroundUsageTimeSet = new ArraySet();
            for (CharSequence charSequence : mContext.getResources().getTextArray(R.array.allowlist_hide_background_in_battery_usage)) {
                mHideBackgroundUsageTimeSet.add(charSequence.toString());
            }
        }
        return mHideBackgroundUsageTimeSet;
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public synchronized Set<String> getIgnoreScreenOnTimeTaskRootSet() {
        if (mIgnoreScreenOnTimeTaskRootSet == null) {
            mIgnoreScreenOnTimeTaskRootSet = new ArraySet();
            for (CharSequence charSequence : mContext.getResources().getTextArray(R.array.allowlist_ignore_screen_on_time_in_battery_usage)) {
                mIgnoreScreenOnTimeTaskRootSet.add(charSequence.toString());
            }
        }
        return mIgnoreScreenOnTimeTaskRootSet;
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public boolean isExtraDefend() {
        Pair<IGoogleBattery, IBinder.DeathRecipient> initHalInterface = initHalInterface();
        Object obj = initHalInterface.first;
        try {
            if (obj == null) {
                Log.e("PowerUsageFeatureProviderGoogleImpl", "Settings cannot init hal interface");
                return false;
            }
            int dockDefendStatus = ((IGoogleBattery) obj).getDockDefendStatus();
            Log.d("PowerUsageFeatureProviderGoogleImpl", "get dock defend status success: " + dockDefendStatus);
            return dockDefendStatus == 1;
        } catch (Exception e) {
            Log.e("PowerUsageFeatureProviderGoogleImpl", "get dock defend status faield. ", e);
            return false;
        } finally {
            destroyHalInterface(initHalInterface);
        }
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public String getBuildMetadata1(Context context) {
        return String.valueOf(true);
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public String getBuildMetadata2(Context context) {
        return String.valueOf(false);
    }

    @Override // com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl, com.android.settings.fuelgauge.PowerUsageFeatureProvider
    public boolean isValidToRestoreOptimizationMode(ArrayMap<String, String> arrayMap) {
        if (arrayMap == null || arrayMap.isEmpty()) {
            return false;
        }
        return toBoolean(arrayMap.get("device_build_metadata_1")) || toBoolean(arrayMap.get("device_build_metadata_2"));
    }

    private void loadSettingsIntelligenceConfiguration() {
        if (mSettingsIntelligenceConfigurationLoaded) {
            return;
        }
        mBatteryUsageEnabled = !PhenotypeProxy.getBooleanFlagByPackageAndKey(mContext, mContext.getString(R.string.config_settingsintelligence_package_name), "BatteryUsage__is_battery_usage_disabled", false);
        mBatteryUsageListScreenOnTimeThresholdInMs = PhenotypeProxy.getDoubleFlagByPackageAndKey(mContext, mContext.getString(R.string.config_settingsintelligence_package_name), "BatteryUsage__battery_usage_list_screen_on_time_threshold_in_ms", 100.0d);
        mBatteryUsageListConsumePowerThreshold = PhenotypeProxy.getDoubleFlagByPackageAndKey(mContext, mContext.getString(R.string.config_settingsintelligence_package_name), "BatteryUsage__battery_usage_list_consume_power_threshold", 1.0d);
        mSystemAppsAllowlist = PhenotypeProxy.getStringListFlagByPackageAndKey(mContext, mContext.getString(R.string.config_settingsintelligence_package_name), "BatteryUsage__allowlist_system_apps", List.of("com.google.android.gms"));
        mSettingsIntelligenceConfigurationLoaded = true;
    }

    private static boolean toBoolean(String str) {
        if (str != null && !str.isEmpty()) {
            try {
                return Boolean.parseBoolean(str);
            } catch (Exception unused) {
            }
        }
        return false;
    }

    private static Pair<IGoogleBattery, IBinder.DeathRecipient> initHalInterface() {
        IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() { // from class: com.google.android.settings.fuelgauge.PowerUsageFeatureProviderGoogleImpl$$ExternalSyntheticLambda0
            @Override // android.os.IBinder.DeathRecipient
            public final void binderDied() {}
        };
        return new Pair<>(GoogleBatteryManager.initHalInterface(deathRecipient), deathRecipient);
    }

    private static void destroyHalInterface(Pair<IGoogleBattery, IBinder.DeathRecipient> pair) {
        try {
            GoogleBatteryManager.destroyHalInterface((IGoogleBattery) pair.first, (IBinder.DeathRecipient) pair.second);
        } catch (Exception unused) {
            Log.e("PowerUsageFeatureProviderGoogleImpl", "Settings cannot destroy hal interface");
        }
    }
}
