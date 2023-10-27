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
import android.health.connect.datatypes.RecordTypeIdentifier;

import java.util.List;

/**
 * Holder for the following information for each {@link RecordTypeIdentifier.RecordType}:
 *
 * <ul>
 *   <li>{@link HealthPermissionCategory} for the record type.
 *   <li>{@link HealthDataCategory} for the record type.
 *   <li>Packages using this record type.
 * </ul>
 *
 * @hide
 */
@SystemApi
public class RecordTypeInfoResponse {
    @HealthPermissionCategory.Type private final int mPermissionCategory;
    @HealthDataCategory.Type private final int mDataCategory;
    private final List<DataOrigin> mContributingPackages;

    /** @hide */
    public RecordTypeInfoResponse(
            @NonNull @HealthPermissionCategory.Type int permissionCategory,
            @NonNull @HealthDataCategory.Type int dataCategory,
            @NonNull List<DataOrigin> contributingPackages) {
        this.mPermissionCategory = permissionCategory;
        this.mDataCategory = dataCategory;
        this.mContributingPackages = contributingPackages;
    }

    /** Returns {@link HealthPermissionCategory} for the {@link RecordTypeIdentifier.RecordType}. */
    @HealthPermissionCategory.Type
    public int getPermissionCategory() {
        return mPermissionCategory;
    }

    /** Returns {@link HealthDataCategory} for the {@link RecordTypeIdentifier.RecordType}. */
    @HealthDataCategory.Type
    public int getDataCategory() {
        return mDataCategory;
    }

    /** Returns contributing packages for the {@link RecordTypeIdentifier.RecordType}. */
    @NonNull
    public List<DataOrigin> getContributingPackages() {
        return mContributingPackages;
    }
}
