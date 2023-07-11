/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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
public abstract class Enum<E extends java.lang.Enum<E>> implements java.lang.Comparable<E>, java.io.Serializable {

protected Enum(@libcore.util.NonNull java.lang.String name, int ordinal) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public final java.lang.String name() { throw new RuntimeException("Stub!"); }

public final int ordinal() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }

public final boolean equals(@libcore.util.Nullable java.lang.Object other) { throw new RuntimeException("Stub!"); }

public final int hashCode() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull protected final java.lang.Object clone() throws java.lang.CloneNotSupportedException { throw new RuntimeException("Stub!"); }

public final int compareTo(@libcore.util.NullFromTypeParam E o) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public final java.lang.Class<E> getDeclaringClass() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T extends java.lang.Enum<T>> T valueOf(@libcore.util.NonNull java.lang.Class<T> enumType, @libcore.util.NonNull java.lang.String name) { throw new RuntimeException("Stub!"); }

protected final void finalize() { throw new RuntimeException("Stub!"); }
}
