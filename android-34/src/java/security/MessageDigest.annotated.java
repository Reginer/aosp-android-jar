/*
 * Copyright (c) 1996, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.*;
import java.io.InputStream;
import java.nio.ByteBuffer;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public abstract class MessageDigest extends java.security.MessageDigestSpi {

protected MessageDigest(@libcore.util.NonNull java.lang.String algorithm) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.security.MessageDigest getInstance(@libcore.util.NonNull java.lang.String algorithm) throws java.security.NoSuchAlgorithmException { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.security.MessageDigest getInstance(@libcore.util.NonNull java.lang.String algorithm, @libcore.util.NonNull java.lang.String provider) throws java.security.NoSuchAlgorithmException, java.security.NoSuchProviderException { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.security.MessageDigest getInstance(@libcore.util.NonNull java.lang.String algorithm, @libcore.util.NonNull java.security.Provider provider) throws java.security.NoSuchAlgorithmException { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public final java.security.Provider getProvider() { throw new RuntimeException("Stub!"); }

public void update(byte input) { throw new RuntimeException("Stub!"); }

public void update(byte @libcore.util.NonNull [] input, int offset, int len) { throw new RuntimeException("Stub!"); }

public void update(byte @libcore.util.NonNull [] input) { throw new RuntimeException("Stub!"); }

public final void update(@libcore.util.NonNull java.nio.ByteBuffer input) { throw new RuntimeException("Stub!"); }

public byte @libcore.util.NonNull [] digest() { throw new RuntimeException("Stub!"); }

public int digest(byte @libcore.util.NonNull [] buf, int offset, int len) throws java.security.DigestException { throw new RuntimeException("Stub!"); }

public byte @libcore.util.NonNull [] digest(byte @libcore.util.NonNull [] input) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }

public static boolean isEqual(byte @libcore.util.Nullable [] digesta, byte @libcore.util.Nullable [] digestb) { throw new RuntimeException("Stub!"); }

public void reset() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public final java.lang.String getAlgorithm() { throw new RuntimeException("Stub!"); }

public final int getDigestLength() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.Object clone() throws java.lang.CloneNotSupportedException { throw new RuntimeException("Stub!"); }
}
