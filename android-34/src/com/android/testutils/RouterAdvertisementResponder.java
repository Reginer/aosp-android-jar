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

import static com.android.net.module.util.NetworkStackConstants.ETHER_TYPE_IPV6;
import static com.android.net.module.util.NetworkStackConstants.ICMPV6_ND_OPTION_SLLA;
import static com.android.net.module.util.NetworkStackConstants.ICMPV6_ND_OPTION_TLLA;
import static com.android.net.module.util.NetworkStackConstants.ICMPV6_NEIGHBOR_SOLICITATION;
import static com.android.net.module.util.NetworkStackConstants.ICMPV6_ROUTER_SOLICITATION;
import static com.android.net.module.util.NetworkStackConstants.IPV6_ADDR_ALL_NODES_MULTICAST;
import static com.android.net.module.util.NetworkStackConstants.NEIGHBOR_ADVERTISEMENT_FLAG_OVERRIDE;
import static com.android.net.module.util.NetworkStackConstants.NEIGHBOR_ADVERTISEMENT_FLAG_ROUTER;
import static com.android.net.module.util.NetworkStackConstants.NEIGHBOR_ADVERTISEMENT_FLAG_SOLICITED;
import static com.android.net.module.util.NetworkStackConstants.PIO_FLAG_AUTONOMOUS;
import static com.android.net.module.util.NetworkStackConstants.PIO_FLAG_ON_LINK;

import android.net.InetAddresses;
import android.net.IpPrefix;
import android.net.MacAddress;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;

import com.android.net.module.util.Ipv6Utils;
import com.android.net.module.util.Struct;
import com.android.net.module.util.structs.EthernetHeader;
import com.android.net.module.util.structs.Icmpv6Header;
import com.android.net.module.util.structs.Ipv6Header;
import com.android.net.module.util.structs.LlaOption;
import com.android.net.module.util.structs.NsHeader;
import com.android.net.module.util.structs.PrefixInformationOption;
import com.android.net.module.util.structs.RdnssOption;

import java.io.IOException;
import java.net.Inet6Address;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * ND (RA & NA) responder class useful for tests that require a provisioned IPv6 interface.
 * TODO: rename to NdResponder
 */
public class RouterAdvertisementResponder extends PacketResponder {
    private static final String TAG = "RouterAdvertisementResponder";
    private static final Inet6Address DNS_SERVER =
            (Inet6Address) InetAddresses.parseNumericAddress("2001:4860:4860::64");
    private final TapPacketReader mPacketReader;
    // Maps IPv6 address to MacAddress and isRouter boolean.
    private final Map<Inet6Address, Pair<MacAddress, Boolean>> mNeighborMap = new ArrayMap<>();
    private final IpPrefix mPrefix;

    public RouterAdvertisementResponder(TapPacketReader packetReader, IpPrefix prefix) {
        super(packetReader, RouterAdvertisementResponder::isRsOrNs, TAG);
        mPacketReader = packetReader;
        mPrefix = Objects.requireNonNull(prefix);
    }

    public RouterAdvertisementResponder(TapPacketReader packetReader) {
        this(packetReader, makeRandomPrefix());
    }

    private static IpPrefix makeRandomPrefix() {
        final byte[] prefixBytes = new IpPrefix("2001:db8::/64").getAddress().getAddress();
        final Random r = new Random();
        for (int i = 4; i < 8; i++) {
            prefixBytes[i] = (byte) r.nextInt();
        }
        return new IpPrefix(prefixBytes, 64);
    }

    /** Returns true if the packet is a router solicitation or neighbor solicitation message. */
    private static boolean isRsOrNs(byte[] packet) {
        final ByteBuffer buffer = ByteBuffer.wrap(packet);
        final EthernetHeader ethHeader = Struct.parse(EthernetHeader.class, buffer);
        if (ethHeader.etherType != ETHER_TYPE_IPV6) {
            return false;
        }
        final Ipv6Header ipv6Header = Struct.parse(Ipv6Header.class, buffer);
        if (ipv6Header.nextHeader != IPPROTO_ICMPV6) {
            return false;
        }
        final Icmpv6Header icmpv6Header = Struct.parse(Icmpv6Header.class, buffer);
        return icmpv6Header.type == ICMPV6_ROUTER_SOLICITATION
            || icmpv6Header.type == ICMPV6_NEIGHBOR_SOLICITATION;
    }

    /**
     * Adds a new router to be advertised.
     * @param mac the mac address of the router.
     * @param ip the link-local address of the router.
     */
    public void addRouterEntry(MacAddress mac, Inet6Address ip) {
        mNeighborMap.put(ip, new Pair<>(mac, true));
    }

    /**
     * Adds a new neighbor to be advertised.
     * @param mac the mac address of the neighbor.
     * @param ip the link-local address of the neighbor.
     */
    public void addNeighborEntry(MacAddress mac, Inet6Address ip) {
        mNeighborMap.put(ip, new Pair<>(mac, false));
    }

    /**
     * @return the prefix that is announced in the Router Advertisements sent by this object.
     */
    public IpPrefix getPrefix() {
        return mPrefix;
    }

    private ByteBuffer buildPrefixOption() {
        return PrefixInformationOption.build(
                mPrefix, (byte) (PIO_FLAG_ON_LINK | PIO_FLAG_AUTONOMOUS),
                3600 /* valid lifetime */, 3600 /* preferred lifetime */);
    }

    private ByteBuffer buildRdnssOption() {
        return RdnssOption.build(3600/*lifetime, must be at least 120*/, DNS_SERVER);
    }

    private ByteBuffer buildSllaOption(MacAddress srcMac) {
        return LlaOption.build((byte) ICMPV6_ND_OPTION_SLLA, srcMac);
    }

    private ByteBuffer buildRaPacket(MacAddress srcMac, MacAddress dstMac, Inet6Address srcIp) {
        return Ipv6Utils.buildRaPacket(srcMac, dstMac, srcIp, IPV6_ADDR_ALL_NODES_MULTICAST,
                (byte) 0 /*M=0, O=0*/, 3600 /*lifetime*/, 0 /*reachableTime, unspecified*/,
                0/*retransTimer, unspecified*/, buildPrefixOption(), buildRdnssOption(),
                buildSllaOption(srcMac));
    }

    private static void sendResponse(TapPacketReader reader, ByteBuffer buffer) {
        try {
            reader.sendResponse(buffer);
        } catch (IOException e) {
            // Throwing an exception here will crash the test process. Let's stick to logging, as
            // the test will fail either way.
            Log.e(TAG, "Failed to send buffer", e);
        }
    }

    private void replyToRouterSolicitation(TapPacketReader reader, MacAddress dstMac) {
        for (Map.Entry<Inet6Address, Pair<MacAddress, Boolean>> it : mNeighborMap.entrySet()) {
            final boolean isRouter = it.getValue().second;
            if (!isRouter) {
                continue;
            }
            final ByteBuffer raResponse = buildRaPacket(it.getValue().first, dstMac, it.getKey());
            sendResponse(reader, raResponse);
        }
    }

    private void replyToNeighborSolicitation(TapPacketReader reader, MacAddress dstMac,
            Inet6Address dstIp, Inet6Address targetIp) {
        final Pair<MacAddress, Boolean> neighbor = mNeighborMap.get(targetIp);
        if (neighbor == null) {
            return;
        }

        final MacAddress srcMac = neighbor.first;
        final boolean isRouter = neighbor.second;
        int flags = NEIGHBOR_ADVERTISEMENT_FLAG_SOLICITED | NEIGHBOR_ADVERTISEMENT_FLAG_OVERRIDE;
        if (isRouter) {
            flags |= NEIGHBOR_ADVERTISEMENT_FLAG_ROUTER;
        }

        final ByteBuffer tlla = LlaOption.build((byte) ICMPV6_ND_OPTION_TLLA, srcMac);
        final ByteBuffer naResponse = Ipv6Utils.buildNaPacket(srcMac, dstMac, targetIp, dstIp,
                flags, targetIp, tlla);
        sendResponse(reader, naResponse);
    }

    @Override
    protected void replyToPacket(byte[] packet, TapPacketReader reader) {
        final ByteBuffer buf = ByteBuffer.wrap(packet);
        // Messages are filtered by parent class, so it is safe to assume that packet is either an
        // RS or NS.
        final EthernetHeader ethHdr = Struct.parse(EthernetHeader.class, buf);
        final Ipv6Header ipv6Hdr = Struct.parse(Ipv6Header.class, buf);
        final Icmpv6Header icmpv6Header = Struct.parse(Icmpv6Header.class, buf);

        if (icmpv6Header.type == ICMPV6_ROUTER_SOLICITATION) {
            replyToRouterSolicitation(reader, ethHdr.srcMac);
        } else if (icmpv6Header.type == ICMPV6_NEIGHBOR_SOLICITATION) {
            final NsHeader nsHeader = Struct.parse(NsHeader.class, buf);
            replyToNeighborSolicitation(reader, ethHdr.srcMac, ipv6Hdr.srcIp, nsHeader.target);
        }
    }
}
