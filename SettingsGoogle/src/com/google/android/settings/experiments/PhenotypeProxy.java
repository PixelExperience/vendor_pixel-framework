package com.google.android.settings.experiments;

import android.content.ContentProviderClient;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import java.util.List;

/* loaded from: classes2.dex */
public class PhenotypeProxy {
    private static final Uri PROXY_AUTHORITY = new Uri.Builder().scheme("content").authority("com.google.android.settings.intelligence.provider.experimentflags").build();

    public static boolean getBooleanFlagByPackageAndKey(Context context, String str, String str2, boolean z) {
        Bundle flagByPackageAndKey = getFlagByPackageAndKey(context, str, str2, "getBooleanForPackageAndKey");
        return flagByPackageAndKey == null ? z : flagByPackageAndKey.getBoolean("value", z);
    }

    public static double getDoubleFlagByPackageAndKey(Context context, String str, String str2, double d) {
        Bundle flagByPackageAndKey = getFlagByPackageAndKey(context, str, str2, "getDoubleForPackageAndKey");
        return flagByPackageAndKey == null ? d : flagByPackageAndKey.getDouble("value", d);
    }

    public static String getStringFlagByPackageAndKey(Context context, String str, String str2, String str3) {
        Bundle flagByPackageAndKey = getFlagByPackageAndKey(context, str, str2, "getStringForPackageAndKey");
        return flagByPackageAndKey == null ? str3 : flagByPackageAndKey.getString("value", str3);
    }

    public static List<String> getStringListFlagByPackageAndKey(Context context, String str, String str2, List<String> list) {
        Bundle flagByPackageAndKey = getFlagByPackageAndKey(context, str, str2, "getStringListForPackageAndKey");
        return flagByPackageAndKey == null ? list : flagByPackageAndKey.getStringArrayList("value");
    }

    private static Bundle getFlagByPackageAndKey(Context context, String str, String str2, String str3) {
        Bundle bundle = new Bundle();
        bundle.putString("package_name", str);
        bundle.putString("key", str2);
        Bundle bundle2 = null;
        try {
            ContentProviderClient acquireUnstableContentProviderClient = context.getApplicationContext().getContentResolver().acquireUnstableContentProviderClient(PROXY_AUTHORITY);
            bundle2 = acquireUnstableContentProviderClient.call(str3, null, bundle);
            acquireUnstableContentProviderClient.close();
        } catch (Exception e) {
            Log.e("PhenotypeProxy", "Failed to query experiment provider", e);
        }
        return bundle2;
    }
}
