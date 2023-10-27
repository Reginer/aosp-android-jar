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

package android.health.connect.internal.datatypes.utils;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.os.Bundle;

/** @hide */
public final class BundleUtils {

    private BundleUtils() {}

    /**
     * Returns the value if the {@code bundle} contains the {@code key}, otherwise throws {@link
     * IllegalArgumentException}.
     */
    public static long requireLong(@NonNull Bundle bundle, @NonNull String key) {
        requireKey(bundle, key);
        return bundle.getLong(key);
    }

    /**
     * Returns the value if the {@code bundle} contains the {@code key}, otherwise throws {@link
     * IllegalArgumentException}.
     */
    public static double requireDouble(@NonNull Bundle bundle, @NonNull String key) {
        requireKey(bundle, key);
        return bundle.getDouble(key);
    }

    /**
     * Returns the value if the {@code bundle} contains the {@code key}, otherwise throws {@link
     * IllegalArgumentException}.
     */
    @NonNull
    public static String requireString(@NonNull Bundle bundle, @NonNull String key) {
        requireKey(bundle, key);
        return requireNonNull(bundle.getString(key));
    }

    private static void requireKey(@NonNull Bundle bundle, @NonNull String key) {
        if (!bundle.containsKey(key)) {
            throw new IllegalArgumentException("Key " + key + " not found in bundle: " + bundle);
        }
    }
}
