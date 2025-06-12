/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.server.net;

/**
 * NetworkManagement local system service interface.
 *
 * @hide Only for use within the system server.
 */
public abstract class NetworkManagementInternal {
    /**
     * Checks if network is restricted for {@param uid} as per the app idle state, device idle mode,
     * battery and data saver modes.
     */
    public abstract boolean isNetworkRestrictedForUid(int uid);
}
