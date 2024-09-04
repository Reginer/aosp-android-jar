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

package android.nearby;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * A class that can describe what offload functions are available.
 *
 * @hide
 */
@SystemApi
public final class OffloadCapability implements Parcelable {
    private final boolean mFastPairSupported;
    private final boolean mNearbyShareSupported;
    private final long mVersion;

    public boolean isFastPairSupported() {
        return mFastPairSupported;
    }

    public boolean isNearbyShareSupported() {
        return mNearbyShareSupported;
    }

    public long getVersion() {
        return mVersion;
    }


    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeBoolean(mFastPairSupported);
        dest.writeBoolean(mNearbyShareSupported);
        dest.writeLong(mVersion);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<OffloadCapability> CREATOR = new Creator<OffloadCapability>() {
        @Override
        public OffloadCapability createFromParcel(Parcel in) {
            boolean isFastPairSupported = in.readBoolean();
            boolean isNearbyShareSupported = in.readBoolean();
            long version = in.readLong();
            return new Builder()
                    .setFastPairSupported(isFastPairSupported)
                    .setNearbyShareSupported(isNearbyShareSupported)
                    .setVersion(version)
                    .build();
        }

        @Override
        public OffloadCapability[] newArray(int size) {
            return new OffloadCapability[size];
        }
    };

    private OffloadCapability(boolean fastPairSupported, boolean nearbyShareSupported,
            long version) {
        mFastPairSupported = fastPairSupported;
        mNearbyShareSupported = nearbyShareSupported;
        mVersion = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OffloadCapability)) return false;
        OffloadCapability that = (OffloadCapability) o;
        return isFastPairSupported() == that.isFastPairSupported()
                && isNearbyShareSupported() == that.isNearbyShareSupported()
                && getVersion() == that.getVersion();
    }

    @Override
    public int hashCode() {
        return Objects.hash(isFastPairSupported(), isNearbyShareSupported(), getVersion());
    }

    @Override
    public String toString() {
        return "OffloadCapability{"
                + "fastPairSupported=" + mFastPairSupported
                + ", nearbyShareSupported=" + mNearbyShareSupported
                + ", version=" + mVersion
                + '}';
    }

    /**
     * Builder class for {@link OffloadCapability}.
     */
    public static final class Builder {
        private boolean mFastPairSupported;
        private boolean mNearbyShareSupported;
        private long mVersion;

        /**
         * Sets if the Nearby Share feature is supported
         *
         * @param fastPairSupported {@code true} if the Fast Pair feature is supported
         */
        @NonNull
        public Builder setFastPairSupported(boolean fastPairSupported) {
            mFastPairSupported = fastPairSupported;
            return this;
        }

        /**
         * Sets if the Nearby Share feature is supported.
         *
         * @param nearbyShareSupported {@code true} if the Nearby Share feature is supported
         */
        @NonNull
        public Builder setNearbyShareSupported(boolean nearbyShareSupported) {
            mNearbyShareSupported = nearbyShareSupported;
            return this;
        }

        /**
         * Sets the version number of Nearby Offload.
         *
         * @param version Nearby Offload version number
         */
        @NonNull
        public Builder setVersion(long version) {
            mVersion = version;
            return this;
        }

        /**
         * Builds an OffloadCapability object.
         */
        @NonNull
        public OffloadCapability build() {
            return new OffloadCapability(mFastPairSupported, mNearbyShareSupported, mVersion);
        }
    }
}
