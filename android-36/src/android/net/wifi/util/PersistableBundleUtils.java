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

package android.net.wifi.util;

import android.annotation.Nullable;
import android.os.PersistableBundle;

import java.util.Arrays;
import java.util.Objects;

/**
 * Utilities for {@link PersistableBundle}, which does not provide its own implementations of
 * equals() and hashCode().
 *
 * @hide
 */
public class PersistableBundleUtils {
    /** Generate a hashcode for the provided PersistableBundle. */
    public static int getHashCode(@Nullable PersistableBundle bundle) {
        if (bundle == null) {
            return -1;
        }

        int iterativeHashcode = 0;
        for (String key : bundle.keySet()) {
            Object val = bundle.get(key);
            if (val instanceof PersistableBundle) {
                iterativeHashcode =
                        Objects.hash(iterativeHashcode, key, getHashCode((PersistableBundle) val));
            } else if (val.getClass().isArray()) {
                iterativeHashcode = Objects.hash(iterativeHashcode, key, getArrayHashCode(val));
            } else {
                iterativeHashcode = Objects.hash(iterativeHashcode, key, val);
            }
        }

        return iterativeHashcode;
    }

    private static int getArrayHashCode(Object arr) {
        if (arr instanceof boolean[]) {
            return Arrays.hashCode((boolean[]) arr);
        } else if (arr instanceof double[]) {
            return Arrays.hashCode((double[]) arr);
        } else if (arr instanceof int[]) {
            return Arrays.hashCode((int[]) arr);
        } else if (arr instanceof long[]) {
            return Arrays.hashCode((long[]) arr);
        } else if (arr instanceof String[]) {
            return Arrays.hashCode((String[]) arr);
        }
        return -1;
    }

    private static boolean arraysEqual(Object left, Object right) {
        if (left instanceof boolean[]) {
            return Arrays.equals((boolean[]) left, (boolean[]) right);
        } else if (left instanceof double[]) {
            return Arrays.equals((double[]) left, (double[]) right);
        } else if (left instanceof int[]) {
            return Arrays.equals((int[]) left, (int[]) right);
        } else if (left instanceof long[]) {
            return Arrays.equals((long[]) left, (long[]) right);
        } else if (left instanceof String[]) {
            return Arrays.equals((String[]) left, (String[]) right);
        }
        return false;
    }

    /** Check whether the provided PersistableBundles are equal. */
    public static boolean isEqual(
            @Nullable PersistableBundle left, @Nullable PersistableBundle right) {
        // Check for pointer equality and null equality.
        if (Objects.equals(left, right)) {
            return true;
        }
        if (Objects.isNull(left) != Objects.isNull(right)) {
            return false;
        }
        if (!left.keySet().equals(right.keySet())) {
            return false;
        }

        for (String key : left.keySet()) {
            Object leftVal = left.get(key);
            Object rightVal = right.get(key);

            if (Objects.equals(leftVal, rightVal)) {
                continue;
            } else if (!Objects.equals(leftVal.getClass(), rightVal.getClass())) {
                // If classes are different, not equal by definition.
                return false;
            }

            if (leftVal instanceof PersistableBundle) {
                if (!isEqual((PersistableBundle) leftVal, (PersistableBundle) rightVal)) {
                    return false;
                }
            } else if (leftVal.getClass().isArray()) {
                if (!arraysEqual(leftVal, rightVal)) {
                    return false;
                }
            } else {
                if (!Objects.equals(leftVal, rightVal)) {
                    return false;
                }
            }
        }
        return true;
    }
}
