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

import android.net.MacAddress;
import android.system.OsConstants;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.nio.ByteBuffer;

/**
 * A NetlinkMessage subclass for rtnetlink link messages.
 *
 * RtNetlinkLinkMessage.parse() must be called with a ByteBuffer that contains exactly one netlink
 * message.
 *
 * see also:
 *
 *     include/uapi/linux/rtnetlink.h
 *
 * @hide
 */
public class RtNetlinkLinkMessage extends NetlinkMessage {
    public static final short IFLA_ADDRESS   = 1;
    public static final short IFLA_IFNAME    = 3;
    public static final short IFLA_MTU       = 4;

    private int mMtu;
    @NonNull
    private StructIfinfoMsg mIfinfomsg;
    @Nullable
    private MacAddress mHardwareAddress;
    @Nullable
    private String mInterfaceName;

    private RtNetlinkLinkMessage(@NonNull StructNlMsgHdr header) {
        super(header);
        mIfinfomsg = null;
        mMtu = 0;
        mHardwareAddress = null;
        mInterfaceName = null;
    }

    public int getMtu() {
        return mMtu;
    }

    @NonNull
    public StructIfinfoMsg getIfinfoHeader() {
        return mIfinfomsg;
    }

    @Nullable
    public MacAddress getHardwareAddress() {
        return mHardwareAddress;
    }

    @Nullable
    public String getInterfaceName() {
        return mInterfaceName;
    }

    /**
     * Parse rtnetlink link message from {@link ByteBuffer}. This method must be called with a
     * ByteBuffer that contains exactly one netlink message.
     *
     * @param header netlink message header.
     * @param byteBuffer the ByteBuffer instance that wraps the raw netlink message bytes.
     */
    @Nullable
    public static RtNetlinkLinkMessage parse(@NonNull final StructNlMsgHdr header,
            @NonNull final ByteBuffer byteBuffer) {
        final RtNetlinkLinkMessage linkMsg = new RtNetlinkLinkMessage(header);

        linkMsg.mIfinfomsg = StructIfinfoMsg.parse(byteBuffer);
        if (linkMsg.mIfinfomsg == null) return null;

        // IFLA_MTU
        final int baseOffset = byteBuffer.position();
        StructNlAttr nlAttr = StructNlAttr.findNextAttrOfType(IFLA_MTU, byteBuffer);
        if (nlAttr != null) {
            linkMsg.mMtu = nlAttr.getValueAsInt(0 /* default value */);
        }

        // IFLA_ADDRESS
        byteBuffer.position(baseOffset);
        nlAttr = StructNlAttr.findNextAttrOfType(IFLA_ADDRESS, byteBuffer);
        if (nlAttr != null) {
            linkMsg.mHardwareAddress = nlAttr.getValueAsMacAddress();
        }

        // IFLA_IFNAME
        byteBuffer.position(baseOffset);
        nlAttr = StructNlAttr.findNextAttrOfType(IFLA_IFNAME, byteBuffer);
        if (nlAttr != null) {
            linkMsg.mInterfaceName = nlAttr.getValueAsString();
        }

        return linkMsg;
    }

    /**
     * Write a rtnetlink link message to {@link ByteBuffer}.
     */
    @VisibleForTesting
    protected void pack(ByteBuffer byteBuffer) {
        getHeader().pack(byteBuffer);
        mIfinfomsg.pack(byteBuffer);

        if (mMtu != 0) {
            final StructNlAttr mtu = new StructNlAttr(IFLA_MTU, mMtu);
            mtu.pack(byteBuffer);
        }
        if (mHardwareAddress != null) {
            final StructNlAttr hardwareAddress = new StructNlAttr(IFLA_ADDRESS, mHardwareAddress);
            hardwareAddress.pack(byteBuffer);
        }
        if (mInterfaceName != null) {
            final StructNlAttr ifname = new StructNlAttr(IFLA_IFNAME, mInterfaceName);
            ifname.pack(byteBuffer);
        }
    }

    @Override
    public String toString() {
        return "RtNetlinkLinkMessage{ "
                + "nlmsghdr{" + mHeader.toString(OsConstants.NETLINK_ROUTE) + "}, "
                + "Ifinfomsg{" + mIfinfomsg.toString() + "}, "
                + "Hardware Address{" + mHardwareAddress + "}, "
                + "MTU{" + mMtu + "}, "
                + "Ifname{" + mInterfaceName + "} "
                + "}";
    }
}
