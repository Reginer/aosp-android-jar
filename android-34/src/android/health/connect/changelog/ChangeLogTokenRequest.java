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

package android.health.connect.changelog;

import android.annotation.NonNull;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.utils.RecordMapper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A class to request changelog token using {@link HealthConnectManager#getChangeLogToken}
 *
 * @see HealthConnectManager#getChangeLogToken
 */
public final class ChangeLogTokenRequest implements Parcelable {
    private final Set<DataOrigin> mDataOriginFilters;
    private final Set<Class<? extends Record>> mRecordTypes;

    /**
     * @param dataOriginFilters list of package names to filter the data
     * @param recordTypes list of records for which change log is required
     */
    private ChangeLogTokenRequest(
            @NonNull Set<DataOrigin> dataOriginFilters,
            @NonNull Set<Class<? extends Record>> recordTypes) {
        Objects.requireNonNull(recordTypes);
        Objects.requireNonNull(dataOriginFilters);

        mDataOriginFilters = dataOriginFilters;
        mRecordTypes = recordTypes;
    }

    private ChangeLogTokenRequest(@NonNull Parcel in) {
        RecordMapper recordMapper = RecordMapper.getInstance();
        Set<Class<? extends Record>> recordTypes = new ArraySet<>();
        for (@RecordTypeIdentifier.RecordType int recordType : in.createIntArray()) {
            recordTypes.add(recordMapper.getRecordIdToExternalRecordClassMap().get(recordType));
        }
        mRecordTypes = recordTypes;
        Set<DataOrigin> dataOrigin = new ArraySet<>();
        for (String packageName : in.createStringArrayList()) {
            dataOrigin.add(new DataOrigin.Builder().setPackageName(packageName).build());
        }
        mDataOriginFilters = dataOrigin;
    }

    @NonNull
    public static final Creator<ChangeLogTokenRequest> CREATOR =
            new Creator<ChangeLogTokenRequest>() {
                @Override
                public ChangeLogTokenRequest createFromParcel(@NonNull Parcel in) {
                    return new ChangeLogTokenRequest(in);
                }

                @Override
                public ChangeLogTokenRequest[] newArray(int size) {
                    return new ChangeLogTokenRequest[size];
                }
            };

    /** Returns list of package names corresponding to which the logs are required */
    @NonNull
    public Set<DataOrigin> getDataOriginFilters() {
        return mDataOriginFilters;
    }

    /** Returns list of record classes for which the logs are to be fetched */
    @NonNull
    public Set<Class<? extends Record>> getRecordTypes() {
        return mRecordTypes;
    }

    /**
     * Returns List of Record types for which logs are to be fetched
     *
     * @hide
     */
    @NonNull
    public int[] getRecordTypesArray() {
        return getRecordTypesAsInteger();
    }

    /**
     * Returns List of Record types for which logs are to be fetched
     *
     * @hide
     */
    @NonNull
    public List<Integer> getRecordTypesList() {
        return Arrays.stream(getRecordTypesAsInteger()).boxed().collect(Collectors.toList());
    }

    /**
     * Returns list of package names corresponding to which the logs are required
     *
     * @hide
     */
    @NonNull
    public List<String> getPackageNamesToFilter() {
        return getPackageNames();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeIntArray(getRecordTypesAsInteger());
        dest.writeStringList(getPackageNames());
    }

    @NonNull
    private int[] getRecordTypesAsInteger() {
        int[] recordTypes = new int[mRecordTypes.size()];
        int index = 0;
        for (Class<? extends Record> recordClass : mRecordTypes) {
            recordTypes[index++] = RecordMapper.getInstance().getRecordType(recordClass);
        }
        return recordTypes;
    }

    @NonNull
    private List<String> getPackageNames() {
        List<String> packageNamesToFilter = new ArrayList<>(mDataOriginFilters.size());
        mDataOriginFilters.forEach(
                (dataOrigin) -> packageNamesToFilter.add(dataOrigin.getPackageName()));
        return packageNamesToFilter;
    }

    /** Builder for {@link ChangeLogTokenRequest} */
    public static final class Builder {
        private final Set<Class<? extends Record>> mRecordTypes = new ArraySet<>();
        private final Set<DataOrigin> mDataOriginFilters = new ArraySet<>();

        /**
         * @param recordType type of record for which change log is required. If not set includes
         *     all record types
         */
        @NonNull
        public Builder addRecordType(@NonNull Class<? extends Record> recordType) {
            Objects.requireNonNull(recordType);

            mRecordTypes.add(recordType);
            return this;
        }

        /**
         * @param dataOriginFilter list of package names on which to filter the data.
         *     <p>If not set logs from all the sources will be returned
         */
        @NonNull
        public Builder addDataOriginFilter(@NonNull DataOrigin dataOriginFilter) {
            Objects.requireNonNull(dataOriginFilter);

            mDataOriginFilters.add(dataOriginFilter);
            return this;
        }

        /**
         * Returns Object of {@link ChangeLogTokenRequest}
         *
         * @throws IllegalArgumentException if record types are empty
         */
        @NonNull
        public ChangeLogTokenRequest build() {
            return new ChangeLogTokenRequest(mDataOriginFilters, mRecordTypes);
        }
    }
}
