/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.annotation.NonNull;
import android.provider.DeviceConfig;
import android.util.Slog;

import com.android.internal.os.BackgroundThread;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for maintaining and updating the strengths for biometric sensors. Strengths can only
 * be downgraded from the device's default, and never upgraded.
 */
public class BiometricStrengthController {
    private static final String TAG = "BiometricStrengthController";

    private final BiometricService mService;

    /**
     * Flag stored in the DeviceConfig API: biometric modality strengths to downgrade.
     * This is encoded as a key:value list, separated by comma, e.g.
     *
     * "id1:strength1,id2:strength2,id3:strength3"
     *
     * where strength is one of the values defined in
     * {@link android.hardware.biometrics.BiometricManager.Authenticators}
     *
     * Both id and strength should be int, otherwise Exception will be thrown when parsing and the
     * downgrade will fail.
     */
    private static final String KEY_BIOMETRIC_STRENGTHS = "biometric_strengths";

    private DeviceConfig.OnPropertiesChangedListener mDeviceConfigListener = properties -> {
        if (properties.getKeyset().contains(KEY_BIOMETRIC_STRENGTHS)) {
            updateStrengths();
        }
    };

    public BiometricStrengthController(@NonNull BiometricService service) {
        mService = service;
    }

    public void startListening() {
        DeviceConfig.addOnPropertiesChangedListener(DeviceConfig.NAMESPACE_BIOMETRICS,
                BackgroundThread.getExecutor(), mDeviceConfigListener);
    }

    /**
     * Updates the strengths of authenticators in BiometricService if a matching ID's configuration
     * has been changed.
     */
    public void updateStrengths() {
        final String newValue = DeviceConfig.getString(DeviceConfig.NAMESPACE_BIOMETRICS,
                KEY_BIOMETRIC_STRENGTHS, "null");
        if ("null".equals(newValue) || newValue.isEmpty()) {
            revertStrengths();
        } else {
            updateStrengths(newValue);
        }
    }

    private void updateStrengths(String flags) {
        final Map<Integer, Integer> idToStrength = getIdToStrengthMap(flags);
        if (idToStrength == null) {
            return;
        }

        for (BiometricSensor sensor : mService.mSensors) {
            final int id = sensor.id;
            if (idToStrength.containsKey(id)) {
                final int newStrength = idToStrength.get(id);
                Slog.d(TAG, "updateStrengths: update sensorId=" + id + " to newStrength="
                        + newStrength);
                sensor.updateStrength(newStrength);
            }
        }
    }

    private void revertStrengths() {
        for (BiometricSensor sensor : mService.mSensors) {
            Slog.d(TAG, "updateStrengths: revert sensorId=" + sensor.id + " to oemStrength="
                    + sensor.oemStrength);
            sensor.updateStrength(sensor.oemStrength);
        }
    }

    /**
     * @return a map of <ID, Strength>
     */
    private static Map<Integer, Integer> getIdToStrengthMap(String flags) {
        if (flags == null || flags.isEmpty()) {
            Slog.d(TAG, "Flags are null or empty");
            return null;
        }

        Map<Integer, Integer> map = new HashMap<>();
        try {
            for (String item : flags.split(",")) {
                String[] elems = item.split(":");
                final int id = Integer.parseInt(elems[0]);
                final int strength = Integer.parseInt(elems[1]);
                map.put(id, strength);
            }
        } catch (Exception e) {
            Slog.e(TAG, "Can't parse flag: " + flags);
            map = null;
        }
        return map;
    }
}
