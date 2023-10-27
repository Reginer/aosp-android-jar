// Copyright 2016 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.net.impl;

import android.net.http.HeaderBlock;
import android.net.http.UrlRequest;
import android.net.http.UrlResponseInfo;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implements the container for basic information about a response. Included in
 * {@link UrlRequest.Callback} callbacks. Each
 * {@link UrlRequest.Callback#onRedirectReceived onRedirectReceived()}
 * callback gets a different copy of {@code UrlResponseInfo} describing a particular
 * redirect response.
 */
public final class UrlResponseInfoImpl extends UrlResponseInfo {
    private final List<String> mResponseInfoUrlChain;
    private final int mHttpStatusCode;
    private final String mHttpStatusText;
    private final boolean mWasCached;
    private final String mNegotiatedProtocol;
    private final String mProxyServer;
    private final AtomicLong mReceivedByteCount;
    private final HeaderBlockImpl mHeaders;

    /**
     * Creates an implementation of {@link UrlResponseInfo}.
     *
     * @param urlChain the URL chain. The first entry is the originally requested URL;
     *         the following entries are redirects followed.
     * @param httpStatusCode the HTTP status code.
     * @param httpStatusText the HTTP status text of the status line.
     * @param allHeadersList list of response header field and value pairs.
     * @param wasCached {@code true} if the response came from the cache, {@code false}
     *         otherwise.
     * @param negotiatedProtocol the protocol negotiated with the server.
     * @param proxyServer the proxy server that was used for the request.
     * @param receivedByteCount minimum count of bytes received from the network to process this
     *         request.
     */
    public UrlResponseInfoImpl(List<String> urlChain, int httpStatusCode, String httpStatusText,
            List<Map.Entry<String, String>> allHeadersList, boolean wasCached,
            String negotiatedProtocol, String proxyServer, long receivedByteCount) {
        mResponseInfoUrlChain = Collections.unmodifiableList(urlChain);
        mHttpStatusCode = httpStatusCode;
        mHttpStatusText = httpStatusText;
        mHeaders = new HeaderBlockImpl(Collections.unmodifiableList(allHeadersList));
        mWasCached = wasCached;
        mNegotiatedProtocol = negotiatedProtocol;
        mProxyServer = proxyServer;
        mReceivedByteCount = new AtomicLong(receivedByteCount);
    }

    /**
     * Constructor for backwards compatibility.  See main constructor above for more info.
     */
    @Deprecated
    public UrlResponseInfoImpl(List<String> urlChain, int httpStatusCode, String httpStatusText,
            List<Map.Entry<String, String>> allHeadersList, boolean wasCached,
            String negotiatedProtocol, String proxyServer) {
        this(urlChain, httpStatusCode, httpStatusText, allHeadersList, wasCached,
                negotiatedProtocol, proxyServer, 0);
    }

    @Override
    public String getUrl() {
        return mResponseInfoUrlChain.get(mResponseInfoUrlChain.size() - 1);
    }

    @Override
    public List<String> getUrlChain() {
        return mResponseInfoUrlChain;
    }

    @Override
    public int getHttpStatusCode() {
        return mHttpStatusCode;
    }

    @Override
    public String getHttpStatusText() {
        return mHttpStatusText;
    }

    @Override
    public HeaderBlock getHeaders() {
        return mHeaders;
    }

    @Override
    public boolean wasCached() {
        return mWasCached;
    }

    @Override
    public String getNegotiatedProtocol() {
        return mNegotiatedProtocol;
    }

    @Override
    public String getProxyServer() {
        return mProxyServer;
    }

    @Override
    public long getReceivedByteCount() {
        return mReceivedByteCount.get();
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "UrlResponseInfo@[%s][%s]: urlChain = %s, "
                        + "httpStatus = %d %s, headers = %s, wasCached = %b, "
                        + "negotiatedProtocol = %s, proxyServer= %s, receivedByteCount = %d",
                // Prevent asserting on the contents of this string
                Integer.toHexString(System.identityHashCode(this)), getUrl(),
                getUrlChain().toString(), getHttpStatusCode(), getHttpStatusText(),
                getHeaders().getAsList().toString(), wasCached(), getNegotiatedProtocol(),
                getProxyServer(), getReceivedByteCount());
    }

    /**
     * Sets mReceivedByteCount. Must not be called after request completion or cancellation.
     */
    public void setReceivedByteCount(long currentReceivedByteCount) {
        mReceivedByteCount.set(currentReceivedByteCount);
    }
}
