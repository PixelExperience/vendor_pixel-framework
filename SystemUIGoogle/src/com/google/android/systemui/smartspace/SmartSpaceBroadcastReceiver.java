/*
 * Copyright (C) 2023 The PixelExperience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.systemui.smartspace;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.util.Log;
import com.android.systemui.broadcast.BroadcastSender;
import com.android.systemui.smartspace.nano.SmartspaceProto;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;

public class SmartSpaceBroadcastReceiver extends BroadcastReceiver {
    public final BroadcastSender mBroadcastSender;
    public final SmartSpaceController mController;

    public SmartSpaceBroadcastReceiver(SmartSpaceController smartSpaceController, BroadcastSender broadcastSender) {
        this.mController = smartSpaceController;
        this.mBroadcastSender = broadcastSender;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SmartspaceProto.SmartspaceUpdate.SmartspaceCard[] smartspaceCardArr;
        if (SmartSpaceController.DEBUG) {
            Log.d("SmartSpaceReceiver", "receiving update");
        }
        int myUserId = UserHandle.myUserId();
        if (myUserId != 0) {
            if (intent.getBooleanExtra("rebroadcast", false)) {
                return;
            }
            intent.putExtra("rebroadcast", true);
            intent.putExtra("uid", myUserId);
            this.mBroadcastSender.sendBroadcastAsUser(intent, UserHandle.ALL);
            return;
        }
        if (!intent.hasExtra("uid")) {
            intent.putExtra("uid", myUserId);
        }
        byte[] byteArrayExtra = intent.getByteArrayExtra("com.google.android.apps.nexuslauncher.extra.SMARTSPACE_CARD");
        if (byteArrayExtra != null) {
            SmartspaceProto.SmartspaceUpdate smartspaceUpdate = new SmartspaceProto.SmartspaceUpdate();
            try {
                MessageNano.mergeFrom(smartspaceUpdate, byteArrayExtra);
                for (SmartspaceProto.SmartspaceUpdate.SmartspaceCard smartspaceCard : smartspaceUpdate.card) {
                    int i = smartspaceCard.cardPriority;
                    boolean z = i == 1;
                    boolean z2 = i == 2;
                    if (!z && !z2) {
                        Log.w("SmartSpaceReceiver", "unrecognized card priority: " + smartspaceCard.cardPriority);
                    }
                    notify(smartspaceCard, context, intent, z);
                }
                return;
            } catch (InvalidProtocolBufferNanoException ex) {
                Log.e("SmartSpaceReceiver", "proto", ex);
                return;
            }
        }
        Log.e("SmartSpaceReceiver", "receiving update with no proto: " + intent.getExtras());
    }

    public void notify(SmartspaceProto.SmartspaceUpdate.SmartspaceCard smartspaceCard, Context context, Intent intent, boolean z) {
        PackageInfo packageInfo;
        long currentTimeMillis = System.currentTimeMillis();
        try {
            packageInfo = context.getPackageManager().getPackageInfo("com.google.android.googlequicksearchbox", PackageManager.PackageInfoFlags.of(0L));
        } catch (PackageManager.NameNotFoundException ex) {
            Log.w("SmartSpaceReceiver", "Cannot find GSA", ex);
            packageInfo = null;
        }
        this.mController.onNewCard(new NewCardInfo(smartspaceCard, intent, z, currentTimeMillis, packageInfo));
    }
}