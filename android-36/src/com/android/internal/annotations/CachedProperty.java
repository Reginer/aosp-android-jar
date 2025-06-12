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
 * The annotation that generates boilerplate code required by {@link android.os.IpcDataCache}
 * Instead of implementing IpcDataCache and adding the same code into multiple places within the
 * same class, annotating method with CachedProperty generate the property and making sure it is
 * thread safe if property is defined as static.
 *
 * To use this annotation on method, owning class needs to be annotated with
 * {@link com.android.internal.annotations.CachedPropertyDefaults}
 *
 * <p>Need static IpcDataCache use @CachedProperty() or @CachedProperty(modifiers =
 * {Modifier.STATIC}) in front of a method which calls a binder.
 *
 * <p>Need NON-static IpcDataCache use @CachedProperty(modifiers = {}) in front of a method which
 * calls a binder.
 *
 * <p>Need to change the max capacity of cache or give custom API name use @CachedProperty(
 * modifiers = {}, max = 1, apiName = "my_unique_key") in front of a method which calls a binder.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD})
public @interface CachedProperty {
  /**
   * The module under which the cache is registered {@link android.os.IpcDataCache.Config#module}.
   * There are some well-known modules (such as {@link android.os.IpcDataCache.MODULE_SYSTEM}
   * but any string is permitted. New modules needs to be registered.
   * When the module is empty, then the module will be the same value as defined in
   * CachedPropertyDefaults.
   */
  String module() default "";

  /**
   * The name of the {@link android.os.IpcDataCache.Config#api}
   * When the api is empty, the api name will be the same value as defined in
   * class level annotation {@link com.android.internal.annotations.CachedPropertyDefaults}.
   */
  String api() default "";

  /**
   * The maximum number of entries in the cache {@link android.os.IpcDataCache.Config#maxEntries}
   * When the value is -1, the value will be the same value as defined in
   * class level annotation {@link com.android.internal.annotations.CachedPropertyDefaults}.
   */
  int max() default -1;

  /**
   * Specify modifiers for generating cached property. By default it will be static property.
   * This modifiers will apply when flag is on or does not exist.
   * TODO: Add support for flag modifiers. b/361731022
   */
  CacheModifier[] modsFlagOnOrNone() default { CacheModifier.STATIC };
}

