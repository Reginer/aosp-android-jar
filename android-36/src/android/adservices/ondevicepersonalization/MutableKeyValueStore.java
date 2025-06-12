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

package android.adservices.ondevicepersonalization;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.WorkerThread;

/**
 * An interface to a read-write key-value store.
 *
 * Used as a Data Access Object for the LOCAL_DATA table.
 *
 * @see IsolatedService#getLocalData(RequestToken)
 *
 */
public interface MutableKeyValueStore extends KeyValueStore {
    /**
     * Associates the specified value with the specified key.
     * If a value already exists for that key, the old value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     *
     * @return the previous value associated with key, or null if there was no mapping for key.
     */
    @WorkerThread
    @Nullable byte[] put(@NonNull String key, @NonNull byte[] value);

    /**
     * Removes the mapping for the specified key.
     *
     * @param key key whose mapping is to be removed
     *
     * @return the previous value associated with key, or null if there was no mapping for key.
     */
    @WorkerThread
    @Nullable byte[] remove(@NonNull String key);
}
