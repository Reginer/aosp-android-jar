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

package android.graphics.pdf.models.jni;

import android.graphics.pdf.utils.Preconditions;

import androidx.annotation.NonNull;

import java.util.AbstractList;
import java.util.List;

/**
 * Represents a {@code List<List<T>>}, but only uses two 1-dimensional
 * lists internally to minimize overhead. Particularly useful for a large outer
 * list that contains mostly single-element inner lists.
 *
 * @param <T> the type of the elements in the list
 * @hide
 */
// TODO(b/324536951): Remove this class after updating the native code to directly use
//  PageMatchBounds
public class ListOfList<T> extends AbstractList<List<T>> {
    private final List<T> mValues;
    private final List<Integer> mIndexToFirstValue;

    public ListOfList(@NonNull List<T> values, @NonNull List<Integer> indexToFirstValue) {
        this.mValues = Preconditions.checkNotNull(values, "values cannot be null");
        this.mIndexToFirstValue = Preconditions.checkNotNull(indexToFirstValue,
                "indexToFirstValue cannot be null");
    }

    @NonNull
    @Override
    public List<T> get(int index) {
        if (index < 0 || index >= mIndexToFirstValue.size()) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        int start = indexToFirstValue(index);
        int stop = indexToFirstValue(index + 1);
        Preconditions.checkArgument(start < stop, "Empty inner lists are not allowed.");
        return mValues.subList(start, stop);
    }

    @Override
    public int size() {
        return mIndexToFirstValue.size();
    }

    /**
     * Returns the index of the first value where the highlighter {@link android.graphics.Rect}
     * starts as a search result can span multiple lines.
     */
    public int indexToFirstValue(int match) {
        return (match < mIndexToFirstValue.size()) ? mIndexToFirstValue.get(match) : mValues.size();
    }
}

