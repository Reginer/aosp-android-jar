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

import static android.os.PowerManager.WAKE_REASON_APPLICATION;

import static com.android.clockwork.power.IWearPowerService.OFFLOAD_BACKEND_TYPE_DISPLAYOFFLOAD;
import static com.android.clockwork.power.IWearPowerService.OFFLOAD_BACKEND_TYPE_NA;
import static com.android.clockwork.power.IWearPowerService.OFFLOAD_BACKEND_TYPE_SIDEKICK;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.view.Display;

import com.google.android.clockwork.ambient.offload.IDisplayOffloadService;
import com.google.android.clockwork.sidekick.ISidekickService;
import com.google.android.clockwork.sidekick.SidekickServiceConstants;

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
import org.robolectric.shadows.ShadowSystemProperties;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowServiceManagerExtended.class, ShadowSettings.class})
public class WearDisplayOffloadMediatorTest {

    private static final String CLOCKWORK_SYSUI_PACKAGE_NAME = "SysUiPackage";

    @Mock
    IDisplayOffloadService.Stub mDisplayOffloadServiceMock;
    @Mock
    ISidekickService.Stub mSidekickServiceMock;
    @Mock
    IActivityTaskManager.Stub mActivityTaskManagerServiceMock;
    @Mock
    PowerManager mPowerManagerMock;
    @Mock
    DisplayManager mDisplayManagerMock;
    @Mock
    WallpaperManager mWallpaperManagerMock;
    @Mock
    Display mDisplayMock;

    @Spy
    Context mContextSpy;

    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);

        Settings.Global.putString(mContextSpy.getContentResolver(),
                Settings.Global.Wearable.CLOCKWORK_SYSUI_PACKAGE, CLOCKWORK_SYSUI_PACKAGE_NAME);

        // set 'build' to debug. This makes WearDisplayOffloadMediator produce more logs.
        ReflectionHelpers.setStaticField(Build.class, "IS_DEBUGGABLE", true);

        // Have Stub.asInterface() and Stub.asBinder() return the mocked interface.
        when(mDisplayOffloadServiceMock.queryLocalInterface(anyString())).thenReturn(
                mDisplayOffloadServiceMock);
        when(mDisplayOffloadServiceMock.asBinder()).thenReturn(mDisplayOffloadServiceMock);
        when(mSidekickServiceMock.queryLocalInterface(anyString())).thenReturn(
                mSidekickServiceMock);
        when(mSidekickServiceMock.asBinder()).thenReturn(mSidekickServiceMock);
        when(mActivityTaskManagerServiceMock.queryLocalInterface(anyString())).thenReturn(
                mActivityTaskManagerServiceMock);
        when(mActivityTaskManagerServiceMock.asBinder()).thenReturn(
                mActivityTaskManagerServiceMock);

        ActivityTaskManager.RootTaskInfo ri = mock(ActivityTaskManager.RootTaskInfo.class);
        ri.topActivity = new ComponentName(CLOCKWORK_SYSUI_PACKAGE_NAME, "SomeSysUiClass.class");
        when(mActivityTaskManagerServiceMock.getFocusedRootTaskInfo()).thenReturn(ri);

        when(mDisplayManagerMock.getDisplay(anyInt())).thenReturn(mDisplayMock);
        when(mContextSpy.getSystemService(PowerManager.class)).thenReturn(mPowerManagerMock);
        when(mContextSpy.getSystemService(DisplayManager.class)).thenReturn(mDisplayManagerMock);
        when(mContextSpy.getSystemService(WallpaperManager.class)).thenReturn(
                mWallpaperManagerMock);

        ServiceManager.addService(Context.ACTIVITY_TASK_SERVICE, mActivityTaskManagerServiceMock);
    }

    @After
    public void tearDown() {
        ShadowSettings.reset();
        ShadowSystemProperties.reset();
        ShadowServiceManagerExtended.reset();
    }

    private void enableDisplayOffloadService() {
        ServiceManager.addService(IDisplayOffloadService.NAME, mDisplayOffloadServiceMock);
    }

    private void enableSidekickService() throws RemoteException {
        ServiceManager.addService(SidekickServiceConstants.NAME, mSidekickServiceMock);
        when(mSidekickServiceMock.sidekickExists()).thenReturn(true);
    }

    @Test
    public void testDisplayOffloadServiceBackend_offloadBackendGetType() throws RemoteException {
        enableDisplayOffloadService();
        WearDisplayOffloadMediator wearDisplayOffloadMediator = new WearDisplayOffloadMediator(
                mContextSpy);

        assertEquals(wearDisplayOffloadMediator.offloadBackendGetType(),
                OFFLOAD_BACKEND_TYPE_DISPLAYOFFLOAD);
    }

    @Test
    public void testDisplayOffloadServiceBackend_setShouldControlDisplay() throws RemoteException {
        enableDisplayOffloadService();
        WearDisplayOffloadMediator wearDisplayOffloadMediator = new WearDisplayOffloadMediator(
                mContextSpy);

        wearDisplayOffloadMediator.setShouldControlDisplay(true);
        // When the backend is DisplayOffloadService, verify that setShouldControlDisplay()
        // is not invoked on the SidekickService backend.
        verify(mSidekickServiceMock, never()).setShouldControlDisplay(anyBoolean());
    }

    @Test
    public void testDisplayOffloadServiceBackend_offloadBackendReadyToDisplay()
            throws RemoteException {
        enableDisplayOffloadService();
        WearDisplayOffloadMediator wearDisplayOffloadMediator = new WearDisplayOffloadMediator(
                mContextSpy);

        when(mDisplayOffloadServiceMock.readyToDisplay()).thenReturn(true);

        assertTrue(wearDisplayOffloadMediator.offloadBackendReadyToDisplay());
        verify(mDisplayOffloadServiceMock, times(1)).readyToDisplay();
    }

    @Test
    public void testSidekickServiceBackend_offloadBackendGetType() throws RemoteException {
        enableSidekickService();
        WearDisplayOffloadMediator wearDisplayOffloadMediator = new WearDisplayOffloadMediator(
                mContextSpy);

        assertEquals(wearDisplayOffloadMediator.offloadBackendGetType(),
                OFFLOAD_BACKEND_TYPE_SIDEKICK);
    }

    @Test
    public void testSidekickServiceBackend_setShouldControlDisplay() throws RemoteException {
        enableSidekickService();
        WearDisplayOffloadMediator wearDisplayOffloadMediator = new WearDisplayOffloadMediator(
                mContextSpy);

        wearDisplayOffloadMediator.setShouldControlDisplay(true);
        verify(mSidekickServiceMock).setShouldControlDisplay(eq(true));
    }

    @Test
    public void testSidekickServiceBackend_offloadBackendReadyToDisplay() throws RemoteException {
        enableSidekickService();
        WearDisplayOffloadMediator wearDisplayOffloadMediator = new WearDisplayOffloadMediator(
                mContextSpy);

        when(mSidekickServiceMock.readyToDisplay()).thenReturn(true);

        assertTrue(wearDisplayOffloadMediator.offloadBackendReadyToDisplay());
        verify(mSidekickServiceMock, times(1)).readyToDisplay();
    }

    @Test
    public void testNoBackend_offloadBackendGetType() {
        WearDisplayOffloadMediator wearDisplayOffloadMediator = new WearDisplayOffloadMediator(
                mContextSpy);
        assertEquals(wearDisplayOffloadMediator.offloadBackendGetType(), OFFLOAD_BACKEND_TYPE_NA);
    }

    @Test
    public void testNoBackend_setShouldControlDisplay() throws RemoteException {
        WearDisplayOffloadMediator wearDisplayOffloadMediator = new WearDisplayOffloadMediator(
                mContextSpy);

        wearDisplayOffloadMediator.setShouldControlDisplay(true);
        verify(mSidekickServiceMock, never()).setShouldControlDisplay(anyBoolean());
    }

    @Test
    public void testNoBackend_offloadBackendReadyToDisplay() {
        WearDisplayOffloadMediator wearDisplayOffloadMediator = new WearDisplayOffloadMediator(
                mContextSpy);
        assertFalse(wearDisplayOffloadMediator.offloadBackendReadyToDisplay());
    }

    @Test
    public void testColorChangeListener() {
        enableDisplayOffloadService();
        // invoke the constructor to register the color change listener. We don't need a reference
        // to the WearDisplayOffloadMediator object for this test.
        new WearDisplayOffloadMediator(mContextSpy);

        // capture the color change listener that is registered
        ArgumentCaptor<WallpaperManager.OnColorsChangedListener>
                wallpaperColorsChangeListenerCaptor =
                ArgumentCaptor.forClass(WallpaperManager.OnColorsChangedListener.class);
        verify(mWallpaperManagerMock).addOnColorsChangedListener(
                wallpaperColorsChangeListenerCaptor.capture(), any());
        WallpaperManager.OnColorsChangedListener wallpaperColorsChangeListener =
                wallpaperColorsChangeListenerCaptor.getValue();
        assertNotNull(wallpaperColorsChangeListener);

        when(mDisplayMock.getState()).thenReturn(Display.STATE_DOZE_SUSPEND);

        // invoke the listener as if a color change has occurred
        wallpaperColorsChangeListener.onColorsChanged(
                new WallpaperColors(Color.valueOf(Color.CYAN), Color.valueOf(Color.DKGRAY),
                        Color.valueOf(Color.WHITE)), 0);

        // verify the screen is woken as a result of the color change
        verify(mPowerManagerMock).wakeUp(anyLong(), eq(WAKE_REASON_APPLICATION), anyString());
    }
}
