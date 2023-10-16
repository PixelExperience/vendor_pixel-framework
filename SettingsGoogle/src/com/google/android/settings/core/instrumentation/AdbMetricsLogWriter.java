package com.google.android.settings.core.instrumentation;

import android.content.Context;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.util.Pair;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.LogWriter;

public class AdbMetricsLogWriter implements LogWriter {
    private final Context mContext = FeatureFactory.getAppContext();

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void visible(Context context, int i, int i2, int i3) {
        if (FeatureFlagUtils.isEnabled(mContext, "settings_adb_metrics_writer")) {
            Log.v("AdbMetricsLogWriter", "visible (pageId = " + i2 + ", source = " + i + " , latency = " + i3 + ")");
        }
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void hidden(Context context, int i, int i2) {
        if (FeatureFlagUtils.isEnabled(mContext, "settings_adb_metrics_writer")) {
            Log.v("AdbMetricsLogWriter", "hidden (pageId = " + i + ", visibleTime = " + i2 + ")");
        }
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void clicked(int i, String str) {
        if (FeatureFlagUtils.isEnabled(mContext, "settings_adb_metrics_writer")) {
            Log.v("AdbMetricsLogWriter", "clicked (pageId = " + i + ", key = " + str + ")");
        }
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void changed(int i, String str, int i2) {
        if (FeatureFlagUtils.isEnabled(mContext, "settings_adb_metrics_writer")) {
            Log.v("AdbMetricsLogWriter", "changed (pageId = " + i + ", key = " + str + ", value = " + i2 + ")");
        }
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void action(Context context, int i, Pair<Integer, Object>... pairArr) {
        FeatureFlagUtils.isEnabled(mContext, "settings_adb_metrics_writer");
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void action(Context context, int i, int i2) {
        FeatureFlagUtils.isEnabled(mContext, "settings_adb_metrics_writer");
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void action(Context context, int i, boolean z) {
        FeatureFlagUtils.isEnabled(mContext, "settings_adb_metrics_writer");
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void action(Context context, int i, String str) {
        FeatureFlagUtils.isEnabled(mContext, "settings_adb_metrics_writer");
    }

    @Override // com.android.settingslib.core.instrumentation.LogWriter
    public void action(int i, int i2, int i3, String str, int i4) {
        FeatureFlagUtils.isEnabled(mContext, "settings_adb_metrics_writer");
    }
}
