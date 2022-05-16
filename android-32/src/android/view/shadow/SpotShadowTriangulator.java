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

import com.android.ide.common.rendering.api.ILayoutLog;
import com.android.layoutlib.bridge.Bridge;

/**
 * Generate spot shadow bitmap.
 */
class SpotShadowTriangulator {

    private final SpotShadowConfig mShadowConfig;
    private float[][] mStrips;

    public SpotShadowTriangulator(SpotShadowConfig config) {
        mShadowConfig = config;
    }

    /**
     * Populate the shadow bitmap.
     */
    public void triangulate() {
        try {
            float[] lightSources =
                    SpotShadowVertexCalculator.calculateLight(mShadowConfig.getLightRadius(),
                            mShadowConfig.getLightCoord()[0],
                            mShadowConfig.getLightCoord()[1], mShadowConfig.getLightCoord()[2]);


            mStrips = new float[2][];
            int[] sizes = SpotShadowVertexCalculator.getStripSizes(mShadowConfig.getPolyLength());
            for (int i = 0; i < sizes.length; ++i) {
                mStrips[i] = new float[3 * sizes[i]];
            }

            SpotShadowVertexCalculator.calculateShadow(lightSources,
                    mShadowConfig.getPoly(),
                    mShadowConfig.getPolyLength(),
                    mShadowConfig.getShadowStrength(),
                    mStrips);
        } catch (IndexOutOfBoundsException|ArithmeticException mathError) {
            Bridge.getLog().warning(ILayoutLog.TAG_INFO,  "Arithmetic error while drawing " +
                            "spot shadow",
                    null, mathError);
        } catch (Exception ex) {
            Bridge.getLog().warning(ILayoutLog.TAG_INFO,  "Error while drawing shadow",
                    null, ex);
        }
    }
    /**
     * @return true if generated shadow poly is valid. False otherwise.
     */
    public boolean validate() {
        return mStrips != null && mStrips[0].length >= 9;
    }

    public float[][] getStrips() {
        return mStrips;
    }
}
