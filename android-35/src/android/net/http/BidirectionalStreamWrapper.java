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

public class BidirectionalStreamWrapper extends android.net.http.BidirectionalStream {

    private final org.chromium.net.BidirectionalStream backend;

    BidirectionalStreamWrapper(org.chromium.net.BidirectionalStream backend) {
        this.backend = backend;
    }

    @Override
    public void start() {
        backend.start();
    }

    @Override
    public void read(ByteBuffer buffer) {
        backend.read(buffer);
    }

    @Override
    public void write(ByteBuffer buffer, boolean endOfStream) {
        backend.write(buffer, endOfStream);
    }

    @Override
    public void flush() {
        backend.flush();
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
    public boolean isDelayRequestHeadersUntilFirstFlushEnabled() {
        return backend.isDelayRequestHeadersUntilFirstFlushEnabled();
    }

    @Override
    public int getPriority() {
        return backend.getPriority();
    }

    @Override
    public String getHttpMethod() {
        return backend.getHttpMethod();
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

    @Override
    public android.net.http.HeaderBlock getHeaders() {
        return new HeaderBlockWrapper(backend.getHeaders());
    }
}
