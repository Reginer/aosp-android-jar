/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.net.ipsec.ike;

import static com.android.internal.net.ipsec.ike.message.IkeConfigPayload.CONFIG_ATTR_APPLICATION_VERSION;
import static com.android.internal.net.ipsec.ike.message.IkeConfigPayload.CONFIG_ATTR_IP4_PCSCF;
import static com.android.internal.net.ipsec.ike.message.IkeConfigPayload.CONFIG_ATTR_IP6_PCSCF;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.net.eap.EapInfo;

import com.android.internal.net.ipsec.ike.message.IkeConfigPayload;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttribute;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeAppVersion;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv4Pcscf;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv6Pcscf;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * IkeSessionConfiguration represents the negotiated configuration for a {@link IkeSession}.
 *
 * <p>Configurations include remote application version and enabled IKE extensions.
 */
public final class IkeSessionConfiguration {
    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({EXTENSION_TYPE_FRAGMENTATION, EXTENSION_TYPE_MOBIKE})
    public @interface ExtensionType {}

    /** IKE Message Fragmentation */
    public static final int EXTENSION_TYPE_FRAGMENTATION = 1;
    /** IKEv2 Mobility and Multihoming Protocol */
    public static final int EXTENSION_TYPE_MOBIKE = 2;

    private static final int VALID_EXTENSION_MIN = EXTENSION_TYPE_FRAGMENTATION;
    private static final int VALID_EXTENSION_MAX = EXTENSION_TYPE_MOBIKE;

    private final String mRemoteApplicationVersion;
    private final IkeSessionConnectionInfo mIkeConnInfo;
    private final List<InetAddress> mPcscfServers = new ArrayList<>();
    private final List<byte[]> mRemoteVendorIds = new ArrayList<>();
    private final Set<Integer> mEnabledExtensions = new HashSet<>();
    private final EapInfo mEapInfo;

    /**
     * Construct an instance of {@link IkeSessionConfiguration}.
     *
     * <p>IkeSessionConfigurations may contain negotiated configuration information that is included
     * in a Configure(Reply) Payload. Thus the input configPayload should always be a
     * Configure(Reply), and never be a Configure(Request).
     *
     * @hide
     */
    public IkeSessionConfiguration(
            IkeSessionConnectionInfo ikeConnInfo,
            IkeConfigPayload configPayload,
            List<byte[]> remoteVendorIds,
            List<Integer> enabledExtensions,
            EapInfo eapInfo) {
        mIkeConnInfo = ikeConnInfo;
        mRemoteVendorIds.addAll(remoteVendorIds);
        mEnabledExtensions.addAll(enabledExtensions);
        mEapInfo = eapInfo;

        String appVersion = "";
        if (configPayload != null) {
            if (configPayload.configType != IkeConfigPayload.CONFIG_TYPE_REPLY) {
                throw new IllegalArgumentException(
                        "Cannot build IkeSessionConfiguration with configuration type: "
                                + configPayload.configType);
            }

            for (ConfigAttribute attr : configPayload.recognizedAttributeList) {
                if (attr.isEmptyValue()) continue;
                switch (attr.attributeType) {
                    case CONFIG_ATTR_APPLICATION_VERSION:
                        ConfigAttributeAppVersion appVersionAttr = (ConfigAttributeAppVersion) attr;
                        appVersion = appVersionAttr.applicationVersion;
                        break;
                    case CONFIG_ATTR_IP4_PCSCF:
                        ConfigAttributeIpv4Pcscf ip4Pcscf = (ConfigAttributeIpv4Pcscf) attr;
                        mPcscfServers.add(ip4Pcscf.getAddress());
                        break;
                    case CONFIG_ATTR_IP6_PCSCF:
                        ConfigAttributeIpv6Pcscf ip6Pcscf = (ConfigAttributeIpv6Pcscf) attr;
                        mPcscfServers.add(ip6Pcscf.getAddress());
                        break;
                    default:
                        // Not relevant to IKE session
                }
            }
        }
        mRemoteApplicationVersion = appVersion;
        validateOrThrow();
    }

    /**
     * Construct an instance of {@link IkeSessionConfiguration}.
     *
     * @hide
     */
    private IkeSessionConfiguration(
            IkeSessionConnectionInfo ikeConnInfo,
            List<InetAddress> pcscfServers,
            List<byte[]> remoteVendorIds,
            Set<Integer> enabledExtensions,
            String remoteApplicationVersion,
            EapInfo eapInfo) {
        mIkeConnInfo = ikeConnInfo;
        mPcscfServers.addAll(pcscfServers);
        mRemoteVendorIds.addAll(remoteVendorIds);
        mEnabledExtensions.addAll(enabledExtensions);
        mRemoteApplicationVersion = remoteApplicationVersion;
        mEapInfo = eapInfo;

        validateOrThrow();
    }

    private void validateOrThrow() {
        String errMsg = " was null";
        Objects.requireNonNull(mIkeConnInfo, "ikeConnInfo" + errMsg);
        Objects.requireNonNull(mPcscfServers, "pcscfServers" + errMsg);
        Objects.requireNonNull(mRemoteVendorIds, "remoteVendorIds" + errMsg);
        Objects.requireNonNull(mRemoteApplicationVersion, "remoteApplicationVersion" + errMsg);
        Objects.requireNonNull(mRemoteVendorIds, "remoteVendorIds" + errMsg);
    }

    /**
     * Gets remote (server) version information.
     *
     * @return application version of the remote server, or an empty string if the remote server did
     *     not provide the application version.
     */
    @NonNull
    public String getRemoteApplicationVersion() {
        return mRemoteApplicationVersion;
    }

    /**
     * Returns remote vendor IDs received during IKE Session setup.
     *
     * <p>According to the IKEv2 specification (RFC 7296), a vendor ID may indicate the sender is
     * capable of accepting certain extensions to the protocol, or it may simply identify the
     * implementation as an aid in debugging.
     *
     * @return the vendor IDs of the remote server, or an empty list if no vendor ID is received
     *     during IKE Session setup.
     */
    @NonNull
    public List<byte[]> getRemoteVendorIds() {
        return Collections.unmodifiableList(mRemoteVendorIds);
    }

    /**
     * Checks if an IKE extension is enabled.
     *
     * <p>An IKE extension is enabled when both sides can support it. This negotiation always
     * happens in IKE initial exchanges (IKE INIT and IKE AUTH).
     *
     * @param extensionType the extension type.
     * @return {@code true} if this extension is enabled.
     */
    public boolean isIkeExtensionEnabled(@ExtensionType int extensionType) {
        return mEnabledExtensions.contains(extensionType);
    }

    /**
     * Returns the assigned P_CSCF servers.
     *
     * @return the assigned P_CSCF servers, or an empty list when no servers are assigned by the
     *     remote IKE server.
     * @hide
     */
    @SystemApi
    @NonNull
    public List<InetAddress> getPcscfServers() {
        return Collections.unmodifiableList(mPcscfServers);
    }

    /**
     * Returns the connection information.
     *
     * @return the IKE Session connection information.
     */
    @NonNull
    public IkeSessionConnectionInfo getIkeSessionConnectionInfo() {
        return mIkeConnInfo;
    }

    /**
     * Retrieves the EAP information.
     *
     * @return the EAP information provided by the server during EAP authentication (e.g. next
     *    re-authentication ID), or null if the server did not provide any information that will be
     *    useful after the authentication.
     */
    @Nullable
    public EapInfo getEapInfo() {
        return mEapInfo;
    }

    /**
     * This class can be used to incrementally construct a {@link IkeSessionConfiguration}.
     *
     * <p>Except for testing, IKE library users normally do not instantiate {@link
     * IkeSessionConfiguration} themselves but instead get a reference via {@link
     * IkeSessionCallback}
     */
    public static final class Builder {
        private final IkeSessionConnectionInfo mIkeConnInfo;
        private final List<InetAddress> mPcscfServers = new ArrayList<>();
        private final List<byte[]> mRemoteVendorIds = new ArrayList<>();
        private final Set<Integer> mEnabledExtensions = new HashSet<>();
        private String mRemoteApplicationVersion = "";
        private EapInfo mEapInfo;

        /**
         * Constructs a Builder.
         *
         * @param ikeConnInfo the connection information
         */
        public Builder(@NonNull IkeSessionConnectionInfo ikeConnInfo) {
            Objects.requireNonNull(ikeConnInfo, "ikeConnInfo was null");
            mIkeConnInfo = ikeConnInfo;
        }

        /**
         * Adds an assigned P_CSCF server for the {@link IkeSessionConfiguration} being built.
         *
         * @param pcscfServer an assigned P_CSCF server
         * @return Builder this, to facilitate chaining
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder addPcscfServer(@NonNull InetAddress pcscfServer) {
            Objects.requireNonNull(pcscfServer, "pcscfServer was null");
            mPcscfServers.add(pcscfServer);
            return this;
        }

        /**
         * Clear all P_CSCF servers from the {@link IkeSessionConfiguration} being built.
         *
         * @return Builder this, to facilitate chaining
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder clearPcscfServers() {
            mPcscfServers.clear();
            return this;
        }

        /**
         * Adds a remote vendor ID for the {@link IkeSessionConfiguration} being built.
         *
         * @param remoteVendorId a remote vendor ID
         * @return Builder this, to facilitate chaining
         */
        @NonNull
        public Builder addRemoteVendorId(@NonNull byte[] remoteVendorId) {
            Objects.requireNonNull(remoteVendorId, "remoteVendorId was null");
            mRemoteVendorIds.add(remoteVendorId);
            return this;
        }

        /**
         * Clears all remote vendor IDs from the {@link IkeSessionConfiguration} being built.
         *
         * @return Builder this, to facilitate chaining
         */
        @NonNull
        public Builder clearRemoteVendorIds() {
            mRemoteVendorIds.clear();
            return this;
        }

        /**
         * Sets the remote application version for the {@link IkeSessionConfiguration} being built.
         *
         * @param remoteApplicationVersion the remote application version. Defaults to an empty
         *     string.
         * @return Builder this, to facilitate chaining
         */
        @NonNull
        public Builder setRemoteApplicationVersion(@NonNull String remoteApplicationVersion) {
            Objects.requireNonNull(remoteApplicationVersion, "remoteApplicationVersion was null");
            mRemoteApplicationVersion = remoteApplicationVersion;
            return this;
        }

        /**
         * Clears the remote application version from the {@link IkeSessionConfiguration} being
         * built.
         *
         * @return Builder this, to facilitate chaining
         */
        @NonNull
        public Builder clearRemoteApplicationVersion() {
            mRemoteApplicationVersion = "";
            return this;
        }

        private static void validateExtensionOrThrow(@ExtensionType int extensionType) {
            if (extensionType >= VALID_EXTENSION_MIN && extensionType <= VALID_EXTENSION_MAX) {
                return;
            }
            throw new IllegalArgumentException("Invalid extension type: " + extensionType);
        }

        /**
         * Marks an IKE extension as enabled for the {@link IkeSessionConfiguration} being built.
         *
         * @param extensionType the enabled extension
         * @return Builder this, to facilitate chaining
         */
        // MissingGetterMatchingBuilder: Use #isIkeExtensionEnabled instead of #getIkeExtension
        // because #isIkeExtensionEnabled allows callers to check the presence of an IKE extension
        // more easily
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder addIkeExtension(@ExtensionType int extensionType) {
            validateExtensionOrThrow(extensionType);
            mEnabledExtensions.add(extensionType);
            return this;
        }

        /**
         * Clear all enabled IKE extensions from the {@link IkeSessionConfiguration} being built.
         *
         * @return Builder this, to facilitate chaining
         */
        @NonNull
        public Builder clearIkeExtensions() {
            mEnabledExtensions.clear();
            return this;
        }

        /**
         * Sets EapInfo for the {@link IkeSessionConfiguration} being built.
         *
         * @return Builder this, to facilitate chaining
         */
        @NonNull
        public Builder setEapInfo(@Nullable EapInfo eapInfo) {
            mEapInfo = eapInfo;
            return this;
        }

        /** Constructs an {@link IkeSessionConfiguration} instance. */
        @NonNull
        public IkeSessionConfiguration build() {
            return new IkeSessionConfiguration(
                    mIkeConnInfo,
                    mPcscfServers,
                    mRemoteVendorIds,
                    mEnabledExtensions,
                    mRemoteApplicationVersion,
                    mEapInfo);
        }
    }
}
