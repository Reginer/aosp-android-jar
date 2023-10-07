/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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


@SuppressWarnings({"unchecked", "deprecation", "all"})
public final class Inet6Address extends java.net.InetAddress {

Inet6Address() { throw new RuntimeException("Stub!"); }

public static java.net.Inet6Address getByAddress(java.lang.String host, byte[] addr, java.net.NetworkInterface nif) throws java.net.UnknownHostException { throw new RuntimeException("Stub!"); }

public static java.net.Inet6Address getByAddress(java.lang.String host, byte[] addr, int scope_id) throws java.net.UnknownHostException { throw new RuntimeException("Stub!"); }

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

public byte[] getAddress() { throw new RuntimeException("Stub!"); }

public int getScopeId() { throw new RuntimeException("Stub!"); }

public java.net.NetworkInterface getScopedInterface() { throw new RuntimeException("Stub!"); }

public java.lang.String getHostAddress() { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

public boolean equals(java.lang.Object obj) { throw new RuntimeException("Stub!"); }

public boolean isIPv4CompatibleAddress() { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public static final java.net.InetAddress ANY;
static { ANY = null; }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public static final java.net.InetAddress LOOPBACK;
static { LOOPBACK = null; }
}
