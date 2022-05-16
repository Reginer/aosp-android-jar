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
 * Generates ambient shadow bitmap
 */
class AmbientShadowTriangulator {

    private final AmbientShadowConfig mShadowConfig;
    private final AmbientShadowVertexCalculator mCalculator;

    private boolean mValid;

    public AmbientShadowTriangulator(AmbientShadowConfig shadowConfig) {
        mShadowConfig = shadowConfig;

        mCalculator = new AmbientShadowVertexCalculator(mShadowConfig);
    }

    /**
     * Populate vertices and fill the triangle buffers.
     */
    public void triangulate() {
        try {
            mCalculator.generateVertex();
            mValid = true;
        } catch (IndexOutOfBoundsException|ArithmeticException mathError) {
            Bridge.getLog().warning(ILayoutLog.TAG_INFO,  "Arithmetic error while drawing " +
                            "ambient shadow",
                    null, mathError);
        } catch (Exception ex) {
            Bridge.getLog().warning(ILayoutLog.TAG_INFO,  "Error while drawing shadow",
                    null, ex);
        }
    }

    public boolean isValid() {
        return mValid;
    }

    public float[] getVertices() { return mCalculator.getVertex(); }

    public int[] getIndices() { return mCalculator.getIndex(); }

    public float[] getColors() { return mCalculator.getColor(); }
}
