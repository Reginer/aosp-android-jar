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

package com.android.net.module.util.ip;

import static android.system.OsConstants.NETLINK_ROUTE;

import static com.android.net.module.util.netlink.NetlinkConstants.RTM_DELNEIGH;
import static com.android.net.module.util.netlink.NetlinkConstants.hexify;
import static com.android.net.module.util.netlink.NetlinkConstants.stringForNlMsgType;

import android.net.MacAddress;
import android.os.Handler;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Log;

import com.android.net.module.util.SharedLog;
import com.android.net.module.util.netlink.NetlinkMessage;
import com.android.net.module.util.netlink.NetlinkSocket;
import com.android.net.module.util.netlink.RtNetlinkNeighborMessage;
import com.android.net.module.util.netlink.StructNdMsg;

import java.net.InetAddress;
import java.util.StringJoiner;

/**
 * IpNeighborMonitor.
 *
 * Monitors the kernel rtnetlink neighbor notifications and presents to callers
 * NeighborEvents describing each event. Callers can provide a consumer instance
 * to both filter (e.g. by interface index and IP address) and handle the
 * generated NeighborEvents.
 *
 * @hide
 */
public class IpNeighborMonitor extends NetlinkMonitor {
    private static final String TAG = IpNeighborMonitor.class.getSimpleName();
    private static final boolean DBG = false;
    private static final boolean VDBG = false;

    /**
     * Make the kernel perform neighbor reachability detection (IPv4 ARP or IPv6 ND)
     * for the given IP address on the specified interface index.
     *
     * @return 0 if the request was successfully passed to the kernel; otherwise return
     *         a non-zero error code.
     */
    public static int startKernelNeighborProbe(int ifIndex, InetAddress ip) {
        final String msgSnippet = "probing ip=" + ip.getHostAddress() + "%" + ifIndex;
        if (DBG) Log.d(TAG, msgSnippet);

        final byte[] msg = RtNetlinkNeighborMessage.newNewNeighborMessage(
                1, ip, StructNdMsg.NUD_PROBE, ifIndex, null);

        try {
            NetlinkSocket.sendOneShotKernelMessage(NETLINK_ROUTE, msg);
        } catch (ErrnoException e) {
            Log.e(TAG, "Error " + msgSnippet + ": " + e);
            return -e.errno;
        }

        return 0;
    }

    /**
     * An event about a neighbor.
     */
    public static class NeighborEvent {
        final long elapsedMs;
        final short msgType;
        final int ifindex;
        final InetAddress ip;
        final short nudState;
        final MacAddress macAddr;

        public NeighborEvent(long elapsedMs, short msgType, int ifindex, InetAddress ip,
                short nudState, MacAddress macAddr) {
            this.elapsedMs = elapsedMs;
            this.msgType = msgType;
            this.ifindex = ifindex;
            this.ip = ip;
            this.nudState = nudState;
            this.macAddr = macAddr;
        }

        boolean isConnected() {
            return (msgType != RTM_DELNEIGH) && StructNdMsg.isNudStateConnected(nudState);
        }

        boolean isValid() {
            return (msgType != RTM_DELNEIGH) && StructNdMsg.isNudStateValid(nudState);
        }

        @Override
        public String toString() {
            final StringJoiner j = new StringJoiner(",", "NeighborEvent{", "}");
            return j.add("@" + elapsedMs)
                    .add(stringForNlMsgType(msgType, NETLINK_ROUTE))
                    .add("if=" + ifindex)
                    .add(ip.getHostAddress())
                    .add(StructNdMsg.stringForNudState(nudState))
                    .add("[" + macAddr + "]")
                    .toString();
        }
    }

    /**
     * A client that consumes NeighborEvent instances.
     * Implement this to listen to neighbor events.
     */
    public interface NeighborEventConsumer {
        // Every neighbor event received on the netlink socket is passed in
        // here. Subclasses should filter for events of interest.
        /**
         * Consume a neighbor event
         * @param event the event
         */
        void accept(NeighborEvent event);
    }

    private final NeighborEventConsumer mConsumer;

    public IpNeighborMonitor(Handler h, SharedLog log, NeighborEventConsumer cb) {
        super(h, log, TAG, NETLINK_ROUTE, OsConstants.RTMGRP_NEIGH);
        mConsumer = (cb != null) ? cb : (event) -> { /* discard */ };
    }

    @Override
    public void processNetlinkMessage(NetlinkMessage nlMsg, final long whenMs) {
        if (!(nlMsg instanceof RtNetlinkNeighborMessage)) {
            mLog.e("non-rtnetlink neighbor msg: " + nlMsg);
            return;
        }

        final RtNetlinkNeighborMessage neighMsg = (RtNetlinkNeighborMessage) nlMsg;
        final short msgType = neighMsg.getHeader().nlmsg_type;
        final StructNdMsg ndMsg = neighMsg.getNdHeader();
        if (ndMsg == null) {
            mLog.e("RtNetlinkNeighborMessage without ND message header!");
            return;
        }

        final int ifindex = ndMsg.ndm_ifindex;
        final InetAddress destination = neighMsg.getDestination();
        final short nudState =
                (msgType == RTM_DELNEIGH)
                ? StructNdMsg.NUD_NONE
                : ndMsg.ndm_state;

        final NeighborEvent event = new NeighborEvent(
                whenMs, msgType, ifindex, destination, nudState,
                getMacAddress(neighMsg.getLinkLayerAddress()));

        if (VDBG) {
            Log.d(TAG, neighMsg.toString());
        }
        if (DBG) {
            Log.d(TAG, event.toString());
        }

        mConsumer.accept(event);
    }

    private static MacAddress getMacAddress(byte[] linkLayerAddress) {
        if (linkLayerAddress != null) {
            try {
                return MacAddress.fromBytes(linkLayerAddress);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Failed to parse link-layer address: " + hexify(linkLayerAddress));
            }
        }

        return null;
    }
}
