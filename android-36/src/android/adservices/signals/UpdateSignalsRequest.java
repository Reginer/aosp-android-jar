/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.adservices.signals;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.net.Uri;

import com.android.adservices.flags.Flags;

import java.util.Objects;

/**
 * The request object for updateSignals.
 *
 * <p>{@code updateUri} is the only parameter. It represents the URI that the service will reach out
 * to retrieve the signals updates.
 */
@FlaggedApi(Flags.FLAG_PROTECTED_SIGNALS_ENABLED)
public final class UpdateSignalsRequest {
    @NonNull private final Uri mUpdateUri;

    private UpdateSignalsRequest(@NonNull Uri updateUri) {
        Objects.requireNonNull(updateUri, "updateUri must not be null in UpdateSignalsRequest");

        mUpdateUri = updateUri;
    }

    /**
     * @return the {@link Uri} from which the signal updates will be fetched.
     */
    @NonNull
    public Uri getUpdateUri() {
        return mUpdateUri;
    }

    /**
     * @return {@code true} if and only if the other object is {@link UpdateSignalsRequest} with the
     *     same update URI.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UpdateSignalsRequest)) return false;
        UpdateSignalsRequest other = (UpdateSignalsRequest) o;
        return mUpdateUri.equals(other.mUpdateUri);
    }

    /**
     * @return the hash of the {@link UpdateSignalsRequest} object's data.
     */
    @Override
    public int hashCode() {
        return Objects.hash(mUpdateUri);
    }

    /**
     * @return a human-readable representation of {@link UpdateSignalsRequest}.
     */
    @Override
    public String toString() {
        return "UpdateSignalsRequest{" + "updateUri=" + mUpdateUri + '}';
    }

    /** Builder for {@link UpdateSignalsRequest} objects. */
    public static final class Builder {
        @NonNull private Uri mUpdateUri;

        /**
         * Instantiates a {@link Builder} with the {@link Uri} from which the signal updates will be
         * fetched.
         */
        public Builder(@NonNull Uri updateUri) {
            Objects.requireNonNull(updateUri);
            this.mUpdateUri = updateUri;
        }

        /**
         * Sets the {@link Uri} from which the JSON is to be fetched.
         *
         * <p>See {@link #getUpdateUri()} ()} for details.
         */
        @NonNull
        public Builder setUpdateUri(@NonNull Uri updateUri) {
            Objects.requireNonNull(updateUri, "updateUri must not be null in UpdateSignalsRequest");
            this.mUpdateUri = updateUri;
            return this;
        }

        /**
         * Builds an instance of a {@link UpdateSignalsRequest}.
         *
         * @throws NullPointerException if any non-null parameter is null.
         */
        @NonNull
        public UpdateSignalsRequest build() {
            return new UpdateSignalsRequest(mUpdateUri);
        }
    }
}
