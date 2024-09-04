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

import static com.android.internal.net.ipsec.ike.message.IkeConfigPayload.CONFIG_ATTR_INTERNAL_IP4_ADDRESS;
import static com.android.internal.net.ipsec.ike.message.IkeConfigPayload.CONFIG_ATTR_INTERNAL_IP4_DHCP;
import static com.android.internal.net.ipsec.ike.message.IkeConfigPayload.CONFIG_ATTR_INTERNAL_IP4_DNS;
import static com.android.internal.net.ipsec.ike.message.IkeConfigPayload.CONFIG_ATTR_INTERNAL_IP4_NETMASK;
import static com.android.internal.net.ipsec.ike.message.IkeConfigPayload.CONFIG_ATTR_INTERNAL_IP4_SUBNET;
import static com.android.internal.net.ipsec.ike.message.IkeConfigPayload.CONFIG_ATTR_INTERNAL_IP6_ADDRESS;
import static com.android.internal.net.ipsec.ike.message.IkeConfigPayload.CONFIG_ATTR_INTERNAL_IP6_DNS;
import static com.android.internal.net.ipsec.ike.message.IkeConfigPayload.CONFIG_ATTR_INTERNAL_IP6_SUBNET;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.net.IpPrefix;
import android.net.LinkAddress;

import com.android.internal.net.ipsec.ike.message.IkeConfigPayload;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttribute;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv4Address;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv4Dhcp;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv4Dns;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv4Netmask;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv4Subnet;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv6Address;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv6Dns;
import com.android.internal.net.ipsec.ike.message.IkeConfigPayload.ConfigAttributeIpv6Subnet;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ChildSessionConfiguration represents the negotiated configuration for a Child Session.
 *
 * <p>Configurations include traffic selectors and internal network information.
 */
public final class ChildSessionConfiguration {
    private static final int IPv4_DEFAULT_PREFIX_LEN = 32;

    private final List<IkeTrafficSelector> mInboundTs = new ArrayList<>();
    private final List<IkeTrafficSelector> mOutboundTs = new ArrayList<>();
    private final List<LinkAddress> mInternalAddressList = new ArrayList<>();
    private final List<InetAddress> mInternalDnsAddressList = new ArrayList<>();
    private final List<IpPrefix> mSubnetAddressList = new ArrayList<>();
    private final List<InetAddress> mInternalDhcpAddressList = new ArrayList<>();

    /**
     * Construct an instance of {@link ChildSessionConfiguration}.
     *
     * <p>ChildSessionConfiguration may contain negotiated configuration information that is
     * included in a Configure(Reply) Payload. Thus the input configPayload should always be a
     * Configure(Reply), and never be a Configure(Request).
     *
     * @hide
     */
    public ChildSessionConfiguration(
            List<IkeTrafficSelector> inTs,
            List<IkeTrafficSelector> outTs,
            IkeConfigPayload configPayload) {
        this(inTs, outTs);

        if (configPayload.configType != IkeConfigPayload.CONFIG_TYPE_REPLY) {
            throw new IllegalArgumentException(
                    "Cannot build ChildSessionConfiguration with configuration type: "
                            + configPayload.configType);
        }

        // It is validated in IkeConfigPayload that a config reply only has at most one non-empty
        // netmask and netmask exists only when IPv4 internal address exists.
        ConfigAttributeIpv4Netmask netmaskAttr = null;
        for (ConfigAttribute att : configPayload.recognizedAttributeList) {
            if (att.attributeType == CONFIG_ATTR_INTERNAL_IP4_NETMASK && !att.isEmptyValue()) {
                netmaskAttr = (ConfigAttributeIpv4Netmask) att;
            }
        }

        for (ConfigAttribute att : configPayload.recognizedAttributeList) {
            if (att.isEmptyValue()) continue;
            switch (att.attributeType) {
                case CONFIG_ATTR_INTERNAL_IP4_ADDRESS:
                    ConfigAttributeIpv4Address addressAttr = (ConfigAttributeIpv4Address) att;
                    if (netmaskAttr != null) {
                        mInternalAddressList.add(
                                new LinkAddress(addressAttr.address, netmaskAttr.getPrefixLen()));
                    } else {
                        mInternalAddressList.add(
                                new LinkAddress(addressAttr.address, IPv4_DEFAULT_PREFIX_LEN));
                    }
                    break;
                case CONFIG_ATTR_INTERNAL_IP4_NETMASK:
                    // No action.
                    break;
                case CONFIG_ATTR_INTERNAL_IP6_ADDRESS:
                    mInternalAddressList.add(((ConfigAttributeIpv6Address) att).linkAddress);
                    break;
                case CONFIG_ATTR_INTERNAL_IP4_DNS:
                    mInternalDnsAddressList.add(((ConfigAttributeIpv4Dns) att).address);
                    break;
                case CONFIG_ATTR_INTERNAL_IP6_DNS:
                    mInternalDnsAddressList.add(((ConfigAttributeIpv6Dns) att).address);
                    break;
                case CONFIG_ATTR_INTERNAL_IP4_SUBNET:
                    ConfigAttributeIpv4Subnet ipv4SubnetAttr = (ConfigAttributeIpv4Subnet) att;
                    mSubnetAddressList.add(
                            new IpPrefix(
                                    ipv4SubnetAttr.linkAddress.getAddress(),
                                    ipv4SubnetAttr.linkAddress.getPrefixLength()));
                    break;
                case CONFIG_ATTR_INTERNAL_IP6_SUBNET:
                    ConfigAttributeIpv6Subnet ipV6SubnetAttr = (ConfigAttributeIpv6Subnet) att;
                    mSubnetAddressList.add(
                            new IpPrefix(
                                    ipV6SubnetAttr.linkAddress.getAddress(),
                                    ipV6SubnetAttr.linkAddress.getPrefixLength()));
                    break;
                case CONFIG_ATTR_INTERNAL_IP4_DHCP:
                    mInternalDhcpAddressList.add(((ConfigAttributeIpv4Dhcp) att).address);
                    break;
                default:
                    // Not relevant to child session
            }
        }
    }

    /**
     * Construct an instance of {@link ChildSessionConfiguration}.
     *
     * @hide
     */
    public ChildSessionConfiguration(
            List<IkeTrafficSelector> inTs, List<IkeTrafficSelector> outTs) {
        mInboundTs.addAll(inTs);
        mOutboundTs.addAll(outTs);
    }

    /**
     * Construct an instance of {@link ChildSessionConfiguration}.
     *
     * @hide
     */
    private ChildSessionConfiguration(
            List<IkeTrafficSelector> inTs,
            List<IkeTrafficSelector> outTs,
            List<LinkAddress> internalAddresses,
            List<IpPrefix> internalSubnets,
            List<InetAddress> internalDnsServers,
            List<InetAddress> internalDhcpServers) {
        this(inTs, outTs);
        mInternalAddressList.addAll(internalAddresses);
        mSubnetAddressList.addAll(internalSubnets);
        mInternalDnsAddressList.addAll(internalDnsServers);
        mInternalDhcpAddressList.addAll(internalDhcpServers);
    }


    /**
     * Returns the negotiated inbound traffic selectors.
     *
     * <p>Only inbound traffic within the range is acceptable to the Child Session.
     *
     * <p>The Android platform does not support port-based routing. Port ranges of traffic selectors
     * are only informational.
     *
     * @return the inbound traffic selectors.
     */
    @NonNull
    public List<IkeTrafficSelector> getInboundTrafficSelectors() {
        return mInboundTs;
    }

    /**
     * Returns the negotiated outbound traffic selectors.
     *
     * <p>Only outbound traffic within the range is acceptable to the Child Session.
     *
     * <p>The Android platform does not support port-based routing. Port ranges of traffic selectors
     * are only informational.
     *
     * @return the outbound traffic selectors.
     */
    @NonNull
    public List<IkeTrafficSelector> getOutboundTrafficSelectors() {
        return mOutboundTs;
    }

    /**
     * Returns the assigned internal addresses.
     *
     * @return the assigned internal addresses, or an empty list when no addresses are assigned by
     *     the remote IKE server (e.g. for a non-tunnel mode Child Session).
     * @hide
     */
    @SystemApi
    @NonNull
    public List<LinkAddress> getInternalAddresses() {
        return Collections.unmodifiableList(mInternalAddressList);
    }

    /**
     * Returns the internal subnets protected by the IKE server.
     *
     * @return the internal subnets, or an empty list when no information of protected subnets is
     *     provided by the IKE server (e.g. for a non-tunnel mode Child Session).
     * @hide
     */
    @SystemApi
    @NonNull
    public List<IpPrefix> getInternalSubnets() {
        return Collections.unmodifiableList(mSubnetAddressList);
    }

    /**
     * Returns the internal DNS server addresses.
     *
     * @return the internal DNS server addresses, or an empty list when no DNS server is provided by
     *     the IKE server (e.g. for a non-tunnel mode Child Session).
     * @hide
     */
    @SystemApi
    @NonNull
    public List<InetAddress> getInternalDnsServers() {
        return Collections.unmodifiableList(mInternalDnsAddressList);
    }

    /**
     * Returns the internal DHCP server addresses.
     *
     * @return the internal DHCP server addresses, or an empty list when no DHCP server is provided
     *     by the IKE server (e.g. for a non-tunnel mode Child Session).
     * @hide
     */
    @SystemApi
    @NonNull
    public List<InetAddress> getInternalDhcpServers() {
        return Collections.unmodifiableList(mInternalDhcpAddressList);
    }

    /**
     * This class can be used to incrementally construct a {@link ChildSessionConfiguration}.
     *
     * <p>Except for testing, IKE library users normally do not instantiate {@link
     * ChildSessionConfiguration} themselves but instead get a reference via {@link
     * ChildSessionCallback}
     */
    public static final class Builder {
        private final List<IkeTrafficSelector> mInboundTs = new ArrayList<>();
        private final List<IkeTrafficSelector> mOutboundTs = new ArrayList<>();
        private final List<LinkAddress> mInternalAddressList = new ArrayList<>();
        private final List<IpPrefix> mSubnetAddressList = new ArrayList<>();
        private final List<InetAddress> mInternalDnsAddressList = new ArrayList<>();
        private final List<InetAddress> mInternalDhcpAddressList = new ArrayList<>();

        /**
         * Constructs a Builder.
         *
         * @param inTs the negotiated inbound traffic selectors
         * @param outTs the negotiated outbound traffic selectors
         */
        public Builder(
                @NonNull List<IkeTrafficSelector> inTs, @NonNull List<IkeTrafficSelector> outTs) {
            Objects.requireNonNull(inTs, "inTs was null");
            Objects.requireNonNull(outTs, "outTs was null");
            if (inTs.isEmpty() || outTs.isEmpty()) {
                throw new IllegalArgumentException("inTs or outTs is empty.");
            }
            mInboundTs.addAll(inTs);
            mOutboundTs.addAll(outTs);
        }

        /**
         * Adds an assigned internal address for the {@link ChildSessionConfiguration} being built.
         *
         * @param address an assigned internal addresses
         * @return Builder this, to facilitate chaining
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder addInternalAddress(@NonNull LinkAddress address) {
            Objects.requireNonNull(address, "address was null");
            mInternalAddressList.add(address);
            return this;
        }

        /**
         * Clears all assigned internal addresses from the {@link ChildSessionConfiguration} being
         * built.
         *
         * @return Builder this, to facilitate chaining
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder clearInternalAddresses() {
            mInternalAddressList.clear();
            return this;
        }

        /**
         * Adds an assigned internal subnet for the {@link ChildSessionConfiguration} being built.
         *
         * @param subnet an assigned internal subnet
         * @return Builder this, to facilitate chaining
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder addInternalSubnet(@NonNull IpPrefix subnet) {
            Objects.requireNonNull(subnet, "subnet was null");
            mSubnetAddressList.add(subnet);
            return this;
        }

        /**
         * Clears all assigned internal subnets from the {@link ChildSessionConfiguration} being
         * built.
         *
         * @return Builder this, to facilitate chaining
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder clearInternalSubnets() {
            mSubnetAddressList.clear();
            return this;
        }

        /**
         * Adds an assigned internal DNS server for the {@link ChildSessionConfiguration} being
         * built.
         *
         * @param dnsServer an assigned internal DNS server
         * @return Builder this, to facilitate chaining
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder addInternalDnsServer(@NonNull InetAddress dnsServer) {
            Objects.requireNonNull(dnsServer, "dnsServer was null");
            mInternalDnsAddressList.add(dnsServer);
            return this;
        }

        /**
         * Clears all assigned internal DNS servers from the {@link ChildSessionConfiguration} being
         * built.
         *
         * @return Builder this, to facilitate chaining
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder clearInternalDnsServers() {
            mInternalDnsAddressList.clear();
            return this;
        }

        /**
         * Adds an assigned internal DHCP server for the {@link ChildSessionConfiguration} being
         * built.
         *
         * @param dhcpServer an assigned internal DHCP server
         * @return Builder this, to facilitate chaining
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder addInternalDhcpServer(@NonNull InetAddress dhcpServer) {
            Objects.requireNonNull(dhcpServer, "dhcpServer was null");
            mInternalDhcpAddressList.add(dhcpServer);
            return this;
        }

        /**
         * Clears all assigned internal DHCP servers for the {@link ChildSessionConfiguration} being
         * built.
         *
         * @return Builder this, to facilitate chaining
         * @hide
         */
        @SystemApi
        @NonNull
        public Builder clearInternalDhcpServers() {
            mInternalDhcpAddressList.clear();
            return this;
        }

        /** Constructs an {@link ChildSessionConfiguration} instance. */
        @NonNull
        public ChildSessionConfiguration build() {
            return new ChildSessionConfiguration(
                    mInboundTs,
                    mOutboundTs,
                    mInternalAddressList,
                    mSubnetAddressList,
                    mInternalDnsAddressList,
                    mInternalDhcpAddressList);
        }
    }
}
