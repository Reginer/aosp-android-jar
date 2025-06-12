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

package android.net.wifi.p2p.nsd;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresApi;
import android.annotation.Size;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Objects;

/**
 * A class for creating a USD based service discovery configuration for use with
 * {@link WifiP2pServiceInfo}.<br> or {@link WifiP2pServiceRequest}.<br>
 * For the details of the configuration, refer Wi-Fi Alliance Wi-Fi Direct R2 specification
 * - Appendix H - Unsynchronized Service Discovery (as defined in Wi-Fi Aware) and section
 * 4.2.13 USD frame format.
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@FlaggedApi(Flags.FLAG_WIFI_DIRECT_R2)
public final class WifiP2pUsdBasedServiceConfig implements Parcelable {
    /** Maximum allowed length of service specific information */
    private static final int SERVICE_SPECIFIC_INFO_MAXIMUM_LENGTH = 1024;

    /** Bonjour service protocol type */
    public static final int SERVICE_PROTOCOL_TYPE_BONJOUR = 1;

    /** Generic service protocol type */
    public static final int SERVICE_PROTOCOL_TYPE_GENERIC = 2;

    /**
     * Currently for Wi-Fi Direct R2, status codes are defined in Wi-Fi Direct R2 specification
     * (Table 129).
     * @hide
     */
    @IntDef(flag = false, prefix = { "SERVICE_PROTOCOL_TYPE_" }, value = {
            SERVICE_PROTOCOL_TYPE_BONJOUR,
            SERVICE_PROTOCOL_TYPE_GENERIC,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ServiceProtocolType {}

    /**
     * Service protocol type.
     */
    private int mServiceProtocolType;
    /**
     * UTF-8 string defining the service.
     */
    private String mServiceName;
    /**
     * Optional Service specific information content determined by the application.
     */
    private byte[] mServiceSpecificInfo;

    /** Get the service protocol type of this USD service configuration. See also
     * {@link Builder#setServiceProtocolType(int)}.
     *
     * @return A non-negative service layer protocol type.
     */
    @IntRange(from = 0, to = 255)
    public int getServiceProtocolType() {
        return mServiceProtocolType;
    }

    /** Get the service name of this USD service configuration. See also
     * {@link Builder}.
     *
     * @return UTF-8 string defining the service.
     */
    @NonNull
    public String getServiceName() {
        return mServiceName;
    }

    /** Get the service specific info of this USD service configuration. See also
     * {@link Builder#setServiceSpecificInfo(byte[])} .
     *
     *
     *  @return A byte-array of service specification information, or null if unset.
     */
    @Nullable
    public byte[] getServiceSpecificInfo() {
        return mServiceSpecificInfo;
    }

    /**
     * Maximum allowed length of service specific information that can be set in the USD service
     * configuration.
     * See also {@link Builder#setServiceSpecificInfo(byte[])}.
     */
    public static int getMaxAllowedServiceSpecificInfoLength() {
        return SERVICE_SPECIFIC_INFO_MAXIMUM_LENGTH;
    }

    /**
     * Generates a string of all the defined elements.
     *
     * @return a compiled string representing all elements
     */
    public String toString() {
        StringBuilder sbuf = new StringBuilder("WifiP2pUsdBasedServiceConfig:");
        sbuf.append("\n Protocol type: ").append(mServiceProtocolType);
        sbuf.append("\n Service name : ").append(mServiceName);
        sbuf.append("\n Service specific info : ").append((mServiceSpecificInfo == null)
                ? "<null>" : Arrays.toString(mServiceSpecificInfo));
        return sbuf.toString();
    }

    /** Implement the Parcelable interface */
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface */
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mServiceProtocolType);
        dest.writeString(mServiceName);
        dest.writeByteArray(mServiceSpecificInfo);
    }

    /** Implement the Parcelable interface */
    @NonNull
    public static final Creator<WifiP2pUsdBasedServiceConfig> CREATOR =
            new Creator<WifiP2pUsdBasedServiceConfig>() {
                public WifiP2pUsdBasedServiceConfig createFromParcel(Parcel in) {
                    WifiP2pUsdBasedServiceConfig config = new WifiP2pUsdBasedServiceConfig();
                    config.mServiceProtocolType = in.readInt();
                    config.mServiceName = in.readString();
                    config.mServiceSpecificInfo = in.createByteArray();
                    return config;
                }

                public WifiP2pUsdBasedServiceConfig[] newArray(int size) {
                    return new WifiP2pUsdBasedServiceConfig[size];
                }
            };

    /**
     * Builder used to build {@link WifiP2pUsdBasedServiceConfig} objects for
     * USD based service discovery and advertisement.
     */
    public static final class Builder {
        /** Maximum allowed length of service name */
        private static final int SERVICE_NAME_MAXIMUM_LENGTH = 100;
        private int mServiceProtocolType = SERVICE_PROTOCOL_TYPE_GENERIC;
        private @NonNull String mServiceName;
        byte[] mServiceSpecificInfo;

        /**
         * Constructor for {@link Builder}.
         *
         * @param serviceName The service name defining the service. The maximum
         *                    allowed length of the service name is 100 characters.
         */
        public Builder(@Size(min = 1) @NonNull String serviceName) {
            Objects.requireNonNull(serviceName, "Service name cannot be null");
            if (TextUtils.isEmpty(serviceName)) {
                throw new IllegalArgumentException("Service name cannot be empty!");
            }
            if (serviceName.length() > SERVICE_NAME_MAXIMUM_LENGTH) {
                throw new IllegalArgumentException("Service name length: " + serviceName.length()
                        + " must be less than " + SERVICE_NAME_MAXIMUM_LENGTH);
            }
            mServiceName = serviceName;
        }


        /**
         * Specify the service discovery protocol type.
         *
         * <p>
         * Optional. {@code SERVICE_PROTOCOL_TYPE_GENERIC} by default.
         *
         * @param serviceProtocolType One of the {@code SERVICE_PROTOCOL_TYPE_*} or a non-negative
         *                            number set by the service layer.
         * @return The builder to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setServiceProtocolType(
                @IntRange(from = 0, to = 255) int serviceProtocolType) {
            if (serviceProtocolType < 0 || serviceProtocolType > 255) {
                throw new IllegalArgumentException(
                        "serviceProtocolType must be between 0-255 (inclusive)");
            }
            mServiceProtocolType = serviceProtocolType;
            return this;
        }

        /**
         * Specify service specific information content determined by the application.
         * <p>
         *     Optional. Empty by default.
         *
         * @param serviceSpecificInfo A byte-array of service-specific information available to the
         *                            application to send additional information. Users must call
         *                            {@link #getMaxAllowedServiceSpecificInfoLength()} method to
         *                            know maximum allowed legth.
         *
         * @return The builder to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setServiceSpecificInfo(
                @Size(min = 1) @Nullable byte[] serviceSpecificInfo) {
            if (serviceSpecificInfo != null
                    && serviceSpecificInfo.length > getMaxAllowedServiceSpecificInfoLength()) {
                throw new IllegalArgumentException("Service specific info length: "
                        + serviceSpecificInfo.length
                        + " must be less than " + getMaxAllowedServiceSpecificInfoLength());
            }
            mServiceSpecificInfo = serviceSpecificInfo;
            return this;
        }

        /**
         * Build {@link WifiP2pUsdBasedServiceConfig} given the current requests made on the
         * builder.
         * @return {@link WifiP2pUsdBasedServiceConfig} constructed based on builder method calls.
         */
        @NonNull
        public WifiP2pUsdBasedServiceConfig build() {
            if (TextUtils.isEmpty(mServiceName)) {
                throw new IllegalStateException(
                        "Service name must be non-empty");
            }
            WifiP2pUsdBasedServiceConfig config = new WifiP2pUsdBasedServiceConfig();
            config.mServiceName = mServiceName;
            config.mServiceProtocolType = mServiceProtocolType;
            config.mServiceSpecificInfo = mServiceSpecificInfo;
            return config;
        }
    }
}
