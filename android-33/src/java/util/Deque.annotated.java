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
 * Written by Doug Lea and Josh Bloch with assistance from members of
 * JCP JSR-166 Expert Group and released to the public domain, as explained
 * at http://creativecommons.org/publicdomain/zero/1.0/
 */


package java.util;


@SuppressWarnings({"unchecked", "deprecation", "all"})
public interface Deque<E> extends java.util.Queue<E> {

public void addFirst(@libcore.util.NullFromTypeParam E e);

public void addLast(@libcore.util.NullFromTypeParam E e);

public boolean offerFirst(@libcore.util.NullFromTypeParam E e);

public boolean offerLast(@libcore.util.NullFromTypeParam E e);

@libcore.util.NullFromTypeParam public E removeFirst();

@libcore.util.NullFromTypeParam public E removeLast();

@libcore.util.Nullable public E pollFirst();

@libcore.util.Nullable public E pollLast();

@libcore.util.NullFromTypeParam public E getFirst();

@libcore.util.NullFromTypeParam public E getLast();

@libcore.util.Nullable public E peekFirst();

@libcore.util.Nullable public E peekLast();

public boolean removeFirstOccurrence(@libcore.util.Nullable java.lang.Object o);

public boolean removeLastOccurrence(@libcore.util.Nullable java.lang.Object o);

public boolean add(@libcore.util.NullFromTypeParam E e);

public boolean offer(@libcore.util.NullFromTypeParam E e);

@libcore.util.NullFromTypeParam public E remove();

@libcore.util.Nullable public E poll();

@libcore.util.NullFromTypeParam public E element();

@libcore.util.Nullable public E peek();

public void push(@libcore.util.NullFromTypeParam E e);

@libcore.util.NullFromTypeParam public E pop();

public boolean remove(@libcore.util.Nullable java.lang.Object o);

public boolean contains(@libcore.util.Nullable java.lang.Object o);

public int size();

@libcore.util.NonNull public java.util.Iterator<@libcore.util.NullFromTypeParam E> iterator();

@libcore.util.NonNull public java.util.Iterator<@libcore.util.NullFromTypeParam E> descendingIterator();
}
