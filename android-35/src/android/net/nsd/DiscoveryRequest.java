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

package android.net.nsd;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Network;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.Objects;

/**
 * Encapsulates parameters for {@link NsdManager#discoverServices}.
 */
@FlaggedApi(NsdManager.Flags.NSD_SUBTYPES_SUPPORT_ENABLED)
public final class DiscoveryRequest implements Parcelable {
    private final int mProtocolType;

    @NonNull
    private final String mServiceType;

    @Nullable
    private final String mSubtype;

    @Nullable
    private final Network mNetwork;

    // TODO: add mDiscoveryConfig for more fine-grained discovery behavior control

    @NonNull
    public static final Creator<DiscoveryRequest> CREATOR =
            new Creator<>() {
                @Override
                public DiscoveryRequest createFromParcel(Parcel in) {
                    int protocolType = in.readInt();
                    String serviceType = in.readString();
                    String subtype = in.readString();
                    Network network =
                            in.readParcelable(Network.class.getClassLoader(), Network.class);
                    return new DiscoveryRequest(protocolType, serviceType, subtype, network);
                }

                @Override
                public DiscoveryRequest[] newArray(int size) {
                    return new DiscoveryRequest[size];
                }
            };

    private DiscoveryRequest(int protocolType, @NonNull String serviceType,
            @Nullable String subtype, @Nullable Network network) {
        mProtocolType = protocolType;
        mServiceType = serviceType;
        mSubtype = subtype;
        mNetwork = network;
    }

    /**
     * Returns the service type in format of dot-joint string of two labels.
     *
     * For example, "_ipp._tcp" for internet printer and "_matter._tcp" for <a
     * href="https://csa-iot.org/all-solutions/matter">Matter</a> operational device.
     */
    @NonNull
    public String getServiceType() {
        return mServiceType;
    }

    /**
     * Returns the subtype without the trailing "._sub" label or {@code null} if no subtype is
     * specified.
     *
     * For example, the return value will be "_printer" for subtype "_printer._sub".
     */
    @Nullable
    public String getSubtype() {
        return mSubtype;
    }

    /**
     * Returns the service discovery protocol.
     *
     * @hide
     */
    public int getProtocolType() {
        return mProtocolType;
    }

    /**
     * Returns the {@link Network} on which the query should be sent or {@code null} if no
     * network is specified.
     */
    @Nullable
    public Network getNetwork() {
        return mNetwork;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(", protocolType: ").append(mProtocolType)
            .append(", serviceType: ").append(mServiceType)
            .append(", subtype: ").append(mSubtype)
            .append(", network: ").append(mNetwork);
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof DiscoveryRequest)) {
            return false;
        } else {
            DiscoveryRequest otherRequest = (DiscoveryRequest) other;
            return mProtocolType == otherRequest.mProtocolType
                    && Objects.equals(mServiceType, otherRequest.mServiceType)
                    && Objects.equals(mSubtype, otherRequest.mSubtype)
                    && Objects.equals(mNetwork, otherRequest.mNetwork);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(mProtocolType, mServiceType, mSubtype, mNetwork);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mProtocolType);
        dest.writeString(mServiceType);
        dest.writeString(mSubtype);
        dest.writeParcelable(mNetwork, flags);
    }

    /** The builder for creating new {@link DiscoveryRequest} objects. */
    public static final class Builder {
        private final int mProtocolType;

        @NonNull
        private String mServiceType;

        @Nullable
        private String mSubtype;

        @Nullable
        private Network mNetwork;

        /**
         * Creates a new default {@link Builder} object with given service type.
         *
         * @throws IllegalArgumentException if {@code serviceType} is {@code null} or an empty
         * string
         */
        public Builder(@NonNull String serviceType) {
            this(NsdManager.PROTOCOL_DNS_SD, serviceType);
        }

        /** @hide */
        public Builder(int protocolType, @NonNull String serviceType) {
            NsdManager.checkProtocol(protocolType);
            mProtocolType = protocolType;
            setServiceType(serviceType);
        }

        /**
         * Sets the service type to be discovered or {@code null} if no services should be queried.
         *
         * The {@code serviceType} must be a dot-joint string of two labels. For example,
         * "_ipp._tcp" for internet printer. Additionally, the first label must start with
         * underscore ('_') and the second label must be either "_udp" or "_tcp". Otherwise, {@link
         * NsdManager#discoverServices} will fail with {@link NsdManager#FAILURE_BAD_PARAMETER}.
         *
         * @throws IllegalArgumentException if {@code serviceType} is {@code null} or an empty
         * string
         *
         * @hide
         */
        @NonNull
        public Builder setServiceType(@NonNull String serviceType) {
            if (TextUtils.isEmpty(serviceType)) {
                throw new IllegalArgumentException("Service type cannot be empty");
            }
            mServiceType = serviceType;
            return this;
        }

        /**
         * Sets the optional subtype of the services to be discovered.
         *
         * If a non-empty {@code subtype} is specified, it must start with underscore ('_') and
         * have the trailing "._sub" removed. Otherwise, {@link NsdManager#discoverServices} will
         * fail with {@link NsdManager#FAILURE_BAD_PARAMETER}. For example, {@code subtype} should
         * be "_printer" for DNS name "_printer._sub._http._tcp". In this case, only services with
         * this {@code subtype} will be queried, rather than all services of the base service type.
         *
         * Note that a non-empty service type must be specified with {@link #setServiceType} if a
         * non-empty subtype is specified by this method.
         */
        @NonNull
        public Builder setSubtype(@Nullable String subtype) {
            mSubtype = subtype;
            return this;
        }

        /**
         * Sets the {@link Network} on which the discovery queries should be sent.
         *
         * @param network the discovery network or {@code null} if the query should be sent on
         * all supported networks
         */
        @NonNull
        public Builder setNetwork(@Nullable Network network) {
            mNetwork = network;
            return this;
        }

        /**
         * Creates a new {@link DiscoveryRequest} object.
         */
        @NonNull
        public DiscoveryRequest build() {
            return new DiscoveryRequest(mProtocolType, mServiceType, mSubtype, mNetwork);
        }
    }
}
