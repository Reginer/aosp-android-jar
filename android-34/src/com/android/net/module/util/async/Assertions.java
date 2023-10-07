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

package com.android.net.module.util.async;

import android.os.Build;

/**
 * Implements basic assert functions for runtime error-checking.
 *
 * @hide
 */
public final class Assertions {
    public static final boolean IS_USER_BUILD = "user".equals(Build.TYPE);

    public static void throwsIfOutOfBounds(int totalLength, int pos, int len) {
        if (!IS_USER_BUILD && ((totalLength | pos | len) < 0 || pos > totalLength - len)) {
            throw new ArrayIndexOutOfBoundsException(
                "length=" + totalLength + "; regionStart=" + pos + "; regionLength=" + len);
        }
    }

    public static void throwsIfOutOfBounds(byte[] buffer, int pos, int len) {
        throwsIfOutOfBounds(buffer != null ? buffer.length : 0, pos, len);
    }

    private Assertions() {}
}
