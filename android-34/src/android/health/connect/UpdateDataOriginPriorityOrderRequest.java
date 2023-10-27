/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.health.connect.datatypes.DataOrigin;

import java.util.List;
import java.util.Objects;

/**
 * Request class for setting app priority in the HealthConnect platform
 *
 * @hide
 */
@SystemApi
public final class UpdateDataOriginPriorityOrderRequest {
    private final List<DataOrigin> mDataOriginInOrder;
    @HealthDataCategory.Type private final int mDataCategory;

    /**
     * @param dataOriginInOrder new priority order of the apps
     * @param dataCategory {@link HealthDataCategory} for the priority order
     */
    public UpdateDataOriginPriorityOrderRequest(
            @NonNull List<DataOrigin> dataOriginInOrder,
            @HealthDataCategory.Type int dataCategory) {
        Objects.requireNonNull(dataOriginInOrder);

        mDataOriginInOrder = dataOriginInOrder;
        mDataCategory = dataCategory;
    }

    /**
     * @return List of {@link DataOrigin} in priority order
     */
    @NonNull
    public List<DataOrigin> getDataOriginInOrder() {
        return mDataOriginInOrder;
    }

    /**
     * @return {@link HealthDataCategory} for which to get the priority
     */
    @HealthDataCategory.Type
    public int getDataCategory() {
        return mDataCategory;
    }
}
