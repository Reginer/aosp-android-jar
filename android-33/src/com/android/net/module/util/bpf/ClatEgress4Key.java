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

/** Key type for clat egress IPv4 maps. */
public class ClatEgress4Key extends Struct {
    @Field(order = 0, type = Type.U32)
    public final long iif; // The input interface index

    @Field(order = 1, type = Type.Ipv4Address)
    public final Inet4Address local4; // The source IPv4 address

    public ClatEgress4Key(final long iif, final Inet4Address local4) {
        this.iif = iif;
        this.local4 = local4;
    }
}
