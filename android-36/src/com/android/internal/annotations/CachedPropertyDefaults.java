/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.internal.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation that generates default values for {@link CachedProperty} within class.
 * This annotation is rquired on class level when intending to use {@link CachedProperty}
 * annotation within that class.
 *
 * <p>To use, annotate the class with {@code @CachedPropertyDefaults}. By default it has a maximum
 * capacity of 4 and stores in the "system_server" module
 * {@link android.os.IpcDataCache.MODULE_SYSTEM}. Both parameters can be overwritten and will be
 * used as default for each property inside of annotated class, eg:
 * {@code @CachedPropertyDefaults(module = "my_custom_module", max=32)}
 *
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface CachedPropertyDefaults {
  /**
   * The module name under which the {@link android.os.IpcDataCache} will be registered, by default it is
   * "system_server".
   */
  String module() default "system_server";

  /**
   * The default number of entries in the {@link android.os.IpcDataCache}.
   */
  int max() default 32;
}
