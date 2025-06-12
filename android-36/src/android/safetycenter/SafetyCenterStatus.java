/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.safetycenter;

import static android.os.Build.VERSION_CODES.TIRAMISU;

import static java.util.Objects.requireNonNull;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * The overall status of the Safety Center.
 *
 * @hide
 */
@SystemApi
@RequiresApi(TIRAMISU)
public final class SafetyCenterStatus implements Parcelable {

    /** Indicates the overall severity level of the Safety Center is not currently known. */
    public static final int OVERALL_SEVERITY_LEVEL_UNKNOWN = 1000;

    /**
     * Indicates the overall safety status of the device is OK and there are no actionable issues.
     */
    public static final int OVERALL_SEVERITY_LEVEL_OK = 1100;

    /** Indicates the presence of safety recommendations which the user is encouraged to act on. */
    public static final int OVERALL_SEVERITY_LEVEL_RECOMMENDATION = 1200;

    /** Indicates the presence of critical safety warnings on the device. */
    public static final int OVERALL_SEVERITY_LEVEL_CRITICAL_WARNING = 1300;

    /**
     * All possible overall severity levels for the Safety Center.
     *
     * <p>The overall severity level is calculated from the severity level and statuses of all
     * issues and entries in the Safety Center.
     *
     * @hide
     * @see #getSeverityLevel()
     * @see Builder#setSeverityLevel(int)
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = "OVERALL_SEVERITY_LEVEL_",
            value = {
                OVERALL_SEVERITY_LEVEL_UNKNOWN,
                OVERALL_SEVERITY_LEVEL_OK,
                OVERALL_SEVERITY_LEVEL_RECOMMENDATION,
                OVERALL_SEVERITY_LEVEL_CRITICAL_WARNING,
            })
    public @interface OverallSeverityLevel {}

    /** Indicates that no refresh is ongoing. */
    public static final int REFRESH_STATUS_NONE = 0;

    /**
     * Indicates that a data fetch is ongoing, and Safety Sources are being asked for their current
     * safety state.
     *
     * <p>If sources already have their safety data cached, they may provide it without triggering a
     * process to fetch or recompute state which may be expensive and/or slow.
     */
    public static final int REFRESH_STATUS_DATA_FETCH_IN_PROGRESS = 10100;

    /**
     * Indicates that a full rescan is ongoing, and Safety Sources are being asked to fetch fresh
     * data for their safety state.
     *
     * <p>The term "fresh" here means that the sources should ensure that the safety data is
     * accurate as possible at the time of providing it to Safety Center, even if it involves
     * performing an expensive and/or slow process.
     */
    public static final int REFRESH_STATUS_FULL_RESCAN_IN_PROGRESS = 10200;

    /**
     * All possible refresh states for the Safety Center.
     *
     * @hide
     * @see #getRefreshStatus()
     * @see Builder#setRefreshStatus(int)
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = "REFRESH_STATUS_",
            value = {
                REFRESH_STATUS_NONE,
                REFRESH_STATUS_DATA_FETCH_IN_PROGRESS,
                REFRESH_STATUS_FULL_RESCAN_IN_PROGRESS,
            })
    public @interface RefreshStatus {}

    @NonNull
    public static final Creator<SafetyCenterStatus> CREATOR =
            new Creator<SafetyCenterStatus>() {
                @Override
                public SafetyCenterStatus createFromParcel(Parcel in) {
                    CharSequence title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
                    CharSequence summary = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
                    return new Builder(title, summary)
                            .setSeverityLevel(in.readInt())
                            .setRefreshStatus(in.readInt())
                            .build();
                }

                @Override
                public SafetyCenterStatus[] newArray(int size) {
                    return new SafetyCenterStatus[size];
                }
            };

    @NonNull private final CharSequence mTitle;
    @NonNull private final CharSequence mSummary;
    @OverallSeverityLevel private final int mSeverityLevel;
    @RefreshStatus private final int mRefreshStatus;

    private SafetyCenterStatus(
            @NonNull CharSequence title,
            @NonNull CharSequence summary,
            @OverallSeverityLevel int severityLevel,
            @RefreshStatus int refreshStatus) {
        mTitle = title;
        mSummary = summary;
        mSeverityLevel = severityLevel;
        mRefreshStatus = refreshStatus;
    }

    /** Returns the title which describes the overall safety state of the device. */
    @NonNull
    public CharSequence getTitle() {
        return mTitle;
    }

    /** Returns the summary text which adds detail to the overall safety state of the device. */
    @NonNull
    public CharSequence getSummary() {
        return mSummary;
    }

    /** Returns the current {@link OverallSeverityLevel} of the Safety Center. */
    @OverallSeverityLevel
    public int getSeverityLevel() {
        return mSeverityLevel;
    }

    /** Returns the current {@link RefreshStatus} of the Safety Center */
    @RefreshStatus
    public int getRefreshStatus() {
        return mRefreshStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetyCenterStatus)) return false;
        SafetyCenterStatus that = (SafetyCenterStatus) o;
        return mSeverityLevel == that.mSeverityLevel
                && mRefreshStatus == that.mRefreshStatus
                && TextUtils.equals(mTitle, that.mTitle)
                && TextUtils.equals(mSummary, that.mSummary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mSummary, mSeverityLevel, mRefreshStatus);
    }

    @Override
    public String toString() {
        return "SafetyCenterStatus{"
                + "mTitle="
                + mTitle
                + ", mSummary="
                + mSummary
                + ", mSeverityLevel="
                + mSeverityLevel
                + ", mRefreshStatus="
                + mRefreshStatus
                + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        TextUtils.writeToParcel(mTitle, dest, flags);
        TextUtils.writeToParcel(mSummary, dest, flags);
        dest.writeInt(mSeverityLevel);
        dest.writeInt(mRefreshStatus);
    }

    /** Builder class for {@link SafetyCenterStatus}. */
    public static final class Builder {

        @NonNull private CharSequence mTitle;
        @NonNull private CharSequence mSummary;
        @OverallSeverityLevel private int mSeverityLevel = OVERALL_SEVERITY_LEVEL_UNKNOWN;
        @RefreshStatus private int mRefreshStatus = REFRESH_STATUS_NONE;

        /**
         * Creates a new {@link Builder} for a {@link SafetyCenterStatus}.
         *
         * @param title an overall title for the status
         * @param summary a summary for the status
         */
        public Builder(@NonNull CharSequence title, @NonNull CharSequence summary) {
            mTitle = requireNonNull(title);
            mSummary = requireNonNull(summary);
        }

        /** Creates a {@link Builder} with the values from the given {@link SafetyCenterStatus}. */
        public Builder(@NonNull SafetyCenterStatus safetyCenterStatus) {
            mTitle = safetyCenterStatus.mTitle;
            mSummary = safetyCenterStatus.mSummary;
            mSeverityLevel = safetyCenterStatus.mSeverityLevel;
            mRefreshStatus = safetyCenterStatus.mRefreshStatus;
        }

        /** Sets the title for this status. */
        @NonNull
        public Builder setTitle(@NonNull CharSequence title) {
            mTitle = requireNonNull(title);
            return this;
        }

        /** Sets the summary text for this status. */
        @NonNull
        public Builder setSummary(@NonNull CharSequence summary) {
            mSummary = requireNonNull(summary);
            return this;
        }

        /**
         * Sets the {@link OverallSeverityLevel} of this status. Defaults to {@link
         * #OVERALL_SEVERITY_LEVEL_UNKNOWN}.
         */
        @NonNull
        public Builder setSeverityLevel(@OverallSeverityLevel int severityLevel) {
            mSeverityLevel = validateOverallSeverityLevel(severityLevel);
            return this;
        }

        /**
         * Sets the {@link RefreshStatus} of this status. Defaults to {@link #REFRESH_STATUS_NONE}.
         */
        @NonNull
        public Builder setRefreshStatus(@RefreshStatus int refreshStatus) {
            mRefreshStatus = validateRefreshStatus(refreshStatus);
            return this;
        }

        /** Creates the {@link SafetyCenterStatus} defined by this {@link Builder}. */
        @NonNull
        public SafetyCenterStatus build() {
            return new SafetyCenterStatus(mTitle, mSummary, mSeverityLevel, mRefreshStatus);
        }
    }

    @OverallSeverityLevel
    private static int validateOverallSeverityLevel(int value) {
        switch (value) {
            case OVERALL_SEVERITY_LEVEL_UNKNOWN:
            case OVERALL_SEVERITY_LEVEL_OK:
            case OVERALL_SEVERITY_LEVEL_RECOMMENDATION:
            case OVERALL_SEVERITY_LEVEL_CRITICAL_WARNING:
                return value;
            default:
        }
        throw new IllegalArgumentException(
                "Unexpected OverallSeverityLevel for SafetyCenterStatus: " + value);
    }

    @RefreshStatus
    private static int validateRefreshStatus(int value) {
        switch (value) {
            case REFRESH_STATUS_NONE:
            case REFRESH_STATUS_DATA_FETCH_IN_PROGRESS:
            case REFRESH_STATUS_FULL_RESCAN_IN_PROGRESS:
                return value;
            default:
        }
        throw new IllegalArgumentException(
                "Unexpected RefreshStatus for SafetyCenterStatus: " + value);
    }
}
