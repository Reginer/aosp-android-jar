/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.os.Process.INVALID_UID;
import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.AF_INET6;
import static android.system.OsConstants.ENOENT;
import static android.system.OsConstants.IPPROTO_TCP;
import static android.system.OsConstants.IPPROTO_UDP;
import static android.system.OsConstants.NETLINK_INET_DIAG;

import static com.android.net.module.util.netlink.NetlinkConstants.NLMSG_DONE;
import static com.android.net.module.util.netlink.NetlinkConstants.SOCK_DESTROY;
import static com.android.net.module.util.netlink.NetlinkConstants.SOCK_DIAG_BY_FAMILY;
import static com.android.net.module.util.netlink.NetlinkConstants.hexify;
import static com.android.net.module.util.netlink.NetlinkConstants.stringForAddressFamily;
import static com.android.net.module.util.netlink.NetlinkConstants.stringForProtocol;
import static com.android.net.module.util.netlink.NetlinkUtils.DEFAULT_RECV_BUFSIZE;
import static com.android.net.module.util.netlink.NetlinkUtils.IO_TIMEOUT_MS;
import static com.android.net.module.util.netlink.NetlinkUtils.TCP_ALIVE_STATE_FILTER;
import static com.android.net.module.util.netlink.NetlinkUtils.connectSocketToNetlink;
import static com.android.net.module.util.netlink.StructNlMsgHdr.NLM_F_DUMP;
import static com.android.net.module.util.netlink.StructNlMsgHdr.NLM_F_REQUEST;

import android.net.util.SocketUtils;
import android.os.Process;
import android.system.ErrnoException;
import android.util.Log;
import android.util.Range;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A NetlinkMessage subclass for netlink inet_diag messages.
 *
 * see also: &lt;linux_src&gt;/include/uapi/linux/inet_diag.h
 *
 * @hide
 */
public class InetDiagMessage extends NetlinkMessage {
    public static final String TAG = "InetDiagMessage";
    private static final int TIMEOUT_MS = 500;

    /**
     * Construct an inet_diag_req_v2 message. This method will throw
     * {@link IllegalArgumentException} if local and remote are not both null or both non-null.
     */
    public static byte[] inetDiagReqV2(int protocol, InetSocketAddress local,
            InetSocketAddress remote, int family, short flags) {
        return inetDiagReqV2(protocol, local, remote, family, flags, 0 /* pad */,
                0 /* idiagExt */, StructInetDiagReqV2.INET_DIAG_REQ_V2_ALL_STATES);
    }

    /**
     * Construct an inet_diag_req_v2 message. This method will throw
     * {@code IllegalArgumentException} if local and remote are not both null or both non-null.
     *
     * @param protocol the request protocol type. This should be set to one of IPPROTO_TCP,
     *                 IPPROTO_UDP, or IPPROTO_UDPLITE.
     * @param local local socket address of the target socket. This will be packed into a
     *              {@link StructInetDiagSockId}. Request to diagnose for all sockets if both of
     *              local or remote address is null.
     * @param remote remote socket address of the target socket. This will be packed into a
     *              {@link StructInetDiagSockId}. Request to diagnose for all sockets if both of
     *              local or remote address is null.
     * @param family the ip family of the request message. This should be set to either AF_INET or
     *               AF_INET6 for IPv4 or IPv6 sockets respectively.
     * @param flags message flags. See &lt;linux_src&gt;/include/uapi/linux/netlink.h.
     * @param pad for raw socket protocol specification.
     * @param idiagExt a set of flags defining what kind of extended information to report.
     * @param state a bit mask that defines a filter of socket states.
     *
     * @return bytes array representation of the message
     */
    public static byte[] inetDiagReqV2(int protocol, @Nullable InetSocketAddress local,
            @Nullable InetSocketAddress remote, int family, short flags, int pad, int idiagExt,
            int state) throws IllegalArgumentException {
        // Request for all sockets if no specific socket is requested. Specify the local and remote
        // socket address information for target request socket.
        if ((local == null) != (remote == null)) {
            throw new IllegalArgumentException(
                    "Local and remote must be both null or both non-null");
        }
        final StructInetDiagSockId id = ((local != null && remote != null)
                ? new StructInetDiagSockId(local, remote) : null);
        return inetDiagReqV2(protocol, id, family,
                SOCK_DIAG_BY_FAMILY, flags, pad, idiagExt, state);
    }

    /**
     * Construct an inet_diag_req_v2 message.
     *
     * @param protocol the request protocol type. This should be set to one of IPPROTO_TCP,
     *                 IPPROTO_UDP, or IPPROTO_UDPLITE.
     * @param id inet_diag_sockid. See {@link StructInetDiagSockId}
     * @param family the ip family of the request message. This should be set to either AF_INET or
     *               AF_INET6 for IPv4 or IPv6 sockets respectively.
     * @param type message types.
     * @param flags message flags. See &lt;linux_src&gt;/include/uapi/linux/netlink.h.
     * @param pad for raw socket protocol specification.
     * @param idiagExt a set of flags defining what kind of extended information to report.
     * @param state a bit mask that defines a filter of socket states.
     * @return bytes array representation of the message
     */
    public static byte[] inetDiagReqV2(int protocol, @Nullable StructInetDiagSockId id, int family,
            short type, short flags, int pad, int idiagExt, int state) {
        final byte[] bytes = new byte[StructNlMsgHdr.STRUCT_SIZE + StructInetDiagReqV2.STRUCT_SIZE];
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.nativeOrder());

        final StructNlMsgHdr nlMsgHdr = new StructNlMsgHdr();
        nlMsgHdr.nlmsg_len = bytes.length;
        nlMsgHdr.nlmsg_type = type;
        nlMsgHdr.nlmsg_flags = flags;
        nlMsgHdr.pack(byteBuffer);
        final StructInetDiagReqV2 inetDiagReqV2 =
                new StructInetDiagReqV2(protocol, id, family, pad, idiagExt, state);

        inetDiagReqV2.pack(byteBuffer);
        return bytes;
    }

    public StructInetDiagMsg inetDiagMsg;

    @VisibleForTesting
    public InetDiagMessage(@NonNull StructNlMsgHdr header) {
        super(header);
        inetDiagMsg = new StructInetDiagMsg();
    }

    /**
     * Parse an inet_diag_req_v2 message from buffer.
     */
    @Nullable
    public static InetDiagMessage parse(@NonNull StructNlMsgHdr header,
            @NonNull ByteBuffer byteBuffer) {
        final InetDiagMessage msg = new InetDiagMessage(header);
        msg.inetDiagMsg = StructInetDiagMsg.parse(byteBuffer);
        if (msg.inetDiagMsg == null) {
            return null;
        }
        return msg;
    }

    private static void closeSocketQuietly(final FileDescriptor fd) {
        try {
            SocketUtils.closeSocket(fd);
        } catch (IOException ignored) {
        }
    }

    private static int lookupUidByFamily(int protocol, InetSocketAddress local,
                                         InetSocketAddress remote, int family, short flags,
                                         FileDescriptor fd)
            throws ErrnoException, InterruptedIOException {
        byte[] msg = inetDiagReqV2(protocol, local, remote, family, flags);
        NetlinkUtils.sendMessage(fd, msg, 0, msg.length, TIMEOUT_MS);
        ByteBuffer response = NetlinkUtils.recvMessage(fd, DEFAULT_RECV_BUFSIZE, TIMEOUT_MS);

        final NetlinkMessage nlMsg = NetlinkMessage.parse(response, NETLINK_INET_DIAG);
        if (nlMsg == null) {
            return INVALID_UID;
        }
        final StructNlMsgHdr hdr = nlMsg.getHeader();
        if (hdr.nlmsg_type == NetlinkConstants.NLMSG_DONE) {
            return INVALID_UID;
        }
        if (nlMsg instanceof InetDiagMessage) {
            return ((InetDiagMessage) nlMsg).inetDiagMsg.idiag_uid;
        }
        return INVALID_UID;
    }

    private static final int[] FAMILY = {AF_INET6, AF_INET};

    private static int lookupUid(int protocol, InetSocketAddress local,
                                 InetSocketAddress remote, FileDescriptor fd)
            throws ErrnoException, InterruptedIOException {
        int uid;

        for (int family : FAMILY) {
            /**
             * For exact match lookup, swap local and remote for UDP lookups due to kernel
             * bug which will not be fixed. See aosp/755889 and
             * https://www.mail-archive.com/netdev@vger.kernel.org/msg248638.html
             */
            if (protocol == IPPROTO_UDP) {
                uid = lookupUidByFamily(protocol, remote, local, family, NLM_F_REQUEST, fd);
            } else {
                uid = lookupUidByFamily(protocol, local, remote, family, NLM_F_REQUEST, fd);
            }
            if (uid != INVALID_UID) {
                return uid;
            }
        }

        /**
         * For UDP it's possible for a socket to send packets to arbitrary destinations, even if the
         * socket is not connected (and even if the socket is connected to a different destination).
         * If we want this API to work for such packets, then on miss we need to do a second lookup
         * with only the local address and port filled in.
         * Always use flags == NLM_F_REQUEST | NLM_F_DUMP for wildcard.
         */
        if (protocol == IPPROTO_UDP) {
            try {
                InetSocketAddress wildcard = new InetSocketAddress(
                        Inet6Address.getByName("::"), 0);
                uid = lookupUidByFamily(protocol, local, wildcard, AF_INET6,
                        (short) (NLM_F_REQUEST | NLM_F_DUMP), fd);
                if (uid != INVALID_UID) {
                    return uid;
                }
                wildcard = new InetSocketAddress(Inet4Address.getByName("0.0.0.0"), 0);
                uid = lookupUidByFamily(protocol, local, wildcard, AF_INET,
                        (short) (NLM_F_REQUEST | NLM_F_DUMP), fd);
                if (uid != INVALID_UID) {
                    return uid;
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, e.toString());
            }
        }
        return INVALID_UID;
    }

    /**
     * Use an inet_diag socket to look up the UID associated with the input local and remote
     * address/port and protocol of a connection.
     */
    public static int getConnectionOwnerUid(int protocol, InetSocketAddress local,
                                            InetSocketAddress remote) {
        int uid = INVALID_UID;
        FileDescriptor fd = null;
        try {
            fd = NetlinkUtils.netlinkSocketForProto(NETLINK_INET_DIAG);
            NetlinkUtils.connectSocketToNetlink(fd);
            uid = lookupUid(protocol, local, remote, fd);
        } catch (ErrnoException | SocketException | IllegalArgumentException
                | InterruptedIOException e) {
            Log.e(TAG, e.toString());
        } finally {
            closeSocketQuietly(fd);
        }
        return uid;
    }

    /**
     * Construct an inet_diag_req_v2 message for querying alive TCP sockets from kernel.
     */
    public static byte[] buildInetDiagReqForAliveTcpSockets(int family) {
        return inetDiagReqV2(IPPROTO_TCP,
                null /* local addr */,
                null /* remote addr */,
                family,
                (short) (StructNlMsgHdr.NLM_F_REQUEST | StructNlMsgHdr.NLM_F_DUMP) /* flag */,
                0 /* pad */,
                1 << NetlinkConstants.INET_DIAG_MEMINFO /* idiagExt */,
                TCP_ALIVE_STATE_FILTER);
    }

    private static void sendNetlinkDestroyRequest(FileDescriptor fd, int proto,
            InetDiagMessage diagMsg) throws InterruptedIOException, ErrnoException {
        final byte[] destroyMsg = InetDiagMessage.inetDiagReqV2(
                proto,
                diagMsg.inetDiagMsg.id,
                diagMsg.inetDiagMsg.idiag_family,
                SOCK_DESTROY,
                (short) (StructNlMsgHdr.NLM_F_REQUEST | StructNlMsgHdr.NLM_F_ACK),
                0 /* pad */,
                0 /* idiagExt */,
                1 << diagMsg.inetDiagMsg.idiag_state
        );
        NetlinkUtils.sendMessage(fd, destroyMsg, 0, destroyMsg.length, IO_TIMEOUT_MS);
        NetlinkUtils.receiveNetlinkAck(fd);
    }

    private static void sendNetlinkDumpRequest(FileDescriptor fd, int proto, int states, int family)
            throws InterruptedIOException, ErrnoException {
        final byte[] dumpMsg = InetDiagMessage.inetDiagReqV2(
                proto,
                null /* id */,
                family,
                SOCK_DIAG_BY_FAMILY,
                (short) (StructNlMsgHdr.NLM_F_REQUEST | StructNlMsgHdr.NLM_F_DUMP),
                0 /* pad */,
                0 /* idiagExt */,
                states);
        NetlinkUtils.sendMessage(fd, dumpMsg, 0, dumpMsg.length, IO_TIMEOUT_MS);
    }

    private static int processNetlinkDumpAndDestroySockets(FileDescriptor dumpFd,
            FileDescriptor destroyFd, int proto, Predicate<InetDiagMessage> filter)
            throws InterruptedIOException, ErrnoException {
        int destroyedSockets = 0;

        while (true) {
            final ByteBuffer buf = NetlinkUtils.recvMessage(
                    dumpFd, DEFAULT_RECV_BUFSIZE, IO_TIMEOUT_MS);

            while (buf.remaining() > 0) {
                final int position = buf.position();
                final NetlinkMessage nlMsg = NetlinkMessage.parse(buf, NETLINK_INET_DIAG);
                if (nlMsg == null) {
                    // Move to the position where parse started for error log.
                    buf.position(position);
                    Log.e(TAG, "Failed to parse netlink message: " + hexify(buf));
                    break;
                }

                if (nlMsg.getHeader().nlmsg_type == NLMSG_DONE) {
                    return destroyedSockets;
                }

                if (!(nlMsg instanceof InetDiagMessage)) {
                    Log.wtf(TAG, "Received unexpected netlink message: " + nlMsg);
                    continue;
                }

                final InetDiagMessage diagMsg = (InetDiagMessage) nlMsg;
                if (filter.test(diagMsg)) {
                    try {
                        sendNetlinkDestroyRequest(destroyFd, proto, diagMsg);
                        destroyedSockets++;
                    } catch (InterruptedIOException | ErrnoException e) {
                        if (!(e instanceof ErrnoException
                                && ((ErrnoException) e).errno == ENOENT)) {
                            Log.e(TAG, "Failed to destroy socket: diagMsg=" + diagMsg + ", " + e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns whether the InetDiagMessage is for adb socket or not
     */
    @VisibleForTesting
    public static boolean isAdbSocket(final InetDiagMessage msg) {
        // This is inaccurate since adb could run with ROOT_UID or other services can run with
        // SHELL_UID. But this check covers most cases and enough.
        // Note that getting service.adb.tcp.port system property is prohibited by sepolicy
        // TODO: skip the socket only if there is a listen socket owned by SHELL_UID with the same
        // source port as this socket
        return msg.inetDiagMsg.idiag_uid == Process.SHELL_UID;
    }

    /**
     * Returns whether the range contains the uid in the InetDiagMessage or not
     */
    @VisibleForTesting
    public static boolean containsUid(InetDiagMessage msg, Set<Range<Integer>> ranges) {
        for (final Range<Integer> range: ranges) {
            if (range.contains(msg.inetDiagMsg.idiag_uid)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLoopbackAddress(InetAddress addr) {
        if (addr.isLoopbackAddress()) return true;
        if (!(addr instanceof Inet6Address)) return false;

        // Following check is for v4-mapped v6 address. StructInetDiagSockId contains v4-mapped v6
        // address as Inet6Address, See StructInetDiagSockId#parse
        final byte[] addrBytes = addr.getAddress();
        for (int i = 0; i < 10; i++) {
            if (addrBytes[i] != 0) return false;
        }
        return addrBytes[10] == (byte) 0xff
                && addrBytes[11] == (byte) 0xff
                && addrBytes[12] == 127;
    }

    /**
     * Returns whether the socket address in the InetDiagMessage is loopback or not
     */
    @VisibleForTesting
    public static boolean isLoopback(InetDiagMessage msg) {
        final InetAddress srcAddr = msg.inetDiagMsg.id.locSocketAddress.getAddress();
        final InetAddress dstAddr = msg.inetDiagMsg.id.remSocketAddress.getAddress();
        return isLoopbackAddress(srcAddr)
                || isLoopbackAddress(dstAddr)
                || srcAddr.equals(dstAddr);
    }

    private static void destroySockets(int proto, int states, Predicate<InetDiagMessage> filter)
            throws ErrnoException, SocketException, InterruptedIOException {
        FileDescriptor dumpFd = null;
        FileDescriptor destroyFd = null;

        try {
            dumpFd = NetlinkUtils.createNetLinkInetDiagSocket();
            destroyFd = NetlinkUtils.createNetLinkInetDiagSocket();
            connectSocketToNetlink(dumpFd);
            connectSocketToNetlink(destroyFd);

            for (int family : List.of(AF_INET, AF_INET6)) {
                try {
                    sendNetlinkDumpRequest(dumpFd, proto, states, family);
                } catch (InterruptedIOException | ErrnoException e) {
                    Log.e(TAG, "Failed to send netlink dump request: " + e);
                    continue;
                }
                final int destroyedSockets = processNetlinkDumpAndDestroySockets(
                        dumpFd, destroyFd, proto, filter);
                Log.d(TAG, "Destroyed " + destroyedSockets + " sockets"
                        + ", proto=" + stringForProtocol(proto)
                        + ", family=" + stringForAddressFamily(family)
                        + ", states=" + states);
            }
        } finally {
            closeSocketQuietly(dumpFd);
            closeSocketQuietly(destroyFd);
        }
    }

    /**
     * Close tcp sockets that match the following condition
     *  1. TCP status is one of TCP_ESTABLISHED, TCP_SYN_SENT, and TCP_SYN_RECV
     *  2. Owner uid of socket is not in the exemptUids
     *  3. Owner uid of socket is in the ranges
     *  4. Socket is not loopback
     *  5. Socket is not adb socket
     *
     * @param ranges target uid ranges
     * @param exemptUids uids to skip close socket
     */
    public static void destroyLiveTcpSockets(Set<Range<Integer>> ranges, Set<Integer> exemptUids)
            throws SocketException, InterruptedIOException, ErrnoException {
        destroySockets(IPPROTO_TCP, TCP_ALIVE_STATE_FILTER,
                (diagMsg) -> !exemptUids.contains(diagMsg.inetDiagMsg.idiag_uid)
                        && containsUid(diagMsg, ranges)
                        && !isLoopback(diagMsg)
                        && !isAdbSocket(diagMsg));
    }

    /**
     * Close tcp sockets that match the following condition
     *  1. TCP status is one of TCP_ESTABLISHED, TCP_SYN_SENT, and TCP_SYN_RECV
     *  2. Owner uid of socket is in the targetUids
     *  3. Socket is not loopback
     *  4. Socket is not adb socket
     *
     * @param ownerUids target uids to close sockets
     */
    public static void destroyLiveTcpSocketsByOwnerUids(Set<Integer> ownerUids)
            throws SocketException, InterruptedIOException, ErrnoException {
        destroySockets(IPPROTO_TCP, TCP_ALIVE_STATE_FILTER,
                (diagMsg) -> ownerUids.contains(diagMsg.inetDiagMsg.idiag_uid)
                        && !isLoopback(diagMsg)
                        && !isAdbSocket(diagMsg));
    }

    @Override
    public String toString() {
        return "InetDiagMessage{ "
                + "nlmsghdr{"
                + (mHeader == null ? "" : mHeader.toString(NETLINK_INET_DIAG)) + "}, "
                + "inet_diag_msg{"
                + (inetDiagMsg == null ? "" : inetDiagMsg.toString()) + "} "
                + "}";
    }
}
