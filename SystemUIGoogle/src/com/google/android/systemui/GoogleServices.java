package com.google.android.systemui;

import android.app.AlarmManager;
import android.content.Context;
import com.android.internal.logging.UiEventLogger;

import com.android.systemui.R;
import com.android.systemui.VendorServices;
import com.android.systemui.statusbar.phone.CentralSurfaces;

import com.google.android.systemui.ambientmusic.AmbientIndicationContainer;
import com.google.android.systemui.ambientmusic.AmbientIndicationService;
import com.google.android.systemui.input.TouchContextService;
import com.google.android.systemui.columbus.ColumbusContext;
import com.google.android.systemui.columbus.ColumbusServiceWrapper;
import com.google.android.systemui.elmyra.ElmyraContext;
import com.google.android.systemui.elmyra.ElmyraService;
import com.google.android.systemui.elmyra.ServiceConfigurationGoogle;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.Lazy;

public class GoogleServices extends VendorServices {
    private final ArrayList<Object> mServices;
    private final CentralSurfaces mCentralSurfaces;
    private final AlarmManager mAlarmManager;
    private final UiEventLogger mUiEventLogger;
    private final Lazy<ServiceConfigurationGoogle> mServiceConfigurationGoogle;
    private final Lazy<ColumbusServiceWrapper> mColumbusServiceLazy;

    @Inject
    public GoogleServices(Context context, AlarmManager alarmManager, CentralSurfaces centralSurfaces, UiEventLogger uiEventLogger, Lazy<ServiceConfigurationGoogle> serviceConfigurationGoogleLazy, Lazy<ColumbusServiceWrapper> columbusServiceWrapperLazy) {
        super(context);
        mServices = new ArrayList<>();
        mAlarmManager = alarmManager;
        mCentralSurfaces = centralSurfaces;
        mUiEventLogger = uiEventLogger;
        mServiceConfigurationGoogle = serviceConfigurationGoogleLazy;
        mColumbusServiceLazy = columbusServiceWrapperLazy;
    }

    @Override
    public void start() {
        if (mContext.getPackageManager().hasSystemFeature("android.hardware.context_hub") && new ElmyraContext(mContext).isAvailable()) {
            addService(new ElmyraService(mContext, mServiceConfigurationGoogle.get(), mUiEventLogger));
        }
        if (new ColumbusContext(mContext).isAvailable()) {
            addService(mColumbusServiceLazy.get());
        }
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
