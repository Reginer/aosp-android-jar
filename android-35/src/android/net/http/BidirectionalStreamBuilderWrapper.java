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

public class BidirectionalStreamBuilderWrapper
        extends android.net.http.BidirectionalStream.Builder {

    private final org.chromium.net.ExperimentalBidirectionalStream.Builder backend;

    public BidirectionalStreamBuilderWrapper(
            org.chromium.net.ExperimentalBidirectionalStream.Builder backend) {
        this.backend = backend;
    }

    @Override
    public android.net.http.BidirectionalStream.Builder setHttpMethod(String method) {
        backend.setHttpMethod(method);
        return this;
    }

    @Override
    public android.net.http.BidirectionalStream.Builder addHeader(String header, String value) {
        backend.addHeader(header, value);
        return this;
    }

    @Override
    public android.net.http.BidirectionalStream.Builder setPriority(int priority) {
        backend.setPriority(priority);
        return this;
    }

    @Override
    public android.net.http.BidirectionalStream.Builder
            setDelayRequestHeadersUntilFirstFlushEnabled(
                    boolean delayRequestHeadersUntilFirstFlush) {
        backend.delayRequestHeadersUntilFirstFlush(delayRequestHeadersUntilFirstFlush);
        return this;
    }

    @Override
    public android.net.http.BidirectionalStream build() {
        return new BidirectionalStreamWrapper(backend.build());
    }

    @Override
    public android.net.http.BidirectionalStream.Builder setTrafficStatsTag(int tag) {
        backend.setTrafficStatsTag(tag);
        return this;
    }

    @Override
    public android.net.http.BidirectionalStream.Builder setTrafficStatsUid(int uid) {
        backend.setTrafficStatsUid(uid);
        return this;
    }
}
