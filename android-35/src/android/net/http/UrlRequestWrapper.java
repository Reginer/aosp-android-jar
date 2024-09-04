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

import java.nio.ByteBuffer;

class UrlRequestWrapper extends android.net.http.UrlRequest {

    private final org.chromium.net.UrlRequest backend;

    public UrlRequestWrapper(org.chromium.net.UrlRequest backend) {
        this.backend = backend;
    }

    @Override
    public void start() {
        backend.start();
    }

    @Override
    public void followRedirect() {
        backend.followRedirect();
    }

    @Override
    public void read(ByteBuffer buffer) {
        backend.read(buffer);
    }

    @Override
    public void cancel() {
        backend.cancel();
    }

    @Override
    public boolean isDone() {
        return backend.isDone();
    }

    @Override
    public void getStatus(android.net.http.UrlRequest.StatusListener listener) {
        backend.getStatus(new UrlRequestStatusListenerWrapper(listener));
    }

    @Override
    public String getHttpMethod() {
        return backend.getHttpMethod();
    }

    @Override
    public android.net.http.HeaderBlock getHeaders() {
        org.chromium.net.UrlResponseInfo.HeaderBlock headers = backend.getHeaders();
        return new HeaderBlockWrapper(headers);
    }

    @Override
    public boolean isCacheDisabled() {
        return backend.isCacheDisabled();
    }

    @Override
    public boolean isDirectExecutorAllowed() {
        return backend.isDirectExecutorAllowed();
    }

    @Override
    public int getPriority() {
        return backend.getPriority();
    }

    @Override
    public boolean hasTrafficStatsTag() {
        return backend.hasTrafficStatsTag();
    }

    @Override
    public int getTrafficStatsTag() {
        return backend.getTrafficStatsTag();
    }

    @Override
    public boolean hasTrafficStatsUid() {
        return backend.hasTrafficStatsUid();
    }

    @Override
    public int getTrafficStatsUid() {
        return backend.getTrafficStatsUid();
    }
}
