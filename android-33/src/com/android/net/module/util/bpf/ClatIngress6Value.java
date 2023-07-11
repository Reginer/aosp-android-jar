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

import java.net.Inet4Address;

/** Value type for clat ingress IPv6 maps. */
public class ClatIngress6Value extends Struct {
    @Field(order = 0, type = Type.U32)
    public final long oif; // The output interface to redirect to (0 means don't redirect)

    @Field(order = 1, type = Type.Ipv4Address)
    public final Inet4Address local4; // The destination IPv4 address

    public ClatIngress6Value(final long oif, final Inet4Address local4) {
        this.oif = oif;
        this.local4 = local4;
    }
}
