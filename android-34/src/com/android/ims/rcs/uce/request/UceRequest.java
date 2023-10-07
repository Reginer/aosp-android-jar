/*
 * Copyright (c) 2020 The Android Open Source Project
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

package com.android.ims.rcs.uce.request;

import android.annotation.IntDef;
import android.net.Uri;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * The interface of the UCE request to request the capabilities from the carrier network.
 */
public interface UceRequest {
    /** The request type: CAPABILITY */
    int REQUEST_TYPE_CAPABILITY = 1;

    /** The request type: AVAILABILITY */
    int REQUEST_TYPE_AVAILABILITY = 2;

    /**@hide*/
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = "REQUEST_TYPE_", value = {
            REQUEST_TYPE_CAPABILITY,
            REQUEST_TYPE_AVAILABILITY
    })
    @interface UceRequestType {}

    /**
     * Set the UceRequestCoordinator ID associated with this request.
     */
    void setRequestCoordinatorId(long coordinatorId);

    /**
     * @return Return the UceRequestCoordinator ID associated with this request.
     */
    long getRequestCoordinatorId();

    /**
     * @return Return the task ID of this request.
     */
    long getTaskId();

    /**
     * Notify that the request is finish.
     */
    void onFinish();

    /**
     * Set the contact URIs associated with this request.
     */
    void setContactUri(List<Uri> uris);

    /**
     * Execute the request.
     */
    void executeRequest();
}
