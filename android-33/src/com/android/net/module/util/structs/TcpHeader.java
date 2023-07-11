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
 * L4 TCP header as per https://tools.ietf.org/html/rfc793.
 * This class does not contain option and data fields.
 *
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |          Source Port          |       Destination Port        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                        Sequence Number                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    Acknowledgment Number                      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Data |           |U|A|P|R|S|F|                               |
 * | Offset| Reserved  |R|C|S|S|Y|I|            Window             |
 * |       |           |G|K|H|T|N|N|                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           Checksum            |         Urgent Pointer        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    Options                    |    Padding    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                             data                              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class TcpHeader extends Struct {
    @Field(order = 0, type = Type.U16)
    public final int srcPort;
    @Field(order = 1, type = Type.U16)
    public final int dstPort;
    @Field(order = 2, type = Type.U32)
    public final long seq;
    @Field(order = 3, type = Type.U32)
    public final long ack;
    @Field(order = 4, type = Type.S16)
    // data Offset (4 bits), reserved (6 bits), control bits (6 bits)
    // TODO: update with bitfields once class Struct supports it
    public final short dataOffsetAndControlBits;
    @Field(order = 5, type = Type.U16)
    public final int window;
    @Field(order = 6, type = Type.S16)
    public final short checksum;
    @Field(order = 7, type = Type.U16)
    public final int urgentPointer;

    public TcpHeader(final int srcPort, final int dstPort, final long seq, final long ack,
            final short dataOffsetAndControlBits, final int window, final short checksum,
            final int urgentPointer) {
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.seq = seq;
        this.ack = ack;
        this.dataOffsetAndControlBits = dataOffsetAndControlBits;
        this.window = window;
        this.checksum = checksum;
        this.urgentPointer = urgentPointer;
    }
}
