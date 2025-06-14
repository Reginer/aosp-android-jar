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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.ManagerFactoryParameters;

/**
 * Parameters for configuring a {@code KeyManager} that supports PAKE
 * (Password Authenticated Key Exchange) on the server side.
 *
 * <p>This class holds the necessary information for the {@code KeyManager} to perform PAKE
 * authentication, including a mapping of client and server IDs (links) to their corresponding PAKE
 * options.</p>
 *
 * <p>Instances of this class are immutable. Use the {@link Builder} to create
 * instances.</p>
 *
 * @hide
 */
@SystemApi
@FlaggedApi(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
public final class PakeServerKeyManagerParameters implements ManagerFactoryParameters {
    /**
     * A map of links to their corresponding PAKE options.
     */
    private final Map<Link, List<PakeOption>> links;

    /**
     * Private constructor to enforce immutability.
     *
     * @param links A map of links to their corresponding PAKE options.
     */
    private PakeServerKeyManagerParameters(Map<Link, List<PakeOption>> links) {
        this.links = Collections.unmodifiableMap(new HashMap<>(links));
    }

    /**
     * Returns a set of the links.
     *
     * @return The known links.
     */
    public @NonNull Set<Link> getLinks() {
        return Collections.unmodifiableSet(links.keySet());
    }

    /**
     * Returns an unmodifiable list of PAKE options for the given {@link Link}.
     *
     * @param link The link for which to retrieve the options. Should have been obtained through
     *             {@link #getLinks}.
     * @return An unmodifiable list of PAKE options for the given link.
     */
    public @NonNull List<PakeOption> getOptions(@NonNull Link link) {
        requireNonNull(link, "Link cannot be null.");
        List<PakeOption> options = links.get(link);
        if (options == null) {
            throw new InvalidParameterException("Link not found.");
        }
        return Collections.unmodifiableList(options);
    }

    /**
     * Returns an unmodifiable list of PAKE options for the given client-server pair.
     *
     * @param clientId The client identifier for the link.
     * @param serverId The server identifier for the link.
     * @return An unmodifiable list of PAKE options for the given link.
     */
    public @NonNull List<PakeOption> getOptions(
            @Nullable byte[] clientId, @Nullable byte[] serverId) {
        return getOptions(new Link(clientId, serverId));
    }

    /**
     * A PAKE link class combining the client and server IDs.
     *
     * @hide
     */
    @SystemApi
    @FlaggedApi(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
    public static final class Link {
        private final byte[] clientId;
        private final byte[] serverId;

        /**
         * Constructs a {@code Link} object.
         *
         * @param clientId The client identifier for the link.
         * @param serverId The server identifier for the link.
         */
        private Link(@Nullable byte[] clientId, @Nullable byte[] serverId) {
            this.clientId = clientId;
            this.serverId = serverId;
        }

        /**
         * Returns the client identifier for the link.
         *
         * @return The client identifier for the link.
         */
        public @Nullable byte[] getClientId() {
            return clientId;
        }

        /**
         * Returns the server identifier for the link.
         *
         * @return The server identifier for the link.
         */
        public @Nullable byte[] getServerId() {
            return serverId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Link that = (Link) o;
            return java.util.Arrays.equals(clientId, that.clientId)
                    && java.util.Arrays.equals(serverId, that.serverId);
        }

        @Override
        public int hashCode() {
            int result = java.util.Arrays.hashCode(clientId);
            result = 31 * result + java.util.Arrays.hashCode(serverId);
            return result;
        }
    }

    /**
     * A builder for creating {@link PakeServerKeyManagerParameters} instances.
     *
     * @hide
     */
    @SystemApi
    @FlaggedApi(com.android.org.conscrypt.flags.Flags.FLAG_SPAKE2PLUS_API)
    public static final class Builder {
        private final Map<Link, List<PakeOption>> links = new HashMap<>();

        /**
         * Adds PAKE options for the given client and server IDs.
         * Only the first link for SPAKE2PLUS_PRERELEASE will be used.
         *
         * @param clientId The client ID.
         * @param serverId The server ID.
         * @param options The list of PAKE options to add.
         * @return This builder.
         * @throws InvalidParameterException If the provided options are invalid.
         */
        public @NonNull Builder setOptions(@Nullable byte[] clientId, @Nullable byte[] serverId,
                @NonNull List<PakeOption> options) {
            requireNonNull(options, "options cannot be null.");
            if (options.isEmpty()) {
                throw new InvalidParameterException("options cannot be empty.");
            }

            Link link = new Link(clientId, serverId);
            List<PakeOption> storedOptions = new ArrayList<PakeOption>(options.size());

            for (PakeOption option : options) {
                // Check that options are not duplicated.
                for (PakeOption previousOption : storedOptions) {
                    if (previousOption.getAlgorithm().equals(option.getAlgorithm())) {
                        throw new InvalidParameterException(
                                "There are multiple options with the same algorithm.");
                    }
                }
                storedOptions.add(option);
            }

            links.put(link, storedOptions);
            return this;
        }

        /**
         * Builds a new {@link PakeServerKeyManagerParameters} instance.
         *
         * @return A new {@link PakeServerKeyManagerParameters} instance.
         * @throws InvalidParameterException If no links are provided.
         */
        public @NonNull PakeServerKeyManagerParameters build() {
            if (links.isEmpty()) {
                throw new InvalidParameterException("At least one link must be provided.");
            }
            return new PakeServerKeyManagerParameters(links);
        }
    }
}
