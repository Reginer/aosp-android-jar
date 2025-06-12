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

package com.android.internal.net.ipsec.ike.keepalive;

import static android.net.ipsec.ike.IkeManager.getIkeLog;

import android.content.Context;
import android.net.IpSecManager.UdpEncapsulationSocket;
import android.system.ErrnoException;
import android.system.Os;

import com.android.internal.net.ipsec.ike.IkeSocket;
import com.android.internal.net.ipsec.ike.utils.IkeAlarm;
import com.android.internal.net.ipsec.ike.utils.IkeAlarm.IkeAlarmConfig;

import java.net.Inet4Address;
import java.net.SocketException;
import java.nio.ByteBuffer;

/** This class provides methods to schedule and send keepalive packet. */
public final class SoftwareKeepaliveImpl implements IkeNattKeepalive.NattKeepalive {
    private static final String TAG = "SoftwareKeepaliveImpl";

    // NAT-Keepalive packet payload as per RFC 3948
    private static final byte[] NATT_KEEPALIVE_PAYLOAD = new byte[] {(byte) 0xff};

    private final UdpEncapsulationSocket mSocket;
    private final Inet4Address mDestAddress;
    private final IkeAlarm mIkeAlarm;

    /**
     * Construct an instance of SoftwareKeepaliveImpl
     *
     * <p>Caller that provides keepAliveAlarmIntent is responsible for handling the alarm.
     */
    public SoftwareKeepaliveImpl(
            Context context,
            Inet4Address dest,
            UdpEncapsulationSocket socket,
            IkeAlarmConfig alarmConfig) {
        mSocket = socket;
        mDestAddress = dest;

        // It is time-critical to send packets periodically to keep the dynamic NAT mapping
        // alive. Thus, the alarm has to be "setExact" to avoid batching delay (can be at most 75%)
        // and allowed to goes off when the device is in doze mode. There will still be a rate limit
        // on firing alarms. Please check AlarmManager#setExactAndAllowWhileIdle for more details.
        mIkeAlarm = IkeAlarm.newExactAndAllowWhileIdleAlarm(alarmConfig);
    }

    @Override
    public void start() {
        sendKeepaliveAndScheduleNext();
    }

    @Override
    public void stop() {
        mIkeAlarm.cancel();
    }

    @Override
    public void onAlarmFired() {
        sendKeepaliveAndScheduleNext();
    }

    /** Send out keepalive packet and schedule next keepalive event */
    private void sendKeepaliveAndScheduleNext() {
        getIkeLog().d(TAG, "Send keepalive to " + mDestAddress.getHostAddress());
        try {
            Os.sendto(
                    mSocket.getFileDescriptor(),
                    ByteBuffer.wrap(NATT_KEEPALIVE_PAYLOAD),
                    0,
                    mDestAddress,
                    IkeSocket.SERVER_PORT_UDP_ENCAPSULATED);

        } catch (ErrnoException | SocketException e) {
            getIkeLog().i(TAG, "Failed to keepalive packet to " + mDestAddress.getHostAddress(), e);
        }

        mIkeAlarm.schedule();
    }
}
