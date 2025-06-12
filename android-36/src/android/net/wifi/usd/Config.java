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
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.net.wifi.aware.TlvBufferUtils;
import android.net.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * USD configuration for publish and subscribe operation. This is the base class and not intended
 * to be created directly.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_USD)
public abstract class Config {
    /** @hide */
    public static final int MAX_NUM_OF_OPERATING_FREQUENCIES = 32;

    /**
     * Transmission type.
     *
     * @hide
     */
    @IntDef({TRANSMISSION_TYPE_UNICAST, TRANSMISSION_TYPE_MULTICAST})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TransmissionType {
    }

    /**
     * A unicast transmission sends data from one device to a single, specific destination device.
     */
    public static final int TRANSMISSION_TYPE_UNICAST = 0;

    /**
     * A multicast transmission sends data from one device to a group of devices on the network
     * simultaneously.
     */
    public static final int TRANSMISSION_TYPE_MULTICAST = 1;

    /**
     * Subscribe type.
     *
     * @hide
     */
    @IntDef({SUBSCRIBE_TYPE_PASSIVE, SUBSCRIBE_TYPE_ACTIVE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SubscribeType {
    }

    /**
     * Defines a passive subscribe session - a subscribe session where subscribe packets are not
     * transmitted over-the-air and the device listens and matches to received publish packets.
     */
    public static final int SUBSCRIBE_TYPE_PASSIVE = 0;

    /**
     * Defines an active subscribe session - a subscribe session where subscribe packets are
     * transmitted over-the-air.
     */
    public static final int SUBSCRIBE_TYPE_ACTIVE = 1;

    /**
     * Service Protocol Type.
     *
     * @hide
     */
    @IntDef({SERVICE_PROTO_TYPE_GENERIC, SERVICE_PROTO_TYPE_CSA_MATTER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ServiceProtoType {
    }

    /**
     * Generic type.
     */
    public static final int SERVICE_PROTO_TYPE_GENERIC = 0;

    /**
     * CSA (Connectivity Standards Alliance) Matter.
     * Note: CSA Matter is an open-source standard for smart home technology that allows devices to
     * work with any Matter-certified ecosystem.
     */
    public static final int SERVICE_PROTO_TYPE_CSA_MATTER = 1;

    private final byte[] mServiceName;
    private final int mTtlSeconds;
    @ServiceProtoType
    private final int mServiceProtoType;
    private final byte[] mTxMatchFilterTlv;
    private final byte[] mRxMatchFilterTlv;
    private final byte[] mServiceSpecificInfo;
    private final int[] mOperatingFrequencies;

    /**
     * @hide
     */
    public Config(@NonNull byte[] serviceName, int ttlSeconds, int serviceProtoType,
            @Nullable byte[] txMatchFilterTlv, @Nullable byte[] rxMatchFilterTlv,
            @Nullable byte[] serviceSpecificInfo, @Nullable int[] operatingFrequencies) {
        mServiceName = serviceName;
        mTtlSeconds = ttlSeconds;
        mServiceProtoType = serviceProtoType;
        mTxMatchFilterTlv = txMatchFilterTlv;
        mRxMatchFilterTlv = rxMatchFilterTlv;
        mServiceSpecificInfo = serviceSpecificInfo;
        mOperatingFrequencies = operatingFrequencies;
    }

    /**
     * Gets the service name of the USD session.
     * <p>
     * The Service Name is a UTF-8 encoded string from 1 to 255 bytes in length.
     * The only acceptable single-byte UTF-8 symbols for a Service Name are alphanumeric
     * values (A-Z, a-z, 0-9), the hyphen ('-'), the period ('.') and the underscore ('_'). All
     * valid multi-byte UTF-8 characters are acceptable in a Service Name.
     *
     * @return service name
     */
    @NonNull
    public byte[] getServiceName() {
        return mServiceName;
    }

    /**
     * Gets the time interval (in seconds) a USD session will be alive. When the TTL is reached the
     * session will be terminated with an event.
     *
     * @return ttl value in seconds
     */
    @IntRange(from = 0)
    public int getTtlSeconds() {
        return mTtlSeconds;
    }

    /**
     * Get the Service protocol type for the USD session.
     *
     * @return service protocol type as defined in {@code SERVICE_PROTOCOL_TYPE_*}
     */
    @ServiceProtoType
    public int getServiceProtoType() {
        return mServiceProtoType;
    }

    /**
     * Gets the Tx filter which is an ordered sequence of (length, value) pairs to be included in
     * the USD discovery frame.
     *
     * @return tx match filter or empty list
     */
    @NonNull
    public List<byte[]> getTxMatchFilter() {
        return new TlvBufferUtils.TlvIterable(0, 1, mTxMatchFilterTlv).toList();
    }

    /**
     * @return tx match filter in TLV format
     * @hide
     */
    @Nullable
    public byte[] getTxMatchFilterTlv() {
        return mTxMatchFilterTlv;
    }

    /**
     * Gets the Rx match filter, which is an ordered sequence of (length, value) pairs that specify
     * further the response conditions beyond the service name used to filter subscribe messages.
     *
     * @return rx match filter or empty list
     */
    @NonNull
    public List<byte[]> getRxMatchFilter() {
        return new TlvBufferUtils.TlvIterable(0, 1, mRxMatchFilterTlv).toList();
    }

    /**
     * @return receive match filter in TLV format.
     * @hide
     */
    @Nullable
    public byte[] getRxMatchFilterTlv() {
        return mRxMatchFilterTlv;
    }

    /**
     * Get the service specific information set for the USD session.
     *
     * @return byte array or null
     */
    @Nullable
    public byte[] getServiceSpecificInfo() {
        return mServiceSpecificInfo;
    }

    /**
     * Get the frequencies where the USD session operates if overridden by {@code
     * setOperatingFrequenciesMhz(int[])}. If null, the application has not set the operating
     * frequencies using {@link PublishConfig.Builder#setOperatingFrequenciesMhz(int[])} for the
     * publisher or {@link SubscribeConfig.Builder#setOperatingFrequenciesMhz(int[])} for the
     * subscriber.
     *
     * <p>If the operating frequencies are not set the default behavior for the publisher and
     * subscriber is,
     * <ul>
     * <li>The publisher defaults to channel 6 (in the 2.4 GHz band) and a list of allowed channels
     * in the 2.4 GHz and 5 GHz bands for multichannel publishing. Publisher may prioritize the
     * channel with Access Points having best RSSI.
     * <li>The subscriber defaults to either channel 6 (in the 2.4 Ghz band) or Station channel or
     * pick a channel from
     * {@link SubscribeConfig.Builder#setRecommendedOperatingFrequenciesMhz(int[])} in given order
     * of preference.
     * </ul>
     *
     * @return an array of frequencies or null
     */
    @Nullable
    public int[] getOperatingFrequenciesMhz() {
        return mOperatingFrequencies;
    }

    @Override
    public String toString() {
        return "Config{" + "mServiceName=" + Arrays.toString(mServiceName) + ", mTtlSeconds="
                + mTtlSeconds + ", mServiceProtoType=" + mServiceProtoType + ", mTxMatchFilterTlv="
                + Arrays.toString(mTxMatchFilterTlv) + ", mRxMatchFilterTlv=" + Arrays.toString(
                mRxMatchFilterTlv) + ", mServiceSpecificInfo=" + Arrays.toString(
                mServiceSpecificInfo) + ", mOperatingFrequencies=" + Arrays.toString(
                mOperatingFrequencies) + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Config config)) return false;
        return mTtlSeconds == config.mTtlSeconds && mServiceProtoType == config.mServiceProtoType
                && Arrays.equals(mServiceName, config.mServiceName)
                && Arrays.equals(mTxMatchFilterTlv, config.mTxMatchFilterTlv)
                && Arrays.equals(mRxMatchFilterTlv, config.mRxMatchFilterTlv)
                && Arrays.equals(mServiceSpecificInfo, config.mServiceSpecificInfo)
                && Arrays.equals(mOperatingFrequencies, config.mOperatingFrequencies);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mTtlSeconds, mServiceProtoType);
        result = 31 * result + Arrays.hashCode(mServiceName);
        result = 31 * result + Arrays.hashCode(mTxMatchFilterTlv);
        result = 31 * result + Arrays.hashCode(mRxMatchFilterTlv);
        result = 31 * result + Arrays.hashCode(mServiceSpecificInfo);
        result = 31 * result + Arrays.hashCode(mOperatingFrequencies);
        return result;
    }
}
