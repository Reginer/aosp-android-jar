/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static com.android.testutils.PacketReflector.IPPROTO_TCP;
import static com.android.testutils.PacketReflector.IPPROTO_UDP;
import static com.android.testutils.PacketReflector.IPV4_HEADER_LENGTH;
import static com.android.testutils.PacketReflector.IPV6_HEADER_LENGTH;
import static com.android.testutils.PacketReflector.IPV6_PROTO_OFFSET;
import static com.android.testutils.PacketReflector.TCP_HEADER_LENGTH;
import static com.android.testutils.PacketReflector.UDP_HEADER_LENGTH;

import android.annotation.NonNull;
import android.net.TestNetworkInterface;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import androidx.annotation.GuardedBy;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Objects;

/**
 * A class that forwards packets from a {@link TestNetworkInterface} to another
 * {@link TestNetworkInterface} with NAT.
 *
 * For testing purposes, a {@link TestNetworkInterface} provides a {@link FileDescriptor}
 * which allows content injection on the test network. However, this could be hard to use
 * because the callers need to compose IP packets in order to inject content to the
 * test network.
 *
 * In order to remove the need of composing the IP packets, this class forwards IP packets to
 * the {@link FileDescriptor} of another {@link TestNetworkInterface} instance. Thus,
 * the TCP/IP headers could be parsed/composed automatically by the protocol stack of this
 * additional {@link TestNetworkInterface}, while the payload is supplied by the
 * servers run on the interface.
 *
 * To make it work, an internal interface and an external interface are defined, where
 * the client might send packets from the internal interface which are originated from
 * multiple addresses to a server that listens on the external address.
 *
 * When forwarding the outgoing packet on the internal interface, a simple NAT mechanism
 * is implemented during forwarding, which will swap the source and destination,
 * but replacing the source address with the external address,
 * e.g. 192.168.1.1:1234 -> 8.8.8.8:80 will be translated into 8.8.8.8:1234 -> 1.2.3.4:80.
 *
 * For the above example, a client who sends http request will have a hallucination that
 * it is talking to a remote server at 8.8.8.8. Also, the server listens on 1.2.3.4 will
 * have a different hallucination that the request is sent from a remote client at 8.8.8.8,
 * to a local address 1.2.3.4.
 *
 * And a NAT mapping is created at the time when the outgoing packet is forwarded.
 * With a different internal source port, the instance learned that when a response with the
 * destination port 1234, it should forward the packet to the internal address 192.168.1.1.
 *
 * For the incoming packet received from external interface, for example a http response sent
 * from the http server, the same mechanism is applied but in a different direction,
 * where the source and destination will be swapped, and the source address will be replaced
 * with the internal address, which is obtained from the NAT mapping described above.
 */
public abstract class NatPacketForwarderBase extends Thread {
    private static final String TAG = "NatPacketForwarder";
    static final int DESTINATION_PORT_OFFSET = 2;

    // The source fd to read packets from.
    @NonNull
    final FileDescriptor mSrcFd;
    // The buffer to temporarily hold the entire packet after receiving.
    @NonNull
    final byte[] mBuf;
    // The destination fd to write packets to.
    @NonNull
    final FileDescriptor mDstFd;
    // The NAT mapping table shared between two NatPacketForwarder instances to map from
    // the source port to the associated internal address. The map can be read/write from two
    // different threads on any given time whenever receiving packets on the
    // {@link TestNetworkInterface}. Thus, synchronize on the object when reading/writing is needed.
    @GuardedBy("mNatMap")
    @NonNull
    final PacketBridge.NatMap mNatMap;
    // The address of the external interface. See {@link NatPacketForwarder}.
    @NonNull
    final InetAddress mExtAddr;

    /**
     * Construct a {@link NatPacketForwarderBase}.
     *
     * This class reads packets from {@code srcFd} of a {@link TestNetworkInterface}, and
     * forwards them to the {@code dstFd} of another {@link TestNetworkInterface} with
     * NAT applied. See {@link NatPacketForwarderBase}.
     *
     * To apply NAT, the address of the external interface needs to be supplied through
     * {@code extAddr} to identify the external interface. And a shared NAT mapping table,
     * {@code natMap} is needed to be shared between these two instances.
     *
     * Note that this class is not useful if the instance is not managed by a
     * {@link PacketBridge} to set up a two-way communication.
     *
     * @param srcFd   {@link FileDescriptor} to read packets from.
     * @param mtu     MTU of the test network.
     * @param dstFd   {@link FileDescriptor} to write packets to.
     * @param extAddr the external address, which is the address of the external interface.
     *                See {@link NatPacketForwarderBase}.
     * @param natMap  the NAT mapping table shared between two {@link NatPacketForwarderBase}
     *                instance.
     */
    public NatPacketForwarderBase(@NonNull FileDescriptor srcFd, int mtu,
            @NonNull FileDescriptor dstFd, @NonNull InetAddress extAddr,
            @NonNull PacketBridge.NatMap natMap) {
        super(TAG);
        mSrcFd = Objects.requireNonNull(srcFd);
        mBuf = new byte[mtu];
        mDstFd = Objects.requireNonNull(dstFd);
        mExtAddr = Objects.requireNonNull(extAddr);
        mNatMap = Objects.requireNonNull(natMap);
    }

    /**
     * A method to prepare forwarding packets between two instances of {@link TestNetworkInterface},
     * which includes re-write addresses, ports and fix up checksums.
     * Subclasses should override this method to implement a simple NAT.
     */
    abstract void preparePacketForForwarding(@NonNull byte[] buf, int len, int version, int proto);

    private void forwardPacket(@NonNull byte[] buf, int len) {
        try {
            Os.write(mDstFd, buf, 0, len);
        } catch (ErrnoException | IOException e) {
            Log.e(TAG, "Error writing packet: " + e.getMessage());
        }
    }

    // Reads one packet from mSrcFd, and writes the packet to the mDstFd for supported protocols.
    private void processPacket() {
        final int len = PacketReflectorUtil.readPacket(mSrcFd, mBuf);
        if (len < 1) {
            throw new IllegalStateException("Unexpected buffer length: " + len);
        }

        final int version = mBuf[0] >>> 4;
        final int protoPos, ipHdrLen;
        switch (version) {
            case 4:
                ipHdrLen = IPV4_HEADER_LENGTH;
                protoPos = PacketReflector.IPV4_PROTO_OFFSET;
                break;
            case 6:
                ipHdrLen = IPV6_HEADER_LENGTH;
                protoPos = IPV6_PROTO_OFFSET;
                break;
            default:
                throw new IllegalStateException("Unexpected version: " + version);
        }
        if (len < ipHdrLen) {
            throw new IllegalStateException("Unexpected buffer length: " + len);
        }

        final byte proto = mBuf[protoPos];
        final int transportHdrLen;
        switch (proto) {
            case IPPROTO_TCP:
                transportHdrLen = TCP_HEADER_LENGTH;
                break;
            case IPPROTO_UDP:
                transportHdrLen = UDP_HEADER_LENGTH;
                break;
            // TODO: Support ICMP.
            default:
                return; // Unknown protocol, ignored.
        }

        if (len < ipHdrLen + transportHdrLen) {
            throw new IllegalStateException("Unexpected buffer length: " + len);
        }
        // Re-write addresses, ports and fix up checksums.
        preparePacketForForwarding(mBuf, len, version, proto);
        // Send the packet to the destination fd.
        forwardPacket(mBuf, len);
    }

    @Override
    public void run() {
        Log.i(TAG, "starting fd=" + mSrcFd + " valid=" + mSrcFd.valid());
        while (!interrupted() && mSrcFd.valid()) {
            processPacket();
        }
        Log.i(TAG, "exiting fd=" + mSrcFd + " valid=" + mSrcFd.valid());
    }
}
