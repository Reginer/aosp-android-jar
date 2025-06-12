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

package android.net.ipsec.ike.ike3gpp;

import static com.android.internal.net.ipsec.ike.message.IkeNotifyPayload.ERROR_NOTIFY_TYPE_MAX;

import android.annotation.IntRange;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;

import com.android.internal.net.ipsec.ike.message.IkeNotifyPayload;

/**
 * Ike3gppBackoffTimer represents the data provided by the peer/remote endpoint for a BACKOFF_TIMER
 * Notify payload.
 *
 * @see 3GPP TS 24.302 Section 8.2.9.1 BACKOFF_TIMER Notify Payload
 * @hide
 */
@SystemApi
public final class Ike3gppBackoffTimer extends Ike3gppData {
    /**
     * Error-Notify indicating that access is not authorized because no subscription was found for
     * the specified APN.
     *
     * <p>NOTE: PRIVATE-USE VALUE; not IANA specified. This value MAY conflict with other private
     * use values from other extensions.
     *
     * <p>Corresponds to DIAMETER_ERROR_USER_NO_APN_SUBSCRIPTION Result code as specified in 3GPP TS
     * 29.273 Section 10.3.7
     *
     * @see 3GPP TS 24.302 Section 8.1.2.2
     * @deprecated This API is no longer used to distinguish back off timer support. As per TS24.302
     *     section 8.2.9.1, the BACK_OFF timer can be used with any ERROR_NOTIFY type.
     */
    @Deprecated public static final int ERROR_TYPE_NO_APN_SUBSCRIPTION = 9002;

    /**
     * Error-Notify indicating that the procedure could not be completed due to network failure.
     *
     * <p>NOTE: PRIVATE-USE VALUE; not IANA specified. This value MAY conflict with other private
     * use values from other extensions.
     *
     * <p>Corresponds to DIAMETER_UNABLE_TO_COMPLY Result code as specified in 3GPP TS 29.273
     *
     * @see 3GPP TS 24.302 Section 8.1.2.2
     * @deprecated This API is no longer used to distinguish back off timer support. As per TS24.302
     *     section 8.2.9.1, the BACK_OFF timer can be used with any ERROR_NOTIFY type.
     */
    @Deprecated public static final int ERROR_TYPE_NETWORK_FAILURE = 10500;

    private final byte mBackoffTimer;
    private final int mBackoffCause;

    /**
     * Constructs an Ike3gppBackoffTimer with the specified parameters.
     *
     * @param backoffTimer the backoff timer indicated by the peer
     * @param backoffCause the cause for this backoff timer, indicated by the peer
     * @hide
     */
    // NoByteOrShort: using byte to be consistent with the Backoff Timer specification
    @SystemApi
    public Ike3gppBackoffTimer(
            @SuppressLint("NoByteOrShort") byte backoffTimer,
            @IntRange(from = 0, to = ERROR_NOTIFY_TYPE_MAX) int backoffCause) {
        mBackoffTimer = backoffTimer;
        mBackoffCause = backoffCause;
    }

    @Override
    public @DataType int getDataType() {
        return DATA_TYPE_NOTIFY_BACKOFF_TIMER;
    }

    /**
     * Returns the Backoff Timer specified by the peer.
     *
     * <p>The Backoff Timer is coded as the value part (as specified in 3GPP TS 24.007 for type 4
     * IE) of the GPRS timer 3 information element defined in 3GPP TS 24.008 subclause 10.5.7.4a.
     */
    // NoByteOrShort: using byte to be consistent with the Backoff Timer specification
    @SuppressLint("NoByteOrShort")
    public byte getBackoffTimer() {
        return mBackoffTimer;
    }

    /** Returns the cause for this Backoff Timer specified by the peer. */
    public @IntRange(from = 0, to = ERROR_NOTIFY_TYPE_MAX) int getBackoffCause() {
        return mBackoffCause;
    }

    /** @hide */
    public static boolean isValidErrorNotifyCause(IkeNotifyPayload notifyPayload) {
        return notifyPayload.isErrorNotify();
    }
}
