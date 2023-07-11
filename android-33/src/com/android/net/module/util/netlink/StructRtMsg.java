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

package com.android.net.module.util.netlink;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.net.module.util.Struct;
import com.android.net.module.util.Struct.Field;
import com.android.net.module.util.Struct.Type;

import java.nio.ByteBuffer;

/**
 * struct rtmsg
 *
 * see also:
 *
 *     include/uapi/linux/rtnetlink.h
 *
 * @hide
 */
public class StructRtMsg extends Struct {
    // Already aligned.
    public static final int STRUCT_SIZE = 12;

    @Field(order = 0, type = Type.U8)
    public final short family; // Address family of route.
    @Field(order = 1, type = Type.U8)
    public final short dstLen; // Length of destination.
    @Field(order = 2, type = Type.U8)
    public final short srcLen; // Length of source.
    @Field(order = 3, type = Type.U8)
    public final short tos;    // TOS filter.
    @Field(order = 4, type = Type.U8)
    public final short table;  // Routing table ID.
    @Field(order = 5, type = Type.U8)
    public final short protocol; // Routing protocol.
    @Field(order = 6, type = Type.U8)
    public final short scope;  // distance to the destination.
    @Field(order = 7, type = Type.U8)
    public final short type;   // route type
    @Field(order = 8, type = Type.U32)
    public final long flags;

    StructRtMsg(short family, short dstLen, short srcLen, short tos, short table, short protocol,
            short scope, short type, long flags) {
        this.family = family;
        this.dstLen = dstLen;
        this.srcLen = srcLen;
        this.tos = tos;
        this.table = table;
        this.protocol = protocol;
        this.scope = scope;
        this.type = type;
        this.flags = flags;
    }

    /**
     * Parse a rtmsg struct from a {@link ByteBuffer}.
     *
     * @param byteBuffer The buffer from which to parse the rtmsg struct.
     * @return the parsed rtmsg struct, or {@code null} if the rtmsg struct could not be
     *         parsed successfully (for example, if it was truncated).
     */
    @Nullable
    public static StructRtMsg parse(@NonNull final ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < STRUCT_SIZE) return null;

        // The ByteOrder must already have been set to native order.
        return Struct.parse(StructRtMsg.class, byteBuffer);
    }

    /**
     * Write the rtmsg struct to {@link ByteBuffer}.
     */
    public void pack(@NonNull final ByteBuffer byteBuffer) {
        // The ByteOrder must already have been set to native order.
        this.writeToByteBuffer(byteBuffer);
    }
}
