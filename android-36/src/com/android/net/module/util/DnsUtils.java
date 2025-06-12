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

package com.android.net.module.util;

import android.annotation.NonNull;
import android.annotation.Nullable;

/**
 * Dns common utility functions.
 *
 * @hide
 */
public class DnsUtils {

    private DnsUtils() { }

    /**
     * Convert the string to DNS case-insensitive uppercase.
     *
     * Per rfc6762#page-46, accented characters are not defined to be automatically equivalent to
     * their unaccented counterparts. So the "DNS uppercase" should be if character is a-z then they
     * transform into A-Z. Otherwise, they are kept as-is.
     */
    public static String toDnsUpperCase(@NonNull String string) {
        final char[] outChars = new char[string.length()];
        for (int i = 0; i < string.length(); i++) {
            outChars[i] = toDnsUpperCase(string.charAt(i));
        }
        return new String(outChars);
    }

    /**
     * Convert the array of labels to DNS case-insensitive uppercase.
     */
    public static String[] toDnsLabelsUpperCase(@NonNull String[] labels) {
        final String[] outStrings = new String[labels.length];
        for (int i = 0; i < labels.length; ++i) {
            outStrings[i] = toDnsUpperCase(labels[i]);
        }
        return outStrings;
    }

    /**
     * Compare two strings by DNS case-insensitive uppercase.
     */
    public static boolean equalsIgnoreDnsCase(@Nullable String a, @Nullable String b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        if (a.length() != b.length()) return false;
        for (int i = 0; i < a.length(); i++) {
            if (toDnsUpperCase(a.charAt(i)) != toDnsUpperCase(b.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compare two set of DNS labels by DNS case-insensitive uppercase.
     */
    public static boolean equalsDnsLabelIgnoreDnsCase(@NonNull String[] a, @NonNull String[] b) {
        if (a == b) {
            return true;
        }
        int length = a.length;
        if (b.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (!equalsIgnoreDnsCase(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }

    private static char toDnsUpperCase(char a) {
        return a >= 'a' && a <= 'z' ? (char) (a - ('a' - 'A')) : a;
    }
}
