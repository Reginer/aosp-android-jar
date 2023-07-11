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
public final class Matcher implements java.util.regex.MatchResult {

Matcher(@libcore.util.NonNull java.util.regex.Pattern parent, @libcore.util.NonNull java.lang.CharSequence text) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.regex.Pattern pattern() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.regex.MatchResult toMatchResult() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.regex.Matcher usePattern(@libcore.util.NonNull java.util.regex.Pattern newPattern) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.regex.Matcher reset() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.regex.Matcher reset(@libcore.util.NonNull java.lang.CharSequence input) { throw new RuntimeException("Stub!"); }

public int start() { throw new RuntimeException("Stub!"); }

public int start(int group) { throw new RuntimeException("Stub!"); }

public int start(@libcore.util.NonNull java.lang.String name) { throw new RuntimeException("Stub!"); }

public int end() { throw new RuntimeException("Stub!"); }

public int end(int group) { throw new RuntimeException("Stub!"); }

public int end(@libcore.util.NonNull java.lang.String name) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String group() { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.lang.String group(int group) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.lang.String group(@libcore.util.NonNull java.lang.String name) { throw new RuntimeException("Stub!"); }

public int groupCount() { throw new RuntimeException("Stub!"); }

public boolean matches() { throw new RuntimeException("Stub!"); }

public boolean find() { throw new RuntimeException("Stub!"); }

public boolean find(int start) { throw new RuntimeException("Stub!"); }

public boolean lookingAt() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String quoteReplacement(@libcore.util.NonNull java.lang.String s) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.regex.Matcher appendReplacement(@libcore.util.NonNull java.lang.StringBuffer sb, @libcore.util.NonNull java.lang.String replacement) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.StringBuffer appendTail(@libcore.util.NonNull java.lang.StringBuffer sb) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String replaceAll(@libcore.util.NonNull java.lang.String replacement) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String replaceFirst(@libcore.util.NonNull java.lang.String replacement) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.regex.Matcher region(int start, int end) { throw new RuntimeException("Stub!"); }

public int regionStart() { throw new RuntimeException("Stub!"); }

public int regionEnd() { throw new RuntimeException("Stub!"); }

public boolean hasTransparentBounds() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.regex.Matcher useTransparentBounds(boolean b) { throw new RuntimeException("Stub!"); }

public boolean hasAnchoringBounds() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.regex.Matcher useAnchoringBounds(boolean b) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }

public boolean hitEnd() { throw new RuntimeException("Stub!"); }

public boolean requireEnd() { throw new RuntimeException("Stub!"); }
}
