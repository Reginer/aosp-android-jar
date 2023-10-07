/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static android.system.OsConstants.ICMP6_ECHO_REPLY;
import static android.system.OsConstants.ICMP6_ECHO_REQUEST;

import android.annotation.NonNull;
import android.net.TestNetworkInterface;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Objects;

/**
 * A class that echoes packets received on a {@link TestNetworkInterface} back to itself.
 *
 * For testing purposes, sometimes a mocked environment to simulate a simple echo from the
 * server side is needed. This is particularly useful if the test, e.g. VpnTest, is
 * heavily relying on the outside world.
 *
 * This class reads packets from the {@link FileDescriptor} of a {@link TestNetworkInterface}, and:
 *   1. For TCP and UDP packets, simply swaps the source address and the destination
 *      address, then send it back to the {@link FileDescriptor}.
 *   2. For ICMP ping packets, composes a ping reply and sends it back to the sender.
 *   3. Ignore all other packets.
 */
public class PacketReflector extends Thread {

    static final int IPV4_HEADER_LENGTH = 20;
    static final int IPV6_HEADER_LENGTH = 40;

    static final int IPV4_ADDR_OFFSET = 12;
    static final int IPV6_ADDR_OFFSET = 8;
    static final int IPV4_ADDR_LENGTH = 4;
    static final int IPV6_ADDR_LENGTH = 16;

    static final int IPV4_PROTO_OFFSET = 9;
    static final int IPV6_PROTO_OFFSET = 6;

    static final byte IPPROTO_ICMP = 1;
    static final byte IPPROTO_TCP = 6;
    static final byte IPPROTO_UDP = 17;
    private static final byte IPPROTO_ICMPV6 = 58;

    private static final int ICMP_HEADER_LENGTH = 8;
    static final int TCP_HEADER_LENGTH = 20;
    static final int UDP_HEADER_LENGTH = 8;

    private static final byte ICMP_ECHO = 8;
    private static final byte ICMP_ECHOREPLY = 0;

    private static String TAG = "PacketReflector";

    @NonNull
    private final FileDescriptor mFd;
    @NonNull
    private final byte[] mBuf;

    /**
     * Construct a {@link PacketReflector} from the given {@code fd} of
     * a {@link TestNetworkInterface}.
     *
     * @param fd {@link FileDescriptor} to read/write packets.
     * @param mtu MTU of the test network.
     */
    public PacketReflector(@NonNull FileDescriptor fd, int mtu) {
        super("PacketReflector");
        mFd = Objects.requireNonNull(fd);
        mBuf = new byte[mtu];
    }

    private static void swapBytes(@NonNull byte[] buf, int pos1, int pos2, int len) {
        for (int i = 0; i < len; i++) {
            byte b = buf[pos1 + i];
            buf[pos1 + i] = buf[pos2 + i];
            buf[pos2 + i] = b;
        }
    }

    private static void swapAddresses(@NonNull byte[] buf, int version) {
        int addrPos, addrLen;
        switch (version) {
            case 4:
                addrPos = IPV4_ADDR_OFFSET;
                addrLen = IPV4_ADDR_LENGTH;
                break;
            case 6:
                addrPos = IPV6_ADDR_OFFSET;
                addrLen = IPV6_ADDR_LENGTH;
                break;
            default:
                throw new IllegalArgumentException();
        }
        swapBytes(buf, addrPos, addrPos + addrLen, addrLen);
    }

    // Reflect TCP packets: swap the source and destination addresses, but don't change the ports.
    // This is used by the test to "connect to itself" through the VPN.
    private void processTcpPacket(@NonNull byte[] buf, int version, int len, int hdrLen) {
        if (len < hdrLen + TCP_HEADER_LENGTH) {
            return;
        }

        // Swap src and dst IP addresses.
        swapAddresses(buf, version);

        // Send the packet back.
        writePacket(buf, len);
    }

    // Echo UDP packets: swap source and destination addresses, and source and destination ports.
    // This is used by the test to check that the bytes it sends are echoed back.
    private void processUdpPacket(@NonNull byte[] buf, int version, int len, int hdrLen) {
        if (len < hdrLen + UDP_HEADER_LENGTH) {
            return;
        }

        // Swap src and dst IP addresses.
        swapAddresses(buf, version);

        // Swap dst and src ports.
        int portOffset = hdrLen;
        swapBytes(buf, portOffset, portOffset + 2, 2);

        // Send the packet back.
        writePacket(buf, len);
    }

    private void processIcmpPacket(@NonNull byte[] buf, int version, int len, int hdrLen) {
        if (len < hdrLen + ICMP_HEADER_LENGTH) {
            return;
        }

        byte type = buf[hdrLen];
        if (!(version == 4 && type == ICMP_ECHO) &&
                !(version == 6 && type == (byte) ICMP6_ECHO_REQUEST)) {
            return;
        }

        // Save the ping packet we received.
        byte[] request = buf.clone();

        // Swap src and dst IP addresses, and send the packet back.
        // This effectively pings the device to see if it replies.
        swapAddresses(buf, version);
        writePacket(buf, len);

        // The device should have replied, and buf should now contain a ping response.
        int received = PacketReflectorUtil.readPacket(mFd, buf);
        if (received != len) {
            Log.i(TAG, "Reflecting ping did not result in ping response: " +
                    "read=" + received + " expected=" + len);
            return;
        }

        byte replyType = buf[hdrLen];
        if ((type == ICMP_ECHO && replyType != ICMP_ECHOREPLY)
                || (type == (byte) ICMP6_ECHO_REQUEST && replyType != (byte) ICMP6_ECHO_REPLY)) {
            Log.i(TAG, "Received unexpected ICMP reply: original " + type
                    + ", reply " + replyType);
            return;
        }

        // Compare the response we got with the original packet.
        // The only thing that should have changed are addresses, type and checksum.
        // Overwrite them with the received bytes and see if the packet is otherwise identical.
        request[hdrLen] = buf[hdrLen];          // Type
        request[hdrLen + 2] = buf[hdrLen + 2];  // Checksum byte 1.
        request[hdrLen + 3] = buf[hdrLen + 3];  // Checksum byte 2.

        // Since Linux kernel 4.2, net.ipv6.auto_flowlabels is set by default, and therefore
        // the request and reply may have different IPv6 flow label: ignore that as well.
        if (version == 6) {
            request[1] = (byte) (request[1] & 0xf0 | buf[1] & 0x0f);
            request[2] = buf[2];
            request[3] = buf[3];
        }

        for (int i = 0; i < len; i++) {
            if (buf[i] != request[i]) {
                Log.i(TAG, "Received non-matching packet when expecting ping response.");
                return;
            }
        }

        // Now swap the addresses again and reflect the packet. This sends a ping reply.
        swapAddresses(buf, version);
        writePacket(buf, len);
    }

    private void writePacket(@NonNull byte[] buf, int len) {
        try {
            Os.write(mFd, buf, 0, len);
        } catch (ErrnoException | IOException e) {
            Log.e(TAG, "Error writing packet: " + e.getMessage());
        }
    }

    // Reads one packet from our mFd, and possibly writes the packet back.
    private void processPacket() {
        int len = PacketReflectorUtil.readPacket(mFd, mBuf);
        if (len < 1) {
            // Usually happens when socket read is being interrupted, e.g. stopping PacketReflector.
            return;
        }

        int version = mBuf[0] >> 4;
        int protoPos, hdrLen;
        if (version == 4) {
            hdrLen = IPV4_HEADER_LENGTH;
            protoPos = IPV4_PROTO_OFFSET;
        } else if (version == 6) {
            hdrLen = IPV6_HEADER_LENGTH;
            protoPos = IPV6_PROTO_OFFSET;
        } else {
            throw new IllegalStateException("Unexpected version: " + version);
        }

        if (len < hdrLen) {
            throw new IllegalStateException("Unexpected buffer length: " + len);
        }

        byte proto = mBuf[protoPos];
        switch (proto) {
            case IPPROTO_ICMP:
                // fall through
            case IPPROTO_ICMPV6:
                processIcmpPacket(mBuf, version, len, hdrLen);
                break;
            case IPPROTO_TCP:
                processTcpPacket(mBuf, version, len, hdrLen);
                break;
            case IPPROTO_UDP:
                processUdpPacket(mBuf, version, len, hdrLen);
                break;
        }
    }

    public void run() {
        Log.i(TAG, "starting fd=" + mFd + " valid=" + mFd.valid());
        while (!interrupted() && mFd.valid()) {
            processPacket();
        }
        Log.i(TAG, "exiting fd=" + mFd + " valid=" + mFd.valid());
    }
}
