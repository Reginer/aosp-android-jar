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

import java.lang.reflect.Array;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class Collections {

Collections() { throw new RuntimeException("Stub!"); }

public static <T extends java.lang.Comparable<? super T>> void sort(@libcore.util.NonNull java.util.List<@libcore.util.NonNull T> list) { throw new RuntimeException("Stub!"); }

public static <T> void sort(@libcore.util.NonNull java.util.List<@libcore.util.NullFromTypeParam T> list, @libcore.util.Nullable java.util.Comparator<? super @libcore.util.NullFromTypeParam T> c) { throw new RuntimeException("Stub!"); }

public static <T> int binarySearch(@libcore.util.NonNull java.util.List<? extends @libcore.util.NonNull java.lang.Comparable<? super T>> list, @libcore.util.NonNull T key) { throw new RuntimeException("Stub!"); }

public static <T> int binarySearch(@libcore.util.NonNull java.util.List<? extends @libcore.util.NullFromTypeParam T> list, @libcore.util.NullFromTypeParam T key, @libcore.util.Nullable java.util.Comparator<? super @libcore.util.NullFromTypeParam T> c) { throw new RuntimeException("Stub!"); }

public static void reverse(@libcore.util.NonNull java.util.List<?> list) { throw new RuntimeException("Stub!"); }

public static void shuffle(@libcore.util.NonNull java.util.List<?> list) { throw new RuntimeException("Stub!"); }

public static void shuffle(@libcore.util.NonNull java.util.List<?> list, @libcore.util.NonNull java.util.Random rnd) { throw new RuntimeException("Stub!"); }

public static void swap(@libcore.util.NonNull java.util.List<?> list, int i, int j) { throw new RuntimeException("Stub!"); }

public static <T> void fill(@libcore.util.NonNull java.util.List<? super @libcore.util.NullFromTypeParam T> list, @libcore.util.NullFromTypeParam T obj) { throw new RuntimeException("Stub!"); }

public static <T> void copy(@libcore.util.NonNull java.util.List<? super @libcore.util.NullFromTypeParam T> dest, @libcore.util.NonNull java.util.List<? extends @libcore.util.NullFromTypeParam T> src) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T extends java.lang.Object & java.lang.Comparable<? super T>> T min(@libcore.util.NonNull java.util.Collection<? extends @libcore.util.NonNull T> coll) { throw new RuntimeException("Stub!"); }

@libcore.util.NullFromTypeParam public static <T> T min(@libcore.util.NonNull java.util.Collection<? extends @libcore.util.NullFromTypeParam T> coll, @libcore.util.Nullable java.util.Comparator<? super @libcore.util.NullFromTypeParam T> comp) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T extends java.lang.Object & java.lang.Comparable<? super T>> T max(@libcore.util.NonNull java.util.Collection<? extends @libcore.util.NonNull T> coll) { throw new RuntimeException("Stub!"); }

@libcore.util.NullFromTypeParam public static <T> T max(@libcore.util.NonNull java.util.Collection<? extends @libcore.util.NullFromTypeParam T> coll, @libcore.util.Nullable java.util.Comparator<? super @libcore.util.NullFromTypeParam T> comp) { throw new RuntimeException("Stub!"); }

public static void rotate(@libcore.util.NonNull java.util.List<?> list, int distance) { throw new RuntimeException("Stub!"); }

public static <T> boolean replaceAll(@libcore.util.NonNull java.util.List<@libcore.util.NullFromTypeParam T> list, @libcore.util.NullFromTypeParam T oldVal, @libcore.util.NullFromTypeParam T newVal) { throw new RuntimeException("Stub!"); }

public static int indexOfSubList(@libcore.util.NonNull java.util.List<?> source, @libcore.util.NonNull java.util.List<?> target) { throw new RuntimeException("Stub!"); }

public static int lastIndexOfSubList(@libcore.util.NonNull java.util.List<?> source, @libcore.util.NonNull java.util.List<?> target) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.Collection<@libcore.util.NullFromTypeParam T> unmodifiableCollection(@libcore.util.NonNull java.util.Collection<? extends @libcore.util.NullFromTypeParam T> c) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.Set<@libcore.util.NullFromTypeParam T> unmodifiableSet(@libcore.util.NonNull java.util.Set<? extends @libcore.util.NullFromTypeParam T> s) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.SortedSet<@libcore.util.NullFromTypeParam T> unmodifiableSortedSet(@libcore.util.NonNull java.util.SortedSet<@libcore.util.NullFromTypeParam T> s) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.NavigableSet<@libcore.util.NullFromTypeParam T> unmodifiableNavigableSet(@libcore.util.NonNull java.util.NavigableSet<@libcore.util.NullFromTypeParam T> s) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.List<@libcore.util.NullFromTypeParam T> unmodifiableList(@libcore.util.NonNull java.util.List<? extends @libcore.util.NullFromTypeParam T> list) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <K, V> java.util.Map<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> unmodifiableMap(@libcore.util.NonNull java.util.Map<? extends @libcore.util.NullFromTypeParam K, ? extends @libcore.util.NullFromTypeParam V> m) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <K, V> java.util.SortedMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> unmodifiableSortedMap(@libcore.util.NonNull java.util.SortedMap<@libcore.util.NullFromTypeParam K, ? extends @libcore.util.NullFromTypeParam V> m) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <K, V> java.util.NavigableMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> unmodifiableNavigableMap(@libcore.util.NonNull java.util.NavigableMap<@libcore.util.NullFromTypeParam K, ? extends @libcore.util.NullFromTypeParam  V> m) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.Collection<@libcore.util.NullFromTypeParam T> synchronizedCollection(@libcore.util.NonNull java.util.Collection<@libcore.util.NullFromTypeParam T> c) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.Set<@libcore.util.NullFromTypeParam T> synchronizedSet(@libcore.util.NonNull java.util.Set<@libcore.util.NullFromTypeParam T> s) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.SortedSet<@libcore.util.NullFromTypeParam T> synchronizedSortedSet(@libcore.util.NonNull java.util.SortedSet<@libcore.util.NullFromTypeParam T> s) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.NavigableSet<@libcore.util.NullFromTypeParam T> synchronizedNavigableSet(@libcore.util.NonNull java.util.NavigableSet<@libcore.util.NullFromTypeParam T> s) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.List<@libcore.util.NullFromTypeParam T> synchronizedList(@libcore.util.NonNull java.util.List<@libcore.util.NullFromTypeParam T> list) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <K, V> java.util.Map<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> synchronizedMap(@libcore.util.NonNull java.util.Map<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> m) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <K, V> java.util.SortedMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> synchronizedSortedMap(@libcore.util.NonNull java.util.SortedMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> m) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <K, V> java.util.NavigableMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> synchronizedNavigableMap(@libcore.util.NonNull java.util.NavigableMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> m) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <E> java.util.Collection<@libcore.util.NullFromTypeParam E> checkedCollection(@libcore.util.NonNull java.util.Collection<@libcore.util.NullFromTypeParam E> c, @libcore.util.NonNull java.lang.Class<E> type) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <E> java.util.Queue<@libcore.util.NullFromTypeParam E> checkedQueue(@libcore.util.NonNull java.util.Queue<@libcore.util.NullFromTypeParam E> queue, @libcore.util.NonNull java.lang.Class<E> type) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <E> java.util.Set<@libcore.util.NullFromTypeParam E> checkedSet(@libcore.util.NonNull java.util.Set<@libcore.util.NullFromTypeParam E> s, @libcore.util.NonNull java.lang.Class<E> type) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <E> java.util.SortedSet<@libcore.util.NullFromTypeParam E> checkedSortedSet(@libcore.util.NonNull java.util.SortedSet<@libcore.util.NullFromTypeParam E> s, @libcore.util.NonNull java.lang.Class<E> type) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <E> java.util.NavigableSet<@libcore.util.NullFromTypeParam E> checkedNavigableSet(@libcore.util.NonNull java.util.NavigableSet<@libcore.util.NullFromTypeParam E> s, @libcore.util.NonNull java.lang.Class<E> type) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <E> java.util.List<@libcore.util.NullFromTypeParam E> checkedList(@libcore.util.NonNull java.util.List<@libcore.util.NullFromTypeParam E> list, @libcore.util.NonNull java.lang.Class<E> type) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <K, V> java.util.Map<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> checkedMap(@libcore.util.NonNull java.util.Map<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> m, @libcore.util.NonNull java.lang.Class<K> keyType, @libcore.util.NonNull java.lang.Class<V> valueType) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <K, V> java.util.SortedMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> checkedSortedMap(@libcore.util.NonNull java.util.SortedMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> m, @libcore.util.NonNull java.lang.Class<K> keyType, @libcore.util.NonNull java.lang.Class<V> valueType) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <K, V> java.util.NavigableMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> checkedNavigableMap(@libcore.util.NonNull java.util.NavigableMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> m, @libcore.util.NonNull java.lang.Class<K> keyType, @libcore.util.NonNull java.lang.Class<V> valueType) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.Iterator<@libcore.util.NullFromTypeParam T> emptyIterator() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.ListIterator<@libcore.util.NullFromTypeParam T> emptyListIterator() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.Enumeration<@libcore.util.NullFromTypeParam T> emptyEnumeration() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static final <T> java.util.Set<@libcore.util.NullFromTypeParam T> emptySet() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <E> java.util.SortedSet<@libcore.util.NullFromTypeParam E> emptySortedSet() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <E> java.util.NavigableSet<@libcore.util.NullFromTypeParam E> emptyNavigableSet() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static final <T> java.util.List<@libcore.util.NullFromTypeParam T> emptyList() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static final <K, V> java.util.Map<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> emptyMap() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static final <K, V> java.util.SortedMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> emptySortedMap() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static final <K, V> java.util.NavigableMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> emptyNavigableMap() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.Set<@libcore.util.NullFromTypeParam T> singleton(@libcore.util.NullFromTypeParam T o) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.List<@libcore.util.NullFromTypeParam T> singletonList(@libcore.util.NullFromTypeParam T o) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <K, V> java.util.Map<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> singletonMap(@libcore.util.NullFromTypeParam K key, @libcore.util.NullFromTypeParam V value) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.List<@libcore.util.NullFromTypeParam T> nCopies(int n, @libcore.util.NullFromTypeParam T o) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.Comparator<@libcore.util.NonNull T> reverseOrder() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.Comparator<@libcore.util.NullFromTypeParam T> reverseOrder(@libcore.util.Nullable java.util.Comparator<@libcore.util.NullFromTypeParam T> cmp) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.Enumeration<@libcore.util.NullFromTypeParam T> enumeration(@libcore.util.NonNull java.util.Collection<@libcore.util.NullFromTypeParam T> c) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.ArrayList<@libcore.util.NullFromTypeParam T> list(@libcore.util.NonNull java.util.Enumeration<@libcore.util.NullFromTypeParam T> e) { throw new RuntimeException("Stub!"); }

public static int frequency(@libcore.util.NonNull java.util.Collection<?> c, @libcore.util.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

public static boolean disjoint(@libcore.util.NonNull java.util.Collection<?> c1, @libcore.util.NonNull java.util.Collection<?> c2) { throw new RuntimeException("Stub!"); }

@java.lang.SafeVarargs
public static <T> boolean addAll(@libcore.util.NonNull java.util.Collection<? super @libcore.util.NullFromTypeParam T> c, T @libcore.util.NonNull ... elements) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <E> java.util.Set<@libcore.util.NullFromTypeParam E> newSetFromMap(@libcore.util.NonNull java.util.Map<@libcore.util.NullFromTypeParam E, @libcore.util.NonNull java.lang.Boolean> map) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static <T> java.util.Queue<@libcore.util.NullFromTypeParam T> asLifoQueue(@libcore.util.NonNull java.util.Deque<@libcore.util.NullFromTypeParam T> deque) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public static final java.util.List EMPTY_LIST;
static { EMPTY_LIST = null; }

@libcore.util.NonNull public static final java.util.Map EMPTY_MAP;
static { EMPTY_MAP = null; }

@libcore.util.NonNull public static final java.util.Set EMPTY_SET;
static { EMPTY_SET = null; }
}
