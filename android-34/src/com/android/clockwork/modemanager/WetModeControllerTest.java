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

import static com.android.clockwork.modemanager.WetModeController.ACTION_WET_MODE_ENDED;
import static com.android.clockwork.modemanager.WetModeController.ACTION_WET_MODE_STARTED;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class WetModeControllerTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();

    private WetModeController mController;

    @Mock PowerManager mPowerManager;

    @Before
    public void setUp() {
        initMocks(this);
        mController = new WetModeController(mContext, mPowerManager, /* wetModeSupported= */ true);
    }

    @Test
    public void wetModeEnabled_sendsDeviceToSleep() {
        mController.onWetModeStateChange(/* wetModeEnabled= */ true);

        verify(mPowerManager).goToSleep(anyLong());
    }

    @Test
    public void wetModeEnabled_sendsWetModeStartedBroadcast() {
        mController.onWetModeStateChange(/* wetModeEnabled= */ true);

        List<Intent> broadcastIntents = shadowOf((Application) mContext).getBroadcastIntents();
        assertEquals(1, broadcastIntents.size());
        assertEquals(ACTION_WET_MODE_STARTED, broadcastIntents.get(0).getAction());
    }

    @Test
    public void wetModeDisabled_sendsWetModeEndedBroadcast() {
        mController.onWetModeStateChange(/* wetModeEnabled= */ false);

        List<Intent> broadcastIntents = shadowOf((Application) mContext).getBroadcastIntents();
        assertEquals(1, broadcastIntents.size());
        assertEquals(ACTION_WET_MODE_ENDED, broadcastIntents.get(0).getAction());
    }

    @Test
    public void wetModeNotSupported() {
        mController = new WetModeController(mContext, mPowerManager,
                /* wetModeSupported= */ false);

        mController.onWetModeStateChange(/* wetModeEnabled= */ true);

        List<Intent> broadcastIntents = shadowOf((Application) mContext).getBroadcastIntents();
        assertEquals(0, broadcastIntents.size());
    }

}
