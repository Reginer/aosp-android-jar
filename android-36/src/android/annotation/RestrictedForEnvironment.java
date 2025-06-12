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

package android.annotation;


import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates if an API is restricted in certain environments.
 *
 * <p>
 * The explicit annotation aids in surfacing the restriction to the developers of the environments
 * in multiple ways:
 * </p>
 *
 * <ul>
 * <li>Metalava will consume these annotations to generate appropriate javadocs.</li>
 * <li>Linters will consume these annotation to show warning to developers on their IDE.</li>
 * <li>Tests will consume these annotation to verify restricted APIs have been annotated.</li>
 * </ul>
 *
 * @hide
 */
@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RestrictedForEnvironment.Container.class)
public @interface RestrictedForEnvironment {

    /** List of environments where the entity is restricted. */
    Environment[] environments();

    /**
     * SDK version since when the restriction started.
     *
     * Possible values are defined in {@link android.os.Build.VERSION_CODES}.
     */
    int from();

    enum Environment {
        /**
         * See {@link android.app.sdksandbox.SdkSandboxManager}
         */
        SDK_SANDBOX {
            @Override
            public String toString() {
                return "SDK Runtime";
            }
        }
    }

    /**
     * Container for {@link RestrictedForEnvironment} that allows it to be applied repeatedly to
     * types.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(TYPE)
    @interface Container {
        RestrictedForEnvironment[] value();
    }
}
