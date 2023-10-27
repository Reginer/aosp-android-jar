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
import android.health.connect.datatypes.HeightRecord;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.units.Length;
import android.os.Parcel;

/**
 * @see HeightRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_HEIGHT)
public final class HeightRecordInternal extends InstantRecordInternal<HeightRecord> {
    private double mHeight;

    public double getHeight() {
        return mHeight;
    }

    /** returns this object with the specified height */
    @NonNull
    public HeightRecordInternal setHeight(double height) {
        this.mHeight = height;
        return this;
    }

    @NonNull
    @Override
    public HeightRecord toExternalRecord() {
        return new HeightRecord.Builder(buildMetaData(), getTime(), Length.fromMeters(getHeight()))
                .setZoneOffset(getZoneOffset())
                .buildWithoutValidation();
    }

    @Override
    void populateInstantRecordFrom(@NonNull Parcel parcel) {
        mHeight = parcel.readDouble();
    }

    @Override
    void populateInstantRecordTo(@NonNull Parcel parcel) {
        parcel.writeDouble(mHeight);
    }
}
