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

package dalvik.annotation.optimization;

import static android.annotation.SystemApi.Client.MODULE_LIBRARIES;

import android.annotation.SystemApi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an API should never be inlined.
 *
 * <p>
 * NeverInline can be used to annotate methods that should not be inlined into other methods.
 * Methods that are not called frequently, are never speed-critical, or are only used for
 * debugging do not necessarily need to run quickly. Applying this annotation to prevent these
 * methods from being inlined will return some size improvements in .odex files.
 * </p>
 *
 * <p>
 * The <code>fillInStackTrace</code> method in java.lang.Throwable can be used as a concrete
 * example. This is a method that fills in the execution stack trace and it is not used for
 * performance. Annotating this method with NeverInline can be seen to significantly reduce
 * the size of services.odex.
 * </p>
 * @hide
 */
@SystemApi(client = MODULE_LIBRARIES)
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface NeverInline {}
