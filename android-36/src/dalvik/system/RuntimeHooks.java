/*
 * Copyright (C) 2018 The Android Open Source Project
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

package dalvik.system;

import static android.annotation.SystemApi.Client.MODULE_LIBRARIES;

import android.annotation.SystemApi;

import java.util.Objects;
import java.util.TimeZone;
import java.util.function.Supplier;
import libcore.util.NonNull;
import libcore.util.Nullable;

/**
 * Provides lifecycle methods and other hooks for an Android runtime "container" to call into the
 * runtime and core libraries during initialization. For example, from
 * {@link com.android.internal.os.RuntimeInit}.
 *
 * <p>Having a facade class helps to limit the container's knowledge of runtime and core libraries
 * internal details. All methods assume the container initialization is single threaded.
 *
 * @hide
 */
@SystemApi(client = MODULE_LIBRARIES)
public final class RuntimeHooks {

    private static Supplier<String> zoneIdSupplier;

    private RuntimeHooks() {
        // No need to construct an instance. All methods are static.
    }

    /**
     * Sets the {@link Supplier} that is used by {@link TimeZone} to retrieve the current time zone
     * ID iff the cached default is {@code null}.
     *
     * <p>This method also clears the current {@link TimeZone} default ensuring that the supplier
     * will be used next time {@link TimeZone#getDefault()} is called (unless
     * {@link TimeZone#setDefault(TimeZone)} is called with a non-{@code null} value in the interim).
     *
     * <p>Once set the supplier cannot be changed.
     *
     * @param zoneIdSupplier new {@link Supplier} of the time zone ID
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void setTimeZoneIdSupplier(@NonNull Supplier<String> zoneIdSupplier) {
        if (RuntimeHooks.zoneIdSupplier != null) {
            throw new UnsupportedOperationException("zoneIdSupplier instance already set");
        }
        RuntimeHooks.zoneIdSupplier = Objects.requireNonNull(zoneIdSupplier);
        TimeZone.setDefault(null);
    }

    /**
     * @hide
     */
    // VisibleForTesting
    public static void clearTimeZoneIdSupplier() {
        RuntimeHooks.zoneIdSupplier = null;
    }

    /**
     * Returns the {@link Supplier} that should be used to discover the time zone.
     *
     * @hide
     */
    public static Supplier<String> getTimeZoneIdSupplier() {
        return RuntimeHooks.zoneIdSupplier;
    }

    /**
     * Sets an {@link Thread.UncaughtExceptionHandler} that will be called before any
     * returned by {@link Thread#getUncaughtExceptionHandler()}. To allow the standard
     * handlers to run, this handler should never terminate this process. Any
     * throwables thrown by the handler will be ignored by
     * {@link Thread#dispatchUncaughtException(Throwable)}.
     *
     * @param uncaughtExceptionHandler handler for uncaught exceptions
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void setUncaughtExceptionPreHandler(
            @Nullable Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        Thread.setUncaughtExceptionPreHandler(uncaughtExceptionHandler);
    }
}
