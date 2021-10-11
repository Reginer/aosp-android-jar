/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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
public class TreeMap<K, V> extends java.util.AbstractMap<K,V> implements java.util.NavigableMap<K,V>, java.lang.Cloneable, java.io.Serializable {

public TreeMap() { throw new RuntimeException("Stub!"); }

public TreeMap(@libcore.util.Nullable java.util.Comparator<? super @libcore.util.NullFromTypeParam K> comparator) { throw new RuntimeException("Stub!"); }

public TreeMap(@libcore.util.NonNull java.util.Map<? extends @libcore.util.NullFromTypeParam K,? extends @libcore.util.NullFromTypeParam V> m) { throw new RuntimeException("Stub!"); }

public TreeMap(@libcore.util.NonNull java.util.SortedMap<@libcore.util.NullFromTypeParam K,? extends @libcore.util.NullFromTypeParam V> m) { throw new RuntimeException("Stub!"); }

public int size() { throw new RuntimeException("Stub!"); }

public boolean containsKey(@libcore.util.Nullable java.lang.Object key) { throw new RuntimeException("Stub!"); }

public boolean containsValue(@libcore.util.Nullable java.lang.Object value) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public V get(@libcore.util.Nullable java.lang.Object key) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.util.Comparator<? super @libcore.util.NullFromTypeParam K> comparator() { throw new RuntimeException("Stub!"); }

@libcore.util.NullFromTypeParam public K firstKey() { throw new RuntimeException("Stub!"); }

@libcore.util.NullFromTypeParam public K lastKey() { throw new RuntimeException("Stub!"); }

public void putAll(@libcore.util.NonNull java.util.Map<? extends @libcore.util.NullFromTypeParam K,? extends @libcore.util.NullFromTypeParam V> map) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public V put(@libcore.util.NullFromTypeParam K key, @libcore.util.NullFromTypeParam V value) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public V remove(@libcore.util.Nullable java.lang.Object key) { throw new RuntimeException("Stub!"); }

public void clear() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.Object clone() { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> firstEntry() { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> lastEntry() { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> pollFirstEntry() { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> pollLastEntry() { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> lowerEntry(@libcore.util.NullFromTypeParam K key) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public K lowerKey(@libcore.util.NullFromTypeParam K key) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> floorEntry(@libcore.util.NullFromTypeParam K key) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public K floorKey(@libcore.util.NullFromTypeParam K key) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> ceilingEntry(@libcore.util.NullFromTypeParam K key) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public K ceilingKey(@libcore.util.NullFromTypeParam K key) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> higherEntry(@libcore.util.NullFromTypeParam K key) { throw new RuntimeException("Stub!"); }

@libcore.util.Nullable public K higherKey(@libcore.util.NullFromTypeParam K key) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Set<@libcore.util.NullFromTypeParam K> keySet() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.NavigableSet<@libcore.util.NullFromTypeParam K> navigableKeySet() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.NavigableSet<@libcore.util.NullFromTypeParam K> descendingKeySet() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Collection<@libcore.util.NullFromTypeParam V> values() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Set<java.util.Map.@libcore.util.NonNull Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V>> entrySet() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.NavigableMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> descendingMap() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.NavigableMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> subMap(@libcore.util.NullFromTypeParam K fromKey, boolean fromInclusive, @libcore.util.NullFromTypeParam K toKey, boolean toInclusive) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.NavigableMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> headMap(@libcore.util.NullFromTypeParam K toKey, boolean inclusive) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.NavigableMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> tailMap(@libcore.util.NullFromTypeParam K fromKey, boolean inclusive) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.SortedMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> subMap(@libcore.util.NullFromTypeParam K fromKey, @libcore.util.NullFromTypeParam K toKey) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.SortedMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> headMap(@libcore.util.NullFromTypeParam K toKey) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.SortedMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> tailMap(@libcore.util.NullFromTypeParam K fromKey) { throw new RuntimeException("Stub!"); }

public boolean replace(@libcore.util.NullFromTypeParam K key, @libcore.util.Nullable V oldValue, @libcore.util.NullFromTypeParam V newValue) { throw new RuntimeException("Stub!"); }

@libcore.util.NullFromTypeParam public V replace(@libcore.util.NullFromTypeParam K key, @libcore.util.NullFromTypeParam V value) { throw new RuntimeException("Stub!"); }

public void forEach(@libcore.util.NonNull java.util.function.BiConsumer<? super @libcore.util.NullFromTypeParam K,? super @libcore.util.NullFromTypeParam V> action) { throw new RuntimeException("Stub!"); }

public void replaceAll(@libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NullFromTypeParam K,? super @libcore.util.NullFromTypeParam V,? extends @libcore.util.NullFromTypeParam V> function) { throw new RuntimeException("Stub!"); }
}
