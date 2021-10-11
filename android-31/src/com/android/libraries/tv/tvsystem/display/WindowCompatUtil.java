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
 * limitations under the License
 */

package com.android.libraries.tv.tvsystem.display;

import android.view.Window;

public final class WindowCompatUtil {

    /**
     * Specify whether this Window should request the Display it's presented on to switch to the
     * minimal post-processing mode, if the Display supports such mode.
     * Usually minimal post-processing is backed by HDMI ContentType="game" and HDMI Auto
     * Low-Latency Mode. However, different manufacturers may have their own implementations of
     * similar features.
     *
     * @see Window#setPreferMinimalPostProcessing
     * @see DisplayCompatUtil#isMinimalPostProcessingSupported
     */
    public static void setPreferMinimalPostProcessing(Window window, boolean isPreferred) {
        window.setPreferMinimalPostProcessing(isPreferred);
    }

    private WindowCompatUtil() {}
}
