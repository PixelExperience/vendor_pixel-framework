package com.google.android.settings.fuelgauge;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;
import com.android.settings.fuelgauge.batteryusage.BatteryChartPreferenceController;
import com.android.settings.fuelgauge.batteryusage.BatteryDiffEntry;
import java.util.List;
import java.util.function.Consumer;

public final class BatteryUsageContentProvider extends ContentProvider {
    private static final UriMatcher URI_MATCHER;
    static List<BatteryDiffEntry> sCacheBatteryDiffEntries;

    @Override
    public String getType(Uri uri) {
        return null;
    }

    static {
        URI_MATCHER = new UriMatcher(-1);
        URI_MATCHER.addURI("com.google.android.settings.fuelgauge.provider", "BatteryUsageState", 1);
        sCacheBatteryDiffEntries = null;
    }

    @Override
    public boolean onCreate() {
        Log.v("BatteryUsageContentProvider", "initialize provider");
        return true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException("insert() unsupported!");
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        throw new UnsupportedOperationException("update() unsupported!");
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        throw new UnsupportedOperationException("delete() unsupported!");
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        Log.d("BatteryUsageContentProvider", "query:" + uri);
        if (URI_MATCHER.match(uri) != 1) {
            return null;
        }
        return getBatteryUsageData();
    }

    private Cursor getBatteryUsageData() {
        List<BatteryDiffEntry> list = sCacheBatteryDiffEntries;
        if (list == null) {
            list = BatteryChartPreferenceController.getAppBatteryUsageData(getContext());
        }
        if (list == null || list.isEmpty()) {
            Log.w("BatteryUsageContentProvider", "no data found in the getBatterySinceLastFullChargeUsageData()");
            return null;
        }
        final MatrixCursor matrixCursor = new MatrixCursor(BatteryUsageContract.KEYS_BATTERY_USAGE_STATE);
        list.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                BatteryDiffEntry batteryDiffEntry = (BatteryDiffEntry) obj;
                if (batteryDiffEntry.mBatteryHistEntry == null || batteryDiffEntry.getPercentOfTotal() == 0.0d) {
                    return;
                }
                addUsageDataRow(matrixCursor, batteryDiffEntry);
            }
        });
        Log.d("BatteryUsageContentProvider", "usage data count:" + matrixCursor.getCount());
        return matrixCursor;
    }

    private static void addUsageDataRow(MatrixCursor matrixCursor, BatteryDiffEntry batteryDiffEntry) {
        String packageName = batteryDiffEntry.getPackageName();
        if (packageName == null) {
            Log.w("BatteryUsageContentProvider", "no package name found for\n" + batteryDiffEntry);
            return;
        }
        matrixCursor.addRow(new Object[]{new Long(batteryDiffEntry.mBatteryHistEntry.mUserId), packageName, new Double(batteryDiffEntry.getPercentOfTotal()), new Long(batteryDiffEntry.mForegroundUsageTimeInMs), new Long(batteryDiffEntry.mBackgroundUsageTimeInMs)});
    }
}
