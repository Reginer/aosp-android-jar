/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.graphics.animation;

import android.animation.TimeInterpolator;
import android.util.TimeUtils;
import android.view.Choreographer;

/**
 * Interpolator that builds a lookup table to use. This is a fallback for
 * building a native interpolator from a TimeInterpolator that is not marked
 * with {@link HasNativeInterpolator}
 *
 * This implements TimeInterpolator to allow for easier interop with Animators
 * @hide
 */
@HasNativeInterpolator
public class FallbackLUTInterpolator implements NativeInterpolator, TimeInterpolator {

    // If the duration of an animation is more than 300 frames, we cap the sample size to 300.
    private static final int MAX_SAMPLE_POINTS = 300;
    private TimeInterpolator mSourceInterpolator;
    private final float[] mLut;

    /**
     * Used to cache the float[] LUT for use across multiple native
     * interpolator creation
     */
    public FallbackLUTInterpolator(TimeInterpolator interpolator, long duration) {
        mSourceInterpolator = interpolator;
        mLut = createLUT(interpolator, duration);
    }

    private static float[] createLUT(TimeInterpolator interpolator, long duration) {
        long frameIntervalNanos = Choreographer.getInstance().getFrameIntervalNanos();
        int animIntervalMs = (int) (frameIntervalNanos / TimeUtils.NANOS_PER_MS);
        // We need 2 frame values as the minimal.
        int numAnimFrames = Math.max(2, (int) Math.ceil(((double) duration) / animIntervalMs));
        numAnimFrames = Math.min(numAnimFrames, MAX_SAMPLE_POINTS);
        float[] values = new float[numAnimFrames];
        float lastFrame = numAnimFrames - 1;
        for (int i = 0; i < numAnimFrames; i++) {
            float inValue = i / lastFrame;
            values[i] = interpolator.getInterpolation(inValue);
        }
        return values;
    }

    @Override
    public long createNativeInterpolator() {
        return NativeInterpolatorFactory.createLutInterpolator(mLut);
    }

    /**
     * Used to create a one-shot float[] LUT & native interpolator
     */
    public static long createNativeInterpolator(TimeInterpolator interpolator, long duration) {
        float[] lut = createLUT(interpolator, duration);
        return NativeInterpolatorFactory.createLutInterpolator(lut);
    }

    @Override
    public float getInterpolation(float input) {
        return mSourceInterpolator.getInterpolation(input);
    }
}
