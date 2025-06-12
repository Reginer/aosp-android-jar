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
import static android.health.connect.TimeRangeFilterHelper.getInstantFromLocalTime;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.AggregateRecordsGroupedByDurationResponse;
import android.health.connect.AggregateRecordsGroupedByPeriodResponse;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.AggregateResult;
import android.health.connect.LocalTimeRangeFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.TimeRangeFilter;
import android.health.connect.TimeRangeFilterHelper;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.internal.datatypes.utils.AggregationTypeIdMapper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** @hide */
public class AggregateDataResponseParcel implements Parcelable {
    public static final Creator<AggregateDataResponseParcel> CREATOR =
            new Creator<>() {
                @Override
                public AggregateDataResponseParcel createFromParcel(Parcel in) {
                    return new AggregateDataResponseParcel(in);
                }

                @Override
                public AggregateDataResponseParcel[] newArray(int size) {
                    return new AggregateDataResponseParcel[size];
                }
            };
    private final List<AggregateRecordsResponse<?>> mAggregateRecordsResponses;
    private Duration mDuration;
    private Period mPeriod;
    private TimeRangeFilter mTimeRangeFilter;

    @SuppressWarnings("NullAway.Init") // TODO(b/317029272): fix this suppression
    public AggregateDataResponseParcel(List<AggregateRecordsResponse<?>> aggregateRecordsResponse) {
        mAggregateRecordsResponses = aggregateRecordsResponse;
    }

    @SuppressWarnings("NullAway.Init") // TODO(b/317029272): fix this suppression
    protected AggregateDataResponseParcel(Parcel in) {
        final int size = in.readInt();
        mAggregateRecordsResponses = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            final int mapSize = in.readInt();
            Map<Integer, AggregateResult<?>> result = new ArrayMap<>(mapSize);

            for (int mapI = 0; mapI < mapSize; mapI++) {
                int id = in.readInt();
                boolean hasValue = in.readBoolean();
                if (hasValue) {
                    result.put(
                            id,
                            AggregationTypeIdMapper.getInstance()
                                    .getAggregateResultFor(id, in)
                                    .setZoneOffset(parseZoneOffset(in))
                                    .setDataOrigins(in.createStringArrayList()));
                } else {
                    result.put(id, null);
                }
            }

            mAggregateRecordsResponses.add(new AggregateRecordsResponse<>(result));
        }

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

        boolean isLocaltimeFilter = in.readBoolean();
        long startTime = in.readLong();
        long endTime = in.readLong();
        if (startTime != DEFAULT_LONG && endTime != DEFAULT_LONG) {
            if (isLocaltimeFilter) {
                mTimeRangeFilter =
                        new LocalTimeRangeFilter.Builder()
                                .setStartTime(
                                        TimeRangeFilterHelper.getLocalTimeFromMillis(startTime))
                                .setEndTime(TimeRangeFilterHelper.getLocalTimeFromMillis(endTime))
                                .build();
            } else {
                mTimeRangeFilter =
                        new TimeInstantRangeFilter.Builder()
                                .setStartTime(Instant.ofEpochMilli(startTime))
                                .setEndTime(Instant.ofEpochMilli(endTime))
                                .build();
            }
        }
    }

    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    public AggregateDataResponseParcel setDuration(
            @Nullable Duration duration, @Nullable TimeRangeFilter timeRangeFilter) {
        mDuration = duration;
        mTimeRangeFilter = timeRangeFilter;

        return this;
    }

    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    public AggregateDataResponseParcel setPeriod(
            @Nullable Period period, @Nullable TimeRangeFilter timeRangeFilter) {
        mPeriod = period;
        mTimeRangeFilter = timeRangeFilter;

        return this;
    }

    /**
     * @return the first response from {@code mAggregateRecordsResponses}
     */
    public AggregateRecordsResponse<?> getAggregateDataResponse() {
        return mAggregateRecordsResponses.get(0);
    }

    /**
     * @return responses from {@code mAggregateRecordsResponses} grouped as per the {@code
     *     mDuration}
     */
    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    public List<AggregateRecordsGroupedByDurationResponse<?>>
            getAggregateDataResponseGroupedByDuration() {
        Objects.requireNonNull(mDuration);

        if (mAggregateRecordsResponses.isEmpty()) {
            return List.of();
        }

        if (mTimeRangeFilter instanceof LocalTimeRangeFilter timeFilter) {
            return getAggregateDataResponseForLocalTimeGroupedByDuration(
                    timeFilter.getStartTime(), timeFilter.getEndTime());
        }

        if (mTimeRangeFilter instanceof TimeInstantRangeFilter timeFilter) {
            return getAggregateDataResponseForInstantTimeGroupedByDuration(
                    timeFilter.getStartTime(), timeFilter.getEndTime());
        }

        throw new IllegalArgumentException(
                "Invalid time filter object. Object should be either TimeInstantRangeFilter or "
                        + "LocalTimeRangeFilter.");
    }

    private List<AggregateRecordsGroupedByDurationResponse<?>>
            getAggregateDataResponseForLocalTimeGroupedByDuration(
                    LocalDateTime startTime, LocalDateTime endTime) {
        List<AggregateRecordsGroupedByDurationResponse<?>> responses = new ArrayList<>();
        Duration bucketStartTimeOffset = Duration.ZERO;
        for (AggregateRecordsResponse<?> response : mAggregateRecordsResponses) {
            ZoneOffset zoneOffset = response.getFirstZoneOffset();
            Instant endTimeInstant = getInstantFromLocalTime(endTime, zoneOffset);
            Instant bucketStartTime =
                    getInstantFromLocalTime(startTime, zoneOffset).plus(bucketStartTimeOffset);
            Instant bucketEndTime = bucketStartTime.plus(mDuration);
            if (bucketEndTime.isAfter(endTimeInstant)) {
                bucketEndTime = endTimeInstant;
            }

            responses.add(
                    new AggregateRecordsGroupedByDurationResponse<>(
                            bucketStartTime, bucketEndTime, response.getAggregateResults()));
            bucketStartTimeOffset = bucketStartTimeOffset.plus(mDuration);
        }

        return responses;
    }

    private List<AggregateRecordsGroupedByDurationResponse<?>>
            getAggregateDataResponseForInstantTimeGroupedByDuration(
                    Instant startTime, Instant endTime) {
        List<AggregateRecordsGroupedByDurationResponse<?>> responses = new ArrayList<>();
        Duration offsetDuration = Duration.ZERO;
        for (AggregateRecordsResponse<?> response : mAggregateRecordsResponses) {
            Instant buckedStartTime = startTime.plus(offsetDuration);
            Instant buckedEndTime = buckedStartTime.plus(mDuration);
            if (buckedEndTime.isAfter(endTime)) {
                buckedEndTime = endTime;
            }

            responses.add(
                    new AggregateRecordsGroupedByDurationResponse<>(
                            buckedStartTime, buckedEndTime, response.getAggregateResults()));

            offsetDuration = offsetDuration.plus(mDuration);
        }

        return responses;
    }

    /**
     * @return responses from {@code mAggregateRecordsResponses} grouped as per the {@code mPeriod}
     */
    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    public List<AggregateRecordsGroupedByPeriodResponse<?>>
            getAggregateDataResponseGroupedByPeriod() {
        Objects.requireNonNull(mPeriod);

        List<AggregateRecordsGroupedByPeriodResponse<?>> aggregateRecordsGroupedByPeriodResponses =
                new ArrayList<>();

        LocalDateTime groupBoundary = ((LocalTimeRangeFilter) mTimeRangeFilter).getStartTime();
        for (AggregateRecordsResponse<?> aggregateRecordsResponse : mAggregateRecordsResponses) {
            aggregateRecordsGroupedByPeriodResponses.add(
                    new AggregateRecordsGroupedByPeriodResponse<>(
                            groupBoundary,
                            groupBoundary.plus(mPeriod),
                            aggregateRecordsResponse.getAggregateResults()));
            groupBoundary = groupBoundary.plus(mPeriod);
        }

        if (!aggregateRecordsGroupedByPeriodResponses.isEmpty()) {
            aggregateRecordsGroupedByPeriodResponses
                    .get(aggregateRecordsGroupedByPeriodResponses.size() - 1)
                    .setEndTime(getPeriodEndLocalDateTime(mTimeRangeFilter));
        }

        return aggregateRecordsGroupedByPeriodResponses;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mAggregateRecordsResponses.size());
        for (AggregateRecordsResponse<?> aggregateRecordsResponse : mAggregateRecordsResponses) {
            dest.writeInt(aggregateRecordsResponse.getAggregateResults().size());
            aggregateRecordsResponse
                    .getAggregateResults()
                    .forEach(
                            (key, val) -> {
                                dest.writeInt(key);
                                // to represent if the value is present or not
                                dest.writeBoolean(val != null);
                                if (val != null) {
                                    val.putToParcel(dest);
                                    ZoneOffset zoneOffset = val.getZoneOffset();
                                    if (zoneOffset != null) {
                                        dest.writeInt(val.getZoneOffset().getTotalSeconds());
                                    } else {
                                        dest.writeInt(DEFAULT_INT);
                                    }
                                    Set<DataOrigin> dataOrigins = val.getDataOrigins();
                                    List<String> packageNames = new ArrayList<>();
                                    for (DataOrigin dataOrigin : dataOrigins) {
                                        packageNames.add(dataOrigin.getPackageName());
                                    }
                                    dest.writeStringList(packageNames);
                                }
                            });
        }

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

        if (mTimeRangeFilter != null) {
            dest.writeBoolean(TimeRangeFilterHelper.isLocalTimeFilter(mTimeRangeFilter));
            dest.writeLong(TimeRangeFilterHelper.getFilterStartTimeMillis(mTimeRangeFilter));
            dest.writeLong(TimeRangeFilterHelper.getFilterEndTimeMillis(mTimeRangeFilter));
        } else {
            dest.writeBoolean(false);
            dest.writeLong(DEFAULT_LONG);
            dest.writeLong(DEFAULT_LONG);
        }
    }

    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    private ZoneOffset parseZoneOffset(Parcel in) {
        int zoneOffsetInSecs = in.readInt();
        ZoneOffset zoneOffset = null;
        if (zoneOffsetInSecs != DEFAULT_INT) {
            zoneOffset = ZoneOffset.ofTotalSeconds(zoneOffsetInSecs);
        }

        return zoneOffset;
    }

    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    private LocalDateTime getPeriodEndLocalDateTime(TimeRangeFilter timeRangeFilter) {
        if (timeRangeFilter instanceof TimeInstantRangeFilter) {
            return LocalDateTime.ofInstant(
                    ((TimeInstantRangeFilter) timeRangeFilter).getEndTime(),
                    ZoneOffset.systemDefault());
        } else if (timeRangeFilter instanceof LocalTimeRangeFilter) {
            return ((LocalTimeRangeFilter) timeRangeFilter).getEndTime();
        } else {
            throw new IllegalArgumentException(
                    "Invalid time filter object. Object should be either "
                            + "TimeInstantRangeFilter or LocalTimeRangeFilter.");
        }
    }

    private long getPeriodDeltaInDays(Period period) {
        return period.getDays();
    }

    private Instant getDurationInstant(long duration) {
        return Instant.ofEpochMilli(duration);
    }

    private long getDurationDelta(Duration duration) {
        return duration.toMillis();
    }
}
