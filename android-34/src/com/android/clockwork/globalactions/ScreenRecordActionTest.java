/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.clockwork.globalactions;

import static com.android.clockwork.globalactions.GlobalActionsProviderImpl.ScreenRecordAction;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ScreenRecordActionTest {
    @Mock
    private Context mContext;
    @Mock
    private Resources mResources;

    @Captor
    private ArgumentCaptor<Intent> mIntentCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void onPress_startsScreenRecordServiceIntent() {
        assertScreenActionLaunchesCorrectIntent(new ScreenRecordAction(mContext, mResources, false),
                "com.google.android.clockwork.systemui.screenrecord.START");
    }

    @Test
    public void onPress_stopsScreenRecordServiceIntent() {
        assertScreenActionLaunchesCorrectIntent(new ScreenRecordAction(mContext, mResources, true),
                "com.google.android.clockwork.systemui.screenrecord.STOP");
    }

    public void assertScreenActionLaunchesCorrectIntent(ScreenRecordAction action,
            String expectedIntentAction) {
        action.onPress();

        verify(mContext).startService(mIntentCaptor.capture());
        Intent intent = mIntentCaptor.getValue();

        final ComponentName expectedComponent = new ComponentName(
                "com.google.android.apps.wearable.systemui",
                "com.google.android.clockwork.systemui.screenrecord.WearRecordingService");

        assertEquals(expectedComponent, intent.getComponent());
        assertEquals(expectedIntentAction, intent.getAction());
    }
}
