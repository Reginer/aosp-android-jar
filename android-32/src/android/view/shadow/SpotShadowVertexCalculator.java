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

package android.view.shadow;

import android.graphics.Rect;
import android.view.math.Math3DHelper;

/**
 * Generates the vertices required for spot shadow and all other shadow-related rendering.
 */
class SpotShadowVertexCalculator {

    private SpotShadowVertexCalculator() { }

    /**
     * Create evenly distributed circular light source points from x and y (on flat z plane).
     * This is useful for ray tracing the shadow points later. Format : (x1,y1,z1,x2,y2,z2 ...)
     *
     * @param radius - radius of the light source
     * @param x - center X of the light source
     * @param y - center Y of the light source
     * @param height - how high (z depth) the light should be
     * @return float points (x,y,z) of light source points.
     */
    public static float[] calculateLight(float radius, float x, float y, float height) {
        float[] ret = new float[4 * 3];
        // bottom
        ret[0] = x;
        ret[1] = y + radius;
        ret[2] = height;
        // left
        ret[3] = x - radius;
        ret[4] = y;
        ret[5] = height;
        // top
        ret[6] = x;
        ret[7] = y - radius;
        ret[8] = height;
        // right
        ret[9] = x + radius;
        ret[10] = y;
        ret[11] = height;

        return ret;
    }

    /**
     * @param polyLength - length of the outline polygon
     * @return size required for shadow vertices mData array based on # of vertices in the
     * outline polygon
     */
    public static int[] getStripSizes(int polyLength){
        return new int[] { ((polyLength + 4) / 8) * 16 + 2, 4};
    }

    /**
     * Generate shadow vertices based on params. Format : (x1,y1,z1,x2,y2,z2 ...)
     * Precondition : Light poly must be evenly distributed on a flat surface
     * Precondition : Poly vertices must be a convex
     * Precondition : Light height must be higher than any poly vertices
     *
     * @param lightPoly - Vertices of a light source.
     * @param poly - Vertices of opaque object casting shadow
     * @param polyLength - Size of the vertices
     * @param strength - Strength of the shadow overall [0-1]
     * @param retstrips - Arrays of triplets, each triplet represents a point, thus every array to
     * be filled in format : {x1, y1, z1, x2, y2, z2, ...},
     * every 3 consecutive triplets constitute a triangle to fill, namely [t1, t2, t3], [t2, t3,
     * t4], ... If at some point [t(n-1), tn, t(n+1)] is no longer a desired a triangle and
     * there are more triangles to draw one can start a new array, hence retstrips is a 2D array.
     */
    public static void calculateShadow(
            float[] lightPoly,
            float[] poly,
            int polyLength,
            float strength,
            float[][] retstrips) {
        float[] outerStrip = retstrips[0];

        // We would like to unify the cases where we have roundrects and rectangles
        int roundedEdgeSegments = ((polyLength == 4) ? 0 : ShadowConstants.SPLICE_ROUNDED_EDGE);
        int sideLength = (roundedEdgeSegments / 2 + 1) * 2;
        float[] umbra = new float[4 * 2 * sideLength];
        int idx = (roundedEdgeSegments + 1) / 2;
        int uShift = 0;
        // If we have even number of segments in rounded corner (0 included), the central point of
        // rounded corner contributes to the hull twice, from 2 different light sources, thus
        // rollBack in that case, otherwise every point contributes only once
        int rollBack = (((polyLength % 8) == 0) ? 0 : 1);
        // Calculate umbra - a hull of all projections
        for (int s = 0; s < 4; ++s) { // 4 sides
            float lx = lightPoly[s * 3 + 0];
            float ly = lightPoly[s * 3 + 1];
            float lz = lightPoly[s * 3 + 2];
            for (int i = 0; i < sideLength; ++i, uShift += 2, ++idx) {
                int shift = (idx % polyLength) * 3;

                float t = lz / (lz - poly[shift + 2]);

                umbra[uShift + 0] = lx - t * (lx - poly[shift + 0]);
                umbra[uShift + 1] = ly - t * (ly - poly[shift + 1]);
            }

            idx -= rollBack;
        }

        idx = roundedEdgeSegments;
        // An array that wil contain top, right, bottom, left coordinate of penumbra
        float[] penumbraRect = new float[4];
        // Calculate penumbra
        for (int s = 0; s < 4; ++s, idx += (roundedEdgeSegments + 1)) { // 4 sides
            int sp = (s + 2) % 4;

            float lz = lightPoly[sp * 3 + 2];

            int shift = (idx % polyLength) * 3;

            float t = lz / (lz - poly[shift + 2]);

            // We are interested in just one coordinate: x or y, depending on the light source
            int c = (s + 1) % 2;
            penumbraRect[s] =
                    lightPoly[sp * 3 + c] - t * (lightPoly[sp * 3 + c] - poly[shift + c]);
        }
        if (penumbraRect[0] > penumbraRect[2]) {
            float tmp = (penumbraRect[0] + penumbraRect[2]) / 2.0f;
            penumbraRect[0] = penumbraRect[2] = tmp;
        }
        if (penumbraRect[3] > penumbraRect[1]) {
            float tmp = (penumbraRect[1] + penumbraRect[3]) / 2.0f;
            penumbraRect[1] = penumbraRect[3] = tmp;
        }

        // Now just connect umbra points (at least 8 of them) with the closest points from
        // penumbra (only 4 of them) to form triangles to fill the entire space between umbra and
        // penumbra
        idx = sideLength * 4 - sideLength / 2;
        int rsShift = 0;
        for (int s = 0; s < 4; ++s) {
            int xidx = (((s + 3) % 4) / 2) * 2 + 1;
            int yidx = (s / 2) * 2;
            float penumbraX = penumbraRect[xidx];
            float penumbraY = penumbraRect[yidx];
            for (int i = 0; i < sideLength; ++i, rsShift += 6, ++idx) {
                int shift = (idx % (sideLength * 4)) * 2;

                outerStrip[rsShift + 0] = umbra[shift + 0];
                outerStrip[rsShift + 1] = umbra[shift + 1];
                outerStrip[rsShift + 3] = penumbraX;
                outerStrip[rsShift + 4] = penumbraY;
                outerStrip[rsShift + 5] = strength;
            }
        }
        // Connect with the beginning
        outerStrip[rsShift + 0] = outerStrip[0];
        outerStrip[rsShift + 1] = outerStrip[1];
        // outerStrip[rsShift + 2] = 0;
        outerStrip[rsShift + 3] = outerStrip[3];
        outerStrip[rsShift + 4] = outerStrip[4];
        outerStrip[rsShift + 5] = strength;

        float[] innerStrip = retstrips[1];
        // Covering penumbra rectangle
        // left, top
        innerStrip[0] = penumbraRect[3];
        innerStrip[1] = penumbraRect[0];
        innerStrip[2] = strength;
        // right, top
        innerStrip[3] = penumbraRect[1];
        innerStrip[4] = penumbraRect[0];
        innerStrip[5] = strength;
        // left, bottom
        innerStrip[6] = penumbraRect[3];
        innerStrip[7] = penumbraRect[2];
        innerStrip[8] = strength;
        // right, bottom
        innerStrip[9] = penumbraRect[1];
        innerStrip[10] = penumbraRect[2];
        innerStrip[11] = strength;
    }
}