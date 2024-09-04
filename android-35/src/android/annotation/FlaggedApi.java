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
package android.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates an API is part of a feature that is guarded by an aconfig flag, and only available if
 * the flag is enabled.
 * <p>
 * Unless the API has been finalized and has become part of the SDK, callers of the annotated API
 * must check that the flag is enabled before making any assumptions about the existence of the API.
 * <p>
 * Example:
 * <code><pre>
 *     import com.example.foobar.Flags;
 *
 *     &#64;FlaggedApi(Flags.FLAG_FOOBAR)
 *     public void foobar() { ... }
 * </pre></code>
 * Usage example:
 * <code><pre>
 *     public void codeThatUsesFoobarApi() {
 *         if (Flags.foobar()) {
 *             foobar();
 *         } else {
 *             // gracefully handle absence of the foobar API.
 *         }
 *     }
 * </pre></code>
 * @hide
 */
@Target({TYPE, METHOD, CONSTRUCTOR, FIELD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface FlaggedApi {
    /**
     * The aconfig flag used to guard the feature this API is part of. Use the aconfig
     * auto-generated constant to refer to the flag, e.g. @FlaggedApi(Flags.FLAG_FOOBAR).
     */
    String value();
}
