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

package com.android.internal.annotations;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes that the annotated field should not be optimized away when
 * the code is minified at build time. This is typically used
 * on write-only members that could otherwise be removed except for downstream
 * weak references that might rely on keep-alive semantics for that field.
 * <p>Example:
 * <pre><code>
 *  &#64;KeepForWeakReference
 *  private final Runnable mFooCallback = this::onFoo;
 *  ....
 *  public void registerFooCallbacks(WeakCallbackRegistry registry) {
 *      registry.register(mFooCallback);
 *  }
 * </code></pre>
 */
@Retention(RetentionPolicy.CLASS)
@Target({FIELD})
public @interface KeepForWeakReference {
}
