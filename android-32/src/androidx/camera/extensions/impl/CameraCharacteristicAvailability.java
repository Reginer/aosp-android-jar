/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.extensions.impl;

import android.hardware.camera2.CameraCharacteristics;
import android.util.Log;

import java.util.Arrays;

/**
 * A utility class to check the availabilities of camera characteristics.
 */
final class CameraCharacteristicAvailability {
    private static final String TAG = "CharacteristicAbility";

    private CameraCharacteristicAvailability() {
    }

    /**
     * Check if the given white balance mode id is available in the camera characteristics.
     *
     * @param cameraCharacteristics the camera characteristics.
     * @param mode white balance mode id.
     * @return {@code true} if the given white balance mode id is available in the camera
     *                      characteristics.
     * {@code false} otherwise.
     */
    static boolean isWBModeAvailable(CameraCharacteristics cameraCharacteristics,
            int mode) {
        int[] availableModes = cameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
        if (availableModes == null) {
            Log.d(TAG, "No CONTROL_AWB_AVAILABLE_MODES info");
            return false;
        }

        for (int availableMode : availableModes) {
            if (availableMode == mode) {
                return true;
            }
        }
        Log.d(TAG, "wb mode: " + mode + " is not in available list "
                + Arrays.toString(availableModes));
        return false;
    }

    /**
     * Check if the given effect id is available in the camera characteristics.
     *
     * @param cameraCharacteristics the camera characteristics.
     * @param effect the effect id.
     * @return {@code true} if the given effect id is available in the camera characteristics.
     * {@code false} otherwise.
     */
    static boolean isEffectAvailable(CameraCharacteristics cameraCharacteristics,
            int effect) {
        int[] availableEffects = cameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
        if (availableEffects == null) {
            Log.d(TAG, "No CONTROL_AVAILABLE_EFFECTS info");
            return false;
        }

        for (int availableEffect : availableEffects) {
            if (availableEffect == effect) {
                return true;
            }
        }
        Log.d(TAG, "effect: " + effect + " is not in available list "
                + Arrays.toString(availableEffects));
        return false;
    }
}
