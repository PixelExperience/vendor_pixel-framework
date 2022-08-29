package com.google.android.systemui;

import android.content.Context;

import com.android.systemui.VendorServices;

import java.util.ArrayList;

public class GoogleServices extends VendorServices {
    private final ArrayList<Object> mServices;

    public GoogleServices(Context context) {
        super(context);
        mServices = new ArrayList<>();
    }

    @Override
    public void start() {
    }

    private void addService(Object obj) {
        if (obj != null) {
            mServices.add(obj);
        }
    }
}
