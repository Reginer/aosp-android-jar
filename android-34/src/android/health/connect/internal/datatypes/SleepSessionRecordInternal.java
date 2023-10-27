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
import android.annotation.Nullable;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.SleepSessionRecord;
import android.os.Parcel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @see SleepSessionRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION)
public final class SleepSessionRecordInternal extends IntervalRecordInternal<SleepSessionRecord> {
    private List<SleepStageInternal> mStages;
    private String mNotes;
    private String mTitle;

    @Nullable
    public String getNotes() {
        return mNotes;
    }

    /** returns this object with the specified notes */
    @NonNull
    public SleepSessionRecordInternal setNotes(String notes) {
        mNotes = notes;
        return this;
    }

    @Nullable
    public String getTitle() {
        return mTitle;
    }

    /** returns this object with the specified title */
    @NonNull
    public SleepSessionRecordInternal setTitle(String title) {
        mTitle = title;
        return this;
    }

    /** returns sleep stages of this session */
    @Nullable
    public List<SleepStageInternal> getSleepStages() {
        return mStages;
    }

    /** returns this object with sleep stages set */
    public SleepSessionRecordInternal setSleepStages(@NonNull List<SleepStageInternal> stages) {
        Objects.requireNonNull(stages);
        mStages = new ArrayList<>(stages);
        return this;
    }

    /** Add sleep stage to record stages. */
    public SleepSessionRecordInternal addSleepStage(@Nullable SleepStageInternal stage) {
        if (stage != null) {
            if (mStages == null) {
                mStages = new ArrayList<>();
            }
            mStages.add(stage);
        }
        return this;
    }

    @Override
    public void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeString(mNotes);
        parcel.writeString(mTitle);
        SleepStageInternal.writeStagesToParcel(mStages, parcel);
    }

    @Override
    public void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        mNotes = parcel.readString();
        mTitle = parcel.readString();
        mStages = SleepStageInternal.populateStagesFromParcel(parcel);
    }

    @NonNull
    @Override
    public SleepSessionRecord toExternalRecord() {
        SleepSessionRecord.Builder builder =
                new SleepSessionRecord.Builder(buildMetaData(), getStartTime(), getEndTime());

        if (getStartZoneOffset() != null) {
            builder.setStartZoneOffset(getStartZoneOffset());
        }

        if (getEndZoneOffset() != null) {
            builder.setEndZoneOffset(getEndZoneOffset());
        }

        if (getNotes() != null) {
            builder.setNotes(getNotes());
        }

        if (getTitle() != null) {
            builder.setTitle(getTitle());
        }

        if (getSleepStages() != null) {
            builder.setStages(SleepStageInternal.getExternalStages(mStages));
        }
        return builder.buildWithoutValidation();
    }
}
