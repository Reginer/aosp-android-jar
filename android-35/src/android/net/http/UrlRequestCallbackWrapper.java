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

@SuppressWarnings("Override")
class UrlRequestCallbackWrapper extends org.chromium.net.UrlRequest.Callback {

    private final android.net.http.UrlRequest.Callback backend;

    public UrlRequestCallbackWrapper(android.net.http.UrlRequest.Callback backend) {
        this.backend = backend;
    }

    @Override
    public void onRedirectReceived(
            org.chromium.net.UrlRequest request,
            org.chromium.net.UrlResponseInfo info,
            String newLocationUrl)
            throws Exception {
        UrlRequestWrapper wrappedRequest = new UrlRequestWrapper(request);
        UrlResponseInfoWrapper wrappedInfo = new UrlResponseInfoWrapper(info);
        backend.onRedirectReceived(wrappedRequest, wrappedInfo, newLocationUrl);
    }

    @Override
    public void onResponseStarted(
            org.chromium.net.UrlRequest request, org.chromium.net.UrlResponseInfo info)
            throws Exception {
        UrlRequestWrapper wrappedRequest = new UrlRequestWrapper(request);
        UrlResponseInfoWrapper wrappedInfo = new UrlResponseInfoWrapper(info);
        backend.onResponseStarted(wrappedRequest, wrappedInfo);
    }

    @Override
    public void onReadCompleted(
            org.chromium.net.UrlRequest request,
            org.chromium.net.UrlResponseInfo info,
            ByteBuffer buffer)
            throws Exception {
        UrlRequestWrapper wrappedRequest = new UrlRequestWrapper(request);
        UrlResponseInfoWrapper wrappedInfo = new UrlResponseInfoWrapper(info);
        backend.onReadCompleted(wrappedRequest, wrappedInfo, buffer);
    }

    @Override
    public void onSucceeded(
            org.chromium.net.UrlRequest request, org.chromium.net.UrlResponseInfo info) {
        UrlRequestWrapper wrappedRequest = new UrlRequestWrapper(request);
        UrlResponseInfoWrapper wrappedInfo = new UrlResponseInfoWrapper(info);
        backend.onSucceeded(wrappedRequest, wrappedInfo);
    }

    @Override
    public void onFailed(
            org.chromium.net.UrlRequest request,
            org.chromium.net.UrlResponseInfo info,
            CronetException e) {
        UrlRequestWrapper wrappedRequest = new UrlRequestWrapper(request);
        UrlResponseInfoWrapper wrappedInfo = new UrlResponseInfoWrapper(info);
        HttpException translatedException = CronetExceptionTranslationUtils.translateException(e);
        backend.onFailed(wrappedRequest, wrappedInfo, translatedException);
    }

    @Override
    public void onCanceled(
            org.chromium.net.UrlRequest request, org.chromium.net.UrlResponseInfo info) {
        UrlRequestWrapper wrappedRequest = new UrlRequestWrapper(request);
        UrlResponseInfoWrapper wrappedInfo = new UrlResponseInfoWrapper(info);
        backend.onCanceled(wrappedRequest, wrappedInfo);
    }
}
