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

import static java.util.Objects.requireNonNull;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Data for a safety source status in the Safety Center page, which conveys the overall state of the
 * safety source and allows a user to navigate to the source.
 *
 * @hide
 */
@SystemApi
@RequiresApi(TIRAMISU)
public final class SafetySourceStatus implements Parcelable {

    @NonNull
    public static final Creator<SafetySourceStatus> CREATOR =
            new Creator<SafetySourceStatus>() {
                @Override
                public SafetySourceStatus createFromParcel(Parcel in) {
                    CharSequence title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
                    CharSequence summary = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
                    int severityLevel = in.readInt();
                    return new Builder(title, summary, severityLevel)
                            .setPendingIntent(in.readTypedObject(PendingIntent.CREATOR))
                            .setIconAction(in.readTypedObject(IconAction.CREATOR))
                            .setEnabled(in.readBoolean())
                            .build();
                }

                @Override
                public SafetySourceStatus[] newArray(int size) {
                    return new SafetySourceStatus[size];
                }
            };

    @NonNull private final CharSequence mTitle;
    @NonNull private final CharSequence mSummary;
    @SafetySourceData.SeverityLevel private final int mSeverityLevel;
    @Nullable private final PendingIntent mPendingIntent;
    @Nullable private final IconAction mIconAction;
    private final boolean mEnabled;

    private SafetySourceStatus(
            @NonNull CharSequence title,
            @NonNull CharSequence summary,
            @SafetySourceData.SeverityLevel int severityLevel,
            @Nullable PendingIntent pendingIntent,
            @Nullable IconAction iconAction,
            boolean enabled) {
        this.mTitle = title;
        this.mSummary = summary;
        this.mSeverityLevel = severityLevel;
        this.mPendingIntent = pendingIntent;
        this.mIconAction = iconAction;
        this.mEnabled = enabled;
    }

    /** Returns the localized title of the safety source status to be displayed in the UI. */
    @NonNull
    public CharSequence getTitle() {
        return mTitle;
    }

    /** Returns the localized summary of the safety source status to be displayed in the UI. */
    @NonNull
    public CharSequence getSummary() {
        return mSummary;
    }

    /** Returns the {@link SafetySourceData.SeverityLevel} of the status. */
    @SafetySourceData.SeverityLevel
    public int getSeverityLevel() {
        return mSeverityLevel;
    }

    /**
     * Returns an optional {@link PendingIntent} that will start an activity when the safety source
     * status UI is clicked on.
     *
     * <p>The action contained in the {@link PendingIntent} must start an activity.
     *
     * <p>If {@code null} the intent action defined in the Safety Center configuration will be
     * invoked when the safety source status UI is clicked on. If the intent action is undefined or
     * disabled the source is considered as disabled.
     */
    @Nullable
    public PendingIntent getPendingIntent() {
        return mPendingIntent;
    }

    /**
     * Returns an optional {@link IconAction} to be displayed in the safety source status UI.
     *
     * <p>The icon action will be a clickable icon which performs an action as indicated by the
     * icon.
     */
    @Nullable
    public IconAction getIconAction() {
        return mIconAction;
    }

    /**
     * Returns whether the safety source status is enabled.
     *
     * <p>A safety source status should be disabled if it is currently unavailable on the device
     *
     * <p>If disabled, the status will show as grayed out in the UI, and interactions with it may be
     * limited.
     */
    public boolean isEnabled() {
        return mEnabled;
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
        dest.writeTypedObject(mPendingIntent, flags);
        dest.writeTypedObject(mIconAction, flags);
        dest.writeBoolean(mEnabled);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetySourceStatus)) return false;
        SafetySourceStatus that = (SafetySourceStatus) o;
        return mSeverityLevel == that.mSeverityLevel
                && mEnabled == that.mEnabled
                && TextUtils.equals(mTitle, that.mTitle)
                && TextUtils.equals(mSummary, that.mSummary)
                && Objects.equals(mPendingIntent, that.mPendingIntent)
                && Objects.equals(mIconAction, that.mIconAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mTitle, mSummary, mSeverityLevel, mPendingIntent, mIconAction, mEnabled);
    }

    @Override
    public String toString() {
        return "SafetySourceStatus{"
                + "mTitle="
                + mTitle
                + ", mSummary="
                + mSummary
                + ", mSeverityLevel="
                + mSeverityLevel
                + ", mPendingIntent="
                + mPendingIntent
                + ", mIconAction="
                + mIconAction
                + ", mEnabled="
                + mEnabled
                + '}';
    }

    /**
     * Data for an action supported from a safety source status {@link SafetySourceStatus} in the
     * Safety Center page.
     *
     * <p>The purpose of the action is to add a surface to allow the user to perform an action
     * relating to the safety source status.
     *
     * <p>The action will be shown as a clickable icon chosen from a predefined set of icons (see
     * {@link IconType}). The icon should indicate to the user what action will be performed on
     * clicking on it.
     */
    public static final class IconAction implements Parcelable {

        @NonNull
        public static final Creator<IconAction> CREATOR =
                new Creator<IconAction>() {
                    @Override
                    public IconAction createFromParcel(Parcel in) {
                        int iconType = in.readInt();
                        PendingIntent pendingIntent = in.readTypedObject(PendingIntent.CREATOR);
                        return new IconAction(iconType, pendingIntent);
                    }

                    @Override
                    public IconAction[] newArray(int size) {
                        return new IconAction[size];
                    }
                };

        /** Indicates a gear (cog) icon. */
        public static final int ICON_TYPE_GEAR = 100;

        /** Indicates an information icon. */
        public static final int ICON_TYPE_INFO = 200;

        /**
         * All possible icons which can be displayed in an {@link IconAction}.
         *
         * @hide
         */
        @IntDef(
                prefix = {"ICON_TYPE_"},
                value = {
                    ICON_TYPE_GEAR,
                    ICON_TYPE_INFO,
                })
        @Retention(RetentionPolicy.SOURCE)
        public @interface IconType {}

        @IconType private final int mIconType;
        @NonNull private final PendingIntent mPendingIntent;

        public IconAction(@IconType int iconType, @NonNull PendingIntent pendingIntent) {
            this.mIconType = validateIconType(iconType);
            this.mPendingIntent = requireNonNull(pendingIntent);
        }

        /**
         * Returns the type of icon to be displayed in the UI.
         *
         * <p>The icon type should indicate what action will be performed if when invoked.
         */
        @IconType
        public int getIconType() {
            return mIconType;
        }

        /**
         * Returns a {@link PendingIntent} that will start an activity when the icon action is
         * clicked on.
         */
        @NonNull
        public PendingIntent getPendingIntent() {
            return mPendingIntent;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeInt(mIconType);
            dest.writeTypedObject(mPendingIntent, flags);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IconAction)) return false;
            IconAction that = (IconAction) o;
            return mIconType == that.mIconType && mPendingIntent.equals(that.mPendingIntent);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mIconType, mPendingIntent);
        }

        @Override
        public String toString() {
            return "IconAction{"
                    + "mIconType="
                    + mIconType
                    + ", mPendingIntent="
                    + mPendingIntent
                    + '}';
        }

        @IconType
        private static int validateIconType(int value) {
            switch (value) {
                case ICON_TYPE_GEAR:
                case ICON_TYPE_INFO:
                    return value;
                default:
            }
            throw new IllegalArgumentException("Unexpected IconType for IconAction: " + value);
        }
    }

    /** Builder class for {@link SafetySourceStatus}. */
    public static final class Builder {

        @NonNull private final CharSequence mTitle;
        @NonNull private final CharSequence mSummary;
        @SafetySourceData.SeverityLevel private final int mSeverityLevel;

        @Nullable private PendingIntent mPendingIntent;
        @Nullable private IconAction mIconAction;
        private boolean mEnabled = true;

        /** Creates a {@link Builder} for a {@link SafetySourceStatus}. */
        public Builder(
                @NonNull CharSequence title,
                @NonNull CharSequence summary,
                @SafetySourceData.SeverityLevel int severityLevel) {
            this.mTitle = requireNonNull(title);
            this.mSummary = requireNonNull(summary);
            this.mSeverityLevel = validateSeverityLevel(severityLevel);
        }

        /** Creates a {@link Builder} with the values of the given {@link SafetySourceStatus}. */
        @RequiresApi(UPSIDE_DOWN_CAKE)
        public Builder(@NonNull SafetySourceStatus safetySourceStatus) {
            if (!SdkLevel.isAtLeastU()) {
                throw new UnsupportedOperationException(
                        "Method not supported on versions lower than UPSIDE_DOWN_CAKE");
            }
            requireNonNull(safetySourceStatus);
            mTitle = safetySourceStatus.mTitle;
            mSummary = safetySourceStatus.mSummary;
            mSeverityLevel = safetySourceStatus.mSeverityLevel;
            mPendingIntent = safetySourceStatus.mPendingIntent;
            mIconAction = safetySourceStatus.mIconAction;
            mEnabled = safetySourceStatus.mEnabled;
        }

        /**
         * Sets an optional {@link PendingIntent} for the safety source status.
         *
         * <p>The action contained in the {@link PendingIntent} must start an activity.
         *
         * @see #getPendingIntent()
         */
        @NonNull
        public Builder setPendingIntent(@Nullable PendingIntent pendingIntent) {
            checkArgument(
                    pendingIntent == null || pendingIntent.isActivity(),
                    "Safety source status pending intent must start an activity");
            this.mPendingIntent = pendingIntent;
            return this;
        }

        /**
         * Sets an optional {@link IconAction} for the safety source status.
         *
         * @see #getIconAction()
         */
        @NonNull
        public Builder setIconAction(@Nullable IconAction iconAction) {
            this.mIconAction = iconAction;
            return this;
        }

        /**
         * Sets whether the safety source status is enabled.
         *
         * <p>By default, the safety source status will be enabled. If disabled, the status severity
         * level must be set to {@link SafetySourceData#SEVERITY_LEVEL_UNSPECIFIED}.
         *
         * @see #isEnabled()
         */
        @NonNull
        public Builder setEnabled(boolean enabled) {
            checkArgument(
                    enabled || mSeverityLevel == SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED,
                    "Safety source status must have a severity level of "
                            + "SEVERITY_LEVEL_UNSPECIFIED when disabled");
            this.mEnabled = enabled;
            return this;
        }

        /** Creates the {@link SafetySourceStatus} defined by this {@link Builder}. */
        @NonNull
        public SafetySourceStatus build() {
            return new SafetySourceStatus(
                    mTitle, mSummary, mSeverityLevel, mPendingIntent, mIconAction, mEnabled);
        }
    }

    @SafetySourceData.SeverityLevel
    private static int validateSeverityLevel(int value) {
        switch (value) {
            case SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED:
            case SafetySourceData.SEVERITY_LEVEL_INFORMATION:
            case SafetySourceData.SEVERITY_LEVEL_RECOMMENDATION:
            case SafetySourceData.SEVERITY_LEVEL_CRITICAL_WARNING:
                return value;
            default:
        }
        throw new IllegalArgumentException(
                "Unexpected SeverityLevel for SafetySourceStatus: " + value);
    }
}
