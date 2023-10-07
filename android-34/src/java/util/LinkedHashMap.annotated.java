/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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


package java.util;


@SuppressWarnings({"unchecked", "deprecation", "all"})
public class LinkedHashMap<K, V> extends java.util.HashMap<K,V> implements java.util.Map<K,V> {

public LinkedHashMap(int initialCapacity, float loadFactor) { throw new RuntimeException("Stub!"); }

public LinkedHashMap(int initialCapacity) { throw new RuntimeException("Stub!"); }

public LinkedHashMap() { throw new RuntimeException("Stub!"); }

public LinkedHashMap(java.util.Map<? extends K, ? extends V> m) { throw new RuntimeException("Stub!"); }

public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) { throw new RuntimeException("Stub!"); }

public boolean containsValue(java.lang.Object value) { throw new RuntimeException("Stub!"); }

public V get(java.lang.Object key) { throw new RuntimeException("Stub!"); }

public V getOrDefault(java.lang.Object key, V defaultValue) { throw new RuntimeException("Stub!"); }

public void clear() { throw new RuntimeException("Stub!"); }

@android.annotation.SystemApi(client = android.annotation.SystemApi.Client.MODULE_LIBRARIES)
public java.util.Map.Entry<K,V> eldest() { throw new RuntimeException("Stub!"); }

protected boolean removeEldestEntry(java.util.Map.Entry<K,V> eldest) { throw new RuntimeException("Stub!"); }

public java.util.Set<K> keySet() { throw new RuntimeException("Stub!"); }

public java.util.Collection<V> values() { throw new RuntimeException("Stub!"); }

public java.util.Set<java.util.Map.Entry<K,V>> entrySet() { throw new RuntimeException("Stub!"); }

public void forEach(java.util.function.BiConsumer<? super K,? super V> action) { throw new RuntimeException("Stub!"); }

public void replaceAll(java.util.function.BiFunction<? super K,? super V,? extends V> function) { throw new RuntimeException("Stub!"); }
}

