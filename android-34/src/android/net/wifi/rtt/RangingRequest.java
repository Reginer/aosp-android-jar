/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.net.wifi.rtt;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.net.MacAddress;
import android.net.wifi.ScanResult;
import android.net.wifi.aware.AttachCallback;
import android.net.wifi.aware.DiscoverySessionCallback;
import android.net.wifi.aware.IdentityChangedListener;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.WifiAwareManager;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Defines the ranging request to other devices. The ranging request is built using
 * {@link RangingRequest.Builder}.
 * A ranging request is executed using
 * {@link WifiRttManager#startRanging(RangingRequest, java.util.concurrent.Executor, RangingResultCallback)}.
 * <p>
 * The ranging request is a batch request - specifying a set of devices (specified using
 * {@link RangingRequest.Builder#addAccessPoint(ScanResult)} and
 * {@link RangingRequest.Builder#addAccessPoints(List)}).
 */
public final class RangingRequest implements Parcelable {
    private static final int MAX_PEERS = 10;
    private static final int DEFAULT_RTT_BURST_SIZE = 8;
    private static final int MIN_RTT_BURST_SIZE = 2;
    private static final int MAX_RTT_BURST_SIZE = 31;

    /**
     * Returns the maximum number of peers to range which can be specified in a single {@code
     * RangingRequest}. The limit applies no matter how the peers are added to the request, e.g.
     * through {@link RangingRequest.Builder#addAccessPoint(ScanResult)} or
     * {@link RangingRequest.Builder#addAccessPoints(List)}.
     *
     * @return Maximum number of peers.
     */
    public static int getMaxPeers() {
        return MAX_PEERS;
    }

    /**
     * Returns the default RTT burst size used to determine the average range.
     *
     * @return the RTT burst size used by default
     */
    public static int getDefaultRttBurstSize() {
        return DEFAULT_RTT_BURST_SIZE;
    }

    /**
     * Returns the minimum RTT burst size that can be used to determine a average range.
     *
     * @return the minimum RTT burst size that can be used
     */
    public static int getMinRttBurstSize() {
        return MIN_RTT_BURST_SIZE;
    }

    /**
     * Returns the minimum RTT burst size that can be used to determine a average range.
     *
     * @return the maximum RTT burst size that can be used
     */
    public static int getMaxRttBurstSize() {
        return MAX_RTT_BURST_SIZE;
    }

    /** @hide */
    public final List<ResponderConfig> mRttPeers;

    /** @hide */
    public final int mRttBurstSize;

    /** @hide */
    private RangingRequest(List<ResponderConfig> rttPeers, int rttBurstSize) {
        mRttPeers = rttPeers;
        mRttBurstSize = rttBurstSize;
    }

    /**
     * Returns the list of RTT capable responding peers.
     *
     * @return the list of RTT capable responding peers in a common system representation
     *
     * @hide
     */
    @SystemApi
    @NonNull
    public List<ResponderConfig> getRttResponders() {
        return mRttPeers;
    }

    /**
     * Returns the RTT burst size used to determine the average range.
     *
     * @return the RTT burst size used
     */
    public int getRttBurstSize() {
        return mRttBurstSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(mRttPeers);
        dest.writeInt(mRttBurstSize);
    }

    public static final @android.annotation.NonNull Creator<RangingRequest> CREATOR = new Creator<RangingRequest>() {
        @Override
        public RangingRequest[] newArray(int size) {
            return new RangingRequest[size];
        }

        @Override
        public RangingRequest createFromParcel(Parcel in) {
            return new RangingRequest(in.readArrayList(null), in.readInt());
        }
    };

    /** @hide */
    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ", "RangingRequest: mRttPeers=[", "]");
        for (ResponderConfig rc : mRttPeers) {
            sj.add(rc.toString());
        }
        return sj.toString();
    }

    /** @hide */
    public void enforceValidity(boolean awareSupported) {
        if (mRttPeers.size() > MAX_PEERS) {
            throw new IllegalArgumentException(
                    "Ranging to too many peers requested. Use getMaxPeers() API to get limit.");
        }
        for (ResponderConfig peer: mRttPeers) {
            if (!peer.isValid(awareSupported)) {
                throw new IllegalArgumentException("Invalid Responder specification");
            }
        }
        if (mRttBurstSize < getMinRttBurstSize() || mRttBurstSize > getMaxRttBurstSize()) {
            throw new IllegalArgumentException("RTT burst size is out of range");
        }
    }

    /**
     * Builder class used to construct {@link RangingRequest} objects.
     */
    public static final class Builder {
        private List<ResponderConfig> mRttPeers = new ArrayList<>();
        private int mRttBurstSize = DEFAULT_RTT_BURST_SIZE;

        /**
         * Set the RTT Burst size for the ranging request.
         * <p>
         * If not set, the default RTT burst size given by
         * {@link #getDefaultRttBurstSize()} is used to determine the default value.
         * If set, the value must be in the range {@link #getMinRttBurstSize()} and
         * {@link #getMaxRttBurstSize()} inclusively, or a
         * {@link java.lang.IllegalArgumentException} will be thrown.
         *
         * @param rttBurstSize The number of FTM packets used to estimate a range.
         * @return The builder to facilitate chaining
         * {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setRttBurstSize(int rttBurstSize) {
            if (rttBurstSize < MIN_RTT_BURST_SIZE || rttBurstSize > MAX_RTT_BURST_SIZE) {
                throw new IllegalArgumentException("RTT burst size out of range.");
            }
            mRttBurstSize = rttBurstSize;
            return this;
        }

        /**
         * Add the device specified by the {@link ScanResult} to the list of devices with
         * which to measure range. The total number of peers added to a request cannot exceed the
         * limit specified by {@link #getMaxPeers()}.
         * <p>
         * Two-sided Ranging will be supported if the Access Point supports IEEE 802.11mc, also
         * known as two-sided RTT, and this is determined by the method
         * {@link ScanResult#is80211mcResponder()}. If not supported, one-sided RTT will be
         * performed with no correction for the AP packet turnaround time.
         *
         * @param apInfo Information about an Access Point (AP) obtained in a Scan Result.
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder addAccessPoint(@NonNull ScanResult apInfo) {
            if (apInfo == null) {
                throw new IllegalArgumentException("Null ScanResult!");
            }
            return addResponder(ResponderConfig.fromScanResult(apInfo));
        }

        /**
         * Add the devices specified by the {@link ScanResult}s to the list of devices with
         * which to measure range. The total number of peers added to a request cannot exceed the
         * limit specified by {@link #getMaxPeers()}.
         * <p>
         * Two-sided Ranging will be supported if the Access Point supports IEEE 802.11mc, also
         * known as two-sided RTT, and this is determined by the method
         * {@link ScanResult#is80211mcResponder()}. If not supported, one-sided RTT will be
         * performed with no correction for the AP packet turnaround time.
         *
         * @param apInfos Information about Access Points (APs) obtained in a Scan Result.
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder addAccessPoints(@NonNull List<ScanResult> apInfos) {
            if (apInfos == null) {
                throw new IllegalArgumentException("Null list of ScanResults!");
            }
            for (ScanResult scanResult : apInfos) {
                addAccessPoint(scanResult);
            }
            return this;
        }

        /**
         * Add the Responder device specified by the {@link ResponderConfig} to the list of devices
         * with which to measure range. The total number of peers added to the request cannot exceed
         * the limit specified by {@link #getMaxPeers()}.
         * <p>
         * Two-sided Ranging will be supported if an Access Point supports IEEE 802.11mc, also
         * known as two-sided RTT, and this is specified in the {@link ResponderConfig} builder.
         * If not supported, one-sided RTT will be performed with no correction for
         * the AP packet turnaround time.
         *
         * @param responder Information on the RTT Responder.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder addResponder(@NonNull ResponderConfig responder) {
            if (responder == null) {
                throw new IllegalArgumentException("Null Responder!");
            }

            mRttPeers.add(responder);
            return this;
        }

        /**
         * Add the devices specified by the {@link ResponderConfig}s to the list of devices with
         * which to measure range. The total number of peers added to a request cannot exceed the
         * limit specified by {@link #getMaxPeers()}.
         * <p>
         * Two-sided Ranging will be supported if an Access Point supports IEEE 802.11mc, also
         * known as two-sided RTT, and this is specified in the {@link ResponderConfig} builder.
         * If not supported, one-sided RTT will be performed with no correction for the AP packet
         * turnaround time.
         *
         * @param responders Information representing the set of access points to be ranged
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder addResponders(@NonNull List<ResponderConfig> responders) {
            if (responders == null) {
                throw new IllegalArgumentException("Null list of Responders");
            }
            for (ResponderConfig responder : responders) {
                addResponder(responder);
            }
            return this;
        }

        /**
         * Add the non-802.11mc capable device specified by the {@link ScanResult} to the list of
         * devices with which to measure range. The total number of peers added to a request cannot
         * exceed the limit specified by {@link #getMaxPeers()}.
         * <p>
         * Accurate ranging cannot be supported if the Access Point does not support IEEE 802.11mc,
         * and instead an alternate protocol called one-sided RTT will be used with lower
         * accuracy. Use {@link ScanResult#is80211mcResponder()} to verify the Access Point)s) are
         * not 802.11mc capable.
         * <p>
         * One-sided RTT does not subtract the RTT turnaround time at the Access Point, which can
         * add hundreds of meters to the estimate. With experimentation it is possible to use this
         * information to make a statistical estimate of the range by taking multiple measurements
         * to several Access Points and normalizing the result. For some applications this can be
         * used to improve range estimates based on Receive Signal Strength Indication (RSSI), but
         * will not be as accurate as IEEE 802.11mc (two-sided RTT).
         * <p>
         * Note: one-sided RTT should only be used if you are very familiar with statistical
         * estimation techniques.
         *
         * @param apInfo Information about an Access Point (AP) obtained in a Scan Result
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder addNon80211mcCapableAccessPoint(@NonNull ScanResult apInfo) {
            if (apInfo == null) {
                throw new IllegalArgumentException("Null ScanResult!");
            }
            if (apInfo.is80211mcResponder()) {
                throw new IllegalArgumentException("AP supports the 802.11mc protocol.");
            }
            return addResponder(ResponderConfig.fromScanResult(apInfo));
        }

        /**
         * Add the non-802.11mc capable devices specified by the {@link ScanResult} to the list of
         * devices with which to measure range. The total number of peers added to a request cannot
         * exceed the limit specified by {@link #getMaxPeers()}.
         * <p>
         * Accurate ranging cannot be supported if the Access Point does not support IEEE 802.11mc,
         * and instead an alternate protocol called one-sided RTT will be used with lower
         * accuracy. Use {@link ScanResult#is80211mcResponder()} to verify the Access Point)s) are
         * not 802.11mc capable.
         * <p>
         * One-sided RTT does not subtract the RTT turnaround time at the Access Point, which can
         * add hundreds of meters to the estimate. With experimentation it is possible to use this
         * information to make a statistical estimate of the range by taking multiple measurements
         * to several Access Points and normalizing the result. For some applications this can be
         * used to improve range estimates based on Receive Signal Strength Indication (RSSI), but
         * will not be as accurate as IEEE 802.11mc (two-sided RTT).
         * <p>
         * Note: one-sided RTT should only be used if you are very familiar with statistical
         * estimation techniques.
         *
         * @param apInfos Information about Access Points (APs) obtained in a Scan Result.
         * @return The builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder addNon80211mcCapableAccessPoints(@NonNull List<ScanResult> apInfos) {
            if (apInfos == null) {
                throw new IllegalArgumentException("Null list of ScanResults!");
            }
            for (ScanResult scanResult : apInfos) {
                if (scanResult.is80211mcResponder()) {
                    throw new IllegalArgumentException(
                            "At least one AP supports the 802.11mc protocol.");
                }
                addAccessPoint(scanResult);
            }
            return this;
        }

        /**
         * Add the device specified by the {@code peerMacAddress} to the list of devices with
         * which to measure range.
         * <p>
         * The MAC address may be obtained out-of-band from a peer Wi-Fi Aware device. A Wi-Fi
         * Aware device may obtain its MAC address using the {@link IdentityChangedListener}
         * provided to
         * {@link WifiAwareManager#attach(AttachCallback, IdentityChangedListener, Handler)}.
         * <p>
         * Note: in order to use this API the device must support Wi-Fi Aware
         * {@link android.net.wifi.aware}. The peer device which is being ranged to must be
         * configured to publish a service (with any name) with:
         * <li>Type {@link android.net.wifi.aware.PublishConfig#PUBLISH_TYPE_UNSOLICITED}.
         * <li>Ranging enabled
         * {@link android.net.wifi.aware.PublishConfig.Builder#setRangingEnabled(boolean)}.
         *
         * @param peerMacAddress The MAC address of the Wi-Fi Aware peer.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder addWifiAwarePeer(@NonNull MacAddress peerMacAddress) {
            if (peerMacAddress == null) {
                throw new IllegalArgumentException("Null peer MAC address");
            }
            return addResponder(
                    ResponderConfig.fromWifiAwarePeerMacAddressWithDefaults(peerMacAddress));
        }

        /**
         * Add a device specified by a {@link PeerHandle} to the list of devices with which to
         * measure range.
         * <p>
         * The {@link PeerHandle} may be obtained as part of the Wi-Fi Aware discovery process. E.g.
         * using {@link DiscoverySessionCallback#onServiceDiscovered(PeerHandle, byte[], List)}.
         * <p>
         * Note: in order to use this API the device must support Wi-Fi Aware
         * {@link android.net.wifi.aware}. The requesting device can be either publisher or
         * subscriber in a discovery session. For both requesting device and peer device ranging
         * must be enabled on the discovery session:
         * <li>{@link android.net.wifi.aware.PublishConfig.Builder#setRangingEnabled(boolean)} for
         * publisher.</li>
         * <li>Either {@link android.net.wifi.aware.SubscribeConfig.Builder#setMinDistanceMm(int)}
         * or {@link android.net.wifi.aware.SubscribeConfig.Builder#setMaxDistanceMm(int)} must be
         * set to enable ranging on subscriber </li>
         *
         * @param peerHandle The peer handler of the peer Wi-Fi Aware device.
         * @return The builder, to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         */
        public Builder addWifiAwarePeer(@NonNull PeerHandle peerHandle) {
            if (peerHandle == null) {
                throw new IllegalArgumentException("Null peer handler (identifier)");
            }

            return addResponder(ResponderConfig.fromWifiAwarePeerHandleWithDefaults(peerHandle));
        }


        /**
         * Build {@link RangingRequest} given the current configurations made on the
         * builder.
         */
        public RangingRequest build() {
            return new RangingRequest(mRttPeers, mRttBurstSize);
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof RangingRequest)) {
            return false;
        }

        RangingRequest lhs = (RangingRequest) o;

        return mRttPeers.size() == lhs.mRttPeers.size()
                && mRttPeers.containsAll(lhs.mRttPeers)
                && mRttBurstSize == lhs.mRttBurstSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mRttPeers, mRttBurstSize);
    }
}
