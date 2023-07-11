/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates an API that uses {@code context.getUser} or {@code context.getUserId}
 * to operate across users (as the user associated with the context)
 * <p>
 * To create a {@link android.content.Context} associated with a different user,
 *  use {@link android.content.Context#createContextAsUser} or
 *  {@link android.content.Context#createPackageContextAsUser}
 * <p>
 * Example:
 * <pre>{@code
 * {@literal @}UserHandleAware
 * public abstract PackageInfo getPackageInfo({@literal @}NonNull String packageName,
 *      {@literal @}PackageInfoFlags int flags) throws NameNotFoundException;
 * }</pre>
 *
 * @memberDoc This method uses {@linkplain android.content.Context#getUser}
 *            or {@linkplain android.content.Context#getUserId} to execute across users.
 * @hide
 */
@Retention(SOURCE)
@Target({TYPE, METHOD, CONSTRUCTOR, PACKAGE})
public @interface UserHandleAware {

    /**
     * Specifies the SDK version at which this method became {@literal @}UserHandleAware,
     * if it was not always so.
     *
     * Prior to this level, the method is not considered {@literal @}UserHandleAware and therefore
     * uses the {@link android.os#myUserHandle() calling user},
     * not the {@link android.content.Context#getUser context user}.
     *
     * Note that when an API marked with this parameter is run on a device whose OS predates the
     * stated version, the calling user will be used, since on such a
     * device, the API is not {@literal @}UserHandleAware yet.
     */
    int enabledSinceTargetSdkVersion() default 0;

    /**
     * Specifies the permission name required
     * if the context user differs from the calling user.
     *
     * This requirement is in addition to any specified by
     * {@link android.annotation.RequiresPermission}.
     *
     * @see android.annotation.RequiresPermission#value()
     */
    String requiresPermissionIfNotCaller() default "";

    /**
     * Specifies a list of permission names where at least one is required
     * if the context user differs from the calling user.
     *
     * This requirement is in addition to any specified by
     * {@link android.annotation.RequiresPermission}.
     *
     * @see android.annotation.RequiresPermission#anyOf()
     */
    String[] requiresAnyOfPermissionsIfNotCaller() default {};

    /**
     * Specifies a list of permission names where at least one is required if the context
     * user is not in the same profile group as the calling user.
     *
     * This requirement is in addition to any specified by
     * {@link android.annotation.RequiresPermission}.
     *
     * @see android.annotation.RequiresPermission#anyOf()
     */
    String[] requiresAnyOfPermissionsIfNotCallerProfileGroup() default {};
}
