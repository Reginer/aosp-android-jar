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
import android.annotation.LongDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.nsd.NsdManager.ProtocolType;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.net.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.Objects;

/**
 * Encapsulates parameters for {@link NsdManager#registerService}.
 */
@FlaggedApi(Flags.FLAG_IPV6_OVER_BLE)
public final class AdvertisingRequest implements Parcelable {

    /**
     * Only update the registration without sending exit and re-announcement.
     * @hide
     */
    public static final long NSD_ADVERTISING_UPDATE_ONLY = 1;

    // TODO: if apps are allowed to set hostnames, the below doc should be updated to mention that
    // passed in hostnames must also be known unique to use this flag.
    /**
     * Skip the probing step when advertising.
     *
     * <p>This must only be used when the service name ({@link NsdServiceInfo#getServiceName()} is
     * known to be unique and cannot possibly be used by any other device on the network.
     */
    public static final long FLAG_SKIP_PROBING = 1 << 1;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @LongDef(flag = true, prefix = {"FLAG_"}, value = {
            FLAG_SKIP_PROBING,
    })
    public @interface AdvertisingFlags {}

    @NonNull
    public static final Creator<AdvertisingRequest> CREATOR =
            new Creator<>() {
                @Override
                public AdvertisingRequest createFromParcel(Parcel in) {
                    final NsdServiceInfo serviceInfo = in.readParcelable(
                            NsdServiceInfo.class.getClassLoader(), NsdServiceInfo.class);
                    final int protocolType = in.readInt();
                    final long advertiseConfig = in.readLong();
                    final long ttlSeconds = in.readLong();
                    final Duration ttl = ttlSeconds < 0 ? null : Duration.ofSeconds(ttlSeconds);
                    return new AdvertisingRequest(serviceInfo, protocolType, advertiseConfig, ttl);
                }

                @Override
                public AdvertisingRequest[] newArray(int size) {
                    return new AdvertisingRequest[size];
                }
            };
    @NonNull
    private final NsdServiceInfo mServiceInfo;
    private final int mProtocolType;
    // Bitmask of @AdvertisingConfig flags. Uses a long to allow 64 possible flags in the future.
    private final long mAdvertisingConfig;

    @Nullable
    private final Duration mTtl;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @LongDef(flag = true, prefix = {"NSD_ADVERTISING"}, value = {
            NSD_ADVERTISING_UPDATE_ONLY,
    })
    @interface AdvertisingConfig {}

    /**
     * The constructor for the advertiseRequest
     */
    private AdvertisingRequest(@NonNull NsdServiceInfo serviceInfo, @ProtocolType int protocolType,
            long advertisingConfig, @NonNull Duration ttl) {
        mServiceInfo = serviceInfo;
        mProtocolType = protocolType;
        mAdvertisingConfig = advertisingConfig;
        mTtl = ttl;
    }

    /**
     * @return the {@link NsdServiceInfo} describing the service to advertise.
     */
    @NonNull
    public NsdServiceInfo getServiceInfo() {
        return mServiceInfo;
    }

    /**
     * @return the service advertisement protocol.
     */
    @ProtocolType
    public int getProtocolType() {
        return mProtocolType;
    }

    /**
     * @return the flags affecting advertising behavior.
     */
    @AdvertisingFlags
    public long getFlags() {
        return mAdvertisingConfig;
    }

    /**
     * Returns the time interval that the resource records may be cached on a DNS resolver.
     *
     * The value will be {@code null} if it's not specified with the {@link #Builder}.
     *
     * @hide
     */
    // @FlaggedApi(NsdManager.Flags.NSD_CUSTOM_TTL_ENABLED)
    @Nullable
    public Duration getTtl() {
        return mTtl;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("serviceInfo: ").append(mServiceInfo)
                .append(", protocolType: ").append(mProtocolType)
                .append(", advertisingConfig: ").append(mAdvertisingConfig)
                .append(", ttl: ").append(mTtl);
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof AdvertisingRequest)) {
            return false;
        } else {
            final AdvertisingRequest otherRequest = (AdvertisingRequest) other;
            return mServiceInfo.equals(otherRequest.mServiceInfo)
                    && mProtocolType == otherRequest.mProtocolType
                    && mAdvertisingConfig == otherRequest.mAdvertisingConfig
                    && Objects.equals(mTtl, otherRequest.mTtl);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(mServiceInfo, mProtocolType, mAdvertisingConfig, mTtl);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(mServiceInfo, flags);
        dest.writeInt(mProtocolType);
        dest.writeLong(mAdvertisingConfig);
        dest.writeLong(mTtl == null ? -1L : mTtl.getSeconds());
    }

    /**
     * A builder for creating new {@link AdvertisingRequest} objects.
     */
    @FlaggedApi(Flags.FLAG_IPV6_OVER_BLE)
    public static final class Builder {
        @NonNull
        private final NsdServiceInfo mServiceInfo;
        private int mProtocolType;
        private long mAdvertisingConfig;
        @Nullable
        private Duration mTtl;

        /**
         * Creates a new {@link Builder} object.
         * @param serviceInfo the {@link NsdServiceInfo} describing the service to advertise.
         * @param protocolType the advertising protocol to use.
         * @hide
         */
        public Builder(@NonNull NsdServiceInfo serviceInfo, @ProtocolType int protocolType) {
            mServiceInfo = serviceInfo;
            mProtocolType = protocolType;
        }

        /**
         * Creates a new {@link Builder} object.
         * @param serviceInfo the {@link NsdServiceInfo} describing the service to advertise.
         */
        public Builder(@NonNull NsdServiceInfo serviceInfo) {
            this(serviceInfo, NsdManager.PROTOCOL_DNS_SD);
        }

        /**
         * Sets advertising configuration flags.
         *
         * @param flags flags to use for advertising.
         */
        @NonNull
        public Builder setFlags(@AdvertisingFlags long flags) {
            mAdvertisingConfig = flags;
            return this;
        }

        /**
         * Sets the time interval that the resource records may be cached on a DNS resolver.
         *
         * If this method is not called or {@code ttl} is {@code null}, default TTL values
         * will be used for the service when it's registered. Otherwise, the {@code ttl}
         * will be used for all resource records of this service.
         *
         * When registering a service, {@link NsdManager#FAILURE_BAD_PARAMETERS} will be returned
         * if {@code ttl} is smaller than 30 seconds.
         *
         * Note: the value after the decimal point (in unit of seconds) will be discarded. For
         * example, {@code 30} seconds will be used when {@code Duration.ofSeconds(30L, 50_000L)}
         * is provided.
         *
         * @param ttl the maximum duration that the DNS resource records will be cached
         *
         * @see AdvertisingRequest#getTtl
         * @hide
         */
        // @FlaggedApi(NsdManager.Flags.NSD_CUSTOM_TTL_ENABLED)
        @NonNull
        public Builder setTtl(@Nullable Duration ttl) {
            if (ttl == null) {
                mTtl = null;
                return this;
            }
            final long ttlSeconds = ttl.getSeconds();
            if (ttlSeconds < 0 || ttlSeconds > 0xffffffffL) {
                throw new IllegalArgumentException(
                        "ttlSeconds exceeds the allowed range (value = " + ttlSeconds
                                + ", allowedRanged = [0, 0xffffffffL])");
            }
            mTtl = Duration.ofSeconds(ttlSeconds);
            return this;
        }

        /**
         * Sets the protocol to use for advertising.
         * @param protocolType the advertising protocol to use.
         */
        @NonNull
        public Builder setProtocolType(@ProtocolType int protocolType) {
            mProtocolType = protocolType;
            return this;
        }

        /** Creates a new {@link AdvertisingRequest} object. */
        @NonNull
        public AdvertisingRequest build() {
            return new AdvertisingRequest(mServiceInfo, mProtocolType, mAdvertisingConfig, mTtl);
        }
    }
}
