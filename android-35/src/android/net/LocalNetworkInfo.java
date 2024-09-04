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

package android.net;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;


/**
 * Information about a local network.
 *
 * This is sent to ConnectivityManager.NetworkCallback.
 * @hide
 */
// TODO : make public
public final class LocalNetworkInfo implements Parcelable {
    @Nullable private final Network mUpstreamNetwork;

    public LocalNetworkInfo(@Nullable final Network upstreamNetwork) {
        this.mUpstreamNetwork = upstreamNetwork;
    }

    /**
     * Return the upstream network, or null if none.
     */
    @Nullable
    public Network getUpstreamNetwork() {
        return mUpstreamNetwork;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeParcelable(mUpstreamNetwork, flags);
    }

    @Override
    public String toString() {
        return "LocalNetworkInfo { upstream=" + mUpstreamNetwork + " }";
    }

    public static final @NonNull Creator<LocalNetworkInfo> CREATOR = new Creator<>() {
        public LocalNetworkInfo createFromParcel(Parcel in) {
            final Network upstreamNetwork = in.readParcelable(null);
            return new LocalNetworkInfo(upstreamNetwork);
        }

        @Override
        public LocalNetworkInfo[] newArray(final int size) {
            return new LocalNetworkInfo[size];
        }
    };

    /**
     * Builder for LocalNetworkInfo
     */
    public static final class Builder {
        @Nullable private Network mUpstreamNetwork;

        /**
         * Set the upstream network, or null if none.
         * @return the builder
         */
        @NonNull public Builder setUpstreamNetwork(@Nullable final Network network) {
            mUpstreamNetwork = network;
            return this;
        }

        /**
         * Build the LocalNetworkInfo
         */
        @NonNull public LocalNetworkInfo build() {
            return new LocalNetworkInfo(mUpstreamNetwork);
        }
    }
}
