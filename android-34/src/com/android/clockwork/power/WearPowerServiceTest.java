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

package com.android.clockwork.power;

import static com.android.clockwork.power.IWearPowerService.OFFLOAD_BACKEND_TYPE_DISPLAYOFFLOAD;
import static com.android.clockwork.power.IWearPowerService.OFFLOAD_BACKEND_TYPE_NA;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.clockwork.flags.BooleanFlag;
import com.android.server.LocalServices;
import com.android.server.SystemService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSettings;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowServiceManagerExtended.class, ShadowSettings.class})
public class WearPowerServiceTest {
    class OffsettableClock {
        private long mOffset = 0L;

        public long now() {
            return mOffset;
        }

        public void advance(long timeMs) {
            mOffset += timeMs;
        }

        public void reset() {
            mOffset = 0;
        }
    }

    @Mock
    private AmbientConfig mAmbientConfigMock;
    @Mock
    private PowerTracker mPowerTrackerMock;
    @Mock
    private TimeOnlyMode mTimeOnlyModeMock;
    @Mock
    private WearTouchMediator mWearTouchMediatorMock;
    @Mock
    private WearBurnInProtectionMediator mWearBurnInProtectionMediatorMock;
    @Mock
    private WearDisplayOffloadMediator mWearDisplayOffloadMediatorMock;
    @Mock
    private WearCoreControlMediator mWearCoreControlMediatorMock;
    @Mock
    private PowerManager mPowerManagerMock;
    @Mock
    private PowerManagerInternal mPowerManagerInternalMock;

    @Spy
    private Context mContextSpy;

    private WearPowerService mService;
    private WearPowerServiceInternal mLocalService;
    private com.android.clockwork.power.IWearPowerService mBinderService;
    private BroadcastReceiver mGoToSleepReceiver;
    private ContentObserver mSettingObserver;
    private OffsettableClock mClock;

    private static final int POWER_BUTTON_SUPPRESSION_DELAY_TEST_DEFAULT_MS = 1000;
    private static final String ACTION_GO_TO_SLEEEP =
            "com.android.clockwork.action.GO_TO_SLEEP";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContextSpy = spy(ApplicationProvider.getApplicationContext());
        mClock = new OffsettableClock();
        ContentResolver cr = spy(mContextSpy.getContentResolver());

        when(mContextSpy.getContentResolver()).thenReturn(cr);
        when(mContextSpy.getSystemService(PowerManager.class)).thenReturn(mPowerManagerMock);
        Settings.Global.putInt(mContextSpy.getContentResolver(),
                Settings.Global.POWER_BUTTON_SUPPRESSION_DELAY_AFTER_GESTURE_WAKE,
                POWER_BUTTON_SUPPRESSION_DELAY_TEST_DEFAULT_MS);

        // enable Core Control Mediator
        SystemProperties.set(WearCoreControlMediator.SYSPROP_CORE_CONTROL_ENABLE, "true");

        LocalServices.addService(PowerManagerInternal.class, mPowerManagerInternalMock);
    }

    @After
    public void tearDown() {
        LocalServices.removeServiceForTest(PowerManagerInternal.class);
        LocalServices.removeServiceForTest(WearPowerServiceInternal.class);
        ShadowServiceManagerExtended.reset();
        ShadowSettings.reset();
    }

    private void initService() {
        mService = new WearPowerService(mContextSpy, new WearPowerService.Injector() {
            @Override
            AmbientConfig createAmbientConfig(ContentResolver contentResolver) {
                return mAmbientConfigMock;
            }

            @Override
            PowerTracker createPowerTracker(Context context, PowerManager powerManager) {
                return mPowerTrackerMock;
            }

            @Override
            TimeOnlyMode createTimeOnlyMode(ContentResolver contentResolver,
                    PowerTracker powerTracker) {
                return mTimeOnlyModeMock;
            }

            @Override
            WearTouchMediator createWearTouchMediator(Context context, AmbientConfig ambientConfig,
                    PowerTracker powerTracker, TimeOnlyMode timeOnlyMode,
                    BooleanFlag userAbsentTouchOff) {
                return mWearTouchMediatorMock;
            }

            @Override
            WearBurnInProtectionMediator createWearBurnInProtectionMediator(Context context,
                    AmbientConfig ambientConfig) {
                return mWearBurnInProtectionMediatorMock;
            }

            @Override
            WearDisplayOffloadMediator createWearDisplayOffloadMediator(Context context) {
                return mWearDisplayOffloadMediatorMock;
            }

            @Override
            WearCoreControlMediator createWearCoreControlMediator(Context context,
                    PowerTracker powerTracker) {
                return mWearCoreControlMediatorMock;
            }

            @Override
            WearPowerService.Clock createClock() {
                return () -> mClock.now();
            }
        });

        ArgumentCaptor<BroadcastReceiver> goToSleepReceiverCaptor = ArgumentCaptor.forClass(
                BroadcastReceiver.class);
        ArgumentCaptor<ContentObserver> settingObserverCaptor = ArgumentCaptor.forClass(
                ContentObserver.class);

        mService.onStart();
        mBinderService = IWearPowerService.Stub.asInterface(
                ServiceManager.getService(WearPowerService.SERVICE_NAME));
        mLocalService = LocalServices.getService(WearPowerServiceInternal.class);
        verify(mContextSpy).registerReceiver(goToSleepReceiverCaptor.capture(), any(), any(),
                any(), eq(Context.RECEIVER_EXPORTED));
        mGoToSleepReceiver = goToSleepReceiverCaptor.getValue();

        assertNotNull(mBinderService);
        assertNotNull(mLocalService);
        assertNotNull(mGoToSleepReceiver);

        mService.onBootPhase(SystemService.PHASE_SYSTEM_SERVICES_READY);
        verify(mContextSpy.getContentResolver()).registerContentObserver(isA(Uri.class),
                anyBoolean(), settingObserverCaptor.capture(), eq(
                        UserHandle.USER_ALL));
        mSettingObserver = settingObserverCaptor.getValue();

        assertNotNull(mSettingObserver);

        mService.onBootPhase(SystemService.PHASE_BOOT_COMPLETED);
        verify(mAmbientConfigMock, times(1)).register();
        verify(mPowerTrackerMock, times(1)).onBootCompleted();
        verify(mWearTouchMediatorMock, times(1)).onBootCompleted();
        verify(mWearCoreControlMediatorMock, times(1)).onBootCompleted();
    }

    @Test
    public void testBinderInterface_offloadBackendGetType() throws RemoteException {
        initService();
        int offloadBackend = OFFLOAD_BACKEND_TYPE_DISPLAYOFFLOAD;
        when(mWearDisplayOffloadMediatorMock.offloadBackendGetType()).thenReturn(offloadBackend);
        assertEquals(offloadBackend, mBinderService.offloadBackendGetType());
    }

    @Test
    public void testBinderInterface_offloadBackendGetType_nullMediator() throws RemoteException {
        mWearDisplayOffloadMediatorMock = null;
        initService();
        assertEquals(OFFLOAD_BACKEND_TYPE_NA, mBinderService.offloadBackendGetType());
    }

    @Test
    public void testBinderInterface_offloadBackendReadyToDisplay_nullMediator()
            throws RemoteException {
        mWearDisplayOffloadMediatorMock = null;
        initService();
        assertFalse(mBinderService.offloadBackendReadyToDisplay());
    }

    @Test
    public void testBinderInterface_offloadBackendReadyToDisplay_Ready() throws RemoteException {
        initService();
        when(mWearDisplayOffloadMediatorMock.offloadBackendReadyToDisplay()).thenReturn(true);
        assertTrue(mBinderService.offloadBackendReadyToDisplay());
    }

    @Test
    public void testBinderInterface_offloadBackendReadyToDisplay_NotReady() throws RemoteException {
        initService();
        when(mWearDisplayOffloadMediatorMock.offloadBackendReadyToDisplay()).thenReturn(false);
        assertFalse(mBinderService.offloadBackendReadyToDisplay());
    }

    @Test
    public void testBinderInterface_offloadBackendSetShouldControlDisplay() throws RemoteException {
        initService();
        mBinderService.offloadBackendSetShouldControlDisplay(true);
        verify(mWearDisplayOffloadMediatorMock).setShouldControlDisplay(eq(true));
    }

    @Test
    public void testLocalService_getAmbientConfig() {
        initService();
        assertEquals(mAmbientConfigMock, mLocalService.getAmbientConfig());
    }

    @Test
    public void testLocalService_getPowerTracker() {
        initService();
        assertEquals(mPowerTrackerMock, mLocalService.getPowerTracker());
    }

    @Test
    public void testLocalService_getTimeOnlyMode() {
        initService();
        assertEquals(mTimeOnlyModeMock, mLocalService.getTimeOnlyMode());
    }

    @Test
    public void testUpdatePowerButtonSettings() {
        final int suppressionDelayMs = 2000;
        initService();
        Settings.Global.putInt(mContextSpy.getContentResolver(),
                Settings.Global.POWER_BUTTON_SUPPRESSION_DELAY_AFTER_GESTURE_WAKE,
                suppressionDelayMs);
        mSettingObserver.onChange(true);
        assertEquals(suppressionDelayMs, mService.getPowerButtonSuppressionDelayMsForTest());
    }

    @Test
    public void testGoToSleep_WokenByGesture_ButtonPressedAfterSuppressionDelay() {
        initService();
        // set last wake reason
        PowerManager.WakeData wd = new PowerManager.WakeData(mClock.now(),
                PowerManager.WAKE_REASON_GESTURE, 1000);
        // advance the system clock by 5000ms so that now() > lastWake + suppressionDelay
        mClock.advance(5000);
        when(mPowerManagerInternalMock.getLastWakeup()).thenReturn(wd);
        Intent sleepIntent = new Intent(ACTION_GO_TO_SLEEEP);
        mGoToSleepReceiver.onReceive(mContextSpy, sleepIntent);

        // device should be put to sleep
        verify(mPowerManagerMock).goToSleep(eq(mClock.now()),
                eq(PowerManager.GO_TO_SLEEP_REASON_POWER_BUTTON), anyInt());
    }

    @Test
    public void testGoToSleep_WokenByGesture_ButtonPressedBeforeSuppressionDelay() {
        initService();
        // set last wake reason
        PowerManager.WakeData wd = new PowerManager.WakeData(mClock.now(),
                PowerManager.WAKE_REASON_GESTURE, 1000);
        // advance the system clock by 200ms so that now() < lastWake + suppressionDelay
        mClock.advance(200);
        when(mPowerManagerInternalMock.getLastWakeup()).thenReturn(wd);
        Intent sleepIntent = new Intent(ACTION_GO_TO_SLEEEP);
        mGoToSleepReceiver.onReceive(mContextSpy, sleepIntent);

        // device should not be put to sleep
        verify(mPowerManagerMock, never()).goToSleep(anyLong(), anyInt(), anyInt());
    }
}
