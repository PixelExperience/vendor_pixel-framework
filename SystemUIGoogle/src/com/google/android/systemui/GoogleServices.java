package com.google.android.systemui;

import android.content.Context;

import com.android.systemui.R;
import com.android.systemui.VendorServices;

import com.google.android.systemui.input.TouchContextService;

import java.util.ArrayList;

public class GoogleServices extends VendorServices {
    private final ArrayList<Object> mServices;

    public GoogleServices(Context context) {
        super(context);
        mServices = new ArrayList<>();
    }

    @Override
    public void start() {
        if (mContext.getResources().getBoolean(R.bool.config_touch_context_enabled)) {
            addService(new TouchContextService(mContext));
        }
    }

    private void addService(Object obj) {
        if (obj != null) {
            mServices.add(obj);
        }
    }
}
