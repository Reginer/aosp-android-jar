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

import java.util.Map.Entry;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public abstract class AbstractMap<K, V> implements java.util.Map<K,V> {

protected AbstractMap() { throw new RuntimeException("Stub!"); }

public int size() { throw new RuntimeException("Stub!"); }

public boolean isEmpty() { throw new RuntimeException("Stub!"); }

public boolean containsValue(@libcore.util.Nullable java.lang.Object value) { throw new RuntimeException("Stub!"); }

public boolean containsKey(@libcore.util.Nullable java.lang.Object key) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public V get(@libcore.util.Nullable java.lang.Object key) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public V put(@libcore.util.NullFromTypeParam K key, @libcore.util.NullFromTypeParam V value) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public V remove(@libcore.util.Nullable java.lang.Object key) { throw new RuntimeException("Stub!"); }

public void putAll(@libcore.util.NonNull java.util.Map<? extends @libcore.util.NullFromTypeParam K,? extends @libcore.util.NullFromTypeParam V> m) { throw new RuntimeException("Stub!"); }

public void clear() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Set<@libcore.util.NullFromTypeParam K> keySet() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Collection<@libcore.util.NullFromTypeParam V> values() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public abstract java.util.Set<java.util.Map.@libcore.util.NonNull Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V>> entrySet();

public boolean equals(@libcore.util.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull protected java.lang.Object clone() throws java.lang.CloneNotSupportedException { throw new RuntimeException("Stub!"); }

@SuppressWarnings({"unchecked", "deprecation", "all"})
public static class SimpleEntry<K, V> implements java.util.Map.Entry<K,V>, java.io.Serializable {

public SimpleEntry(@libcore.util.NullFromTypeParam K key, @libcore.util.NullFromTypeParam V value) { throw new RuntimeException("Stub!"); }

public SimpleEntry(@libcore.util.NonNull java.util.Map.Entry<? extends @libcore.util.NullFromTypeParam K, ? extends @libcore.util.NullFromTypeParam V> entry) { throw new RuntimeException("Stub!"); }

@libcore.util.NullFromTypeParam public K getKey() { throw new RuntimeException("Stub!"); }

@libcore.util.NullFromTypeParam public V getValue() { throw new RuntimeException("Stub!"); }

@libcore.util.NullFromTypeParam public V setValue(@libcore.util.NullFromTypeParam V value) { throw new RuntimeException("Stub!"); }

public boolean equals(@libcore.util.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }
}

@SuppressWarnings({"unchecked", "deprecation", "all"})
public static class SimpleImmutableEntry<K, V> implements java.util.Map.Entry<K,V>, java.io.Serializable {

public SimpleImmutableEntry(@libcore.util.NullFromTypeParam K key, @libcore.util.NullFromTypeParam V value) { throw new RuntimeException("Stub!"); }

public SimpleImmutableEntry(@libcore.util.NonNull java.util.Map.Entry<? extends @libcore.util.NullFromTypeParam K,? extends @libcore.util.NullFromTypeParam V> entry) { throw new RuntimeException("Stub!"); }

@libcore.util.NullFromTypeParam public K getKey() { throw new RuntimeException("Stub!"); }

@libcore.util.NullFromTypeParam public V getValue() { throw new RuntimeException("Stub!"); }

@libcore.util.NullFromTypeParam public V setValue(@libcore.util.NullFromTypeParam V value) { throw new RuntimeException("Stub!"); }

public boolean equals(@libcore.util.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }
}

}
