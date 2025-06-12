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
import android.annotation.IntDef;
import android.annotation.Nullable;

import com.android.adservices.ondevicepersonalization.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

/**
 * Exception thrown by OnDevicePersonalization APIs.
 *
 */
public class OnDevicePersonalizationException extends Exception {
    /**
     * The {@link IsolatedService} that was invoked failed to run.
     */
    public static final int ERROR_ISOLATED_SERVICE_FAILED = 1;

    /**
     * The {@link IsolatedService} was not started because personalization is disabled by
     * device configuration.
     */
    public static final int ERROR_PERSONALIZATION_DISABLED = 2;

    /** The ODP module was unable to load the {@link IsolatedService}.
     *
     * <p> Retrying may be successful for platform internal errors.
     */
    @FlaggedApi(Flags.FLAG_EXECUTE_IN_ISOLATED_SERVICE_API_ENABLED)
    public static final int ERROR_ISOLATED_SERVICE_LOADING_FAILED = 3;

    /**
     * The ODP specific manifest settings for the {@link IsolatedService} are either missing or
     * misconfigured.
     */
    @FlaggedApi(Flags.FLAG_EXECUTE_IN_ISOLATED_SERVICE_API_ENABLED)
    public static final int ERROR_ISOLATED_SERVICE_MANIFEST_PARSING_FAILED = 4;

    /** The {@link IsolatedService} was invoked but timed out before returning successfully.
     *
     * <p> This is likely due to an issue with the {@link IsolatedWorker} implementation taking too
     * long and retries are likely to fail.
     */
    @FlaggedApi(Flags.FLAG_EXECUTE_IN_ISOLATED_SERVICE_API_ENABLED)
    public static final int ERROR_ISOLATED_SERVICE_TIMEOUT = 5;

    /** The {@link IsolatedService}'s call to {@link FederatedComputeScheduler#schedule} failed.
     *
     <p> Retrying may be successful if the issue is due to a platform internal error.
     */
    @FlaggedApi(Flags.FLAG_EXECUTE_IN_ISOLATED_SERVICE_API_ENABLED)
    public static final int ERROR_SCHEDULE_TRAINING_FAILED = 6;

    /**
     * The {@link IsolatedService}'s call to {@link FederatedComputeScheduler#schedule} failed due
     * to missing or misconfigured federated compute settings URL in the manifest.
     */
    @FlaggedApi(Flags.FLAG_EXECUTE_IN_ISOLATED_SERVICE_API_ENABLED)
    public static final int ERROR_INVALID_TRAINING_MANIFEST = 7;

    /** Inference failed due to {@link ModelManager} not finding the downloaded model. */
    @FlaggedApi(Flags.FLAG_EXECUTE_IN_ISOLATED_SERVICE_API_ENABLED)
    public static final int ERROR_INFERENCE_MODEL_NOT_FOUND = 8;

    /** {@link ModelManager} failed to run inference.
     *
     <p> Retrying may be successful if the issue is due to a platform internal error.
     */
    @FlaggedApi(Flags.FLAG_EXECUTE_IN_ISOLATED_SERVICE_API_ENABLED)
    public static final int ERROR_INFERENCE_FAILED = 9;

    /** @hide */
    private static final Set<Integer> VALID_ERROR_CODE =
            Set.of(
                    ERROR_ISOLATED_SERVICE_FAILED,
                    ERROR_PERSONALIZATION_DISABLED,
                    ERROR_ISOLATED_SERVICE_LOADING_FAILED,
                    ERROR_ISOLATED_SERVICE_MANIFEST_PARSING_FAILED,
                    ERROR_ISOLATED_SERVICE_TIMEOUT,
                    ERROR_SCHEDULE_TRAINING_FAILED,
                    ERROR_INVALID_TRAINING_MANIFEST,
                    ERROR_INFERENCE_MODEL_NOT_FOUND,
                    ERROR_INFERENCE_FAILED);

    /** @hide */
    @IntDef(
            prefix = "ERROR_",
            value = {
                ERROR_ISOLATED_SERVICE_FAILED,
                ERROR_PERSONALIZATION_DISABLED,
                ERROR_ISOLATED_SERVICE_LOADING_FAILED,
                ERROR_ISOLATED_SERVICE_MANIFEST_PARSING_FAILED,
                ERROR_ISOLATED_SERVICE_TIMEOUT,
                ERROR_SCHEDULE_TRAINING_FAILED,
                ERROR_INVALID_TRAINING_MANIFEST,
                ERROR_INFERENCE_MODEL_NOT_FOUND,
                ERROR_INFERENCE_FAILED
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ErrorCode {}

    private final @ErrorCode int mErrorCode;

    @FlaggedApi(Flags.FLAG_UNHIDDEN_ON_DEVICE_PERSONALIZATION_EXCEPTION_ENABLED)
    public OnDevicePersonalizationException(@ErrorCode int errorCode) {
        mErrorCode = errorCode;
    }

    @FlaggedApi(Flags.FLAG_UNHIDDEN_ON_DEVICE_PERSONALIZATION_EXCEPTION_ENABLED)
    public OnDevicePersonalizationException(
            @ErrorCode int errorCode, @Nullable String message) {
        super(message);
        mErrorCode = errorCode;
    }

    @FlaggedApi(Flags.FLAG_UNHIDDEN_ON_DEVICE_PERSONALIZATION_EXCEPTION_ENABLED)
    public OnDevicePersonalizationException(
            @ErrorCode int errorCode, @Nullable Throwable cause) {
        super(cause);
        mErrorCode = errorCode;
    }

    @FlaggedApi(Flags.FLAG_UNHIDDEN_ON_DEVICE_PERSONALIZATION_EXCEPTION_ENABLED)
    public OnDevicePersonalizationException(
            @ErrorCode int errorCode, @Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
        mErrorCode = errorCode;
    }

    /** Returns the error code for this exception. */
    public @ErrorCode int getErrorCode() {
        return mErrorCode;
    }

    /**
     * @hide Only used by internal error code validation.
     */
    public static boolean isValidErrorCode(int errorCode) {
        return VALID_ERROR_CODE.contains(errorCode);
    }
}
