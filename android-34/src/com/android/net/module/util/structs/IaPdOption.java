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

package com.android.net.module.util.structs;

import static com.android.net.module.util.NetworkStackConstants.DHCP6_OPTION_IA_PD;

import com.android.net.module.util.Struct;
import com.android.net.module.util.Struct.Field;
import com.android.net.module.util.Struct.Type;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * DHCPv6 IA_PD option.
 * https://tools.ietf.org/html/rfc8415. This does not contain any option.
 *
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         OPTION_IA_PD          |           option-len          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         IAID (4 octets)                       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                              T1                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                              T2                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * .                                                               .
 * .                          IA_PD-options                        .
 * .                                                               .
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 */
public class IaPdOption extends Struct {
    public static final int LENGTH = 12; // option length excluding IA_PD options

    @Field(order = 0, type = Type.S16)
    public final short code;
    @Field(order = 1, type = Type.S16)
    public final short length;
    @Field(order = 2, type = Type.U32)
    public final long id;
    @Field(order = 3, type = Type.U32)
    public final long t1;
    @Field(order = 4, type = Type.U32)
    public final long t2;

    IaPdOption(final short code, final short length, final long id, final long t1,
            final long t2) {
        this.code = code;
        this.length = length;
        this.id = id;
        this.t1 = t1;
        this.t2 = t2;
    }

    /**
     * Build an IA_PD option from the required specific parameters.
     */
    public static ByteBuffer build(final short length, final long id, final long t1,
            final long t2) {
        final IaPdOption option = new IaPdOption((short) DHCP6_OPTION_IA_PD,
                length /* 12 + IA_PD options length */, id, t1, t2);
        return ByteBuffer.wrap(option.writeToBytes(ByteOrder.BIG_ENDIAN));
    }
}
