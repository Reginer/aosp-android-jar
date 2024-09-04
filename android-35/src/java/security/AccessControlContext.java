/*
 * Copyright (C) 2014 The Android Open Source Project
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

import java.util.ArrayList;
import java.util.List;

import sun.security.util.Debug;
import sun.security.util.SecurityConstants;


// Android-changed: Stubbed the implementation.  Android doesn't support SecurityManager.
// See comments in java.lang.SecurityManager for details.
/**
 * Android doesn't support {@link SecurityManager}. Do not use this class.
 */
public final class AccessControlContext {

    public AccessControlContext(ProtectionDomain[] context) {
    }

    public AccessControlContext(AccessControlContext acc,
                                DomainCombiner combiner) {
    }

    /**
     * package private to allow calls from ProtectionDomain without performing
     * the security check for {@linkplain SecurityConstants#CREATE_ACC_PERMISSION}
     * permission
     */
    AccessControlContext(AccessControlContext acc,
                        DomainCombiner combiner,
                        boolean preauthorized) {
    }

    /**
     * package private for AccessController
     *
     * This "argument wrapper" context will be passed as the actual context
     * parameter on an internal doPrivileged() call used in the implementation.
     */
    AccessControlContext(ProtectionDomain caller, DomainCombiner combiner,
        AccessControlContext parent, AccessControlContext context,
        Permission[] perms)
    {
    }


    /**
     * package private constructor for AccessController.getContext()
     */

    AccessControlContext(ProtectionDomain[] context,
                         boolean isPrivileged)
    {
    }

    /**
     * Constructor for JavaSecurityAccess.doIntersectionPrivilege()
     */
    AccessControlContext(ProtectionDomain[] context,
                         AccessControlContext privilegedContext)
    {
    }

    ProtectionDomain[] getContext() {
        return null;
    }

    boolean isPrivileged()
    {
        return false;
    }

    /**
     * get the assigned combiner from the privileged or inherited context
     */
    DomainCombiner getAssignedCombiner() {
        return null;
    }

    public DomainCombiner getDomainCombiner() {
        return null;
    }

    public void checkPermission(Permission perm)
        throws AccessControlException {
    }

    /**
     * Take the stack-based context (this) and combine it with the
     * privileged or inherited context, if need be. Any limited
     * privilege scope is flagged regardless of whether the assigned
     * context comes from an immediately enclosing limited doPrivileged().
     * The limited privilege scope can indirectly flow from the inherited
     * parent thread or an assigned context previously captured by getContext().
     */
    AccessControlContext optimize() {
        return null;
    }

}
