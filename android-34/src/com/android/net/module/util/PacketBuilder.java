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

import static android.system.OsConstants.IPPROTO_IP;
import static android.system.OsConstants.IPPROTO_IPV6;
import static android.system.OsConstants.IPPROTO_TCP;
import static android.system.OsConstants.IPPROTO_UDP;

import static com.android.net.module.util.IpUtils.ipChecksum;
import static com.android.net.module.util.IpUtils.tcpChecksum;
import static com.android.net.module.util.IpUtils.udpChecksum;
import static com.android.net.module.util.NetworkStackConstants.IPV4_CHECKSUM_OFFSET;
import static com.android.net.module.util.NetworkStackConstants.IPV4_LENGTH_OFFSET;
import static com.android.net.module.util.NetworkStackConstants.IPV6_HEADER_LEN;
import static com.android.net.module.util.NetworkStackConstants.IPV6_LEN_OFFSET;
import static com.android.net.module.util.NetworkStackConstants.TCP_CHECKSUM_OFFSET;
import static com.android.net.module.util.NetworkStackConstants.UDP_CHECKSUM_OFFSET;
import static com.android.net.module.util.NetworkStackConstants.UDP_LENGTH_OFFSET;

import android.net.MacAddress;

import androidx.annotation.NonNull;

import com.android.net.module.util.structs.EthernetHeader;
import com.android.net.module.util.structs.Ipv4Header;
import com.android.net.module.util.structs.Ipv6Header;
import com.android.net.module.util.structs.TcpHeader;
import com.android.net.module.util.structs.UdpHeader;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * The class is used to build a packet.
 *
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                Layer 2 header (EthernetHeader)                | (optional)
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           Layer 3 header (Ipv4Header, Ipv6Header)             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           Layer 4 header (TcpHeader, UdpHeader)               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           Payload                             | (optional)
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * Below is a sample code to build a packet.
 *
 * // Initialize builder
 * final ByteBuffer buf = ByteBuffer.allocate(...);
 * final PacketBuilder pb = new PacketBuilder(buf);
 * // Write headers
 * pb.writeL2Header(...);
 * pb.writeIpHeader(...);
 * pb.writeTcpHeader(...);
 * // Write payload
 * buf.putInt(...);
 * buf.putShort(...);
 * buf.putByte(...);
 * // Finalize and use the packet
 * pb.finalizePacket();
 * sendPacket(buf);
 */
public class PacketBuilder {
    private static final int INVALID_OFFSET = -1;

    private final ByteBuffer mBuffer;

    private int mIpv4HeaderOffset = INVALID_OFFSET;
    private int mIpv6HeaderOffset = INVALID_OFFSET;
    private int mTcpHeaderOffset = INVALID_OFFSET;
    private int mUdpHeaderOffset = INVALID_OFFSET;

    public PacketBuilder(@NonNull ByteBuffer buffer) {
        mBuffer = buffer;
    }

    /**
     * Write an ethernet header.
     *
     * @param srcMac source MAC address
     * @param dstMac destination MAC address
     * @param etherType ether type
     */
    public void writeL2Header(MacAddress srcMac, MacAddress dstMac, short etherType) throws
            IOException {
        final EthernetHeader ethHeader = new EthernetHeader(dstMac, srcMac, etherType);
        try {
            ethHeader.writeToByteBuffer(mBuffer);
        } catch (IllegalArgumentException | BufferOverflowException e) {
            throw new IOException("Error writing to buffer: ", e);
        }
    }

    /**
     * Write an IPv4 header.
     * The IP header length and checksum are calculated and written back in #finalizePacket.
     *
     * @param tos type of service
     * @param id the identification
     * @param flagsAndFragmentOffset flags and fragment offset
     * @param ttl time to live
     * @param protocol protocol
     * @param srcIp source IP address
     * @param dstIp destination IP address
     */
    public void writeIpv4Header(byte tos, short id, short flagsAndFragmentOffset, byte ttl,
            byte protocol, @NonNull final Inet4Address srcIp, @NonNull final Inet4Address dstIp)
            throws IOException {
        mIpv4HeaderOffset = mBuffer.position();
        final Ipv4Header ipv4Header = new Ipv4Header(tos,
                (short) 0 /* totalLength, calculate in #finalizePacket */, id,
                flagsAndFragmentOffset, ttl, protocol,
                (short) 0 /* checksum, calculate in #finalizePacket */, srcIp, dstIp);

        try {
            ipv4Header.writeToByteBuffer(mBuffer);
        } catch (IllegalArgumentException | BufferOverflowException e) {
            throw new IOException("Error writing to buffer: ", e);
        }
    }

    /**
     * Write an IPv6 header.
     * The IP header length is calculated and written back in #finalizePacket.
     *
     * @param vtf version, traffic class and flow label
     * @param nextHeader the transport layer protocol
     * @param hopLimit hop limit
     * @param srcIp source IP address
     * @param dstIp destination IP address
     */
    public void writeIpv6Header(int vtf, byte nextHeader, short hopLimit,
            @NonNull final Inet6Address srcIp, @NonNull final Inet6Address dstIp)
            throws IOException {
        mIpv6HeaderOffset = mBuffer.position();
        final Ipv6Header ipv6Header = new Ipv6Header(vtf,
                (short) 0 /* payloadLength, calculate in #finalizePacket */, nextHeader,
                hopLimit, srcIp, dstIp);

        try {
            ipv6Header.writeToByteBuffer(mBuffer);
        } catch (IllegalArgumentException | BufferOverflowException e) {
            throw new IOException("Error writing to buffer: ", e);
        }
    }

    /**
     * Write a TCP header.
     * The TCP header checksum is calculated and written back in #finalizePacket.
     *
     * @param srcPort source port
     * @param dstPort destination port
     * @param seq sequence number
     * @param ack acknowledgement number
     * @param tcpFlags tcp flags
     * @param window window size
     * @param urgentPointer urgent pointer
     */
    public void writeTcpHeader(short srcPort, short dstPort, short seq, short ack,
            byte tcpFlags, short window, short urgentPointer) throws IOException {
        mTcpHeaderOffset = mBuffer.position();
        final TcpHeader tcpHeader = new TcpHeader(srcPort, dstPort, seq, ack,
                (short) ((short) 0x5000 | ((byte) 0x3f & tcpFlags)) /* dataOffsetAndControlBits,
                dataOffset is always 5(*4bytes) because options not supported */, window,
                (short) 0 /* checksum, calculate in #finalizePacket */,
                urgentPointer);

        try {
            tcpHeader.writeToByteBuffer(mBuffer);
        } catch (IllegalArgumentException | BufferOverflowException e) {
            throw new IOException("Error writing to buffer: ", e);
        }
    }

    /**
     * Write a UDP header.
     * The UDP header length and checksum are calculated and written back in #finalizePacket.
     *
     * @param srcPort source port
     * @param dstPort destination port
     */
    public void writeUdpHeader(short srcPort, short dstPort) throws IOException {
        mUdpHeaderOffset = mBuffer.position();
        final UdpHeader udpHeader = new UdpHeader(srcPort, dstPort,
                (short) 0 /* length, calculate in #finalizePacket */,
                (short) 0 /* checksum, calculate in #finalizePacket */);

        try {
            udpHeader.writeToByteBuffer(mBuffer);
        } catch (IllegalArgumentException | BufferOverflowException e) {
            throw new IOException("Error writing to buffer: ", e);
        }
    }

    /**
     * Finalize the packet.
     *
     * Call after writing L4 header (no payload) or payload to the buffer used by the builder.
     * L3 header length, L3 header checksum and L4 header checksum are calculated and written back
     * after finalization.
     */
    @NonNull
    public ByteBuffer finalizePacket() throws IOException {
        // [1] Finalize IPv4 or IPv6 header.
        int ipHeaderOffset = INVALID_OFFSET;
        if (mIpv4HeaderOffset != INVALID_OFFSET) {
            ipHeaderOffset = mIpv4HeaderOffset;

            // Populate the IPv4 totalLength field.
            mBuffer.putShort(mIpv4HeaderOffset + IPV4_LENGTH_OFFSET,
                    (short) (mBuffer.position() - mIpv4HeaderOffset));

            // Populate the IPv4 header checksum field.
            mBuffer.putShort(mIpv4HeaderOffset + IPV4_CHECKSUM_OFFSET,
                    ipChecksum(mBuffer, mIpv4HeaderOffset /* headerOffset */));
        } else if (mIpv6HeaderOffset != INVALID_OFFSET) {
            ipHeaderOffset = mIpv6HeaderOffset;

            // Populate the IPv6 payloadLength field.
            // The payload length doesn't include IPv6 header length. See rfc8200 section 3.
            mBuffer.putShort(mIpv6HeaderOffset + IPV6_LEN_OFFSET,
                    (short) (mBuffer.position() - mIpv6HeaderOffset - IPV6_HEADER_LEN));
        } else {
            throw new IOException("Packet is missing neither IPv4 nor IPv6 header");
        }

        // [2] Finalize TCP or UDP header.
        if (mTcpHeaderOffset != INVALID_OFFSET) {
            // Populate the TCP header checksum field.
            mBuffer.putShort(mTcpHeaderOffset + TCP_CHECKSUM_OFFSET, tcpChecksum(mBuffer,
                    ipHeaderOffset /* ipOffset */, mTcpHeaderOffset /* transportOffset */,
                    mBuffer.position() - mTcpHeaderOffset /* transportLen */));
        } else if (mUdpHeaderOffset != INVALID_OFFSET) {
            // Populate the UDP header length field.
            mBuffer.putShort(mUdpHeaderOffset + UDP_LENGTH_OFFSET,
                    (short) (mBuffer.position() - mUdpHeaderOffset));

            // Populate the UDP header checksum field.
            mBuffer.putShort(mUdpHeaderOffset + UDP_CHECKSUM_OFFSET, udpChecksum(mBuffer,
                    ipHeaderOffset /* ipOffset */, mUdpHeaderOffset /* transportOffset */));
        } else {
            throw new IOException("Packet is missing neither TCP nor UDP header");
        }

        mBuffer.flip();
        return mBuffer;
    }

    /**
     * Allocate bytebuffer for building the packet.
     *
     * @param hasEther has ethernet header. Set this flag to indicate that the packet has an
     *        ethernet header.
     * @param l3proto the layer 3 protocol. Only {@code IPPROTO_IP} and {@code IPPROTO_IPV6}
     *        currently supported.
     * @param l4proto the layer 4 protocol. Only {@code IPPROTO_TCP} and {@code IPPROTO_UDP}
     *        currently supported.
     * @param payloadLen length of the payload.
     */
    @NonNull
    public static ByteBuffer allocate(boolean hasEther, int l3proto, int l4proto, int payloadLen) {
        if (l3proto != IPPROTO_IP && l3proto != IPPROTO_IPV6) {
            throw new IllegalArgumentException("Unsupported layer 3 protocol " + l3proto);
        }

        if (l4proto != IPPROTO_TCP && l4proto != IPPROTO_UDP) {
            throw new IllegalArgumentException("Unsupported layer 4 protocol " + l4proto);
        }

        if (payloadLen < 0) {
            throw new IllegalArgumentException("Invalid payload length " + payloadLen);
        }

        int packetLen = 0;
        if (hasEther) packetLen += Struct.getSize(EthernetHeader.class);
        packetLen += (l3proto == IPPROTO_IP) ? Struct.getSize(Ipv4Header.class)
                : Struct.getSize(Ipv6Header.class);
        packetLen += (l4proto == IPPROTO_TCP) ? Struct.getSize(TcpHeader.class)
                : Struct.getSize(UdpHeader.class);
        packetLen += payloadLen;

        return ByteBuffer.allocate(packetLen);
    }
}
