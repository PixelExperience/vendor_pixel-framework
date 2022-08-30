package com.google.android.systemui;

import android.app.AlarmManager;
import android.content.Context;

import com.android.systemui.R;
import com.android.systemui.VendorServices;
import com.android.systemui.statusbar.phone.CentralSurfaces;

import com.google.android.systemui.ambientmusic.AmbientIndicationContainer;
import com.google.android.systemui.ambientmusic.AmbientIndicationService;
import com.google.android.systemui.input.TouchContextService;

import java.util.ArrayList;

import javax.inject.Inject;

public class GoogleServices extends VendorServices {
    private final ArrayList<Object> mServices;
    private final CentralSurfaces mCentralSurfaces;
    private final AlarmManager mAlarmManager;

    @Inject
    public GoogleServices(Context context, AlarmManager alarmManager, CentralSurfaces centralSurfaces) {
        super(context);
        mServices = new ArrayList<>();
        mAlarmManager = alarmManager;
        mCentralSurfaces = centralSurfaces;
    }

    @Override
    public void start() {
        if (mContext.getResources().getBoolean(R.bool.config_touch_context_enabled)) {
            addService(new TouchContextService(mContext));
        }
        AmbientIndicationContainer ambientIndicationContainer = (AmbientIndicationContainer) mCentralSurfaces.getNotificationShadeWindowView().findViewById(R.id.ambient_indication_container);
        ambientIndicationContainer.initializeView(mCentralSurfaces);
        addService(new AmbientIndicationService(mContext, ambientIndicationContainer, mAlarmManager));
    }

    private void addService(Object obj) {
        if (obj != null) {
            mServices.add(obj);
        }
    }
}
