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

import android.system.OsConstants;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.net.module.util.HexDump;

import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * A NetlinkMessage subclass for rtnetlink address messages.
 *
 * RtNetlinkAddressMessage.parse() must be called with a ByteBuffer that contains exactly one
 * netlink message.
 *
 * see also:
 *
 *     include/uapi/linux/rtnetlink.h
 *
 * @hide
 */
public class RtNetlinkAddressMessage extends NetlinkMessage {
    public static final short IFA_ADDRESS        = 1;
    public static final short IFA_CACHEINFO      = 6;
    public static final short IFA_FLAGS          = 8;

    private int mFlags;
    @NonNull
    private StructIfaddrMsg mIfaddrmsg;
    @NonNull
    private InetAddress mIpAddress;
    @Nullable
    private StructIfacacheInfo mIfacacheInfo;

    private RtNetlinkAddressMessage(@NonNull StructNlMsgHdr header) {
        super(header);
        mIfaddrmsg = null;
        mIpAddress = null;
        mIfacacheInfo = null;
        mFlags = 0;
    }

    public int getFlags() {
        return mFlags;
    }

    @NonNull
    public StructIfaddrMsg getIfaddrHeader() {
        return mIfaddrmsg;
    }

    @NonNull
    public InetAddress getIpAddress() {
        return mIpAddress;
    }

    @Nullable
    public StructIfacacheInfo getIfacacheInfo() {
        return mIfacacheInfo;
    }

    /**
     * Parse rtnetlink address message from {@link ByteBuffer}. This method must be called with a
     * ByteBuffer that contains exactly one netlink message.
     *
     * @param header netlink message header.
     * @param byteBuffer the ByteBuffer instance that wraps the raw netlink message bytes.
     */
    @Nullable
    public static RtNetlinkAddressMessage parse(@NonNull final StructNlMsgHdr header,
            @NonNull final ByteBuffer byteBuffer) {
        final RtNetlinkAddressMessage addrMsg = new RtNetlinkAddressMessage(header);

        addrMsg.mIfaddrmsg = StructIfaddrMsg.parse(byteBuffer);
        if (addrMsg.mIfaddrmsg == null) return null;

        // IFA_ADDRESS
        final int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = StructNlAttr.findNextAttrOfType(IFA_ADDRESS, byteBuffer);
        if (nlAttr == null) return null;
        addrMsg.mIpAddress = nlAttr.getValueAsInetAddress();
        if (addrMsg.mIpAddress == null) return null;

        // IFA_CACHEINFO
        byteBuffer.position(baseOffset);
        nlAttr = StructNlAttr.findNextAttrOfType(IFA_CACHEINFO, byteBuffer);
        if (nlAttr != null) {
            addrMsg.mIfacacheInfo = StructIfacacheInfo.parse(nlAttr.getValueAsByteBuffer());
        }

        // The first 8 bits of flags are in the ifaddrmsg.
        addrMsg.mFlags = addrMsg.mIfaddrmsg.flags;
        // IFA_FLAGS. All the flags are in the IF_FLAGS attribute. This should always be present,
        // and will overwrite the flags set above.
        byteBuffer.position(baseOffset);
        nlAttr = StructNlAttr.findNextAttrOfType(IFA_FLAGS, byteBuffer);
        if (nlAttr == null) return null;
        final Integer value = nlAttr.getValueAsInteger();
        if (value == null) return null;
        addrMsg.mFlags = value;

        return addrMsg;
    }

    /**
     * Write a rtnetlink address message to {@link ByteBuffer}.
     */
    @VisibleForTesting
    protected void pack(ByteBuffer byteBuffer) {
        getHeader().pack(byteBuffer);
        mIfaddrmsg.pack(byteBuffer);

        final StructNlAttr address = new StructNlAttr(IFA_ADDRESS, mIpAddress);
        address.pack(byteBuffer);

        if (mIfacacheInfo != null) {
            final StructNlAttr cacheInfo = new StructNlAttr(IFA_CACHEINFO,
                    mIfacacheInfo.writeToBytes());
            cacheInfo.pack(byteBuffer);
        }

        // If IFA_FLAGS attribute isn't present on the wire at parsing netlink message, it will
        // still be packed to ByteBuffer even if the flag is 0.
        final StructNlAttr flags = new StructNlAttr(IFA_FLAGS, mFlags);
        flags.pack(byteBuffer);
    }

    @Override
    public String toString() {
        return "RtNetlinkAddressMessage{ "
                + "nlmsghdr{" + mHeader.toString(OsConstants.NETLINK_ROUTE) + "}, "
                + "Ifaddrmsg{" + mIfaddrmsg.toString() + "}, "
                + "IP Address{" + mIpAddress.getHostAddress() + "}, "
                + "IfacacheInfo{" + (mIfacacheInfo == null ? "" : mIfacacheInfo.toString()) + "}, "
                + "Address Flags{" + HexDump.toHexString(mFlags) + "} "
                + "}";
    }
}
