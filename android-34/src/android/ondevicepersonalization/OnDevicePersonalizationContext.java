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

package android.ondevicepersonalization;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.os.OutcomeReceiver;

import java.util.concurrent.Executor;

/**
 * Container for per-request state and APIs for code that runs in the isolated process.
 *
 * @hide
 */
public interface OnDevicePersonalizationContext {
    /**
     * Returns a DAO for the REMOTE_DATA table.
     * @return A {@link ImmutableMap} object that provides access to the REMOTE_DATA table.
     */
    @NonNull ImmutableMap getRemoteData();

    /**
     * Returns a DAO for the LOCAL_DATA table.
     * @return A {@link MutableMap} object that provides access to the LOCAL_DATA table.
     */
    @NonNull MutableMap getLocalData();

    /** Return an Event URL for a single bid. */
    void getEventUrl(
            int eventType,
            @NonNull String bidId,
            @NonNull EventUrlOptions options,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<String, Exception> receiver);

    // TODO(b/228200518): Add DAOs for LOCAL_DATA and USER_DATA.
}
