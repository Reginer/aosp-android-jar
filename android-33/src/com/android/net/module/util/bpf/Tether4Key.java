/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.net.MacAddress;

import androidx.annotation.NonNull;

import com.android.net.module.util.Struct;
import com.android.net.module.util.Struct.Field;
import com.android.net.module.util.Struct.Type;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Objects;

/** Key type for downstream & upstream IPv4 forwarding maps. */
public class Tether4Key extends Struct {
    @Field(order = 0, type = Type.U32)
    public final long iif;

    @Field(order = 1, type = Type.EUI48)
    public final MacAddress dstMac;

    @Field(order = 2, type = Type.U8, padding = 1)
    public final short l4proto;

    @Field(order = 3, type = Type.ByteArray, arraysize = 4)
    public final byte[] src4;

    @Field(order = 4, type = Type.ByteArray, arraysize = 4)
    public final byte[] dst4;

    @Field(order = 5, type = Type.UBE16)
    public final int srcPort;

    @Field(order = 6, type = Type.UBE16)
    public final int dstPort;

    public Tether4Key(final long iif, @NonNull final MacAddress dstMac, final short l4proto,
            final byte[] src4, final byte[] dst4, final int srcPort,
            final int dstPort) {
        Objects.requireNonNull(dstMac);

        this.iif = iif;
        this.dstMac = dstMac;
        this.l4proto = l4proto;
        this.src4 = src4;
        this.dst4 = dst4;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
    }

    @Override
    public String toString() {
        try {
            return String.format(
                    "iif: %d, dstMac: %s, l4proto: %d, src4: %s, dst4: %s, "
                            + "srcPort: %d, dstPort: %d",
                    iif, dstMac, l4proto,
                    Inet4Address.getByAddress(src4), Inet4Address.getByAddress(dst4),
                    Short.toUnsignedInt((short) srcPort), Short.toUnsignedInt((short) dstPort));
        } catch (UnknownHostException | IllegalArgumentException e) {
            return String.format("Invalid IP address", e);
        }
    }
}
