/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.provider.mediacognitionutils;

import android.os.RemoteException;
import android.provider.MediaCognitionGetVersionsCallback;
import android.provider.MediaCognitionProcessingVersions;
import android.provider.MediaCognitionService;
import android.util.Log;

import androidx.annotation.NonNull;


/**
 * @hide
 */
public final class GetProcessingVersionsCallbackImpl implements MediaCognitionGetVersionsCallback {

    private final ICognitionGetVersionsCallbackInternal mBinderCallback;
    /**
     * To ensure a callback instance is called only once.
     */
    private boolean mCalled;

    public GetProcessingVersionsCallbackImpl(ICognitionGetVersionsCallbackInternal binderCallback) {
        mBinderCallback = binderCallback;
        mCalled = false;
    }

    @Override
    public void onSuccess(@NonNull MediaCognitionProcessingVersions processingVersions) {
        if (mCalled) {
            Log.w(MediaCognitionService.TAG, "The callback can only be called once");
            return;
        }
        mCalled = true;
        try {
            mBinderCallback.onGetProcessingVersionsSuccess(processingVersions);
        } catch (RemoteException e) {
            Log.w(MediaCognitionService.TAG,
                    "Unable to send callback of Get Versions result", e);
        }
    }

    @Override
    public void onFailure(@NonNull String message) {
        if (mCalled) {
            Log.w(MediaCognitionService.TAG, "The callback can only be called once");
            return;
        }
        mCalled = true;
        try {
            mBinderCallback.onGetProcessingVersionsFailure(message);
        } catch (RemoteException e) {
            Log.w(MediaCognitionService.TAG,
                    "Unable to send callback of Get Versions Failure", e);
        }
    }
}
