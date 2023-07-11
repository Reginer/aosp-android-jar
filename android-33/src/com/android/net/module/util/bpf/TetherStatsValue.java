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

package com.android.net.module.util.bpf;

import com.android.net.module.util.Struct;
import com.android.net.module.util.Struct.Field;
import com.android.net.module.util.Struct.Type;

/** The key of BpfMap which is used for tethering stats. */
public class TetherStatsValue extends Struct {
    // Use the signed long variable to store the uint64 stats from stats BPF map.
    // U63 is enough for each data element even at 5Gbps for ~468 years.
    // 2^63 / (5 * 1000 * 1000 * 1000) * 8 / 86400 / 365 = 468.
    @Field(order = 0, type = Type.U63)
    public final long rxPackets;
    @Field(order = 1, type = Type.U63)
    public final long rxBytes;
    @Field(order = 2, type = Type.U63)
    public final long rxErrors;
    @Field(order = 3, type = Type.U63)
    public final long txPackets;
    @Field(order = 4, type = Type.U63)
    public final long txBytes;
    @Field(order = 5, type = Type.U63)
    public final long txErrors;

    public TetherStatsValue(final long rxPackets, final long rxBytes, final long rxErrors,
            final long txPackets, final long txBytes, final long txErrors) {
        this.rxPackets = rxPackets;
        this.rxBytes = rxBytes;
        this.rxErrors = rxErrors;
        this.txPackets = txPackets;
        this.txBytes = txBytes;
        this.txErrors = txErrors;
    }

    // TODO: remove equals, hashCode and toString once aosp/1536721 is merged.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof TetherStatsValue)) return false;

        final TetherStatsValue that = (TetherStatsValue) obj;

        return rxPackets == that.rxPackets
                && rxBytes == that.rxBytes
                && rxErrors == that.rxErrors
                && txPackets == that.txPackets
                && txBytes == that.txBytes
                && txErrors == that.txErrors;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(rxPackets) ^ Long.hashCode(rxBytes) ^ Long.hashCode(rxErrors)
                ^ Long.hashCode(txPackets) ^ Long.hashCode(txBytes) ^ Long.hashCode(txErrors);
    }

    @Override
    public String toString() {
        return String.format("rxPackets: %s, rxBytes: %s, rxErrors: %s, txPackets: %s, "
                + "txBytes: %s, txErrors: %s", rxPackets, rxBytes, rxErrors, txPackets,
                txBytes, txErrors);
    }
}
