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

import static com.android.net.module.util.NetworkStackConstants.ICMPV6_ND_OPTION_MTU;

import com.android.net.module.util.Struct;
import com.android.net.module.util.Struct.Field;
import com.android.net.module.util.Struct.Type;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ICMPv6 MTU option, as per https://tools.ietf.org/html/rfc4861.
 *
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     Type      |    Length     |           Reserved            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                              MTU                              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class MtuOption extends Struct {
    @Field(order = 0, type = Type.S8)
    public final byte type;
    @Field(order = 1, type = Type.S8)
    public final byte length; // Length in 8-byte units
    @Field(order = 2, type = Type.S16)
    public final short reserved;
    @Field(order = 3, type = Type.U32)
    public final long mtu;

    MtuOption(final byte type, final byte length, final short reserved,
            final long mtu) {
        this.type = type;
        this.length = length;
        this.reserved = reserved;
        this.mtu = mtu;
    }

    /**
     * Build a MTU option from the required specified parameters.
     */
    public static ByteBuffer build(final long mtu) {
        final MtuOption option = new MtuOption((byte) ICMPV6_ND_OPTION_MTU,
                (byte) 1 /* option len */, (short) 0 /* reserved */, mtu);
        return ByteBuffer.wrap(option.writeToBytes(ByteOrder.BIG_ENDIAN));
    }
}
