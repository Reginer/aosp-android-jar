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

import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class Socket implements java.io.Closeable {

public Socket() { throw new RuntimeException("Stub!"); }

public Socket(java.net.Proxy proxy) { throw new RuntimeException("Stub!"); }

protected Socket(java.net.SocketImpl impl) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public Socket(java.lang.String host, int port) throws java.io.IOException, java.net.UnknownHostException { throw new RuntimeException("Stub!"); }

public Socket(java.net.InetAddress address, int port) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public Socket(java.lang.String host, int port, java.net.InetAddress localAddr, int localPort) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public Socket(java.net.InetAddress address, int port, java.net.InetAddress localAddr, int localPort) throws java.io.IOException { throw new RuntimeException("Stub!"); }

@Deprecated
public Socket(java.lang.String host, int port, boolean stream) throws java.io.IOException { throw new RuntimeException("Stub!"); }

@Deprecated
public Socket(java.net.InetAddress host, int port, boolean stream) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public void connect(java.net.SocketAddress endpoint) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public void connect(java.net.SocketAddress endpoint, int timeout) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public void bind(java.net.SocketAddress bindpoint) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public java.net.InetAddress getInetAddress() { throw new RuntimeException("Stub!"); }

public java.net.InetAddress getLocalAddress() { throw new RuntimeException("Stub!"); }

public int getPort() { throw new RuntimeException("Stub!"); }

public int getLocalPort() { throw new RuntimeException("Stub!"); }

public java.net.SocketAddress getRemoteSocketAddress() { throw new RuntimeException("Stub!"); }

public java.net.SocketAddress getLocalSocketAddress() { throw new RuntimeException("Stub!"); }

public java.nio.channels.SocketChannel getChannel() { throw new RuntimeException("Stub!"); }

public java.io.InputStream getInputStream() throws java.io.IOException { throw new RuntimeException("Stub!"); }

public java.io.OutputStream getOutputStream() throws java.io.IOException { throw new RuntimeException("Stub!"); }

public void setTcpNoDelay(boolean on) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public boolean getTcpNoDelay() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public void setSoLinger(boolean on, int linger) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public int getSoLinger() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public void sendUrgentData(int data) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public void setOOBInline(boolean on) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public boolean getOOBInline() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized void setSoTimeout(int timeout) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized int getSoTimeout() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized void setSendBufferSize(int size) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized int getSendBufferSize() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized void setReceiveBufferSize(int size) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized int getReceiveBufferSize() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public void setKeepAlive(boolean on) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public boolean getKeepAlive() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public void setTrafficClass(int tc) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public int getTrafficClass() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public void setReuseAddress(boolean on) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public boolean getReuseAddress() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized void close() throws java.io.IOException { throw new RuntimeException("Stub!"); }

public void shutdownInput() throws java.io.IOException { throw new RuntimeException("Stub!"); }

public void shutdownOutput() throws java.io.IOException { throw new RuntimeException("Stub!"); }

public java.lang.String toString() { throw new RuntimeException("Stub!"); }

public boolean isConnected() { throw new RuntimeException("Stub!"); }

public boolean isBound() { throw new RuntimeException("Stub!"); }

public boolean isClosed() { throw new RuntimeException("Stub!"); }

public boolean isInputShutdown() { throw new RuntimeException("Stub!"); }

public boolean isOutputShutdown() { throw new RuntimeException("Stub!"); }

public static synchronized void setSocketImplFactory(java.net.SocketImplFactory fac) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
@libcore.api.IntraCoreApi
public java.io.FileDescriptor getFileDescriptor$() { throw new RuntimeException("Stub!"); }
}

