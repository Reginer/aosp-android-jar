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

package android.adservices.customaudience;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.util.Objects;

/**
 * The request object to join a custom audience.
 */
public class JoinCustomAudienceRequest {
    @NonNull
    private final CustomAudience mCustomAudience;

    private JoinCustomAudienceRequest(@NonNull JoinCustomAudienceRequest.Builder builder) {
        mCustomAudience = builder.mCustomAudience;
    }

    /**
     * Returns the custom audience to join.
     */
    @NonNull
    public CustomAudience getCustomAudience() {
        return mCustomAudience;
    }

    /**
     * Checks whether two {@link JoinCustomAudienceRequest} objects contain the same information.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JoinCustomAudienceRequest)) return false;
        JoinCustomAudienceRequest that = (JoinCustomAudienceRequest) o;
        return mCustomAudience.equals(that.mCustomAudience);
    }

    /**
     * Returns the hash of the {@link JoinCustomAudienceRequest} object's data.
     */
    @Override
    public int hashCode() {
        return Objects.hash(mCustomAudience);
    }

    /** Builder for {@link JoinCustomAudienceRequest} objects. */
    public static final class Builder {
        @Nullable private CustomAudience mCustomAudience;

        public Builder() {
        }

        /**
         * Sets the custom audience to join.
         *
         * <p>See {@link #getCustomAudience()} for more information.
         */
        @NonNull
        public JoinCustomAudienceRequest.Builder setCustomAudience(
                @NonNull CustomAudience customAudience) {
            Objects.requireNonNull(customAudience);
            mCustomAudience = customAudience;
            return this;
        }

        /**
         * Builds an instance of a {@link JoinCustomAudienceRequest}.
         *
         * @throws NullPointerException if any non-null parameter is null
         */
        @NonNull
        public JoinCustomAudienceRequest build() {
            Objects.requireNonNull(mCustomAudience);

            return new JoinCustomAudienceRequest(this);
        }
    }
}
