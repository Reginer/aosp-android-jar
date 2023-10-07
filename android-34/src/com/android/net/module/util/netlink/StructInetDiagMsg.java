/*
 * Copyright (C) 2019 The Android Open Source Project
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

/**
 * struct inet_diag_msg
 *
 * see &lt;linux_src&gt;/include/uapi/linux/inet_diag.h
 *
 * struct inet_diag_msg {
 *      __u8    idiag_family;
 *      __u8    idiag_state;
 *      __u8    idiag_timer;
 *      __u8    idiag_retrans;
 *      struct  inet_diag_sockid id;
 *      __u32   idiag_expires;
 *      __u32   idiag_rqueue;
 *      __u32   idiag_wqueue;
 *      __u32   idiag_uid;
 *      __u32   idiag_inode;
 * };
 *
 * @hide
 */
public class StructInetDiagMsg {
    public static final int STRUCT_SIZE = 4 + StructInetDiagSockId.STRUCT_SIZE + 20;
    public short idiag_family;
    public short idiag_state;
    public short idiag_timer;
    public short idiag_retrans;
    @NonNull
    public StructInetDiagSockId id;
    public long idiag_expires;
    public long idiag_rqueue;
    public long idiag_wqueue;
    // Use int for uid since other code use int for uid and uid fits to int
    public int idiag_uid;
    public long idiag_inode;

    private static short unsignedByte(byte b) {
        return (short) (b & 0xFF);
    }

    /**
     * Parse inet diag netlink message from buffer.
     */
    @Nullable
    public static StructInetDiagMsg parse(@NonNull ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < STRUCT_SIZE) {
            return null;
        }
        StructInetDiagMsg struct = new StructInetDiagMsg();
        struct.idiag_family = unsignedByte(byteBuffer.get());
        struct.idiag_state = unsignedByte(byteBuffer.get());
        struct.idiag_timer = unsignedByte(byteBuffer.get());
        struct.idiag_retrans = unsignedByte(byteBuffer.get());
        struct.id = StructInetDiagSockId.parse(byteBuffer, struct.idiag_family);
        if (struct.id == null) {
            return null;
        }
        struct.idiag_expires = Integer.toUnsignedLong(byteBuffer.getInt());
        struct.idiag_rqueue = Integer.toUnsignedLong(byteBuffer.getInt());
        struct.idiag_wqueue = Integer.toUnsignedLong(byteBuffer.getInt());
        struct.idiag_uid = byteBuffer.getInt();
        struct.idiag_inode = Integer.toUnsignedLong(byteBuffer.getInt());
        return struct;
    }

    @Override
    public String toString() {
        return "StructInetDiagMsg{ "
                + "idiag_family{" + idiag_family + "}, "
                + "idiag_state{" + idiag_state + "}, "
                + "idiag_timer{" + idiag_timer + "}, "
                + "idiag_retrans{" + idiag_retrans + "}, "
                + "id{" + id + "}, "
                + "idiag_expires{" + idiag_expires + "}, "
                + "idiag_rqueue{" + idiag_rqueue + "}, "
                + "idiag_wqueue{" + idiag_wqueue + "}, "
                + "idiag_uid{" + idiag_uid + "}, "
                + "idiag_inode{" + idiag_inode + "}, "
                + "}";
    }
}
