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

package android.system;

import java.lang.ref.Cleaner;
import jdk.internal.ref.CleanerFactory;
import libcore.util.NonNull;

/**
 * Java.lang.ref.Cleaner encourages each library to create a Cleaner, with an associated
 * thread, to process Cleaner Runnables for that library's registered cleaning actions.
 * This approach isolates cleaning actions from different libraries from each other; a slow cleaning
 * action in one library will only minimally affect cleaning actions in another.
 *
 * However, this comes at the cost of introducing one Cleaner thread per library that uses
 * Cleaners. This could introduce dozens of additional threads per process, which is often
 * not an acceptable cost, especially on memory-limited devices.
 *
 * SystemCleaner instead provides access to a shared Cleaner, shared across the entire process.
 * It is greatly preferred when all cleaning actions registered by a client are known to
 * complete quickly, without explicit I/O, interprocess communication, or network access.
 * Registering a non-terminating or excessively slow cleaning action with the shared cleaner
 * may cause the process to perform very badly, hang, or be killed.
 *
 * Only for developers of the Android platform itself: As with all Cleaners, use of SystemCleaner
 * requires an extra thread to be started. This is unsafe for zygote-callable code. Use
 * NativeAllocationRegistry.
 */
public final class SystemCleaner {

    private SystemCleaner() {}

    /**
     * Return a single Cleaner that's shared across the entire process. Thread-safe.
     */
    @NonNull public static Cleaner cleaner() {
        // We just abuse CleanerFactory. That has the down side that a runaway Cleaner will cause
        // issues for system libraries. If this eventually becomes a problem due to widespread use,
        // we can set up another thread here.
        // TODO: Add some sort of watchdog for this.
        return CleanerFactory.cleaner();
    }
}

