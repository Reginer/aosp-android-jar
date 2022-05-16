/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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


@SuppressWarnings({"unchecked", "deprecation", "all"})
public final class Field extends java.lang.reflect.AccessibleObject implements java.lang.reflect.Member {

Field() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.Class<?> getDeclaringClass() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String getName() { throw new RuntimeException("Stub!"); }

public int getModifiers() { throw new RuntimeException("Stub!"); }

public boolean isEnumConstant() { throw new RuntimeException("Stub!"); }

public boolean isSynthetic() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.Class<?> getType() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.reflect.Type getGenericType() { throw new RuntimeException("Stub!"); }

public boolean equals(@libcore.util.Nullable java.lang.Object obj) { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toGenericString() { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public native java.lang.Object get(@libcore.util.Nullable java.lang.Object obj) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native boolean getBoolean(@libcore.util.Nullable java.lang.Object obj) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native byte getByte(@libcore.util.Nullable java.lang.Object obj) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native char getChar(@libcore.util.Nullable java.lang.Object obj) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native short getShort(@libcore.util.Nullable java.lang.Object obj) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native int getInt(@libcore.util.Nullable java.lang.Object obj) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native long getLong(@libcore.util.Nullable java.lang.Object obj) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native float getFloat(@libcore.util.Nullable java.lang.Object obj) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native double getDouble(@libcore.util.Nullable java.lang.Object obj) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native void set(@libcore.util.Nullable java.lang.Object obj, @libcore.util.Nullable java.lang.Object value) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native void setBoolean(@libcore.util.Nullable java.lang.Object obj, boolean z) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native void setByte(@libcore.util.Nullable java.lang.Object obj, byte b) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native void setChar(@libcore.util.Nullable java.lang.Object obj, char c) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native void setShort(@libcore.util.Nullable java.lang.Object obj, short s) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native void setInt(@libcore.util.Nullable java.lang.Object obj, int i) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native void setLong(@libcore.util.Nullable java.lang.Object obj, long l) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native void setFloat(@libcore.util.Nullable java.lang.Object obj, float f) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

public native void setDouble(@libcore.util.Nullable java.lang.Object obj, double d) throws java.lang.IllegalAccessException, java.lang.IllegalArgumentException;

@libcore.util.Nullable public <T extends java.lang.annotation.Annotation> T getAnnotation(@libcore.util.NonNull java.lang.Class<T> annotationClass) { throw new RuntimeException("Stub!"); }

public <T extends java.lang.annotation.Annotation> T[] getAnnotationsByType(@libcore.util.NonNull java.lang.Class<T> annotationClass) { throw new RuntimeException("Stub!"); }

public boolean isAnnotationPresent(@libcore.util.NonNull java.lang.Class<? extends java.lang.annotation.Annotation> annotationType) { throw new RuntimeException("Stub!"); }

public native java.lang.annotation.Annotation[] getDeclaredAnnotations();
}
