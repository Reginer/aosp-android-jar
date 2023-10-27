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
import android.health.connect.datatypes.BodyWaterMassRecord;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.units.Mass;
import android.os.Parcel;

/** @hide */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BODY_WATER_MASS)
public final class BodyWaterMassRecordInternal extends InstantRecordInternal<BodyWaterMassRecord> {
    private double mBodyWaterMass = 0.0;

    @Override
    void populateInstantRecordFrom(@NonNull Parcel parcel) {
        mBodyWaterMass = parcel.readDouble();
    }

    @Override
    void populateInstantRecordTo(@NonNull Parcel parcel) {
        parcel.writeDouble(mBodyWaterMass);
    }

    public double getBodyWaterMass() {
        return mBodyWaterMass;
    }

    public void setBodyWaterMass(double bodyWaterMass) {
        mBodyWaterMass = bodyWaterMass;
    }

    @NonNull
    @Override
    public BodyWaterMassRecord toExternalRecord() {
        return new BodyWaterMassRecord.Builder(
                        buildMetaData(), getTime(), Mass.fromGrams(getBodyWaterMass()))
                .setZoneOffset(getZoneOffset())
                .buildWithoutValidation();
    }
}
