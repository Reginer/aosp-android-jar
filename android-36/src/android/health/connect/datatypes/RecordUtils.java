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

package android.health.connect.datatypes;

import android.annotation.Nullable;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Utils class with record helper functions.
 *
 * @hide
 */
public class RecordUtils {

    // TODO(b/263367261): use this function for sleep session.
    /** Returns true if two char sequences are equal. Accepts null char sequences. */
    public static boolean isEqualNullableCharSequences(
            @Nullable CharSequence sequence1, @Nullable CharSequence sequence2) {
        if (sequence1 == null) {
            return sequence2 == null;
        }

        return CharSequence.compare(sequence1, sequence2) == 0;
    }

    public static ZoneOffset getDefaultZoneOffset() {
        return ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
    }
}
