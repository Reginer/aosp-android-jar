/*
 * Copyright (c) 1998, 2011, Oracle and/or its affiliates. All rights reserved.
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
public interface SortedMap<K, V> extends java.util.Map<K,V> {

@libcore.util.Nullable public java.util.Comparator<? super @libcore.util.NullFromTypeParam K> comparator();

@libcore.util.NonNull public java.util.SortedMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> subMap(@libcore.util.NullFromTypeParam K fromKey, @libcore.util.NullFromTypeParam K toKey);

@libcore.util.NonNull public java.util.SortedMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> headMap(@libcore.util.NullFromTypeParam K toKey);

@libcore.util.NonNull public java.util.SortedMap<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V> tailMap(@libcore.util.NullFromTypeParam K fromKey);

@libcore.util.NullFromTypeParam public K firstKey();

@libcore.util.NullFromTypeParam public K lastKey();

@libcore.util.NonNull public java.util.Set<@libcore.util.NullFromTypeParam K> keySet();

@libcore.util.NonNull public java.util.Collection<@libcore.util.NullFromTypeParam V> values();

@libcore.util.NonNull public java.util.Set<java.util.Map.@libcore.util.NonNull Entry<@libcore.util.NullFromTypeParam K, @libcore.util.NullFromTypeParam V>> entrySet();
}
