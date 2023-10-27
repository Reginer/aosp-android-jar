/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.app.sdksandbox;

import android.annotation.NonNull;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Objects;

/**
 * Singleton for a privacy sandbox, which is initialised when the sandbox is created.
 *
 * @hide
 */
public class SdkSandboxLocalSingleton {

    private static final String TAG = "SandboxLocalSingleton";
    private static SdkSandboxLocalSingleton sInstance = null;
    private final ISdkToServiceCallback mSdkToServiceCallback;

    private SdkSandboxLocalSingleton(ISdkToServiceCallback sdkToServiceCallback) {
        mSdkToServiceCallback = sdkToServiceCallback;
    }

    /**
     * Returns a singleton instance of this class. TODO(b/247313241): Fix parameter once aidl issues
     * are fixed.
     *
     * @param sdkToServiceBinder callback to support communication with the {@link
     *     com.android.server.sdksandbox.SdkSandboxManagerService}
     * @throws IllegalStateException if singleton is already initialised
     * @throws UnsupportedOperationException if the interface passed is not of type {@link
     *     ISdkToServiceCallback}
     */
    public static synchronized void initInstance(@NonNull IBinder sdkToServiceBinder) {
        if (sInstance != null) {
            Log.d(TAG, "Already Initialised");
            return;
        }
        try {
            if (Objects.nonNull(sdkToServiceBinder)
                    && sdkToServiceBinder
                            .getInterfaceDescriptor()
                            .equals(ISdkToServiceCallback.DESCRIPTOR)) {
                sInstance =
                        new SdkSandboxLocalSingleton(
                                ISdkToServiceCallback.Stub.asInterface(sdkToServiceBinder));
                return;
            }
        } catch (RemoteException e) {
            // Fall through to the failure case.
        }
        throw new UnsupportedOperationException("IBinder not supported");
    }

    /** Returns an already initialised singleton instance of this class. */
    public static SdkSandboxLocalSingleton getExistingInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("SdkSandboxLocalSingleton not found");
        }
        return sInstance;
    }

    /** To reset the singleton. Only for Testing. */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public static void destroySingleton() {
        sInstance = null;
    }

    /** Gets the callback to the {@link com.android.server.sdksandbox.SdkSandboxManagerService} */
    public ISdkToServiceCallback getSdkToServiceCallback() {
        return mSdkToServiceCallback;
    }
}
