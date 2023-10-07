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

import android.system.OsConstants;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

/**
 * NetlinkMessage base class for other, more specific netlink message types.
 *
 * Classes that extend NetlinkMessage should:
 *     - implement a public static parse(StructNlMsgHdr, ByteBuffer) method
 *     - returning either null (parse errors) or a new object of the subclass
 *       type (cast-able to NetlinkMessage)
 *
 * NetlinkMessage.parse() should be updated to know which nlmsg_type values
 * correspond with which message subclasses.
 *
 * @hide
 */
public class NetlinkMessage {
    private static final String TAG = "NetlinkMessage";

    /**
     * Parsing netlink messages for reserved control message or specific netlink message. The
     * netlink family is required for parsing specific netlink message. See man-pages/netlink.
     */
    @Nullable
    public static NetlinkMessage parse(@NonNull ByteBuffer byteBuffer, int nlFamily) {
        final int startPosition = (byteBuffer != null) ? byteBuffer.position() : -1;
        final StructNlMsgHdr nlmsghdr = StructNlMsgHdr.parse(byteBuffer);
        if (nlmsghdr == null) {
            return null;
        }

        final int messageLength = NetlinkConstants.alignedLengthOf(nlmsghdr.nlmsg_len);
        final int payloadLength = messageLength - StructNlMsgHdr.STRUCT_SIZE;
        if (payloadLength < 0 || payloadLength > byteBuffer.remaining()) {
            // Malformed message or runt buffer.  Pretend the buffer was consumed.
            byteBuffer.position(byteBuffer.limit());
            return null;
        }

        // Reserved control messages. The netlink family is ignored.
        // See NLMSG_MIN_TYPE in include/uapi/linux/netlink.h.
        if (nlmsghdr.nlmsg_type <= NetlinkConstants.NLMSG_MAX_RESERVED) {
            return parseCtlMessage(nlmsghdr, byteBuffer, payloadLength);
        }

        // Netlink family messages. The netlink family is required. Note that the reason for using
        // if-statement is that switch-case can't be used because the OsConstants.NETLINK_* are
        // not constant.
        final NetlinkMessage parsed;
        if (nlFamily == OsConstants.NETLINK_ROUTE) {
            parsed = parseRtMessage(nlmsghdr, byteBuffer);
        } else if (nlFamily == OsConstants.NETLINK_INET_DIAG) {
            parsed = parseInetDiagMessage(nlmsghdr, byteBuffer);
        } else if (nlFamily == OsConstants.NETLINK_NETFILTER) {
            parsed = parseNfMessage(nlmsghdr, byteBuffer);
        } else {
            parsed = null;
        }

        // Advance to the end of the message, regardless of whether the parsing code consumed
        // all of it or not.
        byteBuffer.position(startPosition + messageLength);

        return parsed;
    }

    @NonNull
    protected final StructNlMsgHdr mHeader;

    public NetlinkMessage(@NonNull StructNlMsgHdr nlmsghdr) {
        mHeader = nlmsghdr;
    }

    @NonNull
    public StructNlMsgHdr getHeader() {
        return mHeader;
    }

    @Override
    public String toString() {
        // The netlink family is not provided to StructNlMsgHdr#toString because NetlinkMessage
        // doesn't store the information. So the netlink message type can't be transformed into
        // a string by StructNlMsgHdr#toString and just keep as an integer. The specific message
        // which inherits NetlinkMessage could override NetlinkMessage#toString and provide the
        // specific netlink family to StructNlMsgHdr#toString.
        return "NetlinkMessage{" + mHeader.toString() + "}";
    }

    @NonNull
    private static NetlinkMessage parseCtlMessage(@NonNull StructNlMsgHdr nlmsghdr,
            @NonNull ByteBuffer byteBuffer, int payloadLength) {
        switch (nlmsghdr.nlmsg_type) {
            case NetlinkConstants.NLMSG_ERROR:
                return (NetlinkMessage) NetlinkErrorMessage.parse(nlmsghdr, byteBuffer);
            default: {
                // Other netlink control messages. Just parse the header for now,
                // pretending the whole message was consumed.
                byteBuffer.position(byteBuffer.position() + payloadLength);
                return new NetlinkMessage(nlmsghdr);
            }
        }
    }

    @Nullable
    private static NetlinkMessage parseRtMessage(@NonNull StructNlMsgHdr nlmsghdr,
            @NonNull ByteBuffer byteBuffer) {
        switch (nlmsghdr.nlmsg_type) {
            case NetlinkConstants.RTM_NEWLINK:
            case NetlinkConstants.RTM_DELLINK:
                return (NetlinkMessage) RtNetlinkLinkMessage.parse(nlmsghdr, byteBuffer);
            case NetlinkConstants.RTM_NEWADDR:
            case NetlinkConstants.RTM_DELADDR:
                return (NetlinkMessage) RtNetlinkAddressMessage.parse(nlmsghdr, byteBuffer);
            case NetlinkConstants.RTM_NEWROUTE:
            case NetlinkConstants.RTM_DELROUTE:
                return (NetlinkMessage) RtNetlinkRouteMessage.parse(nlmsghdr, byteBuffer);
            case NetlinkConstants.RTM_NEWNEIGH:
            case NetlinkConstants.RTM_DELNEIGH:
            case NetlinkConstants.RTM_GETNEIGH:
                return (NetlinkMessage) RtNetlinkNeighborMessage.parse(nlmsghdr, byteBuffer);
            case NetlinkConstants.RTM_NEWNDUSEROPT:
                return (NetlinkMessage) NduseroptMessage.parse(nlmsghdr, byteBuffer);
            default: return null;
        }
    }

    @Nullable
    private static NetlinkMessage parseInetDiagMessage(@NonNull StructNlMsgHdr nlmsghdr,
            @NonNull ByteBuffer byteBuffer) {
        switch (nlmsghdr.nlmsg_type) {
            case NetlinkConstants.SOCK_DIAG_BY_FAMILY:
                return (NetlinkMessage) InetDiagMessage.parse(nlmsghdr, byteBuffer);
            default: return null;
        }
    }

    @Nullable
    private static NetlinkMessage parseNfMessage(@NonNull StructNlMsgHdr nlmsghdr,
            @NonNull ByteBuffer byteBuffer) {
        switch (nlmsghdr.nlmsg_type) {
            case NetlinkConstants.NFNL_SUBSYS_CTNETLINK << 8
                    | NetlinkConstants.IPCTNL_MSG_CT_NEW:
            case NetlinkConstants.NFNL_SUBSYS_CTNETLINK << 8
                    | NetlinkConstants.IPCTNL_MSG_CT_DELETE:
                return (NetlinkMessage) ConntrackMessage.parse(nlmsghdr, byteBuffer);
            default: return null;
        }
    }
}
