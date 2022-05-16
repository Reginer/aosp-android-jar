/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.setupwizardlib.util;

import android.content.Context;

import com.android.car.setupwizardlib.R;

/**
 * Helper class for determining which orientation mode the device is in (narrow or wide) so that
 * layouts can be adjusted accordingly.
 */
public class CarOrientationHelper {

    /**
     * Will return {@code true} if the context is in narrow mode.
     */
    public static boolean isNarrowOrientation(Context context) {
        return context.getResources().getBoolean(R.bool.is_layout_narrow);
    }
}
