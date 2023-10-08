/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.net.wifi.aware;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.util.List;

/**
 * An object providing information about a Wi-Fi Aware discovery session with a specific peer.
 * @see DiscoverySessionCallback#onServiceDiscovered(ServiceDiscoveryInfo)
 * @see DiscoverySessionCallback#onServiceDiscoveredWithinRange(ServiceDiscoveryInfo, int)
 */
public final class ServiceDiscoveryInfo {
    private final byte[] mServiceSpecificInfo;
    private final List<byte[]> mMatchFilters;
    private final int mPeerCipherSuite;
    private final byte[] mScid;
    private final PeerHandle mPeerHandle;
    private final String mPairingAlias;
    private final AwarePairingConfig mPairingConfig;

    /**
     * @hide
     */
    public ServiceDiscoveryInfo(PeerHandle peerHandle, int peerCipherSuite,
            @Nullable byte[] serviceSpecificInfo,
            @NonNull List<byte[]> matchFilter, @Nullable byte[] scid, String pairingAlias,
            AwarePairingConfig pairingConfig) {
        mServiceSpecificInfo = serviceSpecificInfo;
        mMatchFilters = matchFilter;
        mPeerCipherSuite = peerCipherSuite;
        mScid = scid;
        mPeerHandle = peerHandle;
        mPairingAlias = pairingAlias;
        mPairingConfig = pairingConfig;
    }

    /**
     * Get the peer handle for the peer matching our discovery operation
     * @return An opaque handle representing the discovered peer.
     */
    @NonNull
    public PeerHandle getPeerHandle() {
        return mPeerHandle;
    }

    /**
     * Get the filter which resulted in this service discovery. For
     * {@link PublishConfig#PUBLISH_TYPE_UNSOLICITED},
     * {@link SubscribeConfig#SUBSCRIBE_TYPE_PASSIVE} discovery sessions this is the publisher's
     * match filter. For {@link PublishConfig#PUBLISH_TYPE_SOLICITED},
     * {@link SubscribeConfig#SUBSCRIBE_TYPE_ACTIVE} discovery sessions this is the subscriber's
     * match filter.
     * @return A list of byte arrays representing the match filter. An empty list if match filter
     * is not set.
     */
    @NonNull
    public List<byte[]> getMatchFilters() {
        return mMatchFilters;
    }

    /**
     * The service specific information (arbitrary byte array) provided by the peer as part of its
     * discovery configuration.
     * @see PublishConfig.Builder#setServiceSpecificInfo(byte[])
     * @see SubscribeConfig.Builder#setServiceSpecificInfo(byte[])
     * @return An arbitrary byte array represent the service specific information. {@code null} if
     * service specific information is not set.
     */
    @Nullable
    public byte[] getServiceSpecificInfo() {
        return mServiceSpecificInfo;
    }

    /**
     * Get the Security context identifier is associate with PMK for data path security config. Only
     * use for {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_PK_128} and
     * {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_PK_256} to get the PMKID set by
     * {@link WifiAwareDataPathSecurityConfig.Builder#setPmkId(byte[])} from publish session.
     * This can help the Wi-Fi Aware data-path setup to select the correct PMK/PMKID
     * @return An arbitrary byte array represent the security context identifier. {@code null} if
     * Security context identifier is not set.
     */
    @Nullable
    public byte[] getScid() {
        return mScid;
    }

    /**
     * Get the cipher suite type specified by the publish session to be used for data-path setup.
     * @return peerCipherSuite An integer represent the cipher suite used to encrypt the data-path.
     */
    public @Characteristics.WifiAwareDataPathCipherSuites int getPeerCipherSuite() {
        return mPeerCipherSuite;
    }

    /**
     * Get the paired device alias if the discovered device has already paired. If not null device
     * will automatically start the NAN pairing verification,
     * {@link DiscoverySessionCallback#onPairingVerificationSucceed(PeerHandle, String)}
     * will trigger when verification is finished
     */
    @Nullable
    public String getPairedAlias() {
        return mPairingAlias;
    }

    /**
     * Get the discovered device's pairing config. Can be used for the following pairing setup or
     * bootstrapping request.
     * @see AwarePairingConfig
     */
    @Nullable
    public AwarePairingConfig getPairingConfig() {
        return mPairingConfig;
    }
}
