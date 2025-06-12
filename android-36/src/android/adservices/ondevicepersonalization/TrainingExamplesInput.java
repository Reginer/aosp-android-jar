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
import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.adservices.ondevicepersonalization.flags.Flags;

import java.util.Objects;

/** The input data for {@link IsolatedWorker#onTrainingExamples}. */
public final class TrainingExamplesInput {
    /**
     * The name of the federated compute population. It should match the population name in {@link
     * FederatedComputeInput#getPopulationName}.
     */
    @NonNull private String mPopulationName = "";

    /**
     * The name of the task within the population. It should match task plan configured at remote
     * federated compute server. One population may have multiple tasks. The task name can be used
     * to uniquely identify the job.
     */
    @NonNull private String mTaskName = "";

    /**
     * Token used to support the resumption of training. If client app wants to use resumption token
     * to track what examples are already used in previous federated compute jobs, it need set
     * {@link TrainingExampleRecord.Builder#setResumptionToken}, OnDevicePersonalization will store
     * it and pass it here for generating new training examples.
     */
    @Nullable private byte[] mResumptionToken = null;

    /**
     * The data collection name to use to create training examples.
     */
    @Nullable private String mCollectionName;

    /** @hide */
    public TrainingExamplesInput(@NonNull TrainingExamplesInputParcel parcel) {
        this(
                parcel.getPopulationName(),
                parcel.getTaskName(),
                parcel.getResumptionToken(),
                parcel.getCollectionName());
    }

    /**
     * Creates a new TrainingExamplesInput.
     *
     * @param populationName The name of the federated compute population. It should match the
     *     population name in {@link FederatedComputeInput#getPopulationName}.
     * @param taskName The name of the task within the population. It should match task plan
     *     configured at remote federated compute server. One population may have multiple tasks.
     *     The task name can be used to uniquely identify the job.
     * @param resumptionToken Token used to support the resumption of training. If client app wants
     *     to use resumption token to track what examples are already used in previous federated
     *     compute jobs, it need set {@link TrainingExampleRecord.Builder#setResumptionToken},
     *     OnDevicePersonalization will store it and pass it here for generating new training
     *     examples.
     * @param collectionName The data collection name to use to create training examples.
     */
    @FlaggedApi(Flags.FLAG_DATA_CLASS_MISSING_CTORS_AND_GETTERS_ENABLED)
    public TrainingExamplesInput(
            @NonNull String populationName,
            @NonNull String taskName,
            @Nullable byte[] resumptionToken,
            @Nullable String collectionName) {
        this.mPopulationName = Objects.requireNonNull(populationName);
        this.mTaskName = Objects.requireNonNull(taskName);
        this.mResumptionToken = resumptionToken;
        this.mCollectionName = collectionName;
    }

    /**
     * The name of the federated compute population. It should match the population name in {@link
     * FederatedComputeInput#getPopulationName}.
     */
    public @NonNull String getPopulationName() {
        return mPopulationName;
    }

    /**
     * The name of the task within the population. It should match task plan configured at remote
     * federated compute server. One population may have multiple tasks. The task name can be used
     * to uniquely identify the job.
     */
    public @NonNull String getTaskName() {
        return mTaskName;
    }

    /**
     * Token used to support the resumption of training. If client app wants to use resumption token
     * to track what examples are already used in previous federated compute jobs, it need set
     * {@link TrainingExampleRecord.Builder#setResumptionToken}, OnDevicePersonalization will store
     * it and pass it here for generating new training examples.
     */
    public @Nullable byte[] getResumptionToken() {
        return mResumptionToken;
    }

    /** The data collection name to use to create training examples. */
    @FlaggedApi(Flags.FLAG_FCP_MODEL_VERSION_ENABLED)
    public @Nullable String getCollectionName() {
        return mCollectionName;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        TrainingExamplesInput that = (TrainingExamplesInput) o;
        //noinspection PointlessBooleanExpression
        return true
                && java.util.Objects.equals(mPopulationName, that.mPopulationName)
                && java.util.Objects.equals(mTaskName, that.mTaskName)
                && java.util.Arrays.equals(mResumptionToken, that.mResumptionToken)
                && java.util.Objects.equals(mCollectionName, that.mCollectionName);
    }

    @Override
    public int hashCode() {
        int _hash = 1;
        _hash = 31 * _hash + java.util.Objects.hashCode(mPopulationName);
        _hash = 31 * _hash + java.util.Objects.hashCode(mTaskName);
        _hash = 31 * _hash + java.util.Arrays.hashCode(mResumptionToken);
        _hash = 31 * _hash + java.util.Objects.hashCode(mCollectionName);
        return _hash;
    }
}
