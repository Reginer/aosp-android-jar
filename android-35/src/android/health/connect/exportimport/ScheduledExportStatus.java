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

package android.health.connect.exportimport;

import static com.android.healthfitness.flags.Flags.FLAG_EXPORT_IMPORT;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.HealthConnectManager;
import android.os.Parcel;
import android.os.Parcelable;

import java.time.Instant;

/**
 * Status for the scheduled scheduled export.
 *
 * @hide
 */
@FlaggedApi(FLAG_EXPORT_IMPORT)
public final class ScheduledExportStatus implements Parcelable {
    @NonNull
    public static final Creator<ScheduledExportStatus> CREATOR =
            new Creator<>() {
                @Override
                public ScheduledExportStatus createFromParcel(Parcel in) {
                    return new ScheduledExportStatus(in);
                }

                @Override
                public ScheduledExportStatus[] newArray(int size) {
                    return new ScheduledExportStatus[size];
                }
            };

    @Nullable private final Instant mLastSuccessfulExportTime;

    @HealthConnectManager.DataExportError private final int mDataExportError;
    private final int mPeriodInDays;

    public ScheduledExportStatus(
            @Nullable Instant lastSuccessfulExportTime,
            @HealthConnectManager.DataExportError int dataExportError,
            int periodInDays) {
        mLastSuccessfulExportTime = lastSuccessfulExportTime;
        mDataExportError = dataExportError;
        mPeriodInDays = periodInDays;
    }

    /**
     * Returns the time for the last successful export, or null if there hasn't been a successful
     * export to the current location.
     */
    @Nullable
    public Instant getLastSuccessfulExportTime() {
        return mLastSuccessfulExportTime;
    }

    /** Returns the error status of the last export attempt. */
    public int getDataExportError() {
        return mDataExportError;
    }

    /**
     * Returns the period between scheduled exports.
     *
     * <p>A value of 0 indicates that no setting has been set.
     */
    // TODO(b/341017907): Consider adding hasPeriodInDays.
    public int getPeriodInDays() {
        return mPeriodInDays;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private ScheduledExportStatus(@NonNull Parcel in) {
        long timestamp = in.readLong();
        mLastSuccessfulExportTime = timestamp == 0 ? null : Instant.ofEpochMilli(timestamp);
        mDataExportError = in.readInt();
        mPeriodInDays = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(
                mLastSuccessfulExportTime == null ? 0 : mLastSuccessfulExportTime.toEpochMilli());
        dest.writeInt(mDataExportError);
        dest.writeInt(mPeriodInDays);
    }
}
