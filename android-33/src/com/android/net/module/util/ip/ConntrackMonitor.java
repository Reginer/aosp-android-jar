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

import static com.android.net.module.util.netlink.ConntrackMessage.DYING_MASK;
import static com.android.net.module.util.netlink.ConntrackMessage.ESTABLISHED_MASK;

import android.annotation.NonNull;
import android.os.Handler;
import android.system.OsConstants;

import com.android.internal.annotations.VisibleForTesting;
import com.android.net.module.util.SharedLog;
import com.android.net.module.util.netlink.ConntrackMessage;
import com.android.net.module.util.netlink.NetlinkConstants;
import com.android.net.module.util.netlink.NetlinkMessage;

import java.util.Objects;


/**
 * ConntrackMonitor.
 *
 * Monitors the netfilter conntrack notifications and presents to callers
 * ConntrackEvents describing each event.
 *
 * @hide
 */
public class ConntrackMonitor extends NetlinkMonitor {
    private static final String TAG = ConntrackMonitor.class.getSimpleName();
    private static final boolean DBG = false;
    private static final boolean VDBG = false;

    // Reference kernel/uapi/linux/netfilter/nfnetlink_compat.h
    public static final int NF_NETLINK_CONNTRACK_NEW = 1;
    public static final int NF_NETLINK_CONNTRACK_UPDATE = 2;
    public static final int NF_NETLINK_CONNTRACK_DESTROY = 4;

    // The socket receive buffer size in bytes. If too many conntrack messages are sent too
    // quickly, the conntrack messages can overflow the socket receive buffer. This can happen
    // if too many connections are disconnected by losing network and so on. Use a large-enough
    // buffer to avoid the error ENOBUFS while listening to the conntrack messages.
    private static final int SOCKET_RECV_BUFSIZE = 6 * 1024 * 1024;

    /**
     * A class for describing parsed netfilter conntrack events.
     */
    public static class ConntrackEvent {
        /**
         * Conntrack event type.
         */
        public final short msgType;
        /**
         * Original direction conntrack tuple.
         */
        public final ConntrackMessage.Tuple tupleOrig;
        /**
         * Reply direction conntrack tuple.
         */
        public final ConntrackMessage.Tuple tupleReply;
        /**
         * Connection status. A bitmask of ip_conntrack_status enum flags.
         */
        public final int status;
        /**
         * Conntrack timeout.
         */
        public final int timeoutSec;

        public ConntrackEvent(ConntrackMessage msg) {
            this.msgType = msg.getHeader().nlmsg_type;
            this.tupleOrig = msg.tupleOrig;
            this.tupleReply = msg.tupleReply;
            this.status = msg.status;
            this.timeoutSec = msg.timeoutSec;
        }

        @VisibleForTesting
        public ConntrackEvent(short msgType, ConntrackMessage.Tuple tupleOrig,
                ConntrackMessage.Tuple tupleReply, int status, int timeoutSec) {
            this.msgType = msgType;
            this.tupleOrig = tupleOrig;
            this.tupleReply = tupleReply;
            this.status = status;
            this.timeoutSec = timeoutSec;
        }

        @Override
        @VisibleForTesting
        public boolean equals(Object o) {
            if (!(o instanceof ConntrackEvent)) return false;
            ConntrackEvent that = (ConntrackEvent) o;
            return this.msgType == that.msgType
                    && Objects.equals(this.tupleOrig, that.tupleOrig)
                    && Objects.equals(this.tupleReply, that.tupleReply)
                    && this.status == that.status
                    && this.timeoutSec == that.timeoutSec;
        }

        @Override
        public int hashCode() {
            return Objects.hash(msgType, tupleOrig, tupleReply, status, timeoutSec);
        }

        @Override
        public String toString() {
            return "ConntrackEvent{"
                    + "msg_type{"
                    + NetlinkConstants.stringForNlMsgType(msgType, OsConstants.NETLINK_NETFILTER)
                    + "}, "
                    + "tuple_orig{" + tupleOrig + "}, "
                    + "tuple_reply{" + tupleReply + "}, "
                    + "status{"
                    + status + "(" + ConntrackMessage.stringForIpConntrackStatus(status) + ")"
                    + "}, "
                    + "timeout_sec{" + Integer.toUnsignedLong(timeoutSec) + "}"
                    + "}";
        }

        /**
         * Check the established NAT session conntrack message.
         *
         * @param msg the conntrack message to check.
         * @return true if an established NAT message, false if not.
         */
        public static boolean isEstablishedNatSession(@NonNull ConntrackMessage msg) {
            if (msg.getMessageType() != NetlinkConstants.IPCTNL_MSG_CT_NEW) return false;
            if (msg.tupleOrig == null) return false;
            if (msg.tupleReply == null) return false;
            if (msg.timeoutSec == 0) return false;
            if ((msg.status & ESTABLISHED_MASK) != ESTABLISHED_MASK) return false;

            return true;
        }

        /**
         * Check the dying NAT session conntrack message.
         * Note that IPCTNL_MSG_CT_DELETE event has no CTA_TIMEOUT attribute.
         *
         * @param msg the conntrack message to check.
         * @return true if a dying NAT message, false if not.
         */
        public static boolean isDyingNatSession(@NonNull ConntrackMessage msg) {
            if (msg.getMessageType() != NetlinkConstants.IPCTNL_MSG_CT_DELETE) return false;
            if (msg.tupleOrig == null) return false;
            if (msg.tupleReply == null) return false;
            if (msg.timeoutSec != 0) return false;
            if ((msg.status & DYING_MASK) != DYING_MASK) return false;

            return true;
        }
    }

    /**
     * A callback to caller for conntrack event.
     */
    public interface ConntrackEventConsumer {
        /**
         * Every conntrack event received on the netlink socket is passed in
         * here.
         */
        void accept(@NonNull ConntrackEvent event);
    }

    private final ConntrackEventConsumer mConsumer;

    public ConntrackMonitor(@NonNull Handler h, @NonNull SharedLog log,
            @NonNull ConntrackEventConsumer cb) {
        super(h, log, TAG, OsConstants.NETLINK_NETFILTER, NF_NETLINK_CONNTRACK_NEW
                | NF_NETLINK_CONNTRACK_UPDATE | NF_NETLINK_CONNTRACK_DESTROY, SOCKET_RECV_BUFSIZE);
        mConsumer = cb;
    }

    @Override
    public void processNetlinkMessage(NetlinkMessage nlMsg, final long whenMs) {
        if (!(nlMsg instanceof ConntrackMessage)) {
            mLog.e("non-conntrack msg: " + nlMsg);
            return;
        }

        final ConntrackMessage conntrackMsg = (ConntrackMessage) nlMsg;
        if (!(ConntrackEvent.isEstablishedNatSession(conntrackMsg)
                || ConntrackEvent.isDyingNatSession(conntrackMsg))) {
            return;
        }

        mConsumer.accept(new ConntrackEvent(conntrackMsg));
    }
}
