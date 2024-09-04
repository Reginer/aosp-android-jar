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

import static com.android.internal.net.utils.BigIntegerUtils.bigIntegerToUnsignedByteArray;
import static com.android.internal.net.utils.BigIntegerUtils.unsignedByteArrayToBigInteger;

import org.bouncycastle.crypto.digests.SHA1Digest;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * This class implements the Pseudo-Random Number Generator as specified by FIPS 186-2, change
 * notice 1, as required by EAP-SIM (RFC 4186) and EAP-AKA (RFC 4187).
 *
 * <p>Simplifying constraints allow for some parameters to be permanently fixed: The "b" parameter
 * is specified to always be 160 by RFC 4186. Likewise, the seed must be 20 bytes and therefore can
 * never exceed 2^b.
 */
public class Fips186_2Prf {
    private static final int SEED_LEN_BYTES = 20;
    private static final int SHA_OUTPUT_LEN_BYTES = 40;

    /**
     * Gets the next random based on the PRF described in FIPS 186-2, Change Notice 1.
     *
     * @param seed the seed value
     * @param outputLenBytes the output byte count required. Will run multiple iterations as needed.
     * @return the byte-array random result
     */
    public byte[] getRandom(byte[] seed, int outputLenBytes) {
        if (seed.length != SEED_LEN_BYTES) {
            throw new IllegalArgumentException("Invalid seed length. Must be 160b (20B)");
        }

        BigInteger xkey = unsignedByteArrayToBigInteger(seed);
        BigInteger exp_b = new BigInteger("2").pow(SEED_LEN_BYTES * 8);

        ByteBuffer buffer = ByteBuffer.allocate(outputLenBytes);

        // RFC 4186, Appendix B, Step 3:
        //     M is the number of generation runs, based on the desired output bytes (rounded up)
        //     EAP SIM/AKA require at least 16 (K_ENCR) + 16 (K_AUTH) + 64 (MSK) + 64 (EMSK) bytes
        //     (total 160 Bytes)
        int numIterations = (outputLenBytes + SHA_OUTPUT_LEN_BYTES - 1) / SHA_OUTPUT_LEN_BYTES;
        for (int j = 0; j < numIterations; j++) {
            for (int i = 0; i < 2; i++) {
                // RFC 4186, Appendix B, Step 3.1 XSEED_j = 0
                //     (XSEED_j unused, omitted)

                // RFC 4186, Appendix B, Step 3.2a: XVAL = (XKEY + XSEED_j) mod 2^b
                //     (XSEED_j unused, omitted)
                BigInteger xval = xkey.mod(exp_b);

                // RFC 4186, Appendix B, Step 3.2b:
                //     w_i = G(t, XVAL)
                byte[] w_i = new byte[SEED_LEN_BYTES];
                Sha1_186_2_FunctionG digest = new Sha1_186_2_FunctionG();
                digest.update(
                        bigIntegerToUnsignedByteArray(xval, SEED_LEN_BYTES), 0, SEED_LEN_BYTES);
                digest.doFinal(w_i, 0);

                // RFC 4186, Appendix B, Step 3.2c:
                //     XKEY = (1 + XKEY + w_i) mod 2^b
                xkey = xkey.add(BigInteger.ONE).add(unsignedByteArrayToBigInteger(w_i));
                xkey = xkey.mod(exp_b);

                // RFC 4186, Appendix B, Step 3.3:
                //     x_j = w_0|w_1
                buffer.put(w_i, 0, Math.min(buffer.remaining(), w_i.length));
            }
        }

        return buffer.array();
    }

    /**
     * This inner class represents the modifications to the SHA1 digest required for FIPS 186-2 PRFs
     *
     * <p>FIPS 186-2 requires the use of a hashing function with exactly the same transforms as
     * SHA1, but with a slightly different padding (all 0s instead of appending additional metadata
     * for entropy).
     *
     * <p>Specifically, this function extends BouncyCastle's SHA1Digest in order to override the
     * finish method, preventing the additional padding bytes from being appended (leaving them all
     * 0).
     */
    private static class Sha1_186_2_FunctionG extends SHA1Digest {
        // IV (t) specified by SHA-1; Use default.

        @Override
        public void finish() {
            // Don't do any other processing. We only care about the processBlock() functionality.
            processBlock();
        }
    }
}
