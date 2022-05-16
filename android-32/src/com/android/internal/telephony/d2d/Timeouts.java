/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.internal.telephony.d2d;

import android.content.ContentResolver;
import android.provider.Settings;

/**
 * Timeouts and configuration parameters related to the device-to-device communication code.
 */
public final class Timeouts {

    public static class Adapter {
        private final ContentResolver mContentResolver;

        public Adapter(ContentResolver cr) {
            mContentResolver = cr;
        }

        public long getRtpMessageAckDurationMillis()  {
            return Timeouts.getRtpMessageAckDurationMillis(mContentResolver);
        }

        /**
         * The minimum interval between DTMF digits.
         * @return minimum interval in millis.
         */
        public long getDtmfMinimumIntervalMillis() {
            return Timeouts.getDtmfMinimumIntervalMillis(mContentResolver);
        }

        public long getMaxDurationOfDtmfMessageMillis() {
            return Timeouts.getMaxDurationOfDtmfMessageMillis(mContentResolver);
        }

        public long getDtmfDurationFuzzMillis() {
            return Timeouts.getDtmfDurationFuzzMillis(mContentResolver);
        }

        public long getDtmfNegotiationTimeoutMillis() {
            return Timeouts.getDtmfNegotiationTimeoutMillis(mContentResolver);
        }
    }

    /** A prefix to use for all keys so to not clobber the global namespace. */
    private static final String PREFIX = "telephony.d2d.";

    /**
     * Returns the timeout value from Settings or the default value if it hasn't been changed. This
     * method is safe to call from any thread, including the UI thread.
     *
     * @param contentResolver The content resolved.
     * @param key             Settings key to retrieve.
     * @param defaultValue    Default value, in milliseconds.
     * @return The timeout value from Settings or the default value if it hasn't been changed.
     */
    private static long get(ContentResolver contentResolver, String key, long defaultValue) {
        return Settings.Secure.getLong(contentResolver, PREFIX + key, defaultValue);
    }

    /**
     * Determines the length of time to wait for acknowledgement of an RTP header extension before
     * retrying a message send.
     * @param cr
     * @return
     */
    public static long getRtpMessageAckDurationMillis(ContentResolver cr) {
        return get(cr, "rtp_message_ack_duration_millis", 1000L);
    }

    /**
     * Determines the minimum duration between DTMF digits.  Digits are sent with this much spacing
     * between them.
     * @param cr
     * @return
     */
    public static long getDtmfMinimumIntervalMillis(ContentResolver cr) {
        return get(cr, "dtmf_minimum_interval_millis", 100L);
    }

    /**
     * Determines the maximum amount of time to wait for a single DTMF sequence.
     * @param cr
     * @return
     */
    public static long getMaxDurationOfDtmfMessageMillis(ContentResolver cr) {
        return get(cr, "dtmf_max_message_duration_millis", 1000L);
    }

    /**
     * Determines the maximum amount of time to wait for negotiation of the DTMF protocol.
     * @param cr
     * @return
     */
    public static long getDtmfNegotiationTimeoutMillis(ContentResolver cr) {
        return get(cr, "dtmf_negotiation_timeout_millis", 3000L);
    }

    /**
     * A random amount of time up to this amount will be added to
     * {@link #getMaxDurationOfDtmfMessageMillis(ContentResolver)} when determining how long to
     * wait before sending a DTMF message.  This fuzz factor is used to account for timing
     * discrepancies between devices.
     * @param cr
     * @return
     */
    public static long getDtmfDurationFuzzMillis(ContentResolver cr) {
        return get(cr, "dtmf_duration_fuzz_millis", 10L);
    }

}
