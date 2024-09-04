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
import static android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

import static java.util.Objects.requireNonNull;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * A safety event that may trigger a safety source to set its {@link SafetySourceData}.
 *
 * @hide
 */
@SystemApi
@RequiresApi(TIRAMISU)
public final class SafetyEvent implements Parcelable {

    /**
     * Indicates that there has been a change of state for safety source, which may be independent
     * of Safety Center interactions.
     */
    public static final int SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED = 100;

    /**
     * Indicates that the safety source performed a data refresh in response to a request from
     * Safety Center.
     */
    public static final int SAFETY_EVENT_TYPE_REFRESH_REQUESTED = 200;

    /**
     * Indicates that the safety source successfully completed a resolving {@link
     * SafetySourceIssue.Action}.
     */
    public static final int SAFETY_EVENT_TYPE_RESOLVING_ACTION_SUCCEEDED = 300;

    /**
     * Indicates that the safety source failed to complete a resolving {@link
     * SafetySourceIssue.Action}.
     */
    public static final int SAFETY_EVENT_TYPE_RESOLVING_ACTION_FAILED = 400;

    /** Indicates that the device's locale changed. */
    public static final int SAFETY_EVENT_TYPE_DEVICE_LOCALE_CHANGED = 500;

    /** Indicates that the device was rebooted. */
    public static final int SAFETY_EVENT_TYPE_DEVICE_REBOOTED = 600;

    /**
     * Types of safety events that may trigger a set of a safety source's {@link SafetySourceData}.
     *
     * @hide
     */
    @IntDef(
            prefix = {"SAFETY_EVENT_TYPE_"},
            value = {
                SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED,
                SAFETY_EVENT_TYPE_REFRESH_REQUESTED,
                SAFETY_EVENT_TYPE_RESOLVING_ACTION_SUCCEEDED,
                SAFETY_EVENT_TYPE_RESOLVING_ACTION_FAILED,
                SAFETY_EVENT_TYPE_DEVICE_LOCALE_CHANGED,
                SAFETY_EVENT_TYPE_DEVICE_REBOOTED
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {}

    @NonNull
    public static final Creator<SafetyEvent> CREATOR =
            new Creator<SafetyEvent>() {
                @Override
                public SafetyEvent createFromParcel(Parcel in) {
                    int type = in.readInt();
                    return new SafetyEvent.Builder(type)
                            .setRefreshBroadcastId(in.readString())
                            .setSafetySourceIssueId(in.readString())
                            .setSafetySourceIssueActionId(in.readString())
                            .build();
                }

                @Override
                public SafetyEvent[] newArray(int size) {
                    return new SafetyEvent[size];
                }
            };

    @Type private final int mType;
    @Nullable private final String mRefreshBroadcastId;
    @Nullable private final String mSafetySourceIssueId;
    @Nullable private final String mSafetySourceIssueActionId;

    private SafetyEvent(
            @Type int type,
            @Nullable String refreshBroadcastId,
            @Nullable String safetySourceIssueId,
            @Nullable String safetySourceIssueActionId) {
        mType = type;
        mRefreshBroadcastId = refreshBroadcastId;
        mSafetySourceIssueId = safetySourceIssueId;
        mSafetySourceIssueActionId = safetySourceIssueActionId;
    }

    /** Returns the type of the safety event. */
    @Type
    public int getType() {
        return mType;
    }

    /**
     * Returns an optional id provided by Safety Center when requesting a refresh, through {@link
     * SafetyCenterManager#EXTRA_REFRESH_SAFETY_SOURCES_BROADCAST_ID}.
     *
     * <p>This will only be relevant for events of type {@link
     * #SAFETY_EVENT_TYPE_REFRESH_REQUESTED}.
     *
     * @see #getType()
     */
    @Nullable
    public String getRefreshBroadcastId() {
        return mRefreshBroadcastId;
    }

    /**
     * Returns the id of the {@link SafetySourceIssue} this event is associated with (if any).
     *
     * <p>This will only be relevant for events of type {@link
     * #SAFETY_EVENT_TYPE_RESOLVING_ACTION_SUCCEEDED} or {@link
     * #SAFETY_EVENT_TYPE_RESOLVING_ACTION_FAILED}.
     *
     * @see #getType()
     * @see SafetySourceIssue#getId()
     */
    @Nullable
    public String getSafetySourceIssueId() {
        return mSafetySourceIssueId;
    }

    /**
     * Returns the id of the {@link SafetySourceIssue.Action} this event is associated with (if
     * any).
     *
     * <p>This will only be relevant for events of type {@link
     * #SAFETY_EVENT_TYPE_RESOLVING_ACTION_SUCCEEDED} or {@link
     * #SAFETY_EVENT_TYPE_RESOLVING_ACTION_FAILED}.
     *
     * @see #getType()
     * @see SafetySourceIssue.Action#getId()
     */
    @Nullable
    public String getSafetySourceIssueActionId() {
        return mSafetySourceIssueActionId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mType);
        dest.writeString(mRefreshBroadcastId);
        dest.writeString(mSafetySourceIssueId);
        dest.writeString(mSafetySourceIssueActionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetyEvent)) return false;
        SafetyEvent that = (SafetyEvent) o;
        return mType == that.mType
                && Objects.equals(mRefreshBroadcastId, that.mRefreshBroadcastId)
                && Objects.equals(mSafetySourceIssueId, that.mSafetySourceIssueId)
                && Objects.equals(mSafetySourceIssueActionId, that.mSafetySourceIssueActionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mType, mRefreshBroadcastId, mSafetySourceIssueId, mSafetySourceIssueActionId);
    }

    @Override
    public String toString() {
        return "SafetyEvent{"
                + "mType="
                + mType
                + ", mRefreshBroadcastId="
                + mRefreshBroadcastId
                + ", mSafetySourceIssueId="
                + mSafetySourceIssueId
                + ", mSafetySourceIssueActionId="
                + mSafetySourceIssueActionId
                + '}';
    }

    /** Builder class for {@link SafetyEvent}. */
    public static final class Builder {

        @Type private final int mType;
        @Nullable private String mRefreshBroadcastId;
        @Nullable private String mSafetySourceIssueId;
        @Nullable private String mSafetySourceIssueActionId;

        /** Creates a {@link Builder} for {@link SafetyEvent}. */
        public Builder(@Type int type) {
            mType = validateType(type);
        }

        /** Creates a {@link Builder} with the values from the given {@link SafetyEvent}. */
        @RequiresApi(UPSIDE_DOWN_CAKE)
        public Builder(@NonNull SafetyEvent safetyEvent) {
            if (!SdkLevel.isAtLeastU()) {
                throw new UnsupportedOperationException(
                        "Method not supported on versions lower than UPSIDE_DOWN_CAKE");
            }
            requireNonNull(safetyEvent);
            mType = safetyEvent.mType;
            mRefreshBroadcastId = safetyEvent.mRefreshBroadcastId;
            mSafetySourceIssueId = safetyEvent.mSafetySourceIssueId;
            mSafetySourceIssueActionId = safetyEvent.mSafetySourceIssueActionId;
        }

        /**
         * Sets an optional broadcast id provided by Safety Center when requesting a refresh,
         * through {@link SafetyCenterManager#EXTRA_REFRESH_SAFETY_SOURCES_BROADCAST_ID}.
         *
         * <p>This will only be relevant for events of type {@link
         * #SAFETY_EVENT_TYPE_REFRESH_REQUESTED}.
         *
         * @see #getType()
         */
        @NonNull
        public Builder setRefreshBroadcastId(@Nullable String refreshBroadcastId) {
            mRefreshBroadcastId = refreshBroadcastId;
            return this;
        }

        /**
         * Sets the id of the {@link SafetySourceIssue} this event is associated with (if any).
         *
         * <p>This will only be relevant for events of type {@link
         * #SAFETY_EVENT_TYPE_RESOLVING_ACTION_SUCCEEDED} or {@link
         * #SAFETY_EVENT_TYPE_RESOLVING_ACTION_FAILED}.
         *
         * @see #getType()
         * @see SafetySourceIssue#getId()
         */
        @NonNull
        public Builder setSafetySourceIssueId(@Nullable String safetySourceIssueId) {
            mSafetySourceIssueId = safetySourceIssueId;
            return this;
        }

        /**
         * Sets the id of the {@link SafetySourceIssue.Action} this event is associated with (if
         * any).
         *
         * <p>This will only be relevant for events of type {@link
         * #SAFETY_EVENT_TYPE_RESOLVING_ACTION_SUCCEEDED} or {@link
         * #SAFETY_EVENT_TYPE_RESOLVING_ACTION_FAILED}.
         *
         * @see #getType()
         * @see SafetySourceIssue.Action#getId()
         */
        @NonNull
        public Builder setSafetySourceIssueActionId(@Nullable String safetySourceIssueActionId) {
            mSafetySourceIssueActionId = safetySourceIssueActionId;
            return this;
        }

        /** Creates the {@link SafetyEvent} represented by this {@link Builder}. */
        @NonNull
        public SafetyEvent build() {
            switch (mType) {
                case SAFETY_EVENT_TYPE_REFRESH_REQUESTED:
                    if (mRefreshBroadcastId == null) {
                        throw new IllegalArgumentException(
                                "Missing refresh broadcast id for refresh requested safety event");
                    }
                    break;
                case SAFETY_EVENT_TYPE_RESOLVING_ACTION_SUCCEEDED:
                case SAFETY_EVENT_TYPE_RESOLVING_ACTION_FAILED:
                    if (mSafetySourceIssueId == null) {
                        throw new IllegalArgumentException(
                                "Missing issue id for resolving action safety event: " + mType);
                    }
                    if (mSafetySourceIssueActionId == null) {
                        throw new IllegalArgumentException(
                                "Missing issue action id for resolving action safety event: "
                                        + mType);
                    }
                    break;
                case SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED:
                case SAFETY_EVENT_TYPE_DEVICE_LOCALE_CHANGED:
                case SAFETY_EVENT_TYPE_DEVICE_REBOOTED:
                default:
            }
            return new SafetyEvent(
                    mType, mRefreshBroadcastId, mSafetySourceIssueId, mSafetySourceIssueActionId);
        }
    }

    @Type
    private static int validateType(int value) {
        switch (value) {
            case SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED:
            case SAFETY_EVENT_TYPE_REFRESH_REQUESTED:
            case SAFETY_EVENT_TYPE_RESOLVING_ACTION_SUCCEEDED:
            case SAFETY_EVENT_TYPE_RESOLVING_ACTION_FAILED:
            case SAFETY_EVENT_TYPE_DEVICE_LOCALE_CHANGED:
            case SAFETY_EVENT_TYPE_DEVICE_REBOOTED:
                return value;
            default:
        }
        throw new IllegalArgumentException("Unexpected Type for SafetyEvent: " + value);
    }
}
