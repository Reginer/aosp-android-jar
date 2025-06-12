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

package android.net.wifi.usd;

import static android.Manifest.permission.MANAGE_WIFI_NETWORK_SELECTION;

import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.RequiresApi;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.net.wifi.flags.Flags;
import android.net.wifi.util.Environment;
import android.os.Build;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;


/**
 * A class to represent the USD publish session
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@SystemApi
@FlaggedApi(Flags.FLAG_USD)
public class PublishSession {
    private static final String TAG = PublishSession.class.getName();
    private final WeakReference<UsdManager> mUsdManager;
    private final int mSessionId;

    /** @hide */
    public PublishSession(@NonNull UsdManager usdManager, int sessionId) {
        mUsdManager = new WeakReference<>(usdManager);
        mSessionId = sessionId;
    }


    /** @hide */
    public int getSessionId() {
        return mSessionId;
    }

    /**
     * Cancel the Publish Session
     *
     */
    @RequiresPermission(MANAGE_WIFI_NETWORK_SELECTION)
    public void cancel() {
        if (!Environment.isSdkAtLeastB()) {
            throw new UnsupportedOperationException();
        }
        UsdManager usdManager = mUsdManager.get();
        if (usdManager == null) {
            Log.w(TAG, "cancel is called after the UsdManager has been garbage collected");
            return;
        }
        usdManager.cancelPublish(mSessionId);
    }

    /**
     * Update the publish session with service specific info. The new value will override any
     * service specific information previously passed to the publish or updatePublish methods for
     * this session. To clear service specific info, set an empty byte array.
     *
     * @param serviceSpecificInfo service specific info
     */
    @RequiresPermission(MANAGE_WIFI_NETWORK_SELECTION)
    public void updatePublish(@NonNull byte[] serviceSpecificInfo) {
        Objects.requireNonNull(serviceSpecificInfo, "serviceSpecificInfo must not be null");
        if (!Environment.isSdkAtLeastB()) {
            throw new UnsupportedOperationException();
        }
        UsdManager usdManager = mUsdManager.get();
        if (usdManager == null) {
            Log.w(TAG, "updatePublish is called after the UsdManager has been garbage collected");
            return;
        }
        usdManager.updatePublish(mSessionId, serviceSpecificInfo);
    }

    /**
     * Send a message to the peer. Message length is limited by
     * {@link Characteristics#getMaxServiceSpecificInfoLength()}.
     *
     * @param peerId         peer id obtained from {@link DiscoveryResult#getPeerId()}
     * @param message        byte array
     * @param executor       executor
     * @param resultCallback result callback
     */
    @RequiresPermission(MANAGE_WIFI_NETWORK_SELECTION)
    public void sendMessage(int peerId, @NonNull byte[] message,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Boolean> resultCallback) {
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(resultCallback, "resultCallback must not be null");
        Objects.requireNonNull(message, "message must not be null");
        if (!Environment.isSdkAtLeastB()) {
            throw new UnsupportedOperationException();
        }
        UsdManager usdManager = mUsdManager.get();
        if (usdManager == null) {
            Log.w(TAG, "sendMessage is called after the UsdManager has been garbage collected");
            executor.execute(() -> resultCallback.accept(false));
            return;
        }
        usdManager.sendMessage(mSessionId, peerId, message, executor, resultCallback);
    }
}
