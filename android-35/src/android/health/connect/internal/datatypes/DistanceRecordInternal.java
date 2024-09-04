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
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.units.Length;
import android.os.Parcel;

/**
 * @see DistanceRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_DISTANCE)
public final class DistanceRecordInternal extends IntervalRecordInternal<DistanceRecord> {
    private double mDistance;

    public double getDistance() {
        return mDistance;
    }

    /** returns this object with the specified distance */
    @NonNull
    public DistanceRecordInternal setDistance(double distance) {
        this.mDistance = distance;
        return this;
    }

    @NonNull
    @Override
    public DistanceRecord toExternalRecord() {
        return new DistanceRecord.Builder(
                        buildMetaData(),
                        getStartTime(),
                        getEndTime(),
                        Length.fromMeters(getDistance()))
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .buildWithoutValidation();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        mDistance = parcel.readDouble();
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeDouble(mDistance);
    }
}
