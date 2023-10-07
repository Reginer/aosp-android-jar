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

import static com.android.net.module.util.netlink.StructNlMsgHdr.NLM_F_ACK;
import static com.android.net.module.util.netlink.StructNlMsgHdr.NLM_F_REPLACE;
import static com.android.net.module.util.netlink.StructNlMsgHdr.NLM_F_REQUEST;

import android.system.OsConstants;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.net.module.util.HexDump;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

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

    @VisibleForTesting
    public RtNetlinkAddressMessage(@NonNull final StructNlMsgHdr header,
            @NonNull final StructIfaddrMsg ifaddrMsg,
            @NonNull final InetAddress ipAddress,
            @Nullable final StructIfacacheInfo structIfacacheInfo,
            int flags) {
        super(header);
        mIfaddrmsg = ifaddrMsg;
        mIpAddress = ipAddress;
        mIfacacheInfo = structIfacacheInfo;
        mFlags = flags;
    }
    private RtNetlinkAddressMessage(@NonNull StructNlMsgHdr header) {
        this(header, null, null, null, 0);
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

    /**
     * A convenience method to create an RTM_NEWADDR message.
     */
    public static byte[] newRtmNewAddressMessage(int seqNo, @NonNull final InetAddress ip,
            short prefixlen, int flags, byte scope, int ifIndex, long preferred, long valid) {
        Objects.requireNonNull(ip, "IP address to be set via netlink message cannot be null");

        final StructNlMsgHdr nlmsghdr = new StructNlMsgHdr();
        nlmsghdr.nlmsg_type = NetlinkConstants.RTM_NEWADDR;
        nlmsghdr.nlmsg_flags = NLM_F_REQUEST | NLM_F_REPLACE | NLM_F_ACK;
        nlmsghdr.nlmsg_seq = seqNo;

        final RtNetlinkAddressMessage msg = new RtNetlinkAddressMessage(nlmsghdr);
        final byte family =
                (byte) ((ip instanceof Inet6Address) ? OsConstants.AF_INET6 : OsConstants.AF_INET);
        // IFA_FLAGS attribute is always present within this method, just set flags from
        // ifaddrmsg to 0. kernel will prefer the flags from IFA_FLAGS attribute.
        msg.mIfaddrmsg =
                new StructIfaddrMsg(family, prefixlen, (short) 0 /* flags */, scope, ifIndex);
        msg.mIpAddress = ip;
        msg.mIfacacheInfo = new StructIfacacheInfo(preferred, valid, 0 /* cstamp */,
                0 /* tstamp */);
        msg.mFlags = flags;

        final byte[] bytes = new byte[msg.getRequiredSpace()];
        nlmsghdr.nlmsg_len = bytes.length;
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.nativeOrder());
        msg.pack(byteBuffer);
        return bytes;
    }

    private int getRequiredSpace() {
        int spaceRequired = StructNlMsgHdr.STRUCT_SIZE + StructIfaddrMsg.STRUCT_SIZE;
        // IFA_ADDRESS attr
        spaceRequired += NetlinkConstants.alignedLengthOf(
                StructNlAttr.NLA_HEADERLEN + mIpAddress.getAddress().length);
        // IFA_CACHEINFO attr
        spaceRequired += NetlinkConstants.alignedLengthOf(
                StructNlAttr.NLA_HEADERLEN + StructIfacacheInfo.STRUCT_SIZE);
        // IFA_FLAGS "u32" attr
        spaceRequired += StructNlAttr.NLA_HEADERLEN + 4;
        return spaceRequired;
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
