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


package java.io;

import java.nio.channels.FileChannel;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class FileInputStream extends java.io.InputStream {

public FileInputStream(java.lang.String name) throws java.io.FileNotFoundException { throw new RuntimeException("Stub!"); }

public FileInputStream(java.io.File file) throws java.io.FileNotFoundException { throw new RuntimeException("Stub!"); }

public FileInputStream(java.io.FileDescriptor fdObj) { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public FileInputStream(java.io.FileDescriptor fdObj, boolean isFdOwner) { throw new RuntimeException("Stub!"); }

public int read() throws java.io.IOException { throw new RuntimeException("Stub!"); }

public int read(byte[] b) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public int read(byte[] b, int off, int len) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public long skip(long n) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public int available() throws java.io.IOException { throw new RuntimeException("Stub!"); }

public void close() throws java.io.IOException { throw new RuntimeException("Stub!"); }

public final java.io.FileDescriptor getFD() throws java.io.IOException { throw new RuntimeException("Stub!"); }

public java.nio.channels.FileChannel getChannel() { throw new RuntimeException("Stub!"); }

protected void finalize() throws java.io.IOException { throw new RuntimeException("Stub!"); }
}

