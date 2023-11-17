package com.google.android.settings.fuelgauge;

import android.content.Intent;
import com.android.settings.SettingsActivity;
import com.android.settings.fuelgauge.batteryusage.PowerUsageAdvanced;

/* loaded from: classes2.dex */
public final class PowerUsageAdvancedActivity extends SettingsActivity {
    @Override // com.android.settings.SettingsActivity, android.app.Activity
    public Intent getIntent() {
        Intent intent = new Intent(super.getIntent());
        intent.putExtra(":settings:show_fragment", PowerUsageAdvanced.class.getName());
        return intent;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.settings.SettingsActivity
    public boolean isValidFragment(String str) {
        return PowerUsageAdvanced.class.getName().equals(str);
    }
}
