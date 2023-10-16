package com.google.android.settings.core.instrumentation;

import com.android.settings.core.instrumentation.SettingsMetricsFeatureProvider;

public class SettingsGoogleMetricsFeatureProvider extends SettingsMetricsFeatureProvider {
    @Override 
    public void installLogWriters() {
        super.installLogWriters();
        mLoggerWriters.add(new SearchResultTraceLogWriter());
        mLoggerWriters.add(new AdbMetricsLogWriter());
    }
}
