/*
 * Copyright (C) 2021 The Android Open Source Project
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
import static android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

import static com.android.internal.util.Preconditions.checkArgument;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Data class used by safety sources to propagate safety information such as their safety status and
 * safety issues.
 *
 * @hide
 */
@SystemApi
@RequiresApi(TIRAMISU)
public final class SafetySourceData implements Parcelable {

    /**
     * Indicates that no opinion is currently associated with the information provided.
     *
     * <p>This severity level will be reflected in the UI of a {@link SafetySourceStatus} through a
     * grey icon.
     *
     * <p>For a {@link SafetySourceStatus}, this severity level indicates that the safety source
     * currently does not have sufficient information on the severity level of the {@link
     * SafetySourceStatus}.
     *
     * <p>This severity level cannot be used to indicate the severity level of a {@link
     * SafetySourceIssue}.
     */
    public static final int SEVERITY_LEVEL_UNSPECIFIED = 100;

    /**
     * Indicates the presence of an informational message or the absence of any safety issues.
     *
     * <p>This severity level will be reflected in the UI of either a {@link SafetySourceStatus} or
     * a {@link SafetySourceIssue} through a green icon.
     *
     * <p>For a {@link SafetySourceStatus}, this severity level indicates either the absence of any
     * {@link SafetySourceIssue}s or the presence of only {@link SafetySourceIssue}s with the same
     * severity level.
     *
     * <p>For a {@link SafetySourceIssue}, this severity level indicates that the {@link
     * SafetySourceIssue} represents an informational message relating to the safety source. {@link
     * SafetySourceIssue}s of this severity level will be dismissible by the user from the UI, and
     * will not trigger a confirmation dialog upon a user attempting to dismiss the warning.
     */
    public static final int SEVERITY_LEVEL_INFORMATION = 200;

    /**
     * Indicates the presence of a medium-severity safety issue which the user is encouraged to act
     * on.
     *
     * <p>This severity level will be reflected in the UI of either a {@link SafetySourceStatus} or
     * a {@link SafetySourceIssue} through a yellow icon.
     *
     * <p>For a {@link SafetySourceStatus}, this severity level indicates the presence of at least
     * one medium-severity {@link SafetySourceIssue} relating to the safety source which the user is
     * encouraged to act on, and no {@link SafetySourceIssue}s with higher severity level.
     *
     * <p>For a {@link SafetySourceIssue}, this severity level indicates that the {@link
     * SafetySourceIssue} represents a medium-severity safety issue relating to the safety source
     * which the user is encouraged to act on. {@link SafetySourceIssue}s of this severity level
     * will be dismissible by the user from the UI, and will trigger a confirmation dialog upon a
     * user attempting to dismiss the warning.
     */
    public static final int SEVERITY_LEVEL_RECOMMENDATION = 300;

    /**
     * Indicates the presence of a critical or urgent safety issue that should be addressed by the
     * user.
     *
     * <p>This severity level will be reflected in the UI of either a {@link SafetySourceStatus} or
     * a {@link SafetySourceIssue} through a red icon.
     *
     * <p>For a {@link SafetySourceStatus}, this severity level indicates the presence of at least
     * one critical or urgent {@link SafetySourceIssue} relating to the safety source that should be
     * addressed by the user.
     *
     * <p>For a {@link SafetySourceIssue}, this severity level indicates that the {@link
     * SafetySourceIssue} represents a critical or urgent safety issue relating to the safety source
     * that should be addressed by the user. {@link SafetySourceIssue}s of this severity level will
     * be dismissible by the user from the UI, and will trigger a confirmation dialog upon a user
     * attempting to dismiss the warning.
     */
    public static final int SEVERITY_LEVEL_CRITICAL_WARNING = 400;

    /**
     * All possible severity levels for a {@link SafetySourceStatus} or a {@link SafetySourceIssue}.
     *
     * <p>The numerical values of the levels are not used directly, rather they are used to build a
     * continuum of levels which support relative comparison. The higher the severity level the
     * higher the threat to the user.
     *
     * <p>For a {@link SafetySourceStatus}, the severity level is meant to convey the aggregated
     * severity of the safety source, and it contributes to the overall severity level in the Safety
     * Center. If the {@link SafetySourceData} contains {@link SafetySourceIssue}s, the severity
     * level of the s{@link SafetySourceStatus} must match the highest severity level among the
     * {@link SafetySourceIssue}s.
     *
     * <p>For a {@link SafetySourceIssue}, not all severity levels can be used. The severity level
     * also determines how a {@link SafetySourceIssue}s is "dismissible" by the user, i.e. how the
     * user can choose to ignore the issue and remove it from view in the Safety Center.
     *
     * @hide
     */
    @IntDef(
            prefix = {"SEVERITY_LEVEL_"},
            value = {
                SEVERITY_LEVEL_UNSPECIFIED,
                SEVERITY_LEVEL_INFORMATION,
                SEVERITY_LEVEL_RECOMMENDATION,
                SEVERITY_LEVEL_CRITICAL_WARNING
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SeverityLevel {}

    @NonNull
    public static final Creator<SafetySourceData> CREATOR =
            new Creator<SafetySourceData>() {
                @Override
                public SafetySourceData createFromParcel(Parcel in) {
                    SafetySourceStatus status = in.readTypedObject(SafetySourceStatus.CREATOR);
                    List<SafetySourceIssue> issues =
                            requireNonNull(in.createTypedArrayList(SafetySourceIssue.CREATOR));
                    Builder builder = new Builder().setStatus(status);
                    for (int i = 0; i < issues.size(); i++) {
                        builder.addIssue(issues.get(i));
                    }
                    if (SdkLevel.isAtLeastU()) {
                        Bundle extras = in.readBundle(getClass().getClassLoader());
                        if (extras != null) {
                            builder.setExtras(extras);
                        }
                    }
                    return builder.build();
                }

                @Override
                public SafetySourceData[] newArray(int size) {
                    return new SafetySourceData[size];
                }
            };

    @Nullable private final SafetySourceStatus mStatus;
    @NonNull private final List<SafetySourceIssue> mIssues;
    @NonNull private final Bundle mExtras;

    private SafetySourceData(
            @Nullable SafetySourceStatus status,
            @NonNull List<SafetySourceIssue> issues,
            @NonNull Bundle extras) {
        this.mStatus = status;
        this.mIssues = issues;
        this.mExtras = extras;
    }

    /** Returns the data for the {@link SafetySourceStatus} to be shown in UI. */
    @Nullable
    public SafetySourceStatus getStatus() {
        return mStatus;
    }

    /** Returns the data for the list of {@link SafetySourceIssue}s to be shown in UI. */
    @NonNull
    public List<SafetySourceIssue> getIssues() {
        return mIssues;
    }

    /**
     * Returns a {@link Bundle} containing additional information, {@link Bundle#EMPTY} by default.
     *
     * <p>Note: internal state of this {@link Bundle} is not used for {@link Object#equals} and
     * {@link Object#hashCode} implementation of {@link SafetySourceData}.
     */
    @NonNull
    @RequiresApi(UPSIDE_DOWN_CAKE)
    public Bundle getExtras() {
        if (!SdkLevel.isAtLeastU()) {
            throw new UnsupportedOperationException(
                    "Method not supported on versions lower than UPSIDE_DOWN_CAKE");
        }
        return mExtras;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeTypedObject(mStatus, flags);
        dest.writeTypedList(mIssues);
        if (SdkLevel.isAtLeastU()) {
            dest.writeBundle(mExtras);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetySourceData)) return false;
        SafetySourceData that = (SafetySourceData) o;
        return Objects.equals(mStatus, that.mStatus) && mIssues.equals(that.mIssues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mStatus, mIssues);
    }

    @Override
    public String toString() {
        return "SafetySourceData{"
                + "mStatus="
                + mStatus
                + ", mIssues="
                + mIssues
                + (!mExtras.isEmpty() ? ", (has extras)" : "")
                + '}';
    }

    /** Builder class for {@link SafetySourceData}. */
    public static final class Builder {

        @NonNull private final List<SafetySourceIssue> mIssues = new ArrayList<>();

        @Nullable private SafetySourceStatus mStatus;
        @NonNull private Bundle mExtras = Bundle.EMPTY;

        /** Creates a {@link Builder} for a {@link SafetySourceData}. */
        public Builder() {}

        /** Creates a {@link Builder} with the values from the given {@link SafetySourceData}. */
        @RequiresApi(UPSIDE_DOWN_CAKE)
        public Builder(@NonNull SafetySourceData safetySourceData) {
            if (!SdkLevel.isAtLeastU()) {
                throw new UnsupportedOperationException(
                        "Method not supported on versions lower than UPSIDE_DOWN_CAKE");
            }
            requireNonNull(safetySourceData);
            mIssues.addAll(safetySourceData.mIssues);
            mStatus = safetySourceData.mStatus;
            mExtras = safetySourceData.mExtras.deepCopy();
        }

        /** Sets data for the {@link SafetySourceStatus} to be shown in UI. */
        @NonNull
        public Builder setStatus(@Nullable SafetySourceStatus status) {
            mStatus = status;
            return this;
        }

        /** Adds data for a {@link SafetySourceIssue} to be shown in UI. */
        @NonNull
        public Builder addIssue(@NonNull SafetySourceIssue safetySourceIssue) {
            mIssues.add(requireNonNull(safetySourceIssue));
            return this;
        }

        /**
         * Sets additional information for the {@link SafetySourceData}.
         *
         * <p>If not set, the default value is {@link Bundle#EMPTY}.
         */
        @NonNull
        @RequiresApi(UPSIDE_DOWN_CAKE)
        public Builder setExtras(@NonNull Bundle extras) {
            if (!SdkLevel.isAtLeastU()) {
                throw new UnsupportedOperationException(
                        "Method not supported on versions lower than UPSIDE_DOWN_CAKE");
            }
            mExtras = requireNonNull(extras);
            return this;
        }

        /**
         * Resets additional information for the {@link SafetySourceData} to the default value of
         * {@link Bundle#EMPTY}.
         */
        @NonNull
        @RequiresApi(UPSIDE_DOWN_CAKE)
        public Builder clearExtras() {
            if (!SdkLevel.isAtLeastU()) {
                throw new UnsupportedOperationException(
                        "Method not supported on versions lower than UPSIDE_DOWN_CAKE");
            }
            mExtras = Bundle.EMPTY;
            return this;
        }

        /**
         * Clears data for all the {@link SafetySourceIssue}s that were added to this {@link
         * Builder}.
         */
        @NonNull
        public Builder clearIssues() {
            mIssues.clear();
            return this;
        }

        /** Creates the {@link SafetySourceData} defined by this {@link Builder}. */
        @NonNull
        public SafetySourceData build() {
            List<SafetySourceIssue> issues = unmodifiableList(new ArrayList<>(mIssues));
            int issuesMaxSeverityLevel = getIssuesMaxSeverityLevelEnforcingUniqueIds(issues);
            if (mStatus == null) {
                return new SafetySourceData(null, issues, mExtras);
            }
            int statusSeverityLevel = mStatus.getSeverityLevel();
            boolean requiresAttention = issuesMaxSeverityLevel > SEVERITY_LEVEL_INFORMATION;
            if (requiresAttention) {
                checkArgument(
                        statusSeverityLevel >= issuesMaxSeverityLevel,
                        "Safety source data cannot have issues that are more severe than its"
                                + " status");
            }

            return new SafetySourceData(mStatus, issues, mExtras);
        }

        private static int getIssuesMaxSeverityLevelEnforcingUniqueIds(
                @NonNull List<SafetySourceIssue> issues) {
            int max = Integer.MIN_VALUE;
            Set<String> issueIds = new HashSet<>();
            for (int i = 0; i < issues.size(); i++) {
                SafetySourceIssue safetySourceIssue = issues.get(i);

                String issueId = safetySourceIssue.getId();
                checkArgument(
                        !issueIds.contains(issueId),
                        "Safety source data cannot have duplicate issue ids");
                max = Math.max(max, safetySourceIssue.getSeverityLevel());
                issueIds.add(issueId);
            }
            return max;
        }
    }
}
