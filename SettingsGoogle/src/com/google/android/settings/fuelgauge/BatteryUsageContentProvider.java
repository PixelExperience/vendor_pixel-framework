package com.google.android.settings.fuelgauge;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;
import com.android.internal.os.PowerProfile;
import com.android.settings.fuelgauge.batteryusage.BatteryChartPreferenceController;
import com.android.settings.fuelgauge.batteryusage.BatteryDiffEntry;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Consumer;

/* loaded from: classes2.dex */
public final class BatteryUsageContentProvider extends ContentProvider {
    private static final UriMatcher URI_MATCHER;
    static List<BatteryDiffEntry> sCacheBatteryDiffEntries;

    /* renamed from: $r8$lambda$piQTleXoYTG2WU3nGzKMy-cQQZQ */
    public static /* synthetic */ void m4598$r8$lambda$piQTleXoYTG2WU3nGzKMycQQZQ(MatrixCursor matrixCursor, BatteryDiffEntry batteryDiffEntry) {
        lambda$getBatteryUsageData$0(matrixCursor, batteryDiffEntry);
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        return null;
    }

    static {
        UriMatcher uriMatcher = new UriMatcher(-1);
        URI_MATCHER = uriMatcher;
        uriMatcher.addURI("com.google.android.settings.fuelgauge.provider", "BatteryUsageState", 1);
        sCacheBatteryDiffEntries = null;
    }

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        Log.v("BatteryUsageContentProvider", "initialize provider");
        return true;
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException("insert() unsupported!");
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        throw new UnsupportedOperationException("update() unsupported!");
    }

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String str, String[] strArr) {
        throw new UnsupportedOperationException("delete() unsupported!");
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        Log.d("BatteryUsageContentProvider", "query:" + uri);
        if (URI_MATCHER.match(uri) != 1) {
            return null;
        }
        return getBatteryUsageData();
    }

    @Override // android.content.ContentProvider
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("dump BatteryUsage states:");
        double averagePowerForOrdinal = new PowerProfile(getContext()).getAveragePowerForOrdinal("screen.full.display", 0);
        printWriter.println("\tPowerProfile.getAveragePowerForOrdinal(): " + averagePowerForOrdinal);
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
        list.forEach(new Consumer() { // from class: com.google.android.settings.fuelgauge.BatteryUsageContentProvider$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                BatteryUsageContentProvider.m4598$r8$lambda$piQTleXoYTG2WU3nGzKMycQQZQ(matrixCursor, (BatteryDiffEntry) obj);
            }
        });
        Log.d("BatteryUsageContentProvider", "usage data count:" + matrixCursor.getCount());
        return matrixCursor;
    }

    public static /* synthetic */ void lambda$getBatteryUsageData$0(MatrixCursor matrixCursor, BatteryDiffEntry batteryDiffEntry) {
        if (batteryDiffEntry.mBatteryHistEntry == null || batteryDiffEntry.getPercentage() == 0.0d) {
            return;
        }
        addUsageDataRow(matrixCursor, batteryDiffEntry);
    }

    private static void addUsageDataRow(MatrixCursor matrixCursor, BatteryDiffEntry batteryDiffEntry) {
        String packageName = batteryDiffEntry.getPackageName();
        if (packageName == null) {
            Log.w("BatteryUsageContentProvider", "no package name found for\n" + batteryDiffEntry);
            return;
        }
        matrixCursor.addRow(new Object[]{new Long(batteryDiffEntry.mBatteryHistEntry.mUserId), packageName, new Double(batteryDiffEntry.getPercentage()), new Long(batteryDiffEntry.mForegroundUsageTimeInMs), new Long(batteryDiffEntry.mBackgroundUsageTimeInMs)});
    }
}
