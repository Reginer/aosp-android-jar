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

package com.android.i18n.util;

import java.util.Objects;

/**
 * Writes event into the system trace buffer, and exactly like {@link android.os.Trace}.
 * However, ICU4J can't be compiled with the framework class due to the circular dependency.
 * ICU has its our own java class to write the trace event via libcutils.
 *
 * @hide
 */
public class ATrace {

    public static void traceBegin(String event) {
        Objects.requireNonNull(event);
        nativeTraceBegin(event);
    }

    public static void traceEnd() {
        nativeTraceEnd();
    }

    private static native void nativeTraceBegin(String methodName);

    private static native void nativeTraceEnd();

}
