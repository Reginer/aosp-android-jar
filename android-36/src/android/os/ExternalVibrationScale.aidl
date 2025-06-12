/**
 * Copyright (c) 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os;

/**
 * ExternalVibrationScale holds the vibration scale level and adaptive haptics scale. These
 * can be used to scale external vibrations.
 *
 * @hide
 */
parcelable ExternalVibrationScale {
    @Backing(type="int")
    enum ScaleLevel {
        SCALE_MUTE = -100,
        SCALE_VERY_LOW = -2,
        SCALE_LOW = -1,
        SCALE_NONE = 0,
        SCALE_HIGH = 1,
        SCALE_VERY_HIGH = 2
    }

    // TODO(b/345186129): remove this once we finish migrating to scale factor.
    /**
     * The scale level that will be applied to external vibrations.
     */
    ScaleLevel scaleLevel = ScaleLevel.SCALE_NONE;

    /**
     * The scale factor that will be applied to external vibrations.
     *
     * Values in (0,1) will scale down the vibrations, values > 1 will scale up vibrations within
     * hardware limits. A zero scale factor indicates the external vibration should be muted.
     *
     * TODO(b/345186129): update this once we finish migrating, negative should not be expected.
     * Negative values should be ignored in favour of the legacy ScaleLevel.
     */
    float scaleFactor = -1f; // undefined

    /**
     * The adaptive haptics scale that will be applied to external vibrations.
     */
    float adaptiveHapticsScale = 1f;
}
