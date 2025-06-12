/*
 * Copyright (C) 2017 The Android Open Source Project
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


package com.android.internal.util;

import android.annotation.NonNull;
import android.annotation.Nullable;
import java.util.Objects;

/** @hide */
@android.ravenwood.annotation.RavenwoodKeepWholeClass
public class ObjectUtils {
    private ObjectUtils() {}

    /**
     * Returns the first of two given parameters that is not {@code null}, if either is,
     * or otherwise throws a {@link NullPointerException}.
     *
     * @throws NullPointerException if both {@code a} and {@code b} were {@code null}
     */
    @NonNull
    public static <T> T firstNotNull(@Nullable T a, @NonNull T b) {
        return a != null ? a : Objects.requireNonNull(b);
    }

    /**
     * Nullsafe {@link Comparable#compareTo}
     */
    public static <T extends Comparable> int compare(@Nullable T a, @Nullable T b) {
        if (a != null) {
            return (b != null) ? a.compareTo(b) : 1;
        } else {
            return (b != null) ? -1 : 0;
        }
    }

    /**
     * Returns its first argument if non-null, and the second otherwise.
     */
    @Nullable
    public static <T> T getOrElse(@Nullable final T object, @Nullable final T otherwise) {
        return null != object ? object : otherwise;
    }
}
