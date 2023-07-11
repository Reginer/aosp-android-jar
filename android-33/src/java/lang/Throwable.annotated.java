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

import java.io.*;
import java.util.*;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class Throwable implements java.io.Serializable {

public Throwable() { throw new RuntimeException("Stub!"); }

public Throwable(@libcore.util.Nullable java.lang.String message) { throw new RuntimeException("Stub!"); }

public Throwable(@libcore.util.Nullable java.lang.String message, @libcore.util.Nullable java.lang.Throwable cause) { throw new RuntimeException("Stub!"); }

public Throwable(@libcore.util.Nullable java.lang.Throwable cause) { throw new RuntimeException("Stub!"); }

protected Throwable(@libcore.util.Nullable java.lang.String message, @libcore.util.Nullable java.lang.Throwable cause, boolean enableSuppression, boolean writableStackTrace) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.lang.String getMessage() { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.lang.String getLocalizedMessage() { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public synchronized java.lang.Throwable getCause() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.Throwable initCause(@libcore.util.Nullable java.lang.Throwable cause) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }

public void printStackTrace() { throw new RuntimeException("Stub!"); }

public void printStackTrace(@libcore.util.NonNull java.io.PrintStream s) { throw new RuntimeException("Stub!"); }

public void printStackTrace(@libcore.util.NonNull java.io.PrintWriter s) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public synchronized java.lang.Throwable fillInStackTrace() { throw new RuntimeException("Stub!"); }

public java.lang.@libcore.util.NonNull StackTraceElement @libcore.util.NonNull [] getStackTrace() { throw new RuntimeException("Stub!"); }

public void setStackTrace(java.lang.@libcore.util.NonNull StackTraceElement @libcore.util.NonNull [] stackTrace) { throw new RuntimeException("Stub!"); }

public final synchronized void addSuppressed(@libcore.util.NonNull java.lang.Throwable exception) { throw new RuntimeException("Stub!"); }

public final synchronized java.lang.@libcore.util.NonNull Throwable @libcore.util.NonNull [] getSuppressed() { throw new RuntimeException("Stub!"); }
}
