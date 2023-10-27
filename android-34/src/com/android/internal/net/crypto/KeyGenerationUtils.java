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

package com.android.internal.net.crypto;

import java.nio.ByteBuffer;

/**
 * KeyGenerationUtils is a util class that contains utils for key generation needed by IKEv2 and
 * EAP.
 */
public class KeyGenerationUtils {
    /**
     * Returns the derived pseudorandom number with the specified length by iteratively applying a
     * PRF.
     *
     * <p>prf+(K, S) outputs a pseudorandom stream by using the PRF iteratively. In this way it can
     * generate long enough keying material containing all the keys.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7296#section-2.13">RFC 7296 Internet Key
     *     Exchange Protocol Version 2 (IKEv2) 2.13. Generating Keying Material </a>
     * @param byteSigner the PRF used to sign the given data using the given key.
     * @param keyBytes the key to sign data.
     * @param dataToSign the data to be signed.
     * @param keyMaterialLen the length of keying materials to be generated.
     * @return the byte array of keying materials
     */
    public static byte[] prfPlus(
            ByteSigner byteSigner, byte[] keyBytes, byte[] dataToSign, int keyMaterialLen) {
        ByteBuffer keyMatBuffer = ByteBuffer.allocate(keyMaterialLen);

        byte[] previousMac = new byte[0];
        final int padLen = 1;
        byte padValue = 1;

        while (keyMatBuffer.remaining() > 0) {
            ByteBuffer dataToSignBuffer =
                    ByteBuffer.allocate(previousMac.length + dataToSign.length + padLen);
            dataToSignBuffer.put(previousMac).put(dataToSign).put(padValue);

            previousMac = byteSigner.signBytes(keyBytes, dataToSignBuffer.array());

            keyMatBuffer.put(
                    previousMac, 0, Math.min(previousMac.length, keyMatBuffer.remaining()));

            padValue++;
        }

        return keyMatBuffer.array();
    }

    /**
     * ByteSigner is an interface to be used for implementing the byte-signing for generating keys
     * using {@link KeyGenerationUtils#prfPlus(ByteSigner, byte[], byte[], int)}.
     */
    public interface ByteSigner {
        /**
         * Signs the given data using the key given.
         *
         * <p>Caller is responsible for providing a valid key according to their use cases.
         *
         * @param keyBytes the key to sign data.
         * @param dataToSign the data to be signed.
         * @return the signed value.
         */
        byte[] signBytes(byte[] keyBytes, byte[] dataToSign);
    }
}
