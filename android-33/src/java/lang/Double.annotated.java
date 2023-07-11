/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
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


package java.lang;


@SuppressWarnings({"unchecked", "deprecation", "all"})
public final class Double extends java.lang.Number implements java.lang.Comparable<java.lang.Double> {

public Double(double value) { throw new RuntimeException("Stub!"); }

public Double(@libcore.util.NonNull java.lang.String s) throws java.lang.NumberFormatException { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String toString(double d) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String toHexString(double d) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.Double valueOf(@libcore.util.NonNull java.lang.String s) throws java.lang.NumberFormatException { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.Double valueOf(double d) { throw new RuntimeException("Stub!"); }

public static double parseDouble(@libcore.util.NonNull java.lang.String s) throws java.lang.NumberFormatException { throw new RuntimeException("Stub!"); }

public static boolean isNaN(double v) { throw new RuntimeException("Stub!"); }

public static boolean isInfinite(double v) { throw new RuntimeException("Stub!"); }

public static boolean isFinite(double d) { throw new RuntimeException("Stub!"); }

public boolean isNaN() { throw new RuntimeException("Stub!"); }

public boolean isInfinite() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }

public byte byteValue() { throw new RuntimeException("Stub!"); }

public short shortValue() { throw new RuntimeException("Stub!"); }

public int intValue() { throw new RuntimeException("Stub!"); }

public long longValue() { throw new RuntimeException("Stub!"); }

public float floatValue() { throw new RuntimeException("Stub!"); }

public double doubleValue() { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

public static int hashCode(double value) { throw new RuntimeException("Stub!"); }

public boolean equals(@libcore.util.Nullable java.lang.Object obj) { throw new RuntimeException("Stub!"); }

public static long doubleToLongBits(double value) { throw new RuntimeException("Stub!"); }

public static native long doubleToRawLongBits(double value);

public static native double longBitsToDouble(long bits);

public int compareTo(@libcore.util.NonNull java.lang.Double anotherDouble) { throw new RuntimeException("Stub!"); }

public static int compare(double d1, double d2) { throw new RuntimeException("Stub!"); }

public static double sum(double a, double b) { throw new RuntimeException("Stub!"); }

public static double max(double a, double b) { throw new RuntimeException("Stub!"); }

public static double min(double a, double b) { throw new RuntimeException("Stub!"); }

public static final int BYTES = 8; // 0x8

public static final int MAX_EXPONENT = 1023; // 0x3ff

public static final double MAX_VALUE = 1.7976931348623157E308;

public static final int MIN_EXPONENT = -1022; // 0xfffffc02

public static final double MIN_NORMAL = 2.2250738585072014E-308;

public static final double MIN_VALUE = 4.9E-324;

public static final double NEGATIVE_INFINITY = (-1.0/0.0);

public static final double NaN = (0.0/0.0);

public static final double POSITIVE_INFINITY = (1.0/0.0);

public static final int SIZE = 64; // 0x40

public static final java.lang.Class<java.lang.Double> TYPE;
static { TYPE = null; }
}

