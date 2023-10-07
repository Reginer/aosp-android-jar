/*
 * Copyright (c) 1997, 2012, Oracle and/or its affiliates. All rights reserved.
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
public abstract class AbstractList<E> extends java.util.AbstractCollection<E> implements java.util.List<E> {

protected AbstractList() { throw new RuntimeException("Stub!"); }

public boolean add(@libcore.util.NullFromTypeParam E e) { throw new RuntimeException("Stub!"); }

@libcore.util.NullFromTypeParam public abstract E get(int index);

@libcore.util.NullFromTypeParam public E set(int index, @libcore.util.NullFromTypeParam E element) { throw new RuntimeException("Stub!"); }

public void add(int index, @libcore.util.NullFromTypeParam E element) { throw new RuntimeException("Stub!"); }

@libcore.util.NullFromTypeParam public E remove(int index) { throw new RuntimeException("Stub!"); }

public int indexOf(@libcore.util.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

public int lastIndexOf(@libcore.util.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

public void clear() { throw new RuntimeException("Stub!"); }

public boolean addAll(int index, @libcore.util.NonNull java.util.Collection<? extends @libcore.util.NullFromTypeParam E> c) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.Iterator<@libcore.util.NullFromTypeParam E> iterator() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.ListIterator<@libcore.util.NullFromTypeParam E> listIterator() { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.ListIterator<@libcore.util.NullFromTypeParam E> listIterator(int index) { throw new RuntimeException("Stub!"); }

@libcore.util.NonNull public java.util.List<@libcore.util.NullFromTypeParam E> subList(int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

public boolean equals(@libcore.util.Nullable java.lang.Object o) { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

protected void removeRange(int fromIndex, int toIndex) { throw new RuntimeException("Stub!"); }

protected transient int modCount = 0; // 0x0
}
