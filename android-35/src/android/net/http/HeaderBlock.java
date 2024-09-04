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

package android.net.http;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

/**
 * Unmodifiable container of headers or trailers.
 */
public abstract class HeaderBlock {
    /**
     * Returns an unmodifiable list of the header field and value pairs.
     * For response, the headers are in the same order they are received over the wire.
     * For request, the headers are in the same order they are added.
     *
     * @return an unmodifiable list of header field and value pairs
     */
    @NonNull
    public abstract List<Map.Entry<String, String>> getAsList();

    /**
     * Returns an unmodifiable map from header field names to lists of values.
     * Order of each list of values for a single header field is:
     * For response, the same order they were received over the wire.
     * For request, the same order they were added.
     * The iteration order of keys is unspecified.
     *
     * @return an unmodifiable map from header field names to lists of values
     */
    @NonNull
    public abstract Map<String, List<String>> getAsMap();
}
