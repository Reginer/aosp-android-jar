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
 * struct ifaddrmsg
 *
 * see also:
 *
 *     include/uapi/linux/if_addr.h
 *
 * @hide
 */
public class StructIfaddrMsg extends Struct {
    // Already aligned.
    public static final int STRUCT_SIZE = 8;

    @Field(order = 0, type = Type.U8)
    public final short family;
    @Field(order = 1, type = Type.U8)
    public final short prefixLen;
    @Field(order = 2, type = Type.U8)
    public final short flags;
    @Field(order = 3, type = Type.U8)
    public final short scope;
    @Field(order = 4, type = Type.S32)
    public final int index;

    StructIfaddrMsg(short family, short prefixLen, short flags, short scope, int index) {
        this.family = family;
        this.prefixLen = prefixLen;
        this.flags = flags;
        this.scope = scope;
        this.index = index;
    }

    /**
     * Parse an ifaddrmsg struct from a {@link ByteBuffer}.
     *
     * @param byteBuffer The buffer from which to parse the ifaddrmsg.
     * @return the parsed ifaddrmsg struct, or {@code null} if the ifaddrmsg struct
     *         could not be parsed successfully (for example, if it was truncated).
     */
    @Nullable
    public static StructIfaddrMsg parse(@NonNull final ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < STRUCT_SIZE) return null;

        // The ByteOrder must already have been set to native order.
        return Struct.parse(StructIfaddrMsg.class, byteBuffer);
    }

    /**
     * Write an ifaddrmsg struct to {@link ByteBuffer}.
     */
    public void pack(@NonNull final ByteBuffer byteBuffer) {
        // The ByteOrder must already have been set to native order.
        this.writeToByteBuffer(byteBuffer);
    }
}
