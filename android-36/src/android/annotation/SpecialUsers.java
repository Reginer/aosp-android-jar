/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Contains annotations to indicate whether special {@link android.os.UserHandle} values
 * are permitted for a given {@link android.os.UserHandle} or {@link UserIdInt}.
 *
 * <p>User IDs are typically specified by either a UserHandle or a userId int, which ultimately are
 * whole numbers (0 or larger). There also exist special userId values that correspond to
 * special cases ("all users", "the current user", etc.), which are internally handled using
 * negative integers. Some method parameters, return values, and variables accept such special
 * values, but others do not. This annotation indicates whether they are supported, and which.
 *
 * <p>Example usage:
 * <li><code>
 *     public @CanBeALL @CanBeCURRENT UserHandle myMethod(@CanBeALL @UserIdInt int userId) {}
 * </code>
 *
 * @see android.os.UserHandle#ALL
 * @see android.os.UserHandle#CURRENT
 * @see UserHandleAware#specialUsersAllowed() Specification usage for @UserHandleAware
 *
 * @hide
 */
@Retention(SOURCE)
public @interface SpecialUsers {
    /**
     * Special UserHandle and userId ints corresponding to
     * <li>{@link android.os.UserHandle#ALL} and {@link android.os.UserHandle#USER_ALL}</li>
     * <li>{@link android.os.UserHandle#CURRENT} and {@link android.os.UserHandle#USER_CURRENT}</li>
     * as well as more advanced options (and their negations, catchalls, etc.).
     */
    static enum SpecialUser {
        // Values direct from UserHandle (common)
        USER_ALL, USER_CURRENT,
        // Values direct from UserHandle (less common)
        USER_CURRENT_OR_SELF, USER_NULL,
        // Negation of the UserHandle values
        DISALLOW_USER_ALL, DISALLOW_USER_CURRENT, DISALLOW_USER_CURRENT_OR_SELF, DISALLOW_USER_NULL,
        // Catchall values (caution: needs to remain valid even if more specials are ever added!)
        ALLOW_EVERY, DISALLOW_EVERY,
        // Indication that the answer is as-yet unknown
        UNSPECIFIED;
    }

    /**
     * Indication that a {@link android.os.UserHandle} or {@link UserIdInt} can be
     * {@link android.os.UserHandle#ALL} and {@link android.os.UserHandle#USER_ALL}, respectively.
     */
    @Retention(SOURCE)
    @Target({TYPE, TYPE_USE, FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, ANNOTATION_TYPE})
    @CanBeUsers(specialUsersAllowed = {SpecialUser.USER_ALL})
    public @interface CanBeALL {
    }

    /**
     * Indication that a {@link android.os.UserHandle} or {@link UserIdInt} can be
     * {@link android.os.UserHandle#CURRENT} and {@link android.os.UserHandle#USER_CURRENT},
     * respectively.
     */
    @Retention(SOURCE)
    @Target({TYPE, TYPE_USE, FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, ANNOTATION_TYPE})
    @CanBeUsers(specialUsersAllowed = {SpecialUser.USER_CURRENT})
    public @interface CanBeCURRENT {
    }

    /**
     * Indication that a {@link android.os.UserHandle} or {@link UserIdInt} can be
     * {@link android.os.UserHandle#NULL} and {@link android.os.UserHandle#USER_NULL}, respectively.
     * (This is unrelated to the Java concept of <code>null</code>.)
     */
    @Retention(SOURCE)
    @Target({TYPE, TYPE_USE, FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, ANNOTATION_TYPE})
    @CanBeUsers(specialUsersAllowed = {SpecialUser.USER_NULL})
    public @interface CanBeNULL {
    }

    /**
     * Indication that a {@link android.os.UserHandle} or {@link UserIdInt} cannot take on any
     * special values.
     */
    @Retention(SOURCE)
    @Target({TYPE, TYPE_USE, FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, ANNOTATION_TYPE})
    @CanBeUsers(specialUsersAllowed = {SpecialUser.DISALLOW_EVERY})
    public @interface CannotBeSpecialUser {
    }

    /**
     * Indication that a {@link android.os.UserHandle} or {@link UserIdInt} can take on
     * {@link SpecialUser special values} as specified.
     * <p> For use when simple {@link CanBeALL} and {@link CanBeCURRENT} do not suffice.
     */
    @Retention(SOURCE)
    @Target({TYPE, TYPE_USE, FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, ANNOTATION_TYPE})
    public @interface CanBeUsers {
        /** Specify which types of {@link SpecialUser}s are allowed. For use in advanced cases.  */
        SpecialUser[] specialUsersAllowed() default {SpecialUser.UNSPECIFIED};
    }
}
