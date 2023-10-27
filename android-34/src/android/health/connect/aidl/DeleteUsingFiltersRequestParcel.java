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

import static android.health.connect.Constants.DEFAULT_LONG;

import android.annotation.NonNull;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.TimeRangeFilterHelper;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.internal.datatypes.utils.RecordMapper;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @see DeleteUsingFiltersRequest
 * @hide
 */
public class DeleteUsingFiltersRequestParcel implements Parcelable {
    public static final Creator<DeleteUsingFiltersRequestParcel> CREATOR =
            new Creator<>() {
                @Override
                public DeleteUsingFiltersRequestParcel createFromParcel(Parcel in) {
                    return new DeleteUsingFiltersRequestParcel(in);
                }

                @Override
                public DeleteUsingFiltersRequestParcel[] newArray(int size) {
                    return new DeleteUsingFiltersRequestParcel[size];
                }
            };
    private List<String> mPackageNameFilters;
    private final int[] mRecordTypeFilters;
    private final long mStartTime;
    private final long mEndTime;
    private final RecordIdFiltersParcel mRecordIdFiltersParcel;
    private final boolean mLocalTimeFilter;

    protected DeleteUsingFiltersRequestParcel(Parcel in) {
        mPackageNameFilters = in.createStringArrayList();
        mRecordTypeFilters = in.createIntArray();
        mStartTime = in.readLong();
        mEndTime = in.readLong();
        mLocalTimeFilter = in.readBoolean();
        mRecordIdFiltersParcel =
                in.readParcelable(
                        RecordIdFiltersParcel.class.getClassLoader(), RecordIdFiltersParcel.class);
    }

    public DeleteUsingFiltersRequestParcel(DeleteUsingFiltersRequest request) {
        mPackageNameFilters =
                request.getDataOrigins().stream()
                        .map(DataOrigin::getPackageName)
                        .collect(Collectors.toList());
        mRecordTypeFilters =
                request.getRecordTypes().stream()
                        .mapToInt(
                                recordType -> RecordMapper.getInstance().getRecordType(recordType))
                        .toArray();

        if (request.getTimeRangeFilter() == null) {
            // Use defaults values to signal filters not set
            mStartTime = DEFAULT_LONG;
            mEndTime = DEFAULT_LONG;
        } else {
            mStartTime =
                    TimeRangeFilterHelper.getFilterStartTimeMillis(request.getTimeRangeFilter());
            mEndTime = TimeRangeFilterHelper.getFilterEndTimeMillis(request.getTimeRangeFilter());
        }
        mLocalTimeFilter = TimeRangeFilterHelper.isLocalTimeFilter(request.getTimeRangeFilter());
        mRecordIdFiltersParcel = new RecordIdFiltersParcel(Collections.emptyList());
    }

    public DeleteUsingFiltersRequestParcel(
            RecordIdFiltersParcel recordIdFiltersParcel, String packageName) {
        mPackageNameFilters = Collections.singletonList(packageName);
        // Not required with ids
        mRecordTypeFilters = new int[0];
        mStartTime = DEFAULT_LONG;
        mEndTime = DEFAULT_LONG;
        mLocalTimeFilter = false;
        mRecordIdFiltersParcel = recordIdFiltersParcel;
    }

    public RecordIdFiltersParcel getRecordIdFiltersParcel() {
        return mRecordIdFiltersParcel;
    }

    public List<String> getPackageNameFilters() {
        return mPackageNameFilters;
    }

    public void setPackageNameFilters(@NonNull List<String> packages) {
        Objects.requireNonNull(packages);
        mPackageNameFilters = packages;
    }

    public List<Integer> getRecordTypeFilters() {
        if (mRecordIdFiltersParcel != null
                && !mRecordIdFiltersParcel.getRecordIdFilters().isEmpty()) {
            return mRecordIdFiltersParcel.getRecordIdFilters().stream()
                    .map(
                            (recordIdFilter) ->
                                    RecordMapper.getInstance()
                                            .getRecordType(recordIdFilter.getRecordType()))
                    .toList()
                    .stream()
                    .distinct()
                    .toList();
        }

        return Arrays.stream(mRecordTypeFilters).boxed().toList();
    }

    public long getStartTime() {
        return mStartTime;
    }

    public long getEndTime() {
        return mEndTime;
    }

    public boolean usesIdFilters() {
        return mRecordIdFiltersParcel.getRecordIdFilters() != null
                && !mRecordIdFiltersParcel.getRecordIdFilters().isEmpty();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStringList(mPackageNameFilters);
        dest.writeIntArray(mRecordTypeFilters);
        dest.writeLong(mStartTime);
        dest.writeLong(mEndTime);
        dest.writeBoolean(mLocalTimeFilter);
        dest.writeParcelable(mRecordIdFiltersParcel, 0);
    }

    public boolean isLocalTimeFilter() {
        return mLocalTimeFilter;
    }

    public boolean usesNonIdFilters() {
        return mRecordTypeFilters.length != 0
                || mStartTime != DEFAULT_LONG
                || mEndTime != DEFAULT_LONG;
    }
}
