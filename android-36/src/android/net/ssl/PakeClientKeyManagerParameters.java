/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.net.ssl;

import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.SystemApi;

import libcore.util.NonNull;
import libcore.util.Nullable;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.ManagerFactoryParameters;

/**
 * Parameters for configuring a {@code KeyManager} that supports PAKE (Password
 * Authenticated Key Exchange).
 *
 * <p>This class holds the necessary information for the {@code KeyManager} to perform PAKE
 * authentication, including the IDs of the client and server involved and the available PAKE
 * options.</p>
 *
 * <p>Instances of this class are immutable. Use the {@link Builder} to create
 * instances.</p>
 *
 * @hide
 */
@SystemApi
@FlaggedApi(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
public final class PakeClientKeyManagerParameters implements ManagerFactoryParameters {
    /**
     * The ID of the client involved in the PAKE exchange.
     */
    private final byte[] clientId;

    /**
     * The ID of the server involved in the PAKE exchange.
     */
    private final byte[] serverId;

    /**
     * A list of available PAKE options. At least one option needs to be
     * provided.
     */
    private final List<PakeOption> options;

    /**
     * Private constructor to enforce immutability.
     *
     * @param clientId The ID of the client involved in the PAKE exchange.
     * @param serverId The ID of the server involved in the PAKE exchange.
     * @param options  A list of available PAKE options.
     */
    private PakeClientKeyManagerParameters(
            byte[] clientId, byte[] serverId, List<PakeOption> options) {
        this.clientId = clientId;
        this.serverId = serverId;
        this.options = Collections.unmodifiableList(new ArrayList<>(options));
    }

    /**
     * Returns the client identifier.
     *
     * @return The client identifier.
     */
    public @Nullable byte[] getClientId() {
        return clientId;
    }

    /**
     * Returns the server identifier.
     *
     * @return The server identifier.
     */
    public @Nullable byte[] getServerId() {
        return serverId;
    }

    /**
     * Returns a copy of the list of available PAKE options.
     *
     * @return A copy of the list of available PAKE options.
     */
    public @NonNull List<PakeOption> getOptions() {
        return new ArrayList<>(options);
    }

    /**
     * A builder for creating {@link PakeClientKeyManagerParameters} instances.
     *
     * @hide
     */
    @SystemApi
    @FlaggedApi(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
    public static final class Builder {
        private byte[] clientId;
        private byte[] serverId;
        private List<PakeOption> options = new ArrayList<>();

        /**
         * Sets the ID of the client involved in the PAKE exchange.
         *
         * @param clientId The ID of the client involved in the PAKE exchange.
         * @return This builder.
         */
        public @NonNull Builder setClientId(@Nullable byte[] clientId) {
            this.clientId = clientId;
            return this;
        }

        /**
         * Sets the ID of the server involved in the PAKE exchange.
         *
         * @param serverId The ID of the server involved in the PAKE exchange.
         * @return This builder.
         */
        public @NonNull Builder setServerId(@Nullable byte[] serverId) {
            this.serverId = serverId;
            return this;
        }

        /**
         * Adds a PAKE option.
         *
         * @param option The PAKE option to add.
         * @return This builder.
         * @throws InvalidParameterException If an option with the same algorithm already exists.
         */
        public @NonNull Builder addOption(@NonNull PakeOption option) {
            requireNonNull(option, "Option cannot be null.");

            for (PakeOption existingOption : options) {
                if (existingOption.getAlgorithm().equals(option.getAlgorithm())) {
                    throw new InvalidParameterException(
                            "An option with the same algorithm already exists.");
                }
            }
            this.options.add(option);
            return this;
        }

        /**
         * Builds a new {@link PakeClientKeyManagerParameters} instance.
         *
         * @return A new {@link PakeClientKeyManagerParameters} instance.
         * @throws InvalidParameterException If no PAKE options are provided.
         */
        public @NonNull PakeClientKeyManagerParameters build() {
            if (options.isEmpty()) {
                throw new InvalidParameterException("At least one PAKE option must be provided.");
            }
            return new PakeClientKeyManagerParameters(clientId, serverId, options);
        }
    }
}
