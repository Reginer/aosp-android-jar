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

import static android.health.connect.Constants.DEFAULT_INT;
import static android.health.connect.Constants.DEFAULT_LONG;

import android.annotation.NonNull;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.TimeRangeFilterHelper;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.utils.RecordMapper;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A Parcel to carry read request to {@see HealthConnectManager#readRecords}
 *
 * @hide
 */
public class ReadRecordsRequestParcel implements Parcelable {
    public static final Creator<ReadRecordsRequestParcel> CREATOR =
            new Creator<>() {
                @Override
                public ReadRecordsRequestParcel createFromParcel(Parcel in) {
                    return new ReadRecordsRequestParcel(in);
                }

                @Override
                public ReadRecordsRequestParcel[] newArray(int size) {
                    return new ReadRecordsRequestParcel[size];
                }
            };

    private final RecordIdFiltersParcel mRecordIdFiltersParcel;
    @RecordTypeIdentifier.RecordType private final int mRecordType;
    private final List<String> mPackageFilters;
    private final long mStartTime;
    private final long mEndTime;
    private final int mPageSize;
    private final long mPageToken;
    private final boolean mAscending;
    private final boolean mLocalTimeFilter;

    protected ReadRecordsRequestParcel(Parcel in) {
        mRecordType = in.readInt();
        mStartTime = in.readLong();
        mEndTime = in.readLong();
        mLocalTimeFilter = in.readBoolean();
        mPackageFilters = in.createStringArrayList();
        mRecordIdFiltersParcel =
                in.readParcelable(
                        RecordIdFiltersParcel.class.getClassLoader(), RecordIdFiltersParcel.class);
        mPageSize = in.readInt();
        mPageToken = in.readLong();
        mAscending = in.readBoolean();
    }

    public ReadRecordsRequestParcel(ReadRecordsRequestUsingIds<?> request) {
        mPackageFilters = Collections.emptyList();
        mRecordIdFiltersParcel = new RecordIdFiltersParcel(request.getRecordIdFilters());
        mStartTime = DEFAULT_LONG;
        mEndTime = DEFAULT_LONG;
        mRecordType = RecordMapper.getInstance().getRecordType(request.getRecordType());
        mPageSize = DEFAULT_INT;
        mLocalTimeFilter = false;

        // set to -1 as pageToken is not supported for read using ids but only with filters.
        mPageToken = DEFAULT_LONG;
        mAscending = true;
    }

    public ReadRecordsRequestParcel(ReadRecordsRequestUsingFilters<?> request) {
        mPackageFilters =
                request.getDataOrigins().stream()
                        .map(DataOrigin::getPackageName)
                        .collect(Collectors.toList());
        mRecordIdFiltersParcel = null;
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
        mRecordType = RecordMapper.getInstance().getRecordType(request.getRecordType());
        mPageSize = request.getPageSize();
        mPageToken = request.getPageToken();
        mAscending = request.isAscending();
    }

    public int getRecordType() {
        return mRecordType;
    }

    public List<String> getPackageFilters() {
        return mPackageFilters;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public long getEndTime() {
        return mEndTime;
    }

    public RecordIdFiltersParcel getRecordIdFiltersParcel() {
        return mRecordIdFiltersParcel;
    }

    public int getPageSize() {
        return mPageSize;
    }

    public long getPageToken() {
        return mPageToken;
    }

    public boolean isAscending() {
        return mAscending;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean usesLocalTimeFilter() {
        return mLocalTimeFilter;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mRecordType);
        dest.writeLong(mStartTime);
        dest.writeLong(mEndTime);
        dest.writeBoolean(mLocalTimeFilter);
        dest.writeStringList(mPackageFilters);
        dest.writeParcelable(mRecordIdFiltersParcel, 0);
        dest.writeInt(mPageSize);
        dest.writeLong(mPageToken);
        dest.writeBoolean(mAscending);
    }
}
