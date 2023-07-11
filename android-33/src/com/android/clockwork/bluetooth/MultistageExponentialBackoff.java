package com.android.clockwork.bluetooth;

/**
 * A multistage backoff mechanism.
 *
 * Stage 1: Base period with a constant-time interval for an initial set of retries
 * Stage 2: Exponential period with exponentially increasing retry intervals
 * Stage 3: Max backoff period with constant-time retries at the max backoff interval
 *
 * This is best illustrated with an example:
 *   Given a base interval of 2, a base period of 5, and a max interval of 300,
 *   the backoff pattern will look like so:
 *       2, 2, 2, 2, 2, 4, 8, 16, 32, 64, 128, 256, 300, 300, 300, 300, 300 ...
 *
 * This class is purposefully written without time units.
 */
public class MultistageExponentialBackoff {

    private final int baseInterval;
    private final int basePeriod;
    private final int maxInterval;

    private int currentAttempt;

    public MultistageExponentialBackoff(int baseInterval, int basePeriod, int maxInterval) {
        this.baseInterval = baseInterval;
        this.basePeriod = basePeriod;
        this.maxInterval = maxInterval;
        reset();
    }

    public int getNextBackoff() {
        if (currentAttempt <= basePeriod) {
            currentAttempt++;
            return baseInterval;
        }
        int nextInterval = (int) Math.pow(baseInterval, currentAttempt - basePeriod + 1);
        currentAttempt++;
        return nextInterval < maxInterval ? nextInterval : maxInterval;
    }

    public void reset() {
        currentAttempt = 1;
    }
}
