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

package android.graphics;

import com.android.tools.layoutlib.annotations.LayoutlibDelegate;

/**
 * Delegate implementing the native methods of android.graphics.Color
 *
 * Through the layoutlib_create tool, the original native methods of Color have been replaced
 * by calls to methods of the same name in this delegate class.
 */
public class Color_Delegate {

    @LayoutlibDelegate
    /*package*/ static void nativeRGBToHSV(int red, int greed, int blue, float hsv[]) {
        java.awt.Color.RGBtoHSB(red, greed, blue, hsv);
        hsv[0] = hsv[0] * 360;
    }

    @LayoutlibDelegate
    /*package*/ static int nativeHSVToColor(int alpha, float hsv[]) {
        java.awt.Color rgb = new java.awt.Color(java.awt.Color.HSBtoRGB(hsv[0] / 360, pin(hsv[1]), pin(hsv[2])));
        return Color.argb(alpha, rgb.getRed(), rgb.getGreen(), rgb.getBlue());
    }

    private static float pin(float value) {
        return Math.max(0, Math.min(1, value));
    }
}
