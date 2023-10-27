/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.internal.net.eap.crypto;

import com.android.internal.annotations.VisibleForTesting;

/**
 * This class sets parity-bits for a given byte-array as specified by the MSCHAPv2 standard.
 *
 * @see <a href="https://tools.ietf.org/html/rfc2759">RFC 2759, Microsoft PPP CHAP Extensions,
 *     Version 2 (MSCHAPv2)</a>
 */
public class ParityBitUtil {
    private static final int INPUT_LENGTH = 7;
    private static final int OUTPUT_LENGTH = 8;
    private static final int BITS_PER_BYTE = 8;
    private static final int BITS_PER_PARITY_BIT = 7;
    private static final byte MASK = (byte) 0xFE;

    /**
     * Computes and returns a byte[] with the odd-parity bits set.
     *
     * <p>Parity bits are set for the 8th, 16th, 24th, etc. bits as the LSB for each byte.
     *
     * @param input byte[] the input data requiring parity bits
     * @return byte[] the input byte[] with its parity-bits set
     * @throws IllegalArgumentException iff the given data array does not contain 7 bytes
     */
    public static byte[] addParityBits(byte[] input) {
        if (input.length != INPUT_LENGTH) {
            throw new IllegalArgumentException("Data must be 7B long");
        }
        byte[] output = new byte[OUTPUT_LENGTH];

        long allBits = byteArrayToLong(input) << 1; // make room for parity bit

        // allBits stores input bits as [b56, ..., b1, 0]. Consuming allBits in reverse populates
        // output with a right shift
        for (int i = output.length - 1; i >= 0; i--) {
            output[i] = getByteWithParityBit((byte) allBits);
            allBits >>= BITS_PER_PARITY_BIT;
        }

        return output;
    }

    @VisibleForTesting
    static byte getByteWithParityBit(byte b) {
        // Parity bits per RFC 2759#8.6
        byte parity =
                (byte) ((b >> 7) ^ (b >> 6) ^ (b >> 5) ^ (b >> 4) ^ (b >> 3) ^ (b >> 2) ^ (b >> 1));

        // If we have an odd number of bits, b1 should be 0.
        // If we have an even number of bits, b1 should be 1.
        byte parityBit = (byte) (~parity & 1);
        return (byte) ((b & MASK) | parityBit);
    }

    @VisibleForTesting
    static long byteArrayToLong(byte[] input) {
        long result = 0;
        for (int i = 0; i < input.length; i++) {
            result = (result << BITS_PER_BYTE) | Byte.toUnsignedInt(input[i]);
        }
        return result;
    }
}
