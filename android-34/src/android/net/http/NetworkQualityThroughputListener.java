// Copyright 2015 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package android.net.http;

import java.time.Instant;
import java.util.concurrent.Executor;

/**
 * Listener that is notified of throughput observations from the network quality estimator.
 *
 * {@hide}
 */
public abstract class NetworkQualityThroughputListener {
    /**
     * The executor on which this listener will be notified. Set as a final field, so it can be
     * safely accessed across threads.
     */
    private final Executor mExecutor;

    /**
     * @param executor The executor on which the observations are reported.
     */
    public NetworkQualityThroughputListener(Executor executor) {
        if (executor == null) {
            throw new IllegalStateException("Executor must not be null");
        }
        mExecutor = executor;
    }

    public Executor getExecutor() {
        return mExecutor;
    }

    /**
     * Reports a new throughput observation.
     *
     * @param throughputKbps the downstream throughput in kilobits per second.
     * @param observationInstant when the observation was recorded
     * @param source the observation source from {@link NetworkQualityObservationSource}.
     */
    public abstract void onThroughputObservation(int throughputKbps, Instant observationInstant, int source);
}
