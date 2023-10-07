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

import static com.android.net.module.util.netlink.StructNlMsgHdr.NLM_F_ACK;
import static com.android.net.module.util.netlink.StructNlMsgHdr.NLM_F_DUMP;
import static com.android.net.module.util.netlink.StructNlMsgHdr.NLM_F_REPLACE;
import static com.android.net.module.util.netlink.StructNlMsgHdr.NLM_F_REQUEST;

import android.system.OsConstants;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A NetlinkMessage subclass for rtnetlink neighbor messages.
 *
 * see also: &lt;linux_src&gt;/include/uapi/linux/neighbour.h
 *
 * @hide
 */
public class RtNetlinkNeighborMessage extends NetlinkMessage {
    public static final short NDA_UNSPEC    = 0;
    public static final short NDA_DST       = 1;
    public static final short NDA_LLADDR    = 2;
    public static final short NDA_CACHEINFO = 3;
    public static final short NDA_PROBES    = 4;
    public static final short NDA_VLAN      = 5;
    public static final short NDA_PORT      = 6;
    public static final short NDA_VNI       = 7;
    public static final short NDA_IFINDEX   = 8;
    public static final short NDA_MASTER    = 9;

    /**
     * Parse routing socket netlink neighbor message from ByteBuffer.
     *
     * @param header netlink message header.
     * @param byteBuffer the ByteBuffer instance that wraps the raw netlink message bytes.
     */
    @Nullable
    public static RtNetlinkNeighborMessage parse(@NonNull StructNlMsgHdr header,
            @NonNull ByteBuffer byteBuffer) {
        final RtNetlinkNeighborMessage neighMsg = new RtNetlinkNeighborMessage(header);

        neighMsg.mNdmsg = StructNdMsg.parse(byteBuffer);
        if (neighMsg.mNdmsg == null) {
            return null;
        }

        // Some of these are message-type dependent, and not always present.
        final int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = StructNlAttr.findNextAttrOfType(NDA_DST, byteBuffer);
        if (nlAttr != null) {
            neighMsg.mDestination = nlAttr.getValueAsInetAddress();
        }

        byteBuffer.position(baseOffset);
        nlAttr = StructNlAttr.findNextAttrOfType(NDA_LLADDR, byteBuffer);
        if (nlAttr != null) {
            neighMsg.mLinkLayerAddr = nlAttr.nla_value;
        }

        byteBuffer.position(baseOffset);
        nlAttr = StructNlAttr.findNextAttrOfType(NDA_PROBES, byteBuffer);
        if (nlAttr != null) {
            neighMsg.mNumProbes = nlAttr.getValueAsInt(0);
        }

        byteBuffer.position(baseOffset);
        nlAttr = StructNlAttr.findNextAttrOfType(NDA_CACHEINFO, byteBuffer);
        if (nlAttr != null) {
            neighMsg.mCacheInfo = StructNdaCacheInfo.parse(nlAttr.getValueAsByteBuffer());
        }

        final int kMinConsumed = StructNlMsgHdr.STRUCT_SIZE + StructNdMsg.STRUCT_SIZE;
        final int kAdditionalSpace = NetlinkConstants.alignedLengthOf(
                neighMsg.mHeader.nlmsg_len - kMinConsumed);
        if (byteBuffer.remaining() < kAdditionalSpace) {
            byteBuffer.position(byteBuffer.limit());
        } else {
            byteBuffer.position(baseOffset + kAdditionalSpace);
        }

        return neighMsg;
    }

    /**
     * A convenience method to create an RTM_GETNEIGH request message.
     */
    public static byte[] newGetNeighborsRequest(int seqNo) {
        final int length = StructNlMsgHdr.STRUCT_SIZE + StructNdMsg.STRUCT_SIZE;
        final byte[] bytes = new byte[length];
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.nativeOrder());

        final StructNlMsgHdr nlmsghdr = new StructNlMsgHdr();
        nlmsghdr.nlmsg_len = length;
        nlmsghdr.nlmsg_type = NetlinkConstants.RTM_GETNEIGH;
        nlmsghdr.nlmsg_flags = NLM_F_REQUEST | NLM_F_DUMP;
        nlmsghdr.nlmsg_seq = seqNo;
        nlmsghdr.pack(byteBuffer);

        final StructNdMsg ndmsg = new StructNdMsg();
        ndmsg.pack(byteBuffer);

        return bytes;
    }

    /**
     * A convenience method to create an RTM_NEWNEIGH message, to modify
     * the kernel's state information for a specific neighbor.
     */
    public static byte[] newNewNeighborMessage(
            int seqNo, InetAddress ip, short nudState, int ifIndex, byte[] llAddr) {
        final StructNlMsgHdr nlmsghdr = new StructNlMsgHdr();
        nlmsghdr.nlmsg_type = NetlinkConstants.RTM_NEWNEIGH;
        nlmsghdr.nlmsg_flags = NLM_F_REQUEST | NLM_F_ACK | NLM_F_REPLACE;
        nlmsghdr.nlmsg_seq = seqNo;

        final RtNetlinkNeighborMessage msg = new RtNetlinkNeighborMessage(nlmsghdr);
        msg.mNdmsg = new StructNdMsg();
        msg.mNdmsg.ndm_family =
                (byte) ((ip instanceof Inet6Address) ? OsConstants.AF_INET6 : OsConstants.AF_INET);
        msg.mNdmsg.ndm_ifindex = ifIndex;
        msg.mNdmsg.ndm_state = nudState;
        msg.mDestination = ip;
        msg.mLinkLayerAddr = llAddr;  // might be null

        final byte[] bytes = new byte[msg.getRequiredSpace()];
        nlmsghdr.nlmsg_len = bytes.length;
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.nativeOrder());
        msg.pack(byteBuffer);
        return bytes;
    }

    private StructNdMsg mNdmsg;
    private InetAddress mDestination;
    private byte[] mLinkLayerAddr;
    private int mNumProbes;
    private StructNdaCacheInfo mCacheInfo;

    private RtNetlinkNeighborMessage(@NonNull StructNlMsgHdr header) {
        super(header);
        mNdmsg = null;
        mDestination = null;
        mLinkLayerAddr = null;
        mNumProbes = 0;
        mCacheInfo = null;
    }

    public StructNdMsg getNdHeader() {
        return mNdmsg;
    }

    public InetAddress getDestination() {
        return mDestination;
    }

    public byte[] getLinkLayerAddress() {
        return mLinkLayerAddr;
    }

    public int getProbes() {
        return mNumProbes;
    }

    public StructNdaCacheInfo getCacheInfo() {
        return mCacheInfo;
    }

    private int getRequiredSpace() {
        int spaceRequired = StructNlMsgHdr.STRUCT_SIZE + StructNdMsg.STRUCT_SIZE;
        if (mDestination != null) {
            spaceRequired += NetlinkConstants.alignedLengthOf(
                    StructNlAttr.NLA_HEADERLEN + mDestination.getAddress().length);
        }
        if (mLinkLayerAddr != null) {
            spaceRequired += NetlinkConstants.alignedLengthOf(
                    StructNlAttr.NLA_HEADERLEN + mLinkLayerAddr.length);
        }
        // Currently we don't write messages with NDA_PROBES nor NDA_CACHEINFO
        // attributes appended.  Fix later, if necessary.
        return spaceRequired;
    }

    private static void packNlAttr(short nlType, byte[] nlValue, ByteBuffer byteBuffer) {
        final StructNlAttr nlAttr = new StructNlAttr();
        nlAttr.nla_type = nlType;
        nlAttr.nla_value = nlValue;
        nlAttr.nla_len = (short) (StructNlAttr.NLA_HEADERLEN + nlAttr.nla_value.length);
        nlAttr.pack(byteBuffer);
    }

    /**
     * Write a neighbor discovery netlink message to {@link ByteBuffer}.
     */
    public void pack(ByteBuffer byteBuffer) {
        getHeader().pack(byteBuffer);
        mNdmsg.pack(byteBuffer);

        if (mDestination != null) {
            packNlAttr(NDA_DST, mDestination.getAddress(), byteBuffer);
        }
        if (mLinkLayerAddr != null) {
            packNlAttr(NDA_LLADDR, mLinkLayerAddr, byteBuffer);
        }
    }

    @Override
    public String toString() {
        final String ipLiteral = (mDestination == null) ? "" : mDestination.getHostAddress();
        return "RtNetlinkNeighborMessage{ "
                + "nlmsghdr{"
                + (mHeader == null ? "" : mHeader.toString(OsConstants.NETLINK_ROUTE)) + "}, "
                + "ndmsg{" + (mNdmsg == null ? "" : mNdmsg.toString()) + "}, "
                + "destination{" + ipLiteral + "} "
                + "linklayeraddr{" + NetlinkConstants.hexify(mLinkLayerAddr) + "} "
                + "probes{" + mNumProbes + "} "
                + "cacheinfo{" + (mCacheInfo == null ? "" : mCacheInfo.toString()) + "} "
                + "}";
    }
}
