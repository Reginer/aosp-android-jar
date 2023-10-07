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

package com.android.clockwork.display.burninprotection;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.AlarmManager;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class BurnInProtectorTest {

    private BurnInProtector mBurnInProtector;
    private ShadowAlarmManager mShadowAlarmManager;
    @Mock
    private ImageMaskViewController mockImageMaskViewController;
    @Mock
    private DisplayManager mockDisplayManager;
    @Mock
    private Display mockDisplay;
    @Mock
    private BrightnessObserver mBrightnessObserver;

    private ShadowLooper mShadowLooper;
    private long mSystemClockTimeMillis;
    private Handler mHandler;

    @Before
    public void setup() {
        initMocks(this);

        //setup default display
        when(mockDisplayManager.getDisplay(Display.DEFAULT_DISPLAY))
                .thenReturn(mockDisplay);

        Context context = ApplicationProvider.getApplicationContext();
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        mShadowAlarmManager = Shadows.shadowOf(alarmManager);

        when(mBrightnessObserver.shouldEnableBurnInProtector()).thenReturn(true);

        mHandler = new Handler(Looper.getMainLooper());
        mShadowLooper = Shadows.shadowOf(mHandler.getLooper());
        mBurnInProtector = new BurnInProtector(
                mockDisplayManager,
                mockImageMaskViewController,
                () -> mSystemClockTimeMillis,
                mHandler,
                alarmManager,
                mBrightnessObserver);

        mBurnInProtector.init(context);
    }

    private void onDisplayChanged(int displayState) {
        when(mockDisplay.getState())
                .thenReturn(displayState);
        mBurnInProtector.onDisplayChanged(Display.DEFAULT_DISPLAY);
    }

    @Test
    public void dozeState_after2Minutes_enableImageMaskAndUpdateEvery1Minute() {
        // Enter STATE_DOZE
        onDisplayChanged(Display.STATE_DOZE);

        // Assert schedule is after 2 minutes
        assertThat(mShadowAlarmManager.getScheduledAlarms().get(0).triggerAtTime)
                .isAtLeast(mSystemClockTimeMillis + TimeUnit.MINUTES.toMillis(2));

        // Run alarm after 2 minutes
        mSystemClockTimeMillis += TimeUnit.MINUTES.toMillis(2);
        mHandler.post(mShadowAlarmManager.peekNextScheduledAlarm().onAlarmListener::onAlarm);
        mShadowLooper.runOneTask();

        verify(mockImageMaskViewController).enableImageMask();
        when(mockImageMaskViewController.isEnabled()).thenReturn(true);

        for (int i = 0; i < 5; i++) {
            // Assert schedule is after 1 minute
            assertThat(mShadowAlarmManager.getScheduledAlarms().get(0).triggerAtTime)
                    .isAtLeast(mSystemClockTimeMillis + TimeUnit.MINUTES.toMillis(1));

            // After 1 minute
            mSystemClockTimeMillis += TimeUnit.MINUTES.toMillis(1);
            mHandler.post(mShadowAlarmManager.peekNextScheduledAlarm().onAlarmListener::onAlarm);
            mShadowLooper.runOneTask();

            verify(mockImageMaskViewController, Mockito.times(i + 1)).updateImageMask();
        }

        // Display On
        onDisplayChanged(Display.STATE_ON);
        verify(mockImageMaskViewController).disableImageMask();
    }

    @Test
    public void offloadState_after2MinutesAndSingleShot_enableImageMaskAndUpdateEvery1Minute() {
        // singleShotUpdate() is called right before STATE_DOZE_SUSPEND
        mBurnInProtector.singleShotUpdate();
        onDisplayChanged(Display.STATE_DOZE_SUSPEND);

        // After 59 secs
        mSystemClockTimeMillis += TimeUnit.SECONDS.toMillis(59);

        // Should not call enableImageMask()
        mBurnInProtector.singleShotUpdate();
        verify(mockImageMaskViewController, never()).enableImageMask();

        // After 1 mimute
        mSystemClockTimeMillis += TimeUnit.MINUTES.toMillis(1);

        // Should call enableImageMask()
        mBurnInProtector.singleShotUpdate();
        verify(mockImageMaskViewController).enableImageMask();
        when(mockImageMaskViewController.isEnabled()).thenReturn(true);

        // Assert every 1 single minute
        for (int i = 0; i < 5; i++) {
            mSystemClockTimeMillis += TimeUnit.MINUTES.toMillis(1);
            mBurnInProtector.singleShotUpdate();
            verify(mockImageMaskViewController, times(i + 1)).updateImageMask();
        }

        // Display On
        onDisplayChanged(Display.STATE_ON);
        verify(mockImageMaskViewController).disableImageMask();
    }

    @Test
    public void offloadState_disabledAfterEnabled_resetLastTimeStamp() {
        // singleShotUpdate() is called right before STATE_DOZE_SUSPEND
        mBurnInProtector.singleShotUpdate();
        onDisplayChanged(Display.STATE_DOZE_SUSPEND);

        // After 2 mimute
        mSystemClockTimeMillis += TimeUnit.MINUTES.toMillis(2);

        // Should call enableImageMask()
        mBurnInProtector.singleShotUpdate();
        verify(mockImageMaskViewController).enableImageMask();
        when(mockImageMaskViewController.isEnabled()).thenReturn(true);

        // Display On
        onDisplayChanged(Display.STATE_ON);
        verify(mockImageMaskViewController).disableImageMask();
        when(mockImageMaskViewController.isEnabled()).thenReturn(false);

        // Reset mock
        Mockito.clearInvocations(mockImageMaskViewController);
        Mockito.reset(mockImageMaskViewController);
        verify(mockImageMaskViewController, never()).enableImageMask();

        // singleShotUpdate() is called right before STATE_DOZE_SUSPEND
        mBurnInProtector.singleShotUpdate();
        onDisplayChanged(Display.STATE_DOZE_SUSPEND);
        verify(mockImageMaskViewController, never()).enableImageMask();

        // After 59 secs
        mSystemClockTimeMillis += TimeUnit.SECONDS.toMillis(59);

        // Should not call enableImageMask()
        mBurnInProtector.singleShotUpdate();
        verify(mockImageMaskViewController, never()).enableImageMask();

        // After 1 mimute
        mSystemClockTimeMillis += TimeUnit.MINUTES.toMillis(1);

        // Should call enableImageMask()
        mBurnInProtector.singleShotUpdate();
        verify(mockImageMaskViewController).enableImageMask();
    }

    @Test
    public void brightnessUnavailable_offloadState_shouldNotEnableImageMask() {
        when(mBrightnessObserver.shouldEnableBurnInProtector()).thenReturn(false);

        // singleShotUpdate() is called right before STATE_DOZE_SUSPEND
        mBurnInProtector.singleShotUpdate();
        onDisplayChanged(Display.STATE_DOZE_SUSPEND);

        // After 59 secs
        mSystemClockTimeMillis += TimeUnit.SECONDS.toMillis(59);

        // Should not call enableImageMask()
        mBurnInProtector.singleShotUpdate();
        verify(mockImageMaskViewController, never()).enableImageMask();

        // After 1 mimute
        mSystemClockTimeMillis += TimeUnit.MINUTES.toMillis(1);

        // Should call enableImageMask()
        mBurnInProtector.singleShotUpdate();
        verify(mockImageMaskViewController, never()).enableImageMask();

        // Assert every 1 single minute
        for (int i = 0; i < 5; i++) {
            mSystemClockTimeMillis += TimeUnit.MINUTES.toMillis(1);
            mBurnInProtector.singleShotUpdate();
            verify(mockImageMaskViewController, never()).updateImageMask();
        }

        // Display On
        onDisplayChanged(Display.STATE_ON);
        verify(mockImageMaskViewController).disableImageMask();
    }
}
