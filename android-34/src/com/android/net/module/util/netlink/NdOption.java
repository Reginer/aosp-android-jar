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

package com.android.net.module.util.netlink;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

/**
 * Base class for IPv6 neighbour discovery options.
 */
public class NdOption {
    public static final int STRUCT_SIZE = 2;

    /** The option type. */
    public final byte type;
    /** The length of the option in 8-byte units. Actually an unsigned 8-bit integer */
    public final int length;

    /** Constructs a new NdOption. */
    NdOption(byte type, int length) {
        this.type = type;
        this.length = length;
    }

    /**
     * Parses a neighbour discovery option.
     *
     * Parses (and consumes) the option if it is of a known type. If the option is of an unknown
     * type, advances the buffer (so the caller can continue parsing if desired) and returns
     * {@link #UNKNOWN}. If the option claims a length of 0, returns null because parsing cannot
     * continue.
     *
     * No checks are performed on the length other than ensuring it is not 0, so if a caller wants
     * to deal with options that might overflow the structure that contains them, it must explicitly
     * set the buffer's limit to the position at which that structure ends.
     *
     * @param buf the buffer to parse.
     * @return a subclass of {@link NdOption}, or {@code null} for an unknown or malformed option.
     */
    public static NdOption parse(@NonNull ByteBuffer buf) {
        if (buf.remaining() < STRUCT_SIZE) return null;

        // Peek the type without advancing the buffer.
        byte type = buf.get(buf.position());
        int length = Byte.toUnsignedInt(buf.get(buf.position() + 1));
        if (length == 0) return null;

        switch (type) {
            case StructNdOptPref64.TYPE:
                return StructNdOptPref64.parse(buf);

            case StructNdOptRdnss.TYPE:
                return StructNdOptRdnss.parse(buf);

            default:
                int newPosition = Math.min(buf.limit(), buf.position() + length * 8);
                buf.position(newPosition);
                return UNKNOWN;
        }
    }

    void writeToByteBuffer(ByteBuffer buf) {
        buf.put(type);
        buf.put((byte) length);
    }

    @Override
    public String toString() {
        return String.format("NdOption(%d, %d)", Byte.toUnsignedInt(type), length);
    }

    public static final NdOption UNKNOWN = new NdOption((byte) 0, 0);
}
