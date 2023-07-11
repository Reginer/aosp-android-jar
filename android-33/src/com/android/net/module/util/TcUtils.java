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

import java.io.IOException;

/**
 * Contains mostly tc-related functionality.
 */
public class TcUtils {
    static {
        System.loadLibrary(JniUtil.getJniLibraryName(TcUtils.class.getPackage()));
    }

    /**
     * Checks if the network interface uses an ethernet L2 header.
     *
     * @param iface the network interface.
     * @return true if the interface uses an ethernet L2 header.
     * @throws IOException
     */
    public static native boolean isEthernet(String iface) throws IOException;

    /**
     * Attach a tc bpf filter.
     *
     * Equivalent to the following 'tc' command:
     * tc filter add dev .. in/egress prio .. protocol ipv6/ip bpf object-pinned
     * /sys/fs/bpf/... direct-action
     *
     * @param ifIndex the network interface index.
     * @param ingress ingress or egress qdisc.
     * @param prio
     * @param proto
     * @param bpfProgPath
     * @throws IOException
     */
    public static native void tcFilterAddDevBpf(int ifIndex, boolean ingress, short prio,
            short proto, String bpfProgPath) throws IOException;

    /**
     * Attach a tc police action.
     *
     * Attaches a matchall filter to the clsact qdisc with a tc police and tc bpf action attached.
     * This causes the ingress rate to be limited and exceeding packets to be forwarded to a bpf
     * program (specified in bpfProgPah) that accounts for the packets before dropping them.
     *
     * Equivalent to the following 'tc' command:
     * tc filter add dev .. ingress prio .. protocol .. matchall \
     *     action police rate .. burst .. conform-exceed pipe/continue \
     *     action bpf object-pinned .. \
     *     drop
     *
     * @param ifIndex the network interface index.
     * @param prio the filter preference.
     * @param proto protocol.
     * @param rateInBytesPerSec rate limit in bytes/s.
     * @param bpfProgPath bpg program that accounts for rate exceeding packets before they are
     *                    dropped.
     * @throws IOException
     */
    public static native void tcFilterAddDevIngressPolice(int ifIndex, short prio, short proto,
            int rateInBytesPerSec, String bpfProgPath) throws IOException;

    /**
     * Delete a tc filter.
     *
     * Equivalent to the following 'tc' command:
     * tc filter del dev .. in/egress prio .. protocol ..
     *
     * @param ifIndex the network interface index.
     * @param ingress ingress or egress qdisc.
     * @param prio the filter preference.
     * @param proto protocol.
     * @throws IOException
     */
    public static native void tcFilterDelDev(int ifIndex, boolean ingress, short prio,
            short proto) throws IOException;

    /**
     * Add a clsact qdisc.
     *
     * Equivalent to the following 'tc' command:
     * tc qdisc add dev .. clsact
     *
     * @param ifIndex the network interface index.
     * @throws IOException
     */
    public static native void tcQdiscAddDevClsact(int ifIndex) throws IOException;
}
