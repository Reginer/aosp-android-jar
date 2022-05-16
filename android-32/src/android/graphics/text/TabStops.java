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

package android.graphics.text;

import android.annotation.Nullable;

// Based on the native implementation of TabStops in
// frameworks/base/core/jni/android_text_StaticLayout.cpp revision b808260
public class TabStops {
    @Nullable
    private float[] mStops;
    private final float mTabWidth;

    public TabStops(@Nullable float[] stops, float defaultTabWidth) {
        mTabWidth = defaultTabWidth;
        mStops = stops;
    }

    public float width(float widthSoFar) {
        if (mStops != null) {
            for (float f : mStops) {
                if (f > widthSoFar) {
                    return f;
                }
            }
        }
        // find the next tabStop after widthSoFar.
        return ((widthSoFar + mTabWidth) / mTabWidth) * mTabWidth;
    }
}
