/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */


package java.util.concurrent;

import java.util.Set;
import java.util.HashMap;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.Spliterator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Collection;
import java.util.function.Function;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class ConcurrentHashMap<K, V> extends java.util.AbstractMap<@libcore.util.NonNull K, @libcore.util.NonNull V> implements java.util.concurrent.ConcurrentMap<@libcore.util.NonNull K, @libcore.util.NonNull V>, java.io.Serializable {

  public ConcurrentHashMap() { throw new RuntimeException("Stub!"); }

  public ConcurrentHashMap(int initialCapacity) { throw new RuntimeException("Stub!"); }

  public ConcurrentHashMap(@libcore.util.NonNull java.util.Map<? extends @libcore.util.NonNull K,? extends @libcore.util.NonNull V> m) { throw new RuntimeException("Stub!"); }

  public ConcurrentHashMap(int initialCapacity, float loadFactor) { throw new RuntimeException("Stub!"); }

  public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) { throw new RuntimeException("Stub!"); }

  public int size() { throw new RuntimeException("Stub!"); }

  public boolean isEmpty() { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public V get(@libcore.util.NonNull java.lang.Object key) { throw new RuntimeException("Stub!"); }

  public boolean containsKey(@libcore.util.NonNull java.lang.Object key) { throw new RuntimeException("Stub!"); }

  public boolean containsValue(@libcore.util.NonNull java.lang.Object value) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public V put(@libcore.util.NonNull K key, @libcore.util.NonNull V value) { throw new RuntimeException("Stub!"); }

  public void putAll(@libcore.util.NonNull java.util.Map<? extends @libcore.util.NonNull K,? extends @libcore.util.NonNull V> m) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public V remove(@libcore.util.NonNull java.lang.Object key) { throw new RuntimeException("Stub!"); }

  public void clear() { throw new RuntimeException("Stub!"); }

  @libcore.util.NonNull public java.util.Set<@libcore.util.NonNull K> keySet() { throw new RuntimeException("Stub!"); }

  @libcore.util.NonNull public java.util.Collection<@libcore.util.NonNull V> values() { throw new RuntimeException("Stub!"); }

  @libcore.util.NonNull public java.util.Set<java.util.Map.@libcore.util.NonNull Entry<@libcore.util.NonNull K, @libcore.util.NonNull V>> entrySet() { throw new RuntimeException("Stub!"); }

  public int hashCode() { throw new RuntimeException("Stub!"); }

  @libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }

  public boolean equals(@libcore.util.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public V putIfAbsent(@libcore.util.NonNull K key, @libcore.util.NonNull V value) { throw new RuntimeException("Stub!"); }

  public boolean remove(@libcore.util.NonNull java.lang.Object key, @libcore.util.Nullable java.lang.Object value) { throw new RuntimeException("Stub!"); }

  public boolean replace(@libcore.util.NonNull K key, @libcore.util.NonNull V oldValue, @libcore.util.NonNull V newValue) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public V replace(@libcore.util.NonNull K key, @libcore.util.NonNull V value) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public V getOrDefault(@libcore.util.NonNull java.lang.Object key, @libcore.util.Nullable V defaultValue) { throw new RuntimeException("Stub!"); }

  public void forEach(@libcore.util.NonNull java.util.function.BiConsumer<? super @libcore.util.NonNull K,? super @libcore.util.NonNull V> action) { throw new RuntimeException("Stub!"); }

  public void replaceAll(@libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NonNull K,? super @libcore.util.NonNull V,? extends @libcore.util.NonNull V> function) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public V computeIfAbsent(@libcore.util.NonNull K key, @libcore.util.NonNull java.util.function.Function<? super @libcore.util.NonNull K,? extends @libcore.util.Nullable V> mappingFunction) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public V computeIfPresent(@libcore.util.NonNull K key, @libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NonNull K,? super @libcore.util.NonNull V,? extends @libcore.util.Nullable V> remappingFunction) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public V compute(@libcore.util.NonNull K key, @libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NonNull K,? super @libcore.util.Nullable V,? extends @libcore.util.Nullable V> remappingFunction) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public V merge(@libcore.util.NonNull K key, @libcore.util.NonNull V value, @libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NonNull V,? super @libcore.util.NonNull V,? extends @libcore.util.Nullable V> remappingFunction) { throw new RuntimeException("Stub!"); }

  public boolean contains(@libcore.util.NonNull java.lang.Object value) { throw new RuntimeException("Stub!"); }

  @libcore.util.NonNull public java.util.Enumeration<@libcore.util.NonNull K> keys() { throw new RuntimeException("Stub!"); }

  @libcore.util.NonNull public java.util.Enumeration<@libcore.util.NonNull V> elements() { throw new RuntimeException("Stub!"); }

  public long mappingCount() { throw new RuntimeException("Stub!"); }

  @libcore.util.NonNull public static <@libcore.util.NonNull K> java.util.concurrent.ConcurrentHashMap.KeySetView<@libcore.util.NonNull K, @libcore.util.NonNull java.lang.Boolean> newKeySet() { throw new RuntimeException("Stub!"); }

  @libcore.util.NonNull public static <@libcore.util.NonNull K> java.util.concurrent.ConcurrentHashMap.KeySetView<@libcore.util.NonNull K, @libcore.util.NonNull java.lang.Boolean> newKeySet(int initialCapacity) { throw new RuntimeException("Stub!"); }

  @libcore.util.NonNull public java.util.concurrent.ConcurrentHashMap.KeySetView<@libcore.util.NonNull K, @libcore.util.NonNull V> keySet(@libcore.util.NonNull V mappedValue) { throw new RuntimeException("Stub!"); }

  public void forEach(long parallelismThreshold, @libcore.util.NonNull java.util.function.BiConsumer<? super @libcore.util.NonNull K,? super @libcore.util.NonNull V> action) { throw new RuntimeException("Stub!"); }

  public <U> void forEach(long parallelismThreshold, @libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NonNull K,? super @libcore.util.NonNull V,? extends @libcore.util.Nullable U> transformer, @libcore.util.NonNull java.util.function.Consumer<? super @libcore.util.NonNull U> action) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public <U> U search(long parallelismThreshold, @libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NonNull K,? super @libcore.util.NonNull V,? extends @libcore.util.Nullable U> searchFunction) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public <U> U reduce(long parallelismThreshold, @libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NonNull K,? super @libcore.util.NonNull V,? extends @libcore.util.Nullable U> transformer, @libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NonNull U,? super @libcore.util.NonNull U,? extends @libcore.util.Nullable U> reducer) { throw new RuntimeException("Stub!"); }

  public double reduceToDouble(long parallelismThreshold, @libcore.util.NonNull java.util.function.ToDoubleBiFunction<? super @libcore.util.NonNull K,? super @libcore.util.NonNull V> transformer, double basis, @libcore.util.NonNull java.util.function.DoubleBinaryOperator reducer) { throw new RuntimeException("Stub!"); }

  public long reduceToLong(long parallelismThreshold, @libcore.util.NonNull java.util.function.ToLongBiFunction<? super @libcore.util.NonNull K,? super @libcore.util.NonNull V> transformer, long basis, @libcore.util.NonNull java.util.function.LongBinaryOperator reducer) { throw new RuntimeException("Stub!"); }

  public int reduceToInt(long parallelismThreshold, @libcore.util.NonNull java.util.function.ToIntBiFunction<? super @libcore.util.NonNull K,? super @libcore.util.NonNull V> transformer, int basis, @libcore.util.NonNull java.util.function.IntBinaryOperator reducer) { throw new RuntimeException("Stub!"); }

  public void forEachKey(long parallelismThreshold, @libcore.util.NonNull java.util.function.Consumer<? super @libcore.util.NonNull K> action) { throw new RuntimeException("Stub!"); }

  public <U> void forEachKey(long parallelismThreshold, @libcore.util.NonNull java.util.function.Function<? super @libcore.util.NonNull K,? extends @libcore.util.Nullable U> transformer, @libcore.util.NonNull java.util.function.Consumer<? super @libcore.util.NonNull U> action) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public <U> U searchKeys(long parallelismThreshold, @libcore.util.NonNull java.util.function.Function<? super @libcore.util.NonNull K,? extends @libcore.util.Nullable U> searchFunction) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public K reduceKeys(long parallelismThreshold, @libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NonNull K,? super @libcore.util.NonNull K,? extends @libcore.util.Nullable K> reducer) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public <U> U reduceKeys(long parallelismThreshold, @libcore.util.NonNull java.util.function.Function<? super @libcore.util.NonNull K,? extends @libcore.util.Nullable U> transformer, @libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NonNull U,? super @libcore.util.NonNull U,? extends @libcore.util.Nullable U> reducer) { throw new RuntimeException("Stub!"); }

  public double reduceKeysToDouble(long parallelismThreshold, @libcore.util.NonNull java.util.function.ToDoubleFunction<? super @libcore.util.NonNull K> transformer, double basis, @libcore.util.NonNull java.util.function.DoubleBinaryOperator reducer) { throw new RuntimeException("Stub!"); }

  public long reduceKeysToLong(long parallelismThreshold, @libcore.util.NonNull java.util.function.ToLongFunction<? super @libcore.util.NonNull K> transformer, long basis, @libcore.util.NonNull java.util.function.LongBinaryOperator reducer) { throw new RuntimeException("Stub!"); }

  public int reduceKeysToInt(long parallelismThreshold, @libcore.util.NonNull java.util.function.ToIntFunction<? super @libcore.util.NonNull K> transformer, int basis, @libcore.util.NonNull java.util.function.IntBinaryOperator reducer) { throw new RuntimeException("Stub!"); }

  public void forEachValue(long parallelismThreshold, @libcore.util.NonNull java.util.function.Consumer<? super @libcore.util.NonNull V> action) { throw new RuntimeException("Stub!"); }

  public <U> void forEachValue(long parallelismThreshold, @libcore.util.NonNull java.util.function.Function<? super @libcore.util.NonNull V,? extends @libcore.util.Nullable U> transformer, @libcore.util.NonNull java.util.function.Consumer<? super @libcore.util.NonNull U> action) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public <U> U searchValues(long parallelismThreshold, @libcore.util.NonNull java.util.function.Function<? super @libcore.util.NonNull V,? extends @libcore.util.Nullable U> searchFunction) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public V reduceValues(long parallelismThreshold, @libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NonNull V,? super @libcore.util.NonNull V,? extends @libcore.util.Nullable V> reducer) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public <U> U reduceValues(long parallelismThreshold, @libcore.util.NonNull java.util.function.Function<? super @libcore.util.NonNull V,? extends @libcore.util.Nullable U> transformer, @libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NonNull U,? super @libcore.util.NonNull U,? extends @libcore.util.Nullable U> reducer) { throw new RuntimeException("Stub!"); }

  public double reduceValuesToDouble(long parallelismThreshold, @libcore.util.NonNull java.util.function.ToDoubleFunction<? super @libcore.util.NonNull V> transformer, double basis, @libcore.util.NonNull java.util.function.DoubleBinaryOperator reducer) { throw new RuntimeException("Stub!"); }

  public long reduceValuesToLong(long parallelismThreshold, @libcore.util.NonNull java.util.function.ToLongFunction<? super @libcore.util.NonNull V> transformer, long basis, @libcore.util.NonNull java.util.function.LongBinaryOperator reducer) { throw new RuntimeException("Stub!"); }

  public int reduceValuesToInt(long parallelismThreshold, @libcore.util.NonNull java.util.function.ToIntFunction<? super @libcore.util.NonNull V> transformer, int basis, @libcore.util.NonNull java.util.function.IntBinaryOperator reducer) { throw new RuntimeException("Stub!"); }

  public void forEachEntry(long parallelismThreshold, @libcore.util.NonNull java.util.function.Consumer<? super java.util.Map.@libcore.util.NonNull Entry<@libcore.util.NonNull K, @libcore.util.NonNull V>> action) { throw new RuntimeException("Stub!"); }

  public <U> void forEachEntry(long parallelismThreshold, @libcore.util.NonNull java.util.function.Function<java.util.Map.@libcore.util.NonNull Entry<@libcore.util.NonNull K, @libcore.util.NonNull V>,? extends @libcore.util.Nullable U> transformer, @libcore.util.NonNull java.util.function.Consumer<? super @libcore.util.NonNull U> action) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public <U> U searchEntries(long parallelismThreshold, @libcore.util.NonNull java.util.function.Function<java.util.Map.@libcore.util.NonNull Entry<@libcore.util.NonNull K, @libcore.util.NonNull V>,? extends @libcore.util.Nullable U> searchFunction) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NonNull K, @libcore.util.NonNull V> reduceEntries(long parallelismThreshold, @libcore.util.NonNull java.util.function.BiFunction<java.util.Map.@libcore.util.NonNull Entry<@libcore.util.NonNull K, @libcore.util.NonNull V>, java.util.Map.@libcore.util.NonNull Entry<@libcore.util.NonNull K, @libcore.util.NonNull V>,? extends java.util.Map.@libcore.util.Nullable Entry<@libcore.util.NonNull K, @libcore.util.NonNull V>> reducer) { throw new RuntimeException("Stub!"); }

  @libcore.util.Nullable public <U> U reduceEntries(long parallelismThreshold, @libcore.util.NonNull java.util.function.Function<java.util.Map.@libcore.util.NonNull Entry<@libcore.util.NonNull K, @libcore.util.NonNull V>,? extends @libcore.util.Nullable U> transformer, @libcore.util.NonNull java.util.function.BiFunction<? super @libcore.util.NonNull U,? super @libcore.util.NonNull U,? extends @libcore.util.Nullable U> reducer) { throw new RuntimeException("Stub!"); }

  public double reduceEntriesToDouble(long parallelismThreshold, @libcore.util.NonNull java.util.function.ToDoubleFunction<java.util.Map.@libcore.util.NonNull Entry<@libcore.util.NonNull K, @libcore.util.NonNull V>> transformer, double basis, @libcore.util.NonNull java.util.function.DoubleBinaryOperator reducer) { throw new RuntimeException("Stub!"); }

  public long reduceEntriesToLong(long parallelismThreshold, @libcore.util.NonNull java.util.function.ToLongFunction<java.util.Map.@libcore.util.NonNull Entry<@libcore.util.NonNull K, @libcore.util.NonNull V>> transformer, long basis, @libcore.util.NonNull java.util.function.LongBinaryOperator reducer) { throw new RuntimeException("Stub!"); }

  public int reduceEntriesToInt(long parallelismThreshold, @libcore.util.NonNull java.util.function.ToIntFunction<java.util.Map.@libcore.util.NonNull Entry<@libcore.util.NonNull K, @libcore.util.NonNull V>> transformer, int basis, @libcore.util.NonNull java.util.function.IntBinaryOperator reducer) { throw new RuntimeException("Stub!"); }

  @SuppressWarnings({"unchecked", "deprecation", "all"})
  public static class KeySetView<K, V> implements java.util.Collection<K>, java.io.Serializable, java.util.Set<K> {

    KeySetView(@libcore.util.NonNull java.util.concurrent.ConcurrentHashMap<K,V> map, @libcore.util.Nullable V value) { throw new RuntimeException("Stub!"); }

    @libcore.util.Nullable public V getMappedValue() { throw new RuntimeException("Stub!"); }

    public boolean contains(@libcore.util.NonNull java.lang.Object o) { throw new RuntimeException("Stub!"); }

    public boolean remove(@libcore.util.NonNull java.lang.Object o) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public java.util.Iterator<@libcore.util.NonNull K> iterator() { throw new RuntimeException("Stub!"); }

    public boolean add(@libcore.util.NonNull K e) { throw new RuntimeException("Stub!"); }

    public boolean addAll(@libcore.util.NonNull java.util.Collection<? extends @libcore.util.NonNull K> c) { throw new RuntimeException("Stub!"); }

    public int hashCode() { throw new RuntimeException("Stub!"); }

    public boolean equals(@libcore.util.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public java.util.Spliterator<@libcore.util.NonNull K> spliterator() { throw new RuntimeException("Stub!"); }

    public void forEach(@libcore.util.NonNull java.util.function.Consumer<? super @libcore.util.NonNull K> action) { throw new RuntimeException("Stub!"); }

    public final boolean removeAll(@libcore.util.NonNull java.util.Collection<?> c) { throw new RuntimeException("Stub!"); }

    public final int size() { throw new RuntimeException("Stub!"); }

    public final boolean containsAll(@libcore.util.NonNull java.util.Collection<?> c) { throw new RuntimeException("Stub!"); }

    public final void clear() { throw new RuntimeException("Stub!"); }

    public final boolean isEmpty() { throw new RuntimeException("Stub!"); }

    public final java.lang.@libcore.util.NonNull Object @libcore.util.NonNull [] toArray() { throw new RuntimeException("Stub!"); }

    public final <T> T @libcore.util.NonNull [] toArray(T @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public final java.lang.String toString() { throw new RuntimeException("Stub!"); }

    @libcore.util.NonNull public java.util.concurrent.ConcurrentHashMap<@libcore.util.NonNull K, @libcore.util.NonNull V> getMap() { throw new RuntimeException("Stub!"); }

    public final boolean retainAll(@libcore.util.NonNull java.util.Collection<?> c) { throw new RuntimeException("Stub!"); }
  }

}
