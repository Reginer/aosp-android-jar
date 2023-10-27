// Copyright 2015 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package android.net.http;

import java.time.Instant;
import java.util.concurrent.Executor;

/**
 * Watches observations of various round trip times (RTTs) at various layers of the network stack.
 * These include RTT estimates by QUIC and TCP, as well as the time between when a URL request is
 * sent and when the first byte of the response is received.
 *
 * @hide
 */
public abstract class NetworkQualityRttListener {
    /**
     * The executor on which this listener will be notified. Set as a final field, so it can be
     * safely accessed across threads.
     */
    private final Executor mExecutor;

    /**
     * @param executor The executor on which the observations are reported.
     */
    public NetworkQualityRttListener(Executor executor) {
        if (executor == null) {
            throw new IllegalStateException("Executor must not be null");
        }
        mExecutor = executor;
    }

    public Executor getExecutor() {
        return mExecutor;
    }

    /**
     * Reports a new round trip time observation.
     *
     * @param rttMs the round trip time in milliseconds.
     * @param observationInstant when the observation was recorded
     * @param source the observation source from {@link NetworkQualityObservationSource}.
     */
    public abstract void onRttObservation(int rttMs, Instant observationInstant, int source);
}
