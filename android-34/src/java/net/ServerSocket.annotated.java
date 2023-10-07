/*
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

import java.nio.channels.ServerSocketChannel;
import java.io.IOException;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class ServerSocket implements java.io.Closeable {

public ServerSocket() throws java.io.IOException { throw new RuntimeException("Stub!"); }

public ServerSocket(int port) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public ServerSocket(int port, int backlog) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public ServerSocket(int port, int backlog, java.net.InetAddress bindAddr) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public void bind(java.net.SocketAddress endpoint) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public void bind(java.net.SocketAddress endpoint, int backlog) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public java.net.InetAddress getInetAddress() { throw new RuntimeException("Stub!"); }

public int getLocalPort() { throw new RuntimeException("Stub!"); }

public java.net.SocketAddress getLocalSocketAddress() { throw new RuntimeException("Stub!"); }

public java.net.Socket accept() throws java.io.IOException { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public SocketImpl getImpl() throws SocketException { throw new RuntimeException("Stub!"); }

protected final void implAccept(java.net.Socket s) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public void close() throws java.io.IOException { throw new RuntimeException("Stub!"); }

public java.nio.channels.ServerSocketChannel getChannel() { throw new RuntimeException("Stub!"); }

public boolean isBound() { throw new RuntimeException("Stub!"); }

public boolean isClosed() { throw new RuntimeException("Stub!"); }

public synchronized void setSoTimeout(int timeout) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized int getSoTimeout() throws java.io.IOException { throw new RuntimeException("Stub!"); }

public void setReuseAddress(boolean on) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public boolean getReuseAddress() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public java.lang.String toString() { throw new RuntimeException("Stub!"); }

public static synchronized void setSocketFactory(java.net.SocketImplFactory fac) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public synchronized void setReceiveBufferSize(int size) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized int getReceiveBufferSize() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) { throw new RuntimeException("Stub!"); }
}

