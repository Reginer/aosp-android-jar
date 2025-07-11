/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * THIS ANNOTATION IS EXPERIMENTAL. REACH OUT TO g/ravenwood BEFORE USING IT, OR YOU HAVE ANY
 * QUESTIONS ABOUT IT.
 *
 * TODO: Javadoc
 *
 * @hide
 */
@Target({METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface RavenwoodIgnore {
    /**
     * One or more classes that aren't yet supported by Ravenwood, which is why this method is
     * being replaced.
     */
    Class<?>[] blockedBy() default {};

    /**
     * General free-form description of why this method is being replaced.
     */
    String reason() default "";

    /**
     * Tracking bug number, if any.
     */
    long bug() default 0;
}
