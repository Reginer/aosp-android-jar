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
public final class StringBuffer implements java.lang.Appendable, java.lang.CharSequence, java.io.Serializable {

public StringBuffer() { throw new RuntimeException("Stub!"); }

public StringBuffer(int capacity) { throw new RuntimeException("Stub!"); }

public StringBuffer(@libcore.util.NonNull java.lang.String str) { throw new RuntimeException("Stub!"); }

public StringBuffer(@libcore.util.NonNull java.lang.CharSequence seq) { throw new RuntimeException("Stub!"); }

public synchronized int length() { throw new RuntimeException("Stub!"); }

public synchronized int capacity() { throw new RuntimeException("Stub!"); }

public synchronized void ensureCapacity(int minimumCapacity) { throw new RuntimeException("Stub!"); }

public synchronized void trimToSize() { throw new RuntimeException("Stub!"); }

public synchronized void setLength(int newLength) { throw new RuntimeException("Stub!"); }

public synchronized char charAt(int index) { throw new RuntimeException("Stub!"); }

public synchronized int codePointAt(int index) { throw new RuntimeException("Stub!"); }

public synchronized int codePointBefore(int index) { throw new RuntimeException("Stub!"); }

public synchronized int codePointCount(int beginIndex, int endIndex) { throw new RuntimeException("Stub!"); }

public synchronized int offsetByCodePoints(int index, int codePointOffset) { throw new RuntimeException("Stub!"); }

public synchronized void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) { throw new RuntimeException("Stub!"); }

public synchronized void setCharAt(int index, char ch) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer append(@libcore.util.Nullable java.lang.Object obj) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer append(@libcore.util.Nullable java.lang.String str) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer append(@libcore.util.Nullable java.lang.StringBuffer sb) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer append(@libcore.util.Nullable java.lang.CharSequence s) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer append(@libcore.util.Nullable java.lang.CharSequence s, int start, int end) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer append(char[] str) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer append(char[] str, int offset, int len) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer append(boolean b) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer append(char c) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer append(int i) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer appendCodePoint(int codePoint) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer append(long lng) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer append(float f) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer append(double d) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer delete(int start, int end) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer deleteCharAt(int index) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer replace(int start, int end, @libcore.util.NonNull java.lang.String str) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.String substring(int start) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.CharSequence subSequence(int start, int end) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.String substring(int start, int end) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer insert(int index, char[] str, int offset, int len) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer insert(int offset, @libcore.util.Nullable java.lang.Object obj) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer insert(int offset, @libcore.util.Nullable java.lang.String str) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer insert(int offset, char[] str) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.StringBuffer insert(int dstOffset, @libcore.util.Nullable java.lang.CharSequence s) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer insert(int dstOffset, @libcore.util.Nullable java.lang.CharSequence s, int start, int end) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.StringBuffer insert(int offset, boolean b) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer insert(int offset, char c) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.StringBuffer insert(int offset, int i) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.StringBuffer insert(int offset, long l) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.StringBuffer insert(int offset, float f) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.StringBuffer insert(int offset, double d) { throw new RuntimeException("Stub!"); }

public int indexOf(@libcore.util.NonNull java.lang.String str) { throw new RuntimeException("Stub!"); }

public synchronized int indexOf(@libcore.util.NonNull java.lang.String str, int fromIndex) { throw new RuntimeException("Stub!"); }

public int lastIndexOf(@libcore.util.NonNull java.lang.String str) { throw new RuntimeException("Stub!"); }

public synchronized int lastIndexOf(@libcore.util.NonNull java.lang.String str, int fromIndex) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.StringBuffer reverse() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.String toString() { throw new RuntimeException("Stub!"); }
}

