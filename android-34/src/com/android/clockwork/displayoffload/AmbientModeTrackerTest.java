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

import static com.android.clockwork.displayoffload.Utils.ACTION_AMBIENT_STARTED;
import static com.android.clockwork.displayoffload.Utils.ACTION_AMBIENT_STOPPED;
import static com.android.clockwork.displayoffload.Utils.ACTION_STOP_AMBIENT_DREAM;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowSettings;

@Config(shadows = {ShadowSettings.class})
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class AmbientModeTrackerTest {

    @Mock
    private Runnable mRunnableMock;
    @Spy
    private Context mContext;

    private AmbientModeTracker mAmbientModeTracker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAmbientModeTracker =
                new AmbientModeTracker(mRunnableMock, new Handler(Looper.getMainLooper()));
    }

    @Test
    public void testGetIntentFilter() {
        IntentFilter intentFilter = mAmbientModeTracker.getIntentFilter();
        assertTrue(intentFilter.hasAction(ACTION_STOP_AMBIENT_DREAM));
        assertTrue(intentFilter.hasAction(ACTION_AMBIENT_STARTED));
        assertTrue(intentFilter.hasAction(ACTION_AMBIENT_STOPPED));
    }

    @Test
    public void testIsTheaterMode_theaterModeIsOn() {
        // set Theater Mode on
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.THEATER_MODE_ON, 1);
        assertTrue(mAmbientModeTracker.isTheaterMode(mContext));
    }

    @Test
    public void testStateChange_stopAmbientDream() {
        // tell the tracker that ambient mode was started
        Intent startAmbient = new Intent(ACTION_AMBIENT_STARTED);
        mAmbientModeTracker.onReceive(mContext, startAmbient);
        assertTrue(mAmbientModeTracker.isInAmbient());
        assertFalse(mAmbientModeTracker.isAmbientEarlyStop());

        // stop ambient dream
        Intent stopAmbientDream = new Intent(ACTION_STOP_AMBIENT_DREAM);
        mAmbientModeTracker.onReceive(mContext, stopAmbientDream);
        assertTrue(mAmbientModeTracker.isInAmbient());
        assertTrue(mAmbientModeTracker.isAmbientEarlyStop());
        verify(mRunnableMock, times(2)).run();
    }

    @Test
    public void testStateChange_AmbientStopped() {
        // tell the tracker that ambient mode was started
        Intent startAmbient = new Intent(ACTION_AMBIENT_STARTED);
        mAmbientModeTracker.onReceive(mContext, startAmbient);
        assertTrue(mAmbientModeTracker.isInAmbient());
        assertFalse(mAmbientModeTracker.isAmbientEarlyStop());

        // tell the tracker that ambient mode was stopped
        Intent stopAmbient = new Intent(ACTION_AMBIENT_STOPPED);
        mAmbientModeTracker.onReceive(mContext, stopAmbient);
        assertFalse(mAmbientModeTracker.isInAmbient());
        assertFalse(mAmbientModeTracker.isAmbientEarlyStop());
        verify(mRunnableMock, times(2)).run();
    }
}
