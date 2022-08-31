/*
 * Copyright (C) 2022 The PixelExperience Project
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

package com.google.android.systemui.dreamliner;

import android.os.Bundle;
import java.util.ArrayList;

public abstract class WirelessCharger {

    public interface AlignInfoListener {
        void onAlignInfoChanged(DockAlignInfo dockAlignInfo);
    }

    public interface ChallengeCallback {
        void onCallback(int i, ArrayList<Byte> arrayList);
    }

    public interface GetFanInformationCallback {
        void onCallback(int i, Bundle bundle);
    }

    public interface GetFanSimpleInformationCallback {
        void onCallback(int i, Bundle bundle);
    }

    public interface GetFeaturesCallback {
        void onCallback(int i, long j);
    }

    public interface GetInformationCallback {
        void onCallback(int i, DockInfo dockInfo);
    }

    public interface GetWpcAuthCertificateCallback {
        void onCallback(int i, ArrayList<Byte> arrayList);
    }

    public interface GetWpcAuthChallengeResponseCallback {
        void onCallback(int i, byte b, byte b2, byte b3, ArrayList<Byte> arrayList, ArrayList<Byte> arrayList2);
    }

    public interface GetWpcAuthDigestsCallback {
        void onCallback(int i, byte b, byte b2, ArrayList<byte[]> arrayList);
    }

    public interface IsDockPresentCallback {
        void onCallback(boolean z, byte b, byte b2, boolean z2, int i);
    }

    public interface KeyExchangeCallback {
        void onCallback(int i, byte b, ArrayList<Byte> arrayList);
    }

    public interface SetFanCallback {
        void onCallback(int i, Bundle bundle);
    }

    public interface SetFeaturesCallback {
        void onCallback(int i);
    }

    public abstract void asyncIsDockPresent(IsDockPresentCallback isDockPresentCallback);

    public abstract void challenge(byte b, byte[] bArr, ChallengeCallback challengeCallback);

    public abstract void getFanInformation(byte b, GetFanInformationCallback getFanInformationCallback);

    abstract int getFanLevel();

    public abstract void getFanSimpleInformation(byte b, GetFanSimpleInformationCallback getFanSimpleInformationCallback);

    public abstract void getFeatures(long j, GetFeaturesCallback getFeaturesCallback);

    public abstract void getInformation(GetInformationCallback getInformationCallback);

    public abstract void getWpcAuthCertificate(byte b, short s, short s2, GetWpcAuthCertificateCallback getWpcAuthCertificateCallback);

    public abstract void getWpcAuthChallengeResponse(byte b, byte[] bArr, GetWpcAuthChallengeResponseCallback getWpcAuthChallengeResponseCallback);

    public abstract void getWpcAuthDigests(byte b, GetWpcAuthDigestsCallback getWpcAuthDigestsCallback);

    public abstract void keyExchange(byte[] bArr, KeyExchangeCallback keyExchangeCallback);

    public abstract void registerAlignInfo(AlignInfoListener alignInfoListener);

    public abstract void setFan(byte b, byte b2, int i, SetFanCallback setFanCallback);

    public abstract void setFeatures(long j, long j2, SetFeaturesCallback setFeaturesCallback);
}
