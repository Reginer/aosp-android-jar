/*
 * Copyright (C) 2021 The Android Open Source Project
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
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes that the annotated element enforces one or more permissions.
 * <p/>
 * Example of requiring a single permission:
 * <pre>{@code
 *   {@literal @}EnforcePermission(Manifest.permission.SET_WALLPAPER)
 *   public abstract void setWallpaper(Bitmap bitmap) throws IOException;
 *
 *   {@literal @}RequiresPermission(ACCESS_COARSE_LOCATION)
 *   public abstract Location getLastKnownLocation(String provider);
 * }</pre>
 * Example of requiring at least one permission from a set:
 * <pre>{@code
 *   {@literal @}EnforcePermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
 *   public abstract Location getLastKnownLocation(String provider);
 * }</pre>
 * Example of requiring multiple permissions:
 * <pre>{@code
 *   {@literal @}EnforcePermission(allOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
 *   public abstract Location getLastKnownLocation(String provider);
 * }</pre>
 * <p>
 * This annotation is automatically added to generated code (e.g., aidl
 * interface). It is rarely required to manually add it, unless prompted by a
 * linter.
 * </p>
 *
 * @see RequiresPermission
 * @see RequiresNoPermission
 * @hide
 */
@Retention(CLASS)
@Target({METHOD,TYPE})
public @interface EnforcePermission {
    /**
     * The name of the permission that is required, if precisely one permission
     * is required. If more than one permission is required, specify either
     * {@link #allOf()} or {@link #anyOf()} instead.
     * <p>
     * If specified, {@link #anyOf()} and {@link #allOf()} must both be null.
     */
    String value() default "";

    /**
     * Specifies a list of permission names that are all required.
     * <p>
     * If specified, {@link #anyOf()} and {@link #value()} must both be null.
     */
    String[] allOf() default {};

    /**
     * Specifies a list of permission names where at least one is required
     * <p>
     * If specified, {@link #allOf()} and {@link #value()} must both be null.
     */
    String[] anyOf() default {};
}
