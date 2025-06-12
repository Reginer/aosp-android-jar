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

package android.health.connect;

import static android.health.connect.Constants.MAXIMUM_PAGE_SIZE;
import static android.health.connect.Constants.MINIMUM_PAGE_SIZE;
import static android.health.connect.datatypes.validation.ValidationUtils.requireInRange;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;

import android.annotation.FlaggedApi;
import android.annotation.IntRange;
import android.health.connect.aidl.ReadMedicalResourcesRequestParcel;

/**
 * A base class to represent a read request for {@link HealthConnectManager#readMedicalResources}.
 */
@FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
public abstract class ReadMedicalResourcesRequest {
    private final int mPageSize;

    /**
     * @param pageSize The maximum number of {@code MedicalResource}s to be returned by the read
     *     operation.
     * @throws IllegalArgumentException if {@code pageSize} is less than 1 or more than 5000.
     * @hide
     */
    protected ReadMedicalResourcesRequest(
            @IntRange(from = MINIMUM_PAGE_SIZE, to = MAXIMUM_PAGE_SIZE) int pageSize) {
        requireInRange(pageSize, MINIMUM_PAGE_SIZE, MAXIMUM_PAGE_SIZE, "pageSize");
        mPageSize = pageSize;
    }

    /**
     * Returns maximum number of {@code MedicalResource}s to be returned by the read operation if
     * set, 1000 otherwise.
     */
    @IntRange(from = MINIMUM_PAGE_SIZE, to = MAXIMUM_PAGE_SIZE)
    public int getPageSize() {
        return mPageSize;
    }

    /**
     * Returns an instance of {@link ReadMedicalResourcesRequestParcel} to carry the request.
     *
     * @hide
     */
    @SuppressWarnings("HiddenAbstractMethod")
    abstract ReadMedicalResourcesRequestParcel toParcel();
}
