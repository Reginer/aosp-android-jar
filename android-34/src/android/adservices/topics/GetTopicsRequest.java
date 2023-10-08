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
package android.adservices.topics;

import static android.adservices.topics.TopicsManager.EMPTY_SDK;
import static android.adservices.topics.TopicsManager.RECORD_OBSERVATION_DEFAULT;

import android.annotation.NonNull;

/** Get Topics Request. */
public final class GetTopicsRequest {

    /** Name of Ads SDK that is involved in this request. */
    private final String mAdsSdkName;

    /** Whether to record that the caller has observed the topics of the host app or not. */
    private final boolean mRecordObservation;

    private GetTopicsRequest(@NonNull Builder builder) {
        mAdsSdkName = builder.mAdsSdkName;
        mRecordObservation = builder.mRecordObservation;
    }

    /** Get the Sdk Name. */
    @NonNull
    public String getAdsSdkName() {
        return mAdsSdkName;
    }

    /** Get Record Observation. */
    public boolean shouldRecordObservation() {
        return mRecordObservation;
    }

    /** Builder for {@link GetTopicsRequest} objects. */
    public static final class Builder {
        private String mAdsSdkName = EMPTY_SDK;
        private boolean mRecordObservation = RECORD_OBSERVATION_DEFAULT;

        /** Creates a {@link Builder} for {@link GetTopicsRequest} objects. */
        public Builder() {}

        /**
         * Set Ads Sdk Name.
         *
         * <p>This must be called by SDKs running outside of the Sandbox. Other clients must not
         * call it.
         *
         * @param adsSdkName the Ads Sdk Name.
         */
        @NonNull
        public Builder setAdsSdkName(@NonNull String adsSdkName) {
            // This is the case the SDK calling from outside of the Sandbox.
            // Check if the caller set the adsSdkName
            if (adsSdkName == null) {
                throw new IllegalArgumentException(
                        "When calling Topics API outside of the Sandbox, caller should set Ads Sdk"
                                + " Name");
            }

            mAdsSdkName = adsSdkName;
            return this;
        }

        /**
         * Set the Record Observation.
         *
         * @param recordObservation whether to record that the caller has observed the topics of the
         *     host app or not. This will be used to determine if the caller can receive the topic
         *     in the next epoch.
         */
        @NonNull
        public Builder setShouldRecordObservation(boolean recordObservation) {
            mRecordObservation = recordObservation;
            return this;
        }

        /** Builds a {@link GetTopicsRequest} instance. */
        @NonNull
        public GetTopicsRequest build() {
            return new GetTopicsRequest(this);
        }
    }
}
