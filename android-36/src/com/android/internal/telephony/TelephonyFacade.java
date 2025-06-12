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

package com.android.internal.telephony;

import android.net.TrafficStats;
import android.os.SystemClock;

/**
 * This class is a wrapper of various static methods to simplify unit tests with static methods
 */
public class TelephonyFacade {
    /**
     * Returns milliseconds since boot, including time spent in sleep.
     *
     * @return Current time since boot in milliseconds.
     */
    public long getElapsedSinceBootMillis() {
        return SystemClock.elapsedRealtime();
    }

    /**
     * Wrapper for {@link TrafficStats#getMobileTxBytes}.
     */
    public long getMobileTxBytes() {
        return TrafficStats.getMobileTxBytes();
    }

    /**
     * Wrapper for {@link TrafficStats#getMobileRxBytes}.
     */
    public long getMobileRxBytes() {
        return TrafficStats.getMobileRxBytes();
    }
}
