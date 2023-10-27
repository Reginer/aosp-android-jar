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
import android.annotation.Nullable;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.LocalTimeRangeFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.TimeRangeFilter;
import android.health.connect.TimeRangeFilterHelper;
import android.health.connect.datatypes.AggregationType;
import android.health.connect.datatypes.DataOrigin;
import android.os.Parcel;
import android.os.Parcelable;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

/** @hide */
public class AggregateDataRequestParcel implements Parcelable {
    public static final Creator<AggregateDataRequestParcel> CREATOR =
            new Creator<>() {
                @Override
                public AggregateDataRequestParcel createFromParcel(Parcel in) {
                    return new AggregateDataRequestParcel(in);
                }

                @Override
                public AggregateDataRequestParcel[] newArray(int size) {
                    return new AggregateDataRequestParcel[size];
                }
            };
    private final long mStartTime;
    private final long mEndTime;
    private final int[] mAggregateIds;
    private final List<String> mPackageFilters;
    // If set represents that the aggregations have to be grouped by on {@code mDuration}
    private Duration mDuration;
    // If set represents that the aggregations have to be grouped by on {@code mPeriod}. If both are
    // set the duration takes precedence, but there should not be case when both are set.
    private Period mPeriod;

    private boolean mLocalTimeFilter;

    public AggregateDataRequestParcel(AggregateRecordsRequest<?> request) {
        mStartTime = TimeRangeFilterHelper.getFilterStartTimeMillis(request.getTimeRangeFilter());
        mEndTime = TimeRangeFilterHelper.getFilterEndTimeMillis(request.getTimeRangeFilter());
        mAggregateIds = new int[request.getAggregationTypes().size()];
        mLocalTimeFilter = TimeRangeFilterHelper.isLocalTimeFilter(request.getTimeRangeFilter());

        int i = 0;
        for (AggregationType<?> aggregationType : request.getAggregationTypes()) {
            mAggregateIds[i++] = aggregationType.getAggregationTypeIdentifier();
        }
        mPackageFilters =
                request.getDataOriginsFilters().stream()
                        .map(DataOrigin::getPackageName)
                        .collect(Collectors.toList());
        // Use durations group by single queries as it support null values
        mDuration = Duration.ofMillis(mEndTime - mStartTime);
    }

    public AggregateDataRequestParcel(AggregateRecordsRequest request, Duration duration) {
        this(request);
        mDuration = duration;
    }

    public AggregateDataRequestParcel(AggregateRecordsRequest request, Period period) {
        this(request);
        mDuration = null;
        mPeriod = period;

        if (!TimeRangeFilterHelper.isLocalTimeFilter(request.getTimeRangeFilter())) {
            throw new UnsupportedOperationException(
                    "For period requests should use LocalTimeRangeFilter");
        }
    }

    protected AggregateDataRequestParcel(Parcel in) {
        mStartTime = in.readLong();
        mEndTime = in.readLong();
        mLocalTimeFilter = in.readBoolean();
        mAggregateIds = in.createIntArray();
        mPackageFilters = in.createStringArrayList();
        int periodDays = in.readInt();
        if (periodDays != DEFAULT_INT) {
            int periodMonths = in.readInt();
            int periodYears = in.readInt();
            mPeriod = Period.of(periodYears, periodMonths, periodDays);
        }

        long duration = in.readLong();
        if (duration != DEFAULT_LONG) {
            mDuration = Duration.ofMillis(duration);
        }
    }

    @Nullable
    public Duration getDuration() {
        return mDuration;
    }

    @Nullable
    public Period getPeriod() {
        return mPeriod;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(mStartTime);
        dest.writeLong(mEndTime);
        dest.writeBoolean(mLocalTimeFilter);
        dest.writeIntArray(mAggregateIds);
        dest.writeStringList(mPackageFilters);
        if (mPeriod != null) {
            dest.writeInt(mPeriod.getDays());
            dest.writeInt(mPeriod.getMonths());
            dest.writeInt(mPeriod.getYears());
        } else {
            dest.writeInt(DEFAULT_INT);
        }

        if (mDuration != null) {
            dest.writeLong(mDuration.toMillis());
        } else {
            dest.writeLong(DEFAULT_LONG);
        }
    }

    public long getStartTime() {
        return mStartTime;
    }

    public long getEndTime() {
        return mEndTime;
    }

    public int[] getAggregateIds() {
        return mAggregateIds;
    }

    @NonNull
    public List<String> getPackageFilters() {
        return mPackageFilters;
    }

    @NonNull
    public TimeRangeFilter getTimeRangeFilter() {
        if (mLocalTimeFilter) {
            return new LocalTimeRangeFilter.Builder()
                    .setStartTime(TimeRangeFilterHelper.getLocalTimeFromMillis(mStartTime))
                    .setEndTime(TimeRangeFilterHelper.getLocalTimeFromMillis(mEndTime))
                    .build();
        } else {
            return new TimeInstantRangeFilter.Builder()
                    .setStartTime(Instant.ofEpochMilli(mStartTime))
                    .setEndTime(Instant.ofEpochMilli(mEndTime))
                    .build();
        }
    }
}
