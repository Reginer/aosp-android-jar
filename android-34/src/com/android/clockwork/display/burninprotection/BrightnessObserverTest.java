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

import static com.android.wearable.resources.R.integer.config_burnInProtectionMaskThreshold;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowSensorManager;

@RunWith(RobolectricTestRunner.class)
public class BrightnessObserverTest {

    private BrightnessObserver mBrightnessObserver;
    private ShadowSensorManager mShadowSensorManager;
    @Mock
    private Resources mWearResources;
    private ContentResolver mContentResolver;
    private final int mBurnInMaskThreshold = 5000;
    private SensorEvent mSensorEvent;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Context context = ApplicationProvider.getApplicationContext();
        Handler handler = new Handler(Looper.getMainLooper());
        SensorManager sensorManager = context.getSystemService(SensorManager.class);
        mShadowSensorManager = Shadows.shadowOf(sensorManager);
        mSensorEvent = ShadowSensorManager.createSensorEvent(1);
        Mockito.when(mWearResources.getInteger(config_burnInProtectionMaskThreshold))
                .thenReturn(mBurnInMaskThreshold);
        mContentResolver = context.getContentResolver();
        mBrightnessObserver = new BrightnessObserver(context, mContentResolver, sensorManager,
                handler, mWearResources);
    }

    @Test
    public void autoBrightnessOn_lightSensorHigher_false() {
        setScreenBrightnessMode(true);
        mBrightnessObserver.observe();
        mSensorEvent.values[0] = mBurnInMaskThreshold + 1;
        mShadowSensorManager.sendSensorEventToListeners(mSensorEvent);
        assertThat(mBrightnessObserver.shouldEnableBurnInProtector())
                .isFalse();
    }

    @Test
    public void autoBrightnessOn_lightSensorLower_true() {
        setScreenBrightnessMode(true);
        mBrightnessObserver.observe();
        mSensorEvent.values[0] = mBurnInMaskThreshold - 1;
        mShadowSensorManager.sendSensorEventToListeners(mSensorEvent);
        assertThat(mBrightnessObserver.shouldEnableBurnInProtector())
                .isTrue();
    }

    @Test
    public void autoBrightnessOff_screenSettingValueHigher_false() {
        setScreenBrightnessMode(false);
        setScreenBrightness(PowerManager.BRIGHTNESS_ON / 2 + 1);
        mBrightnessObserver.observe();

        assertThat(mBrightnessObserver.shouldEnableBurnInProtector())
                .isFalse();
    }

    @Test
    public void autoBrightnessOff_screenSettingValueLower_true() {
        setScreenBrightnessMode(false);
        setScreenBrightness(PowerManager.BRIGHTNESS_ON / 2 - 1);
        mBrightnessObserver.observe();

        assertThat(mBrightnessObserver.shouldEnableBurnInProtector())
                .isTrue();
    }

    private void setScreenBrightnessMode(boolean automatic) {
        Settings.System.putInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                automatic
                        ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                        : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    private void setScreenBrightness(int brightness) {
        Settings.System.putInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
    }
}
