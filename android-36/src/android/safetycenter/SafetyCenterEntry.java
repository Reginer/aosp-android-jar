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
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * An individual entry in the Safety Center.
 *
 * <p>A {@link SafetyCenterEntry} conveys the current status of an individual safety feature on the
 * device. Entries are present even if they have no associated active issues. In contrast, a {@link
 * SafetyCenterIssue} is ephemeral and disappears when the issue is resolved.
 *
 * <p>Entries link to their corresponding component or an action on it via {@link
 * #getPendingIntent()}.
 *
 * @hide
 */
@SystemApi
@RequiresApi(TIRAMISU)
public final class SafetyCenterEntry implements Parcelable {

    /**
     * Indicates the severity level of this entry is not currently known. This may be because of an
     * error or because some information is missing.
     */
    public static final int ENTRY_SEVERITY_LEVEL_UNKNOWN = 3000;

    /**
     * Indicates this entry does not have a severity level.
     *
     * <p>This is used when the Safety Center has no opinion on the severity of this entry (e.g. a
     * security setting isn't configured, but it's not considered a risk, or for privacy-related
     * entries).
     */
    public static final int ENTRY_SEVERITY_LEVEL_UNSPECIFIED = 3100;

    /** Indicates that there are no problems present with this entry. */
    public static final int ENTRY_SEVERITY_LEVEL_OK = 3200;

    /** Indicates there are safety recommendations for this entry. */
    public static final int ENTRY_SEVERITY_LEVEL_RECOMMENDATION = 3300;

    /** Indicates there are critical safety warnings for this entry. */
    public static final int ENTRY_SEVERITY_LEVEL_CRITICAL_WARNING = 3400;

    /**
     * All possible severity levels for a {@link SafetyCenterEntry}.
     *
     * @hide
     * @see SafetyCenterEntry#getSeverityLevel()
     * @see Builder#setSeverityLevel(int)
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = "ENTRY_SEVERITY_LEVEL_",
            value = {
                ENTRY_SEVERITY_LEVEL_UNKNOWN,
                ENTRY_SEVERITY_LEVEL_UNSPECIFIED,
                ENTRY_SEVERITY_LEVEL_OK,
                ENTRY_SEVERITY_LEVEL_RECOMMENDATION,
                ENTRY_SEVERITY_LEVEL_CRITICAL_WARNING,
            })
    public @interface EntrySeverityLevel {}

    /** Indicates an entry with {@link #ENTRY_SEVERITY_LEVEL_UNSPECIFIED} should not use an icon. */
    public static final int SEVERITY_UNSPECIFIED_ICON_TYPE_NO_ICON = 0;

    /**
     * Indicates an entry with {@link #ENTRY_SEVERITY_LEVEL_UNSPECIFIED} should use the privacy
     * icon, for privacy features.
     */
    public static final int SEVERITY_UNSPECIFIED_ICON_TYPE_PRIVACY = 1;

    /**
     * Indicates an entry with {@link #ENTRY_SEVERITY_LEVEL_UNSPECIFIED} should use an icon
     * indicating it has no current recommendation.
     */
    public static final int SEVERITY_UNSPECIFIED_ICON_TYPE_NO_RECOMMENDATION = 2;

    /**
     * All possible icon types for a {@link SafetyCenterEntry} to use when its severity level is
     * {@link #ENTRY_SEVERITY_LEVEL_UNSPECIFIED}.
     *
     * <p>It is only relevant when the entry's severity level is {@link
     * #ENTRY_SEVERITY_LEVEL_UNSPECIFIED}.
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = "SEVERITY_UNSPECIFIED_ICON_TYPE_",
            value = {
                SEVERITY_UNSPECIFIED_ICON_TYPE_NO_ICON,
                SEVERITY_UNSPECIFIED_ICON_TYPE_PRIVACY,
                SEVERITY_UNSPECIFIED_ICON_TYPE_NO_RECOMMENDATION,
            })
    public @interface SeverityUnspecifiedIconType {}

    @NonNull
    public static final Creator<SafetyCenterEntry> CREATOR =
            new Creator<SafetyCenterEntry>() {
                @Override
                public SafetyCenterEntry createFromParcel(Parcel in) {
                    String id = in.readString();
                    CharSequence title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
                    return new Builder(id, title)
                            .setSummary(TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in))
                            .setSeverityLevel(in.readInt())
                            .setSeverityUnspecifiedIconType(in.readInt())
                            .setEnabled(in.readBoolean())
                            .setPendingIntent(in.readTypedObject(PendingIntent.CREATOR))
                            .setIconAction(in.readTypedObject(IconAction.CREATOR))
                            .build();
                }

                @Override
                public SafetyCenterEntry[] newArray(int size) {
                    return new SafetyCenterEntry[size];
                }
            };

    @NonNull private final String mId;
    @NonNull private final CharSequence mTitle;
    @Nullable private final CharSequence mSummary;
    @EntrySeverityLevel private final int mSeverityLevel;
    @SeverityUnspecifiedIconType private final int mSeverityUnspecifiedIconType;
    private final boolean mEnabled;
    @Nullable private final PendingIntent mPendingIntent;
    @Nullable private final IconAction mIconAction;

    private SafetyCenterEntry(
            @NonNull String id,
            @NonNull CharSequence title,
            @Nullable CharSequence summary,
            @EntrySeverityLevel int severityLevel,
            @SeverityUnspecifiedIconType int severityUnspecifiedIconType,
            boolean enabled,
            @Nullable PendingIntent pendingIntent,
            @Nullable IconAction iconAction) {
        mId = id;
        mTitle = title;
        mSummary = summary;
        mSeverityLevel = severityLevel;
        mSeverityUnspecifiedIconType = severityUnspecifiedIconType;
        mEnabled = enabled;
        mPendingIntent = pendingIntent;
        mIconAction = iconAction;
    }

    /**
     * Returns the encoded string ID which uniquely identifies this entry within the Safety Center
     * on the device for the current user across all profiles and accounts.
     */
    @NonNull
    public String getId() {
        return mId;
    }

    /** Returns the title that describes this entry. */
    @NonNull
    public CharSequence getTitle() {
        return mTitle;
    }

    /** Returns the summary text that describes this entry if present, or {@code null} otherwise. */
    @Nullable
    public CharSequence getSummary() {
        return mSummary;
    }

    /** Returns the {@link EntrySeverityLevel} of this entry. */
    @EntrySeverityLevel
    public int getSeverityLevel() {
        return mSeverityLevel;
    }

    /** Returns the {@link SeverityUnspecifiedIconType} of this entry. */
    @SeverityUnspecifiedIconType
    public int getSeverityUnspecifiedIconType() {
        return mSeverityUnspecifiedIconType;
    }

    /** Returns whether this entry is enabled. */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Returns the optional {@link PendingIntent} to execute when this entry is selected if present,
     * or {@code null} otherwise.
     */
    @Nullable
    public PendingIntent getPendingIntent() {
        return mPendingIntent;
    }

    /**
     * Returns the optional {@link IconAction} for this entry if present, or {@code null} otherwise.
     */
    @Nullable
    public IconAction getIconAction() {
        return mIconAction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetyCenterEntry)) return false;
        SafetyCenterEntry that = (SafetyCenterEntry) o;
        return mSeverityLevel == that.mSeverityLevel
                && mSeverityUnspecifiedIconType == that.mSeverityUnspecifiedIconType
                && mEnabled == that.mEnabled
                && Objects.equals(mId, that.mId)
                && TextUtils.equals(mTitle, that.mTitle)
                && TextUtils.equals(mSummary, that.mSummary)
                && Objects.equals(mPendingIntent, that.mPendingIntent)
                && Objects.equals(mIconAction, that.mIconAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mId,
                mTitle,
                mSummary,
                mSeverityLevel,
                mSeverityUnspecifiedIconType,
                mEnabled,
                mPendingIntent,
                mIconAction);
    }

    @Override
    public String toString() {
        return "SafetyCenterEntry{"
                + "mId="
                + mId
                + ", mTitle="
                + mTitle
                + ", mSummary="
                + mSummary
                + ", mSeverityLevel="
                + mSeverityLevel
                + ", mSeverityUnspecifiedIconType="
                + mSeverityUnspecifiedIconType
                + ", mEnabled="
                + mEnabled
                + ", mPendingIntent="
                + mPendingIntent
                + ", mIconAction="
                + mIconAction
                + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mId);
        TextUtils.writeToParcel(mTitle, dest, flags);
        TextUtils.writeToParcel(mSummary, dest, flags);
        dest.writeInt(mSeverityLevel);
        dest.writeInt(mSeverityUnspecifiedIconType);
        dest.writeBoolean(mEnabled);
        dest.writeTypedObject(mPendingIntent, flags);
        dest.writeTypedObject(mIconAction, flags);
    }

    /** Builder class for {@link SafetyCenterEntry}. */
    public static final class Builder {

        @NonNull private String mId;
        @NonNull private CharSequence mTitle;
        @Nullable private CharSequence mSummary;
        @EntrySeverityLevel private int mSeverityLevel = ENTRY_SEVERITY_LEVEL_UNKNOWN;

        @SeverityUnspecifiedIconType
        private int mSeverityUnspecifiedIconType = SEVERITY_UNSPECIFIED_ICON_TYPE_NO_ICON;

        private boolean mEnabled = true;
        @Nullable private PendingIntent mPendingIntent;
        @Nullable private IconAction mIconAction;

        /**
         * Creates a {@link Builder} for a {@link SafetyCenterEntry}.
         *
         * @param id a unique encoded string ID, see {@link #getId()} for details
         * @param title a title that describes this entry
         */
        public Builder(@NonNull String id, @NonNull CharSequence title) {
            mId = requireNonNull(id);
            mTitle = requireNonNull(title);
        }

        /** Creates a {@link Builder} with the values from the given {@link SafetyCenterEntry}. */
        public Builder(@NonNull SafetyCenterEntry safetyCenterEntry) {
            mId = safetyCenterEntry.mId;
            mTitle = safetyCenterEntry.mTitle;
            mSummary = safetyCenterEntry.mSummary;
            mSeverityLevel = safetyCenterEntry.mSeverityLevel;
            mSeverityUnspecifiedIconType = safetyCenterEntry.mSeverityUnspecifiedIconType;
            mEnabled = safetyCenterEntry.mEnabled;
            mPendingIntent = safetyCenterEntry.mPendingIntent;
            mIconAction = safetyCenterEntry.mIconAction;
        }

        /** Sets the ID for this entry. */
        @NonNull
        public Builder setId(@NonNull String id) {
            mId = requireNonNull(id);
            return this;
        }

        /** Sets the title for this entry. */
        @NonNull
        public Builder setTitle(@NonNull CharSequence title) {
            mTitle = requireNonNull(title);
            return this;
        }

        /** Sets the optional summary text for this entry. */
        @NonNull
        public Builder setSummary(@Nullable CharSequence summary) {
            mSummary = summary;
            return this;
        }

        /**
         * Sets the {@link EntrySeverityLevel} for this entry. Defaults to {@link
         * #ENTRY_SEVERITY_LEVEL_UNKNOWN}.
         */
        @NonNull
        public Builder setSeverityLevel(@EntrySeverityLevel int severityLevel) {
            mSeverityLevel = validateEntrySeverityLevel(severityLevel);
            return this;
        }

        /**
         * Sets the {@link SeverityUnspecifiedIconType} for this entry. Defaults to {@link
         * #SEVERITY_UNSPECIFIED_ICON_TYPE_NO_ICON}.
         */
        @NonNull
        public Builder setSeverityUnspecifiedIconType(
                @SeverityUnspecifiedIconType int severityUnspecifiedIconType) {
            mSeverityUnspecifiedIconType =
                    validateSeverityUnspecifiedIconType(severityUnspecifiedIconType);
            return this;
        }

        /** Sets whether this entry is enabled. Defaults to {@code true}. */
        @NonNull
        public Builder setEnabled(boolean enabled) {
            mEnabled = enabled;
            return this;
        }

        /** Sets the optional {@link PendingIntent} to execute when this entry is selected. */
        @NonNull
        public Builder setPendingIntent(@Nullable PendingIntent pendingIntent) {
            mPendingIntent = pendingIntent;
            return this;
        }

        /** Sets the optional {@link IconAction} for this entry. */
        @NonNull
        public Builder setIconAction(@Nullable IconAction iconAction) {
            mIconAction = iconAction;
            return this;
        }

        /** Sets the optional {@link IconAction} for this entry. */
        @NonNull
        public Builder setIconAction(
                @IconAction.IconActionType int type, @NonNull PendingIntent pendingIntent) {
            mIconAction = new IconAction(type, pendingIntent);
            return this;
        }

        /** Creates the {@link SafetyCenterEntry} defined by this {@link Builder}. */
        @NonNull
        public SafetyCenterEntry build() {
            return new SafetyCenterEntry(
                    mId,
                    mTitle,
                    mSummary,
                    mSeverityLevel,
                    mSeverityUnspecifiedIconType,
                    mEnabled,
                    mPendingIntent,
                    mIconAction);
        }
    }

    /** An optional additional action with an icon for a {@link SafetyCenterEntry}. */
    public static final class IconAction implements Parcelable {

        /** A gear-type icon action, e.g. that links to a settings page for a specific entry. */
        public static final int ICON_ACTION_TYPE_GEAR = 30100;

        /**
         * An info-type icon action, e.g. that displays some additional detailed info about a
         * specific entry.
         */
        public static final int ICON_ACTION_TYPE_INFO = 30200;

        /**
         * All possible icon action types.
         *
         * @hide
         */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                prefix = "ICON_ACTION_TYPE_",
                value = {
                    ICON_ACTION_TYPE_GEAR,
                    ICON_ACTION_TYPE_INFO,
                })
        public @interface IconActionType {}

        @NonNull
        public static final Creator<IconAction> CREATOR =
                new Creator<IconAction>() {
                    @Override
                    public IconAction createFromParcel(Parcel in) {
                        int type = in.readInt();
                        PendingIntent pendingIntent = in.readTypedObject(PendingIntent.CREATOR);
                        return new IconAction(type, pendingIntent);
                    }

                    @Override
                    public IconAction[] newArray(int size) {
                        return new IconAction[size];
                    }
                };

        @IconActionType private final int mType;
        @NonNull private final PendingIntent mPendingIntent;

        /** Creates an icon action for a {@link SafetyCenterEntry}. */
        public IconAction(@IconActionType int type, @NonNull PendingIntent pendingIntent) {
            mType = validateIconActionType(type);
            mPendingIntent = requireNonNull(pendingIntent);
        }

        /** Returns the {@link IconActionType} of this icon action. */
        @IconActionType
        public int getType() {
            return mType;
        }

        /** Returns the {@link PendingIntent} to execute when this icon action is selected. */
        @NonNull
        public PendingIntent getPendingIntent() {
            return mPendingIntent;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IconAction)) return false;
            IconAction that = (IconAction) o;
            return mType == that.mType && Objects.equals(mPendingIntent, that.mPendingIntent);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mType, mPendingIntent);
        }

        @Override
        public String toString() {
            return "IconAction{" + "mType=" + mType + ", mPendingIntent=" + mPendingIntent + '}';
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeInt(mType);
            dest.writeTypedObject(mPendingIntent, flags);
        }

        @IconActionType
        private static int validateIconActionType(int value) {
            switch (value) {
                case ICON_ACTION_TYPE_GEAR:
                case ICON_ACTION_TYPE_INFO:
                    return value;
                default:
            }
            throw new IllegalArgumentException(
                    "Unexpected IconActionType for IconAction: " + value);
        }
    }

    @EntrySeverityLevel
    private static int validateEntrySeverityLevel(int value) {
        switch (value) {
            case ENTRY_SEVERITY_LEVEL_UNKNOWN:
            case ENTRY_SEVERITY_LEVEL_UNSPECIFIED:
            case ENTRY_SEVERITY_LEVEL_OK:
            case ENTRY_SEVERITY_LEVEL_RECOMMENDATION:
            case ENTRY_SEVERITY_LEVEL_CRITICAL_WARNING:
                return value;
            default:
        }
        throw new IllegalArgumentException(
                "Unexpected EntrySeverityLevel for SafetyCenterEntry: " + value);
    }

    @SeverityUnspecifiedIconType
    private static int validateSeverityUnspecifiedIconType(int value) {
        switch (value) {
            case SEVERITY_UNSPECIFIED_ICON_TYPE_NO_ICON:
            case SEVERITY_UNSPECIFIED_ICON_TYPE_PRIVACY:
            case SEVERITY_UNSPECIFIED_ICON_TYPE_NO_RECOMMENDATION:
                return value;
            default:
        }
        throw new IllegalArgumentException(
                "Unexpected SeverityUnspecifiedIconType for SafetyCenterEntry: " + value);
    }
}
