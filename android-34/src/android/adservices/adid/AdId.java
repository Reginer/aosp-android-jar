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
package android.adservices.adid;

import android.annotation.NonNull;

import java.util.Objects;

/**
 * A unique, user-resettable, device-wide, per-profile ID for advertising.
 *
 * <p>Ad networks may use {@code AdId} to monetize for Interest Based Advertising (IBA), i.e.
 * targeting and remarketing ads. The user may limit availability of this identifier.
 *
 * @see AdIdManager#getAdId(Executor, OutcomeReceiver)
 */
public class AdId {
    @NonNull private final String mAdId;
    private final boolean mLimitAdTrackingEnabled;

    /**
     * A zeroed-out {@link #getAdId ad id} that is returned when the user has {@link
     * #isLimitAdTrackingEnabled limited ad tracking}.
     */
    public static final String ZERO_OUT = "00000000-0000-0000-0000-000000000000";

    /**
     * Creates an instance of {@link AdId}
     *
     * @param adId obtained from the provider service.
     * @param limitAdTrackingEnabled value from the provider service which determines the value of
     *     adId.
     */
    public AdId(@NonNull String adId, boolean limitAdTrackingEnabled) {
        mAdId = adId;
        mLimitAdTrackingEnabled = limitAdTrackingEnabled;
    }

    /**
     * The advertising ID.
     *
     * <p>The value of advertising Id depends on a combination of {@link
     * #isLimitAdTrackingEnabled()} and {@link
     * android.adservices.common.AdServicesPermissions#ACCESS_ADSERVICES_AD_ID}.
     *
     * <p>When the user is {@link #isLimitAdTrackingEnabled limiting ad tracking}, the API returns
     * {@link #ZERO_OUT}. This disallows a caller to track the user for monetization purposes.
     *
     * <p>Otherwise, a string unique to the device and user is returned, which can be used to track
     * users for advertising.
     */
    public @NonNull String getAdId() {
        return mAdId;
    }

    /**
     * Retrieves the limit ad tracking enabled setting.
     *
     * <p>This value is true if user has limit ad tracking enabled, {@code false} otherwise.
     */
    public boolean isLimitAdTrackingEnabled() {
        return mLimitAdTrackingEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AdId)) {
            return false;
        }
        AdId that = (AdId) o;
        return mAdId.equals(that.mAdId)
                && (mLimitAdTrackingEnabled == that.mLimitAdTrackingEnabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAdId, mLimitAdTrackingEnabled);
    }
}
