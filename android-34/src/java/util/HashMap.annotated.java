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
public class HashMap<K, V> extends java.util.AbstractMap<K,V> implements java.util.Map<K,V>, java.lang.Cloneable, java.io.Serializable {

public HashMap(int initialCapacity, float loadFactor) { throw new RuntimeException("Stub!"); }

public HashMap(int initialCapacity) { throw new RuntimeException("Stub!"); }

public HashMap() { throw new RuntimeException("Stub!"); }

public HashMap(@libcore.util.NonNull java.util.Map<? extends @libcore.util.NullFromTypeParam K, ? extends @libcore.util.NullFromTypeParam V> m) { throw new RuntimeException("Stub!"); }

public int size() { throw new RuntimeException("Stub!"); }

public boolean isEmpty() { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public V get(@libcore.util.Nullable java.lang.Object key) { throw new RuntimeException("Stub!"); }

public boolean containsKey(@libcore.util.Nullable java.lang.Object key) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public V put(@libcore.util.NullFromTypeParam K key, @libcore.util.NullFromTypeParam V value) { throw new RuntimeException("Stub!"); }

public void putAll(@libcore.util.NonNull java.util.Map<? extends @libcore.util.NullFromTypeParam K,? extends @libcore.util.NullFromTypeParam V> m) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public V remove(@libcore.util.Nullable java.lang.Object key) { throw new RuntimeException("Stub!"); }

public void clear() { throw new RuntimeException("Stub!"); }

public boolean containsValue(@libcore.util.Nullable java.lang.Object value) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Set<@libcore.util.NullFromTypeParam K> keySet() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Collection<@libcore.util.NullFromTypeParam V> values() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Set<java.util.Map.@libcore.util.NonNull Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V>> entrySet() { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public V getOrDefault(@libcore.util.Nullable java.lang.Object key, @libcore.util.Nullable V defaultValue) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public V putIfAbsent(@libcore.util.NullFromTypeParam K key, @libcore.util.NullFromTypeParam V value) { throw new RuntimeException("Stub!"); }

public boolean remove(@libcore.util.Nullable java.lang.Object key, @libcore.util.Nullable java.lang.Object value) { throw new RuntimeException("Stub!"); }

public boolean replace(@libcore.util.NullFromTypeParam K key, @libcore.util.Nullable V oldValue, @libcore.util.NullFromTypeParam V newValue) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public V replace(@libcore.util.NullFromTypeParam K key, @libcore.util.NullFromTypeParam V value) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public V computeIfAbsent(@libcore.util.NullFromTypeParam K key, @libcore.util.NonNull java.util.function.Function<? super @libcore.util.NullFromTypeParam K,? extends @libcore.util.Nullable V> mappingFunction) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public V computeIfPresent(@libcore.util.NullFromTypeParam K key, @libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NullFromTypeParam K,? super @libcore.util.NonNull V,? extends @libcore.util.Nullable V> remappingFunction) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public V compute(@libcore.util.NullFromTypeParam K key, @libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NullFromTypeParam K,? super @libcore.util.Nullable V,? extends @libcore.util.Nullable V> remappingFunction) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public V merge(@libcore.util.NullFromTypeParam K key, @libcore.util.NonNull V value, @libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NonNull V,? super @libcore.util.NonNull V,? extends @libcore.util.Nullable V> remappingFunction) { throw new RuntimeException("Stub!"); }

public void forEach(@libcore.util.NonNull java.util.function.BiConsumer<? super @libcore.util.NullFromTypeParam K,? super @libcore.util.NullFromTypeParam V> action) { throw new RuntimeException("Stub!"); }

public void replaceAll(@libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NullFromTypeParam K,? super @libcore.util.NullFromTypeParam V,? extends @libcore.util.NullFromTypeParam V> function) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.Object clone() { throw new RuntimeException("Stub!"); }
}
