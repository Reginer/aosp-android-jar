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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ICMPv6 source/target link-layer address option, as per https://tools.ietf.org/html/rfc4861.
 *
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     Type      |    Length     |    Link-Layer Address ...
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class LlaOption extends Struct {
    @Field(order = 0, type = Type.S8)
    public final byte type;
    @Field(order = 1, type = Type.S8)
    public final byte length; // Length in 8-byte units
    @Field(order = 2, type = Type.EUI48)
    // Link layer address length and format varies on different link layers, which is not
    // guaranteed to be a 6-byte MAC address. However, Struct only supports 6-byte MAC
    // addresses type(EUI-48) for now.
    public final MacAddress linkLayerAddress;

    LlaOption(final byte type, final byte length, final MacAddress linkLayerAddress) {
        this.type = type;
        this.length = length;
        this.linkLayerAddress = linkLayerAddress;
    }

    /**
     * Build a target link-layer address option from the required specified parameters.
     */
    public static ByteBuffer build(final byte type, final MacAddress linkLayerAddress) {
        final LlaOption option = new LlaOption(type, (byte) 1 /* option len */, linkLayerAddress);
        return ByteBuffer.wrap(option.writeToBytes(ByteOrder.BIG_ENDIAN));
    }
}
