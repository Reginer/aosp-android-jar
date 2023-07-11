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

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public interface Collection<E> extends java.lang.Iterable<E> {

public int size();

public boolean isEmpty();

public boolean contains(@libcore.util.Nullable java.lang.Object o);

@libcore.util.NonNull public java.util.Iterator<@libcore.util.NullFromTypeParam E> iterator();

public java.lang.@libcore.util.Nullable Object @libcore.util.NonNull [] toArray();

public <T> T @libcore.util.NonNull [] toArray(T @libcore.util.NonNull [] a);

@libcore.util.NonNull public default <T> T @libcore.util.NonNull [] toArray(@libcore.util.NonNull java.util.function.IntFunction<@libcore.util.NullFromTypeParam T @libcore.util.NonNull []> g);

public boolean add(@libcore.util.NullFromTypeParam E e);

public boolean remove(@libcore.util.Nullable java.lang.Object o);

public boolean containsAll(@libcore.util.NonNull java.util.Collection<?> c);

public boolean addAll(@libcore.util.NonNull java.util.Collection<? extends @libcore.util.NullFromTypeParam E> c);

public boolean removeAll(@libcore.util.NonNull java.util.Collection<?> c);

public default boolean removeIf(@libcore.util.NonNull java.util.function.Predicate<? super @libcore.util.NullFromTypeParam E> filter) { throw new RuntimeException("Stub!"); }

public boolean retainAll(@libcore.util.NonNull java.util.Collection<?> c);

public void clear();

public boolean equals(@libcore.util.Nullable java.lang.Object o);

public int hashCode();

@libcore.util.NonNull public default java.util.Spliterator<@libcore.util.NullFromTypeParam E> spliterator() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public default java.util.stream.Stream<@libcore.util.NullFromTypeParam E> stream() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public default java.util.stream.Stream<@libcore.util.NullFromTypeParam E> parallelStream() { throw new RuntimeException("Stub!"); }
}
