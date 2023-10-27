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
import android.health.connect.datatypes.WheelchairPushesRecord;
import android.os.Parcel;

/**
 * @see WheelchairPushesRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_WHEELCHAIR_PUSHES)
public final class WheelchairPushesRecordInternal
        extends IntervalRecordInternal<WheelchairPushesRecord> {
    private int mCount;

    public long getCount() {
        return mCount;
    }

    /** returns this object with the specified count */
    @NonNull
    public WheelchairPushesRecordInternal setCount(int count) {
        this.mCount = count;
        return this;
    }

    @NonNull
    @Override
    public WheelchairPushesRecord toExternalRecord() {
        return new WheelchairPushesRecord.Builder(
                        buildMetaData(), getStartTime(), getEndTime(), getCount())
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
