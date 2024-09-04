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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;
import java.util.concurrent.Executor;

class CronetEngineWrapper extends HttpEngine {

    private final ExperimentalCronetEngine backend;

    public CronetEngineWrapper(ExperimentalCronetEngine backend) {
        this.backend = backend;
    }

    @Override
    public void shutdown() {
        backend.shutdown();
    }

    @Override
    public URLConnection openConnection(URL url) throws IOException {
        return CronetExceptionTranslationUtils.executeTranslatingExceptions(
                () -> backend.openConnection(url));
    }

    @Override
    public URLStreamHandlerFactory createUrlStreamHandlerFactory() {
        return backend.createURLStreamHandlerFactory();
    }

    @Override
    public void bindToNetwork(Network network) {
        long networkHandle = backend.UNBIND_NETWORK_HANDLE;
        if (network != null) {
            networkHandle = network.getNetworkHandle();
        }
        backend.bindToNetwork(networkHandle);
    }

    @Override
    public android.net.http.BidirectionalStream.Builder newBidirectionalStreamBuilder(
            String url, Executor executor, android.net.http.BidirectionalStream.Callback callback) {
        BidirectionalStreamCallbackWrapper wrappedCallback =
                new BidirectionalStreamCallbackWrapper(callback);
        return new BidirectionalStreamBuilderWrapper(
                backend.newBidirectionalStreamBuilder(url, wrappedCallback, executor));
    }

    @Override
    public android.net.http.UrlRequest.Builder newUrlRequestBuilder(
            String url, Executor executor, android.net.http.UrlRequest.Callback callback) {
        UrlRequestCallbackWrapper wrappedCallback = new UrlRequestCallbackWrapper(callback);
        return new UrlRequestBuilderWrapper(
                backend.newUrlRequestBuilder(url, wrappedCallback, executor));
    }
}
