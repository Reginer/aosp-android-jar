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
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group.  Adapted and released, under explicit permission,
 * from JDK ArrayList.java which carries the following copyright:
 *
 * Copyright 1997 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 */


package java.util.concurrent;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Collection;
import java.util.Spliterator;
import java.util.AbstractList;

@SuppressWarnings({"unchecked", "deprecation", "all"})
public class CopyOnWriteArrayList<E> implements java.util.List<E>, java.util.RandomAccess, java.lang.Cloneable, java.io.Serializable {

public CopyOnWriteArrayList() { throw new RuntimeException("Stub!"); }

public CopyOnWriteArrayList(@libcore.util.NonNull java.util.Collection<? extends @libcore.util.NullFromTypeParam E> c) { throw new RuntimeException("Stub!"); }

public CopyOnWriteArrayList(E @libcore.util.NonNull [] toCopyIn) { throw new RuntimeException("Stub!"); }

public int size() { throw new RuntimeException("Stub!"); }

public boolean isEmpty() { throw new RuntimeException("Stub!"); }

public boolean contains(@libcore.util.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

public int indexOf(@libcore.util.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

public int indexOf(@libcore.util.Nullable E e, int index) { throw new RuntimeException("Stub!"); }

public int lastIndexOf(@libcore.util.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

public int lastIndexOf(@libcore.util.Nullable E e, int index) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.Object clone() { throw new RuntimeException("Stub!"); }

public java.lang.@libcore.util.Nullable Object @libcore.util.NonNull [] toArray() { throw new RuntimeException("Stub!"); }

public <T> T @libcore.util.NonNull [] toArray(T @libcore.util.NonNull [] a) { throw new RuntimeException("Stub!"); }

@libcore.util.NullFromTypeParam public E get(int index) { throw new RuntimeException("Stub!"); }

@libcore.util.NullFromTypeParam public E set(int index, @libcore.util.NullFromTypeParam E element) { throw new RuntimeException("Stub!"); }

public boolean add(@libcore.util.NullFromTypeParam E e) { throw new RuntimeException("Stub!"); }

public void add(int index, @libcore.util.NullFromTypeParam E element) { throw new RuntimeException("Stub!"); }

@libcore.util.NullFromTypeParam public E remove(int index) { throw new RuntimeException("Stub!"); }

public boolean remove(@libcore.util.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

public boolean addIfAbsent(@libcore.util.NullFromTypeParam E e) { throw new RuntimeException("Stub!"); }

public boolean containsAll(@libcore.util.NonNull java.util.Collection<?> c) { throw new RuntimeException("Stub!"); }

public boolean removeAll(@libcore.util.NonNull java.util.Collection<?> c) { throw new RuntimeException("Stub!"); }

public boolean retainAll(@libcore.util.NonNull java.util.Collection<?> c) { throw new RuntimeException("Stub!"); }

public int addAllAbsent(@libcore.util.NonNull java.util.Collection<? extends @libcore.util.NullFromTypeParam E> c) { throw new RuntimeException("Stub!"); }

public void clear() { throw new RuntimeException("Stub!"); }

public boolean addAll(@libcore.util.NonNull java.util.Collection<? extends @libcore.util.NullFromTypeParam E> c) { throw new RuntimeException("Stub!"); }

public boolean addAll(int index, @libcore.util.NonNull java.util.Collection<? extends @libcore.util.NullFromTypeParam E> c) { throw new RuntimeException("Stub!"); }

public void forEach(@libcore.util.NonNull java.util.function.Consumer<? super @libcore.util.NullFromTypeParam E> action) { throw new RuntimeException("Stub!"); }

public boolean removeIf(@libcore.util.NonNull java.util.function.Predicate<? super @libcore.util.NullFromTypeParam E> filter) { throw new RuntimeException("Stub!"); }

public void replaceAll(@libcore.util.NonNull java.util.function.UnaryOperator<@libcore.util.NullFromTypeParam E> operator) { throw new RuntimeException("Stub!"); }

public void sort(@libcore.util.Nullable java.util.Comparator<? super @libcore.util.NullFromTypeParam E> c) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.lang.String toString() { throw new RuntimeException("Stub!"); }

public boolean equals(@libcore.util.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Iterator<@libcore.util.NullFromTypeParam E> iterator() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.ListIterator<@libcore.util.NullFromTypeParam E> listIterator() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.ListIterator<@libcore.util.NullFromTypeParam E> listIterator(int index) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Spliterator<@libcore.util.NullFromTypeParam E> spliterator() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.List<@libcore.util.NullFromTypeParam E> subList(int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }
}
