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
import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.BloodPressureRecord.BloodPressureMeasurementLocation;
import android.health.connect.datatypes.BloodPressureRecord.BodyPosition.BodyPositionType;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.units.Pressure;
import android.os.Parcel;

/**
 * @hide
 * @see BloodPressureRecord
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE)
public final class BloodPressureRecordInternal extends InstantRecordInternal<BloodPressureRecord> {
    private int mMeasurementLocation;
    private double mSystolic;
    private double mDiastolic;
    private int mBodyPosition;

    @BloodPressureMeasurementLocation.BloodPressureMeasurementLocations
    public int getMeasurementLocation() {
        return mMeasurementLocation;
    }

    /** returns this object with the specified measurementLocation */
    @NonNull
    public BloodPressureRecordInternal setMeasurementLocation(int measurementLocation) {
        this.mMeasurementLocation = measurementLocation;
        return this;
    }

    public double getSystolic() {
        return mSystolic;
    }

    /** returns this object with the specified systolic */
    @NonNull
    public BloodPressureRecordInternal setSystolic(double systolic) {
        this.mSystolic = systolic;
        return this;
    }

    public double getDiastolic() {
        return mDiastolic;
    }

    /** returns this object with the specified diastolic */
    @NonNull
    public BloodPressureRecordInternal setDiastolic(double diastolic) {
        this.mDiastolic = diastolic;
        return this;
    }

    @BodyPositionType
    public int getBodyPosition() {
        return mBodyPosition;
    }

    /** returns this object with the specified bodyPosition */
    @NonNull
    public BloodPressureRecordInternal setBodyPosition(int bodyPosition) {
        this.mBodyPosition = bodyPosition;
        return this;
    }

    @NonNull
    @Override
    public BloodPressureRecord toExternalRecord() {
        return new BloodPressureRecord.Builder(
                        buildMetaData(),
                        getTime(),
                        getMeasurementLocation(),
                        Pressure.fromMillimetersOfMercury(getSystolic()),
                        Pressure.fromMillimetersOfMercury(getDiastolic()),
                        getBodyPosition())
                .setZoneOffset(getZoneOffset())
                .buildWithoutValidation();
    }

    @Override
    void populateInstantRecordFrom(@NonNull Parcel parcel) {
        mMeasurementLocation = parcel.readInt();
        mSystolic = parcel.readDouble();
        mDiastolic = parcel.readDouble();
        mBodyPosition = parcel.readInt();
    }

    @Override
    void populateInstantRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mMeasurementLocation);
        parcel.writeDouble(mSystolic);
        parcel.writeDouble(mDiastolic);
        parcel.writeInt(mBodyPosition);
    }
}
