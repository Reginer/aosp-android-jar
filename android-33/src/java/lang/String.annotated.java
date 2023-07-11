/*
 * Copyright (C) 2014 The Android Open Source Project
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

import java.nio.charset.Charset;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.StringJoiner;
import java.util.Locale;
import java.util.Formatter;
import java.util.Comparator;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public final class String implements java.io.Serializable, java.lang.Comparable<java.lang.String>, java.lang.CharSequence {

public String() { throw new RuntimeException("Stub!"); }

public String(@libcore.util.NonNull java.lang.String original) { throw new RuntimeException("Stub!"); }

public String(char[] value) { throw new RuntimeException("Stub!"); }

public String(char[] value, int offset, int count) { throw new RuntimeException("Stub!"); }

public String(int[] codePoints, int offset, int count) { throw new RuntimeException("Stub!"); }

@Deprecated public String(byte[] ascii, int hibyte, int offset, int count) { throw new RuntimeException("Stub!"); }

@Deprecated public String(byte[] ascii, int hibyte) { throw new RuntimeException("Stub!"); }

public String(byte[] bytes, int offset, int length, @libcore.util.NonNull java.lang.String charsetName) throws java.io.UnsupportedEncodingException { throw new RuntimeException("Stub!"); }

public String(byte[] bytes, int offset, int length, @libcore.util.NonNull java.nio.charset.Charset charset) { throw new RuntimeException("Stub!"); }

public String(byte[] bytes, @libcore.util.NonNull java.lang.String charsetName) throws java.io.UnsupportedEncodingException { throw new RuntimeException("Stub!"); }

public String(byte[] bytes, @libcore.util.NonNull java.nio.charset.Charset charset) { throw new RuntimeException("Stub!"); }

public String(byte[] bytes, int offset, int length) { throw new RuntimeException("Stub!"); }

public String(byte[] bytes) { throw new RuntimeException("Stub!"); }

public String(@libcore.util.NonNull java.lang.StringBuffer buffer) { throw new RuntimeException("Stub!"); }

public String(@libcore.util.NonNull java.lang.StringBuilder builder) { throw new RuntimeException("Stub!"); }

public int length() { throw new RuntimeException("Stub!"); }

public boolean isEmpty() { throw new RuntimeException("Stub!"); }

public native char charAt(int index);

public int codePointAt(int index) { throw new RuntimeException("Stub!"); }

public int codePointBefore(int index) { throw new RuntimeException("Stub!"); }

public int codePointCount(int beginIndex, int endIndex) { throw new RuntimeException("Stub!"); }

public int offsetByCodePoints(int index, int codePointOffset) { throw new RuntimeException("Stub!"); }

public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) { throw new RuntimeException("Stub!"); }

@Deprecated public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) { throw new RuntimeException("Stub!"); }

public byte[] getBytes(@libcore.util.NonNull java.lang.String charsetName) throws java.io.UnsupportedEncodingException { throw new RuntimeException("Stub!"); }

public byte[] getBytes(@libcore.util.NonNull java.nio.charset.Charset charset) { throw new RuntimeException("Stub!"); }

public byte[] getBytes() { throw new RuntimeException("Stub!"); }

public boolean equals(@libcore.util.Nullable java.lang.Object anObject) { throw new RuntimeException("Stub!"); }

public boolean contentEquals(@libcore.util.NonNull java.lang.StringBuffer sb) { throw new RuntimeException("Stub!"); }

public boolean contentEquals(@libcore.util.NonNull java.lang.CharSequence cs) { throw new RuntimeException("Stub!"); }

public boolean equalsIgnoreCase(@libcore.util.Nullable java.lang.String anotherString) { throw new RuntimeException("Stub!"); }

public native int compareTo(@libcore.util.NonNull java.lang.String anotherString);

public int compareToIgnoreCase(@libcore.util.NonNull java.lang.String str) { throw new RuntimeException("Stub!"); }

public boolean regionMatches(int toffset, @libcore.util.NonNull java.lang.String other, int ooffset, int len) { throw new RuntimeException("Stub!"); }

public boolean regionMatches(boolean ignoreCase, int toffset, @libcore.util.NonNull java.lang.String other, int ooffset, int len) { throw new RuntimeException("Stub!"); }

public boolean startsWith(@libcore.util.NonNull java.lang.String prefix, int toffset) { throw new RuntimeException("Stub!"); }

public boolean startsWith(@libcore.util.NonNull java.lang.String prefix) { throw new RuntimeException("Stub!"); }

public boolean endsWith(@libcore.util.NonNull java.lang.String suffix) { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

public int indexOf(int ch) { throw new RuntimeException("Stub!"); }

public int indexOf(int ch, int fromIndex) { throw new RuntimeException("Stub!"); }

public int lastIndexOf(int ch) { throw new RuntimeException("Stub!"); }

public int lastIndexOf(int ch, int fromIndex) { throw new RuntimeException("Stub!"); }

public int indexOf(@libcore.util.NonNull java.lang.String str) { throw new RuntimeException("Stub!"); }

public int indexOf(@libcore.util.NonNull java.lang.String str, int fromIndex) { throw new RuntimeException("Stub!"); }

public int lastIndexOf(@libcore.util.NonNull java.lang.String str) { throw new RuntimeException("Stub!"); }

public int lastIndexOf(@libcore.util.NonNull java.lang.String str, int fromIndex) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String substring(int beginIndex) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String substring(int beginIndex, int endIndex) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.CharSequence subSequence(int beginIndex, int endIndex) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public native java.lang.String concat(@libcore.util.NonNull java.lang.String str);

@libcore.util.NonNull public java.lang.String replace(char oldChar, char newChar) { throw new RuntimeException("Stub!"); }

public boolean matches(@libcore.util.NonNull java.lang.String regex) { throw new RuntimeException("Stub!"); }

public boolean contains(@libcore.util.NonNull java.lang.CharSequence s) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String replaceFirst(@libcore.util.NonNull java.lang.String regex, @libcore.util.NonNull java.lang.String replacement) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String replaceAll(@libcore.util.NonNull java.lang.String regex, @libcore.util.NonNull java.lang.String replacement) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String replace(@libcore.util.NonNull java.lang.CharSequence target, @libcore.util.NonNull java.lang.CharSequence replacement) { throw new RuntimeException("Stub!"); }

public java.lang.@libcore.util.NonNull String @libcore.util.NonNull [] split(@libcore.util.NonNull java.lang.String regex, int limit) { throw new RuntimeException("Stub!"); }

public java.lang.@libcore.util.NonNull String @libcore.util.NonNull [] split(@libcore.util.NonNull java.lang.String regex) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String join(@libcore.util.NonNull java.lang.CharSequence delimiter, java.lang.@libcore.util.NonNull CharSequence @libcore.util.Nullable ... elements) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String join(@libcore.util.NonNull java.lang.CharSequence delimiter, @libcore.util.NonNull java.lang.Iterable<? extends @libcore.util.Nullable java.lang.CharSequence> elements) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toLowerCase(@libcore.util.NonNull java.util.Locale locale) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toLowerCase() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toUpperCase(@libcore.util.NonNull java.util.Locale locale) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toUpperCase() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String trim() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String strip() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String stripLeading() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String stripTrailing() { throw new RuntimeException("Stub!"); }

public boolean isBlank() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.stream.Stream<String> lines() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }

public native char[] toCharArray();

@libcore.util.NonNull public static java.lang.String format(@libcore.util.NonNull java.lang.String format, java.lang.@libcore.util.Nullable Object @libcore.util.NonNull ... args) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String format(@libcore.util.NonNull java.util.Locale l, @libcore.util.NonNull java.lang.String format, java.lang.@libcore.util.Nullable Object @libcore.util.NonNull ... args) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String valueOf(@libcore.util.Nullable java.lang.Object obj) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String valueOf(char[] data) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String valueOf(char[] data, int offset, int count) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String copyValueOf(char[] data, int offset, int count) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String copyValueOf(char[] data) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String valueOf(boolean b) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String valueOf(char c) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String valueOf(int i) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String valueOf(long l) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String valueOf(float f) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String valueOf(double d) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public native java.lang.String intern();

@libcore.util.NonNull public java.lang.String repeat(int count) { throw new RuntimeException("Stub!"); }

public static final java.util.Comparator<java.lang.String> CASE_INSENSITIVE_ORDER;
static { CASE_INSENSITIVE_ORDER = null; }
}
