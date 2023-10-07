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

package com.android.clockwork.display;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.provider.Settings;

import com.android.server.SystemService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WearDisplayServiceTest {
    private static final int DEFAULT_WINDOW_ID = 0;
    private static final int TEST_OFFSET_X = 0;
    private static final int TEST_OFFSET_Y = 0;
    private static final int PLUGGED = 1;
    private static final int UNPLUGGED = 0;

    private Context mContext;
    private WearDisplayService mService;
    private ContentResolver mContentResolver;

    @Mock private PowerManager mockPowerManager;
    @Mock private WallpaperManager mockWallpaperManager;
    @Mock WallpaperInfo mockWallpaperInfo;

    @Before
    public void setUp() {
        initMocks(this);
        when(mockWallpaperManager.getWallpaperInfo()).thenReturn(mockWallpaperInfo);
        when(mockWallpaperInfo.getComponent()).thenReturn(new ComponentName("testPkg", "testCls"));

        mContext = RuntimeEnvironment.application;
        mService = new WearDisplayService(mContext, mockWallpaperManager, mockPowerManager);
        mContentResolver = mContext.getContentResolver();

        mContext.sendStickyBroadcast(new Intent(Intent.ACTION_BATTERY_CHANGED)
                .putExtra(BatteryManager.EXTRA_PLUGGED, UNPLUGGED));
        Settings.Secure.putInt(mContentResolver, Settings.Secure.DOZE_ENABLED, 0);

        mService.onBootPhase(SystemService.PHASE_SYSTEM_SERVICES_READY);
        mService.onBootPhase(SystemService.PHASE_BOOT_COMPLETED);
    }

    @Test
    public void testDisplayOffsetPermission() {
        boolean securityExceptionCaught = false;
        try {
            mService.getBinderServiceInstance()
                    .setDisplayOffsets(DEFAULT_WINDOW_ID, TEST_OFFSET_X, TEST_OFFSET_Y);
        } catch (SecurityException e) {
            securityExceptionCaught = true;
        }

        assertTrue(
                "Security exception should be thrown for SET_DISPLAY_OFFSET",
                securityExceptionCaught);
    }

    @Test
    public void testDoze_ambientEnabled() {
        writeAmbientSetting(Settings.Global.Wearable.AMBIENT_ENABLED, true);

        mService.onAmbientConfigChanged();
        assertEquals(1, Settings.Secure.getInt(mContentResolver, Settings.Secure.DOZE_ENABLED, -1));
    }

    @Test
    public void testDoze_ambientEnabled_isPlugged_disableAodWhilePluggedEnabled() {
        mService.setDisableAodWhilePlugged(true);
        writeAmbientSetting(Settings.Global.Wearable.AMBIENT_ENABLED, true);
        mContext.sendStickyBroadcast(new Intent(Intent.ACTION_BATTERY_CHANGED)
                .putExtra(BatteryManager.EXTRA_PLUGGED, PLUGGED));

        mService.onAmbientConfigChanged();
        assertEquals(0, Settings.Secure.getInt(mContentResolver, Settings.Secure.DOZE_ENABLED, -1));
    }

    @Test
    public void testDoze_ambientEnabled_isPlugged_disableAodWhilePluggedDisabled() {
        mService.setDisableAodWhilePlugged(false);
        writeAmbientSetting(Settings.Global.Wearable.AMBIENT_ENABLED, true);
        mContext.sendStickyBroadcast(new Intent(Intent.ACTION_BATTERY_CHANGED)
                .putExtra(BatteryManager.EXTRA_PLUGGED, PLUGGED));

        mService.onAmbientConfigChanged();
        assertEquals(1, Settings.Secure.getInt(mContentResolver, Settings.Secure.DOZE_ENABLED, -1));
    }

    @Test
    public void testDoze_ambientDisabled_TTBEnabled_decomposableWF() {
        writeAmbientSetting(Settings.Global.Wearable.AMBIENT_ENABLED, false);
        writeAmbientSetting(Settings.Global.Wearable.AMBIENT_TILT_TO_BRIGHT, true);
        writeAmbientSetting(Settings.Global.Wearable.DECOMPOSABLE_WATCHFACE, true);

        mService.onAmbientConfigChanged();
        assertEquals(1, Settings.Secure.getInt(mContentResolver, Settings.Secure.DOZE_ENABLED, -1));
    }

    @Test
    public void testDoze_ambientDisabled_TTBEnabled_nonDecomposableWF() {
        writeAmbientSetting(Settings.Global.Wearable.AMBIENT_ENABLED, false);
        writeAmbientSetting(Settings.Global.Wearable.AMBIENT_TILT_TO_BRIGHT, true);
        writeAmbientSetting(Settings.Global.Wearable.DECOMPOSABLE_WATCHFACE, false);

        mService.onAmbientConfigChanged();
        assertEquals(0, Settings.Secure.getInt(mContentResolver, Settings.Secure.DOZE_ENABLED, -1));
    }

    @Test
    public void testDoze_ambientDisabled_TTBDisabled_decomposableWF() {
        writeAmbientSetting(Settings.Global.Wearable.AMBIENT_ENABLED, false);
        writeAmbientSetting(Settings.Global.Wearable.AMBIENT_TILT_TO_BRIGHT, false);
        writeAmbientSetting(Settings.Global.Wearable.DECOMPOSABLE_WATCHFACE, true);

        mService.onAmbientConfigChanged();
        assertEquals(0, Settings.Secure.getInt(mContentResolver, Settings.Secure.DOZE_ENABLED, -1));
    }

    @Test
    public void testCheckWatchface_forceEnableTTW() {
        writeAmbientSetting(Settings.Global.Wearable.AMBIENT_TILT_TO_BRIGHT, true);
        writeAmbientSetting(Settings.Global.Wearable.AMBIENT_TILT_TO_WAKE, false);
        writeAmbientSetting(Settings.Global.Wearable.DECOMPOSABLE_WATCHFACE, true);

        mService.onAmbientConfigChanged();
        writeAmbientSetting(Settings.Global.Wearable.DECOMPOSABLE_WATCHFACE, false);

        mService.onAmbientConfigChanged();
        assertTrue(readAmbietSetting(Settings.Global.Wearable.AMBIENT_TILT_TO_WAKE));
    }

    @Test
    public void testCheckWatchface_forceDisableTTW() {
        writeAmbientSetting(Settings.Global.Wearable.AMBIENT_TILT_TO_BRIGHT, true);
        writeAmbientSetting(Settings.Global.Wearable.AMBIENT_TILT_TO_WAKE, false);
        writeAmbientSetting(Settings.Global.Wearable.DECOMPOSABLE_WATCHFACE, false);

        mService.onAmbientConfigChanged();
        writeAmbientSetting(Settings.Global.Wearable.DECOMPOSABLE_WATCHFACE, true);

        mService.onAmbientConfigChanged();
        assertFalse(readAmbietSetting(Settings.Global.Wearable.AMBIENT_TILT_TO_WAKE));
    }

    @Test
    public void testDozeChange_deviceWakeup() {
        writeAmbientSetting(Settings.Global.Wearable.AMBIENT_ENABLED, false);
        mService.onAmbientConfigChanged();

        // Doze will change from enabled to disabled, so expect a wake up.
        verify(mockPowerManager)
                .wakeUp(anyLong(),
                        eq(PowerManager.WAKE_REASON_APPLICATION),
                        eq("onDozeChanged"));
    }

    @Test
    public void testNoDozeChange_noDeviceWakeup() {
        writeAmbientSetting(Settings.Global.Wearable.AMBIENT_ENABLED, true);
        mService.onAmbientConfigChanged();

        // Doze will remains disabled, so expect NO wake up.
        verify(mockPowerManager, never()).wakeUp(anyLong(), anyInt(), anyString());
    }

    private void writeAmbientSetting(String key, boolean value) {
        Settings.Global.putInt(mContentResolver, key, value ? 1 : 0);
    }

    private boolean readAmbietSetting(String key) {
        return Settings.Global.getInt(mContentResolver, key, 0) == 1;
    }
}
