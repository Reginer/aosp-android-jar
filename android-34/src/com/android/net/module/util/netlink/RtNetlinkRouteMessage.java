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

import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.AF_INET6;

import static com.android.net.module.util.NetworkStackConstants.IPV4_ADDR_ANY;
import static com.android.net.module.util.NetworkStackConstants.IPV6_ADDR_ANY;

import android.annotation.SuppressLint;
import android.net.IpPrefix;
import android.system.OsConstants;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * A NetlinkMessage subclass for rtnetlink route messages.
 *
 * RtNetlinkRouteMessage.parse() must be called with a ByteBuffer that contains exactly one
 * netlink message.
 *
 * see also:
 *
 *     include/uapi/linux/rtnetlink.h
 *
 * @hide
 */
public class RtNetlinkRouteMessage extends NetlinkMessage {
    public static final short RTA_DST           = 1;
    public static final short RTA_OIF           = 4;
    public static final short RTA_GATEWAY       = 5;

    private int mIfindex;
    @NonNull
    private StructRtMsg mRtmsg;
    @NonNull
    private IpPrefix mDestination;
    @Nullable
    private InetAddress mGateway;

    private RtNetlinkRouteMessage(StructNlMsgHdr header) {
        super(header);
        mRtmsg = null;
        mDestination = null;
        mGateway = null;
        mIfindex = 0;
    }

    public int getInterfaceIndex() {
        return mIfindex;
    }

    @NonNull
    public StructRtMsg getRtMsgHeader() {
        return mRtmsg;
    }

    @NonNull
    public IpPrefix getDestination() {
        return mDestination;
    }

    @Nullable
    public InetAddress getGateway() {
        return mGateway;
    }

    /**
     * Check whether the address families of destination and gateway match rtm_family in
     * StructRtmsg.
     *
     * For example, IPv4-mapped IPv6 addresses as an IPv6 address will be always converted to IPv4
     * address, that's incorrect when upper layer creates a new {@link RouteInfo} class instance
     * for IPv6 route with the converted IPv4 gateway.
     */
    private static boolean matchRouteAddressFamily(@NonNull final InetAddress address,
            int family) {
        return ((address instanceof Inet4Address) && (family == AF_INET))
                || ((address instanceof Inet6Address) && (family == AF_INET6));
    }

    /**
     * Parse rtnetlink route message from {@link ByteBuffer}. This method must be called with a
     * ByteBuffer that contains exactly one netlink message.
     *
     * @param header netlink message header.
     * @param byteBuffer the ByteBuffer instance that wraps the raw netlink message bytes.
     */
    @SuppressLint("NewApi")
    @Nullable
    public static RtNetlinkRouteMessage parse(@NonNull final StructNlMsgHdr header,
            @NonNull final ByteBuffer byteBuffer) {
        final RtNetlinkRouteMessage routeMsg = new RtNetlinkRouteMessage(header);

        routeMsg.mRtmsg = StructRtMsg.parse(byteBuffer);
        if (routeMsg.mRtmsg == null) return null;
        int rtmFamily = routeMsg.mRtmsg.family;

        // RTA_DST
        final int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = StructNlAttr.findNextAttrOfType(RTA_DST, byteBuffer);
        if (nlAttr != null) {
            final InetAddress destination = nlAttr.getValueAsInetAddress();
            // If the RTA_DST attribute is malformed, return null.
            if (destination == null) return null;
            // If the address family of destination doesn't match rtm_family, return null.
            if (!matchRouteAddressFamily(destination, rtmFamily)) return null;
            routeMsg.mDestination = new IpPrefix(destination, routeMsg.mRtmsg.dstLen);
        } else if (rtmFamily == AF_INET) {
            routeMsg.mDestination = new IpPrefix(IPV4_ADDR_ANY, 0);
        } else if (rtmFamily == AF_INET6) {
            routeMsg.mDestination = new IpPrefix(IPV6_ADDR_ANY, 0);
        } else {
            return null;
        }

        // RTA_GATEWAY
        byteBuffer.position(baseOffset);
        nlAttr = StructNlAttr.findNextAttrOfType(RTA_GATEWAY, byteBuffer);
        if (nlAttr != null) {
            routeMsg.mGateway = nlAttr.getValueAsInetAddress();
            // If the RTA_GATEWAY attribute is malformed, return null.
            if (routeMsg.mGateway == null) return null;
            // If the address family of gateway doesn't match rtm_family, return null.
            if (!matchRouteAddressFamily(routeMsg.mGateway, rtmFamily)) return null;
        }

        // RTA_OIF
        byteBuffer.position(baseOffset);
        nlAttr = StructNlAttr.findNextAttrOfType(RTA_OIF, byteBuffer);
        if (nlAttr != null) {
            // Any callers that deal with interface names are responsible for converting
            // the interface index to a name themselves. This may not succeed or may be
            // incorrect, because the interface might have been deleted, or even deleted
            // and re-added with a different index, since the netlink message was sent.
            routeMsg.mIfindex = nlAttr.getValueAsInt(0 /* 0 isn't a valid ifindex */);
        }

        return routeMsg;
    }

    /**
     * Write a rtnetlink address message to {@link ByteBuffer}.
     */
    @VisibleForTesting
    protected void pack(ByteBuffer byteBuffer) {
        getHeader().pack(byteBuffer);
        mRtmsg.pack(byteBuffer);

        final StructNlAttr destination = new StructNlAttr(RTA_DST, mDestination.getAddress());
        destination.pack(byteBuffer);

        if (mGateway != null) {
            final StructNlAttr gateway = new StructNlAttr(RTA_GATEWAY, mGateway.getAddress());
            gateway.pack(byteBuffer);
        }
        if (mIfindex != 0) {
            final StructNlAttr ifindex = new StructNlAttr(RTA_OIF, mIfindex);
            ifindex.pack(byteBuffer);
        }
    }

    @Override
    public String toString() {
        return "RtNetlinkRouteMessage{ "
                + "nlmsghdr{" + mHeader.toString(OsConstants.NETLINK_ROUTE) + "}, "
                + "Rtmsg{" + mRtmsg.toString() + "}, "
                + "destination{" + mDestination.getAddress().getHostAddress() + "}, "
                + "gateway{" + (mGateway == null ? "" : mGateway.getHostAddress()) + "}, "
                + "ifindex{" + mIfindex + "} "
                + "}";
    }
}
