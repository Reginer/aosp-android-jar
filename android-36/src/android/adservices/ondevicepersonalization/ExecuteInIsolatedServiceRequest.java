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

import static android.adservices.ondevicepersonalization.ExecuteInIsolatedServiceResponse.DEFAULT_BEST_VALUE;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.os.PersistableBundle;

import com.android.adservices.ondevicepersonalization.flags.Flags;
import com.android.ondevicepersonalization.internal.util.AnnotationValidations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/** The request of {@link OnDevicePersonalizationManager#executeInIsolatedService}. */
@FlaggedApi(Flags.FLAG_EXECUTE_IN_ISOLATED_SERVICE_API_ENABLED)
public class ExecuteInIsolatedServiceRequest {
    /** The {@link ComponentName} of the {@link IsolatedService}. */
    @NonNull private ComponentName mService;

    /**
     * A {@link PersistableBundle} that is passed from the calling app to the {@link
     * IsolatedService}. The expected contents of this parameter are defined by the {@link
     * IsolatedService}. The platform does not interpret this parameter.
     */
    @NonNull private PersistableBundle mAppParams;

    /**
     * The set of spec to indicate output of {@link IsolatedService}. It's mainly used by platform.
     * If {@link OutputSpec} is set to {@link OutputSpec#DEFAULT}, OnDevicePersonalization will
     * ignore result returned by {@link IsolatedService}. If {@link OutputSpec} is built with {@link
     * OutputSpec#buildBestValueSpec}, OnDevicePersonalization will verify {@link
     * ExecuteOutput#getBestValue()} returned by {@link IsolatedService} within the max value range
     * set in {@link OutputSpec#getMaxIntValue} and add noise.
     */
    @NonNull private OutputSpec mOutputSpec;

    /**
     * The set of spec to indicate output of {@link IsolatedService}. It's mainly used by platform.
     * If {@link OutputSpec} is set to {@link OutputSpec#DEFAULT}, OnDevicePersonalization will
     * ignore result returned by {@link IsolatedService}. If {@link OutputSpec} is built with {@link
     * OutputSpec#buildBestValueSpec}, OnDevicePersonalization will verify {@link
     * ExecuteOutput#getBestValue()} returned by {@link IsolatedService} within the max value range
     * set in {@link OutputSpec#getMaxIntValue} and add noise.
     */
    public static class OutputSpec {
        /**
         * The default value of OutputType. If set, OnDevicePersonalization will ignore result
         * returned by {@link IsolatedService} and {@link ExecuteInIsolatedServiceResponse} doesn't
         * return any output data.
         */
        public static final int OUTPUT_TYPE_NULL = 0;

        /**
         * If set, {@link ExecuteInIsolatedServiceResponse#getBestValue()} will return an integer
         * that indicates the index of best values passed in {@link
         * ExecuteInIsolatedServiceRequest#getAppParams}.
         */
        public static final int OUTPUT_TYPE_BEST_VALUE = 1;

        /** @hide */
        @IntDef(
                prefix = "OUTPUT_TYPE_",
                value = {OUTPUT_TYPE_NULL, OUTPUT_TYPE_BEST_VALUE})
        @Retention(RetentionPolicy.SOURCE)
        public @interface OutputType {}

        /** Default value is OUTPUT_TYPE_NULL. */
        @OutputType private final int mOutputType;

        /** Optional. Only set when output option is OUTPUT_TYPE_BEST_VALUE. */
        @IntRange(from = DEFAULT_BEST_VALUE)
        private final int mMaxIntValue;

        /** The default value of {@link OutputSpec}. */
        @NonNull
        public static final OutputSpec DEFAULT =
                new OutputSpec(OUTPUT_TYPE_NULL, DEFAULT_BEST_VALUE);

        private OutputSpec(int outputType, int maxIntValue) {
            mMaxIntValue = maxIntValue;
            mOutputType = outputType;
        }

        /**
         * Creates the output spec to get best value out of {@code maxIntValue}. If set this, caller
         * can call {@link ExecuteInIsolatedServiceResponse#getBestValue} to get result.
         *
         * @param maxIntValue the maximum value {@link IsolatedWorker} can return to caller app.
         */
        public @NonNull static OutputSpec buildBestValueSpec(@IntRange(from = 0) int maxIntValue) {
            AnnotationValidations.validate(IntRange.class, null, maxIntValue, "from", 0);
            return new OutputSpec(OUTPUT_TYPE_BEST_VALUE, maxIntValue);
        }

        /**
         * Returns the output type of {@link IsolatedService}. The default value is {@link
         * OutputSpec#OUTPUT_TYPE_NULL}.
         */
        public @OutputType int getOutputType() {
            return mOutputType;
        }

        /**
         * Returns the value set in {@link OutputSpec#buildBestValueSpec}. The value is expected to
         * be {@link ExecuteInIsolatedServiceResponse#DEFAULT_BEST_VALUE} if {@link #getOutputType}
         * is {@link OutputSpec#OUTPUT_TYPE_NULL}.
         */
        public @IntRange(from = DEFAULT_BEST_VALUE) int getMaxIntValue() {
            return mMaxIntValue;
        }
    }

    /* package-private */ ExecuteInIsolatedServiceRequest(
            @NonNull ComponentName service,
            @NonNull PersistableBundle appParams,
            @NonNull OutputSpec outputSpec) {
        Objects.requireNonNull(service);
        Objects.requireNonNull(appParams);
        Objects.requireNonNull(outputSpec);
        this.mService = service;
        this.mAppParams = appParams;
        this.mOutputSpec = outputSpec;
    }

    /** The {@link ComponentName} of the {@link IsolatedService}. */
    public @NonNull ComponentName getService() {
        return mService;
    }

    /**
     * A {@link PersistableBundle} that is passed from the calling app to the {@link
     * IsolatedService}. The expected contents of this parameter are defined by the {@link
     * IsolatedService}. The platform does not interpret this parameter.
     */
    public @NonNull PersistableBundle getAppParams() {
        return mAppParams;
    }

    /**
     * The set of spec to indicate output of {@link IsolatedService}. It's mainly used by platform.
     * For example, platform calls {@link OutputSpec#getOutputType} and validates the result
     * received from {@link IsolatedService}.
     */
    public @NonNull OutputSpec getOutputSpec() {
        return mOutputSpec;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        // You can override field equality logic by defining either of the methods like:
        // boolean fieldNameEquals(ExecuteInIsolatedServiceRequest other) { ... }
        // boolean fieldNameEquals(FieldType otherValue) { ... }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecuteInIsolatedServiceRequest that = (ExecuteInIsolatedServiceRequest) o;
        //noinspection PointlessBooleanExpression
        return java.util.Objects.equals(mService, that.mService)
                && java.util.Objects.equals(mAppParams, that.mAppParams)
                && java.util.Objects.equals(mOutputSpec, that.mOutputSpec);
    }

    @Override
    public int hashCode() {
        // You can override field hashCode logic by defining methods like:
        // int fieldNameHashCode() { ... }

        int _hash = 1;
        _hash = 31 * _hash + java.util.Objects.hashCode(mService);
        _hash = 31 * _hash + java.util.Objects.hashCode(mAppParams);
        _hash = 31 * _hash + java.util.Objects.hashCode(mOutputSpec);
        return _hash;
    }

    /** A builder for {@link ExecuteInIsolatedServiceRequest} */
    public static final class Builder {

        private @NonNull ComponentName mService;
        private @NonNull PersistableBundle mAppParams = PersistableBundle.EMPTY;
        private @NonNull OutputSpec mOutputSpec = OutputSpec.DEFAULT;

        /**
         * Creates a new Builder.
         *
         * @param service The {@link ComponentName} of the {@link IsolatedService}.
         */
        public Builder(@NonNull ComponentName service) {
            Objects.requireNonNull(service);
            mService = service;
        }

        /**
         * A {@link PersistableBundle} that is passed from the calling app to the {@link
         * IsolatedService}. The expected contents of this parameter are defined by the {@link
         * IsolatedService}. The platform does not interpret this parameter.
         */
        public @NonNull Builder setAppParams(@NonNull PersistableBundle value) {
            Objects.requireNonNull(value);
            mAppParams = value;
            return this;
        }

        /**
         * The set of spec to indicate output of {@link IsolatedService}. It's mainly used by
         * platform. If {@link OutputSpec} is set to {@link OutputSpec#DEFAULT},
         * OnDevicePersonalization will ignore result returned by {@link IsolatedService}. If {@link
         * OutputSpec} is built with {@link OutputSpec#buildBestValueSpec}, OnDevicePersonalization
         * will verify {@link ExecuteOutput#getBestValue()} returned by {@link IsolatedService}
         * within the max value range set in {@link OutputSpec#getMaxIntValue} and add noise.
         */
        public @NonNull Builder setOutputSpec(@NonNull OutputSpec value) {
            Objects.requireNonNull(value);
            mOutputSpec = value;
            return this;
        }

        /** Builds the instance. */
        public @NonNull ExecuteInIsolatedServiceRequest build() {
            return new ExecuteInIsolatedServiceRequest(mService, mAppParams, mOutputSpec);
        }
    }
}
