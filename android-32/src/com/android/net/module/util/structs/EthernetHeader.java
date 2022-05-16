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

package com.android.net.module.util.structs;

import android.net.MacAddress;

import com.android.net.module.util.Struct;
import com.android.net.module.util.Struct.Field;
import com.android.net.module.util.Struct.Type;

/**
 * L2 ethernet header as per IEEE 802.3. Does not include a 802.1Q tag.
 *
 * 0                   1
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |          Destination          |
 * +-                             -+
 * |            Ethernet           |
 * +-                             -+
 * |            Address            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |             Source            |
 * +-                             -+
 * |            Ethernet           |
 * +-                             -+
 * |            Address            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |            EtherType          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class EthernetHeader extends Struct {
    @Field(order = 0, type = Type.EUI48)
    public final MacAddress dstMac;
    @Field(order = 1, type = Type.EUI48)
    public final MacAddress srcMac;
    @Field(order = 2, type = Type.U16)
    public final int etherType;

    public EthernetHeader(final MacAddress dstMac, final MacAddress srcMac,
            final int etherType) {
        this.dstMac = dstMac;
        this.srcMac = srcMac;
        this.etherType = etherType;
    }
}
