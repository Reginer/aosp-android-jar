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
import android.util.ArrayMap;
import android.util.Pair;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
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
    public static <T> int indexOf(@NonNull final Collection<T> elem,
            @NonNull final Predicate<? super T> predicate) {
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
     * Returns the index of the needle array in the haystack array, or -1 if it can't be found.
     * This is a byte array equivalent of Collections.indexOfSubList().
     */
    public static int indexOfSubArray(@NonNull byte[] haystack, @NonNull byte[] needle) {
        for (int i = 0; i < haystack.length - needle.length + 1; i++) {
            boolean found = true;
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
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
    public static <T> boolean containsAny(@NonNull final Collection<T> haystack,
            @NonNull final Collection<? extends T> needles) {
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
    public static <T> boolean containsAll(@NonNull final Collection<T> haystack,
            @NonNull final Collection<? extends T> needles) {
        return haystack.containsAll(needles);
    }

    /**
     * Returns the first item of a collection that matches the predicate.
     * @param haystack The collection to search.
     * @param condition The predicate to match.
     * @param <T> The type of element in the collection.
     * @return The first element matching the predicate, or null if none.
     */
    @Nullable
    public static <T> T findFirst(@NonNull final Collection<T> haystack,
            @NonNull final Predicate<? super T> condition) {
        for (T needle : haystack) {
            if (condition.test(needle)) return needle;
        }
        return null;
    }

    /**
     * Returns the last item of a List that matches the predicate.
     * @param haystack The List to search.
     * @param condition The predicate to match.
     * @param <T> The type of element in the list.
     * @return The last element matching the predicate, or null if none.
     */
    // There is no way to reverse iterate a Collection in Java (e.g. the collection may
    // be a single-linked list), so implementing this on Collection is necessarily very
    // wasteful (store and reverse a copy, test all elements, or recurse to the end of the
    // list to test on the up path and possibly blow the call stack)
    @Nullable
    public static <T> T findLast(@NonNull final List<T> haystack,
            @NonNull final Predicate<? super T> condition) {
        for (int i = haystack.size() - 1; i >= 0; --i) {
            final T needle = haystack.get(i);
            if (condition.test(needle)) return needle;
        }
        return null;
    }

    /**
     * Returns whether a collection contains an element matching a condition
     * @param haystack The collection to search.
     * @param condition The predicate to match.
     * @param <T> The type of element in the collection.
     * @return Whether the collection contains any element matching the condition.
     */
    public static <T> boolean contains(@NonNull final Collection<T> haystack,
            @NonNull final Predicate<? super T> condition) {
        return -1 != indexOf(haystack, condition);
    }

    /**
     * Standard map function, but returns a new modifiable ArrayList
     *
     * This returns a new list that contains, for each element of the source collection, its
     * image through the passed transform.
     * Elements in the source can be null if the transform accepts null inputs.
     * Elements in the output can be null if the transform ever returns null.
     * This function never returns null. If the source collection is empty, it returns the
     * empty list.
     * Contract : this method calls the transform function exactly once for each element in the
     * list, in iteration order.
     *
     * @param source the source collection
     * @param transform the function to transform the elements
     * @param <T> type of source elements
     * @param <R> type of destination elements
     * @return an unmodifiable list of transformed elements
     */
    @NonNull
    public static <T, R> ArrayList<R> map(@NonNull final Collection<T> source,
            @NonNull final Function<? super T, ? extends R> transform) {
        final ArrayList<R> dest = new ArrayList<>(source.size());
        for (final T e : source) {
            dest.add(transform.apply(e));
        }
        return dest;
    }

    /**
     * Standard zip function, but returns a new modifiable ArrayList
     *
     * This returns a list of pairs containing, at each position, a pair of the element from the
     * first list at that index and the element from the second list at that index.
     * Both lists must be the same size. They may contain null.
     *
     * The easiest way to visualize what's happening is to think of two lists being laid out next
     * to each other and stitched together with a zipper.
     *
     * Contract : this method will read each element of each list exactly once, in some unspecified
     * order. If it throws, it will not read any element.
     *
     * @param first the first list of elements
     * @param second the second list of elements
     * @param <T> the type of first elements
     * @param <R> the type of second elements
     * @return the zipped list
     */
    @NonNull
    public static <T, R> ArrayList<Pair<T, R>> zip(@NonNull final List<T> first,
            @NonNull final List<R> second) {
        final int size = first.size();
        if (size != second.size()) {
            throw new IllegalArgumentException("zip : collections must be the same size");
        }
        final ArrayList<Pair<T, R>> dest = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            dest.add(new Pair<>(first.get(i), second.get(i)));
        }
        return dest;
    }

    /**
     * Returns a new ArrayMap that associates each key with the value at the same index.
     *
     * Both lists must be the same size.
     * Both keys and values may contain null.
     * Keys may not contain the same value twice.
     *
     * Contract : this method will read each element of each list exactly once, but does not
     * specify the order, except if it throws in which case the number of reads is undefined.
     *
     * @param keys The list of keys
     * @param values The list of values
     * @param <T> The type of keys
     * @param <R> The type of values
     * @return The associated map
     */
    @NonNull
    public static <T, R> ArrayMap<T, R> assoc(
            @NonNull final List<T> keys, @NonNull final List<R> values) {
        final int size = keys.size();
        if (size != values.size()) {
            throw new IllegalArgumentException("assoc : collections must be the same size");
        }
        final ArrayMap<T, R> dest = new ArrayMap<>(size);
        for (int i = 0; i < size; ++i) {
            final T key = keys.get(i);
            if (dest.containsKey(key)) {
                throw new IllegalArgumentException(
                        "assoc : keys may not contain the same value twice");
            }
            dest.put(key, values.get(i));
        }
        return dest;
    }

    /**
     * Returns an index of the given SparseArray that contains the given value, or -1
     * number if no keys map to the given value.
     *
     * <p>Note this is a linear search, and if multiple keys can map to the same value
     * then the smallest index is returned.
     *
     * <p>This function compares values with {@code equals} while the
     * {@link SparseArray#indexOfValue} compares values using {@code ==}.
     */
    public static <T> int getIndexForValue(SparseArray<T> sparseArray, T value) {
        for(int i = 0, nsize = sparseArray.size(); i < nsize; i++) {
            T valueAt = sparseArray.valueAt(i);
            if (valueAt == null) {
                if (value == null) {
                    return i;
                };
            } else if (valueAt.equals(value)) {
                return i;
            }
        }
        return -1;
    }
}
