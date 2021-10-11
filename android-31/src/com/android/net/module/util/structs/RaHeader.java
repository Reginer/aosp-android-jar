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

/**
 * ICMPv6 Router Advertisement header, follow [Icmpv6Header], as per
 * https://tools.ietf.org/html/rfc4861. This does not contain any option.
 *
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     Type      |     Code      |          Checksum             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | Cur Hop Limit |M|O|  Reserved |       Router Lifetime         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Reachable Time                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                          Retrans Timer                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Options ...
 * +-+-+-+-+-+-+-+-+-+-+-+-
 */
public class RaHeader extends Struct {
    @Field(order = 0, type = Type.S8)
    public final byte hopLimit;
    // "Managed address configuration", "Other configuration" bits, and 6 reserved bits
    @Field(order = 1, type = Type.S8)
    public final byte flags;
    @Field(order = 2, type = Type.U16)
    public final int lifetime;
    @Field(order = 3, type = Type.U32)
    public final long reachableTime;
    @Field(order = 4, type = Type.U32)
    public final long retransTimer;

    public RaHeader(final byte hopLimit, final byte flags, final int lifetime,
            final long reachableTime, final long retransTimer) {
        this.hopLimit = hopLimit;
        this.flags = flags;
        this.lifetime = lifetime;
        this.reachableTime = reachableTime;
        this.retransTimer = retransTimer;
    }
}
