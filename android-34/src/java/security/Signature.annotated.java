/*
 * Copyright (c) 1996, 2015, Oracle and/or its affiliates. All rights reserved.
 * Copyright (C) 2014 The Android Open Source Project
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


package java.security;

import java.util.*;
import java.io.*;
import sun.security.jca.*;
import java.nio.ByteBuffer;
import java.security.spec.AlgorithmParameterSpec;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public abstract class Signature extends java.security.SignatureSpi {

protected Signature(java.lang.String algorithm) { throw new RuntimeException("Stub!"); }

public static java.security.Signature getInstance(java.lang.String algorithm) throws java.security.NoSuchAlgorithmException { throw new RuntimeException("Stub!"); }

public static java.security.Signature getInstance(java.lang.String algorithm, java.lang.String provider) throws java.security.NoSuchAlgorithmException, java.security.NoSuchProviderException { throw new RuntimeException("Stub!"); }

public static java.security.Signature getInstance(java.lang.String algorithm, java.security.Provider provider) throws java.security.NoSuchAlgorithmException { throw new RuntimeException("Stub!"); }

public final java.security.Provider getProvider() { throw new RuntimeException("Stub!"); }

public final void initVerify(java.security.PublicKey publicKey) throws java.security.InvalidKeyException { throw new RuntimeException("Stub!"); }

public final void initVerify(java.security.cert.Certificate certificate) throws java.security.InvalidKeyException { throw new RuntimeException("Stub!"); }

public final void initSign(java.security.PrivateKey privateKey) throws java.security.InvalidKeyException { throw new RuntimeException("Stub!"); }

public final void initSign(java.security.PrivateKey privateKey, java.security.SecureRandom random) throws java.security.InvalidKeyException { throw new RuntimeException("Stub!"); }

public final byte[] sign() throws java.security.SignatureException { throw new RuntimeException("Stub!"); }

public final int sign(byte[] outbuf, int offset, int len) throws java.security.SignatureException { throw new RuntimeException("Stub!"); }

public final boolean verify(byte[] signature) throws java.security.SignatureException { throw new RuntimeException("Stub!"); }

public final boolean verify(byte[] signature, int offset, int length) throws java.security.SignatureException { throw new RuntimeException("Stub!"); }

public final void update(byte b) throws java.security.SignatureException { throw new RuntimeException("Stub!"); }

public final void update(byte[] data) throws java.security.SignatureException { throw new RuntimeException("Stub!"); }

public final void update(byte[] data, int off, int len) throws java.security.SignatureException { throw new RuntimeException("Stub!"); }

public final void update(java.nio.ByteBuffer data) throws java.security.SignatureException { throw new RuntimeException("Stub!"); }

public final java.lang.String getAlgorithm() { throw new RuntimeException("Stub!"); }

public java.lang.String toString() { throw new RuntimeException("Stub!"); }

@Deprecated
public final void setParameter(java.lang.String param, java.lang.Object value) throws java.security.InvalidParameterException { throw new RuntimeException("Stub!"); }

public final void setParameter(java.security.spec.AlgorithmParameterSpec params) throws java.security.InvalidAlgorithmParameterException { throw new RuntimeException("Stub!"); }

public final java.security.AlgorithmParameters getParameters() { throw new RuntimeException("Stub!"); }

@Deprecated
public final java.lang.Object getParameter(java.lang.String param) throws java.security.InvalidParameterException { throw new RuntimeException("Stub!"); }

public java.lang.Object clone() throws java.lang.CloneNotSupportedException { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public java.security.SignatureSpi getCurrentSpi() { throw new RuntimeException("Stub!"); }

protected static final int SIGN = 2; // 0x2

protected static final int UNINITIALIZED = 0; // 0x0

protected static final int VERIFY = 3; // 0x3

protected int state = 0; // 0x0
}

