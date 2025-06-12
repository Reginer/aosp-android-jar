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
import android.health.connect.datatypes.SexualActivityRecord;
import android.health.connect.datatypes.SexualActivityRecord.SexualActivityProtectionUsed;
import android.os.Parcel;

/**
 * @see SexualActivityRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_SEXUAL_ACTIVITY)
public final class SexualActivityRecordInternal
        extends InstantRecordInternal<SexualActivityRecord> {
    private int mProtectionUsed;

    @SexualActivityProtectionUsed.SexualActivityProtectionUsedTypes
    public int getProtectionUsed() {
        return mProtectionUsed;
    }

    /** returns this object with the specified protectionUsed */
    @NonNull
    public SexualActivityRecordInternal setProtectionUsed(int protectionUsed) {
        this.mProtectionUsed = protectionUsed;
        return this;
    }

    @NonNull
    @Override
    public SexualActivityRecord toExternalRecord() {
        return new SexualActivityRecord.Builder(buildMetaData(), getTime(), getProtectionUsed())
                .setZoneOffset(getZoneOffset())
                .buildWithoutValidation();
    }

    @Override
    void populateInstantRecordFrom(@NonNull Parcel parcel) {
        mProtectionUsed = parcel.readInt();
    }

    @Override
    void populateInstantRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mProtectionUsed);
    }
}
