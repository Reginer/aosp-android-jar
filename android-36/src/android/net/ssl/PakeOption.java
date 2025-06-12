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

import android.annotation.FlaggedApi;
import android.annotation.SystemApi;

import libcore.util.NonNull;
import libcore.util.Nullable;

import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An class representing a PAKE (Password Authenticated Key Exchange)
 * option for TLS connections.
 *
 * <p>Instances of this class are immutable. Use the {@link Builder} to create
 * instances.</p>
 *
 * @hide
 */
@SystemApi
@FlaggedApi(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
public final class PakeOption {
    // Required length of the password verifier parameters
    private static final int W_LENGTH = 32;
    // Required length of the L parameter
    private static final int L_LENGTH = 65;

    /**
     * The algorithm of the PAKE algorithm.
     */
    private final String algorithm; // For now "SPAKE2PLUS_PRERELEASE" is suported

    /**
     * A map containing the message components for the PAKE exchange.
     *
     * <p>The keys are strings representing the component algorithms (e.g., "password",
     * "w0", "w1"). The values are byte arrays containing the component data.</p>
     */
    private final Map<String, byte[]> messageComponents;

    private PakeOption(String algorithm, Map<String, byte[]> messageComponents) {
        this.algorithm = algorithm;
        this.messageComponents = Collections.unmodifiableMap(new HashMap<>(messageComponents));
    }

    /**
     * Returns the algorithm of the PAKE algorithm.
     *
     * @return The algorithm of the PAKE algorithm.
     */
    public @NonNull String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the message component with the given key.
     *
     * @param key The algorithm of the component.
     * @return The component data, or {@code null} if no component with the given
     *         key exists.
     */
    public @Nullable byte[] getMessageComponent(@NonNull String key) {
        return messageComponents.get(key);
    }

    /**
     * A builder for creating {@link PakeOption} instances.
     *
     * @hide
     */
    @SystemApi
    @FlaggedApi(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
    public static final class Builder {
        private String algorithm;
        private Map<String, byte[]> messageComponents = new HashMap<>();

        /**
         * Constructor for the builder.
         *
         * @param algorithm The algorithm of the PAKE algorithm.
         * @throws InvalidParameterException If the algorithm is invalid.
         */
        public Builder(@NonNull String algorithm) {
            if (algorithm == null || algorithm.isEmpty()) {
                throw new InvalidParameterException("Algorithm cannot be null or empty.");
            }
            this.algorithm = algorithm;
        }

        /**
         * Adds a message component.
         *
         * @param key The algorithm of the component.
         * @param value The component data.
         * @return This builder.
         * @throws InvalidParameterException If the key is invalid.
         */
        public @NonNull Builder addMessageComponent(@NonNull String key, @Nullable byte[] value) {
            if (key == null || key.isEmpty()) {
                throw new InvalidParameterException("Key cannot be null or empty.");
            }
            messageComponents.put(key, value.clone());
            return this;
        }

        /**
         * Builds a new {@link PakeOption} instance.
         *
         * <p>This method performs validation to ensure that the message components
         * are consistent with the PAKE algorithm.</p>
         *
         * @return A new {@link PakeOption} instance.
         * @throws InvalidParameterException If the message components are invalid.
         */
        public @NonNull PakeOption build() {
            if (messageComponents.isEmpty()) {
                throw new InvalidParameterException("Message components cannot be empty.");
            }
            if (algorithm.equals("SPAKE2PLUS_PRERELEASE")) {
                validateSpake2PlusComponents();
            }

            return new PakeOption(algorithm, messageComponents);
        }

        private void validateSpake2PlusComponents() {
            // For SPAKE2+ password is the only required component.
            if (!messageComponents.containsKey("password")) {
                throw new InvalidParameterException(
                        "For SPAKE2+, 'password' must be present.");
            }
        }
    }
}
