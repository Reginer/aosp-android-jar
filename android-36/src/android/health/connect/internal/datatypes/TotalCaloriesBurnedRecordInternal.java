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
import android.health.connect.datatypes.TotalCaloriesBurnedRecord;
import android.health.connect.datatypes.units.Energy;
import android.os.Parcel;

/**
 * @see TotalCaloriesBurnedRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED)
public final class TotalCaloriesBurnedRecordInternal
        extends IntervalRecordInternal<TotalCaloriesBurnedRecord> {
    private double mEnergy;

    public double getEnergy() {
        return mEnergy;
    }

    /** returns this object with the specified energy */
    @NonNull
    public TotalCaloriesBurnedRecordInternal setEnergy(double energy) {
        this.mEnergy = energy;
        return this;
    }

    @NonNull
    @Override
    public TotalCaloriesBurnedRecord toExternalRecord() {
        return new TotalCaloriesBurnedRecord.Builder(
                        buildMetaData(),
                        getStartTime(),
                        getEndTime(),
                        Energy.fromCalories(getEnergy()))
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .buildWithoutValidation();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        mEnergy = parcel.readDouble();
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeDouble(mEnergy);
    }
}
