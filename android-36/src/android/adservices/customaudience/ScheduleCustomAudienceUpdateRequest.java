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

import static com.android.adservices.flags.Flags.FLAG_FLEDGE_ENABLE_SCHEDULE_CUSTOM_AUDIENCE_DEFAULT_PARTIAL_CUSTOM_AUDIENCES_CONSTRUCTOR;
import static com.android.adservices.flags.Flags.FLAG_FLEDGE_SCHEDULE_CUSTOM_AUDIENCE_UPDATE_ENABLED;

import android.adservices.common.AdServicesOutcomeReceiver;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.net.Uri;

import com.android.internal.util.Preconditions;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * The request object wrapping the required and optional parameters to schedule a deferred update
 * for a buyer ad tech's custom audiences.
 *
 * <p>The on-device caller can specify information in a series of {@link PartialCustomAudience}
 * objects that will be sent to the buyer ad tech's server after a designated minimum delay.
 */
@FlaggedApi(FLAG_FLEDGE_SCHEDULE_CUSTOM_AUDIENCE_UPDATE_ENABLED)
public final class ScheduleCustomAudienceUpdateRequest {
    @NonNull private final Uri mUpdateUri;
    @NonNull private final Duration mMinDelay;
    @NonNull private final List<PartialCustomAudience> mPartialCustomAudienceList;
    private final boolean mShouldReplacePendingUpdates;

    private ScheduleCustomAudienceUpdateRequest(
            @NonNull ScheduleCustomAudienceUpdateRequest.Builder builder) {
        Objects.requireNonNull(builder);

        this.mUpdateUri = builder.mUpdateUri;
        this.mMinDelay = builder.mMinDelay;
        this.mPartialCustomAudienceList = builder.mPartialCustomAudienceList;
        this.mShouldReplacePendingUpdates = builder.mShouldReplacePendingUpdates;
    }

    /**
     * Returns the {@link Uri} from which the update for the buyer's custom audiences will be
     * fetched.
     *
     * <p>The {@link Uri} must use the same HTTPS site as the buyer ad tech's enrolled server.
     */
    @NonNull
    public Uri getUpdateUri() {
        return mUpdateUri;
    }

    /**
     * Returns the minimum {@link Duration} that the update will be deferred before the service
     * fetches updates for the buyer ad tech's custom audiences.
     */
    @NonNull
    public Duration getMinDelay() {
        return mMinDelay;
    }

    /**
     * Returns the list of {@link PartialCustomAudience} objects which are sent along with the
     * request to download the updates for the buyer ad tech's custom audiences.
     */
    @NonNull
    public List<PartialCustomAudience> getPartialCustomAudienceList() {
        return mPartialCustomAudienceList;
    }

    /**
     * Returns {@code true} if any pending scheduled updates should be canceled and replaced with
     * the update detailed in the current {@link ScheduleCustomAudienceUpdateRequest}.
     *
     * <p>If this method returns {@code false} and there are previously requested updates still
     * pending for the same buyer in the same app, a call to {@link
     * CustomAudienceManager#scheduleCustomAudienceUpdate(ScheduleCustomAudienceUpdateRequest,
     * Executor, AdServicesOutcomeReceiver)} with this {@link ScheduleCustomAudienceUpdateRequest}
     * will fail.
     */
    public boolean shouldReplacePendingUpdates() {
        return mShouldReplacePendingUpdates;
    }

    /** Returns the hash of {@link ScheduleCustomAudienceUpdateRequest} object's data. */
    @Override
    public int hashCode() {
        return Objects.hash(
                mUpdateUri, mMinDelay, mPartialCustomAudienceList, mShouldReplacePendingUpdates);
    }

    /**
     * @return {@code true} only if two {@link ScheduleCustomAudienceUpdateRequest} objects contain
     *     the same information.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduleCustomAudienceUpdateRequest that)) return false;
        return mShouldReplacePendingUpdates == that.mShouldReplacePendingUpdates
                && mUpdateUri.equals(that.mUpdateUri)
                && mMinDelay.equals(that.mMinDelay)
                && mPartialCustomAudienceList.equals(that.mPartialCustomAudienceList);
    }

    /**
     * @return a human-readable representation of {@link ScheduleCustomAudienceUpdateRequest}.
     */
    @Override
    public String toString() {
        return "ScheduleCustomAudienceUpdateRequest{"
                + "mUpdateUri="
                + mUpdateUri
                + ", mMinDelay="
                + mMinDelay
                + ", mPartialCustomAudienceList="
                + mPartialCustomAudienceList
                + ", mShouldReplacePendingUpdates="
                + mShouldReplacePendingUpdates
                + '}';
    }

    /** Builder for {@link ScheduleCustomAudienceUpdateRequest} objects. */
    @FlaggedApi(FLAG_FLEDGE_SCHEDULE_CUSTOM_AUDIENCE_UPDATE_ENABLED)
    public static final class Builder {
        @NonNull private Uri mUpdateUri;
        @NonNull private Duration mMinDelay;
        @NonNull private List<PartialCustomAudience> mPartialCustomAudienceList;
        private boolean mShouldReplacePendingUpdates;

        /**
         * Instantiates a builder for a {@link ScheduleCustomAudienceUpdateRequest} object.
         *
         * @param updateUri {@link Uri} of the buyer ad tech's server from which the updates for the
         *     buyer's custom audiences will be fetched
         * @param minDelay Minimum {@link Duration} for which the update should be deferred
         * @param partialCustomAudienceList {@link List} of {@link PartialCustomAudience} objects
         *     which will be sent to the buyer ad tech's server
         * @deprecated use {@link #Builder(Uri, Duration)} instead as {@code
         *     partialCustomAudienceList} is no longer a required parameter. Users can still use
         *     {@link #setPartialCustomAudienceList(List)}
         */
        @Deprecated
        public Builder(
                @NonNull Uri updateUri,
                @NonNull Duration minDelay,
                @NonNull List<PartialCustomAudience> partialCustomAudienceList) {
            setRequiredFields(updateUri, minDelay);
            setPartialCustomAudienceList(partialCustomAudienceList);
        }

        /**
         * Instantiates a builder for a {@link ScheduleCustomAudienceUpdateRequest} object.
         *
         * @param updateUri {@link Uri} of the buyer ad tech's server from which the updates for the
         *     buyer's custom audiences will be fetched
         * @param minDelay Minimum {@link Duration} for which the update should be deferred
         */
        @FlaggedApi(
                FLAG_FLEDGE_ENABLE_SCHEDULE_CUSTOM_AUDIENCE_DEFAULT_PARTIAL_CUSTOM_AUDIENCES_CONSTRUCTOR)
        public Builder(@NonNull Uri updateUri, @NonNull Duration minDelay) {
            setRequiredFields(updateUri, minDelay);
        }

        private void setRequiredFields(@NonNull Uri updateUri, @NonNull Duration minDelay) {
            this.mUpdateUri = Objects.requireNonNull(updateUri, "Update URI must not be null");
            this.mMinDelay = Objects.requireNonNull(minDelay, "Minimum delay must not be null");
            Preconditions.checkArgument(
                    !minDelay.isNegative(), "Minimum delay %d must not be negative", minDelay);
        }

        /**
         * Sets the {@link Uri} from which the update for the buyer's custom audiences will be
         * fetched.
         *
         * <p>The {@link Uri} must use the same HTTPS site as the buyer ad tech's enrolled server.
         *
         * <p>See {@link #getUpdateUri()} for more details.
         */
        @NonNull
        public Builder setUpdateUri(@NonNull Uri updateUri) {
            this.mUpdateUri = Objects.requireNonNull(updateUri, "Update URI must not be null");
            return this;
        }

        /**
         * Sets the minimum {@link Duration} that the update will be deferred before the service
         * fetches updates for the buyer ad tech's custom audiences.
         *
         * <p>This delay must not be a negative {@link Duration}.
         *
         * <p>See {@link #getMinDelay()} for more details.
         */
        @NonNull
        public Builder setMinDelay(@NonNull Duration minDelay) {
            Objects.requireNonNull(minDelay, "Minimum delay must not be null");
            Preconditions.checkArgument(
                    !minDelay.isNegative(), "Minimum delay %d must not be negative", minDelay);
            this.mMinDelay = minDelay;
            return this;
        }

        /**
         * Sets the list of {@link PartialCustomAudience} objects that are sent to the buyer ad
         * tech's server when making a request to download updates for the buyer's custom audiences.
         *
         * <p>See {@link #getPartialCustomAudienceList()} for more details.
         */
        @NonNull
        public Builder setPartialCustomAudienceList(
                @NonNull List<PartialCustomAudience> partialCustomAudienceList) {
            this.mPartialCustomAudienceList =
                    Objects.requireNonNull(
                            partialCustomAudienceList,
                            "Partial custom audience list must not be null");
            return this;
        }

        /**
         * Sets whether any pending scheduled updates should be deleted and replaced with this
         * {@link ScheduleCustomAudienceUpdateRequest}.
         *
         * <p>By default, this setting is {@code false}.
         *
         * <p>See {@link #shouldReplacePendingUpdates()} for more details.
         */
        @NonNull
        public Builder setShouldReplacePendingUpdates(boolean shouldReplacePendingUpdates) {
            this.mShouldReplacePendingUpdates = shouldReplacePendingUpdates;
            return this;
        }

        /** Builds an instance of {@link ScheduleCustomAudienceUpdateRequest}. */
        @NonNull
        public ScheduleCustomAudienceUpdateRequest build() {
            if (Objects.isNull(mPartialCustomAudienceList)) {
                mPartialCustomAudienceList = Collections.emptyList();
            }
            return new ScheduleCustomAudienceUpdateRequest(this);
        }
    }
}
