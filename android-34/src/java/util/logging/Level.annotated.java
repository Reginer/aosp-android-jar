/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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


package java.util.logging;


@SuppressWarnings({"unchecked", "deprecation", "all"})
public class Level implements java.io.Serializable {

protected Level(@libcore.util.NonNull java.lang.String name, int value) { throw new RuntimeException("Stub!"); }

protected Level(@libcore.util.NonNull java.lang.String name, int value, @libcore.util.Nullable java.lang.String resourceBundleName) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.lang.String getResourceBundleName() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getName() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getLocalizedName() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public final java.lang.String toString() { throw new RuntimeException("Stub!"); }

public final int intValue() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static synchronized java.util.logging.Level parse(@libcore.util.NonNull java.lang.String name) throws java.lang.IllegalArgumentException { throw new RuntimeException("Stub!"); }

public boolean equals(@libcore.util.Nullable java.lang.Object ox) { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static final java.util.logging.Level ALL;
static { ALL = null; }

@libcore.util.NonNull public static final java.util.logging.Level CONFIG;
static { CONFIG = null; }

@libcore.util.NonNull public static final java.util.logging.Level FINE;
static { FINE = null; }

@libcore.util.NonNull public static final java.util.logging.Level FINER;
static { FINER = null; }

@libcore.util.NonNull public static final java.util.logging.Level FINEST;
static { FINEST = null; }

@libcore.util.NonNull public static final java.util.logging.Level INFO;
static { INFO = null; }

@libcore.util.NonNull public static final java.util.logging.Level OFF;
static { OFF = null; }

@libcore.util.NonNull public static final java.util.logging.Level SEVERE;
static { SEVERE = null; }

@libcore.util.NonNull public static final java.util.logging.Level WARNING;
static { WARNING = null; }
}
