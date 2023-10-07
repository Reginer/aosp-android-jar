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

import java.nio.channels.DatagramChannel;
import java.io.IOException;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class DatagramSocket implements java.io.Closeable {

public DatagramSocket() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

protected DatagramSocket(java.net.DatagramSocketImpl impl) { throw new RuntimeException("Stub!"); }

public DatagramSocket(java.net.SocketAddress bindaddr) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public DatagramSocket(int port) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public DatagramSocket(int port, java.net.InetAddress laddr) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized void bind(java.net.SocketAddress addr) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public void connect(java.net.InetAddress address, int port) { throw new RuntimeException("Stub!"); }

public void connect(java.net.SocketAddress addr) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public void disconnect() { throw new RuntimeException("Stub!"); }

public boolean isBound() { throw new RuntimeException("Stub!"); }

public boolean isConnected() { throw new RuntimeException("Stub!"); }

public java.net.InetAddress getInetAddress() { throw new RuntimeException("Stub!"); }

public int getPort() { throw new RuntimeException("Stub!"); }

public java.net.SocketAddress getRemoteSocketAddress() { throw new RuntimeException("Stub!"); }

public java.net.SocketAddress getLocalSocketAddress() { throw new RuntimeException("Stub!"); }

public void send(java.net.DatagramPacket p) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public synchronized void receive(java.net.DatagramPacket p) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public java.net.InetAddress getLocalAddress() { throw new RuntimeException("Stub!"); }

public int getLocalPort() { throw new RuntimeException("Stub!"); }

public synchronized void setSoTimeout(int timeout) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized int getSoTimeout() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized void setSendBufferSize(int size) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized int getSendBufferSize() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized void setReceiveBufferSize(int size) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized int getReceiveBufferSize() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized void setReuseAddress(boolean on) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized boolean getReuseAddress() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized void setBroadcast(boolean on) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized boolean getBroadcast() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized void setTrafficClass(int tc) throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public synchronized int getTrafficClass() throws java.net.SocketException { throw new RuntimeException("Stub!"); }

public void close() { throw new RuntimeException("Stub!"); }

public boolean isClosed() { throw new RuntimeException("Stub!"); }

public java.nio.channels.DatagramChannel getChannel() { throw new RuntimeException("Stub!"); }

public static synchronized void setDatagramSocketImplFactory(java.net.DatagramSocketImplFactory fac) throws java.io.IOException { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public java.io.FileDescriptor getFileDescriptor$() { throw new RuntimeException("Stub!"); }
}

