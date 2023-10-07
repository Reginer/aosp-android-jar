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

package com.android.clockwork.displayoffload;


import android.util.ArrayMap;
import android.util.Log;

import java.util.Map;
import java.util.function.Function;

/**
 * Helper class that generates monotonically increasing unique id for arbitrary Object key.
 */
class UniqueIdGenerator {
    private static final boolean DEBUG = false;
    private final String mTag = toString();
    private final Map<Object, Integer> mIdMap = new ArrayMap<>();
    private final Function<Integer, Boolean> mShouldSkip;
    private Integer mGlobalBindingId = 0;

    UniqueIdGenerator(Function<Integer, Boolean> shouldSkip) {
        mShouldSkip = shouldSkip;
    }

    /**
     * Reset the internal state. Subsequent getId calls will no longer return previously returned
     * values.
     */
    synchronized void reset() {
        mGlobalBindingId = 0;
        mIdMap.clear();
    }

    /**
     * Create and reserve a new integer identifier.
     *
     * @return the newly created unique integer identifier
     */
    synchronized int nextId() {
        while (mIdMap.containsKey(mGlobalBindingId) || mShouldSkip.apply(mGlobalBindingId)) {
            // Skip numbers that are used or reserved
            mGlobalBindingId += 1;
        }
        int retVal = mGlobalBindingId++;
        if (DEBUG) {
            Log.d(mTag, "nextId: " + retVal);
        }
        return retVal;
    }

    /**
     * Get a new integer identifier for an Object. Create and reserve a new one if this Object
     * has not been seen before. Otherwise, return the same integer value as last time.
     *
     * @param originalId any Object identifier
     * @return the newly created unique integer identifier, or a previously returned value for
     *         this Object
     */
    synchronized int getId(Object originalId) {
        if (originalId instanceof Integer && mShouldSkip.apply((Integer) originalId)) {
            // Explicitly requesting a system resource id
            int originalIdInt = (Integer) originalId;
            mIdMap.put(originalId, originalIdInt);
            if (DEBUG) {
                Log.d(mTag, "getId(Reserved): " + originalIdInt);
            }
            return originalIdInt;
        }
        if (mIdMap.containsKey(originalId)) {
            int retVal = mIdMap.get(originalId);
            if (DEBUG) {
                Log.d(mTag, "getId(hit/" + originalId + "): " + retVal);
            }
            return retVal;
        }
        int id = nextId();
        mIdMap.put(originalId, id);
        if (DEBUG) {
            Log.d(mTag, "getId(miss/" + originalId + "):" + id);
        }
        return id;
    }
}
