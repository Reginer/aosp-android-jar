/*
 * Copyright (c) 1995, 2011, Oracle and/or its affiliates. All rights reserved.
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


package java.io;


@SuppressWarnings({"unchecked", "deprecation", "all"})
public final class FileDescriptor {

public FileDescriptor() { throw new RuntimeException("Stub!"); }

public boolean valid() { throw new RuntimeException("Stub!"); }

public native void sync() throws java.io.SyncFailedException;

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public int getInt$() { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public void setInt$(int fd) { throw new RuntimeException("Stub!"); }

public long getOwnerId$() { throw new RuntimeException("Stub!"); }

public void setOwnerId$(long newOwnerId) { throw new RuntimeException("Stub!"); }

public java.io.FileDescriptor release$() { throw new RuntimeException("Stub!"); }

public boolean isSocket$() { throw new RuntimeException("Stub!"); }

public static final long NO_OWNER = 0L; // 0x0L

public static final java.io.FileDescriptor err;
static { err = null; }

public static final java.io.FileDescriptor in;
static { in = null; }

public static final java.io.FileDescriptor out;
static { out = null; }
}

