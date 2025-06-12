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

package com.android.internal.net.ipsec.ike.ike3gpp;

import android.annotation.NonNull;

import com.android.internal.net.ipsec.ike.message.IkeNotifyPayload;

import java.nio.ByteBuffer;

/**
 * Ike3gppDeviceIdentityUtils contains functions needed to support 3GPP-specific DEVICE_IDENTITY
 * payload.
 */
public class Ike3gppDeviceIdentityUtils {
    private static final int DEVICE_IDENTITY_PAYLOAD_LENGTH = 11;
    // actual value to fill in length field of device identity payload
    private static final short DEVICE_IDENTITY_PAYLOAD_LENGTH_FIELD_VAL = (short) 9;
    private static final byte DEVICE_IDENTITY_TYPE_IMEI = (byte) 1;
    private static final byte DEVICE_IDENTITY_TYPE_IMEISV = (byte) 2;
    private static final int ENCODED_DEVICE_IDENTITY_LENGTH = 8;
    private static final int IMEI_LENGTH = 15;
    private static final int IMEISV_LENGTH = 16;

    /**
     * Generate DEVICE_IDENTITY Notify payload.
     *
     * <p>This method formats the given device identity string to its payload format.
     *
     * @see TS 124 302, Fig/Table 8.2.9.2 for specification of DEVICE_IDENTITY Notify payload.
     * @param deviceIdentity the device identity (IMEI/IMEISV)
     * @return the formatted DEVICE_IDENTITY Notify payload data as a byte array.
     */
    static IkeNotifyPayload generateDeviceIdentityPayload(@NonNull String deviceIdentity)
            throws IllegalArgumentException {

        if (!isValidDeviceIdentity(deviceIdentity)) {
            throw new IllegalArgumentException(
                    "device identity should be a 15 or 16 digit numeric string");
        }

        /*
         * Actual payload format inside the Notify for DEVICE_IDENTITY:
         *
         *          BITS
         * 7   6   5   4   3   2   1   0          OCTET
         * ------------------------------
         *          Length                         0-1
         *
         * ------------------------------
         *         Identity type                    2
         * ------------------------------
         *
         *
         *
         *         Identity Value                   3-10
         *
         *
         *
         * --------------------------------
         *
         *
         * Identity Value field represents the device identity digits of the corresponding
         * Identity type and is coded using BCD coding.
         * For Identity Type 'IMEI' and 'IMEISV', Identity value digits are coded based on
         * the IMEI and IMEISV structure defined in 3GPP TS 23.003 [3]. IMEI is 15 BCD
         * digits and IMEISV is 16 BCD digits. Both IMEI and IMEISV are TBCD encoded. Bits 5
         * to 8 of octet i+4 (where i represents the octet of the IMEI(SV) being encoded)
         * encodes digit 2i, bits 1 to 4 of octet i+4 encodes digit 2i-1 (i.e the order of
         * digits is swapped in each octet compared to the digit order defined in 3GPP TS
         * 23.003 [2]). Digits are packed contiguously with no internal padding. For IMEI,
         * bits 5 to 8 of the last octet shall be filled with an end mark coded as '1111'.
         */

        ByteBuffer payloadData = ByteBuffer.allocate(DEVICE_IDENTITY_PAYLOAD_LENGTH);
        payloadData.putShort(DEVICE_IDENTITY_PAYLOAD_LENGTH_FIELD_VAL);

        byte deviceIdentityType;
        deviceIdentityType =
                (deviceIdentity.length() == IMEI_LENGTH)
                        ? DEVICE_IDENTITY_TYPE_IMEI
                        : DEVICE_IDENTITY_TYPE_IMEISV;
        payloadData.put(deviceIdentityType);

        // pad IMEI
        if (deviceIdentityType == DEVICE_IDENTITY_TYPE_IMEI) {
            deviceIdentity += "0";
        }

        byte[] encodedIdentity = new byte[ENCODED_DEVICE_IDENTITY_LENGTH];
        for (int i = 0, j = 0; i + 1 < 16; i += 2, j++) {

            // nibble1 and nibble2 represent the nibbles in the final swapped BCD encoding
            byte nibble1 = (byte) Character.getNumericValue(deviceIdentity.charAt(i + 1));
            byte nibble2 = (byte) Character.getNumericValue(deviceIdentity.charAt(i));

            if (deviceIdentityType == DEVICE_IDENTITY_TYPE_IMEI && j == 7) {
                // For IMEI, bits 5 to 8 of the last octet shall be filled with
                // an end mark coded as '1111'.
                encodedIdentity[j] = (byte) ((0xF << 4) | nibble2);
            } else {
                encodedIdentity[j] = (byte) ((nibble1 << 4) | nibble2);
            }
        }

        payloadData.put(encodedIdentity);

        return new IkeNotifyPayload(
                Ike3gppExtensionExchange.NOTIFY_TYPE_DEVICE_IDENTITY, payloadData.array());
    }

    public static boolean isValidDeviceIdentity(String deviceIdentity) {
        if (deviceIdentity != null) {
            int deviceIdentityLen = deviceIdentity.length();
            if ((deviceIdentityLen == IMEI_LENGTH || deviceIdentityLen == IMEISV_LENGTH)
                    && (deviceIdentity.matches("[0-9]+"))) {
                return true;
            }
        }
        return false;
    }
}
