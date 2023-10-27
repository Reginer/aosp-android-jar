// Copyright 2015 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package android.net.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Basic information about a response. Included in {@link UrlRequest.Callback} callbacks. Each
 * {@link UrlRequest.Callback#onRedirectReceived onRedirectReceived()} callback gets a different
 * copy of {@code UrlResponseInfo} describing a particular redirect response.
 */
public abstract class UrlResponseInfo {

    /**
     * Returns the URL the response is for. This is the URL after following redirects, so it may not
     * be the originally requested URL.
     *
     * @return the URL the response is for.
     */
    @NonNull
    public abstract String getUrl();

    /**
     * Returns the URL chain. The first entry is the originally requested URL; the following entries
     * are redirects followed.
     *
     * @return the URL chain.
     */
    @NonNull
    public abstract List<String> getUrlChain();

    /**
     * Returns the HTTP status code. When a resource is retrieved from the cache, whether it was
     * revalidated or not, the original status code is returned.
     *
     * @return the HTTP status code.
     */
    public abstract int getHttpStatusCode();

    /**
     * Returns the HTTP status text of the status line. For example, if the request received a
     * "HTTP/1.1 200 OK" response, this method returns "OK".
     *
     * @return the HTTP status text of the status line.
     */
    @NonNull
    public abstract String getHttpStatusText();

    /**
     * Returns the response headers.
     */
    @NonNull
    public abstract HeaderBlock getHeaders();

    /**
     * Returns {@code true} if the response came from the cache, including requests that were
     * revalidated over the network before being retrieved from the cache.
     *
     * @return {@code true} if the response came from the cache, {@code false} otherwise.
     */
    public abstract boolean wasCached();

    /**
     * Returns the protocol (for example 'quic/1+spdy/3') negotiated with the server. Returns an
     * empty string if no protocol was negotiated, the protocol is not known, or when using plain
     * HTTP or HTTPS.
     *
     * @return the protocol negotiated with the server.
     */
    // TODO(mef): Figure out what this returns in the cached case, both with
    // and without a revalidation request.
    @NonNull
    public abstract String getNegotiatedProtocol();

    /**
     * Returns the proxy server that was used for the request.
     *
     * @return the proxy server that was used for the request.
     *
     * @hide
     */
    @Nullable
    public String getProxyServer(){
        return null;
    };

    /**
     * Returns a minimum count of bytes received from the network to process this request. This
     * count may ignore certain overheads (for example IP and TCP/UDP framing, SSL handshake and
     * framing, proxy handling). This count is taken prior to decompression (for example GZIP) and
     * includes headers and data from all redirects.
     *
     * This value may change (even for one {@link UrlResponseInfo} instance) as the request
     * progresses until completion, when {@link UrlRequest.Callback#onSucceeded onSucceeded()},
     * {@link UrlRequest.Callback#onFailed onFailed()}, or {@link UrlRequest.Callback#onCanceled
     * onCanceled()} is called.
     *
     * @return a minimum count of bytes received from the network to process this request.
     */
    public abstract long getReceivedByteCount();
}
