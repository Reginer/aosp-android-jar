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

package android.adservices.customaudience;

import android.annotation.NonNull;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Allows AdTechs to provide an Update Uri, and the minimum Delay Time to schedule the update.
 *
 * <p>Refer to {@link ScheduleCustomAudienceUpdateRequest} for more information about the
 * parameters.
 *
 * @hide
 */
public final class ScheduleCustomAudienceUpdateInput implements Parcelable {
    @NonNull private final Uri mUpdateUri;
    @NonNull private final String mCallerPackageName;
    @NonNull private final Duration mMinDelay;
    @NonNull private final List<PartialCustomAudience> mPartialCustomAudienceList;
    private final boolean mShouldReplacePendingUpdates;

    @NonNull
    public static final Creator<ScheduleCustomAudienceUpdateInput> CREATOR =
            new Creator<ScheduleCustomAudienceUpdateInput>() {
                @NonNull
                @Override
                public ScheduleCustomAudienceUpdateInput createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new ScheduleCustomAudienceUpdateInput(in);
                }

                @NonNull
                @Override
                public ScheduleCustomAudienceUpdateInput[] newArray(int size) {
                    return new ScheduleCustomAudienceUpdateInput[size];
                }
            };

    private ScheduleCustomAudienceUpdateInput(
            @NonNull ScheduleCustomAudienceUpdateInput.Builder builder) {
        Objects.requireNonNull(builder);

        mUpdateUri = builder.mUpdateUri;
        mCallerPackageName = builder.mCallerPackageName;
        mMinDelay = builder.mMinDelay;
        mPartialCustomAudienceList = builder.mPartialCustomAudienceList;
        mShouldReplacePendingUpdates = builder.mShouldReplacePendingUpdates;
    }

    private ScheduleCustomAudienceUpdateInput(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        mUpdateUri = Uri.CREATOR.createFromParcel(in);
        mCallerPackageName = in.readString();
        mMinDelay = Duration.ofMillis(in.readLong());
        mPartialCustomAudienceList = in.createTypedArrayList(PartialCustomAudience.CREATOR);
        mShouldReplacePendingUpdates = in.readBoolean();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);

        mUpdateUri.writeToParcel(dest, flags);
        dest.writeString(mCallerPackageName);
        dest.writeLong(mMinDelay.toMillis());
        dest.writeTypedList(mPartialCustomAudienceList);
        dest.writeBoolean(mShouldReplacePendingUpdates);
    }

    /** Returns the {@link Uri} from which the Custom Audience is to be fetched */
    @NonNull
    public Uri getUpdateUri() {
        return mUpdateUri;
    }

    /** Returns the caller app's package name. */
    @NonNull
    public String getCallerPackageName() {
        return mCallerPackageName;
    }

    /** Returns the {@link Duration} min time duration for which the update is deferred */
    @NonNull
    public Duration getMinDelay() {
        return mMinDelay;
    }

    /**
     * Returns {@code true} if any pending scheduled updates should be canceled and replaced with
     * the update detailed in the current {@link ScheduleCustomAudienceUpdateInput}.
     */
    public boolean shouldReplacePendingUpdates() {
        return mShouldReplacePendingUpdates;
    }

    /**
     * Returns the list of {@link PartialCustomAudience} which are sent along with the request to
     * download the update for Custom Audience
     */
    @NonNull
    public List<PartialCustomAudience> getPartialCustomAudienceList() {
        return mPartialCustomAudienceList;
    }


    /** Returns the hash of {@link ScheduleCustomAudienceUpdateInput} object's data. */
    @Override
    public int hashCode() {
        return Objects.hash(mUpdateUri, mCallerPackageName, mMinDelay, mPartialCustomAudienceList);
    }


    /**
     * @return {@code true} only if two {@link ScheduleCustomAudienceUpdateInput} objects contain
     *     the same information.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduleCustomAudienceUpdateInput)) return false;
        ScheduleCustomAudienceUpdateInput that = (ScheduleCustomAudienceUpdateInput) o;
        return mUpdateUri.equals(that.mUpdateUri)
                && mCallerPackageName.equals(that.mCallerPackageName)
                && mMinDelay.equals(that.mMinDelay)
                && Objects.equals(mPartialCustomAudienceList, that.mPartialCustomAudienceList)
                && mShouldReplacePendingUpdates == that.mShouldReplacePendingUpdates;
    }

    /**
     * @return a human-readable representation of {@link ScheduleCustomAudienceUpdateInput}.
     */
    @Override
    public String toString() {
        return "ScheduleCustomAudienceUpdateInput{"
                + "mUpdateUri="
                + mUpdateUri
                + ", mCallerPackageName="
                + mCallerPackageName
                + ", mMinDelay="
                + mMinDelay
                + ", mPartialCustomAudienceList="
                + mPartialCustomAudienceList
                + ", mShouldReplacePendingUpdates="
                + mShouldReplacePendingUpdates
                + '}';
    }


    /** Builder for {@link ScheduleCustomAudienceUpdateInput} objects. */
    public static final class Builder {
        @NonNull private Uri mUpdateUri;
        @NonNull private Duration mMinDelay;
        @NonNull private String mCallerPackageName;
        @NonNull private List<PartialCustomAudience> mPartialCustomAudienceList;
        boolean mShouldReplacePendingUpdates;

        /**
         * Instantiates a {@link ScheduleCustomAudienceUpdateInput.Builder} with the following
         *
         * @param updateUri from which the update for Custom Audience is to be fetched
         * @param callerPackageName the caller app's package name
         * @param minDelay minimum delay time duration for which the update is to be deferred
         * @param partialCustomAudienceList list of partial Custom Audiences that are overridden by
         *     on-device ad tech's SDK on update
         */
        public Builder(
                @NonNull Uri updateUri,
                @NonNull String callerPackageName,
                @NonNull Duration minDelay,
                @NonNull List<PartialCustomAudience> partialCustomAudienceList) {
            Objects.requireNonNull(updateUri);
            Objects.requireNonNull(callerPackageName);
            Objects.requireNonNull(minDelay);
            Objects.requireNonNull(partialCustomAudienceList);

            mUpdateUri = updateUri;
            mCallerPackageName = callerPackageName;
            mMinDelay = minDelay;
            mPartialCustomAudienceList = partialCustomAudienceList;
        }

        /**
         * Sets the {@link Uri} from which the update for Custom Audience is to be fetched
         *
         * <p>See {@link #getUpdateUri()} for details
         */
        @NonNull
        public Builder setUpdateUri(@NonNull Uri updateUri) {
            Objects.requireNonNull(updateUri);
            this.mUpdateUri = updateUri;
            return this;
        }

        /**
         * Sets the caller app's package name.
         *
         * <p>See {@link #getCallerPackageName()} for details.
         */
        @NonNull
        public Builder setCallerPackageName(@NonNull String callerPackageName) {
            Objects.requireNonNull(callerPackageName);
            this.mCallerPackageName = callerPackageName;
            return this;
        }

        /**
         * Sets the {@link Duration} , min time for which the update is to be deferred
         *
         * <p>See {@link #getMinDelay()} for more details
         */
        @NonNull
        public Builder setMinDelay(@NonNull Duration minDelay) {
            Objects.requireNonNull(minDelay);
            this.mMinDelay = minDelay;
            return this;
        }

        /**
         * Sets list of Partial Custom Audiences that are sent to the buyer ad tech server when
         * making a request to download updates for Custom Audience
         *
         * <p>See {@link #getPartialCustomAudienceList()} for more details
         */
        @NonNull
        public Builder setPartialCustomAudienceList(
                @NonNull List<PartialCustomAudience> partialCustomAudiences) {
            this.mPartialCustomAudienceList = partialCustomAudiences;
            return this;
        }

        /**
         * Sets whether any pending scheduled updates should be deleted and replaced with this.
         *
         * <p>See {@link #shouldReplacePendingUpdates()} for more details
         */
        @NonNull
        public Builder setShouldReplacePendingUpdates(boolean shouldReplacePendingUpdates) {
            this.mShouldReplacePendingUpdates = shouldReplacePendingUpdates;
            return this;
        }

        /**
         * Builds an instance of {@link ScheduleCustomAudienceUpdateInput}
         *
         * @throws NullPointerException if any of the non-null parameters is null
         */
        @NonNull
        public ScheduleCustomAudienceUpdateInput build() {
            Objects.requireNonNull(mUpdateUri);
            Objects.requireNonNull(mCallerPackageName);
            Objects.requireNonNull(mMinDelay);
            Objects.requireNonNull(mPartialCustomAudienceList);

            return new ScheduleCustomAudienceUpdateInput(this);
        }
    }
}
