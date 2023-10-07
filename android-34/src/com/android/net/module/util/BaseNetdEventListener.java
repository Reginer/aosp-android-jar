/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.net.module.util;

import android.net.metrics.INetdEventListener;

/**
 * Base {@link INetdEventListener} that provides no-op implementations which can
 * be overridden.
 */
public class BaseNetdEventListener extends INetdEventListener.Stub {

    @Override
    public void onDnsEvent(int netId, int eventType, int returnCode,
            int latencyMs, String hostname, String[] ipAddresses,
            int ipAddressesCount, int uid) { }

    @Override
    public void onPrivateDnsValidationEvent(int netId, String ipAddress,
            String hostname, boolean validated) { }

    @Override
    public void onConnectEvent(int netId, int error, int latencyMs,
            String ipAddr, int port, int uid) { }

    @Override
    public void onWakeupEvent(String prefix, int uid, int ethertype,
            int ipNextHeader, byte[] dstHw, String srcIp, String dstIp,
            int srcPort, int dstPort, long timestampNs) { }

    @Override
    public void onTcpSocketStatsEvent(int[] networkIds, int[] sentPackets,
            int[] lostPackets, int[] rttUs, int[] sentAckDiffMs) { }

    @Override
    public void onNat64PrefixEvent(int netId, boolean added,
            String prefixString, int prefixLength) { }

    @Override
    public int getInterfaceVersion() {
        return INetdEventListener.VERSION;
    }

    @Override
    public String getInterfaceHash() {
        return INetdEventListener.HASH;
    }
}
