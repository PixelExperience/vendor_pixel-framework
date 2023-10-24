package com.google.android.systemui.columbus.actions;

import android.content.Context;
import android.os.Handler;
import com.android.internal.logging.UiEventLogger;
import com.android.internal.util.ScreenshotHelper;
import com.google.android.systemui.columbus.ColumbusEvent;
import com.google.android.systemui.columbus.sensors.GestureSensor;
import java.util.function.Consumer;
import kotlin.jvm.internal.DefaultConstructorMarker;

public final class TakeScreenshot extends UserAction {
    public static final Companion Companion = new Companion(null);
    private final Handler handler;
    private final ScreenshotHelper screenshotHelper;
    private final String tag;
    private final UiEventLogger uiEventLogger;

    public static final class Companion {
        private Companion() {}

        public Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }
    }

    public TakeScreenshot(Context context, Handler handler, UiEventLogger uiEventLogger) {
        super(context, null, 2, null);
        this.handler = handler;
        this.uiEventLogger = uiEventLogger;
        this.tag = "Columbus/TakeScreenshot";
        this.screenshotHelper = new ScreenshotHelper(context);
        setAvailable(true);
    }

    @Override
    public boolean availableOnLockscreen() {
        return true;
    }

    @Override
    public String getTag$vendor__unbundled_google__packages__SystemUIGoogle__android_common__sysuig() {
        return tag;
    }

    @Override
    public void onTrigger(GestureSensor.DetectionProperties detectionProperties) {
        screenshotHelper.takeScreenshot(6, handler, null);
        uiEventLogger.log(ColumbusEvent.COLUMBUS_INVOKED_SCREENSHOT);
    }
}
