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
import android.health.connect.datatypes.OvulationTestRecord;
import android.health.connect.datatypes.OvulationTestRecord.OvulationTestResult;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.os.Parcel;

/**
 * @see OvulationTestRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_OVULATION_TEST)
public final class OvulationTestRecordInternal extends InstantRecordInternal<OvulationTestRecord> {
    private int mResult;

    @OvulationTestResult.OvulationTestResults
    public int getResult() {
        return mResult;
    }

    /** returns this object with the specified result */
    @NonNull
    public OvulationTestRecordInternal setResult(int result) {
        this.mResult = result;
        return this;
    }

    @NonNull
    @Override
    public OvulationTestRecord toExternalRecord() {
        return new OvulationTestRecord.Builder(buildMetaData(), getTime(), getResult())
                .setZoneOffset(getZoneOffset())
                .buildWithoutValidation();
    }

    @Override
    void populateInstantRecordFrom(@NonNull Parcel parcel) {
        mResult = parcel.readInt();
    }

    @Override
    void populateInstantRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mResult);
    }
}
