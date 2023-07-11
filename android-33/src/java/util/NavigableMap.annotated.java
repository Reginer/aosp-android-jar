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
 * Written by Doug Lea and Josh Bloch with assistance from members of JCP
 * JSR-166 Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */


package java.util;


@SuppressWarnings({"unchecked", "deprecation", "all"})
public interface NavigableMap<K, V> extends java.util.SortedMap<K,V> {

@libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> lowerEntry(@libcore.util.NullFromTypeParam K key);

@libcore.util.Nullable public K lowerKey(@libcore.util.NullFromTypeParam K key);

@libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> floorEntry(@libcore.util.NullFromTypeParam K key);

@libcore.util.Nullable public K floorKey(@libcore.util.NullFromTypeParam K key);

@libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> ceilingEntry(@libcore.util.NullFromTypeParam K key);

@libcore.util.Nullable public K ceilingKey(@libcore.util.NullFromTypeParam K key);

@libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> higherEntry(@libcore.util.NullFromTypeParam K key);

@libcore.util.Nullable public K higherKey(@libcore.util.NullFromTypeParam K key);

@libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> firstEntry();

@libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> lastEntry();

@libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> pollFirstEntry();

@libcore.util.Nullable public java.util.Map.Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> pollLastEntry();

@libcore.util.NonNull public java.util.NavigableMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> descendingMap();

@libcore.util.NonNull public java.util.NavigableSet<@libcore.util.NullFromTypeParam K> navigableKeySet();

@libcore.util.NonNull public java.util.NavigableSet<@libcore.util.NullFromTypeParam K> descendingKeySet();

@libcore.util.NonNull public java.util.NavigableMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> subMap(@libcore.util.NullFromTypeParam K fromKey, boolean fromInclusive, @libcore.util.NullFromTypeParam K toKey, boolean toInclusive);

@libcore.util.NonNull public java.util.NavigableMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> headMap(@libcore.util.NullFromTypeParam K toKey, boolean inclusive);

@libcore.util.NonNull public java.util.NavigableMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> tailMap(@libcore.util.NullFromTypeParam K fromKey, boolean inclusive);

@libcore.util.NonNull public java.util.SortedMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> subMap(@libcore.util.NullFromTypeParam K fromKey, @libcore.util.NullFromTypeParam K toKey);

@libcore.util.NonNull public java.util.SortedMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> headMap(@libcore.util.NullFromTypeParam K toKey);

@libcore.util.NonNull public java.util.SortedMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> tailMap(@libcore.util.NullFromTypeParam K fromKey);
}
