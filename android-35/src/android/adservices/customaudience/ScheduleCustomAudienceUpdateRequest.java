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

import static com.android.adservices.flags.Flags.FLAG_FLEDGE_SCHEDULE_CUSTOM_AUDIENCE_UPDATE_ENABLED;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.net.Uri;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * The request object wrapping the required and optional parameters to schedule a deferred update
 * for Custom Audience on device. Allows AdTechs to provide an Update Uri, and the minimum Delay
 * Time to schedule the update.
 */
@FlaggedApi(FLAG_FLEDGE_SCHEDULE_CUSTOM_AUDIENCE_UPDATE_ENABLED)
public final class ScheduleCustomAudienceUpdateRequest {
    @NonNull private final Uri mUpdateUri;
    @NonNull private final Duration mMinDelay;
    @NonNull private final List<PartialCustomAudience> mPartialCustomAudienceList;

    private ScheduleCustomAudienceUpdateRequest(
            @NonNull ScheduleCustomAudienceUpdateRequest.Builder builder) {
        Objects.requireNonNull(builder);

        this.mUpdateUri = builder.mUpdateUri;
        this.mMinDelay = builder.mMinDelay;
        this.mPartialCustomAudienceList = builder.mPartialCustomAudienceList;
    }

    /** Returns the {@link Uri} from which the Custom Audience is to be fetched */
    @NonNull
    public Uri getUpdateUri() {
        return mUpdateUri;
    }

    /** Returns the {@link Duration} min time duration for which the update is deferred */
    @NonNull
    public Duration getMinDelay() {
        return mMinDelay;
    }

    /**
     * Returns the list of {@link PartialCustomAudience} which are sent along with the request to
     * download the update for Custom Audience
     */
    @NonNull
    public List<PartialCustomAudience> getPartialCustomAudienceList() {
        return mPartialCustomAudienceList;
    }

    /** Returns the hash of {@link ScheduleCustomAudienceUpdateRequest} object's data. */
    @Override
    public int hashCode() {
        return Objects.hash(mUpdateUri, mMinDelay, mPartialCustomAudienceList);
    }

    /**
     * @return {@code true} only if two {@link ScheduleCustomAudienceUpdateRequest} objects contain
     *     the same information.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduleCustomAudienceUpdateRequest)) return false;
        ScheduleCustomAudienceUpdateRequest that = (ScheduleCustomAudienceUpdateRequest) o;
        return mUpdateUri.equals(that.mUpdateUri)
                && mMinDelay.equals(that.mMinDelay)
                && Objects.equals(mPartialCustomAudienceList, that.mPartialCustomAudienceList);
    }

    /**
     * @return a human-readable representation of {@link ScheduleCustomAudienceUpdateRequest}.
     */
    @Override
    public String toString() {
        return "ScheduleCustomAudienceUpdateRequest {"
                + "updateUri="
                + mUpdateUri
                + ", delayTimeMinutes="
                + mMinDelay.toMinutes()
                + ", partialCustomAudienceList="
                + mPartialCustomAudienceList
                + '}';
    }

    /** Builder for {@link ScheduleCustomAudienceUpdateRequest} objects. */
    public static final class Builder {
        @NonNull private Uri mUpdateUri;
        @NonNull private Duration mMinDelay;
        @NonNull private List<PartialCustomAudience> mPartialCustomAudienceList;

        /**
         * Instantiates a {@link ScheduleCustomAudienceUpdateRequest.Builder} with the following
         *
         * @param updateUri from which the update for Custom Audience is to be fetched
         * @param minDelay minimum delay time duration for which the update is to be deferred
         */
        public Builder(
                @NonNull Uri updateUri,
                @NonNull Duration minDelay,
                @NonNull List<PartialCustomAudience> partialCustomAudienceList) {
            Objects.requireNonNull(updateUri);
            Objects.requireNonNull(minDelay);
            Objects.requireNonNull(partialCustomAudienceList);

            this.mUpdateUri = updateUri;
            this.mMinDelay = minDelay;
            this.mPartialCustomAudienceList = partialCustomAudienceList;
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
         * Sets list of Partial Custom Audiences that are sent to the DSP server when making a
         * request to download updates for Custom Audience
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
         * Builds an instance of {@link ScheduleCustomAudienceUpdateRequest}
         *
         * @throws NullPointerException if any of the non-null parameters is null
         */
        @NonNull
        public ScheduleCustomAudienceUpdateRequest build() {
            Objects.requireNonNull(mUpdateUri);
            Objects.requireNonNull(mMinDelay);
            Objects.requireNonNull(mPartialCustomAudienceList);

            return new ScheduleCustomAudienceUpdateRequest(this);
        }
    }
}
