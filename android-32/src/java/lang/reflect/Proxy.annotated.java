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


package java.lang.reflect;

import java.security.Permission;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class Proxy implements java.io.Serializable {

protected Proxy(@libcore.util.NonNull java.lang.reflect.InvocationHandler h) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.Class<?> getProxyClass(@libcore.util.Nullable java.lang.ClassLoader loader, java.lang.@libcore.util.NonNull Class<?> @libcore.util.NonNull ... interfaces) throws java.lang.IllegalArgumentException { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.Object newProxyInstance(@libcore.util.Nullable java.lang.ClassLoader loader, java.lang.@libcore.util.NonNull Class<?> @libcore.util.NonNull [] interfaces, @libcore.util.NonNull java.lang.reflect.InvocationHandler h) throws java.lang.IllegalArgumentException { throw new RuntimeException("Stub!"); }

public static boolean isProxyClass(@libcore.util.NonNull java.lang.Class<?> cl) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static java.lang.reflect.InvocationHandler getInvocationHandler(@libcore.util.NonNull java.lang.Object proxy) throws java.lang.IllegalArgumentException { throw new RuntimeException("Stub!"); }

protected java.lang.reflect.InvocationHandler h;
}
