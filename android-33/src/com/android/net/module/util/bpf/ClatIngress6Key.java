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

/** Key type for clat ingress IPv6 maps. */
public class ClatIngress6Key extends Struct {
    @Field(order = 0, type = Type.U32)
    public final long iif; // The input interface index

    @Field(order = 1, type = Type.Ipv6Address)
    public final Inet6Address pfx96; // The source /96 nat64 prefix, bottom 32 bits must be 0

    @Field(order = 2, type = Type.Ipv6Address)
    public final Inet6Address local6; // The full 128-bits of the destination IPv6 address

    public ClatIngress6Key(final long iif, final Inet6Address pfx96, final Inet6Address local6) {
        this.iif = iif;
        this.pfx96 = pfx96;
        this.local6 = local6;
    }
}
