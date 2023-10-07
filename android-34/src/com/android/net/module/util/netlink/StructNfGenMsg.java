/*
 * Copyright (C) 2017 The Android Open Source Project
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * struct nfgenmsg
 *
 * see &lt;linux_src&gt;/include/uapi/linux/netfilter/nfnetlink.h
 *
 * @hide
 */
public class StructNfGenMsg {
    public static final int STRUCT_SIZE = 2 + Short.BYTES;

    public static final int NFNETLINK_V0 = 0;

    public final byte nfgen_family;
    public final byte version;
    public final short res_id;  // N.B.: this is big endian in the kernel

    /**
     * Parse a netfilter netlink header from a {@link ByteBuffer}.
     *
     * @param byteBuffer The buffer from which to parse the netfilter netlink header.
     * @return the parsed netfilter netlink header, or {@code null} if the netfilter netlink header
     *         could not be parsed successfully (for example, if it was truncated).
     */
    @Nullable
    public static StructNfGenMsg parse(@NonNull ByteBuffer byteBuffer) {
        Objects.requireNonNull(byteBuffer);

        if (!hasAvailableSpace(byteBuffer)) return null;

        final byte nfgen_family = byteBuffer.get();
        final byte version = byteBuffer.get();

        final ByteOrder originalOrder = byteBuffer.order();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        final short res_id = byteBuffer.getShort();
        byteBuffer.order(originalOrder);

        return new StructNfGenMsg(nfgen_family, version, res_id);
    }

    public StructNfGenMsg(byte family, byte ver, short id) {
        nfgen_family = family;
        version = ver;
        res_id = id;
    }

    public StructNfGenMsg(byte family) {
        nfgen_family = family;
        version = (byte) NFNETLINK_V0;
        res_id = (short) 0;
    }

    /**
     * Write a netfilter netlink header to a {@link ByteBuffer}.
     */
    public void pack(ByteBuffer byteBuffer) {
        byteBuffer.put(nfgen_family);
        byteBuffer.put(version);

        final ByteOrder originalOrder = byteBuffer.order();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort(res_id);
        byteBuffer.order(originalOrder);
    }

    private static boolean hasAvailableSpace(@NonNull ByteBuffer byteBuffer) {
        return byteBuffer.remaining() >= STRUCT_SIZE;
    }

    @Override
    public String toString() {
        final String familyStr = NetlinkConstants.stringForAddressFamily(nfgen_family);

        return "NfGenMsg{ "
                + "nfgen_family{" + familyStr + "}, "
                + "version{" + Byte.toUnsignedInt(version) + "}, "
                + "res_id{" + Short.toUnsignedInt(res_id) + "} "
                + "}";
    }
}
