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

package com.android.clockwork.displayoffload;

import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_HAL_REMOTE_EXCEPTION;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_HAL_STATUS_NOT_OK;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_CONTAINS_CYCLE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_CONVERSION_FAILURE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_INVALID_RESOURCE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.FIELD_ERROR_TYPE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.FIELD_ERROR_MSG;

import android.annotation.IntDef;
import android.os.Bundle;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Exception class that encapsulates error and related messages from DisplayOffloadService. */
public class DisplayOffloadException extends Exception {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                ERROR_LAYOUT_CONTAINS_CYCLE,
                ERROR_LAYOUT_INVALID_RESOURCE,
                ERROR_LAYOUT_CONVERSION_FAILURE,
                ERROR_HAL_STATUS_NOT_OK,
                ERROR_HAL_REMOTE_EXCEPTION,
            })
    public @interface ErrorType {}

    public final Bundle mErrorBundle;

    /**
     * Create DisplayOffloadException without a message or a bundle
     *
     * @param errorType type of error from ErrorType
     */
    public DisplayOffloadException(@ErrorType int errorType) {
        this(errorType, null, "");
    }

    /**
     * Create DisplayOffloadException without a bundle extra
     *
     * @param errorType type of error from ErrorType
     * @param message optional message
     */
    public DisplayOffloadException(@ErrorType int errorType, String message) {
        this(errorType, null, message);
    }

    /**
     * Create DisplayOffloadException with a bundle extra
     *
     * @param errorType type of error from ErrorType
     * @param errorExtra optional bundle that contains more information about the error
     * @param message optional message
     */
    public DisplayOffloadException(@ErrorType int errorType, Bundle errorExtra, String message) {
        super(message);
        if (errorExtra == null) {
            mErrorBundle = new Bundle();
        } else {
            mErrorBundle = errorExtra;
        }
        mErrorBundle.putInt(FIELD_ERROR_TYPE, errorType);
        if (!message.isEmpty()) {
            mErrorBundle.putString(FIELD_ERROR_MSG, message);
        }
    }

    public int getErrorType() {
        return mErrorBundle.getInt(FIELD_ERROR_TYPE, 0);
    }
}
