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

import static android.net.wifi.ScanResult.InformationElement.EID_EXTENSION_PRESENT;
import static android.net.wifi.ScanResult.InformationElement.EID_EXT_EHT_CAPABILITIES;
import static android.net.wifi.ScanResult.InformationElement.EID_EXT_HE_CAPABILITIES;
import static android.net.wifi.ScanResult.InformationElement.EID_HT_CAPABILITIES;
import static android.net.wifi.ScanResult.InformationElement.EID_VHT_CAPABILITIES;

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.net.MacAddress;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiAnnotations;
import android.net.wifi.aware.PeerHandle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Defines the configuration of an IEEE 802.11mc Responder. The Responder may be an Access Point
 * (AP), a Wi-Fi Aware device, or a manually configured Responder.
 * <p>
 * A Responder configuration may be constructed from a {@link ScanResult} or manually (with the
 * data obtained out-of-band from a peer).
 */
public final class ResponderConfig implements Parcelable {
    private static final String TAG = "ResponderConfig";
    private static final int AWARE_BAND_2_DISCOVERY_CHANNEL = 2437;

    /** @hide */
    @IntDef({RESPONDER_AP, RESPONDER_STA, RESPONDER_P2P_GO, RESPONDER_P2P_CLIENT, RESPONDER_AWARE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ResponderType {
    }

    /**
     * Responder is an access point(AP).
     */

    public static final int RESPONDER_AP = 0;

    /**
     * Responder is a client device(STA).
     */
    public static final int RESPONDER_STA = 1;

    /**
     * Responder is a Wi-Fi Direct Group Owner (GO).
     * @hide
     */
    @SystemApi
    public static final int RESPONDER_P2P_GO = 2;

    /**
     * Responder is a Wi-Fi Direct Group Client.
     * @hide
     */
    @SystemApi
    public static final int RESPONDER_P2P_CLIENT = 3;

    /**
     * Responder is a Wi-Fi Aware device.
     * @hide
     */
    @SystemApi
    public static final int RESPONDER_AWARE = 4;

    /** @hide */
    @IntDef({
            CHANNEL_WIDTH_20MHZ, CHANNEL_WIDTH_40MHZ, CHANNEL_WIDTH_80MHZ, CHANNEL_WIDTH_160MHZ,
            CHANNEL_WIDTH_80MHZ_PLUS_MHZ, CHANNEL_WIDTH_320MHZ})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ChannelWidth {
    }


    /**
     * Channel bandwidth is 20 MHZ
     * @hide
     */
    @SystemApi
    public static final int CHANNEL_WIDTH_20MHZ = 0;

    /**
     * Channel bandwidth is 40 MHZ
     * @hide
     */
    @SystemApi
    public static final int CHANNEL_WIDTH_40MHZ = 1;

    /**
     * Channel bandwidth is 80 MHZ
     * @hide
     */
    @SystemApi
    public static final int CHANNEL_WIDTH_80MHZ = 2;

    /**
     * Channel bandwidth is 160 MHZ
     * @hide
     */
    @SystemApi
    public static final int CHANNEL_WIDTH_160MHZ = 3;

    /**
     * Channel bandwidth is 160 MHZ, but 80MHZ + 80MHZ
     * @hide
     */
    @SystemApi
    public static final int CHANNEL_WIDTH_80MHZ_PLUS_MHZ = 4;

    /**
     * Channel bandwidth is 320 MHZ
     * @hide
     */
    @SystemApi
    public static final int CHANNEL_WIDTH_320MHZ = 5;


    /** @hide */
    @IntDef({PREAMBLE_LEGACY, PREAMBLE_HT, PREAMBLE_VHT, PREAMBLE_HE, PREAMBLE_EHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PreambleType {
    }

    /**
     * Preamble type: Legacy.
     * @hide
     */
    @SystemApi
    public static final int PREAMBLE_LEGACY = 0;

    /**
     * Preamble type: HT.
     * @hide
     */
    @SystemApi
    public static final int PREAMBLE_HT = 1;

    /**
     * Preamble type: VHT.
     * @hide
     */
    @SystemApi
    public static final int PREAMBLE_VHT = 2;

    /**
     * Preamble type: HE.
     * @hide
     */
    @SystemApi
    public static final int PREAMBLE_HE = 3;

    /**
     * Preamble type: EHT.
     * @hide
     */
    @SystemApi
    public static final int PREAMBLE_EHT = 4;

    /**
     * The MAC address of the Responder. Will be null if a Wi-Fi Aware peer identifier (the
     * peerHandle field) ise used to identify the Responder.
     * @hide
     */
    @SystemApi
    @Nullable public final MacAddress macAddress;

    /**
     * The peer identifier of a Wi-Fi Aware Responder. Will be null if a MAC Address (the macAddress
     * field) is used to identify the Responder.
     * @hide
     */
    @SystemApi
    @Nullable public final PeerHandle peerHandle;

    /**
     * The device type of the Responder.
     * @hide
     */
    @SystemApi
    public final int responderType;

    /**
     * Indicates whether the Responder device supports IEEE 802.11mc.
     * @hide
     */
    @SystemApi
    public final boolean supports80211mc;

    /**
     * Responder channel bandwidth, specified using {@link ChannelWidth}.
     * @hide
     */
    @SystemApi
    public final int channelWidth;

    /**
     * The primary 20 MHz frequency (in MHz) of the channel of the Responder.
     * @hide
     */
    @SystemApi
    public final int frequency;

    /**
     * Not used if the {@link #channelWidth} is 20 MHz. If the Responder uses 40, 80, 160 or
     * 320 MHz, this is the center frequency (in MHz), if the Responder uses 80 + 80 MHz,
     * this is the center frequency of the first segment (in MHz).
     * @hide
     */
    @SystemApi
    public final int centerFreq0;

    /**
     * Only used if the {@link #channelWidth} is 80 + 80 MHz. If the Responder uses 80 + 80 MHz,
     * this is the center frequency of the second segment (in MHz).
     * @hide
     */
    @SystemApi
    public final int centerFreq1;

    /**
     * The preamble used by the Responder, specified using {@link PreambleType}.
     * @hide
     */
    @SystemApi
    public final int preamble;

    /**
     * Constructs Responder configuration, using a MAC address to identify the Responder.
     *
     * @param macAddress      The MAC address of the Responder.
     * @param responderType   The type of the responder device, specified using
     *                        {@link ResponderType}.
     *                        For an access point (AP) use {@code RESPONDER_AP}.
     * @param supports80211mc Indicates whether the responder supports IEEE 802.11mc.
     * @param channelWidth    Responder channel bandwidth, specified using {@link ChannelWidth}.
     * @param frequency       The primary 20 MHz frequency (in MHz) of the channel of the Responder.
     * @param centerFreq0     Not used if the {@code channelWidth} is 20 MHz. If the Responder uses
     *                        40, 80, 160 or 320 MHz, this is the center frequency (in MHz), if the
     *                        Responder uses 80 + 80 MHz, this is the center frequency of the first
     *                        segment (in MHz).
     * @param centerFreq1     Only used if the {@code channelWidth} is 80 + 80 MHz. If the
     *                        Responder
     *                        uses 80 + 80 MHz, this is the center frequency of the second segment
     *                        (in
     *                        MHz).
     * @param preamble        The preamble used by the Responder, specified using
     *                        {@link PreambleType}.
     * @hide
     */
    @SystemApi
    public ResponderConfig(@NonNull MacAddress macAddress, @ResponderType int responderType,
            boolean supports80211mc, @ChannelWidth int channelWidth, int frequency, int centerFreq0,
            int centerFreq1, @PreambleType int preamble) {
        if (macAddress == null) {
            throw new IllegalArgumentException(
                    "Invalid ResponderConfig - must specify a MAC address");
        }
        this.macAddress = macAddress;
        this.peerHandle = null;
        this.responderType = responderType;
        this.supports80211mc = supports80211mc;
        this.channelWidth = channelWidth;
        this.frequency = frequency;
        this.centerFreq0 = centerFreq0;
        this.centerFreq1 = centerFreq1;
        this.preamble = preamble;
    }

    /**
     * Constructs Responder configuration, using a Wi-Fi Aware PeerHandle to identify the Responder.
     *
     * @param peerHandle      The Wi-Fi Aware peer identifier of the Responder.
     * @param responderType   The type of the responder device, specified using
     *                        {@link ResponderType}.
     * @param supports80211mc Indicates whether the responder supports IEEE 802.11mc.
     * @param channelWidth    Responder channel bandwidth, specified using {@link ChannelWidth}.
     * @param frequency       The primary 20 MHz frequency (in MHz) of the channel of the Responder.
     * @param centerFreq0     Not used if the {@code channelWidth} is 20 MHz. If the Responder uses
     *                        40, 80, 160, or 320 MHz, this is the center frequency (in MHz), if the
     *                        Responder uses 80 + 80 MHz, this is the center frequency of the first
     *                        segment (in MHz).
     * @param centerFreq1     Only used if the {@code channelWidth} is 80 + 80 MHz. If the
     *                        Responder
     *                        uses 80 + 80 MHz, this is the center frequency of the second segment
     *                        (in
     *                        MHz).
     * @param preamble        The preamble used by the Responder, specified using
     *                        {@link PreambleType}.
     * @hide
     */
    @SystemApi
    public ResponderConfig(@NonNull PeerHandle peerHandle, @ResponderType int responderType,
            boolean supports80211mc, @ChannelWidth int channelWidth, int frequency, int centerFreq0,
            int centerFreq1, @PreambleType int preamble) {
        this.macAddress = null;
        this.peerHandle = peerHandle;
        this.responderType = responderType;
        this.supports80211mc = supports80211mc;
        this.channelWidth = channelWidth;
        this.frequency = frequency;
        this.centerFreq0 = centerFreq0;
        this.centerFreq1 = centerFreq1;
        this.preamble = preamble;
    }

    /**
     * Constructs Responder configuration. This is a constructor which specifies both
     * a MAC address and a Wi-Fi PeerHandle to identify the Responder. For an RTT RangingRequest
     * the Wi-Fi Aware peer identifier can be constructed using an Identifier set to zero.
     *
     * @param macAddress      The MAC address of the Responder.
     * @param peerHandle      The Wi-Fi Aware peer identifier of the Responder.
     * @param responderType   The type of the responder device, specified using
     *                        {@link ResponderType}.
     * @param supports80211mc Indicates whether the responder supports IEEE 802.11mc.
     * @param channelWidth    Responder channel bandwidth, specified using {@link ChannelWidth}.
     * @param frequency       The primary 20 MHz frequency (in MHz) of the channel of the Responder.
     * @param centerFreq0     Not used if the {@code channelWidth} is 20 MHz. If the Responder uses
     *                        40, 80, 160 or 320 MHz, this is the center frequency (in MHz), if the
     *                        Responder uses 80 + 80 MHz, this is the center frequency of the first
     *                        segment (in MHz).
     * @param centerFreq1     Only used if the {@code channelWidth} is 80 + 80 MHz. If the
     *                        Responder
     *                        uses 80 + 80 MHz, this is the center frequency of the second segment
     *                        (in
     *                        MHz).
     * @param preamble        The preamble used by the Responder, specified using
     *                        {@link PreambleType}.
     *
     * @hide
     */
    public ResponderConfig(@NonNull MacAddress macAddress, @NonNull PeerHandle peerHandle,
            @ResponderType int responderType, boolean supports80211mc,
            @ChannelWidth int channelWidth, int frequency, int centerFreq0, int centerFreq1,
            @PreambleType int preamble) {
        this.macAddress = macAddress;
        this.peerHandle = peerHandle;
        this.responderType = responderType;
        this.supports80211mc = supports80211mc;
        this.channelWidth = channelWidth;
        this.frequency = frequency;
        this.centerFreq0 = centerFreq0;
        this.centerFreq1 = centerFreq1;
        this.preamble = preamble;
    }

    /**
     * Creates a Responder configuration from a {@link ScanResult} corresponding to an Access
     * Point (AP), which can be obtained from {@link android.net.wifi.WifiManager#getScanResults()}.
     */
    @NonNull
    public static ResponderConfig fromScanResult(@NonNull ScanResult scanResult) {
        MacAddress macAddress = MacAddress.fromString(scanResult.BSSID);
        int responderType = RESPONDER_AP;
        boolean supports80211mc = scanResult.is80211mcResponder();
        int channelWidth = translateFromScanResultToLocalChannelWidth(scanResult.channelWidth);
        int frequency = scanResult.frequency;
        int centerFreq0 = scanResult.centerFreq0;
        int centerFreq1 = scanResult.centerFreq1;

        int preamble;
        if (scanResult.informationElements != null && scanResult.informationElements.length != 0) {
            boolean htCapabilitiesPresent = false;
            boolean vhtCapabilitiesPresent = false;
            boolean heCapabilitiesPresent = false;
            boolean ehtCapabilitiesPresent = false;

            for (ScanResult.InformationElement ie : scanResult.informationElements) {
                if (ie.id == EID_HT_CAPABILITIES) {
                    htCapabilitiesPresent = true;
                } else if (ie.id == EID_VHT_CAPABILITIES) {
                    vhtCapabilitiesPresent = true;
                } else if (ie.id == EID_EXTENSION_PRESENT && ie.idExt == EID_EXT_HE_CAPABILITIES) {
                    heCapabilitiesPresent = true;
                } else if (ie.id == EID_EXTENSION_PRESENT && ie.idExt == EID_EXT_EHT_CAPABILITIES) {
                    ehtCapabilitiesPresent = true;
                }
            }

            if (ehtCapabilitiesPresent && ScanResult.is6GHz(frequency)) {
                preamble = PREAMBLE_EHT;
            } else if (heCapabilitiesPresent && ScanResult.is6GHz(frequency)) {
                preamble = PREAMBLE_HE;
            } else if (vhtCapabilitiesPresent) {
                preamble = PREAMBLE_VHT;
            } else if (htCapabilitiesPresent) {
                preamble = PREAMBLE_HT;
            } else {
                preamble = PREAMBLE_LEGACY;
            }
        } else {
            Log.e(TAG, "Scan Results do not contain IEs - using backup method to select preamble");
            if (channelWidth == CHANNEL_WIDTH_320MHZ) {
                preamble = PREAMBLE_EHT;
            } else if (channelWidth == CHANNEL_WIDTH_80MHZ
                    || channelWidth == CHANNEL_WIDTH_160MHZ) {
                preamble = PREAMBLE_VHT;
            } else {
                preamble = PREAMBLE_HT;
            }
        }

        return new ResponderConfig(macAddress, responderType, supports80211mc, channelWidth,
                frequency, centerFreq0, centerFreq1, preamble);
    }

    /**
     * Creates a Responder configuration from a MAC address corresponding to a Wi-Fi Aware
     * Responder. The Responder parameters are set to defaults.
     * @hide
     */
    @SystemApi
    @NonNull
    public static ResponderConfig fromWifiAwarePeerMacAddressWithDefaults(
            @NonNull MacAddress macAddress) {
        /* Note: the parameters are those of the Aware discovery channel (channel 6). A Responder
         * is expected to be brought up and available to negotiate a maximum accuracy channel
         * (i.e. Band 5 @ 80MHz). A Responder is brought up on the peer by starting an Aware
         * Unsolicited Publisher with Ranging enabled.
         */
        return new ResponderConfig(macAddress, RESPONDER_AWARE, true, CHANNEL_WIDTH_20MHZ,
                AWARE_BAND_2_DISCOVERY_CHANNEL, 0, 0, PREAMBLE_HT);
    }

    /**
     * Creates a Responder configuration from a {@link PeerHandle} corresponding to a Wi-Fi Aware
     * Responder. The Responder parameters are set to defaults.
     * @hide
     */
    @SystemApi
    @NonNull
    public static ResponderConfig fromWifiAwarePeerHandleWithDefaults(
            @NonNull PeerHandle peerHandle) {
        /* Note: the parameters are those of the Aware discovery channel (channel 6). A Responder
         * is expected to be brought up and available to negotiate a maximum accuracy channel
         * (i.e. Band 5 @ 80MHz). A Responder is brought up on the peer by starting an Aware
         * Unsolicited Publisher with Ranging enabled.
         */
        return new ResponderConfig(peerHandle, RESPONDER_AWARE, true, CHANNEL_WIDTH_20MHZ,
                AWARE_BAND_2_DISCOVERY_CHANNEL, 0, 0, PREAMBLE_HT);
    }

    /**
     * Check whether the Responder configuration is valid.
     *
     * @return true if valid, false otherwise.
     *
     * @hide
     */
    public boolean isValid(boolean awareSupported) {
        if (macAddress == null && peerHandle == null || macAddress != null && peerHandle != null) {
            return false;
        }
        if (!awareSupported && responderType == RESPONDER_AWARE) {
            return false;
        }
        return true;
    }

    /**
     * @return the MAC address of the responder
     */
    @Nullable
    public MacAddress getMacAddress() {
        return macAddress;
    }

    /**
     * @return true if the Responder supports the 802.11mc protocol, false otherwise.
     */
    public boolean is80211mcSupported() {
        return supports80211mc;
    }

    /**
     * AP Channel bandwidth; one of {@link ScanResult#CHANNEL_WIDTH_20MHZ},
     * {@link ScanResult#CHANNEL_WIDTH_40MHZ},
     * {@link ScanResult#CHANNEL_WIDTH_80MHZ}, {@link ScanResult#CHANNEL_WIDTH_160MHZ},
     * {@link ScanResult #CHANNEL_WIDTH_80MHZ_PLUS_MHZ} or {@link ScanResult#CHANNEL_WIDTH_320MHZ}.
     *
     * @return the bandwidth repsentation of the Wi-Fi channel
     */
    public @WifiAnnotations.ChannelWidth int getChannelWidth() {
        return translateFromLocalToScanResultChannelWidth(channelWidth);
    }

    /**
     * @return the frequency in MHz of the Wi-Fi channel
     */
    @IntRange(from = 0)
    public int getFrequencyMhz() {
        return frequency;
    }

    /**
     * If the Access Point (AP) bandwidth is 20 MHz, 0 MHz is returned.
     * If the AP use 40, 80 or 160 MHz, this is the center frequency (in MHz).
     * if the AP uses 80 + 80 MHz, this is the center frequency of the first segment (in MHz).
     *
     * @return the center frequency in MHz of the first channel segment
     */
    @IntRange(from = 0)
    public int getCenterFreq0Mhz() {
        return centerFreq0;
    }

    /**
     * If the Access Point (AP) bandwidth is 80 + 80 MHz, this param is not used and returns 0.
     * If the AP uses 80 + 80 MHz, this is the center frequency of the second segment in MHz.
     *
     * @return the center frequency in MHz of the second channel segment (if used)
     */
    @IntRange(from = 0)
    public int getCenterFreq1Mhz() {
        return centerFreq1;
    }

    /**
     * Get the preamble type of the channel.
     *
     * @return the preamble used for this channel
     */
    public @WifiAnnotations.PreambleType int getPreamble() {
        return translateFromLocalToScanResultPreamble(preamble);
    }

    /**
     * Get responder type.
     * @see Builder#setResponderType(int)
     * @return The type of this responder
     */
    public @ResponderType int getResponderType() {
        return responderType;
    }

    /**
     * Builder class used to construct {@link ResponderConfig} objects.
     */
    public static final class Builder {
        private MacAddress mMacAddress;
        private @ResponderType int mResponderType = RESPONDER_AP;
        private boolean mSupports80211Mc = true;
        private @ChannelWidth int mChannelWidth = CHANNEL_WIDTH_20MHZ;
        private int mFrequency = 0;
        private int mCenterFreq0 = 0;
        private int mCenterFreq1 = 0;
        private @PreambleType int mPreamble = PREAMBLE_LEGACY;

        /**
         * Sets the Responder MAC Address.
         *
         * @param macAddress the phyical address of the responder
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setMacAddress(@NonNull MacAddress macAddress) {
            this.mMacAddress = macAddress;
            return this;
        }

        /**
         * Sets an indication the access point can to respond to the two-sided Wi-Fi RTT protocol,
         * but, if false, indicates only one-sided Wi-Fi RTT is possible.
         *
         * @param supports80211mc the ability to support the Wi-Fi RTT protocol
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder set80211mcSupported(boolean supports80211mc) {
            this.mSupports80211Mc = supports80211mc;
            return this;
        }

        /**
         * Sets the channel bandwidth in MHz.
         *
         * @param channelWidth the bandwidth of the channel in MHz
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setChannelWidth(@WifiAnnotations.ChannelWidth int channelWidth) {
            this.mChannelWidth = translateFromScanResultToLocalChannelWidth(channelWidth);
            return this;
        }

        /**
         * Sets the frequency of the channel in MHz.
         * <p>
         * Note: The frequency is used as a hint, and the underlying WiFi subsystem may use it, or
         * select an alternate if its own connectivity scans have determined the frequency of the
         * access point has changed.
         * </p>
         *
         * @param frequency the frequency of the channel in MHz
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setFrequencyMhz(@IntRange(from = 0) int frequency) {
            this.mFrequency = frequency;
            return this;
        }

        /**
         * Sets the center frequency in MHz of the first segment of the channel.
         * <p>
         * Note: The frequency is used as a hint, and the underlying WiFi subsystem may use it, or
         * select an alternate if its own connectivity scans have determined the frequency of the
         * access point has changed.
         * </p>
         *
         * @param centerFreq0 the center frequency in MHz of first channel segment
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setCenterFreq0Mhz(@IntRange(from = 0) int centerFreq0) {
            this.mCenterFreq0 = centerFreq0;
            return this;
        }

        /**
         * Sets the center frequency in MHz of the second segment of the channel, if used.
         * <p>
         * Note: The frequency is used as a hint, and the underlying WiFi subsystem may use it, or
         * select an alternate if its own connectivity scans have determined the frequency of the
         * access point has changed.
         * </p>
         *
         * @param centerFreq1 the center frequency in MHz of second channel segment
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setCenterFreq1Mhz(@IntRange(from = 0) int centerFreq1) {
            this.mCenterFreq1 = centerFreq1;
            return this;
        }

        /**
         * Sets the preamble encoding for the protocol.
         *
         * @param preamble the preamble encoding
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setPreamble(@WifiAnnotations.PreambleType int preamble) {
            this.mPreamble = translateFromScanResultToLocalPreamble(preamble);
            return this;
        }

        /**
         * Sets the responder type, can be {@link #RESPONDER_AP} or {@link #RESPONDER_STA}
         *
         * @param responderType the type of the responder, if not set defaults to
         * {@link #RESPONDER_AP}
         * @return the builder to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setResponderType(@ResponderType int responderType) {
            if (responderType != RESPONDER_STA && responderType != RESPONDER_AP) {
                throw new IllegalArgumentException("invalid responder type");
            }
            mResponderType = responderType;
            return this;
        }

        /**
         * Build {@link ResponderConfig} given the current configurations made on the builder.
         * @return an instance of {@link ResponderConfig}
         */
        @NonNull
        public ResponderConfig build() {
            if (mMacAddress == null) {
                throw new IllegalArgumentException(
                        "Invalid ResponderConfig - must specify a MAC address");
            }
            return new ResponderConfig(mMacAddress, mResponderType, mSupports80211Mc, mChannelWidth,
                    mFrequency, mCenterFreq0, mCenterFreq1, mPreamble);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        if (macAddress == null) {
            dest.writeBoolean(false);
        } else {
            dest.writeBoolean(true);
            macAddress.writeToParcel(dest, flags);
        }
        if (peerHandle == null) {
            dest.writeBoolean(false);
        } else {
            dest.writeBoolean(true);
            dest.writeInt(peerHandle.peerId);
        }
        dest.writeInt(responderType);
        dest.writeInt(supports80211mc ? 1 : 0);
        dest.writeInt(channelWidth);
        dest.writeInt(frequency);
        dest.writeInt(centerFreq0);
        dest.writeInt(centerFreq1);
        dest.writeInt(preamble);
    }

    public static final @android.annotation.NonNull Creator<ResponderConfig> CREATOR = new Creator<ResponderConfig>() {
        @Override
        public ResponderConfig[] newArray(int size) {
            return new ResponderConfig[size];
        }

        @Override
        public ResponderConfig createFromParcel(Parcel in) {
            boolean macAddressPresent = in.readBoolean();
            MacAddress macAddress = null;
            if (macAddressPresent) {
                macAddress = MacAddress.CREATOR.createFromParcel(in);
            }
            boolean peerHandlePresent = in.readBoolean();
            PeerHandle peerHandle = null;
            if (peerHandlePresent) {
                peerHandle = new PeerHandle(in.readInt());
            }
            int responderType = in.readInt();
            boolean supports80211mc = in.readInt() == 1;
            int channelWidth = in.readInt();
            int frequency = in.readInt();
            int centerFreq0 = in.readInt();
            int centerFreq1 = in.readInt();
            int preamble = in.readInt();

            if (peerHandle == null) {
                return new ResponderConfig(macAddress, responderType, supports80211mc, channelWidth,
                        frequency, centerFreq0, centerFreq1, preamble);
            } else {
                return new ResponderConfig(peerHandle, responderType, supports80211mc, channelWidth,
                        frequency, centerFreq0, centerFreq1, preamble);
            }
        }
    };

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ResponderConfig)) {
            return false;
        }

        ResponderConfig lhs = (ResponderConfig) o;

        return Objects.equals(macAddress, lhs.macAddress) && Objects.equals(peerHandle,
                lhs.peerHandle) && responderType == lhs.responderType
                && supports80211mc == lhs.supports80211mc && channelWidth == lhs.channelWidth
                && frequency == lhs.frequency && centerFreq0 == lhs.centerFreq0
                && centerFreq1 == lhs.centerFreq1 && preamble == lhs.preamble;
    }

    @Override
    public int hashCode() {
        return Objects.hash(macAddress, peerHandle, responderType, supports80211mc, channelWidth,
                frequency, centerFreq0, centerFreq1, preamble);
    }

    @Override
    public String toString() {
        return new StringBuffer("ResponderConfig: macAddress=").append(macAddress).append(
                ", peerHandle=").append(peerHandle == null ? "<null>" : peerHandle.peerId).append(
                ", responderType=").append(responderType).append(", supports80211mc=").append(
                supports80211mc).append(", channelWidth=").append(channelWidth).append(
                ", frequency=").append(frequency).append(", centerFreq0=").append(
                centerFreq0).append(", centerFreq1=").append(centerFreq1).append(
                ", preamble=").append(preamble).toString();
    }

    /**
     * Translate an SDK channel width encoding to a local channel width encoding
     *
     * @param scanResultChannelWidth the {@link ScanResult} defined channel width encoding
     * @return the translated channel width encoding
     *
     * @hide
     */
    static int translateFromScanResultToLocalChannelWidth(
            @WifiAnnotations.ChannelWidth int scanResultChannelWidth) {
        switch (scanResultChannelWidth) {
            case ScanResult.CHANNEL_WIDTH_20MHZ:
                return CHANNEL_WIDTH_20MHZ;
            case ScanResult.CHANNEL_WIDTH_40MHZ:
                return CHANNEL_WIDTH_40MHZ;
            case ScanResult.CHANNEL_WIDTH_80MHZ:
                return CHANNEL_WIDTH_80MHZ;
            case ScanResult.CHANNEL_WIDTH_160MHZ:
                return CHANNEL_WIDTH_160MHZ;
            case ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ:
                return CHANNEL_WIDTH_80MHZ_PLUS_MHZ;
            case ScanResult.CHANNEL_WIDTH_320MHZ:
                return CHANNEL_WIDTH_320MHZ;
            default:
                throw new IllegalArgumentException(
                        "translateFromScanResultChannelWidth: bad " + scanResultChannelWidth);
        }
    }

    /**
     * Translate the local channel width encoding to the SDK channel width encoding.
     *
     * @param localChannelWidth the locally defined channel width encoding
     * @return the translated channel width encoding
     *
     * @hide
     */
    static int translateFromLocalToScanResultChannelWidth(@ChannelWidth int localChannelWidth) {
        switch (localChannelWidth) {
            case CHANNEL_WIDTH_20MHZ:
                return ScanResult.CHANNEL_WIDTH_20MHZ;
            case CHANNEL_WIDTH_40MHZ:
                return ScanResult.CHANNEL_WIDTH_40MHZ;
            case CHANNEL_WIDTH_80MHZ:
                return ScanResult.CHANNEL_WIDTH_80MHZ;
            case CHANNEL_WIDTH_160MHZ:
                return ScanResult.CHANNEL_WIDTH_160MHZ;
            case CHANNEL_WIDTH_80MHZ_PLUS_MHZ:
                return ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ;
            case CHANNEL_WIDTH_320MHZ:
                return ScanResult.CHANNEL_WIDTH_320MHZ;
            default:
                throw new IllegalArgumentException(
                        "translateFromLocalChannelWidth: bad " + localChannelWidth);
        }
    }

    /**
     * Translate the {@link ScanResult} preamble encoding to the local preamble encoding.
     *
     * @param scanResultPreamble the channel width supplied
     * @return the local encoding of the Preamble
     *
     * @hide
     */
    static int translateFromScanResultToLocalPreamble(
            @WifiAnnotations.PreambleType int scanResultPreamble) {
        switch (scanResultPreamble) {
            case ScanResult.PREAMBLE_LEGACY:
                return PREAMBLE_LEGACY;
            case ScanResult.PREAMBLE_HT:
                return PREAMBLE_HT;
            case ScanResult.PREAMBLE_VHT:
                return PREAMBLE_VHT;
            case ScanResult.PREAMBLE_HE:
                return PREAMBLE_HE;
            case ScanResult.PREAMBLE_EHT:
                return PREAMBLE_EHT;
            default:
                throw new IllegalArgumentException(
                        "translateFromScanResultPreamble: bad " + scanResultPreamble);
        }
    }

    /**
     * Translate the local preamble encoding to the {@link ScanResult} preamble encoding.
     *
     * @param localPreamble the local preamble encoding
     * @return the {@link ScanResult} encoding of the Preamble
     *
     * @hide
     */
    static int translateFromLocalToScanResultPreamble(@PreambleType int localPreamble) {
        switch (localPreamble) {
            case PREAMBLE_LEGACY:
                return ScanResult.PREAMBLE_LEGACY;
            case PREAMBLE_HT:
                return ScanResult.PREAMBLE_HT;
            case PREAMBLE_VHT:
                return ScanResult.PREAMBLE_VHT;
            case PREAMBLE_HE:
                return ScanResult.PREAMBLE_HE;
            case PREAMBLE_EHT:
                return ScanResult.PREAMBLE_EHT;
            default:
                throw new IllegalArgumentException(
                        "translateFromLocalPreamble: bad " + localPreamble);
        }
    }
}
