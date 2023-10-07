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

package com.android.clockwork.time;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.server.SystemService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class WearTimeServiceTest {

    @Mock
    private TimeStateRecorder mMockTimeStateRecorder;
    @Spy
    private Context mSpyOnContext = ApplicationProvider.getApplicationContext();
    @Captor
    private ArgumentCaptor<Context> mContextArgumentCaptor;
    @Captor
    private ArgumentCaptor<TimeStateRecorder> mTimeStateRecorderArgumentCaptor;

    private WearTimeService mService;
    private boolean mSafeMode;

    /* WearTimeService with injection override methods */
    protected class WearTimeServiceTestable extends WearTimeService {

        public WearTimeServiceTestable(Context context, TimeStateRecorder timeStateRecorder) {
            super(context, timeStateRecorder);
        }

        @Override
        boolean injectIsSafeMode() {
            return mSafeMode;
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mService = new WearTimeServiceTestable(mSpyOnContext, mMockTimeStateRecorder);
        mSafeMode = false;
    }

    @Test
    public void testOnBootPhase_BeforeServicesReady_waitForDefaultDisplay() {
        mService.onBootPhase(SystemService.PHASE_WAIT_FOR_DEFAULT_DISPLAY);

        verifyNoInteractions(mMockTimeStateRecorder, mSpyOnContext);
    }

    @Test
    public void testOnBootPhase_BeforeServicesReady_lockSettingsReady() {
        mService.onBootPhase(SystemService.PHASE_LOCK_SETTINGS_READY);

        verifyNoInteractions(mMockTimeStateRecorder, mSpyOnContext);
    }

    @Test
    public void testOnBootPhase_ServicesReady() {
        // make mockTimeStateRecorder init() call succeed
        when(mMockTimeStateRecorder.init(any(Context.class), any(TimeState.class))).thenReturn(
                true);

        mService.onBootPhase(SystemService.PHASE_SYSTEM_SERVICES_READY);

        // verify mockTimeStateRecorder is initialized with the context supplied to WearTimeService
        verify(mMockTimeStateRecorder).init(mContextArgumentCaptor.capture(), any(TimeState.class));
        Context capturedContext = mContextArgumentCaptor.getValue();
        assertEquals(mSpyOnContext, capturedContext);

        // verify mockTimeStateRecorder is registered as a BroadcastReceiver by WearTimeService
        verify(mSpyOnContext).registerReceiver(mTimeStateRecorderArgumentCaptor.capture(), any());
        TimeStateRecorder capturedTimeStateRecorder = mTimeStateRecorderArgumentCaptor.getValue();
        assertEquals(mMockTimeStateRecorder, capturedTimeStateRecorder);
    }

    @Test
    public void testOnBootPhase_ServicesReady_TimeStateRecorderFails() {
        // make mockTimeStateRecorder init() call fail
        when(mMockTimeStateRecorder.init(any(Context.class), any(TimeState.class))).thenReturn(
                false);

        mService.onBootPhase(SystemService.PHASE_SYSTEM_SERVICES_READY);

        // verify no interaction with context. i.e. no BroadcastReceiver is registered by
        // WearTimeService
        verifyNoInteractions(mSpyOnContext);
    }

    @Test
    public void testOnBootPhase_ServicesReady_InSafeMode() {
        mSafeMode = true;

        mService.onBootPhase(SystemService.PHASE_SYSTEM_SERVICES_READY);

        // verify WearTimeService does nothing. It should not init TimeStateRecorder and should
        // not interact with the system context. i.e. no BroadcastReceiver is registered by
        // WearTimeService
        verifyNoInteractions(mMockTimeStateRecorder, mSpyOnContext);
    }
}
