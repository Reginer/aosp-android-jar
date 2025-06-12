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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;

/**
 * Status for a scheduled export. Parcelable used to communicate the status of a scheduled export
 * between the system server and the UI components via HealthConnectService.
 *
 * @hide
 */
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

    /**
     * No error or success state available yet.
     *
     * @hide
     */
    public static final int DATA_EXPORT_ERROR_UNSPECIFIED = -1;

    /**
     * No error during the last data export.
     *
     * @hide
     */
    public static final int DATA_EXPORT_ERROR_NONE = 0;

    /**
     * Unknown error during the last data export.
     *
     * @hide
     */
    public static final int DATA_EXPORT_ERROR_UNKNOWN = 1;

    /**
     * Indicates that the last export failed because we lost access to the export file location.
     *
     * @hide
     */
    public static final int DATA_EXPORT_LOST_FILE_ACCESS = 2;

    /**
     * Indicates that an export was started and is ongoing.
     *
     * @hide
     */
    public static final int DATA_EXPORT_STARTED = 3;

    /**
     * Indicates that the last export failed while trying to clear the log tables. Probably because
     * the file was corrupted during the copy, and it was not a valid HealthConnectDatabase anymore.
     *
     * @hide
     */
    public static final int DATA_EXPORT_ERROR_CLEARING_LOG_TABLES = 4;

    /**
     * Indicates that the last export failed while trying to clear the PHR tables. Probably because
     * the file was corrupted during the copy, and it was not a valid HealthConnectDatabase anymore.
     *
     * @hide
     */
    public static final int DATA_EXPORT_ERROR_CLEARING_PHR_TABLES = 5;

    /** @hide */
    // TODO(b/356393172) rename to Status & include DATA_EXPORT_STARTED during Statuses cleanup.
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        DATA_EXPORT_ERROR_UNSPECIFIED,
        DATA_EXPORT_ERROR_UNKNOWN,
        DATA_EXPORT_ERROR_NONE,
        DATA_EXPORT_LOST_FILE_ACCESS,
        DATA_EXPORT_ERROR_CLEARING_LOG_TABLES
    })
    public @interface DataExportError {}

    @Nullable private final Instant mLastSuccessfulExportTime;

    @Nullable private final Instant mLastFailedExportTime;

    @DataExportError private final int mDataExportError;
    private final int mPeriodInDays;
    @Nullable private final String mLastExportFileName;
    @Nullable private final String mLastExportAppName;
    @Nullable private final String mNextExportFileName;
    @Nullable private final String mNextExportAppName;
    private final int mNextExportSequentialNumber;

    private ScheduledExportStatus(
            @Nullable Instant lastSuccessfulExportTime,
            @Nullable Instant lastFailedExportTime,
            @DataExportError int dataExportError,
            int periodInDays,
            @Nullable String lastExportFileName,
            @Nullable String lastExportAppName,
            @Nullable String nextExportFileName,
            @Nullable String nextExportAppName,
            int nextExportSequentialNumber) {
        mLastSuccessfulExportTime = lastSuccessfulExportTime;
        mLastFailedExportTime = lastFailedExportTime;
        mDataExportError = dataExportError;
        mPeriodInDays = periodInDays;
        mLastExportFileName = lastExportFileName;
        mLastExportAppName = lastExportAppName;
        mNextExportFileName = nextExportFileName;
        mNextExportAppName = nextExportAppName;
        mNextExportSequentialNumber = nextExportSequentialNumber;
    }

    /**
     * Returns the time for the last successful export, or null if there hasn't been a successful
     * export to the current location.
     */
    @Nullable
    public Instant getLastSuccessfulExportTime() {
        return mLastSuccessfulExportTime;
    }

    /**
     * Returns the time of the last failed export attempt, or null if there hasn't been a failed
     * export.
     */
    @Nullable
    public Instant getLastFailedExportTime() {
        return mLastFailedExportTime;
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

    /** Returns the file name of the last successful export. */
    @Nullable
    public String getLastExportFileName() {
        return mLastExportFileName;
    }

    /** Returns the app name of the last successful export. */
    @Nullable
    public String getLastExportAppName() {
        return mLastExportAppName;
    }

    /** Returns the file name of the next export. */
    @Nullable
    public String getNextExportFileName() {
        return mNextExportFileName;
    }

    /** Returns the app name of the last successful export. */
    @Nullable
    public String getNextExportAppName() {
        return mNextExportAppName;
    }

    /** Returns the next export sequential number. */
    public int getNextExportSequentialNumber() {
        return mNextExportSequentialNumber;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private ScheduledExportStatus(@NonNull Parcel in) {
        long timestamp = in.readLong();
        mLastSuccessfulExportTime = timestamp == 0 ? null : Instant.ofEpochMilli(timestamp);
        long lastFailedExportTimestamp = in.readLong();
        mLastFailedExportTime =
                timestamp == 0 ? null : Instant.ofEpochMilli(lastFailedExportTimestamp);
        mDataExportError = in.readInt();
        mPeriodInDays = in.readInt();
        mLastExportFileName = in.readString();
        mLastExportAppName = in.readString();
        mNextExportFileName = in.readString();
        mNextExportAppName = in.readString();
        mNextExportSequentialNumber = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(
                mLastSuccessfulExportTime == null ? 0 : mLastSuccessfulExportTime.toEpochMilli());
        dest.writeLong(mLastFailedExportTime == null ? 0 : mLastFailedExportTime.toEpochMilli());
        dest.writeInt(mDataExportError);
        dest.writeInt(mPeriodInDays);
        dest.writeString(mLastExportFileName);
        dest.writeString(mLastExportAppName);
        dest.writeString(mNextExportFileName);
        dest.writeString(mNextExportAppName);
        dest.writeInt(mNextExportSequentialNumber);
    }

    /** Builder for {@link ScheduledExportStatus}. */
    public static final class Builder {
        @Nullable private Instant mLastSuccessfulExportTime;
        @Nullable private Instant mLastFailedExportTime;
        @DataExportError private int mDataExportError;
        private int mPeriodInDays;
        @Nullable private String mLastExportFileName;
        @Nullable private String mLastExportAppName;
        @Nullable private String mNextExportFileName;
        @Nullable private String mNextExportAppName;
        private int mNextExportSequentialNumber;

        public Builder() {}

        /**
         * Sets the time for the last successful export, or null if there hasn't been a successful
         * export to the current location.
         */
        public Builder setLastSuccessfulExportTime(@Nullable Instant lastSuccessfulExportTime) {
            mLastSuccessfulExportTime = lastSuccessfulExportTime;
            return this;
        }

        /**
         * Sets the time of the last failed export attempt, or null if there hasn't been a failed
         * export.
         */
        public Builder setLastFailedExportTime(@Nullable Instant lastFailedExportTime) {
            mLastFailedExportTime = lastFailedExportTime;
            return this;
        }

        /**
         * Sets the error status of the last export attempt.
         *
         * <p>Defaults to {@link ScheduledExportStatus#DATA_EXPORT_ERROR_NONE}.
         */
        public Builder setDataExportError(@DataExportError int dataExportError) {
            mDataExportError = dataExportError;
            return this;
        }

        /**
         * Sets the period between scheduled exports.
         *
         * <p>A value of 0 indicates that no setting has been set.
         */
        public Builder setPeriodInDays(int periodInDays) {
            mPeriodInDays = periodInDays;
            return this;
        }

        /**
         * Sets the file name of the last successful export.
         *
         * <p>Defaults to null.
         */
        public Builder setLastExportFileName(@Nullable String lastExportFileName) {
            mLastExportFileName = lastExportFileName;
            return this;
        }

        /**
         * Sets the app name of the last successful export.
         *
         * <p>Defaults to null.
         */
        public Builder setLastExportAppName(@Nullable String lastExportAppName) {
            mLastExportAppName = lastExportAppName;
            return this;
        }

        /**
         * Sets the file name of the next export.
         *
         * <p>Defaults to null.
         */
        public Builder setNextExportFileName(@Nullable String nextExportFileName) {
            mNextExportFileName = nextExportFileName;
            return this;
        }

        /**
         * Sets the app name of the next export.
         *
         * <p>Defaults to null.
         */
        public Builder setNextExportAppName(@Nullable String nextExportAppName) {
            mNextExportAppName = nextExportAppName;
            return this;
        }

        /** Sets the next export sequential number. */
        public Builder setNextExportSequentialNumber(int nextExportSequentialNumber) {
            mNextExportSequentialNumber = nextExportSequentialNumber;
            return this;
        }

        /** Builds a {@link ScheduledExportStatus} object. */
        public ScheduledExportStatus build() {
            return new ScheduledExportStatus(
                    mLastSuccessfulExportTime,
                    mLastFailedExportTime,
                    mDataExportError,
                    mPeriodInDays,
                    mLastExportFileName,
                    mLastExportAppName,
                    mNextExportFileName,
                    mNextExportAppName,
                    mNextExportSequentialNumber);
        }
    }
}
