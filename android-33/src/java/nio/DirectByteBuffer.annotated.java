/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 2000, 2011, Oracle and/or its affiliates. All rights reserved.
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


package java.nio;


@libcore.api.Hide
@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
@SuppressWarnings({"unchecked", "deprecation", "all"})
public class DirectByteBuffer extends java.nio.MappedByteBuffer implements sun.nio.ch.DirectBuffer {

@libcore.api.Hide
@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public DirectByteBuffer(int cap, long addr, java.io.FileDescriptor fd, java.lang.Runnable unmapper, boolean isReadOnly) { super(0, 0, 0, 0); throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.lang.Object attachment() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final sun.misc.Cleaner cleaner() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer slice() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer duplicate() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer asReadOnlyBuffer() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public final long address() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final byte get() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final byte get(int i) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public java.nio.ByteBuffer get(byte[] dst, int dstOffset, int length) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public java.nio.ByteBuffer put(java.nio.ByteBuffer src) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer put(byte x) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer put(int i, byte x) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public java.nio.ByteBuffer put(byte[] src, int srcOffset, int length) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer compact() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final boolean isDirect() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final boolean isReadOnly() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final char getChar() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final char getChar(int i) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer putChar(char x) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer putChar(int i, char x) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.CharBuffer asCharBuffer() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final short getShort() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final short getShort(int i) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer putShort(short x) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer putShort(int i, short x) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ShortBuffer asShortBuffer() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public int getInt() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public int getInt(int i) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer putInt(int x) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer putInt(int i, int x) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.IntBuffer asIntBuffer() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final long getLong() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final long getLong(int i) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer putLong(long x) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer putLong(int i, long x) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.LongBuffer asLongBuffer() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final float getFloat() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final float getFloat(int i) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer putFloat(float x) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer putFloat(int i, float x) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.FloatBuffer asFloatBuffer() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final double getDouble() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final double getDouble(int i) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer putDouble(double x) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.ByteBuffer putDouble(int i, double x) { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final java.nio.DoubleBuffer asDoubleBuffer() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public final boolean isAccessible() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public final void setAccessible(boolean value) { throw new RuntimeException("Stub!"); }
}

