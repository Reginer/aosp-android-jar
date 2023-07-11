/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.net.module.util.bpf;

import com.android.net.module.util.Struct;
import com.android.net.module.util.Struct.Field;
import com.android.net.module.util.Struct.Type;

import java.net.Inet6Address;

/** Value type for clat egress IPv4 maps. */
public class ClatEgress4Value extends Struct {
    @Field(order = 0, type = Type.U32)
    public final long oif; // The output interface to redirect to

    @Field(order = 1, type = Type.Ipv6Address)
    public final Inet6Address local6; // The full 128-bits of the source IPv6 address

    @Field(order = 2, type = Type.Ipv6Address)
    public final Inet6Address pfx96; // The destination /96 nat64 prefix, bottom 32 bits must be 0

    @Field(order = 3, type = Type.U8, padding = 3)
    public final short oifIsEthernet; // Whether the output interface requires ethernet header

    public ClatEgress4Value(final long oif, final Inet6Address local6, final Inet6Address pfx96,
            final short oifIsEthernet) {
        this.oif = oif;
        this.local6 = local6;
        this.pfx96 = pfx96;
        this.oifIsEthernet = oifIsEthernet;
    }
}
