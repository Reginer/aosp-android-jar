/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.net.module.util.wear;

import com.android.net.module.util.async.ReadableByteBuffer;

/**
 * Implements utilities for decoding parts of TCP/UDP/IP headers.
 *
 * @hide
 */
final class NetPacketHelpers {
    static void encodeNetworkUnsignedInt16(int value, byte[] dst, final int dstPos) {
        dst[dstPos] = (byte) ((value >> 8) & 0xFF);
        dst[dstPos + 1] = (byte) (value & 0xFF);
    }

    static int decodeNetworkUnsignedInt16(byte[] data, final int pos) {
        return ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
    }

    static int decodeNetworkUnsignedInt16(ReadableByteBuffer data, final int pos) {
        return ((data.peek(pos) & 0xFF) << 8) | (data.peek(pos + 1) & 0xFF);
    }

    private NetPacketHelpers() {}
}
