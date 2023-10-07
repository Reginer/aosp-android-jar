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
 * L4 UDP header as per https://tools.ietf.org/html/rfc768.
 *
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |          Source Port          |       Destination Port        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           Length              |          Checksum             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                          data octets  ...
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ ...
 */
public class UdpHeader extends Struct {
    @Field(order = 0, type = Type.U16)
    public final int srcPort;
    @Field(order = 1, type = Type.U16)
    public final int dstPort;
    @Field(order = 2, type = Type.U16)
    public final int length;
    @Field(order = 3, type = Type.S16)
    public final short checksum;

    public UdpHeader(final int srcPort, final int dstPort, final int length,
            final short checksum) {
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.length = length;
        this.checksum = checksum;
    }
}
