/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.clockwork;

import java.util.ArrayList;

/**
 * Utility functions for converting and generating Arrays
 */
public final class ArrayUtils {

    /**
     * Converts an int array into an ArrayList of shorts
     */
    public static ArrayList<Short> intArrayToShortArrayList(int[] array) {
        ArrayList<Short> arrayList = new ArrayList<>(array.length);
        for (int element : array) {
            arrayList.add(Integer.valueOf(element).shortValue());
        }
        return arrayList;
    }

    /**
     * Creates an ArrayList of shorts with all the same values
     * @param size size of the ArrayList
     * @param value value of each element in the ArrayList
     */
    public static ArrayList<Short> createMonotonicShortArray(int size, int value) {
        ArrayList<Short> arrayList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            arrayList.add(Integer.valueOf(value).shortValue());
        }
        return arrayList;
    }
}
