/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.internal.telephony;

import android.util.ArrayMap;

import java.util.Collection;
import java.util.Map;

/**
 * A very basic bidirectional map.
 */
public class BiMap<K, V> {
    private Map<K, V> mPrimaryMap = new ArrayMap<>();
    private Map<V, K> mSecondaryMap = new ArrayMap<>();

    public boolean put(K key, V value) {
        if (key == null || value == null || mPrimaryMap.containsKey(key) ||
                mSecondaryMap.containsKey(value)) {
            return false;
        }

        mPrimaryMap.put(key, value);
        mSecondaryMap.put(value, key);
        return true;
    }

    public boolean remove(K key) {
        if (key == null) {
            return false;
        }
        if (mPrimaryMap.containsKey(key)) {
            V value = getValue(key);
            mPrimaryMap.remove(key);
            mSecondaryMap.remove(value);
            return true;
        }
        return false;
    }

    public boolean removeValue(V value) {
        if (value == null) {
            return false;
        }
        return remove(getKey(value));
    }

    public V getValue(K key) {
        return mPrimaryMap.get(key);
    }

    public K getKey(V value) {
        return mSecondaryMap.get(value);
    }

    public Collection<V> getValues() {
        return mPrimaryMap.values();
    }

    public void clear() {
        mPrimaryMap.clear();
        mSecondaryMap.clear();
    }
}
