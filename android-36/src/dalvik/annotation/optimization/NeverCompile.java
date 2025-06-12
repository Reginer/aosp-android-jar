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

package dalvik.annotation.optimization;

import static android.annotation.SystemApi.Client.MODULE_LIBRARIES;

import android.annotation.SystemApi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an API should never be compiled.
 *
 * <p>
 * NeverCompile can be used to annotate methods that should not be compiled and included in .odex
 * files. Methods that are not called frequently, are never speed-critical, or are only used for
 * debugging do not necessarily need to run quickly. Applying this annotation to prevent these
 * methods from being compiled will return some size improvements in the .odex file that they would
 * otherwise be included in.
 * </p>
 *
 * <p>
 * This annotation will have no effect when applied to native methods, as JNI stubs will still be
 * compiled. In addition, it will not stop overriding methods from being compiled, so applying this
 * annotation to abstract methods will not do anything.
 * </p>
 *
 * <p>
 * The <code>dumpPackageLPr</code> method in com.android.server.pm can be used as a concrete
 * example. This is a debug method used to dump all of the information about a device's installed
 * packages. When it is compiled, it is included in services.odex. Annotating this method with
 * NeverCompile can be seen to reduce the size of services.odex by roughly 28KB.
 * </p>
 * @hide
 */
@SystemApi(client = MODULE_LIBRARIES)
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface NeverCompile {}
