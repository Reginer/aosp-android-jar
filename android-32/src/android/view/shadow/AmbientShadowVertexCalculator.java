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

import android.view.math.Math3DHelper;

/**
 * Generates vertices, colours, and indices required for ambient shadow. Ambient shadows are
 * assumed to be raycasted from the centroid of the polygon, and reaches upto a ratio based on
 * the polygon's z-height.
 */
class AmbientShadowVertexCalculator {

    private final float[] mVertex;
    private final float[] mColor;
    private final int[] mIndex;
    private final AmbientShadowConfig mConfig;

    public AmbientShadowVertexCalculator(AmbientShadowConfig config) {
        mConfig = config;

        int size = mConfig.getPolygon().length / 3;

        mVertex = new float[size * 2 * 2];
        mColor = new float[size * 2 * 4];
        mIndex = new int[(size * 3 - 2) * 3];
    }

    /**
     * Generates ambient shadow triangulation using configuration provided
     */
    public void generateVertex() {
        float[] polygon = mConfig.getPolygon();
        float cx = mConfig.getLightSourcePosition()[0];
        float cy = mConfig.getLightSourcePosition()[1];

        int polygonLength = polygon.length/3;

        float opacity = .8f * (0.5f / (mConfig.getEdgeScale() / 10f));

        int trShift = 0;
        for (int i = 0; i < polygonLength; ++i, trShift += 6) {
            int shift = i * 4;
            int colorShift = i * 8;
            int idxShift = i * 2;

            float px = polygon[3 * i + 0];
            float py = polygon[3 * i + 1];
            mVertex[shift + 0] = px;
            mVertex[shift + 1] = py;

            // TODO: I do not really understand this but this matches the previous behavior.
            // The weird bit is that for outlines with low elevation the ambient shadow is
            // entirely drawn underneath the shadow caster. This is most probably incorrect
            float h = polygon[3 * i + 2] * mConfig.getShadowBoundRatio();

            mVertex[shift + 2] = cx + h * (px - cx);
            mVertex[shift + 3] = cy + h * (py - cy);

            mColor[colorShift + 3] = opacity;

            mIndex[trShift + 0] = idxShift + 0;
            mIndex[trShift + 1] = idxShift + 1;
            mIndex[trShift + 2] = idxShift + 2;

            mIndex[trShift + 3] = idxShift + 1;
            mIndex[trShift + 4] = idxShift + 2;
            mIndex[trShift + 5] = idxShift + 3;
        }
        // cycle back to the front
        mIndex[trShift - 1] = 1;
        mIndex[trShift - 2] = 0;
        mIndex[trShift - 4] = 0;

        // Filling the shadow right under the outline. Can we skip that?
        for (int i = 1; i < polygonLength - 1; ++i, trShift += 3) {
            mIndex[trShift + 0] = 0;
            mIndex[trShift + 1] = 2 * i;
            mIndex[trShift + 2] = 2 * (i+1);
        }
    }

    public int[] getIndex() {
        return mIndex;
    }

    /**
     * @return list of vertices in 2d in format : {x1, y1, x2, y2 ...}
     */
    public float[] getVertex() {
        return mVertex;
    }

    public float[] getColor() {
        return mColor;
    }
}
