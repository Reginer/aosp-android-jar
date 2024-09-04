/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.health.connect.Constants.DEFAULT_DOUBLE;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.SkinTemperatureRecord;
import android.health.connect.datatypes.units.Temperature;
import android.health.connect.datatypes.units.TemperatureDelta;
import android.os.Parcel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @see SkinTemperatureRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE)
public final class SkinTemperatureRecordInternal
        extends SeriesRecordInternal<SkinTemperatureRecord, SkinTemperatureRecord.Delta> {

    @NonNull private Temperature mBaseline = Temperature.fromCelsius(DEFAULT_DOUBLE);
    @NonNull private Set<SkinTemperatureDeltaSample> mDeltaSamples = new HashSet<>();

    private int mMeasurementLocation;

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        mMeasurementLocation = parcel.readInt();
        mBaseline = Temperature.fromCelsius(parcel.readDouble());
        int size = parcel.readInt();
        mDeltaSamples = new HashSet<>(size);
        for (int idx = 0; idx < size; idx++) {
            mDeltaSamples.add(
                    new SkinTemperatureRecordInternal.SkinTemperatureDeltaSample(
                            parcel.readDouble(), parcel.readLong()));
        }
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mMeasurementLocation);
        parcel.writeDouble(mBaseline.getInCelsius());
        parcel.writeInt(mDeltaSamples.size());
        for (SkinTemperatureDeltaSample skinTemperatureDeltaSample : mDeltaSamples) {
            parcel.writeDouble(skinTemperatureDeltaSample.mTemperatureDeltaInCelsius());
            parcel.writeLong(skinTemperatureDeltaSample.mEpochMillis());
        }
    }

    @Override
    public SkinTemperatureRecord toExternalRecord() {
        SkinTemperatureRecord.Builder builder =
                new SkinTemperatureRecord.Builder(buildMetaData(), getStartTime(), getEndTime())
                        .setDeltas(getExternalDeltas())
                        .setMeasurementLocation(getMeasurementLocation())
                        .setEndZoneOffset(getEndZoneOffset())
                        .setStartZoneOffset(getStartZoneOffset());
        if (mBaseline.getInCelsius() != DEFAULT_DOUBLE) {
            builder.setBaseline(mBaseline);
        }
        return builder.buildWithoutValidation();
    }

    @NonNull
    @Override
    public Set<? extends Sample> getSamples() {
        return mDeltaSamples;
    }

    /**
     * @return this object with specified skin temperature delta samples.
     */
    @NonNull
    @Override
    public SeriesRecordInternal setSamples(Set<? extends Sample> samples) {
        Objects.requireNonNull(samples);
        mDeltaSamples = (Set<SkinTemperatureDeltaSample>) samples;
        return this;
    }

    @Nullable
    public Temperature getBaseline() {
        return mBaseline;
    }

    /**
     * @return this object with a specified baseline skin temperature.
     */
    @NonNull
    public SkinTemperatureRecordInternal setBaseline(Temperature baseline) {
        Objects.requireNonNull((baseline));
        mBaseline = baseline;
        return this;
    }

    @SkinTemperatureRecord.SkinTemperatureMeasurementLocation
    public int getMeasurementLocation() {
        return mMeasurementLocation;
    }

    /**
     * @return this object with the measurement location.
     */
    @NonNull
    public SkinTemperatureRecordInternal setMeasurementLocation(int measurementLocation) {
        mMeasurementLocation = measurementLocation;
        return this;
    }

    public record SkinTemperatureDeltaSample(double mTemperatureDeltaInCelsius, long mEpochMillis)
            implements Sample {}

    private List<SkinTemperatureRecord.Delta> getExternalDeltas() {
        List<SkinTemperatureRecord.Delta> skinTemperatureDeltas =
                new ArrayList<>(mDeltaSamples.size());
        for (SkinTemperatureRecordInternal.SkinTemperatureDeltaSample skinTemperatureDeltaSample :
                mDeltaSamples) {
            skinTemperatureDeltas.add(
                    new SkinTemperatureRecord.Delta(
                            TemperatureDelta.fromCelsius(
                                    skinTemperatureDeltaSample.mTemperatureDeltaInCelsius()),
                            Instant.ofEpochMilli(skinTemperatureDeltaSample.mEpochMillis()),
                            true));
        }
        return skinTemperatureDeltas;
    }
}
