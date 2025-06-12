/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.internal.net.utils;

import java.math.BigInteger;

/** BigIntegerUtils provides utility methods for doing Key Exchange. */
public final class BigIntegerUtils {

    /**
     * Converts the unsigned Hex String to a positive BigInteger.
     *
     * @param hexString Hex representation of an unsigned value
     * @return the argument converted to BigInteger by an unsigned conversion
     */
    public static BigInteger unsignedHexStringToBigInteger(String hexString) {
        return new BigInteger(hexString, 16);
    }

    /**
     * Converts the unsigned byte array to a positive BigInteger.
     *
     * @param byteArray byte array that represents an unsigned value
     * @return the argument converted to BigInteger by an unsigned conversion
     */
    public static BigInteger unsignedByteArrayToBigInteger(byte[] byteArray) {
        return new BigInteger(1/** positive */, byteArray);
    }

    /**
     * Returns a byte array containing the unsigned representation of this BigInteger. Zero-pad on
     * the left of byte array to desired size.
     *
     * @param bigInteger input BigInteger
     * @param size size of the output byte array
     * @return the byte array containing the unsigned representation of this BigInteger.
     */
    public static byte[] bigIntegerToUnsignedByteArray(BigInteger bigInteger, int size) {
        byte[] byteArrayWithSignBit = bigInteger.toByteArray();
        int len = byteArrayWithSignBit.length;
        byte[] output = new byte[size];

        // {@link BigInteger} provides method {@link toByteArray} that returns a byte array
        // containing the two's-complement representation of this BigInteger (minimum number of
        // bytes required to represent this BigInteger, including at least one sign bit). This
        // method first remove additional byte that caused by this sign bit and zero-pad on the left
        // of byte array to desired size.
        if (bigInteger.bitLength() % 8 == 0) {
            len = len - 1;
            System.arraycopy(byteArrayWithSignBit, 1, output, size - len, len);
        } else {
            System.arraycopy(byteArrayWithSignBit, 0, output, size - len, len);
        }

        return output;
    }
}
