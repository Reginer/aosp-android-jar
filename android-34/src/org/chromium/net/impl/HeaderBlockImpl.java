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

package org.chromium.net.impl;

import android.net.http.HeaderBlock;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * An implementation of {@link HeaderBlock}.
 */
public class HeaderBlockImpl extends HeaderBlock {
    private final List<Map.Entry<String, String>> mAllHeadersList;
    private Map<String, List<String>> mHeadersMap;

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public HeaderBlockImpl(List<Map.Entry<String, String>> allHeadersList) {
        mAllHeadersList = Collections.unmodifiableList(allHeadersList);
    }

    @Override
    public List<Map.Entry<String, String>> getAsList() {
        return mAllHeadersList;
    }

    @Override
    public Map<String, List<String>> getAsMap() {
        // This is potentially racy...but races will only result in wasted resource.
        if (mHeadersMap != null) {
            return mHeadersMap;
        }
        Map<String, List<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, String> entry : mAllHeadersList) {
            List<String> values = map.computeIfAbsent(
                    entry.getKey(),
                    key -> new ArrayList<>());
            values.add(entry.getValue());
        }
        map.replaceAll((key, values) -> Collections.unmodifiableList(values));
        mHeadersMap = Collections.unmodifiableMap(map);
        return mHeadersMap;
    }
}
