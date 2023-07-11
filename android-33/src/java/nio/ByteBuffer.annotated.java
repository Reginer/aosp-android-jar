/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

// -- This file was mechanically generated: Do not edit! -- //


package java.nio;


@SuppressWarnings({"unchecked", "deprecation", "all"})
public abstract class ByteBuffer extends java.nio.Buffer implements java.lang.Comparable<java.nio.ByteBuffer> {

ByteBuffer(int mark, int pos, int lim, int cap) { super(0, 0, 0, 0, 0); throw new RuntimeException("Stub!"); }

public static java.nio.ByteBuffer allocateDirect(int capacity) { throw new RuntimeException("Stub!"); }

public static java.nio.ByteBuffer allocate(int capacity) { throw new RuntimeException("Stub!"); }

public static java.nio.ByteBuffer wrap(byte[] array, int offset, int length) { throw new RuntimeException("Stub!"); }

public static java.nio.ByteBuffer wrap(byte[] array) { throw new RuntimeException("Stub!"); }

public abstract java.nio.ByteBuffer slice();

public abstract java.nio.ByteBuffer duplicate();

public abstract java.nio.ByteBuffer asReadOnlyBuffer();

public abstract byte get();

public abstract java.nio.ByteBuffer put(byte b);

public abstract byte get(int index);

public abstract java.nio.ByteBuffer put(int index, byte b);

public java.nio.ByteBuffer get(byte[] dst, int offset, int length) { throw new RuntimeException("Stub!"); }

public java.nio.ByteBuffer get(byte[] dst) { throw new RuntimeException("Stub!"); }

public java.nio.ByteBuffer put(java.nio.ByteBuffer src) { throw new RuntimeException("Stub!"); }

public java.nio.ByteBuffer put(byte[] src, int offset, int length) { throw new RuntimeException("Stub!"); }

public final java.nio.ByteBuffer put(byte[] src) { throw new RuntimeException("Stub!"); }

public final boolean hasArray() { throw new RuntimeException("Stub!"); }

public final byte[] array() { throw new RuntimeException("Stub!"); }

public final int arrayOffset() { throw new RuntimeException("Stub!"); }

public abstract java.nio.ByteBuffer compact();

public abstract boolean isDirect();

public java.lang.String toString() { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

public boolean equals(java.lang.Object ob) { throw new RuntimeException("Stub!"); }

public int compareTo(java.nio.ByteBuffer that) { throw new RuntimeException("Stub!"); }

public final java.nio.ByteOrder order() { throw new RuntimeException("Stub!"); }

public final java.nio.ByteBuffer order(java.nio.ByteOrder bo) { throw new RuntimeException("Stub!"); }

public abstract char getChar();

public abstract java.nio.ByteBuffer putChar(char value);

public abstract char getChar(int index);

public abstract java.nio.ByteBuffer putChar(int index, char value);

public abstract java.nio.CharBuffer asCharBuffer();

public abstract short getShort();

public abstract java.nio.ByteBuffer putShort(short value);

public abstract short getShort(int index);

public abstract java.nio.ByteBuffer putShort(int index, short value);

public abstract java.nio.ShortBuffer asShortBuffer();

public abstract int getInt();

public abstract java.nio.ByteBuffer putInt(int value);

public abstract int getInt(int index);

public abstract java.nio.ByteBuffer putInt(int index, int value);

public abstract java.nio.IntBuffer asIntBuffer();

public abstract long getLong();

public abstract java.nio.ByteBuffer putLong(long value);

public abstract long getLong(int index);

public abstract java.nio.ByteBuffer putLong(int index, long value);

public abstract java.nio.LongBuffer asLongBuffer();

public abstract float getFloat();

public abstract java.nio.ByteBuffer putFloat(float value);

public abstract float getFloat(int index);

public abstract java.nio.ByteBuffer putFloat(int index, float value);

public abstract java.nio.FloatBuffer asFloatBuffer();

public abstract double getDouble();

public abstract java.nio.ByteBuffer putDouble(double value);

public abstract double getDouble(int index);

public abstract java.nio.ByteBuffer putDouble(int index, double value);

public abstract java.nio.DoubleBuffer asDoubleBuffer();

public boolean isAccessible() { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public void setAccessible(boolean value) { throw new RuntimeException("Stub!"); }
}

