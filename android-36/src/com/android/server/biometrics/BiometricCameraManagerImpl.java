/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.biometrics;

import static android.hardware.SensorPrivacyManager.Sensors.CAMERA;

import android.annotation.NonNull;
import android.hardware.SensorPrivacyManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

public class BiometricCameraManagerImpl implements BiometricCameraManager {

    private static final String TAG = "BiometricCameraManager";

    private final CameraManager mCameraManager;
    private final SensorPrivacyManager mSensorPrivacyManager;
    private final ConcurrentHashMap<String, Boolean> mIsCameraAvailable = new ConcurrentHashMap<>();

    private final CameraManager.AvailabilityCallback mCameraAvailabilityCallback =
            new CameraManager.AvailabilityCallback() {
                @Override
                public void onCameraAvailable(@NonNull String cameraId) {
                    mIsCameraAvailable.put(cameraId, true);
                }

                @Override
                public void onCameraUnavailable(@NonNull String cameraId) {
                    mIsCameraAvailable.put(cameraId, false);
                }
            };

    public BiometricCameraManagerImpl(@NonNull CameraManager cameraManager,
            @NonNull SensorPrivacyManager sensorPrivacyManager) {
        mCameraManager = cameraManager;
        mSensorPrivacyManager = sensorPrivacyManager;
        mCameraManager.registerAvailabilityCallback(mCameraAvailabilityCallback, null);
    }

    @Override
    public boolean isAnyCameraUnavailable() {
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                if (!mIsCameraAvailable.getOrDefault(cameraId, true)) {
                    return true;
                }
            }
            return false;
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera exception thrown when trying to determine availability: ", e);
            //If face HAL is unable to get access to a camera, it will return an error.
            return false;
        }
    }

    @Override
    public boolean isCameraPrivacyEnabled() {
        return mSensorPrivacyManager != null && mSensorPrivacyManager
                .isSensorPrivacyEnabled(SensorPrivacyManager.TOGGLE_TYPE_SOFTWARE, CAMERA);
    }
}
