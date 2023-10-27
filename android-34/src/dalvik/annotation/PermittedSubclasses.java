/*
 * Copyright (C) 2016 The Android Open Source Project
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

package dalvik.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Parameter;

/**
 * Records the classes and interfaces that are authorized to directly extend or implement the
 * current class or interface.
 *
 * <p>The annotation is allowed only on classes that are not final and can be used at most once for
 * each of these classes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@interface PermittedSubclasses {

    /*
     * This annotation is never used in source code; it is expected to be generated in .dex
     * files by tools like compilers. Commented definitions for the annotation members expected
     * by the runtime / reflection code can be found below for reference.
     */

    /*
     * Represents the list of classes and interfaces which are authorized to directly extend or
     * implement the current class or interface.
     */
    // Class<?>[] value();
}

