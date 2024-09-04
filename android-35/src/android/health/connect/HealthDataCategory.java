/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.health.connect;

import android.annotation.IntDef;
import android.annotation.SystemApi;
import android.health.connect.datatypes.Record;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents DataCategory for a {@link Record}. A record can only belong to one and only one {@link
 * HealthDataCategory}.
 *
 * @hide
 */
@SystemApi
public class HealthDataCategory {
    public static final int UNKNOWN = 0;
    public static final int ACTIVITY = 1;
    public static final int BODY_MEASUREMENTS = 2;
    public static final int CYCLE_TRACKING = 3;
    public static final int NUTRITION = 4;
    public static final int SLEEP = 5;
    public static final int VITALS = 6;

    private HealthDataCategory() {}

    /** @hide */
    @IntDef({UNKNOWN, ACTIVITY, BODY_MEASUREMENTS, CYCLE_TRACKING, NUTRITION, SLEEP, VITALS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {}
}
