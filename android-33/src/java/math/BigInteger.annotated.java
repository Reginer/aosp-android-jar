/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.math;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class BigInteger extends java.lang.Number implements java.lang.Comparable<java.math.BigInteger> {

    public BigInteger(byte[] val, int off, int len) { throw new RuntimeException("Stub!"); }

    public BigInteger(byte[] val) { throw new RuntimeException("Stub!"); }

    public BigInteger(int signum, byte[] magnitude, int off, int len) { throw new RuntimeException("Stub!"); }

    public BigInteger(int signum, byte[] magnitude) { throw new RuntimeException("Stub!"); }

    public BigInteger(@libcore.util.NonNull java.lang.String val, int radix) { throw new RuntimeException("Stub!"); }

    public BigInteger(@libcore.util.NonNull java.lang.String val) { throw new RuntimeException("Stub!"); }

    public BigInteger(int numBits, @libcore.util.NonNull java.util.Random rnd) { throw new RuntimeException("Stub!"); }

    public BigInteger(int bitLength, int certainty, @libcore.util.NonNull java.util.Random rnd) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public static BigInteger probablePrime(int bitLength, @libcore.util.NonNull java.util.Random rnd) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger nextProbablePrime() { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull private static BigInteger lucasLehmerSequence(int z, @libcore.util.NonNull BigInteger k, @libcore.util.NonNull BigInteger n) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public static BigInteger valueOf(long val) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public static final BigInteger ZERO = null;

    @libcore.util.NonNull public static final BigInteger ONE = null;

    @libcore.util.NonNull public static final java.math.BigInteger TWO = null;

    @libcore.util.NonNull public static final BigInteger TEN = null;

    @libcore.util.NonNull public BigInteger add(@libcore.util.NonNull BigInteger val) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger subtract(@libcore.util.NonNull BigInteger val) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger multiply(@libcore.util.NonNull BigInteger val) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger divide(@libcore.util.NonNull BigInteger val) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger[] divideAndRemainder(@libcore.util.NonNull BigInteger val) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger remainder(@libcore.util.NonNull BigInteger val) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger pow(int exponent) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger gcd(@libcore.util.NonNull BigInteger val) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger abs() { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger negate() { throw new RuntimeException("Stub!"); }

    public int signum() { return 0; }

    @libcore.util.NonNull public BigInteger sqrt() { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger[] sqrtAndRemainder() { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger mod(@libcore.util.NonNull BigInteger m) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger modPow(@libcore.util.NonNull BigInteger exponent, @libcore.util.NonNull BigInteger m) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger modInverse(@libcore.util.NonNull BigInteger m) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger shiftLeft(int n) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger shiftRight(int n) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger and(@libcore.util.NonNull BigInteger val) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger or(@libcore.util.NonNull BigInteger val) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger xor(@libcore.util.NonNull BigInteger val) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger not() { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger andNot(@libcore.util.NonNull BigInteger val) { throw new RuntimeException("Stub!"); }

    public boolean testBit(int n) { return 0; }

    @libcore.util.NonNull public BigInteger setBit(int n) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger clearBit(int n) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger flipBit(int n) { throw new RuntimeException("Stub!"); }

    public int getLowestSetBit() { return 0; }

    public int bitLength() { return 0; }

    public int bitCount() { return 0; }

    public boolean isProbablePrime(int certainty) { return false; }

    public int compareTo(@libcore.util.NonNull BigInteger val) { return 0; }

    public boolean equals(@libcore.util.NonNull Object x) { return false; }

    @libcore.util.NonNull public BigInteger min(@libcore.util.NonNull BigInteger val) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public BigInteger max(@libcore.util.NonNull BigInteger val) { throw new RuntimeException("Stub!"); }

    public int hashCode() { return 0; }

    @libcore.util.NonNull public java.lang.String toString(int radix) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }

    public byte[] toByteArray() { return null; }

    public int intValue() { return 0; }

    public long longValue() { return 0L; }

    public float floatValue() { return 0.0f; }

    public double doubleValue() { return 0.0; }

    public long longValueExact() { return 0L; }

    public int intValueExact() { return 0; }

    public short shortValueExact() { return 0; }

    public byte byteValueExact() { return 0; }
}
