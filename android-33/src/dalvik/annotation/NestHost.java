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
 * Records the nest host of the nest group to which a class or interface claims to belong to.
 *
 * <p>The annotation is used to determine the nest group to which this class belongs to. All classes
 * in a nest group are considered nest mates and share a common access control mechanism.
 *
 * <p>The NestHost class must also include this class into its {@link NestMembers} list.
 *
 * <p>The absence of the NestHost annotation implies that the class is its own host.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@interface NestHost {

    /*
     * This annotation is never used in source code; it is expected to be generated in .dex
     * files by tools like compilers. Commented definitions for the annotation members expected
     * by the runtime / reflection code can be found below for reference.
     */

    /*
     * The host class for the annotated class. This must not be null.
     *
     * It is expected that if the NestHost annotation is used, the host is a different class from
     * the current, annotated, one.
     */
    Class<?> host();
}

