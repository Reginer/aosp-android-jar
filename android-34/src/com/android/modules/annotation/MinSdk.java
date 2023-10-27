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
package com.android.modules.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates than an API is only supported on platform versions of at least the given value and
 * later.
 *
 * Currently, this annotations is purely informational. The exact meaning of this annotation depends
 * on it's context:
 * <ul>
 *     <li>On a public SDK method, it means that the member should only be included in the SDK of
 *     the given version or later</li>
 *     <li>On a module API, it will mean that it should only be called when running on a device with
 *     the given SDK or later</li>
 * </ul>
 *
 * In future, the annotation will acquire further semantics:
 * <ul>
 *     <li>Classes annotated with this will only be classloaded on devices running the given SDK
 *     version or later.</li>
 *     <li>It will be used to ensure API safety at build time in the context of a codebase with
 *     different parts having different min SDK versions.</li>
 * </ul>
 *
 * This annotation should only be used on code that exports an API (either public SDK or
 * {@code @SystemApi}. For code that just calls APIs that only exist on newer platform versions
 * use {@code androidx.annotation.RequiresApi} instead.
 */
@Retention(CLASS)
@Target({CONSTRUCTOR, METHOD, FIELD, TYPE})
public @interface MinSdk {
    int value();
}