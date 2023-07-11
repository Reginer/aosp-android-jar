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

package com.android.net.module.util;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Utilities for {@link Collection} and arrays.
 * @hide
 */
public final class CollectionUtils {
    private CollectionUtils() {}

    /**
     * @return True if the array is null or 0-length.
     */
    public static <T> boolean isEmpty(@Nullable T[] array) {
        return array == null || array.length == 0;
    }

    /**
     * @return True if the collection is null or 0-length.
     */
    public static <T> boolean isEmpty(@Nullable Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Returns an int array from the given Integer list.
     */
    @NonNull
    public static int[] toIntArray(@NonNull Collection<Integer> list) {
        int[] array = new int[list.size()];
        int i = 0;
        for (Integer item : list) {
            array[i] = item;
            i++;
        }
        return array;
    }

    /**
     * Returns a long array from the given long list.
     */
    @NonNull
    public static long[] toLongArray(@NonNull Collection<Long> list) {
        long[] array = new long[list.size()];
        int i = 0;
        for (Long item : list) {
            array[i] = item;
            i++;
        }
        return array;
    }

    /**
     * @return True if all elements satisfy the predicate, false otherwise.
     *   Note that means this always returns true for empty collections.
     */
    public static <T> boolean all(@NonNull Collection<T> elem, @NonNull Predicate<T> predicate) {
        for (final T e : elem) {
            if (!predicate.test(e)) return false;
        }
        return true;

    }

    /**
     * @return True if any element satisfies the predicate, false otherwise.
     *   Note that means this always returns false for empty collections.
     */
    public static <T> boolean any(@NonNull Collection<T> elem, @NonNull Predicate<T> predicate) {
        return indexOf(elem, predicate) >= 0;
    }

    /**
     * @return The index of the first element that matches the predicate, or -1 if none.
     */
    @Nullable
    public static <T> int indexOf(@NonNull Collection<T> elem, @NonNull Predicate<T> predicate) {
        int idx = 0;
        for (final T e : elem) {
            if (predicate.test(e)) return idx;
            idx++;
        }
        return -1;
    }

    /**
     * @return True if there exists at least one element in the sparse array for which
     * condition {@code predicate}
     */
    public static <T> boolean any(@NonNull SparseArray<T> array, @NonNull Predicate<T> predicate) {
        for (int i = 0; i < array.size(); ++i) {
            if (predicate.test(array.valueAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the array contains the specified value.
     */
    public static boolean contains(@Nullable short[] array, short value) {
        if (array == null) return false;
        for (int element : array) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the array contains the specified value.
     */
    public static boolean contains(@Nullable int[] array, int value) {
        if (array == null) return false;
        for (int element : array) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the array contains the specified value.
     */
    public static <T> boolean contains(@Nullable T[] array, @Nullable T value) {
        return indexOf(array, value) != -1;
    }

    /**
     * Return first index of value in given array, or -1 if not found.
     */
    public static <T> int indexOf(@Nullable T[] array, @Nullable T value) {
        if (array == null) return -1;
        for (int i = 0; i < array.length; i++) {
            if (Objects.equals(array[i], value)) return i;
        }
        return -1;
    }

    /**
     * Returns a new collection of elements that match the passed predicate.
     * @param source the elements to filter.
     * @param test the predicate to test for.
     * @return a new collection containing only the source elements that satisfy the predicate.
     */
    @NonNull public static <T> ArrayList<T> filter(@NonNull final Collection<T> source,
            @NonNull final Predicate<T> test) {
        final ArrayList<T> matches = new ArrayList<>();
        for (final T e : source) {
            if (test.test(e)) {
                matches.add(e);
            }
        }
        return matches;
    }

    /**
     * Return sum of the given long array.
     */
    public static long total(@Nullable long[] array) {
        long total = 0;
        if (array != null) {
            for (long value : array) {
                total += value;
            }
        }
        return total;
    }

    /**
     * Returns true if the first collection contains any of the elements of the second.
     * @param haystack where to search
     * @param needles what to search for
     * @param <T> type of elements
     * @return true if |haystack| contains any of the |needles|, false otherwise
     */
    public static <T> boolean containsAny(Collection<T> haystack, Collection<? extends T> needles) {
        for (T needle : needles) {
            if (haystack.contains(needle)) return true;
        }
        return false;
    }

    /**
     * Returns true if the first collection contains all of the elements of the second.
     * @param haystack where to search
     * @param needles what to search for
     * @param <T> type of elements
     * @return true if |haystack| contains all of the |needles|, false otherwise
     */
    public static <T> boolean containsAll(Collection<T> haystack, Collection<? extends T> needles) {
        return haystack.containsAll(needles);
    }

}
