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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.clockwork.ArrayUtils;
import com.android.clockwork.power.AmbientConfig;
import com.android.internal.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowSensor;
import org.robolectric.shadows.ShadowSensorManager;

import java.util.ArrayList;

/**
 * Tests for {@link com.google.android.clockwork.displayoffload.BrightnessOffloadController}.
 */
@RunWith(AndroidJUnit4.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class BrightnessOffloadControllerTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final ShadowSensorManager mShadowSensorManager = shadowOf(
            mContext.getSystemService(SensorManager.class));
    // Lux threshold levels
    private final int[] mAutoBrightnessLevels = new int[]{5, 10, 15};
    // Brightness values
    private final int[] mAutoBrightnessLcdBacklightValuesNormal = new int[]{2, 2, 4, 14};
    private final int[] mAutoBrightnessLcdBacklightValuesDoze = new int[]{2, 2, 2, 6};
    @Mock
    HalAdapter mHalAdapter;
    @Mock
    Resources mFrameworkResourcesMock;
    @Mock
    Resources mWearResourcesMock;
    private Handler mBgHandler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        HandlerThread bgThread = new HandlerThread("Background Thread");
        bgThread.start();
        mBgHandler = new Handler(bgThread.getLooper());

        // Lux threshold levels
        when(mFrameworkResourcesMock.getIntArray(R.array.config_autoBrightnessLevels)).thenReturn(
                mAutoBrightnessLevels);
        when(mFrameworkResourcesMock.getIntArray(
                R.array.config_autoBrightnessLcdBacklightValues)).thenReturn(
                mAutoBrightnessLcdBacklightValuesNormal);
        when(mFrameworkResourcesMock.getIntArray(
                R.array.config_autoBrightnessLcdBacklightValues_doze)).thenReturn(
                mAutoBrightnessLcdBacklightValuesDoze);

        when(mFrameworkResourcesMock.getInteger(
                R.integer.config_screenBrightnessSettingDefault)).thenReturn(204);
        when(mFrameworkResourcesMock.getInteger(R.integer.config_screenBrightnessDoze)).thenReturn(
                60);

        mShadowSensorManager.addSensor(ShadowSensor.newInstance(Sensor.TYPE_LIGHT));
        setAutomaticBrightnessEnabled(true);
    }

    @After
    public void tearDown() {
        mBgHandler.getLooper().quit();
    }

    @Test
    public void testOnBootComplete_aodOff_shouldUseAllZeroAsDim() throws RemoteException {
        // Disable ambient
        AmbientConfig ambientConfig = spy(new AmbientConfig(mContext.getContentResolver()));
        when(ambientConfig.isAmbientEnabled()).thenReturn(false);
        BrightnessOffloadController brightnessOffloadController = new BrightnessOffloadController(
                mContext.getContentResolver(), mFrameworkResourcesMock,
                mContext.getSystemService(SensorManager.class), mBgHandler, mHalAdapter);

        // setBrightnessConfiguration be called with empty thresholds & only one brightness level
        brightnessOffloadController.onBootComplete(ambientConfig);
        verify(mHalAdapter).setBrightnessConfiguration(eq(false), any(),
                argThat(x -> x.stream().allMatch(e -> e == 0)), any());
    }

    @Test
    public void testOnBootComplete_shouldUseThresholdConfigured_manualBrightness()
            throws RemoteException {
        // Set to manual brightness
        setAutomaticBrightnessEnabled(false);
        // Enable ambient
        AmbientConfig ambientConfig = spy(new AmbientConfig(mContext.getContentResolver()));
        when(ambientConfig.isAmbientEnabled()).thenReturn(true);
        BrightnessOffloadController brightnessOffloadController = new BrightnessOffloadController(
                mContext.getContentResolver(), mFrameworkResourcesMock,
                mContext.getSystemService(SensorManager.class), mBgHandler, mHalAdapter);

        // setBrightnessConfiguration be called with empty thresholds & only one brightness level
        brightnessOffloadController.onBootComplete(ambientConfig);
        verify(mHalAdapter).setBrightnessConfiguration(eq(false), argThat(ArrayList::isEmpty),
                argThat(x -> x.size() == 1), argThat(x -> x.size() == 1));
    }

    @Test
    public void testOnBootComplete_shouldUseThresholdConfigured_automaticBrightness()
            throws RemoteException {
        // Enable ambient
        AmbientConfig ambientConfig = spy(new AmbientConfig(mContext.getContentResolver()));
        when(ambientConfig.isAmbientEnabled()).thenReturn(true);
        BrightnessOffloadController brightnessOffloadController = new BrightnessOffloadController(
                mContext.getContentResolver(), mFrameworkResourcesMock,
                mContext.getSystemService(SensorManager.class), mBgHandler, mHalAdapter);

        // Only test for Normal, others are deprecated.
        brightnessOffloadController.onBootComplete(ambientConfig);
        verify(mHalAdapter).setBrightnessConfiguration(eq(false),
                eq(ArrayUtils.intArrayToShortArrayList(mAutoBrightnessLevels)),
                eq(ArrayUtils.intArrayToShortArrayList(mAutoBrightnessLcdBacklightValuesDoze)),
                eq(ArrayUtils.intArrayToShortArrayList(mAutoBrightnessLcdBacklightValuesNormal)));
    }

    @Test
    public void testOnBootComplete_shouldTriggerOnAmbientConfigChanged() {
        BrightnessOffloadController brightnessOffloadController = spy(
                new BrightnessOffloadController(mContext.getContentResolver(),
                        mFrameworkResourcesMock, mContext.getSystemService(SensorManager.class),
                        mBgHandler, mHalAdapter));
        brightnessOffloadController.onBootComplete();

        verify(brightnessOffloadController).onAmbientConfigChanged();
    }

    @Test
    public void testOnBootComplete_shouldRegisterForAmbientConfigChanges() {
        AmbientConfig ambientConfig = spy(new AmbientConfig(mContext.getContentResolver()));
        BrightnessOffloadController brightnessOffloadController = spy(
                new BrightnessOffloadController(mContext.getContentResolver(),
                        mFrameworkResourcesMock, mContext.getSystemService(SensorManager.class),
                        mBgHandler, mHalAdapter));
        brightnessOffloadController.onBootComplete(ambientConfig);

        verify(brightnessOffloadController).onAmbientConfigChanged();
        verify(ambientConfig).register();
        verify(ambientConfig).addListener(eq(brightnessOffloadController));
    }

    @Test
    public void testOnBootComplete_shouldRegisterForBrightnessConfigChanges() {
        ContentResolver contentResolver = spy(mContext.getContentResolver());
        BrightnessOffloadController brightnessOffloadController = spy(
                new BrightnessOffloadController(contentResolver, mFrameworkResourcesMock,
                        mContext.getSystemService(SensorManager.class), mBgHandler, mHalAdapter));
        brightnessOffloadController.onBootComplete();

        shadowOf(mBgHandler.getLooper()).runToEndOfTasks();

        verify(contentResolver, atLeastOnce()).registerContentObserver(
                eq(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE)), anyBoolean(),
                any());
    }

    private void setAutomaticBrightnessEnabled(boolean useAutomatic) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                useAutomatic ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                        : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }
}

