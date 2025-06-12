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
import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.units.Power;
import android.os.Parcel;

/**
 * @see BasalMetabolicRateRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE)
public final class BasalMetabolicRateRecordInternal
        extends InstantRecordInternal<BasalMetabolicRateRecord> {
    private double mBasalMetabolicRate;

    public double getBasalMetabolicRate() {
        return mBasalMetabolicRate;
    }

    /** returns this object with the specified basalMetabolicRate */
    @NonNull
    public BasalMetabolicRateRecordInternal setBasalMetabolicRate(double basalMetabolicRate) {
        this.mBasalMetabolicRate = basalMetabolicRate;
        return this;
    }

    @NonNull
    @Override
    public BasalMetabolicRateRecord toExternalRecord() {
        return new BasalMetabolicRateRecord.Builder(
                        buildMetaData(), getTime(), Power.fromWatts(getBasalMetabolicRate()))
                .setZoneOffset(getZoneOffset())
                .buildWithoutValidation();
    }

    @Override
    void populateInstantRecordFrom(@NonNull Parcel parcel) {
        mBasalMetabolicRate = parcel.readDouble();
    }

    @Override
    void populateInstantRecordTo(@NonNull Parcel parcel) {
        parcel.writeDouble(mBasalMetabolicRate);
    }
}
