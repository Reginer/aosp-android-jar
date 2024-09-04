/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes that the annotated method validates permissions manually.
 * <p>
 * This explicit annotation helps distinguish which of states an
 * element may exist in:
 * <ul>
 * <li>Annotated with {@link EnforcePermission}, indicating that an element
 * strictly requires one or more permissions. The verification occurs within
 * the annotated element.
 * <li>Annotated with {@link RequiresNoPermission}, indicating that an element
 * requires no permissions.
 * <li>Annotated with {@link PermissionManuallyEnforced}, indicating that the
 * element requires some kind of permission which cannot be described using the
 * other annotations.
 * </ul>
 *
 * @see EnforcePermission
 * @see RequiresNoPermission
 * @hide
 */
@Retention(CLASS)
@Target({METHOD})
public @interface PermissionManuallyEnforced {
}
