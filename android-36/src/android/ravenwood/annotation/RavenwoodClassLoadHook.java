/*
 * Copyright (C) 2023 The Android Open Source Project
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
package android.ravenwood.annotation;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * THIS ANNOTATION IS EXPERIMENTAL. REACH OUT TO g/ravenwood BEFORE USING IT, OR YOU HAVE ANY
 * QUESTIONS ABOUT IT.
 *
 * Add this with a fully-specified method name (e.g. {@code "com.package.Class.methodName"})
 * of a callback to get a callback at the class load time.
 *
 * The method must be {@code public static} with a single argument that takes {@link Class}.
 *
 * Typically, this is used with {@link #LIBANDROID_LOADING_HOOK}, which will load the necessary
 * native libraries.
 *
 * @hide
 */
@Target({TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface RavenwoodClassLoadHook {
    String value();

    /**
     * Class load hook that loads <code>libandroid_runtime</code>.
     */
    public static String LIBANDROID_LOADING_HOOK
            = "com.android.platform.test.ravenwood.runtimehelper.ClassLoadHook.onClassLoaded";
}
