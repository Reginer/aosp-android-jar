/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.healthfitness.flags;

/**
 * Class containing all DB versions of HC database.
 *
 * @hide
 */
public final class DatabaseVersions {
    public static final int DB_VERSION_UUID_BLOB = 9;
    public static final int DB_VERSION_GENERATED_LOCAL_TIME = 10;
    public static final int DB_VERSION_SKIN_TEMPERATURE = 11;
    public static final int DB_VERSION_PLANNED_EXERCISE_SESSIONS = 12;
    // No schema changes between version 12 and 13. See ag/26747988 for more details.
    public static final int DB_VERSION_PLANNED_EXERCISE_SESSIONS_FLAG_RELEASE = 13;
    public static final int DB_VERSION_MINDFULNESS_SESSION = 14;

    /** The DB version in which the schema changes for PHR MVP were added. */
    public static final int DB_VERSION_PERSONAL_HEALTH_RECORD = 15;

    public static final int DB_VERSION_ACTIVITY_INTENSITY = 16;
    public static final int DB_VERSION_ECOSYSTEM_METRICS = 17;

    // For historical reasons, we do not support versions below this
    // See go/hc-mainline-dev/trunk_stable/db-and-aconfig#a-bit-of-history
    public static final int MIN_SUPPORTED_DB_VERSION = DB_VERSION_UUID_BLOB;

    // DB version of the last feature that has been fully rolled out to public.
    // See go/hc-mainline-dev/trunk_stable/db-and-aconfig#last-rolled-out-db-version
    public static final int LAST_ROLLED_OUT_DB_VERSION = DB_VERSION_MINDFULNESS_SESSION;

    private DatabaseVersions() {}
}
