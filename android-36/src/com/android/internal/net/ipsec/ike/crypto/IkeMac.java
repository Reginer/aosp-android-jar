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

package com.android.internal.net.ipsec.ike.crypto;


import com.android.internal.net.crypto.KeyGenerationUtils.ByteSigner;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * IkeMac is an abstract class that represents common information for all negotiated algorithms that
 * generates Message Authentication Code (MAC), e.g. PRF and integrity algorithm.
 */
abstract class IkeMac extends IkeCrypto implements ByteSigner {
    private final boolean mIsJceSupported;
    private final Mac mMac;

    protected IkeMac(int algorithmId, int keyLength, String algorithmName, boolean isJceSupported) {
        super(algorithmId, keyLength, algorithmName);

        mIsJceSupported = isJceSupported;

        try {
            if (mIsJceSupported) {
                mMac = Mac.getInstance(getAlgorithmName());
            } else {
                // Won't use javax.crypto.Mac for algorithm that is not supported by JCE. Will
                // compute MAC using algorithm-specific implementation (e.g. AesXCbcImpl).
                mMac = null;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Failed to construct " + getTypeString(), e);
        }
    }

    /**
     * Signs the bytes to generate a Message Authentication Code (MAC).
     *
     * <p>Caller is responsible for providing valid key according to their use cases (e.g. PSK,
     * SK_p, SK_d ...).
     *
     * @param keyBytes the key to sign data.
     * @param dataToSign the data to be signed.
     * @return the calculated MAC.
     */
    @Override
    public byte[] signBytes(byte[] keyBytes, byte[] dataToSign) {
        try {
            if (mIsJceSupported) {
                SecretKeySpec secretKey = new SecretKeySpec(keyBytes, getAlgorithmName());
                ByteBuffer inputBuffer = ByteBuffer.wrap(dataToSign);
                mMac.init(secretKey);
                mMac.update(inputBuffer);

                return mMac.doFinal();
            } else {
                throw new IllegalStateException("Invalid algorithm: " + getAlgorithmId());
            }
        } catch (GeneralSecurityException | IllegalStateException e) {
            throw new IllegalArgumentException("Failed to generate MAC: ", e);
        }
    }
}
