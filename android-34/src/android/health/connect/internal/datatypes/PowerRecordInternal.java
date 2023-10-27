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
import android.health.connect.datatypes.PowerRecord;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.units.Power;
import android.os.Parcel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @see PowerRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_POWER)
public class PowerRecordInternal
        extends SeriesRecordInternal<PowerRecord, PowerRecord.PowerRecordSample> {
    private Set<PowerRecordSample> mPowerRecordSamples;

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        int size = parcel.readInt();
        mPowerRecordSamples = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            mPowerRecordSamples.add(new PowerRecordSample(parcel.readDouble(), parcel.readLong()));
        }
    }

    @Override
    @NonNull
    public Set<PowerRecordSample> getSamples() {
        return mPowerRecordSamples;
    }

    @NonNull
    @Override
    public PowerRecordInternal setSamples(Set<? extends Sample> samples) {
        Objects.requireNonNull(samples);
        this.mPowerRecordSamples = (Set<PowerRecordSample>) samples;
        return this;
    }

    @Override
    @NonNull
    public PowerRecord toExternalRecord() {
        return new PowerRecord.Builder(
                        buildMetaData(), getStartTime(), getEndTime(), getExternalSamples())
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .buildWithoutValidation();
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mPowerRecordSamples.size());
        for (PowerRecordSample powerRecordSample : mPowerRecordSamples) {
            parcel.writeDouble(powerRecordSample.getPower());
            parcel.writeLong(powerRecordSample.getEpochMillis());
        }
    }

    private List<PowerRecord.PowerRecordSample> getExternalSamples() {
        List<PowerRecord.PowerRecordSample> powerRecords =
                new ArrayList<>(mPowerRecordSamples.size());
        for (PowerRecordSample powerRecordSample : mPowerRecordSamples) {
            powerRecords.add(
                    new PowerRecord.PowerRecordSample(
                            Power.fromWatts(powerRecordSample.getPower()),
                            Instant.ofEpochMilli(powerRecordSample.getEpochMillis()),
                            true));
        }
        return powerRecords;
    }

    /**
     * @see PowerRecord.PowerRecordSample
     */
    public static final class PowerRecordSample implements Sample {
        private final double mPower;
        private final long mEpochMillis;

        public PowerRecordSample(double power, long epochMillis) {
            mPower = power;
            mEpochMillis = epochMillis;
        }

        public double getPower() {
            return mPower;
        }

        public long getEpochMillis() {
            return mEpochMillis;
        }

        @Override
        public boolean equals(@NonNull Object object) {
            if (super.equals(object) && object instanceof PowerRecordInternal.PowerRecordSample) {
                PowerRecordInternal.PowerRecordSample other =
                        (PowerRecordInternal.PowerRecordSample) object;
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
