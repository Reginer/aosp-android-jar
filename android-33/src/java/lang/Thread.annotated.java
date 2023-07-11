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

import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class Thread implements java.lang.Runnable {

public Thread() { throw new RuntimeException("Stub!"); }

public Thread(java.lang.Runnable target) { throw new RuntimeException("Stub!"); }

public Thread(java.lang.ThreadGroup group, java.lang.Runnable target) { throw new RuntimeException("Stub!"); }

public Thread(java.lang.String name) { throw new RuntimeException("Stub!"); }

public Thread(java.lang.ThreadGroup group, java.lang.String name) { throw new RuntimeException("Stub!"); }

public Thread(java.lang.Runnable target, java.lang.String name) { throw new RuntimeException("Stub!"); }

public Thread(java.lang.ThreadGroup group, java.lang.Runnable target, java.lang.String name) { throw new RuntimeException("Stub!"); }

public Thread(java.lang.ThreadGroup group, java.lang.Runnable target, java.lang.String name, long stackSize) { throw new RuntimeException("Stub!"); }

public static native java.lang.Thread currentThread();

public static native void yield();

public static void sleep(long millis) throws java.lang.InterruptedException { throw new RuntimeException("Stub!"); }

public static void sleep(long millis, int nanos) throws java.lang.InterruptedException { throw new RuntimeException("Stub!"); }

protected java.lang.Object clone() throws java.lang.CloneNotSupportedException { throw new RuntimeException("Stub!"); }

public synchronized void start() { throw new RuntimeException("Stub!"); }

public void run() { throw new RuntimeException("Stub!"); }

@Deprecated
public final void stop() { throw new RuntimeException("Stub!"); }

@Deprecated
public final synchronized void stop(java.lang.Throwable obj) { throw new RuntimeException("Stub!"); }

public void interrupt() { throw new RuntimeException("Stub!"); }

public static native boolean interrupted();

public native boolean isInterrupted();

@Deprecated
public void destroy() { throw new RuntimeException("Stub!"); }

public final boolean isAlive() { throw new RuntimeException("Stub!"); }

@Deprecated
public final void suspend() { throw new RuntimeException("Stub!"); }

@Deprecated
public final void resume() { throw new RuntimeException("Stub!"); }

public final void setPriority(int newPriority) { throw new RuntimeException("Stub!"); }

public final int getPriority() { throw new RuntimeException("Stub!"); }

public final synchronized void setName(java.lang.String name) { throw new RuntimeException("Stub!"); }

public final java.lang.String getName() { throw new RuntimeException("Stub!"); }

public final java.lang.ThreadGroup getThreadGroup() { throw new RuntimeException("Stub!"); }

public static int activeCount() { throw new RuntimeException("Stub!"); }

public static int enumerate(java.lang.Thread[] tarray) { throw new RuntimeException("Stub!"); }

@Deprecated
public int countStackFrames() { throw new RuntimeException("Stub!"); }

public final void join(long millis) throws java.lang.InterruptedException { throw new RuntimeException("Stub!"); }

public final void join(long millis, int nanos) throws java.lang.InterruptedException { throw new RuntimeException("Stub!"); }

public final void join() throws java.lang.InterruptedException { throw new RuntimeException("Stub!"); }

public static void dumpStack() { throw new RuntimeException("Stub!"); }

public final void setDaemon(boolean on) { throw new RuntimeException("Stub!"); }

public final boolean isDaemon() { throw new RuntimeException("Stub!"); }

public final void checkAccess() { throw new RuntimeException("Stub!"); }

public java.lang.String toString() { throw new RuntimeException("Stub!"); }

public java.lang.ClassLoader getContextClassLoader() { throw new RuntimeException("Stub!"); }

public void setContextClassLoader(java.lang.ClassLoader cl) { throw new RuntimeException("Stub!"); }

public static native boolean holdsLock(java.lang.Object obj);

public java.lang.StackTraceElement[] getStackTrace() { throw new RuntimeException("Stub!"); }

public static java.util.Map<java.lang.Thread,java.lang.StackTraceElement[]> getAllStackTraces() { throw new RuntimeException("Stub!"); }

public long getId() { throw new RuntimeException("Stub!"); }

public java.lang.Thread.State getState() { throw new RuntimeException("Stub!"); }

public static void setDefaultUncaughtExceptionHandler(java.lang.Thread.UncaughtExceptionHandler eh) { throw new RuntimeException("Stub!"); }

public static java.lang.Thread.UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public static void setUncaughtExceptionPreHandler(UncaughtExceptionHandler eh) { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public static UncaughtExceptionHandler getUncaughtExceptionPreHandler() { throw new RuntimeException("Stub!"); }

public java.lang.Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() { throw new RuntimeException("Stub!"); }

public void setUncaughtExceptionHandler(java.lang.Thread.UncaughtExceptionHandler eh) { throw new RuntimeException("Stub!"); }

public static final int MAX_PRIORITY = 10; // 0xa

public static final int MIN_PRIORITY = 1; // 0x1

public static final int NORM_PRIORITY = 5; // 0x5
@SuppressWarnings({"unchecked", "deprecation", "all"})
public static enum State {
NEW,
RUNNABLE,
BLOCKED,
WAITING,
TIMED_WAITING,
TERMINATED;
}

@SuppressWarnings({"unchecked", "deprecation", "all"})
@java.lang.FunctionalInterface
public static interface UncaughtExceptionHandler {

public void uncaughtException(java.lang.Thread t, java.lang.Throwable e);
}

}

