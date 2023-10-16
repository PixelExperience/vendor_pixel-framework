package com.google.android.settings.core.instrumentation;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.LogWriter;
import com.android.settingslib.utils.ThreadUtils;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class SearchResultTraceLogWriter implements LogWriter {
    static final int DASHBOARD_SEARCH_RESULTS = 34;
    static final String KEY_LOG_TO_DATABASE_ENABLED = "log_to_database_enabled";
    static final int OFF = 0;
    static final int ON = 1;

    public static void writeToContentProvider(ContentProviderClient client, Uri uri, ContentValues values) {
        performWriteAction(client, uri, values);
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void action(Context context, int i, int i2) {
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void action(Context context, int i, String str) {
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void action(Context context, int i, boolean z) {
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void action(Context context, int i, Pair<Integer, Object>... pairArr) {
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void changed(int i, String str, int i2) {
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void clicked(int i, String str) {
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void hidden(Context context, int i, int i2) {
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void visible(Context context, int i, int i2, int i3) {
        if (i == 34) {
            setLogToDatabaseEnabled(true);
        }
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void action(int i, int i2, int i3, String str, int i4) {
        if (isLogToDatabaseEnabled()) {
            final Uri build = new Uri.Builder().scheme("content").authority("com.google.android.settings.intelligence.modules.search.logging").build();
            final ContentValues generateContentValues = generateContentValues(i, i2, i3, str, i4);
            try {
                final ContentProviderClient acquireUnstableContentProviderClient = FeatureFactory.getAppContext().getContentResolver().acquireUnstableContentProviderClient(build);
                if (acquireUnstableContentProviderClient == null) {
                    Log.w("SRTraceLogWriter", "Client not found. Skipping logging.");
                    setLogToDatabaseEnabled(false);
                    if (acquireUnstableContentProviderClient != null) {
                        acquireUnstableContentProviderClient.close();
                        return;
                    }
                    return;
                }
                ThreadUtils.postOnBackgroundThread(new Runnable() {
                    @Override
                    public void run() {
                        SearchResultTraceLogWriter.writeToContentProvider(acquireUnstableContentProviderClient, build, generateContentValues);
                    }
                });
                acquireUnstableContentProviderClient.close();
            } catch (Exception e) {
                setLogToDatabaseEnabled(false);
                Log.w("SRTraceLogWriter", "Unable to send logs. Skipping.", e);
            }
        }
    }

    private static void performWriteAction(ContentProviderClient client, Uri uri, ContentValues values) {
        try {
            client.insert(uri, values);
        } catch (RemoteException e) {
            Log.w("SRTraceLogWriter", "Unable to send logs.", e);
        }
    }

    boolean isLogToDatabaseEnabled() {
        Context appContext = FeatureFactory.getAppContext();
        return appContext != null && Settings.Global.getInt(appContext.getContentResolver(), KEY_LOG_TO_DATABASE_ENABLED, 0) == 1;
    }

    void setLogToDatabaseEnabled(boolean z) {
        Context appContext = FeatureFactory.getAppContext();
        if (appContext == null) {
            Log.w("SRTraceLogWriter", "Context not found.");
        } else {
            Settings.Global.putInt(appContext.getContentResolver(), KEY_LOG_TO_DATABASE_ENABLED, z ? 1 : 0);
        }
    }

    ContentValues generateContentValues(int i, int i2, int i3, String str, int i4) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("created_time", Long.valueOf(Timestamp.from(ZonedDateTime.now(ZoneId.systemDefault()).toInstant()).getTime()));
        contentValues.put("parent_page", Integer.valueOf(i));
        contentValues.put("action", Integer.valueOf(i2));
        contentValues.put("current_page", Integer.valueOf(i3));
        contentValues.put("pref_key", str);
        contentValues.put("value", Integer.valueOf(i4));
        return contentValues;
    }
}
