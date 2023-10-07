/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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

package java.security;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

// Android-changed: Stubbed the implementation.  Android doesn't support SecurityManager.
// See comments in java.lang.SecurityManager for details.
/**
 * Android doesn't support {@link SecurityManager}. Do not use this class.
 */

public abstract class PermissionCollection implements java.io.Serializable {

    /**
     * Adds a permission object to the current collection of permission objects.
     *
     * @param permission the Permission object to add.
     *
     * @exception SecurityException -  if this PermissionCollection object
     *                                 has been marked readonly
     * @exception IllegalArgumentException - if this PermissionCollection
     *                object is a homogeneous collection and the permission
     *                is not of the correct type.
     */
    public abstract void add(Permission permission);

    /**
     * Checks to see if the specified permission is implied by
     * the collection of Permission objects held in this PermissionCollection.
     *
     * @param permission the Permission object to compare.
     *
     * @return true if "permission" is implied by the  permissions in
     * the collection, false if not.
     */
    public abstract boolean implies(Permission permission);

    /**
     * Returns an enumeration of all the Permission objects in the collection.
     *
     * @return an enumeration of all the Permissions.
     */
    public abstract Enumeration<Permission> elements();

    public void setReadOnly() { }

    public boolean isReadOnly() { return true; }

    /**
     * Returns a stream of all the Permission objects in the collection.
     *
     * <p> The collection should not be modified (see {@link #add}) during the
     * execution of the terminal stream operation. Otherwise, the result of the
     * terminal stream operation is undefined.
     *
     * @implSpec
     * The default implementation creates a stream whose source is derived from
     * the enumeration returned from a call to {@link #elements()}.
     *
     * @return a stream of all the Permissions.
     * @since 9
     *
     * @hide
     */
    public Stream<Permission> elementsAsStream() {
        // Android-changed: Stubbed the implementation.
        /*
        int characteristics = isReadOnly()
                ? Spliterator.NONNULL | Spliterator.IMMUTABLE
                : Spliterator.NONNULL;
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        elements().asIterator(), characteristics),
                false);
        */
        return Stream.empty();
    }

}
