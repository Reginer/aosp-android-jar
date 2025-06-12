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

package android.net.wifi.usd;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.net.wifi.flags.Flags;

/**
 * A class providing information about a USD discovery session with a specific peer.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_USD)
public class DiscoveryResult {
    private final int mPeerId;
    private final byte[] mServiceSpecificInfo;
    @Config.ServiceProtoType
    private final int mServiceProtoType;
    private final boolean mIsFsdEnabled;

    private DiscoveryResult(Builder builder) {
        mPeerId = builder.mPeerId;
        mServiceSpecificInfo = builder.mServiceSpecificInfo;
        mServiceProtoType = builder.mServiceProtoType;
        mIsFsdEnabled = builder.mIsFsdEnabled;
    }

    /**
     * Get the peer id.
     */
    public int getPeerId() {
        return mPeerId;
    }

    /**
     * Get the service specific info from the peer. If null, service discovery is without service
     * specific info.
     */
    @Nullable
    public byte[] getServiceSpecificInfo() {
        return mServiceSpecificInfo;
    }

    /**
     * Get service specific protocol type {@code (SERVICE_PROTO_TYPE_*)}.
     */
    @Config.ServiceProtoType
    public int getServiceProtoType() {
        return mServiceProtoType;
    }

    /**
     * Return whether Further Service Discovery (FSD) is enabled or not.
     */
    public boolean isFsdEnabled() {
        return mIsFsdEnabled;
    }

    /**
     * {@code DiscoveryResult} builder static inner class.
     */
    @FlaggedApi(Flags.FLAG_USD)
    public static final class Builder {
        private final int mPeerId;
        private byte[] mServiceSpecificInfo;
        private int mServiceProtoType;
        private boolean mIsFsdEnabled;

        /**
         * Builder constructor.
         *
         * @param peerId an id of the peer
         */
        public Builder(int peerId) {
            mPeerId = peerId;
        }


        /**
         * Sets the service specific information and returns a reference to this Builder enabling
         * method chaining.
         *
         * @param serviceSpecificInfo the {@code serviceSpecificInfo} to set
         * @return a reference to this Builder
         */
        @NonNull
        public Builder setServiceSpecificInfo(@NonNull byte[] serviceSpecificInfo) {
            this.mServiceSpecificInfo = serviceSpecificInfo;
            return this;
        }

        /**
         * Sets the service protocol type and returns a reference to this Builder enabling method
         * chaining.
         *
         * @param serviceProtoType the {@code serviceProtoType} to set
         * @return a reference to this Builder
         */
        @NonNull
        public Builder setServiceProtoType(@Config.ServiceProtoType int serviceProtoType) {
            this.mServiceProtoType = serviceProtoType;
            return this;
        }

        /**
         * Sets whether Further Service Discovery (FSD) is enabled or not and returns a reference
         * to this Builder enabling method chaining.
         *
         * @param isFsdEnabled the {@code isFsdEnabled} to set
         * @return a reference to this Builder
         */
        @NonNull
        public Builder setFsdEnabled(boolean isFsdEnabled) {
            this.mIsFsdEnabled = isFsdEnabled;
            return this;
        }

        /**
         * Returns a {@code DiscoveryResult} built from the parameters previously set.
         *
         * @return a {@code DiscoveryResult} built with parameters of this {@code DiscoveryResult
         * .Builder}
         */
        @NonNull
        public DiscoveryResult build() {
            return new DiscoveryResult(this);
        }
    }
}
