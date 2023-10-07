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
package android.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The annotated element is considered deprecated in the public SDK. This will be turned into a
 * plain &#64;Deprecated annotation in the SDK.
 *
 * <p>The value parameter should be the message to include in the documentation as a &#64;deprecated
 * comment.
 *
 * @hide
 */
@Retention(SOURCE)
@Target(value = {CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, PACKAGE, PARAMETER, TYPE})
public @interface DeprecatedForSdk {
    /**
     * The message to include in the documentation, which will be merged in as a &#64;deprecated
     * tag.
     */
    String value();

    /**
     * If specified, one or more annotation classes corresponding to particular API surfaces where
     * the API will <b>not</b> be marked as deprecated, such as {@link SystemApi} or {@link
     * TestApi}.
     */
    Class<?>[] allowIn() default {};
}
