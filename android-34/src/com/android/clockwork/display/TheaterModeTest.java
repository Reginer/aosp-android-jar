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

import static com.android.clockwork.display.TheaterMode.THEATER_MODE_START_TIME;
import static com.android.clockwork.remote.Home.ACTION_ENABLE_TOUCH_THEATER_MODE_END;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowAlarmManager;

import java.util.List;

// TODO(b/237596183): clean up the unit tests. remove calls of implementation details
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class TheaterModeTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();

    private TheaterMode mTheaterMode;
    private ContentResolver mContentResolver;
    private SharedPreferences mSharedPreferences;
    private ShadowAlarmManager mAlarmManager;

    @Before
    public void setUp() {
        mTheaterMode = new TheaterMode(mContext, "test_off_title", "test_off_content");
        mContentResolver = mContext.getContentResolver();
        mSharedPreferences = TheaterMode.getSharedPreferences(mContext);
        mAlarmManager = shadowOf(mContext.getSystemService(AlarmManager.class));
    }

    private void triggerTheaterModeChange(int enable) {
        Settings.Global.putInt(mContentResolver, Settings.Global.THEATER_MODE_ON, enable);
    }

    @Test
    public void initialize_theaterModeOnFromSettings_startsTheaterMode() {
        triggerTheaterModeChange(1);

        mContext.sendBroadcast(new Intent(Intent.ACTION_SCREEN_OFF));
        mContext.sendBroadcast(new Intent(Intent.ACTION_SCREEN_ON));

        List<Intent> broadcastIntents = shadowOf((Application) mContext).getBroadcastIntents();
        assertEquals(2, broadcastIntents.size());
        assertNotNull(mTheaterMode.mExitIntent);
    }

    @Test
    public void activate_theaterModeOnFromSettings_startTheaterMode() {
        long expectedCurrentTime = System.currentTimeMillis();
        long expectedTheaterAlarm = expectedCurrentTime + TheaterMode.EXIT_DELAY_MS;
        triggerTheaterModeChange(1);

        assertEquals("Theater mode alarm should be set up", 1,
                mAlarmManager.getScheduledAlarms().size());
        assertTrue(expectedCurrentTime <=
                mSharedPreferences.getLong(THEATER_MODE_START_TIME, Long.MIN_VALUE));
        assertTrue(expectedTheaterAlarm <=
                mAlarmManager.getScheduledAlarms().get(0).triggerAtTime);
    }

    @Test
    public void turnOff_theaterModeOnFromSettings_endsTheaterMode() {
        triggerTheaterModeChange(1);
        triggerTheaterModeChange(0);

        mContext.sendBroadcast(new Intent(Intent.ACTION_SCREEN_OFF));
        mContext.sendBroadcast(new Intent(Intent.ACTION_SCREEN_ON));
        List<Intent> broadcastIntents = shadowOf((Application) mContext).getBroadcastIntents();

        assertEquals(3, broadcastIntents.size());
        assertEquals(ACTION_ENABLE_TOUCH_THEATER_MODE_END, broadcastIntents.get(0).getAction());
        assertEquals("There should be no alarm since expiration.", 0,
                mAlarmManager.getScheduledAlarms().size());
    }

    @Test
    public void activate_enabledTheaterModeOnReboot() {
        Settings.Global.putInt(mContentResolver, Settings.Global.THEATER_MODE_ON, 1);
        mSharedPreferences.edit().putLong(THEATER_MODE_START_TIME,
                System.currentTimeMillis()).apply();

        mTheaterMode = new TheaterMode(mContext, "test_off_title", "test_off_context");

        assertEquals(1, Settings.Global.getInt(mContentResolver,
                Settings.Global.THEATER_MODE_ON, -1));
    }

    @Ignore // TODO(b/238189813): fix dependency
    @Test
    public void turnOff_expiredTheaterModeOnReboot() {
        Settings.Global.putInt(mContentResolver, Settings.Global.THEATER_MODE_ON, 1);
        // Set up an expired alarm.
        mSharedPreferences.edit().putLong(THEATER_MODE_START_TIME,
                System.currentTimeMillis() - 2 * TheaterMode.EXIT_DELAY_MS).apply();

        // Reboot
        mTheaterMode = new TheaterMode(mContext, "test_off_title", "test_off_context");

        assertEquals(0, Settings.Global.getInt(mContentResolver,
                Settings.Global.THEATER_MODE_ON, -1));
    }

    @Test
    public void testExitReceived() throws Exception {
        triggerTheaterModeChange(1);
        mTheaterMode.mExitIntent.send();

        assertEquals(0,
                Settings.Global.getInt(mContentResolver, Settings.Global.THEATER_MODE_ON, -1));
    }
}
