/*
 * Copyright 2019 The Android Open Source Project
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

package android.security.identity;

import android.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * @hide
 */
public class Util {
    private static final String TAG = "Util";

    static byte[] stripLeadingZeroes(byte[] value) {
        int n = 0;
        while (n < value.length && value[n] == 0) {
            n++;
        }
        int newLen = value.length - n;
        byte[] ret = new byte[newLen];
        int m = 0;
        while (n < value.length) {
            ret[m++] = value[n++];
        }
        return ret;
    }

    static byte[] publicKeyEncodeUncompressedForm(PublicKey publicKey) {
        ECPoint w = ((ECPublicKey) publicKey).getW();
        BigInteger x = w.getAffineX();
        BigInteger y = w.getAffineY();
        if (x.compareTo(BigInteger.ZERO) < 0) {
            throw new RuntimeException("X is negative");
        }
        if (y.compareTo(BigInteger.ZERO) < 0) {
            throw new RuntimeException("Y is negative");
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(0x04);

            // Each coordinate may be encoded in 33*, 32, or fewer bytes.
            //
            //  * : it can be 33 bytes because toByteArray() guarantees "The array will contain the
            //      minimum number of bytes required to represent this BigInteger, including at
            //      least one sign bit, which is (ceil((this.bitLength() + 1)/8))" which means that
            //      the MSB is always 0x00. This is taken care of by calling calling
            //      stripLeadingZeroes().
            //
            // We need the encoding to be exactly 32 bytes since according to RFC 5480 section 2.2
            // and SEC 1: Elliptic Curve Cryptography section 2.3.3 the encoding is 0x04 | X | Y
            // where X and Y are encoded in exactly 32 byte, big endian integer values each.
            //
            byte[] xBytes = stripLeadingZeroes(x.toByteArray());
            if (xBytes.length > 32) {
                throw new RuntimeException("xBytes is " + xBytes.length + " which is unexpected");
            }
            for (int n = 0; n < 32 - xBytes.length; n++) {
                baos.write(0x00);
            }
            baos.write(xBytes);

            byte[] yBytes = stripLeadingZeroes(y.toByteArray());
            if (yBytes.length > 32) {
                throw new RuntimeException("yBytes is " + yBytes.length + " which is unexpected");
            }
            for (int n = 0; n < 32 - yBytes.length; n++) {
                baos.write(0x00);
            }
            baos.write(yBytes);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException", e);
        }
    }

    /**
     * Computes an HKDF.
     *
     * This is based on https://github.com/google/tink/blob/master/java/src/main/java/com/google
     * /crypto/tink/subtle/Hkdf.java
     * which is also Copyright (c) Google and also licensed under the Apache 2 license.
     *
     * @param macAlgorithm the MAC algorithm used for computing the Hkdf. I.e., "HMACSHA1" or
     *                     "HMACSHA256".
     * @param ikm          the input keying material.
     * @param salt         optional salt. A possibly non-secret random value. If no salt is
     *                     provided (i.e. if
     *                     salt has length 0) then an array of 0s of the same size as the hash
     *                     digest is used as salt.
     * @param info         optional context and application specific information.
     * @param size         The length of the generated pseudorandom string in bytes. The maximal
     *                     size is
     *                     255.DigestSize, where DigestSize is the size of the underlying HMAC.
     * @return size pseudorandom bytes.
     */
    @NonNull public static byte[] computeHkdf(
            @NonNull String macAlgorithm, @NonNull final byte[] ikm, @NonNull final byte[] salt,
            @NonNull final byte[] info, int size) {
        Mac mac = null;
        try {
            mac = Mac.getInstance(macAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No such algorithm: " + macAlgorithm, e);
        }
        if (size > 255 * mac.getMacLength()) {
            throw new RuntimeException("size too large");
        }
        try {
            if (salt == null || salt.length == 0) {
                // According to RFC 5869, Section 2.2 the salt is optional. If no salt is provided
                // then HKDF uses a salt that is an array of zeros of the same length as the hash
                // digest.
                mac.init(new SecretKeySpec(new byte[mac.getMacLength()], macAlgorithm));
            } else {
                mac.init(new SecretKeySpec(salt, macAlgorithm));
            }
            byte[] prk = mac.doFinal(ikm);
            byte[] result = new byte[size];
            int ctr = 1;
            int pos = 0;
            mac.init(new SecretKeySpec(prk, macAlgorithm));
            byte[] digest = new byte[0];
            while (true) {
                mac.update(digest);
                mac.update(info);
                mac.update((byte) ctr);
                digest = mac.doFinal();
                if (pos + digest.length < size) {
                    System.arraycopy(digest, 0, result, pos, digest.length);
                    pos += digest.length;
                    ctr++;
                } else {
                    System.arraycopy(digest, 0, result, pos, size - pos);
                    break;
                }
            }
            return result;
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Error MACing", e);
        }
    }

    private Util() {}
}
