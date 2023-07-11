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
 * struct ifa_cacheinfo
 *
 * see also:
 *
 *     include/uapi/linux/if_addr.h
 *
 * @hide
 */
public class StructIfacacheInfo extends Struct {
    // Already aligned.
    public static final int STRUCT_SIZE = 16;

    @Field(order = 0, type = Type.U32)
    public final long preferred;
    @Field(order = 1, type = Type.U32)
    public final long valid;
    @Field(order = 2, type = Type.U32)
    public final long cstamp; // created timestamp, hundredths of seconds.
    @Field(order = 3, type = Type.U32)
    public final long tstamp; // updated timestamp, hundredths of seconds.

    StructIfacacheInfo(long preferred, long valid, long cstamp, long tstamp) {
        this.preferred = preferred;
        this.valid = valid;
        this.cstamp = cstamp;
        this.tstamp = tstamp;
    }

    /**
     * Parse an ifa_cacheinfo struct from a {@link ByteBuffer}.
     *
     * @param byteBuffer The buffer from which to parse the ifa_cacheinfo.
     * @return the parsed ifa_cacheinfo struct, or {@code null} if the ifa_cacheinfo struct
     *         could not be parsed successfully (for example, if it was truncated).
     */
    @Nullable
    public static StructIfacacheInfo parse(@NonNull final ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < STRUCT_SIZE) return null;

        // The ByteOrder must already have been set to native order.
        return Struct.parse(StructIfacacheInfo.class, byteBuffer);
    }

    /**
     * Write an ifa_cacheinfo struct to {@link ByteBuffer}.
     */
    public void pack(@NonNull final ByteBuffer byteBuffer) {
        // The ByteOrder must already have been set to native order.
        this.writeToByteBuffer(byteBuffer);
    }
}
