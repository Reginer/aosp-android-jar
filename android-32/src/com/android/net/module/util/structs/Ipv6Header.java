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

import com.android.net.module.util.Struct;
import com.android.net.module.util.Struct.Field;
import com.android.net.module.util.Struct.Type;

import java.net.Inet6Address;

/**
 * L3 IPv6 header as per https://tools.ietf.org/html/rfc8200.
 *
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |Version| Traffic Class |           Flow Label                  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         Payload Length        |  Next Header  |   Hop Limit   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                                                               |
 * +                                                               +
 * |                                                               |
 * +                         Source Address                        +
 * |                                                               |
 * +                                                               +
 * |                                                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                                                               |
 * +                                                               +
 * |                                                               |
 * +                      Destination Address                      +
 * |                                                               |
 * +                                                               +
 * |                                                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class Ipv6Header extends Struct {
    @Field(order = 0, type = Type.S32)
    public int vtf;
    @Field(order = 1, type = Type.U16)
    public int payloadLength;
    @Field(order = 2, type = Type.S8)
    public byte nextHeader;
    @Field(order = 3, type = Type.U8)
    public short hopLimit;
    @Field(order = 4, type = Type.Ipv6Address)
    public Inet6Address srcIp;
    @Field(order = 5, type = Type.Ipv6Address)
    public Inet6Address dstIp;

    public Ipv6Header(final int vtf, final int payloadLength, final byte nextHeader,
            final short hopLimit, final Inet6Address srcIp, final Inet6Address dstIp) {
        this.vtf = vtf;
        this.payloadLength = payloadLength;
        this.nextHeader = nextHeader;
        this.hopLimit = hopLimit;
        this.srcIp = srcIp;
        this.dstIp = dstIp;
    }
}
