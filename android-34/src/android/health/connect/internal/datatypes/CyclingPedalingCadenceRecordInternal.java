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
import android.health.connect.datatypes.CyclingPedalingCadenceRecord;
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
 * @see CyclingPedalingCadenceRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE)
public class CyclingPedalingCadenceRecordInternal
        extends SeriesRecordInternal<
                CyclingPedalingCadenceRecord,
                CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample> {
    private Set<CyclingPedalingCadenceRecordSample> mCyclingPedalingCadenceRecordSamples;

    @Override
    @NonNull
    public Set<CyclingPedalingCadenceRecordSample> getSamples() {
        return mCyclingPedalingCadenceRecordSamples;
    }

    @NonNull
    @Override
    public CyclingPedalingCadenceRecordInternal setSamples(Set<? extends Sample> samples) {
        Objects.requireNonNull(samples);
        this.mCyclingPedalingCadenceRecordSamples =
                (Set<CyclingPedalingCadenceRecordSample>) samples;
        return this;
    }

    @Override
    @NonNull
    public CyclingPedalingCadenceRecord toExternalRecord() {
        return new CyclingPedalingCadenceRecord.Builder(
                        buildMetaData(), getStartTime(), getEndTime(), getExternalSamples())
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .buildWithoutValidation();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        int size = parcel.readInt();
        mCyclingPedalingCadenceRecordSamples = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            mCyclingPedalingCadenceRecordSamples.add(
                    new CyclingPedalingCadenceRecordSample(parcel.readDouble(), parcel.readLong()));
        }
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mCyclingPedalingCadenceRecordSamples.size());
        for (CyclingPedalingCadenceRecordSample cyclingPedalingCadenceRecordSample :
                mCyclingPedalingCadenceRecordSamples) {
            parcel.writeDouble(cyclingPedalingCadenceRecordSample.getRevolutionsPerMinute());
            parcel.writeLong(cyclingPedalingCadenceRecordSample.getEpochMillis());
        }
    }

    private List<CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample>
            getExternalSamples() {
        List<CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample>
                cyclingPedalingCadenceRecords =
                        new ArrayList<>(mCyclingPedalingCadenceRecordSamples.size());
        for (CyclingPedalingCadenceRecordSample cyclingPedalingCadenceRecordSample :
                mCyclingPedalingCadenceRecordSamples) {
            cyclingPedalingCadenceRecords.add(
                    new CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample(
                            cyclingPedalingCadenceRecordSample.getRevolutionsPerMinute(),
                            Instant.ofEpochMilli(
                                    cyclingPedalingCadenceRecordSample.getEpochMillis()),
                            true));
        }
        return cyclingPedalingCadenceRecords;
    }

    /**
     * @see CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample
     */
    public static final class CyclingPedalingCadenceRecordSample implements Sample {
        private final double mRevolutionsPerMinute;
        private final long mEpochMillis;

        public CyclingPedalingCadenceRecordSample(double revolutionsPerMinute, long epochMillis) {
            mRevolutionsPerMinute = revolutionsPerMinute;
            mEpochMillis = epochMillis;
        }

        public double getRevolutionsPerMinute() {
            return mRevolutionsPerMinute;
        }

        public long getEpochMillis() {
            return mEpochMillis;
        }

        @Override
        public boolean equals(@NonNull Object object) {
            if (super.equals(object)
                    && object
                            instanceof
                            CyclingPedalingCadenceRecordInternal
                                    .CyclingPedalingCadenceRecordSample) {
                CyclingPedalingCadenceRecordInternal.CyclingPedalingCadenceRecordSample other =
                        (CyclingPedalingCadenceRecordInternal.CyclingPedalingCadenceRecordSample)
                                object;
                return getEpochMillis() == other.getEpochMillis();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getEpochMillis());
        }
    }
}
