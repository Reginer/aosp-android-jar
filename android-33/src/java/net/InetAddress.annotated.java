/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1995, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ObjectStreamException;
import java.io.IOException;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class InetAddress implements java.io.Serializable {

InetAddress() { throw new RuntimeException("Stub!"); }

public boolean isMulticastAddress() { throw new RuntimeException("Stub!"); }

public boolean isAnyLocalAddress() { throw new RuntimeException("Stub!"); }

public boolean isLoopbackAddress() { throw new RuntimeException("Stub!"); }

public boolean isLinkLocalAddress() { throw new RuntimeException("Stub!"); }

public boolean isSiteLocalAddress() { throw new RuntimeException("Stub!"); }

public boolean isMCGlobal() { throw new RuntimeException("Stub!"); }

public boolean isMCNodeLocal() { throw new RuntimeException("Stub!"); }

public boolean isMCLinkLocal() { throw new RuntimeException("Stub!"); }

public boolean isMCSiteLocal() { throw new RuntimeException("Stub!"); }

public boolean isMCOrgLocal() { throw new RuntimeException("Stub!"); }

public boolean isReachable(int timeout) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public boolean isReachable(java.net.NetworkInterface netif, int ttl, int timeout) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public boolean isReachableByICMP(int timeout) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public java.lang.String getHostName() { throw new RuntimeException("Stub!"); }

public java.lang.String getCanonicalHostName() { throw new RuntimeException("Stub!"); }

public byte[] getAddress() { throw new RuntimeException("Stub!"); }

public java.lang.String getHostAddress() { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

public boolean equals(java.lang.Object obj) { throw new RuntimeException("Stub!"); }

public java.lang.String toString() { throw new RuntimeException("Stub!"); }

public static java.net.InetAddress getByAddress(java.lang.String host, byte[] addr) throws java.net.UnknownHostException { throw new RuntimeException("Stub!"); }

public static java.net.InetAddress getByName(java.lang.String host) throws java.net.UnknownHostException { throw new RuntimeException("Stub!"); }

public static java.net.InetAddress[] getAllByName(java.lang.String host) throws java.net.UnknownHostException { throw new RuntimeException("Stub!"); }

public static java.net.InetAddress getLoopbackAddress() { throw new RuntimeException("Stub!"); }

public static java.net.InetAddress getByAddress(byte[] addr) throws java.net.UnknownHostException { throw new RuntimeException("Stub!"); }

public static java.net.InetAddress getLocalHost() throws java.net.UnknownHostException { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public static boolean isNumeric(java.lang.String address) { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public static java.net.InetAddress parseNumericAddress(java.lang.String numericAddress) { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public static void clearDnsCache() { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public static java.net.InetAddress getByNameOnNet(java.lang.String host, int netId) throws java.net.UnknownHostException { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public static java.net.InetAddress[] getAllByNameOnNet(java.lang.String host, int netId) throws java.net.UnknownHostException { throw new RuntimeException("Stub!"); }
}

