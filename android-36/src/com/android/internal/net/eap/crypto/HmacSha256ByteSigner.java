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

import com.android.internal.net.crypto.KeyGenerationUtils;
import com.android.internal.net.crypto.KeyGenerationUtils.ByteSigner;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HmacSha256ByteSigner is a {@link ByteSigner} to be used for computing HMAC-SHA-256 values for
 * specific keys and data.
 */
public class HmacSha256ByteSigner implements KeyGenerationUtils.ByteSigner {
    private static final String TAG = HmacSha256ByteSigner.class.getSimpleName();
    private static final String MAC_ALGORITHM_STRING = "HmacSHA256";
    private static final HmacSha256ByteSigner sInstance = new HmacSha256ByteSigner();

    /**
     * Gets instance of HmacSha256ByteSigner.
     *
     * @return HmacSha256ByteSigner instance.
     */
    public static HmacSha256ByteSigner getInstance() {
        return sInstance;
    }

    @Override
    public byte[] signBytes(byte[] keyBytes, byte[] dataToSign) {
        try {
            Mac mac = Mac.getInstance(MAC_ALGORITHM_STRING);
            mac.init(new SecretKeySpec(keyBytes, MAC_ALGORITHM_STRING));
            return mac.doFinal(dataToSign);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
