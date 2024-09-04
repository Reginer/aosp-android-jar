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
 * A class to communicate configuration info about a local network through {@link NetworkAgent}.
 * @hide
 */
// TODO : @SystemApi
public final class LocalNetworkConfig implements Parcelable {
    @Nullable
    private final NetworkRequest mUpstreamSelector;

    @NonNull
    private final MulticastRoutingConfig mUpstreamMulticastRoutingConfig;

    @NonNull
    private final MulticastRoutingConfig mDownstreamMulticastRoutingConfig;

    private LocalNetworkConfig(@Nullable final NetworkRequest upstreamSelector,
            @Nullable final MulticastRoutingConfig upstreamConfig,
            @Nullable final MulticastRoutingConfig downstreamConfig) {
        mUpstreamSelector = upstreamSelector;
        if (null != upstreamConfig) {
            mUpstreamMulticastRoutingConfig = upstreamConfig;
        } else {
            mUpstreamMulticastRoutingConfig = MulticastRoutingConfig.CONFIG_FORWARD_NONE;
        }
        if (null != downstreamConfig) {
            mDownstreamMulticastRoutingConfig = downstreamConfig;
        } else {
            mDownstreamMulticastRoutingConfig = MulticastRoutingConfig.CONFIG_FORWARD_NONE;
        }
    }

    /**
     * Get the request choosing which network traffic from this network is forwarded to and from.
     *
     * This may be null if the local network doesn't forward the traffic anywhere.
     */
    @Nullable
    public NetworkRequest getUpstreamSelector() {
        return mUpstreamSelector;
    }

    /**
     * Get the upstream multicast routing config
     */
    @NonNull
    public MulticastRoutingConfig getUpstreamMulticastRoutingConfig() {
        return mUpstreamMulticastRoutingConfig;
    }

    /**
     * Get the downstream multicast routing config
     */
    @NonNull
    public MulticastRoutingConfig getDownstreamMulticastRoutingConfig() {
        return mDownstreamMulticastRoutingConfig;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeParcelable(mUpstreamSelector, flags);
        dest.writeParcelable(mUpstreamMulticastRoutingConfig, flags);
        dest.writeParcelable(mDownstreamMulticastRoutingConfig, flags);
    }

    @Override
    public String toString() {
        return "LocalNetworkConfig{"
                + "UpstreamSelector=" + mUpstreamSelector
                + ", UpstreamMulticastConfig=" + mUpstreamMulticastRoutingConfig
                + ", DownstreamMulticastConfig=" + mDownstreamMulticastRoutingConfig
                + '}';
    }

    public static final @NonNull Creator<LocalNetworkConfig> CREATOR = new Creator<>() {
        public LocalNetworkConfig createFromParcel(Parcel in) {
            final NetworkRequest upstreamSelector = in.readParcelable(null);
            final MulticastRoutingConfig upstreamConfig = in.readParcelable(null);
            final MulticastRoutingConfig downstreamConfig = in.readParcelable(null);
            return new LocalNetworkConfig(
                    upstreamSelector, upstreamConfig, downstreamConfig);
        }

        @Override
        public LocalNetworkConfig[] newArray(final int size) {
            return new LocalNetworkConfig[size];
        }
    };


    public static final class Builder {
        @Nullable
        private NetworkRequest mUpstreamSelector;

        @Nullable
        private MulticastRoutingConfig mUpstreamMulticastRoutingConfig;

        @Nullable
        private MulticastRoutingConfig mDownstreamMulticastRoutingConfig;

        /**
         * Create a Builder
         */
        public Builder() {
        }

        /**
         * Set to choose where this local network should forward its traffic to.
         *
         * The system will automatically choose the best network matching the request as an
         * upstream, and set up forwarding between this local network and the chosen upstream.
         * If no network matches the request, there is no upstream and the traffic is not forwarded.
         * The caller can know when this changes by listening to link properties changes of
         * this network with the {@link android.net.LinkProperties#getForwardedNetwork()} getter.
         *
         * Set this to null if the local network shouldn't be forwarded. Default is null.
         */
        @NonNull
        public Builder setUpstreamSelector(@Nullable NetworkRequest upstreamSelector) {
            mUpstreamSelector = upstreamSelector;
            return this;
        }

        /**
         * Set the upstream multicast routing config.
         *
         * If null, don't route multicast packets upstream. This is equivalent to a
         * MulticastRoutingConfig in mode FORWARD_NONE. The default is null.
         */
        @NonNull
        public Builder setUpstreamMulticastRoutingConfig(@Nullable MulticastRoutingConfig cfg) {
            mUpstreamMulticastRoutingConfig = cfg;
            return this;
        }

        /**
         * Set the downstream multicast routing config.
         *
         * If null, don't route multicast packets downstream. This is equivalent to a
         * MulticastRoutingConfig in mode FORWARD_NONE. The default is null.
         */
        @NonNull
        public Builder setDownstreamMulticastRoutingConfig(@Nullable MulticastRoutingConfig cfg) {
            mDownstreamMulticastRoutingConfig = cfg;
            return this;
        }

        /**
         * Build the LocalNetworkConfig object.
         */
        @NonNull
        public LocalNetworkConfig build() {
            return new LocalNetworkConfig(mUpstreamSelector,
                    mUpstreamMulticastRoutingConfig,
                    mDownstreamMulticastRoutingConfig);
        }
    }
}
