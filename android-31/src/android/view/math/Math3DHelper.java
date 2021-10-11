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

package android.view.math;

public class Math3DHelper {

    private Math3DHelper() { }

    public final static int min(int x1, int x2, int x3) {
        return (x1 > x2) ? ((x2 > x3) ? x3 : x2) : ((x1 > x3) ? x3 : x1);
    }

    public final static int max(int x1, int x2, int x3) {
        return (x1 < x2) ? ((x2 < x3) ? x3 : x2) : ((x1 < x3) ? x3 : x1);
    }

    /**
     * @return Rect bound of flattened (ignoring z). LTRB
     * @param dimension - 2D or 3D
     */
    public static float[] flatBound(float[] poly, int dimension) {
        int polySize = poly.length/dimension;
        float left = poly[0];
        float right = poly[0];
        float top = poly[1];
        float bottom = poly[1];

        for (int i = 0; i < polySize; i++) {
            float x = poly[i * dimension + 0];
            float y = poly[i * dimension + 1];

            if (left > x) {
                left = x;
            } else if (right < x) {
                right = x;
            }

            if (top > y) {
                top = y;
            } else if (bottom < y) {
                bottom = y;
            }
        }
        return new float[]{left, top, right, bottom};
    }

    /**
     * Translate the polygon to x and y
     * @param dimension in what dimension is polygon represented (supports 2 or 3D).
     */
    public static void translate(float[] poly, float translateX, float translateY, int dimension) {
        int polySize = poly.length/dimension;

        for (int i = 0; i < polySize; i++) {
            poly[i * dimension + 0] += translateX;
            poly[i * dimension + 1] += translateY;
        }
    }

}

