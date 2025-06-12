/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.graphics.pdf;

import java.util.HashMap;
import java.util.Map;

/**
 * @hide
 * This class manages Id-Index Mapping for PdfAnnotations and PdfPageObjects.
 */
public class PdfPageComponentsIdManager {
    private final HashMap<Integer, Integer> mIdIndexMap;
    private HashMap<Integer, Integer> mIndexIdMap;
    private int mComponentCurrentMaxId;

    public PdfPageComponentsIdManager() {
        mIdIndexMap = new HashMap<>();
        mIndexIdMap = new HashMap<>();
        mComponentCurrentMaxId = -1;
    }

    void deleteId(int id) {
        int deletedComponentIndex = mIdIndexMap.get(id);
        mIdIndexMap.remove(id);

        HashMap<Integer, Integer> updatedIndexIdMap = new HashMap<>();
        // Iterate and modify values greater than deletedComponentIndex
        for (Map.Entry<Integer, Integer> entry : mIdIndexMap.entrySet()) {
            if (entry.getValue() > deletedComponentIndex) {
                entry.setValue(entry.getValue() - 1);
            }
            updatedIndexIdMap.put(entry.getValue(), entry.getKey());
        }
        mIndexIdMap = updatedIndexIdMap;
    }

    int getIndexForId(int id) {
        if (!mIdIndexMap.containsKey(id)) {
            return -1;
        }
        return mIdIndexMap.get(id);
    }

    int getIdForIndex(int index) {
        if (!mIndexIdMap.containsKey(index)) {
            mComponentCurrentMaxId += 1;
            mIdIndexMap.put(mComponentCurrentMaxId, index);
            mIndexIdMap.put(index, mComponentCurrentMaxId);
            return mComponentCurrentMaxId;
        }
        return mIndexIdMap.get(index);
    }
}
