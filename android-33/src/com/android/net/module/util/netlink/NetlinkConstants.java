/*
 * Copyright (C) 2015 The Android Open Source Project
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

import java.nio.ByteBuffer;

/**
 * Various constants and static helper methods for netlink communications.
 *
 * Values taken from:
 *
 *     include/uapi/linux/netfilter/nfnetlink.h
 *     include/uapi/linux/netfilter/nfnetlink_conntrack.h
 *     include/uapi/linux/netlink.h
 *     include/uapi/linux/rtnetlink.h
 *
 * @hide
 */
public class NetlinkConstants {
    private NetlinkConstants() {}

    public static final int NLA_ALIGNTO = 4;
    /**
     * Flag for dumping struct tcp_info.
     * Corresponding to enum definition in external/strace/linux/inet_diag.h.
     */
    public static final int INET_DIAG_MEMINFO = 1;

    public static final int SOCKDIAG_MSG_HEADER_SIZE =
            StructNlMsgHdr.STRUCT_SIZE + StructInetDiagMsg.STRUCT_SIZE;

    /**
     * Get the aligned length based on a Short type number.
     */
    public static final int alignedLengthOf(short length) {
        final int intLength = (int) length & 0xffff;
        return alignedLengthOf(intLength);
    }

    /**
     * Get the aligned length based on a Integer type number.
     */
    public static final int alignedLengthOf(int length) {
        if (length <= 0) return 0;
        return (((length + NLA_ALIGNTO - 1) / NLA_ALIGNTO) * NLA_ALIGNTO);
    }

    /**
     * Convert a address family type to a string.
     */
    public static String stringForAddressFamily(int family) {
        if (family == OsConstants.AF_INET) return "AF_INET";
        if (family == OsConstants.AF_INET6) return "AF_INET6";
        if (family == OsConstants.AF_NETLINK) return "AF_NETLINK";
        if (family == OsConstants.AF_UNSPEC) return "AF_UNSPEC";
        return String.valueOf(family);
    }

    /**
     * Convert a protocol type to a string.
     */
    public static String stringForProtocol(int protocol) {
        if (protocol == OsConstants.IPPROTO_TCP) return "IPPROTO_TCP";
        if (protocol == OsConstants.IPPROTO_UDP) return "IPPROTO_UDP";
        return String.valueOf(protocol);
    }

    /**
     * Convert a byte array to a hexadecimal string.
     */
    public static String hexify(byte[] bytes) {
        if (bytes == null) return "(null)";
        return toHexString(bytes, 0, bytes.length);
    }

    /**
     * Convert a {@link ByteBuffer} to a hexadecimal string.
     */
    public static String hexify(ByteBuffer buffer) {
        if (buffer == null) return "(null)";
        return toHexString(
                buffer.array(), buffer.position(), buffer.remaining());
    }

    // Known values for struct nlmsghdr nlm_type.
    public static final short NLMSG_NOOP                    = 1;   // Nothing
    public static final short NLMSG_ERROR                   = 2;   // Error
    public static final short NLMSG_DONE                    = 3;   // End of a dump
    public static final short NLMSG_OVERRUN                 = 4;   // Data lost
    public static final short NLMSG_MAX_RESERVED            = 15;  // Max reserved value

    public static final short RTM_NEWLINK                   = 16;
    public static final short RTM_DELLINK                   = 17;
    public static final short RTM_GETLINK                   = 18;
    public static final short RTM_SETLINK                   = 19;
    public static final short RTM_NEWADDR                   = 20;
    public static final short RTM_DELADDR                   = 21;
    public static final short RTM_GETADDR                   = 22;
    public static final short RTM_NEWROUTE                  = 24;
    public static final short RTM_DELROUTE                  = 25;
    public static final short RTM_GETROUTE                  = 26;
    public static final short RTM_NEWNEIGH                  = 28;
    public static final short RTM_DELNEIGH                  = 29;
    public static final short RTM_GETNEIGH                  = 30;
    public static final short RTM_NEWRULE                   = 32;
    public static final short RTM_DELRULE                   = 33;
    public static final short RTM_GETRULE                   = 34;
    public static final short RTM_NEWNDUSEROPT              = 68;

    // Netfilter netlink message types are presented by two bytes: high byte subsystem and
    // low byte operation. See the macro NFNL_SUBSYS_ID and NFNL_MSG_TYPE in
    // include/uapi/linux/netfilter/nfnetlink.h
    public static final short NFNL_SUBSYS_CTNETLINK         = 1;

    public static final short IPCTNL_MSG_CT_NEW             = 0;
    public static final short IPCTNL_MSG_CT_GET             = 1;
    public static final short IPCTNL_MSG_CT_DELETE          = 2;
    public static final short IPCTNL_MSG_CT_GET_CTRZERO     = 3;
    public static final short IPCTNL_MSG_CT_GET_STATS_CPU   = 4;
    public static final short IPCTNL_MSG_CT_GET_STATS       = 5;
    public static final short IPCTNL_MSG_CT_GET_DYING       = 6;
    public static final short IPCTNL_MSG_CT_GET_UNCONFIRMED = 7;

    /* see include/uapi/linux/sock_diag.h */
    public static final short SOCK_DIAG_BY_FAMILY = 20;

    // Netlink groups.
    public static final int RTMGRP_LINK = 1;
    public static final int RTMGRP_IPV4_IFADDR = 0x10;
    public static final int RTMGRP_IPV6_IFADDR = 0x100;
    public static final int RTMGRP_IPV6_ROUTE  = 0x400;
    public static final int RTNLGRP_ND_USEROPT = 20;
    public static final int RTMGRP_ND_USEROPT = 1 << (RTNLGRP_ND_USEROPT - 1);

    // Device flags.
    public static final int IFF_UP       = 1 << 0;
    public static final int IFF_LOWER_UP = 1 << 16;

    // Known values for struct rtmsg rtm_protocol.
    public static final short RTPROT_KERNEL     = 2;
    public static final short RTPROT_RA         = 9;

    // Known values for struct rtmsg rtm_scope.
    public static final short RT_SCOPE_UNIVERSE = 0;

    // Known values for struct rtmsg rtm_type.
    public static final short RTN_UNICAST       = 1;

    // Known values for struct rtmsg rtm_flags.
    public static final int RTM_F_CLONED        = 0x200;

    /**
     * Convert a netlink message type to a string for control message.
     */
    @NonNull
    private static String stringForCtlMsgType(short nlmType) {
        switch (nlmType) {
            case NLMSG_NOOP: return "NLMSG_NOOP";
            case NLMSG_ERROR: return "NLMSG_ERROR";
            case NLMSG_DONE: return "NLMSG_DONE";
            case NLMSG_OVERRUN: return "NLMSG_OVERRUN";
            default: return "unknown control message type: " + String.valueOf(nlmType);
        }
    }

    /**
     * Convert a netlink message type to a string for NETLINK_ROUTE.
     */
    @NonNull
    private static String stringForRtMsgType(short nlmType) {
        switch (nlmType) {
            case RTM_NEWLINK: return "RTM_NEWLINK";
            case RTM_DELLINK: return "RTM_DELLINK";
            case RTM_GETLINK: return "RTM_GETLINK";
            case RTM_SETLINK: return "RTM_SETLINK";
            case RTM_NEWADDR: return "RTM_NEWADDR";
            case RTM_DELADDR: return "RTM_DELADDR";
            case RTM_GETADDR: return "RTM_GETADDR";
            case RTM_NEWROUTE: return "RTM_NEWROUTE";
            case RTM_DELROUTE: return "RTM_DELROUTE";
            case RTM_GETROUTE: return "RTM_GETROUTE";
            case RTM_NEWNEIGH: return "RTM_NEWNEIGH";
            case RTM_DELNEIGH: return "RTM_DELNEIGH";
            case RTM_GETNEIGH: return "RTM_GETNEIGH";
            case RTM_NEWRULE: return "RTM_NEWRULE";
            case RTM_DELRULE: return "RTM_DELRULE";
            case RTM_GETRULE: return "RTM_GETRULE";
            case RTM_NEWNDUSEROPT: return "RTM_NEWNDUSEROPT";
            default: return "unknown RTM type: " + String.valueOf(nlmType);
        }
    }

    /**
     * Convert a netlink message type to a string for NETLINK_INET_DIAG.
     */
    @NonNull
    private static String stringForInetDiagMsgType(short nlmType) {
        switch (nlmType) {
            case SOCK_DIAG_BY_FAMILY: return "SOCK_DIAG_BY_FAMILY";
            default: return "unknown SOCK_DIAG type: " + String.valueOf(nlmType);
        }
    }

    /**
     * Convert a netlink message type to a string for NETLINK_NETFILTER.
     */
    @NonNull
    private static String stringForNfMsgType(short nlmType) {
        final byte subsysId = (byte) (nlmType >> 8);
        final byte msgType = (byte) nlmType;
        switch (subsysId) {
            case NFNL_SUBSYS_CTNETLINK:
                switch (msgType) {
                    case IPCTNL_MSG_CT_NEW: return "IPCTNL_MSG_CT_NEW";
                    case IPCTNL_MSG_CT_GET: return "IPCTNL_MSG_CT_GET";
                    case IPCTNL_MSG_CT_DELETE: return "IPCTNL_MSG_CT_DELETE";
                    case IPCTNL_MSG_CT_GET_CTRZERO: return "IPCTNL_MSG_CT_GET_CTRZERO";
                    case IPCTNL_MSG_CT_GET_STATS_CPU: return "IPCTNL_MSG_CT_GET_STATS_CPU";
                    case IPCTNL_MSG_CT_GET_STATS: return "IPCTNL_MSG_CT_GET_STATS";
                    case IPCTNL_MSG_CT_GET_DYING: return "IPCTNL_MSG_CT_GET_DYING";
                    case IPCTNL_MSG_CT_GET_UNCONFIRMED: return "IPCTNL_MSG_CT_GET_UNCONFIRMED";
                }
                break;
        }
        return "unknown NETFILTER type: " + String.valueOf(nlmType);
    }

    /**
     * Convert a netlink message type to a string by netlink family.
     */
    @NonNull
    public static String stringForNlMsgType(short nlmType, int nlFamily) {
        // Reserved control messages. The netlink family is ignored.
        // See NLMSG_MIN_TYPE in include/uapi/linux/netlink.h.
        if (nlmType <= NLMSG_MAX_RESERVED) return stringForCtlMsgType(nlmType);

        // Netlink family messages. The netlink family is required. Note that the reason for using
        // if-statement is that switch-case can't be used because the OsConstants.NETLINK_* are
        // not constant.
        if (nlFamily == OsConstants.NETLINK_ROUTE) return stringForRtMsgType(nlmType);
        if (nlFamily == OsConstants.NETLINK_INET_DIAG) return stringForInetDiagMsgType(nlmType);
        if (nlFamily == OsConstants.NETLINK_NETFILTER) return stringForNfMsgType(nlmType);

        return "unknown type: " + String.valueOf(nlmType);
    }

    private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F' };
    /**
     * Convert a byte array to a hexadecimal string.
     */
    public static String toHexString(byte[] array, int offset, int length) {
        char[] buf = new char[length * 2];

        int bufIndex = 0;
        for (int i = offset; i < offset + length; i++) {
            byte b = array[i];
            buf[bufIndex++] = HEX_DIGITS[(b >>> 4) & 0x0F];
            buf[bufIndex++] = HEX_DIGITS[b & 0x0F];
        }

        return new String(buf);
    }
}
