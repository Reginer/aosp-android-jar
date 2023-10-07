/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.clockwork.displayoffload;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.clockwork.ArrayUtils;
import com.android.clockwork.power.AmbientConfig;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class that handles all brightness offload related logic.
 */
public class BrightnessOffloadController implements AmbientConfig.Listener {
    private static final String TAG = "DOBrightness";
    private static final boolean DEBUG = Build.IS_DEBUGGABLE;

    private static final float DEFAULT_BRIGHTNESS_DOZE_SCREEN_FACTOR = 1f;

    private final Object mBrightnessOperationLock = new Object();

    private final ContentResolver mContentResolver;
    private final SensorManager mSensorManager;
    private final HalAdapter mHalAdapter;
    private final Handler mBackgroundHandler;
    private final ContentObserver mSettingsObserver;

    private final int[] mAutoBrightnessLevels;
    private final int[] mAutoBrightnessLcdBacklightValuesNormal;
    private final int[] mAutoBrightnessLcdBacklightValuesDoze;
    private final int mScreenBrightnessSettingDefault;
    private final int mScreenBrightnessDozeDefault;
    private final float mScreenAutoBrightnessDozeScaleFactor;
    private final int mMaxBrightness;

    // AmbientConfig related
    private AmbientConfig mAmbientConfig;
    private boolean mBrightenOnWristTilt;
    private boolean mAmbientEnabled;

    private float mBrightnessDozeScreenFactor;

    BrightnessOffloadController(ContentResolver contentResolver, Resources baseResources,
            SensorManager sensorManager, Handler backgroundHandler, HalAdapter halAdapter) {
        this.mContentResolver = contentResolver;
        this.mBackgroundHandler = backgroundHandler;
        this.mHalAdapter = halAdapter;
        this.mSensorManager = sensorManager;

        // Read values from frameworks/base resources (possibly set per OEM via overlay)
        mAutoBrightnessLevels = baseResources.getIntArray(R.array.config_autoBrightnessLevels);
        mAutoBrightnessLcdBacklightValuesNormal = baseResources.getIntArray(
                R.array.config_autoBrightnessLcdBacklightValues);
        mAutoBrightnessLcdBacklightValuesDoze = baseResources.getIntArray(
                R.array.config_autoBrightnessLcdBacklightValues_doze);
        mScreenBrightnessSettingDefault = baseResources.getInteger(
                R.integer.config_screenBrightnessSettingDefault);
        mScreenBrightnessDozeDefault = baseResources.getInteger(
                R.integer.config_screenBrightnessDoze);
        mScreenAutoBrightnessDozeScaleFactor = baseResources.getFraction(
                R.fraction.config_screenAutoBrightnessDozeScaleFactor, 1, 1);
        mMaxBrightness = baseResources.getInteger(R.integer.config_screenBrightnessSettingMaximum);
        mBrightnessDozeScreenFactor = DEFAULT_BRIGHTNESS_DOZE_SCREEN_FACTOR;

        mSettingsObserver = new ContentObserver(mBackgroundHandler) {
            // Always run on background handler
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if (DEBUG) {
                    Log.d(TAG, "Brightness settings changed, reloading brightness.");
                }
                loadBrightness();
            }
        };
    }

    /** Load and initialize IDisplayOffload with "bright" and "dim" sets of brightnesses */
    void loadBrightness() {
        synchronized (mBrightnessOperationLock) {
            loadBrightnessLocked();
        }
    }

    /** Load and initialize IDisplayOffload with "bright" and "dim" sets of brightnesses */
    void loadBrightnessLocked() {
        Log.i(TAG, "loadBrightnessLocked()");

        int screenBrightnessDoze = mScreenBrightnessDozeDefault;
        boolean screenBrightnessLockActive = false;

        // Read user configured preferences
        ContentResolver resolver = mContentResolver;
        int screenBrightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS,
                mScreenBrightnessSettingDefault);
        int screenBrightnessMode = Settings.System.getInt(resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE, 0);

        int[] autoBrightnessLcdBacklightValues = mAutoBrightnessLcdBacklightValuesNormal;

        // Determine whether ALS is supported and should be used. Assume it should be used,
        // then check for conditions indicating it should be disabled.
        Sensor lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        final boolean shouldUseAls;
        if (lightSensor == null) {
            Log.d(TAG, "No ALS sensor. Offload ALS won't be used.");
            shouldUseAls = false;
        } else if (autoBrightnessLcdBacklightValues == null
                || autoBrightnessLcdBacklightValues.length <= 1) {
            Log.d(TAG, "Resource configuration doesn't include valid brightness values. "
                    + "Offload ALS won't be used.");
            shouldUseAls = false;
        } else if (mAutoBrightnessLevels == null
                || autoBrightnessLcdBacklightValues.length != mAutoBrightnessLevels.length + 1) {
            Log.d(TAG, "Resource configuration doesn't include valid brightness thresholds. "
                    + "Offload ALS won't be used.");
            shouldUseAls = false;
        } else if (screenBrightnessMode != Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            Log.d(TAG, "User has configured manual brightness. Offload ALS won't be used.");
            shouldUseAls = false;
        } else {
            shouldUseAls = true;
        }

        // If we are using ALS then don't listen for brightness changes
        mBackgroundHandler.post(() -> registerForBrightnessConfigChanges(
                /* shouldListenForBrightnessChange= */ !shouldUseAls));

        Log.d(TAG, "screenBrightness = " + screenBrightness);
        ArrayList<Short> brightnessValuesBright;
        ArrayList<Short> brightnessValuesDim;

        final boolean isAmbientEnabled = mAmbientEnabled;

        // Populate rising/falling thresholds as well as the bright/dim brightness values
        ArrayList<Short> alsThresholds;
        if (shouldUseAls) {
            float factor = screenBrightnessLockActive ? 1 : mScreenAutoBrightnessDozeScaleFactor;
            alsThresholds = ArrayUtils.intArrayToShortArrayList(mAutoBrightnessLevels);
            brightnessValuesBright = ArrayUtils.intArrayToShortArrayList(
                    autoBrightnessLcdBacklightValues);
            if (!isAmbientEnabled) {
                brightnessValuesDim = ArrayUtils.createMonotonicShortArray(
                        brightnessValuesBright.size(), 0);
            } else if (mAutoBrightnessLcdBacklightValuesDoze.length == brightnessValuesBright.size()
                    && !screenBrightnessLockActive) {
                brightnessValuesDim = ArrayUtils.intArrayToShortArrayList(
                        mAutoBrightnessLcdBacklightValuesDoze);
            } else {
                brightnessValuesDim = new ArrayList<>(autoBrightnessLcdBacklightValues.length);
                for (int value : autoBrightnessLcdBacklightValues) {
                    brightnessValuesDim.add((short) (value * factor));
                }
            }
        } else {
            int brightnessValueDim =
                    screenBrightnessLockActive ? screenBrightness
                            : screenBrightnessDoze;

            brightnessValueDim = (int) Math.min(
                    brightnessValueDim * mBrightnessDozeScreenFactor,
                    mMaxBrightness
            );

            screenBrightness = (int) Math.min(
                    screenBrightness * mBrightnessDozeScreenFactor,
                    mMaxBrightness
            );

            alsThresholds = ArrayUtils.intArrayToShortArrayList(new int[]{});
            brightnessValuesBright = ArrayUtils.intArrayToShortArrayList(
                    new int[]{screenBrightness});
            brightnessValuesDim = ArrayUtils.intArrayToShortArrayList(
                    new int[]{isAmbientEnabled ? brightnessValueDim : 0});
        }

        try {
            mHalAdapter.setBrightnessConfiguration(mBrightenOnWristTilt, alsThresholds,
                    brightnessValuesDim, brightnessValuesBright);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to send updated user settings.", e);
        }
    }

    void setBrightnessDozeScreenFactor(float factor) {
        mBrightnessDozeScreenFactor = factor;
    }

    private Sensor findLightSensor(String sensorType) {
        if (!TextUtils.isEmpty(sensorType)) {
            List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor sensor : sensors) {
                if (sensorType.equals(sensor.getStringType())) {
                    return sensor;
                }
            }
        }
        return mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    // TODO(b/240342455): Refactor this call into 2 parts to avoid re-registering necessary
    //  listeners
    private void registerForBrightnessConfigChanges(boolean shouldListenForBrightnessChange) {
        mContentResolver.unregisterContentObserver(mSettingsObserver);
        mContentResolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), true,
                mSettingsObserver);
        if (shouldListenForBrightnessChange) {
            mContentResolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true,
                    mSettingsObserver);
        }
    }

    @Override
    public void onAmbientConfigChanged() {
        mAmbientEnabled = mAmbientConfig.isAmbientEnabled();
        mBrightenOnWristTilt = mAmbientConfig.isUserTiltToBright();
        loadBrightness();
    }

    /** Notify brightness offload controller when HAL restarts. */
    public void onHalRestart() {
        loadBrightness();
    }

    public void onBootComplete() {
        onBootComplete(new AmbientConfig(mContentResolver));
    }

    @VisibleForTesting
    void onBootComplete(AmbientConfig ambientConfig) {
        mAmbientConfig = ambientConfig;
        mAmbientConfig.register();
        onAmbientConfigChanged();
        mAmbientConfig.addListener(this);
    }

}
