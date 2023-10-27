// Copyright 2016 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.net.impl;

import android.net.http.BidirectionalStream;
import android.net.http.HeaderBlock;
import android.net.http.HttpException;
import android.net.http.NetworkQualityRttListener;
import android.net.http.NetworkQualityThroughputListener;
import android.net.http.RequestFinishedInfo;
import android.net.http.UploadDataProvider;
import android.net.http.UploadDataSink;
import android.net.http.UrlRequest;
import android.net.http.UrlResponseInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.Executor;

/**
 * This class contains wrapper classes for all Cronet API callback/listener classes. These classes
 * only permit callbacks that the version of the client API is known to support.  For example, if
 * version 2 of the API adds a callback onFoo() but the client API this class is implementing is
 * version 1, these wrapper classes should not call {@code mWrappedCallback.onFoo()} and should
 * instead silently drop the callback.
 *
 * When adding any callback wrapping here, be sure you add the proper version check.  Only callbacks
 * supported in all versions of the API should forgo a version check.
 */
public class VersionSafeCallbacks {
    /**
     * Wrap a {@link UrlRequest.Callback} in a version safe manner.
     */
    public static final class UrlRequestCallback implements UrlRequest.Callback {
        private final UrlRequest.Callback mWrappedCallback;

        public UrlRequestCallback(UrlRequest.Callback callback) {
            mWrappedCallback = callback;
        }

        @Override
        public void onRedirectReceived(
                UrlRequest request, UrlResponseInfo info, String newLocationUrl) throws Exception {
            mWrappedCallback.onRedirectReceived(request, info, newLocationUrl);
        }

        @Override
        public void onResponseStarted(UrlRequest request, UrlResponseInfo info) throws Exception {
            mWrappedCallback.onResponseStarted(request, info);
        }

        @Override
        public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer)
                throws Exception {
            mWrappedCallback.onReadCompleted(request, info, byteBuffer);
        }

        @Override
        public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
            mWrappedCallback.onSucceeded(request, info);
        }

        @Override
        public void onFailed(UrlRequest request, UrlResponseInfo info, HttpException error) {
            mWrappedCallback.onFailed(request, info, error);
        }

        @Override
        public void onCanceled(UrlRequest request, UrlResponseInfo info) {
            mWrappedCallback.onCanceled(request, info);
        }
    }

    /**
     * Wrap a {@link UrlRequest.StatusListener} in a version safe manner.
     */
    public static final class UrlRequestStatusListener implements UrlRequest.StatusListener {
        private final UrlRequest.StatusListener mWrappedListener;

        public UrlRequestStatusListener(UrlRequest.StatusListener listener) {
            mWrappedListener = listener;
        }

        @Override
        public void onStatus(int status) {
            mWrappedListener.onStatus(status);
        }
    }

    /**
     * Wrap a {@link BidirectionalStream.Callback} in a version safe manner.
     */
    public static final class BidirectionalStreamCallback implements BidirectionalStream.Callback {
        private final BidirectionalStream.Callback mWrappedCallback;

        public BidirectionalStreamCallback(BidirectionalStream.Callback callback) {
            mWrappedCallback = callback;
        }

        @Override
        public void onStreamReady(BidirectionalStream stream) {
            mWrappedCallback.onStreamReady(stream);
        }

        @Override
        public void onResponseHeadersReceived(BidirectionalStream stream, UrlResponseInfo info) {
            mWrappedCallback.onResponseHeadersReceived(stream, info);
        }

        @Override
        public void onReadCompleted(BidirectionalStream stream, UrlResponseInfo info,
                ByteBuffer buffer, boolean endOfStream) {
            mWrappedCallback.onReadCompleted(stream, info, buffer, endOfStream);
        }

        @Override
        public void onWriteCompleted(BidirectionalStream stream, UrlResponseInfo info,
                ByteBuffer buffer, boolean endOfStream) {
            mWrappedCallback.onWriteCompleted(stream, info, buffer, endOfStream);
        }

        @Override
        public void onResponseTrailersReceived(BidirectionalStream stream, UrlResponseInfo info,
                HeaderBlock trailers) {
            mWrappedCallback.onResponseTrailersReceived(stream, info, trailers);
        }

        @Override
        public void onSucceeded(BidirectionalStream stream, UrlResponseInfo info) {
            mWrappedCallback.onSucceeded(stream, info);
        }

        @Override
        public void onFailed(
                BidirectionalStream stream, UrlResponseInfo info, HttpException error) {
            mWrappedCallback.onFailed(stream, info, error);
        }

        @Override
        public void onCanceled(BidirectionalStream stream, UrlResponseInfo info) {
            mWrappedCallback.onCanceled(stream, info);
        }
    }

    /**
     * Wrap a {@link UploadDataProvider} in a version safe manner.
     */
    public static final class UploadDataProviderWrapper extends UploadDataProvider {
        private final UploadDataProvider mWrappedProvider;

        public UploadDataProviderWrapper(UploadDataProvider provider) {
            mWrappedProvider = provider;
        }

        @Override
        public long getLength() throws IOException {
            return mWrappedProvider.getLength();
        }

        @Override
        public void read(UploadDataSink uploadDataSink, ByteBuffer byteBuffer) throws IOException {
            mWrappedProvider.read(uploadDataSink, byteBuffer);
        }

        @Override
        public void rewind(UploadDataSink uploadDataSink) throws IOException {
            mWrappedProvider.rewind(uploadDataSink);
        }

        @Override
        public void close() throws IOException {
            mWrappedProvider.close();
        }
    }

    /**
     * Wrap a {@link RequestFinishedInfo.Listener} in a version safe manner.
     */
    public static final class RequestFinishedInfoListener extends RequestFinishedInfo.Listener {
        private final RequestFinishedInfo.Listener mWrappedListener;

        public RequestFinishedInfoListener(RequestFinishedInfo.Listener listener) {
            super(listener.getExecutor());
            mWrappedListener = listener;
        }

        @Override
        public void onRequestFinished(RequestFinishedInfo requestInfo) {
            mWrappedListener.onRequestFinished(requestInfo);
        }

        @Override
        public Executor getExecutor() {
            return mWrappedListener.getExecutor();
        }
    }

    /**
     * Wrap a {@link NetworkQualityRttListener} in a version safe manner.
     * NOTE(pauljensen): Delegates equals() and hashCode() to wrapped listener to
     * facilitate looking up by wrapped listener in an ArrayList.indexOf().
     */
    public static final class NetworkQualityRttListenerWrapper extends NetworkQualityRttListener {
        private final NetworkQualityRttListener mWrappedListener;

        public NetworkQualityRttListenerWrapper(NetworkQualityRttListener listener) {
            super(listener.getExecutor());
            mWrappedListener = listener;
        }

        @Override
        public void onRttObservation(int rttMs, Instant observationInstant, int source) {
            mWrappedListener.onRttObservation(rttMs, observationInstant, source);
        }

        @Override
        public Executor getExecutor() {
            return mWrappedListener.getExecutor();
        }

        @Override
        public int hashCode() {
            return mWrappedListener.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof NetworkQualityRttListenerWrapper)) {
                return false;
            }
            return mWrappedListener.equals(((NetworkQualityRttListenerWrapper) o).mWrappedListener);
        }
    }

    /**
     * Wrap a {@link NetworkQualityThroughputListener} in a version safe manner.
     * NOTE(pauljensen): Delegates equals() and hashCode() to wrapped listener to
     * facilitate looking up by wrapped listener in an ArrayList.indexOf().
     */
    public static final class NetworkQualityThroughputListenerWrapper
            extends NetworkQualityThroughputListener {
        private final NetworkQualityThroughputListener mWrappedListener;

        public NetworkQualityThroughputListenerWrapper(NetworkQualityThroughputListener listener) {
            super(listener.getExecutor());
            mWrappedListener = listener;
        }

        @Override
        public void onThroughputObservation(int throughputKbps, Instant observationInstant, int source) {
            mWrappedListener.onThroughputObservation(throughputKbps, observationInstant, source);
        }

        @Override
        public Executor getExecutor() {
            return mWrappedListener.getExecutor();
        }

        @Override
        public int hashCode() {
            return mWrappedListener.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof NetworkQualityThroughputListenerWrapper)) {
                return false;
            }
            return mWrappedListener.equals(
                    ((NetworkQualityThroughputListenerWrapper) o).mWrappedListener);
        }
    }
}
