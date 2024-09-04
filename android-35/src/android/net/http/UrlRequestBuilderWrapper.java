/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.net.http;

import android.net.Network;

import org.chromium.net.ExperimentalCronetEngine;

import java.util.concurrent.Executor;

public class UrlRequestBuilderWrapper extends android.net.http.UrlRequest.Builder {

    private final org.chromium.net.ExperimentalUrlRequest.Builder backend;

    public UrlRequestBuilderWrapper(org.chromium.net.ExperimentalUrlRequest.Builder backend) {
        this.backend = backend;
    }

    @Override
    public android.net.http.UrlRequest.Builder setHttpMethod(String method) {
        backend.setHttpMethod(method);
        return this;
    }

    @Override
    public android.net.http.UrlRequest.Builder addHeader(String header, String value) {
        backend.addHeader(header, value);
        return this;
    }

    @Override
    public android.net.http.UrlRequest.Builder setCacheDisabled(boolean disableCache) {
        backend.disableCache();
        return this;
    }

    @Override
    public android.net.http.UrlRequest.Builder setPriority(int priority) {
        backend.setPriority(priority);
        return this;
    }

    @Override
    public android.net.http.UrlRequest.Builder setUploadDataProvider(
            android.net.http.UploadDataProvider provider, Executor executor) {
        UploadDataProviderWrapper wrappedProvider = new UploadDataProviderWrapper(provider);
        backend.setUploadDataProvider(wrappedProvider, executor);
        return this;
    }

    @Override
    public android.net.http.UrlRequest.Builder setDirectExecutorAllowed(
            boolean allowDirectExecutor) {
        backend.allowDirectExecutor();
        return this;
    }

    @Override
    public android.net.http.UrlRequest.Builder bindToNetwork(Network network) {
        long networkHandle = ExperimentalCronetEngine.UNBIND_NETWORK_HANDLE;
        if (network != null) {
            networkHandle = network.getNetworkHandle();
        }
        backend.bindToNetwork(networkHandle);
        return this;
    }

    @Override
    public android.net.http.UrlRequest.Builder setTrafficStatsUid(int uid) {
        backend.setTrafficStatsUid(uid);
        return this;
    }

    @Override
    public android.net.http.UrlRequest.Builder setTrafficStatsTag(int tag) {
        backend.setTrafficStatsTag(tag);
        return this;
    }

    @Override
    public android.net.http.UrlRequest build() {
        return new UrlRequestWrapper(backend.build());
    }
}
