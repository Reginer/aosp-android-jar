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

package com.android.net.module.util;

import android.util.Log;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;

/**
 * Single {@link Clock} that will return the best available time from a set of
 * prioritized {@link Clock} instances.
 * <p>
 * For example, when {@link SystemClock#currentNetworkTimeClock()} isn't able to
 * provide the time, this class could use {@link Clock#systemUTC()} instead.
 *
 * Note that this is re-implemented based on {@code android.os.BestClock} to be used inside
 * the mainline module. And the class does NOT support serialization.
 *
 * @hide
 */
final public class BestClock extends Clock {
    private static final String TAG = "BestClock";
    private final ZoneId mZone;
    private final Clock[] mClocks;

    public BestClock(ZoneId zone, Clock... clocks) {
        super();
        this.mZone = zone;
        this.mClocks = clocks;
    }

    @Override
    public long millis() {
        for (Clock clock : mClocks) {
            try {
                return clock.millis();
            } catch (DateTimeException e) {
                // Ignore and attempt the next clock
                Log.w(TAG, e.toString());
            }
        }
        throw new DateTimeException(
                "No clocks in " + Arrays.toString(mClocks) + " were able to provide time");
    }

    @Override
    public ZoneId getZone() {
        return mZone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new BestClock(zone, mClocks);
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(millis());
    }
}
