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

/**
 * Internal constants for health-connect.
 *
 * @hide
 */
public final class Constants {
    public static final String MANAGE_HEALTH_PERMISSIONS_NAME =
            "android.permission.MANAGE_HEALTH_PERMISSIONS";

    public static final String HEALTH_PERMISSION_GROUP_NAME = "android.permission-group.HEALTH";

    public static final boolean DEBUG = false;
    public static final int DEFAULT_INT = -1;
    public static final long DEFAULT_LONG = -1;
    public static final double DEFAULT_DOUBLE = Double.MIN_VALUE;
    public static final int DEFAULT_PAGE_SIZE = 1000;
    public static final int MAXIMUM_PAGE_SIZE = 5000;

    public static final int UPSERT = 0;
    public static final int DELETE = 1;
    public static final int READ = 2;
    public static final String PARENT_KEY = "parent_key";
}
