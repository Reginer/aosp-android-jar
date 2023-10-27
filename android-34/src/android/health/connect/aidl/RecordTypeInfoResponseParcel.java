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

package android.health.connect.aidl;

import android.annotation.NonNull;
import android.health.connect.RecordTypeInfoResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.utils.RecordMapper;
import android.health.connect.internal.datatypes.utils.RecordTypePermissionCategoryMapper;
import android.health.connect.internal.datatypes.utils.RecordTypeRecordCategoryMapper;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** @hide */
public class RecordTypeInfoResponseParcel implements Parcelable {
    @NonNull
    public static final Parcelable.Creator<RecordTypeInfoResponseParcel> CREATOR =
            new Parcelable.Creator<RecordTypeInfoResponseParcel>() {
                @Override
                public RecordTypeInfoResponseParcel createFromParcel(Parcel in) {
                    return new RecordTypeInfoResponseParcel(in);
                }

                @Override
                public RecordTypeInfoResponseParcel[] newArray(int size) {
                    return new RecordTypeInfoResponseParcel[size];
                }
            };

    private Map<Integer, List<String>> mRecordTypeInfoResponses;

    public RecordTypeInfoResponseParcel(Parcel in) {
        int numberOfResponses = in.readInt();
        mRecordTypeInfoResponses = new HashMap<>(numberOfResponses);
        for (int i = 0; i < numberOfResponses; i++) {
            Integer recordType = in.readInt();
            List<String> contributingPackages = in.createStringArrayList();
            mRecordTypeInfoResponses.put(recordType, contributingPackages);
        }
    }

    public RecordTypeInfoResponseParcel(
            @NonNull Map<Integer, List<DataOrigin>> recordTypeInfoResponses) {
        Objects.requireNonNull(recordTypeInfoResponses);
        mRecordTypeInfoResponses = new HashMap<>(recordTypeInfoResponses.size());
        recordTypeInfoResponses.forEach(
                (recordType, contributingPackages) -> {
                    mRecordTypeInfoResponses.put(
                            recordType,
                            getContributingPackagesAsListOfString(contributingPackages));
                });
    }

    /**
     * Converts and returns a list of {@link DataOrigin} as a List of strings, mapped by the package
     * name in the dataOrigin object.
     */
    @NonNull
    public static List<String> getContributingPackagesAsListOfString(
            @NonNull List<DataOrigin> contributingPackagesAsDataOrigin) {
        return contributingPackagesAsDataOrigin.stream()
                .map(DataOrigin::getPackageName)
                .filter((packageName) -> packageName != null && !packageName.isEmpty())
                .collect(Collectors.toList());
    }

    /** Converts and returns a list of strings as a List of {@link DataOrigin}. */
    @NonNull
    public static List<DataOrigin> getContributingPackagesAsDataOrigin(
            @NonNull List<String> packagesAsListOfString) {
        return packagesAsListOfString.stream()
                .map((packageName) -> new DataOrigin.Builder().setPackageName(packageName).build())
                .collect(Collectors.toList());
    }

    /**
     * Returns a map of {@link RecordTypeInfoResponse} keyed by {@link
     * RecordTypeIdentifier.RecordType}.
     */
    public Map<Class<? extends Record>, RecordTypeInfoResponse> getRecordTypeInfoResponses() {
        Map<Class<? extends Record>, RecordTypeInfoResponse> responses =
                new HashMap<>(mRecordTypeInfoResponses.size());
        mRecordTypeInfoResponses.forEach(
                (recordType, contributingPackages) -> {
                    RecordTypeInfoResponse res =
                            new RecordTypeInfoResponse(
                                    RecordTypePermissionCategoryMapper
                                            .getHealthPermissionCategoryForRecordType(recordType),
                                    RecordTypeRecordCategoryMapper.getRecordCategoryForRecordType(
                                            recordType),
                                    getContributingPackagesAsDataOrigin(contributingPackages));
                    Class<? extends Record> recordTypeClass =
                            RecordMapper.getInstance()
                                    .getRecordIdToExternalRecordClassMap()
                                    .get(recordType);
                    responses.put(recordTypeClass, res);
                });
        return responses;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mRecordTypeInfoResponses.size());
        mRecordTypeInfoResponses.forEach(
                (recordType, contributingPackages) -> {
                    dest.writeInt(recordType);
                    dest.writeStringList(contributingPackages);
                });
    }
}
