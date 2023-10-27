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

package android.nearby;

import android.annotation.IntDef;
import android.annotation.SystemApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Callback when broadcasting request using nearby specification.
 *
 * @hide
 */
@SystemApi
public interface BroadcastCallback {
    /** Broadcast was successful. */
    int STATUS_OK = 0;

    /** General status code when broadcast failed. */
    int STATUS_FAILURE = 1;

    /**
     * Broadcast failed as the callback was already registered.
     */
    int STATUS_FAILURE_ALREADY_REGISTERED = 2;

    /**
     * Broadcast failed as the request contains excessive data.
     */
    int STATUS_FAILURE_SIZE_EXCEED_LIMIT = 3;

    /**
     * Broadcast failed as the client doesn't hold required permissions.
     */
    int STATUS_FAILURE_MISSING_PERMISSIONS = 4;

    /** @hide **/
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATUS_OK, STATUS_FAILURE, STATUS_FAILURE_ALREADY_REGISTERED,
            STATUS_FAILURE_SIZE_EXCEED_LIMIT, STATUS_FAILURE_MISSING_PERMISSIONS})
    @interface BroadcastStatus {
    }

    /**
     * Called when broadcast status changes.
     */
    void onStatusChanged(@BroadcastStatus int status);
}
