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

package com.android.net.module.util.structs;

import static com.android.net.module.util.NetworkStackConstants.ICMPV6_ND_OPTION_RDNSS;

import android.net.InetAddresses;

import com.android.net.module.util.Struct;
import com.android.net.module.util.Struct.Field;
import com.android.net.module.util.Struct.Type;

import java.net.Inet6Address;
import java.nio.ByteBuffer;

/**
 * IPv6 RA recursive DNS server option, as per https://tools.ietf.org/html/rfc8106.
 * This should be followed by a series of DNSv6 server addresses.
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
public class RdnssOption extends Struct {
    @Field(order = 0, type = Type.S8)
    public final byte type;
    @Field(order = 1, type = Type.S8)
    public final byte length; // Length in 8-byte units
    @Field(order = 2, type = Type.S16)
    public final short reserved;
    @Field(order = 3, type = Type.U32)
    public final long lifetime;

    public RdnssOption(final byte type, final byte length, final short reserved,
            final long lifetime) {
        this.type = type;
        this.length = length;
        this.reserved = reserved;
        this.lifetime = lifetime;
    }

    /**
     * Build a RDNSS option from the required specified Inet6Address parameters.
     */
    public static ByteBuffer build(final long lifetime, final Inet6Address... servers) {
        final byte length = (byte) (1 + 2 * servers.length);
        final RdnssOption option = new RdnssOption((byte) ICMPV6_ND_OPTION_RDNSS,
                length, (short) 0, lifetime);
        final ByteBuffer buffer = ByteBuffer.allocate(length * 8);
        option.writeToByteBuffer(buffer);
        for (Inet6Address server : servers) {
            buffer.put(server.getAddress());
        }
        buffer.flip();
        return buffer;
    }

    /**
     * Build a RDNSS option from the required specified String parameters.
     *
     * @throws IllegalArgumentException if {@code servers} does not contain only numeric addresses.
     */
    public static ByteBuffer build(final long lifetime, final String... servers) {
        final Inet6Address[] serverArray = new Inet6Address[servers.length];
        for (int i = 0; i < servers.length; i++) {
            serverArray[i] = (Inet6Address) InetAddresses.parseNumericAddress(servers[i]);
        }
        return build(lifetime, serverArray);
    }
}
