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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.MindfulnessSessionRecord;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.os.Parcel;

/**
 * @see MindfulnessSessionRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_MINDFULNESS_SESSION)
public final class MindfulnessSessionRecordInternal
        extends IntervalRecordInternal<MindfulnessSessionRecord> {

    private int mMindfulnessSessionType;
    @Nullable private String mTitle;
    @Nullable private String mNotes;

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        mMindfulnessSessionType = parcel.readInt();
        mTitle = parcel.readString();
        mNotes = parcel.readString();
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mMindfulnessSessionType);
        parcel.writeString(mTitle);
        parcel.writeString(mNotes);
    }

    @Override
    public MindfulnessSessionRecord toExternalRecord() {
        MindfulnessSessionRecord.Builder builder =
                new MindfulnessSessionRecord.Builder(
                        buildMetaData(), getStartTime(), getEndTime(), mMindfulnessSessionType);

        if (getStartZoneOffset() != null) {
            builder.setStartZoneOffset(getStartZoneOffset());
        }

        if (getEndZoneOffset() != null) {
            builder.setEndZoneOffset(getEndZoneOffset());
        }

        if (getTitle() != null) {
            builder.setTitle(getTitle());
        }

        if (getNotes() != null) {
            builder.setNotes(getNotes());
        }

        return builder.buildWithoutValidation();
    }

    @MindfulnessSessionRecord.MindfulnessSessionType
    public int getMindfulnessSessionType() {
        return mMindfulnessSessionType;
    }

    /**
     * @return this object with the measurement location.
     */
    @NonNull
    public MindfulnessSessionRecordInternal setMindfulnessSessionType(int mindfulnessSessionType) {
        mMindfulnessSessionType = mindfulnessSessionType;
        return this;
    }

    @Nullable
    public String getTitle() {
        return mTitle;
    }

    /** Returns this object with the specified title */
    @NonNull
    public MindfulnessSessionRecordInternal setTitle(String title) {
        this.mTitle = title;
        return this;
    }

    @Nullable
    public String getNotes() {
        return mNotes;
    }

    /** Returns this object with the specified notes. */
    @NonNull
    public MindfulnessSessionRecordInternal setNotes(String notes) {
        this.mNotes = notes;
        return this;
    }
}
