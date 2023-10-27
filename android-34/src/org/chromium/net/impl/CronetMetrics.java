// Copyright 2016 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.net.impl;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import android.net.http.RequestFinishedInfo;

import java.time.Instant;

/**
 * Implementation of {@link RequestFinishedInfo.Metrics}.
 */
@VisibleForTesting
public final class CronetMetrics extends RequestFinishedInfo.Metrics {
    private final long mRequestStartMs;
    private final long mDnsStartMs;
    private final long mDnsEndMs;
    private final long mConnectStartMs;
    private final long mConnectEndMs;
    private final long mSslStartMs;
    private final long mSslEndMs;
    private final long mSendingStartMs;
    private final long mSendingEndMs;
    private final long mPushStartMs;
    private final long mPushEndMs;
    private final long mResponseStartMs;
    private final long mRequestEndMs;
    private final boolean mSocketReused;

    @Nullable
    private final Long mSentByteCount;
    @Nullable
    private final Long mReceivedByteCount;

    @Nullable
    private static Instant toInstant(long timestamp) {
        if (timestamp != -1) {
            return Instant.ofEpochMilli(timestamp);
        }
        return null;
    }

    private static boolean checkOrder(long start, long end) {
        // If end doesn't exist, start can be anything, including also not existing
        // If end exists, start must also exist and be before end
        return (end >= start && start != -1) || end == -1;
    }

    /**
     * New-style constructor
     */
    public CronetMetrics(long requestStartMs, long dnsStartMs, long dnsEndMs, long connectStartMs,
            long connectEndMs, long sslStartMs, long sslEndMs, long sendingStartMs,
            long sendingEndMs, long pushStartMs, long pushEndMs, long responseStartMs,
            long requestEndMs, boolean socketReused, long sentByteCount, long receivedByteCount) {
        // Check that no end times are before corresponding start times,
        // or exist when start time doesn't.
        assert checkOrder(dnsStartMs, dnsEndMs);
        assert checkOrder(connectStartMs, connectEndMs);
        assert checkOrder(sslStartMs, sslEndMs);
        assert checkOrder(sendingStartMs, sendingEndMs);
        assert checkOrder(pushStartMs, pushEndMs);
        // requestEnd always exists, so just check that it's after start
        assert requestEndMs >= responseStartMs;
        // Spot-check some of the other orderings
        assert dnsStartMs >= requestStartMs || dnsStartMs == -1;
        assert sendingStartMs >= requestStartMs || sendingStartMs == -1;
        assert sslStartMs >= connectStartMs || sslStartMs == -1;
        assert responseStartMs >= sendingStartMs || responseStartMs == -1;
        mRequestStartMs = requestStartMs;
        mDnsStartMs = dnsStartMs;
        mDnsEndMs = dnsEndMs;
        mConnectStartMs = connectStartMs;
        mConnectEndMs = connectEndMs;
        mSslStartMs = sslStartMs;
        mSslEndMs = sslEndMs;
        mSendingStartMs = sendingStartMs;
        mSendingEndMs = sendingEndMs;
        mPushStartMs = pushStartMs;
        mPushEndMs = pushEndMs;
        mResponseStartMs = responseStartMs;
        mRequestEndMs = requestEndMs;
        mSocketReused = socketReused;
        mSentByteCount = sentByteCount;
        mReceivedByteCount = receivedByteCount;
    }

    @Nullable
    @Override
    public Instant getRequestStart() {
        return toInstant(mRequestStartMs);
    }

    @Nullable
    @Override
    public Instant getDnsStart() {
        return toInstant(mDnsStartMs);
    }

    @Nullable
    @Override
    public Instant getDnsEnd() {
        return toInstant(mDnsEndMs);
    }

    @Nullable
    @Override
    public Instant getConnectStart() {
        return toInstant(mConnectStartMs);
    }

    @Nullable
    @Override
    public Instant getConnectEnd() {
        return toInstant(mConnectEndMs);
    }

    @Nullable
    @Override
    public Instant getSslStart() {
        return toInstant(mSslStartMs);
    }

    @Nullable
    @Override
    public Instant getSslEnd() {
        return toInstant(mSslEndMs);
    }

    @Nullable
    @Override
    public Instant getSendingStart() {
        return toInstant(mSendingStartMs);
    }

    @Nullable
    @Override
    public Instant getSendingEnd() {
        return toInstant(mSendingEndMs);
    }

    @Nullable
    @Override
    public Instant getPushStart() {
        return toInstant(mPushStartMs);
    }

    @Nullable
    @Override
    public Instant getPushEnd() {
        return toInstant(mPushEndMs);
    }

    @Nullable
    @Override
    public Instant getResponseStart() {
        return toInstant(mResponseStartMs);
    }

    @Nullable
    @Override
    public Instant getRequestEnd() {
        return toInstant(mRequestEndMs);
    }

    @Override
    public boolean getSocketReused() {
        return mSocketReused;
    }

    @Nullable
    @Override
    public Long getSentByteCount() {
        return mSentByteCount;
    }

    @Nullable
    @Override
    public Long getReceivedByteCount() {
        return mReceivedByteCount;
    }
}
