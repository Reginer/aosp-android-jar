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

package android.net;

import static android.net.InvalidPacketException.ERROR_INVALID_IP_ADDRESS;
import static android.net.InvalidPacketException.ERROR_INVALID_PORT;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.system.OsConstants;

import com.android.net.module.util.IpUtils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/** @hide */
@SystemApi
public final class NattKeepalivePacketData extends KeepalivePacketData implements Parcelable {
    private static final int IPV4_HEADER_LENGTH = 20;
    private static final int IPV6_HEADER_LENGTH = 40;
    private static final int UDP_HEADER_LENGTH = 8;

    // This should only be constructed via static factory methods, such as
    // nattKeepalivePacket
    public NattKeepalivePacketData(@NonNull InetAddress srcAddress, int srcPort,
            @NonNull InetAddress dstAddress, int dstPort, @NonNull byte[] data) throws
            InvalidPacketException {
        super(srcAddress, srcPort, dstAddress, dstPort, data);
    }

    /**
     * Factory method to create Nat-T keepalive packet structure.
     * @hide
     */
    public static NattKeepalivePacketData nattKeepalivePacket(
            InetAddress srcAddress, int srcPort, InetAddress dstAddress, int dstPort)
            throws InvalidPacketException {
        if (dstPort != NattSocketKeepalive.NATT_PORT) {
            throw new InvalidPacketException(ERROR_INVALID_PORT);
        }

        // Convert IPv4 mapped v6 address to v4 if any.
        final InetAddress srcAddr, dstAddr;
        try {
            srcAddr = InetAddress.getByAddress(srcAddress.getAddress());
            dstAddr = InetAddress.getByAddress(dstAddress.getAddress());
        } catch (UnknownHostException e) {
            throw new InvalidPacketException(ERROR_INVALID_IP_ADDRESS);
        }

        if (srcAddr instanceof Inet4Address && dstAddr instanceof Inet4Address) {
            return nattKeepalivePacketv4(
                    (Inet4Address) srcAddr, srcPort, (Inet4Address) dstAddr, dstPort);
        } else if (srcAddr instanceof Inet6Address && dstAddr instanceof Inet6Address) {
            return nattKeepalivePacketv6(
                    (Inet6Address) srcAddr, srcPort, (Inet6Address) dstAddr, dstPort);
        } else {
            // Destination address and source address should be the same IP family.
            throw new InvalidPacketException(ERROR_INVALID_IP_ADDRESS);
        }
    }

    private static NattKeepalivePacketData nattKeepalivePacketv4(
            Inet4Address srcAddress, int srcPort, Inet4Address dstAddress, int dstPort)
            throws InvalidPacketException {
        int length = IPV4_HEADER_LENGTH + UDP_HEADER_LENGTH + 1;
        final ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 0x4500);                       // IP version and TOS
        buf.putShort((short) length);
        buf.putShort((short) 0);                            // ID
        buf.putShort((short) 0x4000);                       // Flags(DF), offset
        // Technically speaking, this should be reading/using the v4 sysctl
        // /proc/sys/net/ipv4/ip_default_ttl. Use hard-coded 64 for simplicity.
        buf.put((byte) 64);                                 // TTL
        buf.put((byte) OsConstants.IPPROTO_UDP);
        final int ipChecksumOffset = buf.position();
        buf.putShort((short) 0);                            // IP checksum
        buf.put(srcAddress.getAddress());
        buf.put(dstAddress.getAddress());
        buf.putShort((short) srcPort);
        buf.putShort((short) dstPort);
        buf.putShort((short) (UDP_HEADER_LENGTH + 1));      // UDP length
        final int udpChecksumOffset = buf.position();
        buf.putShort((short) 0);                            // UDP checksum
        buf.put((byte) 0xff);                               // NAT-T keepalive
        buf.putShort(ipChecksumOffset, IpUtils.ipChecksum(buf, 0));
        buf.putShort(udpChecksumOffset, IpUtils.udpChecksum(buf, 0, IPV4_HEADER_LENGTH));

        return new NattKeepalivePacketData(srcAddress, srcPort, dstAddress, dstPort, buf.array());
    }

    private static NattKeepalivePacketData nattKeepalivePacketv6(
            Inet6Address srcAddress, int srcPort, Inet6Address dstAddress, int dstPort)
            throws InvalidPacketException {
        final ByteBuffer buf = ByteBuffer.allocate(IPV6_HEADER_LENGTH + UDP_HEADER_LENGTH + 1);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putInt(0x60000000);                         // IP version, traffic class and flow label
        buf.putShort((short) (UDP_HEADER_LENGTH + 1));  // Payload length
        buf.put((byte) OsConstants.IPPROTO_UDP);        // Next header
        // For native ipv6, this hop limit value should use the per interface v6 hoplimit sysctl.
        // For 464xlat, this value should use the v4 ttl sysctl.
        // Either way, for simplicity, just hard code 64.
        buf.put((byte) 64);                             // Hop limit
        buf.put(srcAddress.getAddress());
        buf.put(dstAddress.getAddress());
        // UDP
        buf.putShort((short) srcPort);
        buf.putShort((short) dstPort);
        buf.putShort((short) (UDP_HEADER_LENGTH + 1));  // UDP length = Payload length
        final int udpChecksumOffset = buf.position();
        buf.putShort((short) 0);                        // UDP checksum
        buf.put((byte) 0xff);                           // NAT-T keepalive. 1 byte of data
        buf.putShort(udpChecksumOffset, IpUtils.udpChecksum(buf, 0, IPV6_HEADER_LENGTH));
        return new NattKeepalivePacketData(srcAddress, srcPort, dstAddress, dstPort, buf.array());
    }
    /** Parcelable Implementation */
    public int describeContents() {
        return 0;
    }

    /** Write to parcel */
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeString(getSrcAddress().getHostAddress());
        out.writeString(getDstAddress().getHostAddress());
        out.writeInt(getSrcPort());
        out.writeInt(getDstPort());
    }

    /** Parcelable Creator */
    public static final @NonNull Parcelable.Creator<NattKeepalivePacketData> CREATOR =
            new Parcelable.Creator<NattKeepalivePacketData>() {
                public NattKeepalivePacketData createFromParcel(Parcel in) {
                    final InetAddress srcAddress =
                            InetAddresses.parseNumericAddress(in.readString());
                    final InetAddress dstAddress =
                            InetAddresses.parseNumericAddress(in.readString());
                    final int srcPort = in.readInt();
                    final int dstPort = in.readInt();
                    try {
                        return NattKeepalivePacketData.nattKeepalivePacket(srcAddress, srcPort,
                                    dstAddress, dstPort);
                    } catch (InvalidPacketException e) {
                        throw new IllegalArgumentException(
                                "Invalid NAT-T keepalive data: " + e.getError());
                    }
                }

                public NattKeepalivePacketData[] newArray(int size) {
                    return new NattKeepalivePacketData[size];
                }
            };

    @Override
    public boolean equals(@Nullable final Object o) {
        if (!(o instanceof NattKeepalivePacketData)) return false;
        final NattKeepalivePacketData other = (NattKeepalivePacketData) o;
        final InetAddress srcAddress = getSrcAddress();
        final InetAddress dstAddress = getDstAddress();
        return srcAddress.equals(other.getSrcAddress())
            && dstAddress.equals(other.getDstAddress())
            && getSrcPort() == other.getSrcPort()
            && getDstPort() == other.getDstPort();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSrcAddress(), getDstAddress(), getSrcPort(), getDstPort());
    }
}
