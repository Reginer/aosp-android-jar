/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.net.module.util.ip;

import static android.system.OsConstants.AF_NETLINK;
import static android.system.OsConstants.ENOBUFS;
import static android.system.OsConstants.SOCK_DGRAM;
import static android.system.OsConstants.SOCK_NONBLOCK;
import static android.system.OsConstants.SOL_SOCKET;
import static android.system.OsConstants.SO_RCVBUF;

import static com.android.net.module.util.SocketUtils.closeSocketQuietly;
import static com.android.net.module.util.SocketUtils.makeNetlinkSocketAddress;
import static com.android.net.module.util.netlink.NetlinkConstants.hexify;

import android.annotation.NonNull;
import android.os.Handler;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import com.android.net.module.util.PacketReader;
import com.android.net.module.util.SharedLog;
import com.android.net.module.util.netlink.NetlinkErrorMessage;
import com.android.net.module.util.netlink.NetlinkMessage;
import com.android.net.module.util.netlink.NetlinkUtils;

import java.io.FileDescriptor;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A simple base class to listen for netlink broadcasts.
 *
 * Opens a netlink socket of the given family and binds to the specified groups. Polls the socket
 * from the event loop of the passed-in {@link Handler}, and calls the subclass-defined
 * {@link #processNetlinkMessage} method on the handler thread for each netlink message that
 * arrives. Currently ignores all netlink errors.
 * @hide
 */
public class NetlinkMonitor extends PacketReader {
    protected final SharedLog mLog;
    protected final String mTag;
    private final int mFamily;
    private final int mBindGroups;
    private final int mSockRcvbufSize;

    private static final boolean DBG = false;

    // Default socket receive buffer size. This means the specific buffer size is not set.
    private static final int DEFAULT_SOCKET_RECV_BUFSIZE = -1;

    /**
     * Constructs a new {@code NetlinkMonitor} instance.
     *
     * @param h The Handler on which to poll for messages and on which to call
     *          {@link #processNetlinkMessage}.
     * @param log A SharedLog to log to.
     * @param tag The log tag to use for log messages.
     * @param family the Netlink socket family to, e.g., {@code NETLINK_ROUTE}.
     * @param bindGroups the netlink groups to bind to.
     * @param sockRcvbufSize the specific socket receive buffer size in bytes. -1 means that don't
     *        set the specific socket receive buffer size in #createFd and use the default value in
     *        /proc/sys/net/core/rmem_default file. See SO_RCVBUF in man-pages/socket.
     */
    public NetlinkMonitor(@NonNull Handler h, @NonNull SharedLog log, @NonNull String tag,
            int family, int bindGroups, int sockRcvbufSize) {
        super(h, NetlinkUtils.DEFAULT_RECV_BUFSIZE);
        mLog = log.forSubComponent(tag);
        mTag = tag;
        mFamily = family;
        mBindGroups = bindGroups;
        mSockRcvbufSize = sockRcvbufSize;
    }

    public NetlinkMonitor(@NonNull Handler h, @NonNull SharedLog log, @NonNull String tag,
            int family, int bindGroups) {
        this(h, log, tag, family, bindGroups, DEFAULT_SOCKET_RECV_BUFSIZE);
    }

    @Override
    protected FileDescriptor createFd() {
        FileDescriptor fd = null;

        try {
            fd = Os.socket(AF_NETLINK, SOCK_DGRAM | SOCK_NONBLOCK, mFamily);
            if (mSockRcvbufSize != DEFAULT_SOCKET_RECV_BUFSIZE) {
                try {
                    Os.setsockoptInt(fd, SOL_SOCKET, SO_RCVBUF, mSockRcvbufSize);
                } catch (ErrnoException e) {
                    Log.wtf(mTag, "Failed to set SO_RCVBUF to " + mSockRcvbufSize, e);
                }
            }
            Os.bind(fd, makeNetlinkSocketAddress(0, mBindGroups));
            NetlinkUtils.connectSocketToNetlink(fd);

            if (DBG) {
                final SocketAddress nlAddr = Os.getsockname(fd);
                Log.d(mTag, "bound to sockaddr_nl{" + nlAddr.toString() + "}");
            }
        } catch (ErrnoException | SocketException e) {
            logError("Failed to create rtnetlink socket", e);
            closeSocketQuietly(fd);
            return null;
        }

        return fd;
    }

    @Override
    protected void handlePacket(byte[] recvbuf, int length) {
        final long whenMs = SystemClock.elapsedRealtime();
        final ByteBuffer byteBuffer = ByteBuffer.wrap(recvbuf, 0, length);
        byteBuffer.order(ByteOrder.nativeOrder());

        while (byteBuffer.remaining() > 0) {
            try {
                final int position = byteBuffer.position();
                final NetlinkMessage nlMsg = NetlinkMessage.parse(byteBuffer, mFamily);
                if (nlMsg == null || nlMsg.getHeader() == null) {
                    byteBuffer.position(position);
                    mLog.e("unparsable netlink msg: " + hexify(byteBuffer));
                    break;
                }

                if (nlMsg instanceof NetlinkErrorMessage) {
                    mLog.e("netlink error: " + nlMsg);
                    continue;
                }

                processNetlinkMessage(nlMsg, whenMs);
            } catch (Exception e) {
                mLog.e("Error handling netlink message", e);
            }
        }
    }

    @Override
    protected void logError(String msg, Exception e) {
        mLog.e(msg, e);
    }

    // Ignoring ENOBUFS may miss any important netlink messages, there are some messages which
    // cannot be recovered by dumping current state once missed since kernel doesn't keep state
    // for it. In addition, dumping current state will not result in any RTM_DELxxx messages, so
    // reconstructing current state from a dump will be difficult. However, for those netlink
    // messages don't cause any state changes, e.g. RTM_NEWLINK with current link state, maybe
    // it's okay to ignore them, because these netlink messages won't cause any changes on the
    // LinkProperties. Given the above trade-offs, try to ignore ENOBUFS and that's similar to
    // what netd does today.
    //
    // TODO: log metrics when ENOBUFS occurs, or even force a disconnect, it will help see how
    // often this error occurs on fields with the associated socket receive buffer size.
    @Override
    protected boolean handleReadError(ErrnoException e) {
        logError("readPacket error: ", e);
        if (e.errno == ENOBUFS) {
            Log.wtf(mTag, "Errno: ENOBUFS");
            return false;
        }
        return true;
    }

    /**
     * Processes one netlink message. Must be overridden by subclasses.
     * @param nlMsg the message to process.
     * @param whenMs the timestamp, as measured by {@link SystemClock#elapsedRealtime}, when the
     *               message was received.
     */
    protected void processNetlinkMessage(NetlinkMessage nlMsg, long whenMs) { }
}
