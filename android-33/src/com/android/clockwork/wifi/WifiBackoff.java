package com.android.clockwork.wifi;

import com.android.internal.util.IndentingPrintWriter;

/**
 * Basic interface for Wifi Backoff.
 *
 * To enter Wifi Backoff, it must first be scheduled via a call to scheduleBackoff.
 * Once it has been scheduled, an implementation-specific deadline must pass before
 * Wifi Backoff becomes active.
 *
 * At any point, Wifi Backoff may be canceled. This will either take Wifi Backoff out of its
 * active state, or it will cancel any scheduled Wifi Backoff deadlines.
 */
public interface WifiBackoff {

    interface Listener {
        /**
         * Callback when Wifi Backoff becomes active or inactive.
         */
        void onWifiBackoffChanged();
    }

    void setListener(Listener listener);

    /**
     * Returns whether Wifi Backoff is active.
     *
     * Wifi Backoff is active when a previously-scheduled backoff reaches its deadline.
     *
     * @return true if wifi backoff is active; false otherwise
     */
    boolean isInBackoff();

    /**
     * Schedule to enter backoff after some deadline. The deadline is implementation-specific.
     *
     * This method must be idempotent.
     */
    void scheduleBackoff();

    /**
     * Exit Wifi Backoff if Wifi Backoff is active, or cancel any scheduled backoffs.
     *
     * This method must be idempotent.
     */
    void cancelBackoff();

    void dump(IndentingPrintWriter ipw);
}
