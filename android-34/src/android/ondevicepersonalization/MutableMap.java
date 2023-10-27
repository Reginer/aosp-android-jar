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

package android.ondevicepersonalization;

import android.annotation.NonNull;

/**
 * Data Access Object for the LOCAL_DATA table. The LOCAL_DATA table is a mutable
 * data store that contains data that has been stored locally by the vendor.
 *
 * @hide
 */
public interface MutableMap extends ImmutableMap {
    /**
     * Associates the specified value with the specified key in LOCAL_DATA.
     * If LOCAL_DATA previously contained a mapping for the key, the old value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     *
     * @return the previous value associated with key, or null if there was no mapping for key.
     */
    byte[] put(@NonNull String key, @NonNull byte[] value) throws OnDevicePersonalizationException;

    /**
     * Removes the mapping for the specified key from LOCAL_DATA if present.
     *
     * @param key key whose mapping is to be removed from the LOCAL_DATA
     *
     * @return the previous value associated with key, or null if there was no mapping for key.
     */
    byte[] remove(String key) throws OnDevicePersonalizationException;
}
