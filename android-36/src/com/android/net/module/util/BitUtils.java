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

package com.android.net.module.util;

import android.annotation.NonNull;
import android.annotation.Nullable;

/**
 * @hide
 */
public class BitUtils {
    /**
     * Unpacks long value into an array of bits.
     */
    public static int[] unpackBits(long val) {
        int size = Long.bitCount(val);
        int[] result = new int[size];
        int index = 0;
        int bitPos = 0;
        while (val != 0) {
            if ((val & 1) == 1) result[index++] = bitPos;
            val = val >>> 1;
            bitPos++;
        }
        return result;
    }

    /**
     * Packs a list of ints in the same way as packBits()
     *
     * Each passed int is the rank of a bit that should be set in the returned long.
     * Example : passing (1,3) will return in 0b00001010 and passing (5,6,0) will return 0b01100001
     *
     * @param bits bits to pack
     * @return a long with the specified bits set.
     */
    public static long packBitList(int... bits) {
        return packBits(bits);
    }

    /**
     * Packs array of bits into a long value.
     *
     * Each passed int is the rank of a bit that should be set in the returned long.
     * Example : passing [1,3] will return in 0b00001010 and passing [5,6,0] will return 0b01100001
     *
     * @param bits bits to pack
     * @return a long with the specified bits set.
     */
    public static long packBits(int[] bits) {
        long packed = 0;
        for (int b : bits) {
            packed |= (1L << b);
        }
        return packed;
    }

    /**
     * An interface for a function that can retrieve a name associated with an int.
     *
     * This is useful for bitfields like network capabilities or network score policies.
     */
    @FunctionalInterface
    public interface NameOf {
        /** Retrieve the name associated with the passed value */
        String nameOf(int value);
    }

    /**
     * Given a bitmask and a name fetcher, append names of all set bits to the builder
     *
     * This method takes all bit sets in the passed bitmask, will figure out the name associated
     * with the weight of each bit with the passed name fetcher, and append each name to the
     * passed StringBuilder, separated by the passed separator.
     *
     * For example, if the bitmask is 0110, and the name fetcher return "BIT_1" to "BIT_4" for
     * numbers from 1 to 4, and the separator is "&", this method appends "BIT_2&BIT3" to the
     * StringBuilder.
     */
    public static void appendStringRepresentationOfBitMaskToStringBuilder(@NonNull StringBuilder sb,
            long bitMask, @NonNull NameOf nameFetcher, @NonNull String separator) {
        int bitPos = 0;
        boolean firstElementAdded = false;
        while (bitMask != 0) {
            if ((bitMask & 1) != 0) {
                if (firstElementAdded) {
                    sb.append(separator);
                } else {
                    firstElementAdded = true;
                }
                sb.append(nameFetcher.nameOf(bitPos));
            }
            bitMask >>>= 1;
            ++bitPos;
        }
    }

    /**
     * Returns a short but human-readable string of updates between an old and a new bit fields.
     *
     * @param oldVal the old bit field to diff from
     * @param newVal the new bit field to diff to
     * @return a string fit for logging differences, or null if no differences.
     *         this method cannot return the empty string.
     */
    @Nullable
    public static String describeDifferences(final long oldVal, final long newVal,
            @NonNull final NameOf nameFetcher) {
        final long changed = oldVal ^ newVal;
        if (0 == changed) return null;
        // If the control reaches here, there are changes (additions, removals, or both) so
        // the code below is guaranteed to add something to the string and can't return "".
        final long removed = oldVal & changed;
        final long added = newVal & changed;
        final StringBuilder sb = new StringBuilder();
        if (0 != removed) {
            sb.append("-");
            appendStringRepresentationOfBitMaskToStringBuilder(sb, removed, nameFetcher, "-");
        }
        if (0 != added) {
            sb.append("+");
            appendStringRepresentationOfBitMaskToStringBuilder(sb, added, nameFetcher, "+");
        }
        return sb.toString();
    }
}
