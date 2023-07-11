/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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


package java.util;

import java.util.function.Supplier;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public final class Objects {

Objects() { throw new RuntimeException("Stub!"); }

public static boolean equals(@libcore.util.Nullable java.lang.Object a, @libcore.util.Nullable java.lang.Object b) { throw new RuntimeException("Stub!"); }

public static boolean deepEquals(@libcore.util.Nullable java.lang.Object a, @libcore.util.Nullable java.lang.Object b) { throw new RuntimeException("Stub!"); }

public static int hashCode(@libcore.util.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

public static int hash(java.lang.@libcore.util.Nullable Object @libcore.util.Nullable ... values) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String toString(@libcore.util.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.String toString(@libcore.util.Nullable java.lang.Object o, @libcore.util.NonNull java.lang.String nullDefault) { throw new RuntimeException("Stub!"); }

public static <T> int compare(@libcore.util.NullFromTypeParam T a, @libcore.util.NullFromTypeParam T b, @libcore.util.NonNull java.util.Comparator<? super @libcore.util.NullFromTypeParam T> c) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> T requireNonNull(@libcore.util.Nullable T obj) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> T requireNonNull(@libcore.util.Nullable T obj, @libcore.util.NonNull java.lang.String message) { throw new RuntimeException("Stub!"); }

public static boolean isNull(@libcore.util.Nullable java.lang.Object obj) { throw new RuntimeException("Stub!"); }

public static boolean nonNull(@libcore.util.Nullable java.lang.Object obj) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> T requireNonNullElse(@libcore.util.Nullable T obj, @libcore.util.NonNull T defaultObj) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> T requireNonNullElseGet(@libcore.util.Nullable T obj, @libcore.util.NonNull java.util.function.Supplier<? extends @libcore.util.NonNull T> supplier) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> T requireNonNull(@libcore.util.Nullable T obj, @libcore.util.NonNull java.util.function.Supplier<@libcore.util.NonNull java.lang.String> messageSupplier) { throw new RuntimeException("Stub!"); }
}
