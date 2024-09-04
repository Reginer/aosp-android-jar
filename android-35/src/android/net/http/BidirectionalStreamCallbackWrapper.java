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

import org.chromium.net.CronetException;

import java.nio.ByteBuffer;

public class BidirectionalStreamCallbackWrapper
        extends org.chromium.net.BidirectionalStream.Callback {

    private final android.net.http.BidirectionalStream.Callback backend;

    public BidirectionalStreamCallbackWrapper(
            android.net.http.BidirectionalStream.Callback backend) {
        this.backend = backend;
    }

    @Override
    public void onStreamReady(org.chromium.net.BidirectionalStream stream) {
        BidirectionalStreamWrapper wrappedStream = new BidirectionalStreamWrapper(stream);
        backend.onStreamReady(wrappedStream);
    }

    @Override
    public void onResponseHeadersReceived(org.chromium.net.BidirectionalStream stream, org.chromium.net.UrlResponseInfo info) {
        BidirectionalStreamWrapper wrappedStream = new BidirectionalStreamWrapper(stream);
        UrlResponseInfoWrapper wrappedInfo = new UrlResponseInfoWrapper(info);
        backend.onResponseHeadersReceived(wrappedStream, wrappedInfo);
    }

    @Override
    public void onReadCompleted(
            org.chromium.net.BidirectionalStream stream,
            org.chromium.net.UrlResponseInfo info,
            ByteBuffer byteBuffer,
            boolean endOfStream) {
        BidirectionalStreamWrapper wrappedStream = new BidirectionalStreamWrapper(stream);
        UrlResponseInfoWrapper wrappedInfo = new UrlResponseInfoWrapper(info);
        backend.onReadCompleted(wrappedStream, wrappedInfo, byteBuffer, endOfStream);
    }

    @Override
    public void onWriteCompleted(
            org.chromium.net.BidirectionalStream stream,
            org.chromium.net.UrlResponseInfo info,
            ByteBuffer byteBuffer,
            boolean endOfStream) {
        BidirectionalStreamWrapper wrappedStream = new BidirectionalStreamWrapper(stream);
        UrlResponseInfoWrapper wrappedInfo = new UrlResponseInfoWrapper(info);
        backend.onWriteCompleted(wrappedStream, wrappedInfo, byteBuffer, endOfStream);
    }

    @Override
    public void onResponseTrailersReceived(
            org.chromium.net.BidirectionalStream stream,
            org.chromium.net.UrlResponseInfo info,
            org.chromium.net.UrlResponseInfo.HeaderBlock headers) {
        BidirectionalStreamWrapper wrappedStream = new BidirectionalStreamWrapper(stream);
        UrlResponseInfoWrapper wrappedInfo = new UrlResponseInfoWrapper(info);
        HeaderBlockWrapper wrappedHeaders = new HeaderBlockWrapper(headers);
        backend.onResponseTrailersReceived(wrappedStream, wrappedInfo, wrappedHeaders);
    }

    @Override
    public void onSucceeded(org.chromium.net.BidirectionalStream stream, org.chromium.net.UrlResponseInfo info) {
        BidirectionalStreamWrapper wrappedStream = new BidirectionalStreamWrapper(stream);
        UrlResponseInfoWrapper wrappedInfo = new UrlResponseInfoWrapper(info);
        backend.onSucceeded(wrappedStream, wrappedInfo);
    }

    @Override
    public void onFailed(org.chromium.net.BidirectionalStream stream, org.chromium.net.UrlResponseInfo info, CronetException e) {
        BidirectionalStreamWrapper wrappedStream = new BidirectionalStreamWrapper(stream);
        UrlResponseInfoWrapper wrappedInfo = new UrlResponseInfoWrapper(info);
        HttpException wrappedException = CronetExceptionTranslationUtils.translateException(e);
        backend.onFailed(wrappedStream, wrappedInfo, wrappedException);
    }

    @Override
    public void onCanceled(org.chromium.net.BidirectionalStream stream, org.chromium.net.UrlResponseInfo info) {
        BidirectionalStreamWrapper wrappedStream = new BidirectionalStreamWrapper(stream);
        UrlResponseInfoWrapper wrappedInfo = new UrlResponseInfoWrapper(info);
        backend.onCanceled(wrappedStream, wrappedInfo);
    }
}
