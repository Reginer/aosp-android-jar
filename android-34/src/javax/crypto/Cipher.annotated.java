/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


package javax.crypto;

import static android.annotation.SystemApi.Client.MODULE_LIBRARIES;

import android.annotation.SystemApi;

import java.util.*;
import java.util.regex.*;
import java.security.*;
import javax.crypto.spec.*;
import sun.security.jca.*;
import java.nio.ReadOnlyBufferException;
import java.nio.ByteBuffer;
import java.security.spec.AlgorithmParameterSpec;
import java.security.Provider.Service;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class Cipher {

protected Cipher(javax.crypto.CipherSpi cipherSpi, java.security.Provider provider, java.lang.String transformation) { throw new RuntimeException("Stub!"); }

public static final javax.crypto.Cipher getInstance(java.lang.String transformation) throws java.security.NoSuchAlgorithmException, javax.crypto.NoSuchPaddingException { throw new RuntimeException("Stub!"); }

public static final javax.crypto.Cipher getInstance(java.lang.String transformation, java.lang.String provider) throws java.security.NoSuchAlgorithmException, javax.crypto.NoSuchPaddingException, java.security.NoSuchProviderException { throw new RuntimeException("Stub!"); }

public static final javax.crypto.Cipher getInstance(java.lang.String transformation, java.security.Provider provider) throws java.security.NoSuchAlgorithmException, javax.crypto.NoSuchPaddingException { throw new RuntimeException("Stub!"); }

public final java.security.Provider getProvider() { throw new RuntimeException("Stub!"); }

public final java.lang.String getAlgorithm() { throw new RuntimeException("Stub!"); }

public final int getBlockSize() { throw new RuntimeException("Stub!"); }

public final int getOutputSize(int inputLen) { throw new RuntimeException("Stub!"); }

public final byte[] getIV() { throw new RuntimeException("Stub!"); }

public final java.security.AlgorithmParameters getParameters() { throw new RuntimeException("Stub!"); }

public final javax.crypto.ExemptionMechanism getExemptionMechanism() { throw new RuntimeException("Stub!"); }

public final void init(int opmode, java.security.Key key) throws java.security.InvalidKeyException { throw new RuntimeException("Stub!"); }

public final void init(int opmode, java.security.Key key, java.security.SecureRandom random) throws java.security.InvalidKeyException { throw new RuntimeException("Stub!"); }

public final void init(int opmode, java.security.Key key, java.security.spec.AlgorithmParameterSpec params) throws java.security.InvalidAlgorithmParameterException, java.security.InvalidKeyException { throw new RuntimeException("Stub!"); }

public final void init(int opmode, java.security.Key key, java.security.spec.AlgorithmParameterSpec params, java.security.SecureRandom random) throws java.security.InvalidAlgorithmParameterException, java.security.InvalidKeyException { throw new RuntimeException("Stub!"); }

public final void init(int opmode, java.security.Key key, java.security.AlgorithmParameters params) throws java.security.InvalidAlgorithmParameterException, java.security.InvalidKeyException { throw new RuntimeException("Stub!"); }

public final void init(int opmode, java.security.Key key, java.security.AlgorithmParameters params, java.security.SecureRandom random) throws java.security.InvalidAlgorithmParameterException, java.security.InvalidKeyException { throw new RuntimeException("Stub!"); }

public final void init(int opmode, java.security.cert.Certificate certificate) throws java.security.InvalidKeyException { throw new RuntimeException("Stub!"); }

public final void init(int opmode, java.security.cert.Certificate certificate, java.security.SecureRandom random) throws java.security.InvalidKeyException { throw new RuntimeException("Stub!"); }

public final byte[] update(byte[] input) { throw new RuntimeException("Stub!"); }

public final byte[] update(byte[] input, int inputOffset, int inputLen) { throw new RuntimeException("Stub!"); }

public final int update(byte[] input, int inputOffset, int inputLen, byte[] output) throws javax.crypto.ShortBufferException { throw new RuntimeException("Stub!"); }

public final int update(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws javax.crypto.ShortBufferException { throw new RuntimeException("Stub!"); }

public final int update(java.nio.ByteBuffer input, java.nio.ByteBuffer output) throws javax.crypto.ShortBufferException { throw new RuntimeException("Stub!"); }

public final byte[] doFinal() throws javax.crypto.BadPaddingException, javax.crypto.IllegalBlockSizeException { throw new RuntimeException("Stub!"); }

public final int doFinal(byte[] output, int outputOffset) throws javax.crypto.BadPaddingException, javax.crypto.IllegalBlockSizeException, javax.crypto.ShortBufferException { throw new RuntimeException("Stub!"); }

public final byte[] doFinal(byte[] input) throws javax.crypto.BadPaddingException, javax.crypto.IllegalBlockSizeException { throw new RuntimeException("Stub!"); }

public final byte[] doFinal(byte[] input, int inputOffset, int inputLen) throws javax.crypto.BadPaddingException, javax.crypto.IllegalBlockSizeException { throw new RuntimeException("Stub!"); }

public final int doFinal(byte[] input, int inputOffset, int inputLen, byte[] output) throws javax.crypto.BadPaddingException, javax.crypto.IllegalBlockSizeException, javax.crypto.ShortBufferException { throw new RuntimeException("Stub!"); }

public final int doFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws javax.crypto.BadPaddingException, javax.crypto.IllegalBlockSizeException, javax.crypto.ShortBufferException { throw new RuntimeException("Stub!"); }

public final int doFinal(java.nio.ByteBuffer input, java.nio.ByteBuffer output) throws javax.crypto.BadPaddingException, javax.crypto.IllegalBlockSizeException, javax.crypto.ShortBufferException { throw new RuntimeException("Stub!"); }

public final byte[] wrap(java.security.Key key) throws javax.crypto.IllegalBlockSizeException, java.security.InvalidKeyException { throw new RuntimeException("Stub!"); }

public final java.security.Key unwrap(byte[] wrappedKey, java.lang.String wrappedKeyAlgorithm, int wrappedKeyType) throws java.security.InvalidKeyException, java.security.NoSuchAlgorithmException { throw new RuntimeException("Stub!"); }

public static final int getMaxAllowedKeyLength(java.lang.String transformation) throws java.security.NoSuchAlgorithmException { throw new RuntimeException("Stub!"); }

public static final java.security.spec.AlgorithmParameterSpec getMaxAllowedParameterSpec(java.lang.String transformation) throws java.security.NoSuchAlgorithmException { throw new RuntimeException("Stub!"); }

public final void updateAAD(byte[] src) { throw new RuntimeException("Stub!"); }

public final void updateAAD(byte[] src, int offset, int len) { throw new RuntimeException("Stub!"); }

public final void updateAAD(java.nio.ByteBuffer src) { throw new RuntimeException("Stub!"); }

@SystemApi(client = MODULE_LIBRARIES)
public javax.crypto.CipherSpi getCurrentSpi() { throw new RuntimeException("Stub!"); }

public static final int DECRYPT_MODE = 2; // 0x2

public static final int ENCRYPT_MODE = 1; // 0x1

public static final int PRIVATE_KEY = 2; // 0x2

public static final int PUBLIC_KEY = 1; // 0x1

public static final int SECRET_KEY = 3; // 0x3

public static final int UNWRAP_MODE = 4; // 0x4

public static final int WRAP_MODE = 3; // 0x3
}

