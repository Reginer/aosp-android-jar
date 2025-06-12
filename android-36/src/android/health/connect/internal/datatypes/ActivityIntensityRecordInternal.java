/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.android.healthfitness.flags.Flags.FLAG_ACTIVITY_INTENSITY;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.health.connect.datatypes.ActivityIntensityRecord;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.os.Parcel;

/**
 * @see ActivityIntensityRecord
 * @hide
 */
@FlaggedApi(FLAG_ACTIVITY_INTENSITY)
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_ACTIVITY_INTENSITY)
public final class ActivityIntensityRecordInternal
        extends IntervalRecordInternal<ActivityIntensityRecord> {

    private int mActivityIntensityType;

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        mActivityIntensityType = parcel.readInt();
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mActivityIntensityType);
    }

    @Override
    public ActivityIntensityRecord toExternalRecord() {
        ActivityIntensityRecord.Builder builder =
                new ActivityIntensityRecord.Builder(
                        buildMetaData(), getStartTime(), getEndTime(), mActivityIntensityType);

        if (getStartZoneOffset() != null) {
            builder.setStartZoneOffset(getStartZoneOffset());
        }

        if (getEndZoneOffset() != null) {
            builder.setEndZoneOffset(getEndZoneOffset());
        }

        return builder.buildWithoutValidation();
    }

    @ActivityIntensityRecord.ActivityIntensityType
    public int getActivityIntensityType() {
        return mActivityIntensityType;
    }

    /**
     * @return this object with the measurement location.
     */
    @NonNull
    public ActivityIntensityRecordInternal setActivityIntensityType(int activityIntensityType) {
        mActivityIntensityType = activityIntensityType;
        return this;
    }
}
