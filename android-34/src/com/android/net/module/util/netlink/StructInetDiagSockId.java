/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.net.module.util.netlink;

import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.AF_INET6;

import static com.android.net.module.util.NetworkStackConstants.IPV4_ADDR_LEN;
import static com.android.net.module.util.NetworkStackConstants.IPV6_ADDR_LEN;

import static java.nio.ByteOrder.BIG_ENDIAN;

import android.util.Log;

import androidx.annotation.Nullable;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * struct inet_diag_req_v2
 *
 * see &lt;linux_src&gt;/include/uapi/linux/inet_diag.h
 *
 * struct inet_diag_sockid {
 *        __be16    idiag_sport;
 *        __be16    idiag_dport;
 *        __be32    idiag_src[4];
 *        __be32    idiag_dst[4];
 *        __u32     idiag_if;
 *        __u32     idiag_cookie[2];
 * #define INET_DIAG_NOCOOKIE (~0U)
 * };
 *
 * @hide
 */
public class StructInetDiagSockId {
    private static final String TAG = StructInetDiagSockId.class.getSimpleName();
    public static final int STRUCT_SIZE = 48;

    private static final long INET_DIAG_NOCOOKIE = ~0L;
    private static final byte[] IPV4_PADDING = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public final InetSocketAddress locSocketAddress;
    public final InetSocketAddress remSocketAddress;
    public final int ifIndex;
    public final long cookie;

    public StructInetDiagSockId(InetSocketAddress loc, InetSocketAddress rem) {
        this(loc, rem, 0 /* ifIndex */, INET_DIAG_NOCOOKIE);
    }

    public StructInetDiagSockId(InetSocketAddress loc, InetSocketAddress rem,
            int ifIndex, long cookie) {
        this.locSocketAddress = loc;
        this.remSocketAddress = rem;
        this.ifIndex = ifIndex;
        this.cookie = cookie;
    }

    /**
     * Parse inet diag socket id from buffer.
     */
    @Nullable
    public static StructInetDiagSockId parse(final ByteBuffer byteBuffer, final short family) {
        if (byteBuffer.remaining() < STRUCT_SIZE) {
            return null;
        }

        byteBuffer.order(BIG_ENDIAN);
        final int srcPort = Short.toUnsignedInt(byteBuffer.getShort());
        final int dstPort = Short.toUnsignedInt(byteBuffer.getShort());

        final InetAddress srcAddr;
        final InetAddress dstAddr;
        if (family == AF_INET) {
            final byte[] srcAddrByte = new byte[IPV4_ADDR_LEN];
            final byte[] dstAddrByte = new byte[IPV4_ADDR_LEN];
            byteBuffer.get(srcAddrByte);
            // Address always uses IPV6_ADDR_LEN in the buffer. So if the address is IPv4, position
            // needs to be advanced to the next field.
            byteBuffer.position(byteBuffer.position() + (IPV6_ADDR_LEN - IPV4_ADDR_LEN));
            byteBuffer.get(dstAddrByte);
            byteBuffer.position(byteBuffer.position() + (IPV6_ADDR_LEN - IPV4_ADDR_LEN));
            try {
                srcAddr = Inet4Address.getByAddress(srcAddrByte);
                dstAddr = Inet4Address.getByAddress(dstAddrByte);
            } catch (UnknownHostException e) {
                Log.wtf(TAG, "Failed to parse address: " + e);
                return null;
            }
        } else if (family == AF_INET6) {
            final byte[] srcAddrByte = new byte[IPV6_ADDR_LEN];
            final byte[] dstAddrByte = new byte[IPV6_ADDR_LEN];
            byteBuffer.get(srcAddrByte);
            byteBuffer.get(dstAddrByte);
            try {
                // Using Inet6Address.getByAddress to be consistent with idiag_family field since
                // InetAddress.getByAddress returns Inet4Address if the address is v4-mapped v6
                // address.
                srcAddr = Inet6Address.getByAddress(
                        null /* host */, srcAddrByte, -1 /* scope_id */);
                dstAddr = Inet6Address.getByAddress(
                        null /* host */, dstAddrByte, -1 /* scope_id */);
            } catch (UnknownHostException e) {
                Log.wtf(TAG, "Failed to parse address: " + e);
                return null;
            }
        } else {
            Log.wtf(TAG, "Invalid address family: " + family);
            return null;
        }

        final InetSocketAddress srcSocketAddr = new InetSocketAddress(srcAddr, srcPort);
        final InetSocketAddress dstSocketAddr = new InetSocketAddress(dstAddr, dstPort);

        byteBuffer.order(ByteOrder.nativeOrder());
        final int ifIndex = byteBuffer.getInt();
        final long cookie = byteBuffer.getLong();
        return new StructInetDiagSockId(srcSocketAddr, dstSocketAddr, ifIndex, cookie);
    }

    /**
     * Write inet diag socket id message to ByteBuffer in big endian.
     */
    public void pack(ByteBuffer byteBuffer) {
        byteBuffer.order(BIG_ENDIAN);
        byteBuffer.putShort((short) locSocketAddress.getPort());
        byteBuffer.putShort((short) remSocketAddress.getPort());
        byteBuffer.put(locSocketAddress.getAddress().getAddress());
        if (locSocketAddress.getAddress() instanceof Inet4Address) {
            byteBuffer.put(IPV4_PADDING);
        }
        byteBuffer.put(remSocketAddress.getAddress().getAddress());
        if (remSocketAddress.getAddress() instanceof Inet4Address) {
            byteBuffer.put(IPV4_PADDING);
        }
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.putInt(ifIndex);
        byteBuffer.putLong(cookie);
    }

    @Override
    public String toString() {
        return "StructInetDiagSockId{ "
                + "idiag_sport{" + locSocketAddress.getPort() + "}, "
                + "idiag_dport{" + remSocketAddress.getPort() + "}, "
                + "idiag_src{" + locSocketAddress.getAddress().getHostAddress() + "}, "
                + "idiag_dst{" + remSocketAddress.getAddress().getHostAddress() + "}, "
                + "idiag_if{" + ifIndex + "}, "
                + "idiag_cookie{"
                + (cookie == INET_DIAG_NOCOOKIE ? "INET_DIAG_NOCOOKIE" : cookie) + "}"
                + "}";
    }
}
