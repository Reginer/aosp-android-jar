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
package com.android.internal.net.ipsec.ike.ike3gpp;

import android.net.ipsec.ike.exceptions.InvalidSyntaxException;

import com.android.internal.net.ipsec.ike.message.IkeNotifyPayload;

import java.nio.ByteBuffer;

/**
 * Ike3gppN1ModeUtils contains functions needed to support 3GPP-specific N1 Mode functionality.
 *
 * <p>This class is package-private.
 */
class Ike3gppN1ModeUtils {
    private static final int N1_MODE_CAPABILITY_PAYLOAD_LENGTH = 2;
    private static final byte PDU_SESSION_ID_LEN = (byte) 1;

    /**
     * Generate N1 Mode Capability Notify payload.
     *
     * <p>This method formats the given PDU Session ID to its payload format.
     *
     * @see TS 124 302 Section 8.2.9.15 for specification of N1_MODE_CAPABILITY Notify payload.
     * @param pduSessionId the PDU Session ID to be included in this N1 Mode Capability payload
     * @return the formatted N1_MODE_CAPABILITY Notify payload data as a byte array.
     */
    static IkeNotifyPayload generateN1ModeCapabilityPayload(byte pduSessionId) {
        ByteBuffer payloadData = ByteBuffer.allocate(N1_MODE_CAPABILITY_PAYLOAD_LENGTH);
        payloadData.put(PDU_SESSION_ID_LEN);
        payloadData.put(pduSessionId);

        return new IkeNotifyPayload(
                Ike3gppExtensionExchange.NOTIFY_TYPE_N1_MODE_CAPABILITY, payloadData.array());
    }

    /**
     * Get the S-NSSAI value from the specified Notify Data.
     *
     * @see TS 124 302 Section 8.2.9.16 for specification of N1_MODE_INFORMATION Notify payload.
     * @param notifyData The Notify-Data payload from which the S-NSSAI value will be parsed.
     * @return the parsed S-NSSAI value
     */
    static byte[] getSnssaiFromNotifyData(byte[] notifyData) throws InvalidSyntaxException {
        ByteBuffer buffer = ByteBuffer.wrap(notifyData);

        // Format is: | SNSSAI Length (1B) | SNSSAI (Length B) |
        int snssaiLen = Byte.toUnsignedInt(buffer.get());
        if (snssaiLen != notifyData.length - 1) {
            throw new InvalidSyntaxException("SNSSAI does not match expected length");
        }

        byte[] snssai = new byte[snssaiLen];
        buffer.get(snssai);
        return snssai;
    }
}
