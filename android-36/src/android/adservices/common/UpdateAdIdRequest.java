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

package android.adservices.common;

import android.adservices.adid.AdId;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * The request sent from the AdIdProvider to update the AdId in Adservices, when the device updates
 * the AdId.
 *
 * @hide
 */
// TODO(b/300445889): Consider using codegen for Parcelable.
@SystemApi
public final class UpdateAdIdRequest implements Parcelable {
    private final String mAdId;
    private final boolean mLimitAdTrackingEnabled;

    private UpdateAdIdRequest(String adId, boolean isLimitAdTrackingEnabled) {
        mAdId = Objects.requireNonNull(adId);
        mLimitAdTrackingEnabled = isLimitAdTrackingEnabled;
    }

    private UpdateAdIdRequest(Parcel in) {
        this(in.readString(), in.readBoolean());
    }

    @NonNull
    public static final Creator<UpdateAdIdRequest> CREATOR =
            new Parcelable.Creator<>() {
                @Override
                public UpdateAdIdRequest createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new UpdateAdIdRequest(in);
                }

                @Override
                public UpdateAdIdRequest[] newArray(int size) {
                    return new UpdateAdIdRequest[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        Objects.requireNonNull(out);

        out.writeString(mAdId);
        out.writeBoolean(mLimitAdTrackingEnabled);
    }

    /** Returns the advertising ID associated with this result. */
    @NonNull
    public String getAdId() {
        return mAdId;
    }

    /**
     * Returns the Limited Ad Tracking field associated with this result.
     *
     * <p>When Limited Ad Tracking is enabled, it implies the user opts out the usage of {@link
     * AdId}. {@link AdId#ZERO_OUT} will be assigned to the device.
     */
    public boolean isLimitAdTrackingEnabled() {
        return mLimitAdTrackingEnabled;
    }

    // TODO(b/302682607): Investigate encoding AdId in logcat for related AdId classes.
    @Override
    public String toString() {
        return "UpdateAdIdRequest{"
                + "mAdId="
                + mAdId
                + ", mLimitAdTrackingEnabled="
                + mLimitAdTrackingEnabled
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof UpdateAdIdRequest)) {
            return false;
        }

        UpdateAdIdRequest that = (UpdateAdIdRequest) o;

        return Objects.equals(mAdId, that.mAdId)
                && (mLimitAdTrackingEnabled == that.mLimitAdTrackingEnabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAdId, mLimitAdTrackingEnabled);
    }

    /** Builder for {@link UpdateAdIdRequest} objects. */
    public static final class Builder {
        private final String mAdId;
        private boolean mLimitAdTrackingEnabled;

        public Builder(@NonNull String adId) {
            mAdId = Objects.requireNonNull(adId);
        }

        /** Sets the Limited AdTracking enabled field. */
        @NonNull
        public UpdateAdIdRequest.Builder setLimitAdTrackingEnabled(
                boolean isLimitAdTrackingEnabled) {
            mLimitAdTrackingEnabled = isLimitAdTrackingEnabled;
            return this;
        }

        /** Builds a {@link UpdateAdIdRequest} instance. */
        @NonNull
        public UpdateAdIdRequest build() {
            return new UpdateAdIdRequest(mAdId, mLimitAdTrackingEnabled);
        }
    }
}
