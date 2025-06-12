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
 * Records the nest members of a class or interface.
 *
 * <p>The annotation is used to determine the nest group to which this class belongs to. All classes
 * in a nest group are considered nest mates and share a common access control mechanism.
 *
 * <p>This annotation is used only by a nest host and any nest member listed
 * here must have this class as a {@link NestHost}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@interface NestMembers {

    /*
     * This annotation is never used in source code; it is expected to be generated in .dex
     * files by tools like compilers. Commented definitions for the annotation members expected
     * by the runtime / reflection code can be found below for reference.
     */

    /*
     * The array of member classes for the annotated class. It must not be null
     * and it must not contain any null elements.
     *
     * The nest host is implicitly a member of the nest group and does not need
     * to be included in the list of member classes.
     */
    Class<?>[] classes();
}

