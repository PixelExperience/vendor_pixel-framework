/*
 * Copyright (C) 2022 The PixelExperience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.systemui.assist.uihints;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import com.android.internal.app.AssistUtils;
import com.android.systemui.dagger.SysUISingleton;
import com.google.android.systemui.assist.uihints.NgaMessageHandler;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

@SysUISingleton
public class AssistantPresenceHandler implements NgaMessageHandler.ConfigInfoListener {
    private final AssistUtils mAssistUtils;
    private final ContentResolver mContentResolver;
    private final Set<AssistantPresenceChangeListener> mAssistantPresenceChangeListeners = new HashSet();
    private final Set<SysUiIsNgaUiChangeListener> mSysUiIsNgaUiChangeListeners = new HashSet();
    private boolean mGoogleIsAssistant;
    private boolean mNgaIsAssistant;
    private boolean mSysUiIsNgaUi;

    @Inject
    AssistantPresenceHandler(Context context, AssistUtils assistUtils) {
        ContentResolver contentResolver = context.getContentResolver();
        mContentResolver = contentResolver;
        mAssistUtils = assistUtils;
        boolean z = false;
        mNgaIsAssistant = Settings.Secure.getInt(contentResolver, "com.google.android.systemui.assist.uihints.NGA_IS_ASSISTANT", 0) != 0;
        mSysUiIsNgaUi = Settings.Secure.getInt(contentResolver, "com.google.android.systemui.assist.uihints.SYS_UI_IS_NGA_UI", 0) != 0 || z;
    }

    @Override
    public void onConfigInfo(NgaMessageHandler.ConfigInfo configInfo) {
        updateAssistantPresence(fetchIsGoogleAssistant(), configInfo.ngaIsAssistant, configInfo.sysUiIsNgaUi);
    }

    public void registerAssistantPresenceChangeListener(AssistantPresenceChangeListener assistantPresenceChangeListener) {
        mAssistantPresenceChangeListeners.add(assistantPresenceChangeListener);
    }

    public void registerSysUiIsNgaUiChangeListener(SysUiIsNgaUiChangeListener sysUiIsNgaUiChangeListener) {
        mSysUiIsNgaUiChangeListeners.add(sysUiIsNgaUiChangeListener);
    }

    public void requestAssistantPresenceUpdate() {
        updateAssistantPresence(fetchIsGoogleAssistant(), mNgaIsAssistant, mSysUiIsNgaUi);
    }

    public boolean isSysUiNgaUi() {
        return mSysUiIsNgaUi;
    }

    public boolean isNgaAssistant() {
        return mNgaIsAssistant;
    }

    private void updateAssistantPresence(boolean z, boolean z2, boolean z3) {
        boolean z4 = true;
        boolean z5 = z && z2;
        if (!z5 || !z3) {
            z4 = false;
        }
        if (mGoogleIsAssistant != z || mNgaIsAssistant != z5) {
            mGoogleIsAssistant = z;
            mNgaIsAssistant = z5;
            int i = z5 ? 1 : 0;
            int i2 = z5 ? 1 : 0;
            Settings.Secure.putInt(mContentResolver, "com.google.android.systemui.assist.uihints.NGA_IS_ASSISTANT", i);
            for (AssistantPresenceChangeListener assistantPresenceChangeListener : mAssistantPresenceChangeListeners) {
                assistantPresenceChangeListener.onAssistantPresenceChanged(mGoogleIsAssistant, mNgaIsAssistant);
            }
        }
        if (mSysUiIsNgaUi != z4) {
            mSysUiIsNgaUi = z4;
            int i3 = z4 ? 1 : 0;
            int i4 = z4 ? 1 : 0;
            Settings.Secure.putInt(mContentResolver, "com.google.android.systemui.assist.uihints.SYS_UI_IS_NGA_UI", i3);
            for (SysUiIsNgaUiChangeListener sysUiIsNgaUiChangeListener : mSysUiIsNgaUiChangeListeners) {
                sysUiIsNgaUiChangeListener.onSysUiIsNgaUiChanged(mSysUiIsNgaUi);
            }
        }
    }

    private boolean fetchIsGoogleAssistant() {
        ComponentName assistComponentForUser = mAssistUtils.getAssistComponentForUser(-2);
        return assistComponentForUser != null && "com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService".equals(assistComponentForUser.flattenToString());
    }

    public interface AssistantPresenceChangeListener {
        void onAssistantPresenceChanged(boolean z, boolean z2);
    }

    interface SysUiIsNgaUiChangeListener {
        void onSysUiIsNgaUiChanged(boolean z);
    }
}
