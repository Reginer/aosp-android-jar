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
 * A NetlinkMessage subclass for netlink error messages.
 *
 * @hide
 */
public class NetlinkErrorMessage extends NetlinkMessage {

    /**
     * Parse a netlink error message from a {@link ByteBuffer}.
     *
     * @param byteBuffer The buffer from which to parse the netlink error message.
     * @return the parsed netlink error message, or {@code null} if the netlink error message
     *         could not be parsed successfully (for example, if it was truncated).
     */
    @Nullable
    public static NetlinkErrorMessage parse(@NonNull StructNlMsgHdr header,
            @NonNull ByteBuffer byteBuffer) {
        final NetlinkErrorMessage errorMsg = new NetlinkErrorMessage(header);

        errorMsg.mNlMsgErr = StructNlMsgErr.parse(byteBuffer);
        if (errorMsg.mNlMsgErr == null) {
            return null;
        }

        return errorMsg;
    }

    private StructNlMsgErr mNlMsgErr;

    NetlinkErrorMessage(@NonNull StructNlMsgHdr header) {
        super(header);
        mNlMsgErr = null;
    }

    public StructNlMsgErr getNlMsgError() {
        return mNlMsgErr;
    }

    @Override
    public String toString() {
        return "NetlinkErrorMessage{ "
                + "nlmsghdr{" + (mHeader == null ? "" : mHeader.toString()) + "}, "
                + "nlmsgerr{" + (mNlMsgErr == null ? "" : mNlMsgErr.toString()) + "} "
                + "}";
    }
}
