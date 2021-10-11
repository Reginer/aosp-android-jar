/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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


package java.util.regex;


@SuppressWarnings({"unchecked", "deprecation", "all"})
public final class Pattern implements java.io.Serializable {

Pattern(@libcore.util.NonNull java.lang.String p, int f) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.regex.Pattern compile(@libcore.util.NonNull java.lang.String regex) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.util.regex.Pattern compile(@libcore.util.NonNull java.lang.String regex, int flags) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String pattern() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.regex.Matcher matcher(@libcore.util.NonNull java.lang.CharSequence input) { throw new RuntimeException("Stub!"); }

public int flags() { throw new RuntimeException("Stub!"); }

public static boolean matches(@libcore.util.NonNull java.lang.String regex, @libcore.util.NonNull java.lang.CharSequence input) { throw new RuntimeException("Stub!"); }

public java.lang.@libcore.util.NonNull String @libcore.util.NonNull [] split(@libcore.util.NonNull java.lang.CharSequence input, int limit) { throw new RuntimeException("Stub!"); }

public java.lang.@libcore.util.NonNull String @libcore.util.NonNull [] split(@libcore.util.NonNull java.lang.CharSequence input) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String quote(@libcore.util.NonNull java.lang.String s) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.function.Predicate<java.lang.@libcore.util.NonNull String> asPredicate() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.stream.Stream<java.lang.@libcore.util.NonNull String> splitAsStream(@libcore.util.NonNull java.lang.CharSequence input) { throw new RuntimeException("Stub!"); }

public static final int CANON_EQ = 128; // 0x80

public static final int CASE_INSENSITIVE = 2; // 0x2

public static final int COMMENTS = 4; // 0x4

public static final int DOTALL = 32; // 0x20

public static final int LITERAL = 16; // 0x10

public static final int MULTILINE = 8; // 0x8

public static final int UNICODE_CASE = 64; // 0x40

public static final int UNICODE_CHARACTER_CLASS = 256; // 0x100

public static final int UNIX_LINES = 1; // 0x1
}
