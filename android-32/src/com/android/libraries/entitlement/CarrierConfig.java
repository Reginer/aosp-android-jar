/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.libraries.entitlement;

import android.net.Network;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

/**
 * Carrier specific customization to be used in the service entitlement queries and operations.
 *
 * @see #ServiceEntitlement
 */
@AutoValue
public abstract class CarrierConfig {
    /** Default value of {@link #timeoutInSec} if not set. */
    public static final int DEFAULT_TIMEOUT_IN_SEC = 30;

    /** The carrier's entitlement server URL. See {@link Builder#setServerUrl}. */
    public abstract String serverUrl();

    /** Client side timeout for HTTP connection. See {@link Builder#setTimeoutInSec}. */
    public abstract int timeoutInSec();

    /** The {@link Network} used for HTTP connection. See {@link Builder#setNetwork}. */
    @Nullable
    public abstract Network network();

    /** Returns a new {@link Builder} object. */
    public static Builder builder() {
        return new AutoValue_CarrierConfig.Builder()
                .setServerUrl("")
                .setTimeoutInSec(DEFAULT_TIMEOUT_IN_SEC);
    }

    /** Builder. */
    @AutoValue.Builder
    public abstract static class Builder {
        public abstract CarrierConfig build();

        /**
         * Sets the carrier's entitlement server URL. If not set, will use {@code
         * https://aes.mnc<MNC>.mcc<MCC>.pub.3gppnetwork.org} as defined in GSMA TS.43 section 2.1.
         */
        public abstract Builder setServerUrl(String url);

        /**
         * Sets the client side timeout for HTTP connection. Default to
         * {@link DEFAULT_TIMEOUT_IN_SEC}.
         *
         * <p>This timeout is used by both {@link java.net.URLConnection#setConnectTimeout} and
         * {@link java.net.URLConnection#setReadTimeout}.
         */
        public abstract Builder setTimeoutInSec(int timeoutInSec);

        /**
         * Sets the {@link Network} used for HTTP connection. If not set, the device default network
         * is used.
         */
        public abstract Builder setNetwork(Network network);
    }
}
