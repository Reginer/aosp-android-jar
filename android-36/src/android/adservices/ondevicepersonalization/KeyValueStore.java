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

import java.util.Set;

/**
 * An interface to a read-only key-value store.
 *
 * Used as a Data Access Object for the REMOTE_DATA table.
 *
 * @see IsolatedService#getRemoteData(RequestToken)
 *
 */
public interface KeyValueStore {
    /**
     * Looks up a key in a read-only store.
     *
     * @param key The key to look up.
     * @return the value to which the specified key is mapped,
     * or null if there contains no mapping for the key.
     *
     */
    @WorkerThread
    @Nullable byte[] get(@NonNull String key);

    /**
     * Returns a Set view of the keys contained in the REMOTE_DATA table.
     */
    @WorkerThread
    @NonNull Set<String> keySet();

    /**
     * Returns the table id {@link ModelId.TableId} of KeyValueStore implementation.
     *
     * @hide
     */
    default int getTableId(){
        return 0;
    }
}
