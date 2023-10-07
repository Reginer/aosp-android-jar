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

package com.android.clockwork.display.burninprotection;

import static com.android.internal.R.string.config_displayLightSensorType;
import static com.android.wearable.resources.R.integer.config_burnInProtectionMaskThreshold;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.clockwork.common.WearResourceUtil;
import com.android.internal.annotations.VisibleForTesting;

import java.util.List;

import javax.annotation.Nullable;

/**
 * This class provides the availability to enable burn-in protection by reading display brightness.
 */
public class BrightnessObserver implements SensorEventListener {

    private static final String TAG = "BurnInProtector_BO";
    private static final boolean DEBUG = BurnInProtector.DEBUG;

    private final ContentResolver mContentResolver;
    private final SensorManager mSensorManager;
    private final Handler mHandler;
    private final int mBurnInMaskThreshold;
    private final Sensor mLightSensor;
    private boolean mLightSensorEnabled;
    private boolean mShouldEnableBurnInProtector;

    BrightnessObserver(Context context) {
        this(context, context.getContentResolver(), context.getSystemService(SensorManager.class),
                Handler.getMain(), WearResourceUtil.getWearableResources(context));
    }

    @VisibleForTesting
    BrightnessObserver(Context context, ContentResolver contentResolver,
            SensorManager sensorManager, Handler handler, Resources wearResources) {
        mContentResolver = contentResolver;
        mSensorManager = sensorManager;
        mHandler = handler;
        mBurnInMaskThreshold = wearResources != null
                ? wearResources.getInteger(config_burnInProtectionMaskThreshold)
                : 0;
        mLightSensor = findDisplayLightSensor(context.getString(config_displayLightSensorType));
    }

    /**
     * Start to observe display brightness. This is called when the device is in
     * STATE_DOZE_SUSPENDED.
     */
    void observe() {
        boolean isAutomaticBrightnessMode = Settings.System.getInt(mContentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;

        // Register light sensor if the device in the automatic brightness mode
        if (isAutomaticBrightnessMode) {
            if (!mLightSensorEnabled) {
                mLightSensorEnabled = true;
                mSensorManager.registerListener(this, mLightSensor,
                        SensorManager.SENSOR_DELAY_NORMAL,
                        mHandler);
            }
        } else {
            int screenBrightness = Settings.System.getInt(mContentResolver,
                    Settings.System.SCREEN_BRIGHTNESS, 0);
            // Set availability true if screen brightness is lower than half of the maximum
            // screen brightness.
            mShouldEnableBurnInProtector = screenBrightness < PowerManager.BRIGHTNESS_ON / 2;
        }
    }

    /**
     * Stop observing screen brightness when the device is not in STATE_DOZE_SUSPENDED
     */
    void tearDown() {
        if (mLightSensorEnabled) {
            mLightSensorEnabled = false;
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!mLightSensorEnabled || event == null || event.values == null
                || event.values.length == 0) {
            return;
        }

        final float lux = event.values[0];
        if (DEBUG) {
            Log.d(TAG, "onSensorChanged: lux=" + lux);
        }

        // Set availability true if screen brightness is lower threshold.
        mShouldEnableBurnInProtector = lux < mBurnInMaskThreshold;

        // Sample light sensor only 1 time.
        tearDown();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Don't care
    }

    @Nullable
    private Sensor findDisplayLightSensor(final String sensorType) {
        if (TextUtils.isEmpty(sensorType)) {
            return mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        final List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensors) {
            if (sensorType.equals(sensor.getStringType())) {
                return sensor;
            }
        }

        return mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    boolean shouldEnableBurnInProtector() {
        return mShouldEnableBurnInProtector;
    }
}
