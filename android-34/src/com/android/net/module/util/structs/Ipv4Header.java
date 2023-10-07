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

import androidx.annotation.VisibleForTesting;

import com.android.net.module.util.Struct;
import com.android.net.module.util.Struct.Field;
import com.android.net.module.util.Struct.Type;

import java.net.Inet4Address;

/**
 * L3 IPv4 header as per https://tools.ietf.org/html/rfc791.
 * This class doesn't contain options field.
 *
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |Version|  IHL  |Type of Service|          Total Length         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         Identification        |Flags|      Fragment Offset    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Time to Live |    Protocol   |         Header Checksum       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                       Source Address                          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    Destination Address                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class Ipv4Header extends Struct {
    // IP Version=IPv4, IHL is always 5(*4bytes) because options are not supported.
    @VisibleForTesting
    public static final byte IPHDR_VERSION_IHL = 0x45;

    @Field(order = 0, type = Type.S8)
    // version (4 bits), IHL (4 bits)
    public final byte vi;
    @Field(order = 1, type = Type.S8)
    public final byte tos;
    @Field(order = 2, type = Type.U16)
    public final int totalLength;
    @Field(order = 3, type = Type.S16)
    public final short id;
    @Field(order = 4, type = Type.S16)
    // flags (3 bits), fragment offset (13 bits)
    public final short flagsAndFragmentOffset;
    @Field(order = 5, type = Type.U8)
    public final short ttl;
    @Field(order = 6, type = Type.S8)
    public final byte protocol;
    @Field(order = 7, type = Type.S16)
    public final short checksum;
    @Field(order = 8, type = Type.Ipv4Address)
    public final Inet4Address srcIp;
    @Field(order = 9, type = Type.Ipv4Address)
    public final Inet4Address dstIp;

    public Ipv4Header(final byte tos, final int totalLength, final short id,
            final short flagsAndFragmentOffset, final short ttl, final byte protocol,
            final short checksum, final Inet4Address srcIp, final Inet4Address dstIp) {
        this(IPHDR_VERSION_IHL, tos, totalLength, id, flagsAndFragmentOffset, ttl,
                protocol, checksum, srcIp, dstIp);
    }

    private Ipv4Header(final byte vi, final byte tos, final int totalLength, final short id,
            final short flagsAndFragmentOffset, final short ttl, final byte protocol,
            final short checksum, final Inet4Address srcIp, final Inet4Address dstIp) {
        this.vi = vi;
        this.tos = tos;
        this.totalLength = totalLength;
        this.id = id;
        this.flagsAndFragmentOffset = flagsAndFragmentOffset;
        this.ttl = ttl;
        this.protocol = protocol;
        this.checksum = checksum;
        this.srcIp = srcIp;
        this.dstIp = dstIp;
    }
}
