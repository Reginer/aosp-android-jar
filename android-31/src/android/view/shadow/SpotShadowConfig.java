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
 * Model for spot shadow rendering. Assumes single light, single object.
 */
class SpotShadowConfig {

    // No need to be final but making it immutable for now.
    private final int mLightRadius;


    // No need to be final but making it immutable for now.
    private final float[] mPoly;
    private final int mPolyLength;

    private float[] mLightCoord;

    private final float mShadowStrength;

    private SpotShadowConfig(SpotShadowConfig.Builder builder) {
        mLightRadius = builder.mLightRadius;
        mPoly = builder.mPoly;
        mPolyLength = builder.mPolyLength;

        mLightCoord = new float[3];
        mLightCoord[0] = builder.mLightX;
        mLightCoord[1] = builder.mLightY;
        mLightCoord[2] = builder.mLightHeight;
        mShadowStrength = builder.mShadowStrength;
    }

    /**
     * @return size of the light source radius (light source is always generated as a circular shape)
     */
    public int getLightRadius() {
        return mLightRadius;
    }

    /**
     * @return object that casts shadow. xyz coordinates.
     */
    public float[] getPoly() {
        return mPoly;
    }

    /**
     * @return # of vertices in the object {@link #getPoly()} that casts shadow.
     */
    public int getPolyLength() {
        return mPolyLength;
    }

    /**
     * Update the light source coord.
     * @param x - horizontal coordinate
     * @param y - vertical coordinate
     */
    public void setLightCoord(float x, float y) {
        mLightCoord[0] = x;
        mLightCoord[1] = y;
    }

    /**
     * @return shadow intensity from 0 to 1
     */
    public float getShadowStrength() {
        return mShadowStrength;
    }

    public float[] getLightCoord() {
        return mLightCoord;
    }

    public static class Builder {

        // No need to be final but making it immutable for now.
        private int mLightRadius;

        // No need to be final but making it immutable for now.
        private float[] mPoly;
        private int mPolyLength;

        private float mLightX;
        private float mLightY;
        private float mLightHeight;

        private float mShadowStrength;

        /**
         * @param shadowStrength from 0 to 1
         */
        public Builder setShadowStrength(float shadowStrength) {
            this.mShadowStrength = shadowStrength;
            return this;
        }

        public Builder setLightRadius(int mLightRadius) {
            this.mLightRadius = mLightRadius;
            return this;
        }

        public Builder setPolygon(float[] poly, int polyLength) {
            this.mPoly = poly;
            this.mPolyLength = polyLength;
            return this;
        }

        public Builder setLightCoord(float lightX, float lightY, float lightHeight) {
            this.mLightX = lightX;
            this.mLightY = lightY;
            this.mLightHeight = lightHeight;
            return this;
        }

        public SpotShadowConfig build() {
            return new SpotShadowConfig(this);
        }
    }

}