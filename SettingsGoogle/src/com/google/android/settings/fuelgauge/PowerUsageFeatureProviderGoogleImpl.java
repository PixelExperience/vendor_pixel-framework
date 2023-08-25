package com.google.android.settings.fuelgauge;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import android.util.SparseIntArray;
import com.android.settings.R;
import com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl;
import com.android.settings.fuelgauge.batteryusage.BatteryHistEntry;
import com.android.settingslib.fuelgauge.Estimate;
import com.android.settingslib.utils.PowerUtil;
import com.google.android.settings.experiments.PhenotypeProxy;
import com.google.android.systemui.googlebattery.AdaptiveChargingManager;
import com.google.android.systemui.googlebattery.GoogleBatteryManager;
import java.time.Duration;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import vendor.google.google_battery.IGoogleBattery;

/* loaded from: SettingsGoogle-lib.jar:com/google/android/settings/fuelgauge/PowerUsageFeatureProviderGoogleImpl.class */
public class PowerUsageFeatureProviderGoogleImpl extends PowerUsageFeatureProviderImpl {
    static final String ACTION_RESUME_CHARGING = "PNW.defenderResumeCharging.settings";
    static final String AVERAGE_BATTERY_LIFE_COL = "average_battery_life";
    static final String BATTERY_ESTIMATE_BASED_ON_USAGE_COL = "is_based_on_usage";
    static final String BATTERY_ESTIMATE_COL = "battery_estimate";
    static final String BATTERY_LEVEL_COL = "battery_level";
    static final int CUSTOMIZED_TO_USER = 1;
    static final String EXTRA_IS_DOCK_DEFENDER = "is_dock_defender";
    static final String GFLAG_ADDITIONAL_BATTERY_INFO_ENABLED = "settingsgoogle:additional_battery_info_enabled";
    static final String GFLAG_BATTERY_ADVANCED_UI_ENABLED = "settingsgoogle:battery_advanced_ui_enabled";
    static final String GFLAG_POWER_ACCOUNTING_TOGGLE_ENABLED = "settingsgoogle:power_accounting_toggle_enabled";
    static final String IS_EARLY_WARNING_COL = "is_early_warning";
    static final int NEED_EARLY_WARNING = 1;
    static final String PACKAGE_NAME_SYSTEMUI = "com.android.systemui";
    static final String TIMESTAMP_COL = "timestamp_millis";
    private static boolean sChartGraphEnabled;
    AdaptiveChargingManager mAdaptiveChargingManager;
    private static final String[] PACKAGES_SERVICE = {"com.google.android.gms", "com.google.android.apps.gcs"};
    static boolean sChartConfigurationLoaded = false;

    public PowerUsageFeatureProviderGoogleImpl(Context context) {
        super(context);
    }

    private static void destroyHalInterface(Pair<IGoogleBattery, IBinder.DeathRecipient> pair) {
        try {
            GoogleBatteryManager.destroyHalInterface((IGoogleBattery) pair.first, (IBinder.DeathRecipient) pair.second);
        } catch (Exception e) {
            Log.e("PowerUsageFeatureProviderGoogleImpl", "Settings cannot destroy hal interface");
        }
    }

    private AdaptiveChargingManager getAdaptiveChargingManager() {
        if (this.mAdaptiveChargingManager == null) {
            this.mAdaptiveChargingManager = new AdaptiveChargingManager(mContext);
        }
        return this.mAdaptiveChargingManager;
    }

    private Uri getEnhancedBatteryPredictionCurveUri() {
        return new Uri.Builder().scheme("content").authority("com.google.android.apps.turbo.estimated_time_remaining").appendPath("discharge_curve").build();
    }

    private Uri getEnhancedBatteryPredictionUri() {
        return new Uri.Builder().scheme("content").authority("com.google.android.apps.turbo.estimated_time_remaining").appendPath("time_remaining").build();
    }

    private static Pair<IGoogleBattery, IBinder.DeathRecipient> initHalInterface() {
        IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() { // from class: com.google.android.settings.fuelgauge.PowerUsageFeatureProviderGoogleImpl$$ExternalSyntheticLambda0
            @Override // android.os.IBinder.DeathRecipient
            public final void binderDied() {
                Log.e("PowerUsageFeatureProviderGoogleImpl", "Settings serviceDied");
            }
        };
        return new Pair<>(GoogleBatteryManager.initHalInterface(deathRecipient), deathRecipient);
    }

    private void loadChartConfiguration(Context context) {
        if (sChartConfigurationLoaded) {
            return;
        }
        sChartGraphEnabled = DatabaseUtils.isContentProviderEnabled(context) && Settings.System.getIntForUser(context.getContentResolver(),
                                            "battery_24_hrs_stats", 0, UserHandle.USER_CURRENT) != 0;
        sChartConfigurationLoaded = true;
    }

    public String getAdvancedUsageScreenInfoString() {
        return mContext.getString(R.string.advanced_battery_graph_subtext);
    }

    public Map<Long, Map<String, BatteryHistEntry>> getBatteryHistorySinceLastFullCharge(Context context) {
        return DatabaseUtils.getHistoryMapSinceLastFullCharge(context, Calendar.getInstance());
    }

    public Uri getBatteryHistoryUri() {
        return DatabaseUtils.BATTERY_CONTENT_URI;
    }

    public boolean getEarlyWarningSignal(Context context, String str) {
        Uri.Builder appendPath = new Uri.Builder().scheme("content").authority("com.google.android.apps.turbo.estimated_time_remaining").appendPath("early_warning").appendPath("id");
        if (TextUtils.isEmpty(str)) {
            appendPath.appendPath(context.getPackageName());
        } else {
            appendPath.appendPath(str);
        }
        Cursor query = context.getContentResolver().query(appendPath.build(), null, null, null, null);
        boolean z = false;
        if (query != null) {
            try {
                if (query.moveToFirst()) {
                    if (1 == query.getInt(query.getColumnIndex(IS_EARLY_WARNING_COL))) {
                        z = true;
                    }
                    query.close();
                    return z;
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
            return false;
        }
        return false;
    }

    public Estimate getEnhancedBatteryPrediction(Context context) {
        long j;
        Cursor query = context.getContentResolver().query(getEnhancedBatteryPredictionUri(), null, null, null, null);
        if (query != null) {
            try {
                if (query.moveToFirst()) {
                    int columnIndex = query.getColumnIndex(BATTERY_ESTIMATE_BASED_ON_USAGE_COL);
                    boolean z = true;
                    if (columnIndex != -1) {
                        z = 1 == query.getInt(columnIndex);
                    }
                    int columnIndex2 = query.getColumnIndex(AVERAGE_BATTERY_LIFE_COL);
                    if (columnIndex2 != -1) {
                        long j2 = query.getLong(columnIndex2);
                        if (j2 != -1) {
                            long millis = Duration.ofMinutes(15L).toMillis();
                            if (Duration.ofMillis(j2).compareTo(Duration.ofDays(1L)) >= 0) {
                                millis = Duration.ofHours(1L).toMillis();
                            }
                            j = PowerUtil.roundTimeToNearestThreshold(j2, millis);
                            Estimate estimate = new Estimate(query.getLong(query.getColumnIndex(BATTERY_ESTIMATE_COL)), z, j);
                            query.close();
                            return estimate;
                        }
                    }
                    j = -1;
                    Estimate estimate2 = new Estimate(query.getLong(query.getColumnIndex(BATTERY_ESTIMATE_COL)), z, j);
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

    public SparseIntArray getEnhancedBatteryPredictionCurve(Context context, long j) {
        try {
            Cursor query = context.getContentResolver().query(getEnhancedBatteryPredictionCurveUri(), null, null, null, null);
            if (query == null) {
                if (query != null) {
                    query.close();
                    return null;
                }
                return null;
            }
            int columnIndex = query.getColumnIndex(TIMESTAMP_COL);
            int columnIndex2 = query.getColumnIndex(BATTERY_LEVEL_COL);
            SparseIntArray sparseIntArray = new SparseIntArray(query.getCount());
            while (query.moveToNext()) {
                sparseIntArray.append((int) (query.getLong(columnIndex) - j), query.getInt(columnIndex2));
            }
            query.close();
            return sparseIntArray;
        } catch (NullPointerException e) {
            return null;
        }
    }

    public CharSequence[] getHideApplicationEntries(Context context) {
        return context.getResources().getTextArray(R.array.allowlist_hide_entry_in_battery_usage);
    }

    public CharSequence[] getHideApplicationSummary(Context context) {
        return context.getResources().getTextArray(R.array.allowlist_hide_summary_in_battery_usage);
    }

    public Set<CharSequence> getHideBackgroundUsageTimeSet(Context context) {
        ArraySet arraySet = new ArraySet();
        Collections.addAll(arraySet, context.getResources().getTextArray(R.array.allowlist_hide_background_in_battery_usage));
        return arraySet;
    }

    public Intent getResumeChargeIntent(boolean z) {
        return new Intent(ACTION_RESUME_CHARGING).setPackage(PACKAGE_NAME_SYSTEMUI).addFlags(1342177280).putExtra(EXTRA_IS_DOCK_DEFENDER, z);
    }

    public boolean isAdaptiveChargingSupported() {
        return getAdaptiveChargingManager().isAvailable();
    }

    public boolean isChartGraphEnabled(Context context) {
        loadChartConfiguration(context);
        return sChartGraphEnabled;
    }

    public boolean isEnhancedBatteryPredictionEnabled(Context context) {
        if (isTurboEnabled(context)) {
            try {
                return mPackageManager.getPackageInfo("com.google.android.apps.turbo", 512).applicationInfo.enabled;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
        return false;
    }

    public boolean isExtraDefend() {
        Pair<IGoogleBattery, IBinder.DeathRecipient> initHalInterface = initHalInterface();
        Object obj = initHalInterface.first;
        boolean z = false;
        if (obj == null) {
            Log.e("PowerUsageFeatureProviderGoogleImpl", "Settings cannot init hal interface");
            return false;
        }
        try {
            try {
                int dockDefendStatus = ((IGoogleBattery) obj).getDockDefendStatus();
                Log.d("PowerUsageFeatureProviderGoogleImpl", "get dock defend status success: " + dockDefendStatus);
                if (dockDefendStatus == 1) {
                    z = true;
                }
                destroyHalInterface(initHalInterface);
                return z;
            } catch (Exception e) {
                Log.e("PowerUsageFeatureProviderGoogleImpl", "get dock defend status faield. ", e);
                destroyHalInterface(initHalInterface);
                return false;
            }
        } catch (Throwable th) {
            destroyHalInterface(initHalInterface);
            throw th;
        }
    }

    boolean isTurboEnabled(Context context) {
        return PhenotypeProxy.getBooleanFlagByPackageAndKey(context, "com.google.android.apps.turbo", "NudgesBatteryEstimates__estimated_time_remaining_provider_enabled", false);
    }

    void setPackageManager(PackageManager packageManager) {
        mPackageManager = packageManager;
    }
}
