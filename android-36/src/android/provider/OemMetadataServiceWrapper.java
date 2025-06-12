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

package android.provider;

import static android.provider.OemMetadataService.EXTRA_OEM_DATA_KEYS;
import static android.provider.OemMetadataService.EXTRA_OEM_DATA_VALUES;
import static android.provider.OemMetadataService.EXTRA_OEM_SUPPORTED_MIME_TYPES;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallback;
import android.util.Log;

import com.android.providers.media.flags.Flags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Wrapper defined to handle async calls to OemMetadataService.
 * @hide
 */
public final class OemMetadataServiceWrapper {

    private static final String TAG = "OemMetadataServiceWrapper";

    private static final long DEFAULT_TIMEOUT_IN_SECONDS = 1L;

    private final IOemMetadataService mOemMetadataService;

    private final ExecutorService mExecutorService;

    private final long mServiceTimeoutInSeconds;

    public OemMetadataServiceWrapper(@NonNull IOemMetadataService oemMetadataService) {
        this(oemMetadataService, DEFAULT_TIMEOUT_IN_SECONDS);
    }

    public OemMetadataServiceWrapper(@NonNull IOemMetadataService oemMetadataService,
            long serviceTimeoutInSeconds) {
        Objects.requireNonNull(oemMetadataService);

        this.mOemMetadataService = oemMetadataService;
        this.mServiceTimeoutInSeconds = serviceTimeoutInSeconds;
        mExecutorService = Executors.newFixedThreadPool(3);
    }

    /**
     * Gets supported mimetype from OemMetadataService within certain timeout.
     */
    public Set<String> getSupportedMimeTypes()
            throws ExecutionException, InterruptedException, TimeoutException {
        if (!Flags.enableOemMetadata()) {
            return new HashSet<>();
        }

        return mExecutorService.submit(() -> {
            CompletableFuture<Set<String>> future = new CompletableFuture<>();
            RemoteCallback callback = new RemoteCallback(
                    result -> setResultForGetSupportedMimeTypes(result, future));
            mOemMetadataService.getSupportedMimeTypes(callback);
            return future.get();
        }).get(mServiceTimeoutInSeconds, TimeUnit.SECONDS);
    }

    /**
     * Gets OEM custom data from OemMetadataService within certain timeout.
     */
    public Map<String, String> getOemCustomData(ParcelFileDescriptor pfd)
            throws ExecutionException, InterruptedException, TimeoutException {
        if (!Flags.enableOemMetadata()) {
            return new HashMap<>();
        }

        return mExecutorService.submit(() -> {
            CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
            RemoteCallback callback = new RemoteCallback(
                    result -> setResultForGetOemCustomData(result, future));
            mOemMetadataService.getOemCustomData(pfd, callback);
            return future.get();
        }).get(mServiceTimeoutInSeconds, TimeUnit.SECONDS);
    }

    @FlaggedApi(Flags.FLAG_ENABLE_OEM_METADATA)
    private void setResultForGetSupportedMimeTypes(Bundle result,
            CompletableFuture<Set<String>> future) {
        if (result.containsKey(EXTRA_OEM_SUPPORTED_MIME_TYPES)) {
            ArrayList<String> supportedMimeTypes = result.getStringArrayList(
                    EXTRA_OEM_SUPPORTED_MIME_TYPES);
            future.complete(Set.copyOf(supportedMimeTypes));
        } else {
            Log.v(TAG, "No data received for getSupportedMimeTypes()");
            future.complete(new HashSet<>());
        }
    }

    @FlaggedApi(Flags.FLAG_ENABLE_OEM_METADATA)
    private void setResultForGetOemCustomData(Bundle result,
            CompletableFuture<Map<String, String>> future) {
        if (result.containsKey(EXTRA_OEM_DATA_KEYS) && result.containsKey(EXTRA_OEM_DATA_VALUES)) {
            Map<String, String> oemCustomDataMap = new HashMap<>();
            ArrayList<String> keys = result.getStringArrayList(EXTRA_OEM_DATA_KEYS);
            ArrayList<String> values = result.getStringArrayList(EXTRA_OEM_DATA_VALUES);
            for (int i = 0; i < keys.size(); i++) {
                oemCustomDataMap.put(keys.get(i), values.get(i));
            }
            future.complete(oemCustomDataMap);
        } else {
            Log.v(TAG, "No data received for getOemCustomData()");
            future.complete(new HashMap<>());
        }
    }
}
