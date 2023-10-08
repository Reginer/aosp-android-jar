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

package android.adservices;

import android.annotation.SuppressLint;

/**
 * This class specifies the current version of the AdServices API.
 *
 * @removed
 */
public class AdServicesVersion {

    /**
     * @hide
     */
    public AdServicesVersion() {}

    /**
     * The API version of this AdServices API.
     */
    @SuppressLint("CompileTimeConstant")
    public static final int API_VERSION;

    // This variable needs to be initialized in static {} , otherwise javac
    // would inline these constants and they won't be updatable.
    static {
        API_VERSION = 2;
    }
}

