/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
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


package java.net;

import java.io.IOException;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public abstract class SocketImpl implements java.net.SocketOptions {

public SocketImpl() { throw new RuntimeException("Stub!"); }

protected abstract void create(boolean stream) throws java.io.IOException;

protected abstract void connect(java.lang.String host, int port) throws java.io.IOException;

protected abstract void connect(java.net.InetAddress address, int port) throws java.io.IOException;

protected abstract void connect(java.net.SocketAddress address, int timeout) throws java.io.IOException;

protected abstract void bind(java.net.InetAddress host, int port) throws java.io.IOException;

protected abstract void listen(int backlog) throws java.io.IOException;

protected abstract void accept(java.net.SocketImpl s) throws java.io.IOException;

protected abstract java.io.InputStream getInputStream() throws java.io.IOException;

protected abstract java.io.OutputStream getOutputStream() throws java.io.IOException;

protected abstract int available() throws java.io.IOException;

protected abstract void close() throws java.io.IOException;

protected void shutdownInput() throws java.io.IOException { throw new RuntimeException("Stub!"); }

protected void shutdownOutput() throws java.io.IOException { throw new RuntimeException("Stub!"); }

protected java.io.FileDescriptor getFileDescriptor() { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public java.io.FileDescriptor getFD$() { throw new RuntimeException("Stub!"); }

protected java.net.InetAddress getInetAddress() { throw new RuntimeException("Stub!"); }

protected int getPort() { throw new RuntimeException("Stub!"); }

protected boolean supportsUrgentData() { throw new RuntimeException("Stub!"); }

protected abstract void sendUrgentData(int data) throws java.io.IOException;

protected int getLocalPort() { throw new RuntimeException("Stub!"); }

public java.lang.String toString() { throw new RuntimeException("Stub!"); }

protected void setPerformancePreferences(int connectionTime, int latency, int bandwidth) { throw new RuntimeException("Stub!"); }

protected java.net.InetAddress address;

protected java.io.FileDescriptor fd;

protected int localport;

protected int port;
}

