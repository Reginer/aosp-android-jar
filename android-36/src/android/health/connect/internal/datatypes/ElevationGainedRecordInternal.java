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
import android.health.connect.datatypes.ElevationGainedRecord;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.units.Length;
import android.os.Parcel;

/**
 * @see ElevationGainedRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED)
public final class ElevationGainedRecordInternal
        extends IntervalRecordInternal<ElevationGainedRecord> {
    private double mElevation;

    public double getElevation() {
        return mElevation;
    }

    /** returns this object with the specified elevation */
    @NonNull
    public ElevationGainedRecordInternal setElevation(double elevation) {
        this.mElevation = elevation;
        return this;
    }

    @NonNull
    @Override
    public ElevationGainedRecord toExternalRecord() {
        return new ElevationGainedRecord.Builder(
                        buildMetaData(),
                        getStartTime(),
                        getEndTime(),
                        Length.fromMeters(getElevation()))
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .buildWithoutValidation();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        mElevation = parcel.readDouble();
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeDouble(mElevation);
    }
}
