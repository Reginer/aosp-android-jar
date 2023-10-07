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

package com.android.clockwork;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class ThermalObserverTest {

    @Spy
    private Context mSpyOnContext = ApplicationProvider.getApplicationContext();
    @Captor
    private ArgumentCaptor<Intent> mIntentArgumentCaptor;
    @Captor
    private ArgumentCaptor<UserHandle> mUserHandleArgumentCaptor;

    private static final int SWITCH_STATE_NORMAL = 0;
    private static final int SWITCH_STATE_WARNING = 1;
    private static final int SWITCH_STATE_EXCEEDED = 2;
    private static final int SWITCH_STATE_UNKNOWN = 1 << 31;
    private static final int UNIT_TEST_TIMEOUT_MS = 1000;
    private static final int DEFAULT_INTENT_THERMAL_STATE = -1;

    private ThermalObserver mThermalObserver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mThermalObserver = new ThermalObserver(mSpyOnContext);
    }

    @Test
    public void testThermalStateNormal() {
        mThermalObserver.updateTestOnly(SWITCH_STATE_NORMAL);

        verify(mSpyOnContext, timeout(UNIT_TEST_TIMEOUT_MS)).sendBroadcastAsUser(
                mIntentArgumentCaptor.capture(), mUserHandleArgumentCaptor.capture());
        Intent capturedIntent = mIntentArgumentCaptor.getValue();
        UserHandle capturedUserHandle = mUserHandleArgumentCaptor.getValue();

        assertEquals(Intent.EXTRA_THERMAL_STATE_NORMAL,
                capturedIntent.getIntExtra(Intent.EXTRA_THERMAL_STATE,
                        DEFAULT_INTENT_THERMAL_STATE));
        assertEquals(UserHandle.ALL, capturedUserHandle);
    }

    @Test
    public void testThermalStateWarning() {
        mThermalObserver.updateTestOnly(SWITCH_STATE_WARNING);

        verify(mSpyOnContext, timeout(UNIT_TEST_TIMEOUT_MS)).sendBroadcastAsUser(
                mIntentArgumentCaptor.capture(), mUserHandleArgumentCaptor.capture());
        Intent capturedIntent = mIntentArgumentCaptor.getValue();
        UserHandle capturedUserHandle = mUserHandleArgumentCaptor.getValue();

        assertEquals(Intent.EXTRA_THERMAL_STATE_WARNING,
                capturedIntent.getIntExtra(Intent.EXTRA_THERMAL_STATE,
                        DEFAULT_INTENT_THERMAL_STATE));
        assertEquals(UserHandle.ALL, capturedUserHandle);
    }

    @Test
    public void testThermalStateExceeded() {
        mThermalObserver.updateTestOnly(SWITCH_STATE_EXCEEDED);

        verify(mSpyOnContext, timeout(UNIT_TEST_TIMEOUT_MS)).sendBroadcastAsUser(
                mIntentArgumentCaptor.capture(), mUserHandleArgumentCaptor.capture());
        Intent capturedIntent = mIntentArgumentCaptor.getValue();
        UserHandle capturedUserHandle = mUserHandleArgumentCaptor.getValue();

        assertEquals(Intent.EXTRA_THERMAL_STATE_EXCEEDED,
                capturedIntent.getIntExtra(Intent.EXTRA_THERMAL_STATE,
                        DEFAULT_INTENT_THERMAL_STATE));
        assertEquals(UserHandle.ALL, capturedUserHandle);
    }

    @Test
    public void testThermalStateUnknown() {
        // Pass an unrecognized thermal state to ThermalObserver to make sure it behaves gracefully
        // and emits a THERMAL_STATE_NORMAL intent
        mThermalObserver.updateTestOnly(SWITCH_STATE_UNKNOWN);

        verify(mSpyOnContext, timeout(UNIT_TEST_TIMEOUT_MS)).sendBroadcastAsUser(
                mIntentArgumentCaptor.capture(), mUserHandleArgumentCaptor.capture());
        Intent capturedIntent = mIntentArgumentCaptor.getValue();
        UserHandle capturedUserHandle = mUserHandleArgumentCaptor.getValue();

        assertEquals(Intent.EXTRA_THERMAL_STATE_NORMAL,
                capturedIntent.getIntExtra(Intent.EXTRA_THERMAL_STATE,
                        DEFAULT_INTENT_THERMAL_STATE));
        assertEquals(UserHandle.ALL, capturedUserHandle);

    }

}
