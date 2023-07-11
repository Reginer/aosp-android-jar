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

package com.android.net.module.util.netlink;

import static com.android.net.module.util.NetworkStackConstants.IPV6_ADDR_LEN;

import android.util.Log;

import androidx.annotation.NonNull;

import com.android.net.module.util.Struct;
import com.android.net.module.util.structs.RdnssOption;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * The Recursive DNS Server Option. RFC 8106.
 *
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     Type      |     Length    |           Reserved            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           Lifetime                            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                                                               |
 * :            Addresses of IPv6 Recursive DNS Servers            :
 * |                                                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class StructNdOptRdnss extends NdOption {
    private static final String TAG = StructNdOptRdnss.class.getSimpleName();
    public static final int TYPE = 25;
    // Length in 8-byte units, only if one IPv6 address included.
    public static final byte MIN_OPTION_LEN = 3;

    public final RdnssOption header;
    @NonNull
    public final Inet6Address[] servers;

    public StructNdOptRdnss(@NonNull final Inet6Address[] servers, long lifetime) {
        super((byte) TYPE, servers.length * 2 + 1);

        Objects.requireNonNull(servers, "Recursive DNS Servers address array must not be null");
        if (servers.length == 0) {
            throw new IllegalArgumentException("DNS server address array must not be empty");
        }

        this.header = new RdnssOption((byte) TYPE, (byte) (servers.length * 2 + 1),
                (short) 0 /* reserved */, lifetime);
        this.servers = servers.clone();
    }

    /**
     * Parses an RDNSS option from a {@link ByteBuffer}.
     *
     * @param buf The buffer from which to parse the option. The buffer's byte order must be
     *            {@link java.nio.ByteOrder#BIG_ENDIAN}.
     * @return the parsed option, or {@code null} if the option could not be parsed successfully.
     */
    public static StructNdOptRdnss parse(@NonNull ByteBuffer buf) {
        if (buf == null || buf.remaining() < MIN_OPTION_LEN * 8) return null;
        try {
            final RdnssOption header = Struct.parse(RdnssOption.class, buf);
            if (header.type != TYPE) {
                throw new IllegalArgumentException("Invalid type " + header.type);
            }
            if (header.length < MIN_OPTION_LEN || (header.length % 2 == 0)) {
                throw new IllegalArgumentException("Invalid length " + header.length);
            }

            final int numOfDnses = (header.length - 1) / 2;
            final Inet6Address[] servers = new Inet6Address[numOfDnses];
            for (int i = 0; i < numOfDnses; i++) {
                byte[] rawAddress = new byte[IPV6_ADDR_LEN];
                buf.get(rawAddress);
                servers[i] = (Inet6Address) InetAddress.getByAddress(rawAddress);
            }
            return new StructNdOptRdnss(servers, header.lifetime);
        } catch (IllegalArgumentException | BufferUnderflowException | UnknownHostException e) {
            // Not great, but better than throwing an exception that might crash the caller.
            // Convention in this package is that null indicates that the option was truncated
            // or malformed, so callers must already handle it.
            Log.d(TAG, "Invalid RDNSS option: " + e);
            return null;
        }
    }

    protected void writeToByteBuffer(ByteBuffer buf) {
        header.writeToByteBuffer(buf);
        for (int i = 0; i < servers.length; i++) {
            buf.put(servers[i].getAddress());
        }
    }

    /** Outputs the wire format of the option to a new big-endian ByteBuffer. */
    public ByteBuffer toByteBuffer() {
        final ByteBuffer buf = ByteBuffer.allocate(Struct.getSize(RdnssOption.class)
                + servers.length * IPV6_ADDR_LEN);
        writeToByteBuffer(buf);
        buf.flip();
        return buf;
    }

    @Override
    @NonNull
    public String toString() {
        final StringJoiner sj = new StringJoiner(",", "[", "]");
        for (int i = 0; i < servers.length; i++) {
            sj.add(servers[i].getHostAddress());
        }
        return String.format("NdOptRdnss(%s,servers:%s)", header.toString(), sj.toString());
    }
}
