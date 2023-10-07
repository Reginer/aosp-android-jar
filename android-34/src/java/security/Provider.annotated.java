/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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


package java.security;

import java.io.*;
import java.util.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.security.Security;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public abstract class Provider extends java.util.Properties {

protected Provider(java.lang.String name, double version, java.lang.String info) { throw new RuntimeException("Stub!"); }

public java.lang.String getName() { throw new RuntimeException("Stub!"); }

public double getVersion() { throw new RuntimeException("Stub!"); }

public java.lang.String getInfo() { throw new RuntimeException("Stub!"); }

public java.lang.String toString() { throw new RuntimeException("Stub!"); }

public synchronized void clear() { throw new RuntimeException("Stub!"); }

public synchronized void load(java.io.InputStream inStream) throws java.io.IOException { throw new RuntimeException("Stub!"); }

public synchronized void putAll(java.util.Map<?,?> t) { throw new RuntimeException("Stub!"); }

public synchronized java.util.Set<java.util.Map.Entry<java.lang.Object,java.lang.Object>> entrySet() { throw new RuntimeException("Stub!"); }

public java.util.Set<java.lang.Object> keySet() { throw new RuntimeException("Stub!"); }

public java.util.Collection<java.lang.Object> values() { throw new RuntimeException("Stub!"); }

public synchronized java.lang.Object put(java.lang.Object key, java.lang.Object value) { throw new RuntimeException("Stub!"); }

public synchronized java.lang.Object putIfAbsent(java.lang.Object key, java.lang.Object value) { throw new RuntimeException("Stub!"); }

public synchronized java.lang.Object remove(java.lang.Object key) { throw new RuntimeException("Stub!"); }

public synchronized boolean remove(java.lang.Object key, java.lang.Object value) { throw new RuntimeException("Stub!"); }

public synchronized boolean replace(java.lang.Object key, java.lang.Object oldValue, java.lang.Object newValue) { throw new RuntimeException("Stub!"); }

public synchronized java.lang.Object replace(java.lang.Object key, java.lang.Object value) { throw new RuntimeException("Stub!"); }

public synchronized void replaceAll(java.util.function.BiFunction<? super java.lang.Object,? super java.lang.Object,?> function) { throw new RuntimeException("Stub!"); }

public synchronized java.lang.Object compute(java.lang.Object key, java.util.function.BiFunction<? super java.lang.Object,? super java.lang.Object,?> remappingFunction) { throw new RuntimeException("Stub!"); }

public synchronized java.lang.Object computeIfAbsent(java.lang.Object key, java.util.function.Function<? super java.lang.Object,?> mappingFunction) { throw new RuntimeException("Stub!"); }

public synchronized java.lang.Object computeIfPresent(java.lang.Object key, java.util.function.BiFunction<? super java.lang.Object,? super java.lang.Object,?> remappingFunction) { throw new RuntimeException("Stub!"); }

public synchronized java.lang.Object merge(java.lang.Object key, java.lang.Object value, java.util.function.BiFunction<? super java.lang.Object,? super java.lang.Object,?> remappingFunction) { throw new RuntimeException("Stub!"); }

public java.lang.Object get(java.lang.Object key) { throw new RuntimeException("Stub!"); }

public synchronized java.lang.Object getOrDefault(java.lang.Object key, java.lang.Object defaultValue) { throw new RuntimeException("Stub!"); }

public synchronized void forEach(java.util.function.BiConsumer<? super java.lang.Object,? super java.lang.Object> action) { throw new RuntimeException("Stub!"); }

public java.util.Enumeration<java.lang.Object> keys() { throw new RuntimeException("Stub!"); }

public java.util.Enumeration<java.lang.Object> elements() { throw new RuntimeException("Stub!"); }

public java.lang.String getProperty(java.lang.String key) { throw new RuntimeException("Stub!"); }

public synchronized java.security.Provider.Service getService(java.lang.String type, java.lang.String algorithm) { throw new RuntimeException("Stub!"); }

public synchronized java.util.Set<java.security.Provider.Service> getServices() { throw new RuntimeException("Stub!"); }

protected synchronized void putService(java.security.Provider.Service s) { throw new RuntimeException("Stub!"); }

protected synchronized void removeService(java.security.Provider.Service s) { throw new RuntimeException("Stub!"); }

public void setRegistered() { throw new RuntimeException("Stub!"); }

public void setUnregistered() { throw new RuntimeException("Stub!"); }

public boolean isRegistered() { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public synchronized void warmUpServiceProvision() { throw new RuntimeException("Stub!"); }
@SuppressWarnings({"unchecked", "deprecation", "all"})
public static class Service {

public Service(java.security.Provider provider, java.lang.String type, java.lang.String algorithm, java.lang.String className, java.util.List<java.lang.String> aliases, java.util.Map<java.lang.String, java.lang.String> attributes) { throw new RuntimeException("Stub!"); }

public final java.lang.String getType() { throw new RuntimeException("Stub!"); }

public final java.lang.String getAlgorithm() { throw new RuntimeException("Stub!"); }

public final java.security.Provider getProvider() { throw new RuntimeException("Stub!"); }

public final java.lang.String getClassName() { throw new RuntimeException("Stub!"); }

public final java.lang.String getAttribute(java.lang.String name) { throw new RuntimeException("Stub!"); }

public java.lang.Object newInstance(java.lang.Object constructorParameter) throws java.security.NoSuchAlgorithmException { throw new RuntimeException("Stub!"); }

public boolean supportsParameter(java.lang.Object parameter) { throw new RuntimeException("Stub!"); }

public java.lang.String toString() { throw new RuntimeException("Stub!"); }
}

}

