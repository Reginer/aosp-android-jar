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
package android.adservices.ondevicepersonalization;

import android.annotation.FlaggedApi;
import android.annotation.IntRange;
import android.annotation.Nullable;

import com.android.adservices.ondevicepersonalization.flags.Flags;
import com.android.ondevicepersonalization.internal.util.AnnotationValidations;

/** The response of {@link OnDevicePersonalizationManager#executeInIsolatedService}. */
@FlaggedApi(Flags.FLAG_EXECUTE_IN_ISOLATED_SERVICE_API_ENABLED)
public class ExecuteInIsolatedServiceResponse {
    /**
     * An opaque reference to content that can be displayed in a {@link android.view.SurfaceView}.
     * This may be {@code null} if the {@link IsolatedService} has not generated any content to be
     * displayed within the calling app.
     */
    @Nullable private final SurfacePackageToken mSurfacePackageToken;

    /**
     * The default value of {@link ExecuteInIsolatedServiceResponse#getBestValue} if {@link
     * IsolatedService} didn't return any content.
     */
    public static final int DEFAULT_BEST_VALUE = -1;

    /**
     * The int value that was returned by the {@link IsolatedService} and applied noise. If {@link
     * IsolatedService} didn't return any content, the default value is {@link #DEFAULT_BEST_VALUE}.
     * If {@link IsolatedService} returns an integer value, we will apply the noise to the value and
     * the range of this value is between 0 and {@link
     * ExecuteInIsolatedServiceRequest.OutputSpec#getMaxIntValue()}.
     */
    private int mBestValue = DEFAULT_BEST_VALUE;

    /**
     * Creates a new ExecuteInIsolatedServiceResponse.
     *
     * @param surfacePackageToken an opaque reference to content that can be displayed in a {@link
     *     android.view.SurfaceView}. This may be {@code null} if the {@link IsolatedService} has
     *     not generated any content to be displayed within the calling app.
     * @param bestValue an int value that was returned by the {@link IsolatedService} and applied
     *     noise.If {@link ExecuteInIsolatedServiceRequest} output type is set to {@link
     *     ExecuteInIsolatedServiceRequest.OutputSpec#OUTPUT_TYPE_NULL}, the platform ignores the
     *     data returned by {@link IsolatedService} and returns the default value {@link
     *     #DEFAULT_BEST_VALUE}. If {@link ExecuteInIsolatedServiceRequest} output type is set to
     *     {@link ExecuteInIsolatedServiceRequest.OutputSpec#OUTPUT_TYPE_BEST_VALUE}, the platform
     *     validates {@link ExecuteOutput#getBestValue} between 0 and {@link
     *     ExecuteInIsolatedServiceRequest.OutputSpec#getMaxIntValue} and applies noise to result.
     */
    public ExecuteInIsolatedServiceResponse(
            @Nullable SurfacePackageToken surfacePackageToken,
            @IntRange(from = DEFAULT_BEST_VALUE) int bestValue) {
        AnnotationValidations.validate(IntRange.class, null, bestValue, "from", DEFAULT_BEST_VALUE);
        mSurfacePackageToken = surfacePackageToken;
        mBestValue = bestValue;
    }

    /** @hide */
    public ExecuteInIsolatedServiceResponse(@Nullable SurfacePackageToken surfacePackageToken) {
        mSurfacePackageToken = surfacePackageToken;
    }

    /**
     * Returns a {@link SurfacePackageToken}, which is an opaque reference to content that can be
     * displayed in a {@link android.view.SurfaceView}. This may be {@code null} if the {@link
     * IsolatedService} has not generated any content to be displayed within the calling app.
     */
    @Nullable
    public SurfacePackageToken getSurfacePackageToken() {
        return mSurfacePackageToken;
    }

    /**
     * Returns the int value that was returned by the {@link IsolatedService} and applied noise. If
     * {@link ExecuteInIsolatedServiceRequest} output type is set to {@link
     * ExecuteInIsolatedServiceRequest.OutputSpec#OUTPUT_TYPE_NULL}, the platform ignores the data
     * returned by {@link IsolatedService} and returns the default value {@link
     * #DEFAULT_BEST_VALUE}. If {@link ExecuteInIsolatedServiceRequest} output type is set to {@link
     * ExecuteInIsolatedServiceRequest.OutputSpec#OUTPUT_TYPE_BEST_VALUE}, the platform validates
     * {@link ExecuteOutput#getBestValue} between 0 and {@link
     * ExecuteInIsolatedServiceRequest.OutputSpec#getMaxIntValue()} and applies noise to result.
     */
    public @IntRange(from = DEFAULT_BEST_VALUE) int getBestValue() {
        return mBestValue;
    }
}
