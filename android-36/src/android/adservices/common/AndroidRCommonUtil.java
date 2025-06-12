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

package android.adservices.common;

import android.adservices.exceptions.AdServicesException;
import android.os.Build;

/**
 * Utility class to check if the version is RVC, and invoke callback onError method
 *
 * @hide
 */
public final class AndroidRCommonUtil {

    // Intended to be a utility class that should not be instantiated.
    private AndroidRCommonUtil() {}

    /**
     * Checks if the current Build Version is RVC, and if so, invokes the callback onError method
     *
     * @return true if RVC, false otherwise
     */
    public static <T> boolean invokeCallbackOnErrorOnRvc(
            AdServicesOutcomeReceiver<T, Exception> callback) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            callback.onError(new AdServicesException("AdServices is not supported on Android R"));
            return true;
        }

        return false;
    }
}
