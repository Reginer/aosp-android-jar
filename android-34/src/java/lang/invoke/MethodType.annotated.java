/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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


package java.lang.invoke;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public final class MethodType implements java.lang.invoke.TypeDescriptor.OfMethod<java.lang.Class<?>,java.lang.invoke.MethodType>, java.io.Serializable {

MethodType() { throw new RuntimeException("Stub!"); }

public static java.lang.invoke.MethodType methodType(java.lang.Class<?> rtype, java.lang.Class<?>[] ptypes) { throw new RuntimeException("Stub!"); }

public static java.lang.invoke.MethodType methodType(java.lang.Class<?> rtype, java.util.List<java.lang.Class<?>> ptypes) { throw new RuntimeException("Stub!"); }

public static java.lang.invoke.MethodType methodType(java.lang.Class<?> rtype, java.lang.Class<?> ptype0, java.lang.Class<?>... ptypes) { throw new RuntimeException("Stub!"); }

public static java.lang.invoke.MethodType methodType(java.lang.Class<?> rtype) { throw new RuntimeException("Stub!"); }

public static java.lang.invoke.MethodType methodType(java.lang.Class<?> rtype, java.lang.Class<?> ptype0) { throw new RuntimeException("Stub!"); }

public static java.lang.invoke.MethodType methodType(java.lang.Class<?> rtype, java.lang.invoke.MethodType ptypes) { throw new RuntimeException("Stub!"); }

public static java.lang.invoke.MethodType genericMethodType(int objectArgCount, boolean finalArray) { throw new RuntimeException("Stub!"); }

public static java.lang.invoke.MethodType genericMethodType(int objectArgCount) { throw new RuntimeException("Stub!"); }

public java.lang.invoke.MethodType changeParameterType(int num, java.lang.Class<?> nptype) { throw new RuntimeException("Stub!"); }

public java.lang.invoke.MethodType insertParameterTypes(int num, java.lang.Class<?>... ptypesToInsert) { throw new RuntimeException("Stub!"); }

public java.lang.invoke.MethodType appendParameterTypes(java.lang.Class<?>... ptypesToInsert) { throw new RuntimeException("Stub!"); }

public java.lang.invoke.MethodType insertParameterTypes(int num, java.util.List<java.lang.Class<?>> ptypesToInsert) { throw new RuntimeException("Stub!"); }

public java.lang.invoke.MethodType appendParameterTypes(java.util.List<java.lang.Class<?>> ptypesToInsert) { throw new RuntimeException("Stub!"); }

public java.lang.invoke.MethodType dropParameterTypes(int start, int end) { throw new RuntimeException("Stub!"); }

public java.lang.invoke.MethodType changeReturnType(java.lang.Class<?> nrtype) { throw new RuntimeException("Stub!"); }

public boolean hasPrimitives() { throw new RuntimeException("Stub!"); }

public boolean hasWrappers() { throw new RuntimeException("Stub!"); }

public java.lang.invoke.MethodType erase() { throw new RuntimeException("Stub!"); }

public java.lang.invoke.MethodType generic() { throw new RuntimeException("Stub!"); }

public java.lang.invoke.MethodType wrap() { throw new RuntimeException("Stub!"); }

public java.lang.invoke.MethodType unwrap() { throw new RuntimeException("Stub!"); }

public java.lang.Class<?> parameterType(int num) { throw new RuntimeException("Stub!"); }

public int parameterCount() { throw new RuntimeException("Stub!"); }

public java.lang.Class<?> returnType() { throw new RuntimeException("Stub!"); }

public java.util.List<java.lang.Class<?>> parameterList() { throw new RuntimeException("Stub!"); }

public java.lang.Class<?> lastParameterType() { throw new RuntimeException("Stub!"); }

public java.lang.Class<?>[] parameterArray() { throw new RuntimeException("Stub!"); }

public boolean equals(java.lang.Object x) { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

public java.lang.String toString() { throw new RuntimeException("Stub!"); }

public static java.lang.invoke.MethodType fromMethodDescriptorString(java.lang.String descriptor, java.lang.ClassLoader loader) throws java.lang.IllegalArgumentException, java.lang.TypeNotPresentException { throw new RuntimeException("Stub!"); }

public java.lang.String toMethodDescriptorString() { throw new RuntimeException("Stub!"); }

public @libcore.util.NonNull java.lang.String descriptorString() { throw new RuntimeException("Stub!"); }
}

