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

package com.android.net.module.util;

import static android.system.OsConstants.IPPROTO_ICMPV6;

import static com.android.net.module.util.IpUtils.icmpv6Checksum;
import static com.android.net.module.util.NetworkStackConstants.ETHER_TYPE_IPV6;
import static com.android.net.module.util.NetworkStackConstants.ICMPV6_ECHO_REQUEST_TYPE;
import static com.android.net.module.util.NetworkStackConstants.ICMPV6_NEIGHBOR_ADVERTISEMENT;
import static com.android.net.module.util.NetworkStackConstants.ICMPV6_NEIGHBOR_SOLICITATION;
import static com.android.net.module.util.NetworkStackConstants.ICMPV6_ROUTER_ADVERTISEMENT;
import static com.android.net.module.util.NetworkStackConstants.ICMPV6_ROUTER_SOLICITATION;

import android.net.MacAddress;

import com.android.net.module.util.structs.EthernetHeader;
import com.android.net.module.util.structs.Icmpv6Header;
import com.android.net.module.util.structs.Ipv6Header;
import com.android.net.module.util.structs.NaHeader;
import com.android.net.module.util.structs.NsHeader;
import com.android.net.module.util.structs.RaHeader;
import com.android.net.module.util.structs.RsHeader;

import java.net.Inet6Address;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utilities for IPv6 packets.
 */
public class Ipv6Utils {
    /**
     * Build a generic ICMPv6 packet(e.g., packet used in the neighbor discovery protocol).
     */
    public static ByteBuffer buildIcmpv6Packet(final MacAddress srcMac, final MacAddress dstMac,
            final Inet6Address srcIp, final Inet6Address dstIp, short type, short code,
            final ByteBuffer... options) {
        final int etherHeaderLen = Struct.getSize(EthernetHeader.class);
        final int ipv6HeaderLen = Struct.getSize(Ipv6Header.class);
        final int icmpv6HeaderLen = Struct.getSize(Icmpv6Header.class);
        int payloadLen = 0;
        for (ByteBuffer option: options) {
            payloadLen += option.limit();
        }
        final ByteBuffer packet = ByteBuffer.allocate(etherHeaderLen + ipv6HeaderLen
                + icmpv6HeaderLen + payloadLen);
        final EthernetHeader ethHeader =
                new EthernetHeader(dstMac, srcMac, (short) ETHER_TYPE_IPV6);
        final Ipv6Header ipv6Header =
                new Ipv6Header((int) 0x60000000 /* version, traffic class, flowlabel */,
                icmpv6HeaderLen + payloadLen /* payload length */,
                (byte) IPPROTO_ICMPV6 /* next header */, (byte) 0xff /* hop limit */, srcIp, dstIp);
        final Icmpv6Header icmpv6Header = new Icmpv6Header(type, code, (short) 0 /* checksum */);

        ethHeader.writeToByteBuffer(packet);
        ipv6Header.writeToByteBuffer(packet);
        icmpv6Header.writeToByteBuffer(packet);
        for (ByteBuffer option : options) {
            packet.put(option);
            // in case option might be reused by caller, restore the position and
            // limit of bytebuffer.
            option.clear();
        }
        packet.flip();

        // Populate the ICMPv6 checksum field.
        packet.putShort(etherHeaderLen + ipv6HeaderLen + 2, icmpv6Checksum(packet,
                etherHeaderLen /* ipOffset */,
                (int) (etherHeaderLen + ipv6HeaderLen) /* transportOffset */,
                (short) (icmpv6HeaderLen + payloadLen) /* transportLen */));
        return packet;
    }

    /**
     * Build the ICMPv6 packet payload including payload header and specific options.
     */
    private static ByteBuffer[] buildIcmpv6Payload(final ByteBuffer payloadHeader,
            final ByteBuffer... options) {
        final ByteBuffer[] payload = new ByteBuffer[options.length + 1];
        payload[0] = payloadHeader;
        System.arraycopy(options, 0, payload, 1, options.length);
        return payload;
    }

    /**
     * Build an ICMPv6 Router Advertisement packet from the required specified parameters.
     */
    public static ByteBuffer buildRaPacket(final MacAddress srcMac, final MacAddress dstMac,
            final Inet6Address srcIp, final Inet6Address dstIp, final byte flags,
            final int lifetime, final long reachableTime, final long retransTimer,
            final ByteBuffer... options) {
        final RaHeader raHeader = new RaHeader((byte) 0 /* hopLimit, unspecified */,
                flags, lifetime, reachableTime, retransTimer);
        final ByteBuffer[] payload = buildIcmpv6Payload(
                ByteBuffer.wrap(raHeader.writeToBytes(ByteOrder.BIG_ENDIAN)), options);
        return buildIcmpv6Packet(srcMac, dstMac, srcIp, dstIp,
                (byte) ICMPV6_ROUTER_ADVERTISEMENT /* type */, (byte) 0 /* code */, payload);
    }

    /**
     * Build an ICMPv6 Neighbor Advertisement packet from the required specified parameters.
     */
    public static ByteBuffer buildNaPacket(final MacAddress srcMac, final MacAddress dstMac,
            final Inet6Address srcIp, final Inet6Address dstIp, final int flags,
            final Inet6Address target, final ByteBuffer... options) {
        final NaHeader naHeader = new NaHeader(flags, target);
        final ByteBuffer[] payload = buildIcmpv6Payload(
                ByteBuffer.wrap(naHeader.writeToBytes(ByteOrder.BIG_ENDIAN)), options);
        return buildIcmpv6Packet(srcMac, dstMac, srcIp, dstIp,
                (byte) ICMPV6_NEIGHBOR_ADVERTISEMENT /* type */, (byte) 0 /* code */, payload);
    }

    /**
     * Build an ICMPv6 Neighbor Solicitation packet from the required specified parameters.
     */
    public static ByteBuffer buildNsPacket(final MacAddress srcMac, final MacAddress dstMac,
            final Inet6Address srcIp, final Inet6Address dstIp,
            final Inet6Address target, final ByteBuffer... options) {
        final NsHeader nsHeader = new NsHeader(target);
        final ByteBuffer[] payload = buildIcmpv6Payload(
                ByteBuffer.wrap(nsHeader.writeToBytes(ByteOrder.BIG_ENDIAN)), options);
        return buildIcmpv6Packet(srcMac, dstMac, srcIp, dstIp,
                (byte) ICMPV6_NEIGHBOR_SOLICITATION /* type */, (byte) 0 /* code */, payload);
    }

    /**
     * Build an ICMPv6 Router Solicitation packet from the required specified parameters.
     */
    public static ByteBuffer buildRsPacket(final MacAddress srcMac, final MacAddress dstMac,
            final Inet6Address srcIp, final Inet6Address dstIp, final ByteBuffer... options) {
        final RsHeader rsHeader = new RsHeader((int) 0 /* reserved */);
        final ByteBuffer[] payload = buildIcmpv6Payload(
                ByteBuffer.wrap(rsHeader.writeToBytes(ByteOrder.BIG_ENDIAN)), options);
        return buildIcmpv6Packet(srcMac, dstMac, srcIp, dstIp,
                (byte) ICMPV6_ROUTER_SOLICITATION /* type */, (byte) 0 /* code */, payload);
    }

    /**
     * Build an ICMPv6 Echo Request packet from the required specified parameters.
     */
    public static ByteBuffer buildEchoRequestPacket(final MacAddress srcMac,
            final MacAddress dstMac, final Inet6Address srcIp, final Inet6Address dstIp) {
        final ByteBuffer payload = ByteBuffer.allocate(4); // ID and Sequence number may be zero.
        return buildIcmpv6Packet(srcMac, dstMac, srcIp, dstIp,
                (byte) ICMPV6_ECHO_REQUEST_TYPE /* type */, (byte) 0 /* code */, payload);
    }
}
