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

package android.adservices.ondevicepersonalization;

import android.annotation.FlaggedApi;
import android.annotation.Nullable;

import com.android.adservices.ondevicepersonalization.flags.Flags;

/**
 * A class that an {@link IsolatedService} can use to signal a failure in handling a request and
 * return an error to be logged and aggregated. The error is not reported to the app that invoked
 * the {@link IsolatedService} in order to prevent data leakage from the {@link IsolatedService} to
 * an app. The platform does not interpret the error code, it only logs and aggregates it.
 */
public final class IsolatedServiceException extends Exception {
    private final int mErrorCode;

    /**
     * Creates an {@link IsolatedServiceException} with an error code to be logged. The meaning of
     * the error code is defined by the {@link IsolatedService}. The platform does not interpret
     * the error code.
     *
     * @param errorCode An error code defined by the {@link IsolatedService}.
     */
    public IsolatedServiceException(int errorCode) {
        this(errorCode, "IsolatedServiceException: Error " + errorCode, null);
    }

    /**
     * Creates an {@link IsolatedServiceException} with an error code to be logged. The meaning of
     * the error code is defined by the {@link IsolatedService}. The platform does not interpret
     * the error code.
     *
     * @param errorCode An error code defined by the {@link IsolatedService}.
     * @param cause the cause of this exception.
     */
    @FlaggedApi(Flags.FLAG_DATA_CLASS_MISSING_CTORS_AND_GETTERS_ENABLED)
    public IsolatedServiceException(
            int errorCode,
            @Nullable Throwable cause) {
        this(errorCode, "IsolatedServiceException: Error " + errorCode, cause);
    }

    /**
     * Creates an {@link IsolatedServiceException} with an error code to be logged. The meaning of
     * the error code is defined by the {@link IsolatedService}. The platform does not interpret
     * the error code.
     *
     * @param errorCode An error code defined by the {@link IsolatedService}.
     * @param message the exception message.
     * @param cause the cause of this exception.
     */
    @FlaggedApi(Flags.FLAG_DATA_CLASS_MISSING_CTORS_AND_GETTERS_ENABLED)
    public IsolatedServiceException(
            int errorCode,
            @Nullable String message,
            @Nullable Throwable cause) {
        super(message, cause);
        mErrorCode = errorCode;
    }

    /**
     * Returns the error code for this exception.
     */
    @FlaggedApi(Flags.FLAG_DATA_CLASS_MISSING_CTORS_AND_GETTERS_ENABLED)
    public int getErrorCode() {
        return mErrorCode;
    }
}
