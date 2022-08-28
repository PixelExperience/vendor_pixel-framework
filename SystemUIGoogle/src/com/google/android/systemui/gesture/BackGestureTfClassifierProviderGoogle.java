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

package com.google.android.systemui.gesture;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import com.android.systemui.navigationbar.gestural.BackGestureTfClassifierProvider;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class BackGestureTfClassifierProviderGoogle extends BackGestureTfClassifierProvider {
    private final String mVocabFile;
    private Interpreter mInterpreter;
    private AssetFileDescriptor mModelFileDescriptor;
    private final Map<Integer, Object> mOutputMap = new HashMap();
    private final float[][] mOutput = (float[][]) Array.newInstance(float.class, 1, 1);

    public BackGestureTfClassifierProviderGoogle(AssetManager assetManager, String str) {
        mModelFileDescriptor = null;
        mInterpreter = null;
        mVocabFile = str + ".vocab";
        mOutputMap.put(0, mOutput);
        try {
            AssetFileDescriptor openFd = assetManager.openFd(str + ".tflite");
            mModelFileDescriptor = openFd;
            mInterpreter = new Interpreter(openFd.createInputStream().getChannel().map(FileChannel.MapMode.READ_ONLY, mModelFileDescriptor.getStartOffset(), mModelFileDescriptor.getDeclaredLength()));
        } catch (Exception e) {
            Log.e("BackGestureTfClassifier", "Load TFLite file error:", e);
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public Map<String, Integer> loadVocab(AssetManager assetManager) {
        HashMap hashMap = new HashMap();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open(mVocabFile)));
            int i = 0;
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine == null) {
                    break;
                }
                hashMap.put(readLine, Integer.valueOf(i));
                i++;
            }
            bufferedReader.close();
        } catch (Exception e) {
            Log.e("BackGestureTfClassifier", "Load vocab file error: ", e);
        }
        return hashMap;
    }

    @Override
    public float predict(Object[] objArr) {
        Interpreter interpreter = mInterpreter;
        if (interpreter == null) {
            return -1.0f;
        }
        interpreter.runForMultipleInputsOutputs(objArr, mOutputMap);
        return mOutput[0][0];
    }

    @Override
    public void release() {
        Interpreter interpreter = mInterpreter;
        if (interpreter != null) {
            interpreter.close();
        }
        AssetFileDescriptor assetFileDescriptor = mModelFileDescriptor;
        if (assetFileDescriptor != null) {
            try {
                assetFileDescriptor.close();
            } catch (Exception e) {
                Log.e("BackGestureTfClassifier", "Failed to close model file descriptor: ", e);
            }
        }
    }
}
