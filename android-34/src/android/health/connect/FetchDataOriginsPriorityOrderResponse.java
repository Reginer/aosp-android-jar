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

/** @hide */
@SystemApi
public final class FetchDataOriginsPriorityOrderResponse {

    private final List<DataOrigin> mDataOriginsPriorityOrder;

    /**
     * @param dataOriginsPriorityOrder dataOrigins in priority order
     * @hide
     */
    public FetchDataOriginsPriorityOrderResponse(
            @NonNull List<DataOrigin> dataOriginsPriorityOrder) {
        Objects.requireNonNull(dataOriginsPriorityOrder);

        mDataOriginsPriorityOrder = dataOriginsPriorityOrder;
    }

    /**
     * @return dataOrigins in priority order
     */
    @NonNull
    public List<DataOrigin> getDataOriginsPriorityOrder() {
        return mDataOriginsPriorityOrder;
    }
}
