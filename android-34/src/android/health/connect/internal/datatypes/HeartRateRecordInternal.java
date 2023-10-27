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

package android.health.connect.internal.datatypes;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.os.Parcel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @see HeartRateRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_HEART_RATE)
public class HeartRateRecordInternal
        extends SeriesRecordInternal<HeartRateRecord, HeartRateRecord.HeartRateSample> {
    public static final class HeartRateSample implements Sample {
        private final int mBeatsPerMinute;
        private final long mEpochMillis;

        public HeartRateSample(int beatsPerMinute, long epochMillis) {
            mBeatsPerMinute = beatsPerMinute;
            mEpochMillis = epochMillis;
        }

        public int getBeatsPerMinute() {
            return mBeatsPerMinute;
        }

        public long getEpochMillis() {
            return mEpochMillis;
        }

        @Override
        public boolean equals(@NonNull Object object) {
            if (super.equals(object) && object instanceof HeartRateRecordInternal.HeartRateSample) {
                HeartRateRecordInternal.HeartRateSample other =
                        (HeartRateRecordInternal.HeartRateSample) object;
                return getEpochMillis() == other.getEpochMillis();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getEpochMillis());
        }
    }

    private Set<HeartRateSample> mHeartRateHeartRateSamples;

    @Override
    @Nullable
    public Set<HeartRateSample> getSamples() {
        return mHeartRateHeartRateSamples;
    }

    @Override
    public HeartRateRecordInternal setSamples(Set<? extends Sample> samples) {
        this.mHeartRateHeartRateSamples = (Set<HeartRateSample>) samples;
        return this;
    }

    @Override
    @NonNull
    public HeartRateRecord toExternalRecord() {
        return new HeartRateRecord.Builder(
                        buildMetaData(), getStartTime(), getEndTime(), getExternalSamples())
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .buildWithoutValidation();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        int size = parcel.readInt();
        mHeartRateHeartRateSamples = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            mHeartRateHeartRateSamples.add(
                    new HeartRateSample(parcel.readInt(), parcel.readLong()));
        }
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mHeartRateHeartRateSamples.size());
        for (HeartRateSample heartRateSample : mHeartRateHeartRateSamples) {
            parcel.writeInt(heartRateSample.getBeatsPerMinute());
            parcel.writeLong(heartRateSample.getEpochMillis());
        }
    }

    private List<HeartRateRecord.HeartRateSample> getExternalSamples() {
        List<HeartRateRecord.HeartRateSample> heartRateRecords =
                new ArrayList<>(mHeartRateHeartRateSamples.size());

        for (HeartRateSample heartRateSample : mHeartRateHeartRateSamples) {
            heartRateRecords.add(
                    new HeartRateRecord.HeartRateSample(
                            heartRateSample.getBeatsPerMinute(),
                            Instant.ofEpochMilli(heartRateSample.getEpochMillis()),
                            true));
        }

        return heartRateRecords;
    }
}
