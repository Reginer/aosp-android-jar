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
import android.health.connect.datatypes.MenstruationPeriodRecord;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.os.Parcel;

/**
 * @see MenstruationPeriodRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_PERIOD)
public final class MenstruationPeriodRecordInternal
        extends IntervalRecordInternal<MenstruationPeriodRecord> {

    @NonNull
    @Override
    public MenstruationPeriodRecord toExternalRecord() {
        return new MenstruationPeriodRecord.Builder(buildMetaData(), getStartTime(), getEndTime())
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .buildWithoutValidation();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {}

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {}
}
