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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/** Value type for downstream & upstream IPv4 forwarding maps. */
public class Tether4Value extends Struct {
    @Field(order = 0, type = Type.U32)
    public final long oif;

    // The ethhdr struct which is defined in uapi/linux/if_ether.h
    @Field(order = 1, type = Type.EUI48)
    public final MacAddress ethDstMac;
    @Field(order = 2, type = Type.EUI48)
    public final MacAddress ethSrcMac;
    @Field(order = 3, type = Type.UBE16)
    public final int ethProto;  // Packet type ID field.

    @Field(order = 4, type = Type.U16)
    public final int pmtu;

    @Field(order = 5, type = Type.ByteArray, arraysize = 16)
    public final byte[] src46;

    @Field(order = 6, type = Type.ByteArray, arraysize = 16)
    public final byte[] dst46;

    @Field(order = 7, type = Type.UBE16)
    public final int srcPort;

    @Field(order = 8, type = Type.UBE16)
    public final int dstPort;

    // TODO: consider using U64.
    @Field(order = 9, type = Type.U63)
    public final long lastUsed;

    public Tether4Value(final long oif, @NonNull final MacAddress ethDstMac,
            @NonNull final MacAddress ethSrcMac, final int ethProto, final int pmtu,
            final byte[] src46, final byte[] dst46, final int srcPort,
            final int dstPort, final long lastUsed) {
        Objects.requireNonNull(ethDstMac);
        Objects.requireNonNull(ethSrcMac);

        this.oif = oif;
        this.ethDstMac = ethDstMac;
        this.ethSrcMac = ethSrcMac;
        this.ethProto = ethProto;
        this.pmtu = pmtu;
        this.src46 = src46;
        this.dst46 = dst46;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.lastUsed = lastUsed;
    }

    @Override
    public String toString() {
        try {
            return String.format(
                    "oif: %d, ethDstMac: %s, ethSrcMac: %s, ethProto: %d, pmtu: %d, "
                            + "src46: %s, dst46: %s, srcPort: %d, dstPort: %d, "
                            + "lastUsed: %d",
                    oif, ethDstMac, ethSrcMac, ethProto, pmtu,
                    InetAddress.getByAddress(src46), InetAddress.getByAddress(dst46),
                    Short.toUnsignedInt((short) srcPort), Short.toUnsignedInt((short) dstPort),
                    lastUsed);
        } catch (UnknownHostException | IllegalArgumentException e) {
            return String.format("Invalid IP address", e);
        }
    }
}
