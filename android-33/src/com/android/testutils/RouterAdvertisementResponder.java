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

package com.android.testutils;

import static android.system.OsConstants.IPPROTO_ICMPV6;

import static com.android.net.module.util.NetworkStackConstants.ETHER_ADDR_LEN;
import static com.android.net.module.util.NetworkStackConstants.ETHER_SRC_ADDR_OFFSET;
import static com.android.net.module.util.NetworkStackConstants.ETHER_TYPE_IPV6;
import static com.android.net.module.util.NetworkStackConstants.ICMPV6_ROUTER_SOLICITATION;
import static com.android.net.module.util.NetworkStackConstants.IPV6_ADDR_ALL_NODES_MULTICAST;
import static com.android.net.module.util.NetworkStackConstants.IPV6_ADDR_ALL_ROUTERS_MULTICAST;
import static com.android.net.module.util.NetworkStackConstants.PIO_FLAG_AUTONOMOUS;
import static com.android.net.module.util.NetworkStackConstants.PIO_FLAG_ON_LINK;

import android.net.InetAddresses;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.MacAddress;
import android.util.Pair;

import com.android.net.module.util.Ipv6Utils;
import com.android.net.module.util.Struct;
import com.android.net.module.util.structs.EthernetHeader;
import com.android.net.module.util.structs.Icmpv6Header;
import com.android.net.module.util.structs.Ipv6Header;
import com.android.net.module.util.structs.PrefixInformationOption;
import com.android.net.module.util.structs.RdnssOption;

import java.io.IOException;
import java.net.Inet6Address;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RA responder class useful for tests that require a provisioned interface.
 */
public class RouterAdvertisementResponder extends PacketResponder {
    private static final String TAG = "RouterAdvertisementResponder";
    private static final LinkAddress SLAAC_PREFIX = new LinkAddress("2001:db8::/64");
    private static final Inet6Address DNS_SERVER =
            (Inet6Address) InetAddresses.parseNumericAddress("2001:4860:4860::64");
    private final TapPacketReader mPacketReader;
    private final List<Pair<MacAddress, Inet6Address>> mRouterList = new ArrayList<>();

    public RouterAdvertisementResponder(TapPacketReader packetReader) {
        super(packetReader, RouterAdvertisementResponder::isRouterSolicitation, TAG);
        mPacketReader = packetReader;
    }

    private static boolean isRouterSolicitation(byte[] packet) {
        final ByteBuffer buffer = ByteBuffer.wrap(packet);
        final EthernetHeader ethHeader = Struct.parse(EthernetHeader.class, buffer);
        if (ethHeader.etherType != ETHER_TYPE_IPV6) {
            return false;
        }
        final Ipv6Header ipv6Header = Struct.parse(Ipv6Header.class, buffer);
        if (ipv6Header.nextHeader != IPPROTO_ICMPV6
                || !ipv6Header.dstIp.equals(IPV6_ADDR_ALL_ROUTERS_MULTICAST)) {
            return false;
        }
        final Icmpv6Header icmpv6Header = Struct.parse(Icmpv6Header.class, buffer);
        return icmpv6Header.type == ICMPV6_ROUTER_SOLICITATION;
    }

    /**
     * Adds a new router to be advertised.
     * @param mac the mac address of the router.
     * @param ip the link-local address of the router.
     */
    public void addRouterEntry(MacAddress mac, Inet6Address ip) {
        mRouterList.add(new Pair<>(mac, ip));
    }

    private ByteBuffer buildPrefixOption() {
        return PrefixInformationOption.build(
                new IpPrefix(SLAAC_PREFIX.getAddress(), SLAAC_PREFIX.getPrefixLength()),
                (byte) (PIO_FLAG_ON_LINK | PIO_FLAG_AUTONOMOUS), 3600/*valid lifetime*/,
                3600/*preferred lifetime*/);
    }

    private ByteBuffer buildRdnssOption() {
        return RdnssOption.build(3600/*lifetime, must be at least 120*/, DNS_SERVER);
    }

    private ByteBuffer buildRaPacket(MacAddress srcMac, MacAddress dstMac, Inet6Address srcIp) {
        return Ipv6Utils.buildRaPacket(srcMac, dstMac, srcIp, IPV6_ADDR_ALL_NODES_MULTICAST,
                (byte) 0 /*M=0, O=0*/, 3600 /*lifetime*/, 0 /*reachableTime, unspecified*/,
                0/*retransTimer, unspecified*/, buildPrefixOption(), buildRdnssOption());
    }

    @Override
    protected void replyToPacket(byte[] packet, TapPacketReader reader) {
        final MacAddress srcMac = MacAddress.fromBytes(
                Arrays.copyOfRange(packet, ETHER_SRC_ADDR_OFFSET,
                        ETHER_SRC_ADDR_OFFSET + ETHER_ADDR_LEN));

        for (Pair<MacAddress, Inet6Address> it : mRouterList) {
            final ByteBuffer raResponse = buildRaPacket(it.first, srcMac, it.second);
            try {
                reader.sendResponse(raResponse);
            } catch (IOException e) {
                throw new RuntimeException("Failed to send RA");
            }
        }
    }
}
