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
import android.health.connect.datatypes.HydrationRecord;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.units.Volume;
import android.os.Parcel;

/**
 * @hide
 * @see HydrationRecord
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_HYDRATION)
public final class HydrationRecordInternal extends IntervalRecordInternal<HydrationRecord> {
    private double mVolume;

    public double getVolume() {
        return mVolume;
    }

    /** returns this object with the specified volume */
    @NonNull
    public HydrationRecordInternal setVolume(double volume) {
        this.mVolume = volume;
        return this;
    }

    @NonNull
    @Override
    public HydrationRecord toExternalRecord() {
        return new HydrationRecord.Builder(
                        buildMetaData(),
                        getStartTime(),
                        getEndTime(),
                        Volume.fromLiters(getVolume()))
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .buildWithoutValidation();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        mVolume = parcel.readDouble();
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeDouble(mVolume);
    }
}
