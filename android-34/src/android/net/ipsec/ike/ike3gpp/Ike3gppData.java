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

import android.annotation.IntDef;
import android.annotation.SystemApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Ike3gppData represents 3GPP-specific data sent by the peer/remote endpoint.
 *
 * @see 3GPP ETSI TS 24.302: Access to the 3GPP Evolved Packet Core (EPC) via non-3GPP access
 *     networks
 * @hide
 */
@SystemApi
public abstract class Ike3gppData {
    private static final int DATA_TYPE_SHARED_BASE = 0;
    private static final int DATA_TYPE_CATEGORY_SIZE = 100;

    private static final int DATA_TYPE_PAYLOAD_NOTIFY_BASE = DATA_TYPE_SHARED_BASE;

    /** Data Type representing an {@link Ike3gppN1ModeInformation}. */
    public static final int DATA_TYPE_NOTIFY_N1_MODE_INFORMATION =
            DATA_TYPE_PAYLOAD_NOTIFY_BASE + 1;

    /** Data Type representing an {@link Ike3gppBackoffTimer}. */
    public static final int DATA_TYPE_NOTIFY_BACKOFF_TIMER = DATA_TYPE_PAYLOAD_NOTIFY_BASE + 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DATA_TYPE_NOTIFY_N1_MODE_INFORMATION, DATA_TYPE_NOTIFY_BACKOFF_TIMER})
    public @interface DataType {}

    /** @hide */
    protected Ike3gppData() {}

    /** Returns the DataType that this Ike3gppData represents. */
    public abstract @DataType int getDataType();
}
