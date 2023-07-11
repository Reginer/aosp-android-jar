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

import static android.system.OsConstants.IPPROTO_TCP;
import static android.system.OsConstants.IPPROTO_UDP;

import static com.android.net.module.util.netlink.StructNlAttr.findNextAttrOfType;
import static com.android.net.module.util.netlink.StructNlAttr.makeNestedType;
import static com.android.net.module.util.netlink.StructNlMsgHdr.NLM_F_ACK;
import static com.android.net.module.util.netlink.StructNlMsgHdr.NLM_F_REPLACE;
import static com.android.net.module.util.netlink.StructNlMsgHdr.NLM_F_REQUEST;

import static java.nio.ByteOrder.BIG_ENDIAN;

import android.system.OsConstants;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * A NetlinkMessage subclass for netlink conntrack messages.
 *
 * see also: &lt;linux_src&gt;/include/uapi/linux/netfilter/nfnetlink_conntrack.h
 *
 * @hide
 */
public class ConntrackMessage extends NetlinkMessage {
    public static final int STRUCT_SIZE = StructNlMsgHdr.STRUCT_SIZE + StructNfGenMsg.STRUCT_SIZE;

    // enum ctattr_type
    public static final short CTA_TUPLE_ORIG  = 1;
    public static final short CTA_TUPLE_REPLY = 2;
    public static final short CTA_STATUS      = 3;
    public static final short CTA_TIMEOUT     = 7;

    // enum ctattr_tuple
    public static final short CTA_TUPLE_IP    = 1;
    public static final short CTA_TUPLE_PROTO = 2;

    // enum ctattr_ip
    public static final short CTA_IP_V4_SRC = 1;
    public static final short CTA_IP_V4_DST = 2;

    // enum ctattr_l4proto
    public static final short CTA_PROTO_NUM      = 1;
    public static final short CTA_PROTO_SRC_PORT = 2;
    public static final short CTA_PROTO_DST_PORT = 3;

    // enum ip_conntrack_status
    public static final int IPS_EXPECTED      = 0x00000001;
    public static final int IPS_SEEN_REPLY    = 0x00000002;
    public static final int IPS_ASSURED       = 0x00000004;
    public static final int IPS_CONFIRMED     = 0x00000008;
    public static final int IPS_SRC_NAT       = 0x00000010;
    public static final int IPS_DST_NAT       = 0x00000020;
    public static final int IPS_SEQ_ADJUST    = 0x00000040;
    public static final int IPS_SRC_NAT_DONE  = 0x00000080;
    public static final int IPS_DST_NAT_DONE  = 0x00000100;
    public static final int IPS_DYING         = 0x00000200;
    public static final int IPS_FIXED_TIMEOUT = 0x00000400;
    public static final int IPS_TEMPLATE      = 0x00000800;
    public static final int IPS_UNTRACKED     = 0x00001000;
    public static final int IPS_HELPER        = 0x00002000;
    public static final int IPS_OFFLOAD       = 0x00004000;
    public static final int IPS_HW_OFFLOAD    = 0x00008000;

    // ip_conntrack_status mask
    // Interesting on the NAT conntrack session which has already seen two direction traffic.
    // TODO: Probably IPS_{SRC, DST}_NAT_DONE are also interesting.
    public static final int ESTABLISHED_MASK = IPS_CONFIRMED | IPS_ASSURED | IPS_SEEN_REPLY
            | IPS_SRC_NAT;
    // Interesting on the established NAT conntrack session which is dying.
    public static final int DYING_MASK = ESTABLISHED_MASK | IPS_DYING;

    /**
     * A tuple for the conntrack connection information.
     *
     * see also CTA_TUPLE_ORIG and CTA_TUPLE_REPLY.
     */
    public static class Tuple {
        public final Inet4Address srcIp;
        public final Inet4Address dstIp;

        // Both port and protocol number are unsigned numbers stored in signed integers, and that
        // callers that want to compare them to integers should either cast those integers, or
        // convert them to unsigned using Byte.toUnsignedInt() and Short.toUnsignedInt().
        public final short srcPort;
        public final short dstPort;
        public final byte protoNum;

        public Tuple(TupleIpv4 ip, TupleProto proto) {
            this.srcIp = ip.src;
            this.dstIp = ip.dst;
            this.srcPort = proto.srcPort;
            this.dstPort = proto.dstPort;
            this.protoNum = proto.protoNum;
        }

        @Override
        @VisibleForTesting
        public boolean equals(Object o) {
            if (!(o instanceof Tuple)) return false;
            Tuple that = (Tuple) o;
            return Objects.equals(this.srcIp, that.srcIp)
                    && Objects.equals(this.dstIp, that.dstIp)
                    && this.srcPort == that.srcPort
                    && this.dstPort == that.dstPort
                    && this.protoNum == that.protoNum;
        }

        @Override
        public int hashCode() {
            return Objects.hash(srcIp, dstIp, srcPort, dstPort, protoNum);
        }

        @Override
        public String toString() {
            final String srcIpStr = (srcIp == null) ? "null" : srcIp.getHostAddress();
            final String dstIpStr = (dstIp == null) ? "null" : dstIp.getHostAddress();
            final String protoStr = NetlinkConstants.stringForProtocol(protoNum);

            return "Tuple{"
                    + protoStr + ": "
                    + srcIpStr + ":" + Short.toUnsignedInt(srcPort) + " -> "
                    + dstIpStr + ":" + Short.toUnsignedInt(dstPort)
                    + "}";
        }
    }

    /**
     * A tuple for the conntrack connection address.
     *
     * see also CTA_TUPLE_IP.
     */
    public static class TupleIpv4 {
        public final Inet4Address src;
        public final Inet4Address dst;

        public TupleIpv4(Inet4Address src, Inet4Address dst) {
            this.src = src;
            this.dst = dst;
        }
    }

    /**
     * A tuple for the conntrack connection protocol.
     *
     * see also CTA_TUPLE_PROTO.
     */
    public static class TupleProto {
        public final byte protoNum;
        public final short srcPort;
        public final short dstPort;

        public TupleProto(byte protoNum, short srcPort, short dstPort) {
            this.protoNum = protoNum;
            this.srcPort = srcPort;
            this.dstPort = dstPort;
        }
    }

    /**
     * Create a netlink message to refresh IPv4 conntrack entry timeout.
     */
    public static byte[] newIPv4TimeoutUpdateRequest(
            int proto, Inet4Address src, int sport, Inet4Address dst, int dport, int timeoutSec) {
        // *** STYLE WARNING ***
        //
        // Code below this point uses extra block indentation to highlight the
        // packing of nested tuple netlink attribute types.
        final StructNlAttr ctaTupleOrig = new StructNlAttr(CTA_TUPLE_ORIG,
                new StructNlAttr(CTA_TUPLE_IP,
                        new StructNlAttr(CTA_IP_V4_SRC, src),
                        new StructNlAttr(CTA_IP_V4_DST, dst)),
                new StructNlAttr(CTA_TUPLE_PROTO,
                        new StructNlAttr(CTA_PROTO_NUM, (byte) proto),
                        new StructNlAttr(CTA_PROTO_SRC_PORT, (short) sport, BIG_ENDIAN),
                        new StructNlAttr(CTA_PROTO_DST_PORT, (short) dport, BIG_ENDIAN)));

        final StructNlAttr ctaTimeout = new StructNlAttr(CTA_TIMEOUT, timeoutSec, BIG_ENDIAN);

        final int payloadLength = ctaTupleOrig.getAlignedLength() + ctaTimeout.getAlignedLength();
        final byte[] bytes = new byte[STRUCT_SIZE + payloadLength];
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.nativeOrder());

        final ConntrackMessage ctmsg = new ConntrackMessage();
        ctmsg.mHeader.nlmsg_len = bytes.length;
        ctmsg.mHeader.nlmsg_type = (NetlinkConstants.NFNL_SUBSYS_CTNETLINK << 8)
                | NetlinkConstants.IPCTNL_MSG_CT_NEW;
        ctmsg.mHeader.nlmsg_flags = NLM_F_REQUEST | NLM_F_ACK | NLM_F_REPLACE;
        ctmsg.mHeader.nlmsg_seq = 1;
        ctmsg.pack(byteBuffer);

        ctaTupleOrig.pack(byteBuffer);
        ctaTimeout.pack(byteBuffer);

        return bytes;
    }

    /**
     * Parses a netfilter conntrack message from a {@link ByteBuffer}.
     *
     * @param header the netlink message header.
     * @param byteBuffer The buffer from which to parse the netfilter conntrack message.
     * @return the parsed netfilter conntrack message, or {@code null} if the netfilter conntrack
     *         message could not be parsed successfully (for example, if it was truncated).
     */
    @Nullable
    public static ConntrackMessage parse(@NonNull StructNlMsgHdr header,
            @NonNull ByteBuffer byteBuffer) {
        // Just build the netlink header and netfilter header for now and pretend the whole message
        // was consumed.
        // TODO: Parse the conntrack attributes.
        final StructNfGenMsg nfGenMsg = StructNfGenMsg.parse(byteBuffer);
        if (nfGenMsg == null) {
            return null;
        }

        final int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = findNextAttrOfType(CTA_STATUS, byteBuffer);
        int status = 0;
        if (nlAttr != null) {
            status = nlAttr.getValueAsBe32(0);
        }

        byteBuffer.position(baseOffset);
        nlAttr = findNextAttrOfType(CTA_TIMEOUT, byteBuffer);
        int timeoutSec = 0;
        if (nlAttr != null) {
            timeoutSec = nlAttr.getValueAsBe32(0);
        }

        byteBuffer.position(baseOffset);
        nlAttr = findNextAttrOfType(makeNestedType(CTA_TUPLE_ORIG), byteBuffer);
        Tuple tupleOrig = null;
        if (nlAttr != null) {
            tupleOrig = parseTuple(nlAttr.getValueAsByteBuffer());
        }

        byteBuffer.position(baseOffset);
        nlAttr = findNextAttrOfType(makeNestedType(CTA_TUPLE_REPLY), byteBuffer);
        Tuple tupleReply = null;
        if (nlAttr != null) {
            tupleReply = parseTuple(nlAttr.getValueAsByteBuffer());
        }

        // Advance to the end of the message.
        byteBuffer.position(baseOffset);
        final int kMinConsumed = StructNlMsgHdr.STRUCT_SIZE + StructNfGenMsg.STRUCT_SIZE;
        final int kAdditionalSpace = NetlinkConstants.alignedLengthOf(
                header.nlmsg_len - kMinConsumed);
        if (byteBuffer.remaining() < kAdditionalSpace) {
            return null;
        }
        byteBuffer.position(baseOffset + kAdditionalSpace);

        return new ConntrackMessage(header, nfGenMsg, tupleOrig, tupleReply, status, timeoutSec);
    }

    /**
     * Parses a conntrack tuple from a {@link ByteBuffer}.
     *
     * The attribute parsing is interesting on:
     * - CTA_TUPLE_IP
     *     CTA_IP_V4_SRC
     *     CTA_IP_V4_DST
     * - CTA_TUPLE_PROTO
     *     CTA_PROTO_NUM
     *     CTA_PROTO_SRC_PORT
     *     CTA_PROTO_DST_PORT
     *
     * Assume that the minimum size is the sum of CTA_TUPLE_IP (size: 20) and CTA_TUPLE_PROTO
     * (size: 28). Here is an example for an expected CTA_TUPLE_ORIG message in raw data:
     * +--------------------------------------------------------------------------------------+
     * | CTA_TUPLE_ORIG                                                                       |
     * +--------------------------+-----------------------------------------------------------+
     * | 1400                     | nla_len = 20                                              |
     * | 0180                     | nla_type = nested CTA_TUPLE_IP                            |
     * |     0800 0100 C0A8500C   |     nla_type=CTA_IP_V4_SRC, ip=192.168.80.12              |
     * |     0800 0200 8C700874   |     nla_type=CTA_IP_V4_DST, ip=140.112.8.116              |
     * | 1C00                     | nla_len = 28                                              |
     * | 0280                     | nla_type = nested CTA_TUPLE_PROTO                         |
     * |     0500 0100 06 000000  |     nla_type=CTA_PROTO_NUM, proto=IPPROTO_TCP (6)         |
     * |     0600 0200 F3F1 0000  |     nla_type=CTA_PROTO_SRC_PORT, port=62449 (big endian)  |
     * |     0600 0300 01BB 0000  |     nla_type=CTA_PROTO_DST_PORT, port=433 (big endian)    |
     * +--------------------------+-----------------------------------------------------------+
     *
     * The position of the byte buffer doesn't set to the end when the function returns. It is okay
     * because the caller ConntrackMessage#parse has passed a copy which is used for this parser
     * only. Moreover, the parser behavior is the same as other existing netlink struct class
     * parser. Ex: StructInetDiagMsg#parse.
     */
    @Nullable
    private static Tuple parseTuple(@Nullable ByteBuffer byteBuffer) {
        if (byteBuffer == null) return null;

        TupleIpv4 tupleIpv4 = null;
        TupleProto tupleProto = null;

        final int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = findNextAttrOfType(makeNestedType(CTA_TUPLE_IP), byteBuffer);
        if (nlAttr != null) {
            tupleIpv4 = parseTupleIpv4(nlAttr.getValueAsByteBuffer());
        }
        if (tupleIpv4 == null) return null;

        byteBuffer.position(baseOffset);
        nlAttr = findNextAttrOfType(makeNestedType(CTA_TUPLE_PROTO), byteBuffer);
        if (nlAttr != null) {
            tupleProto = parseTupleProto(nlAttr.getValueAsByteBuffer());
        }
        if (tupleProto == null) return null;

        return new Tuple(tupleIpv4, tupleProto);
    }

    @Nullable
    private static Inet4Address castToInet4Address(@Nullable InetAddress address) {
        if (address == null || !(address instanceof Inet4Address)) return null;
        return (Inet4Address) address;
    }

    @Nullable
    private static TupleIpv4 parseTupleIpv4(@Nullable ByteBuffer byteBuffer) {
        if (byteBuffer == null) return null;

        Inet4Address src = null;
        Inet4Address dst = null;

        final int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = findNextAttrOfType(CTA_IP_V4_SRC, byteBuffer);
        if (nlAttr != null) {
            src = castToInet4Address(nlAttr.getValueAsInetAddress());
        }
        if (src == null) return null;

        byteBuffer.position(baseOffset);
        nlAttr = findNextAttrOfType(CTA_IP_V4_DST, byteBuffer);
        if (nlAttr != null) {
            dst = castToInet4Address(nlAttr.getValueAsInetAddress());
        }
        if (dst == null) return null;

        return new TupleIpv4(src, dst);
    }

    @Nullable
    private static TupleProto parseTupleProto(@Nullable ByteBuffer byteBuffer) {
        if (byteBuffer == null) return null;

        byte protoNum = 0;
        short srcPort = 0;
        short dstPort = 0;

        final int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = findNextAttrOfType(CTA_PROTO_NUM, byteBuffer);
        if (nlAttr != null) {
            protoNum = nlAttr.getValueAsByte((byte) 0);
        }
        if (!(protoNum == IPPROTO_TCP || protoNum == IPPROTO_UDP)) return null;

        byteBuffer.position(baseOffset);
        nlAttr = StructNlAttr.findNextAttrOfType(CTA_PROTO_SRC_PORT, byteBuffer);
        if (nlAttr != null) {
            srcPort = nlAttr.getValueAsBe16((short) 0);
        }
        if (srcPort == 0) return null;

        byteBuffer.position(baseOffset);
        nlAttr = StructNlAttr.findNextAttrOfType(CTA_PROTO_DST_PORT, byteBuffer);
        if (nlAttr != null) {
            dstPort = nlAttr.getValueAsBe16((short) 0);
        }
        if (dstPort == 0) return null;

        return new TupleProto(protoNum, srcPort, dstPort);
    }

    /**
     * Netfilter header.
     */
    public final StructNfGenMsg nfGenMsg;
    /**
     * Original direction conntrack tuple.
     *
     * The tuple is determined by the parsed attribute value CTA_TUPLE_ORIG, or null if the
     * tuple could not be parsed successfully (for example, if it was truncated or absent).
     */
    @Nullable
    public final Tuple tupleOrig;
    /**
     * Reply direction conntrack tuple.
     *
     * The tuple is determined by the parsed attribute value CTA_TUPLE_REPLY, or null if the
     * tuple could not be parsed successfully (for example, if it was truncated or absent).
     */
    @Nullable
    public final Tuple tupleReply;
    /**
     * Connection status. A bitmask of ip_conntrack_status enum flags.
     *
     * The status is determined by the parsed attribute value CTA_STATUS, or 0 if the status could
     * not be parsed successfully (for example, if it was truncated or absent). For the message
     * from kernel, the valid status is non-zero. For the message from user space, the status may
     * be 0 (absent).
     */
    public final int status;
    /**
     * Conntrack timeout.
     *
     * The timeout is determined by the parsed attribute value CTA_TIMEOUT, or 0 if the timeout
     * could not be parsed successfully (for example, if it was truncated or absent). For
     * IPCTNL_MSG_CT_NEW event, the valid timeout is non-zero. For IPCTNL_MSG_CT_DELETE event, the
     * timeout is 0 (absent).
     */
    public final int timeoutSec;

    private ConntrackMessage() {
        super(new StructNlMsgHdr());
        nfGenMsg = new StructNfGenMsg((byte) OsConstants.AF_INET);

        // This constructor is only used by #newIPv4TimeoutUpdateRequest which doesn't use these
        // data member for packing message. Simply fill them to null or 0.
        tupleOrig = null;
        tupleReply = null;
        status = 0;
        timeoutSec = 0;
    }

    private ConntrackMessage(@NonNull StructNlMsgHdr header, @NonNull StructNfGenMsg nfGenMsg,
            @Nullable Tuple tupleOrig, @Nullable Tuple tupleReply, int status, int timeoutSec) {
        super(header);
        this.nfGenMsg = nfGenMsg;
        this.tupleOrig = tupleOrig;
        this.tupleReply = tupleReply;
        this.status = status;
        this.timeoutSec = timeoutSec;
    }

    /**
     * Write a netfilter message to {@link ByteBuffer}.
     */
    public void pack(ByteBuffer byteBuffer) {
        mHeader.pack(byteBuffer);
        nfGenMsg.pack(byteBuffer);
    }

    public short getMessageType() {
        return (short) (getHeader().nlmsg_type & ~(NetlinkConstants.NFNL_SUBSYS_CTNETLINK << 8));
    }

    /**
     * Convert an ip conntrack status to a string.
     */
    public static String stringForIpConntrackStatus(int flags) {
        final StringBuilder sb = new StringBuilder();

        if ((flags & IPS_EXPECTED) != 0) {
            sb.append("IPS_EXPECTED");
        }
        if ((flags & IPS_SEEN_REPLY) != 0) {
            if (sb.length() > 0) sb.append("|");
            sb.append("IPS_SEEN_REPLY");
        }
        if ((flags & IPS_ASSURED) != 0) {
            if (sb.length() > 0) sb.append("|");
            sb.append("IPS_ASSURED");
        }
        if ((flags & IPS_CONFIRMED) != 0) {
            if (sb.length() > 0) sb.append("|");
            sb.append("IPS_CONFIRMED");
        }
        if ((flags & IPS_SRC_NAT) != 0) {
            if (sb.length() > 0) sb.append("|");
            sb.append("IPS_SRC_NAT");
        }
        if ((flags & IPS_DST_NAT) != 0) {
            if (sb.length() > 0) sb.append("|");
            sb.append("IPS_DST_NAT");
        }
        if ((flags & IPS_SEQ_ADJUST) != 0) {
            if (sb.length() > 0) sb.append("|");
            sb.append("IPS_SEQ_ADJUST");
        }
        if ((flags & IPS_SRC_NAT_DONE) != 0) {
            if (sb.length() > 0) sb.append("|");
            sb.append("IPS_SRC_NAT_DONE");
        }
        if ((flags & IPS_DST_NAT_DONE) != 0) {
            if (sb.length() > 0) sb.append("|");
            sb.append("IPS_DST_NAT_DONE");
        }
        if ((flags & IPS_DYING) != 0) {
            if (sb.length() > 0) sb.append("|");
            sb.append("IPS_DYING");
        }
        if ((flags & IPS_FIXED_TIMEOUT) != 0) {
            if (sb.length() > 0) sb.append("|");
            sb.append("IPS_FIXED_TIMEOUT");
        }
        if ((flags & IPS_TEMPLATE) != 0) {
            if (sb.length() > 0) sb.append("|");
            sb.append("IPS_TEMPLATE");
        }
        if ((flags & IPS_UNTRACKED) != 0) {
            if (sb.length() > 0) sb.append("|");
            sb.append("IPS_UNTRACKED");
        }
        if ((flags & IPS_HELPER) != 0) {
            if (sb.length() > 0) sb.append("|");
            sb.append("IPS_HELPER");
        }
        if ((flags & IPS_OFFLOAD) != 0) {
            if (sb.length() > 0) sb.append("|");
            sb.append("IPS_OFFLOAD");
        }
        if ((flags & IPS_HW_OFFLOAD) != 0) {
            if (sb.length() > 0) sb.append("|");
            sb.append("IPS_HW_OFFLOAD");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ConntrackMessage{"
                + "nlmsghdr{"
                + (mHeader == null ? "" : mHeader.toString(OsConstants.NETLINK_NETFILTER))
                + "}, "
                + "nfgenmsg{" + nfGenMsg + "}, "
                + "tuple_orig{" + tupleOrig + "}, "
                + "tuple_reply{" + tupleReply + "}, "
                + "status{" + status + "(" + stringForIpConntrackStatus(status) + ")" + "}, "
                + "timeout_sec{" + Integer.toUnsignedLong(timeoutSec) + "}"
                + "}";
    }
}
