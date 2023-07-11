/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javax.net.ssl;

abstract public class HttpsURLConnection extends HttpURLConnection {
    public abstract java.lang.String getCipherSuite();

    public abstract java.security.cert.Certificate [] getLocalCertificates();

    public abstract java.security.cert.Certificate [] getServerCertificates() throws javax.net.ssl.SSLPeerUnverifiedException;

    public java.security.Principal getPeerPrincipal() throws javax.net.ssl.SSLPeerUnverifiedException { throw new RuntimeException("Stub!"); }

    public java.security.Principal getLocalPrincipal() { throw new RuntimeException("Stub!"); }

    public static void setDefaultHostnameVerifier(javax.net.ssl.HostnameVerifier v) { throw new RuntimeException("Stub!"); }

    public static javax.net.ssl.HostnameVerifier getDefaultHostnameVerifier() { throw new RuntimeException("Stub!"); }

    public void setHostnameVerifier(javax.net.ssl.HostnameVerifier v) { throw new RuntimeException("Stub!"); }

    @android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
    public static javax.net.ssl.HostnameVerifier getStrictHostnameVerifier() { throw new RuntimeException("Stub!"); }

    public javax.net.ssl.HostnameVerifier getHostnameVerifier() { throw new RuntimeException("Stub!"); }

    public static void setDefaultSSLSocketFactory(javax.net.ssl.SSLSocketFactory sf) { throw new RuntimeException("Stub!"); }

    public static javax.net.ssl.SSLSocketFactory getDefaultSSLSocketFactory() { throw new RuntimeException("Stub!"); }

    public void setSSLSocketFactory(javax.net.ssl.SSLSocketFactory sf) { throw new RuntimeException("Stub!"); }

    public javax.net.ssl.SSLSocketFactory getSSLSocketFactory() { throw new RuntimeException("Stub!"); }
}
