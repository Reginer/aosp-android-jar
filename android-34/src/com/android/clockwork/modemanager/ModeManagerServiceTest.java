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

package com.android.clockwork.modemanager;

import static android.provider.Settings.Global.Wearable.WET_MODE_ON;

import static com.android.server.SystemService.PHASE_BOOT_COMPLETED;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ModeManagerServiceTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();

    private ModeManagerService mService;

    @Mock private WetModeController mWetModeController;

    @Before
    public void setup() {
        initMocks(this);
        mService = new ModeManagerService(mContext, mWetModeController);
    }

    @Test
    public void onBoot_wetModeEnabled_resumeWetMode() {
        configureIntGlobalSetting(WET_MODE_ON, 1);

        mService.onBootPhase(PHASE_BOOT_COMPLETED);

        verify(mWetModeController).onBootComplete();
    }

    @Test
    public void onWetModeEnabled_registersChange() {
        when(mWetModeController.isWetModeEnabled()).thenReturn(true);

        simWetModeChange(true);

        verify(mWetModeController).onWetModeStateChange(/* wetModeEnabled= */true);
    }

    @Test
    public void onWetModeDisabled_registersChange() {
        when(mWetModeController.isWetModeEnabled()).thenReturn(false);

        simWetModeChange(false);

        verify(mWetModeController).onWetModeStateChange(/* wetModeEnabled= */false);
    }

    private void simWetModeChange(boolean enabled) {
        configureIntGlobalSetting(WET_MODE_ON, enabled ? 1 : 0);
        mService.mWetModeStateContentObserver.onChange(/* selfChange= */ false);
    }

    private void configureIntGlobalSetting(String key, int value) {
        Settings.Global.putInt(mContext.getContentResolver(), key, value);
    }
}


