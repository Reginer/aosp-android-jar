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

/**
 * Model for ambient shadow rendering. Assumes light sources from centroid of the object.
 */
class AmbientShadowConfig {

    private final float mEdgeScale;
    private final float mShadowBoundRatio;
    private final float mShadowStrength;

    private final float[] mPolygon;
    private float[] mLightSourcePosition;

    private AmbientShadowConfig(Builder builder) {
        mEdgeScale = builder.mEdgeScale;
        mShadowBoundRatio = builder.mShadowBoundRatio;
        mShadowStrength = builder.mShadowStrength;
        mPolygon = builder.mPolygon;
        mLightSourcePosition = builder.mLightSourcePosition;
    }

    /**
     * Returns scales intensity of the edge of the shadow (opacity) [0-100]
     */
    public float getEdgeScale() {
        return mEdgeScale;
    }

    /**
     * Returns scales the area (in xy) of the shadow [0-1]
     */
    public float getShadowBoundRatio() {
        return mShadowBoundRatio;
    }

    /**
     * Returns scales the intensity of the entire shadow (opacity) [0-1]
     */
    public float getShadowStrength() {
        return mShadowStrength;
    }

    /**
     * Returns opaque polygon to cast shadow
     */
    public float[] getPolygon() {
        return mPolygon;
    }

    /**
     * Returns 2D position of the light source
     */
    public float[] getLightSourcePosition() {
        return mLightSourcePosition;
    }

    public static class Builder {

        private float mEdgeScale;
        private float mShadowBoundRatio;
        private float mShadowStrength;

        private float[] mPolygon;
        private float[] mLightSourcePosition;

        public Builder setEdgeScale(float edgeScale) {
            mEdgeScale = edgeScale;
            return this;
        }

        public Builder setShadowBoundRatio(float shadowBoundRatio) {
            mShadowBoundRatio = shadowBoundRatio;
            return this;
        }

        public Builder setShadowStrength(float shadowStrength) {
            mShadowStrength = shadowStrength;
            return this;
        }

        public Builder setPolygon(float[] polygon) {
            mPolygon = polygon;
            return this;
        }

        public Builder setLightSourcePosition(float x, float y) {
            mLightSourcePosition = new float[] { x, y };
            return this;
        }

        public AmbientShadowConfig build() {
            return new AmbientShadowConfig(this);
        }
    }
}
