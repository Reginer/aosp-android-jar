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

package com.android.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import android.car.builtin.annotation.PlatformVersion;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Defines in which version of platform this method / type / field was added.
 *
 * <p>Annotation should be used for APIs exposed to CarService module by {@code android.car.builtin}
 *
 * @hide
 */
@Retention(RUNTIME)
@Target({ANNOTATION_TYPE, FIELD, TYPE, METHOD})
public @interface AddedIn {
    PlatformVersion value();
}
