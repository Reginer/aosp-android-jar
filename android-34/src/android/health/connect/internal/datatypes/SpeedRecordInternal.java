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
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.SpeedRecord;
import android.health.connect.datatypes.units.Velocity;
import android.os.Parcel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @see SpeedRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_SPEED)
public class SpeedRecordInternal
        extends SeriesRecordInternal<SpeedRecord, SpeedRecord.SpeedRecordSample> {
    private Set<SpeedRecordSample> mSpeedRecordSamples;

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        int size = parcel.readInt();
        mSpeedRecordSamples = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            mSpeedRecordSamples.add(new SpeedRecordSample(parcel.readDouble(), parcel.readLong()));
        }
    }

    @Override
    @NonNull
    public Set<SpeedRecordSample> getSamples() {
        return mSpeedRecordSamples;
    }

    @NonNull
    @Override
    public SpeedRecordInternal setSamples(Set<? extends Sample> samples) {
        Objects.requireNonNull(samples);
        this.mSpeedRecordSamples = (Set<SpeedRecordSample>) samples;
        return this;
    }

    @Override
    @NonNull
    public SpeedRecord toExternalRecord() {
        return new SpeedRecord.Builder(
                        buildMetaData(), getStartTime(), getEndTime(), getExternalSamples())
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .buildWithoutValidation();
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mSpeedRecordSamples.size());
        for (SpeedRecordSample speedRecordSample : mSpeedRecordSamples) {
            parcel.writeDouble(speedRecordSample.getSpeed());
            parcel.writeLong(speedRecordSample.getEpochMillis());
        }
    }

    private List<SpeedRecord.SpeedRecordSample> getExternalSamples() {
        List<SpeedRecord.SpeedRecordSample> speedRecords =
                new ArrayList<>(mSpeedRecordSamples.size());
        for (SpeedRecordSample speedRecordSample : mSpeedRecordSamples) {
            speedRecords.add(
                    new SpeedRecord.SpeedRecordSample(
                            Velocity.fromMetersPerSecond(speedRecordSample.getSpeed()),
                            Instant.ofEpochMilli(speedRecordSample.getEpochMillis()),
                            true));
        }
        return speedRecords;
    }

    /**
     * @see SpeedRecord.SpeedRecordSample
     */
    public static final class SpeedRecordSample implements Sample {
        private final double mSpeed;
        private final long mEpochMillis;

        public SpeedRecordSample(double speed, long epochMillis) {
            mSpeed = speed;
            mEpochMillis = epochMillis;
        }

        public double getSpeed() {
            return mSpeed;
        }

        public long getEpochMillis() {
            return mEpochMillis;
        }

        @Override
        public boolean equals(@NonNull Object object) {
            if (super.equals(object) && object instanceof SpeedRecordInternal.SpeedRecordSample) {
                SpeedRecordInternal.SpeedRecordSample other =
                        (SpeedRecordInternal.SpeedRecordSample) object;
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
