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
import android.util.Log;

import java.util.function.BiConsumer;

/**
 * Utility class for logging terrible errors and reporting them for tracking.
 *
 * @hide
 */
public class TerribleErrorLog {

    private static final String TAG = TerribleErrorLog.class.getSimpleName();

    /**
     * Logs a terrible error and reports metrics through a provided statsLog.
     */
    public static void logTerribleError(@NonNull BiConsumer<Integer, Integer> statsLog,
            @NonNull String message, int protoType, int errorType) {
        statsLog.accept(protoType, errorType);
        Log.wtf(TAG, message);
    }
}
