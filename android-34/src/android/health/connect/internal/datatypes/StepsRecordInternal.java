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

import static android.health.connect.Constants.DEFAULT_INT;

import android.annotation.NonNull;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.StepsRecord;
import android.os.Parcel;

/**
 * @see StepsRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_STEPS)
public final class StepsRecordInternal extends IntervalRecordInternal<StepsRecord> {
    private int mCount = DEFAULT_INT;

    public int getCount() {
        return mCount;
    }

    /** returns this object with the specified count */
    @NonNull
    public StepsRecordInternal setCount(int count) {
        this.mCount = count;
        return this;
    }

    @NonNull
    @Override
    public StepsRecord toExternalRecord() {
        return new StepsRecord.Builder(buildMetaData(), getStartTime(), getEndTime(), getCount())
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .buildWithoutValidation();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        mCount = parcel.readInt();
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mCount);
    }
}
