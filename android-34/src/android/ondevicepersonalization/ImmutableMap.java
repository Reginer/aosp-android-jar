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

import java.util.Set;

/**
 * Data Access Object for the REMOTE_DATA table. The REMOTE_DATA table is a immutable
 * data store that contains data that has been downloaded by the ODP platform from
 * the vendor endpoint that is declared in the package manifest.
 *
 * @hide
 */
public interface ImmutableMap {
    /**
     * Looks up a key in the REMOTE_DATA table.
     *
     * @param key The key to look up.
     * @return the value to which the specified key is mapped,
     * or null if there contains no mapping for the key.
     */
    byte[] get(@NonNull String key) throws OnDevicePersonalizationException;

    /**
     * Returns a Set view of the keys contained in the REMOTE_DATA table.
     *
     * @return a Set view of the keys contained in the REMOTE_DATA table.
     */
    Set<String> keySet() throws OnDevicePersonalizationException;
}
