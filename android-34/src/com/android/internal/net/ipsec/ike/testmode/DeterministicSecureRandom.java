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
package com.android.internal.net.ipsec.ike.testmode;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.crypto.KeyGenerationUtils;
import com.android.internal.util.HexDump;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Deterministic SecureRandom to support test mode IKE library.
 *
 * <p>Deterministic SecureRandom MUST only be used in test mode. It allows IKE library to do IKE
 * Session negotiation with pre captured responder packets as well as always generates the same set
 * of crypto keys.
 *
 * <p>This deterministic SecureRandom is deterministic in the way that if caller constructs multiple
 * instances and call the same methods from these instances, all the outputs will be the same. This
 * class is random in the way that, from the same intance, caller will get different outputs by
 * calling the same method multiple times.
 */
@VisibleForTesting
public class DeterministicSecureRandom extends SecureRandom
        implements KeyGenerationUtils.ByteSigner {
    private static final String TAG = DeterministicSecureRandom.class.getSimpleName();

    private static final String MAC_SHA256_NAME = "HmacSHA256";
    private static final String MAC_SHA256_KEY_HEX =
            "5D00F680E84F96374FC1BF8A4FC5F711467CBC62DF81A3B6169812531DF13E6C";
    private static final String INITIAL_BYTE_TO_SIGN_HEX =
            "2514A9C2B797BDDC50A1975A00866C3CC87190C29DCEBB228A4D8730AF8881BC";

    private final Mac mByteSignerMac;

    private byte[] mBytesToSign;

    @VisibleForTesting
    public DeterministicSecureRandom() {
        super(null /*secureRandomSpi*/, null /*provider*/);

        try {
            mByteSignerMac = Mac.getInstance(MAC_SHA256_NAME);
            byte[] byteSignerKey = HexDump.hexStringToByteArray(MAC_SHA256_KEY_HEX);
            mByteSignerMac.init(new SecretKeySpec(byteSignerKey, MAC_SHA256_NAME));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // Should never happen
            throw new IllegalArgumentException("Failed to construct DeterministicSecureRandom", e);
        }

        mBytesToSign = HexDump.hexStringToByteArray(INITIAL_BYTE_TO_SIGN_HEX);
    }

    @Override
    public byte[] signBytes(byte[] keyBytes, byte[] dataToSign) {
        mByteSignerMac.update(dataToSign);
        return mByteSignerMac.doFinal();
    }

    private byte[] generateBytes(int numBytes) {
        mBytesToSign =
                KeyGenerationUtils.prfPlus(
                        this, null /* keyBytes; unused */, mBytesToSign, numBytes);
        return mBytesToSign;
    }

    @Override
    public byte[] generateSeed(int numBytes) {
        return generateBytes(numBytes);
    }

    @Override
    public String getAlgorithm() {
        return TAG;
    }

    // This method serves to provide a source of random bits to all of the method inherited from
    // {@link Random} (for example, {@link nextInt}, {@link nextLong}).
    @Override
    public void nextBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(generateBytes(bytes.length));
        buffer.rewind();
        buffer.get(bytes);
    }

    @Override
    public void setSeed(byte[] seed) {
        // Do nothing
    }

    @Override
    public void setSeed(long seed) {
        // Do nothing
    }
}
