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

import static com.android.net.module.util.NetworkStackConstants.ICMPV6_ND_OPTION_PIO;

import android.net.IpPrefix;

import androidx.annotation.NonNull;

import com.android.net.module.util.Struct;
import com.android.net.module.util.Struct.Field;
import com.android.net.module.util.Struct.Type;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ICMPv6 prefix information option, as per https://tools.ietf.org/html/rfc4861.
 *
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     Type      |    Length     | Prefix Length |L|A| Reserved1 |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Valid Lifetime                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                       Preferred Lifetime                      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           Reserved2                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                                                               |
 * +                                                               +
 * |                                                               |
 * +                            Prefix                             +
 * |                                                               |
 * +                                                               +
 * |                                                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class PrefixInformationOption extends Struct {
    @Field(order = 0, type = Type.S8)
    public final byte type;
    @Field(order = 1, type = Type.S8)
    public final byte length; // Length in 8-byte units
    @Field(order = 2, type = Type.S8)
    public final byte prefixLen;
    @Field(order = 3, type = Type.S8)
    // On-link flag, Autonomous address configuration flag, 6-reserved bits
    public final byte flags;
    @Field(order = 4, type = Type.U32)
    public final long validLifetime;
    @Field(order = 5, type = Type.U32)
    public final long preferredLifetime;
    @Field(order = 6, type = Type.S32)
    public final int reserved;
    @Field(order = 7, type = Type.ByteArray, arraysize = 16)
    public final byte[] prefix;

    PrefixInformationOption(final byte type, final byte length, final byte prefixLen,
            final byte flags, final long validLifetime, final long preferredLifetime,
            final int reserved, @NonNull final byte[] prefix) {
        this.type = type;
        this.length = length;
        this.prefixLen = prefixLen;
        this.flags = flags;
        this.validLifetime = validLifetime;
        this.preferredLifetime = preferredLifetime;
        this.reserved = reserved;
        this.prefix = prefix;
    }

    /**
     * Build a Prefix Information option from the required specified parameters.
     */
    public static ByteBuffer build(final IpPrefix prefix, final byte flags,
            final long validLifetime, final long preferredLifetime) {
        final PrefixInformationOption option = new PrefixInformationOption(
                (byte) ICMPV6_ND_OPTION_PIO, (byte) 4 /* option len */,
                (byte) prefix.getPrefixLength(), flags, validLifetime, preferredLifetime,
                (int) 0, prefix.getRawAddress());
        return ByteBuffer.wrap(option.writeToBytes(ByteOrder.BIG_ENDIAN));
    }
}
