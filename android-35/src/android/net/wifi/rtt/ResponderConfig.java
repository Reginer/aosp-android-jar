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

import android.annotation.FlaggedApi;
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

import com.android.wifi.flags.Flags;

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

    private static final long DEFAULT_NTB_MIN_TIME_BETWEEN_MEASUREMENTS_MICROS = 250000;
    private static final long DEFAULT_NTB_MAX_TIME_BETWEEN_MEASUREMENTS_MICROS = 15000000;

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
     * Indicates whether the Responder device supports IEEE 802.11az non-trigger based ranging.
     * @hide
     */
    @SystemApi
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public final boolean supports80211azNtb;

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

    private long mNtbMinMeasurementTime = DEFAULT_NTB_MIN_TIME_BETWEEN_MEASUREMENTS_MICROS;
    private long mNtbMaxMeasurementTime = DEFAULT_NTB_MAX_TIME_BETWEEN_MEASUREMENTS_MICROS;

    /**
     * Constructs Responder configuration from the builder
     * @param builder See {@link Builder}
     * @hide
     */
    public ResponderConfig(Builder builder) {
        if (builder.mMacAddress == null && builder.mPeerHandle == null) {
            throw new IllegalArgumentException(
                    "Invalid ResponderConfig - must specify a MAC address or Peer handle");
        }
        this.macAddress = builder.mMacAddress;
        this.peerHandle = builder.mPeerHandle;
        this.responderType = builder.mResponderType;
        this.supports80211mc = builder.mSupports80211Mc;
        this.supports80211azNtb = builder.mSupports80211azNtb;
        this.channelWidth = builder.mChannelWidth;
        this.frequency = builder.mFrequency;
        this.centerFreq0 = builder.mCenterFreq0;
        this.centerFreq1 = builder.mCenterFreq1;
        this.preamble = builder.mPreamble;
        this.mNtbMinMeasurementTime = builder.mNtbMinMeasurementTime;
        this.mNtbMaxMeasurementTime = builder.mNtbMaxMeasurementTime;
    }

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
        this.supports80211azNtb = false;
        this.mNtbMinMeasurementTime = DEFAULT_NTB_MIN_TIME_BETWEEN_MEASUREMENTS_MICROS;
        this.mNtbMaxMeasurementTime = DEFAULT_NTB_MAX_TIME_BETWEEN_MEASUREMENTS_MICROS;
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
        this.supports80211azNtb = false;
        this.mNtbMinMeasurementTime = DEFAULT_NTB_MIN_TIME_BETWEEN_MEASUREMENTS_MICROS;
        this.mNtbMaxMeasurementTime = DEFAULT_NTB_MAX_TIME_BETWEEN_MEASUREMENTS_MICROS;
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
        this.supports80211azNtb = false;
        this.mNtbMinMeasurementTime = DEFAULT_NTB_MIN_TIME_BETWEEN_MEASUREMENTS_MICROS;
        this.mNtbMaxMeasurementTime = DEFAULT_NTB_MAX_TIME_BETWEEN_MEASUREMENTS_MICROS;
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
        boolean supports80211azNtbRanging = scanResult.is80211azNtbResponder();
        int channelWidth = scanResult.channelWidth;
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
                preamble = ScanResult.PREAMBLE_EHT;
            } else if (heCapabilitiesPresent && ScanResult.is6GHz(frequency)) {
                preamble = ScanResult.PREAMBLE_HE;
            } else if (vhtCapabilitiesPresent) {
                preamble = ScanResult.PREAMBLE_VHT;
            } else if (htCapabilitiesPresent) {
                preamble = ScanResult.PREAMBLE_HT;
            } else {
                preamble = ScanResult.PREAMBLE_LEGACY;
            }
        } else {
            Log.e(TAG, "Scan Results do not contain IEs - using backup method to select preamble");
            if (channelWidth == ScanResult.CHANNEL_WIDTH_320MHZ) {
                preamble = ScanResult.PREAMBLE_EHT;
            } else if (channelWidth == ScanResult.CHANNEL_WIDTH_80MHZ
                    || channelWidth == ScanResult.CHANNEL_WIDTH_160MHZ) {
                preamble = ScanResult.PREAMBLE_VHT;
            } else {
                preamble = ScanResult.PREAMBLE_HT;
            }
        }

        return new ResponderConfig.Builder()
                .setMacAddress(macAddress)
                .setResponderType(responderType)
                .set80211mcSupported(supports80211mc)
                .set80211azNtbSupported(supports80211azNtbRanging)
                .setChannelWidth(channelWidth)
                .setFrequencyMhz(frequency)
                .setCenterFreq0Mhz(centerFreq0)
                .setCenterFreq1Mhz(centerFreq1)
                .setPreamble(preamble)
                .build();
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
        return new ResponderConfig.Builder()
                .setMacAddress(macAddress)
                .setResponderType(RESPONDER_AWARE)
                .build();
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
        return new ResponderConfig.Builder()
                .setPeerHandle(peerHandle)
                .setResponderType(RESPONDER_AWARE)
                .build();
    }

    private static boolean isResponderTypeSupported(@ResponderType int responderType) {
        switch (responderType) {
            case RESPONDER_AP:
            case RESPONDER_STA:
            case RESPONDER_AWARE:
                break;
            case RESPONDER_P2P_GO:
            case RESPONDER_P2P_CLIENT:
            default:
                return false;
        }
        return true;
    }

    /**
     * Check whether the Responder configuration is valid.
     *
     * @return true if valid, false otherwise.
     *
     * @hide
     */
    public boolean isValid(boolean awareSupported) {
        if (!isResponderTypeSupported(responderType)) return false;
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
     * @return the peer handle of the responder
     *
     * @hide
     */
    @Nullable
    public PeerHandle getPeerHandle() {
        return peerHandle;
    }

    /**
     * @return true if the Responder supports the 802.11mc protocol, false otherwise.
     */
    public boolean is80211mcSupported() {
        return supports80211mc;
    }

    /**
     * @return true if the Responder supports the 802.11az non-trigger based ranging protocol,
     * false otherwise.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public boolean is80211azNtbSupported() {
        return supports80211azNtb;
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
     * Gets the minimum time between IEEE 802.11az non-trigger based ranging measurements in
     * microseconds for the responder.
     *
     * @hide
     */
    public long getNtbMinTimeBetweenMeasurementsMicros() {
        return mNtbMinMeasurementTime;
    }

    /**
     * Gets the maximum time between IEEE 802.11az non-trigger based ranging measurements in
     * microseconds for the responder.
     *
     * @hide
     */
    public long getNtbMaxTimeBetweenMeasurementsMicros() {
        return mNtbMaxMeasurementTime;
    }

    /**
     * @hide
     */
    public void setNtbMinTimeBetweenMeasurementsMicros(long ntbMinMeasurementTime) {
        this.mNtbMinMeasurementTime = ntbMinMeasurementTime;
    }

    /**
     * @hide
     */
    public void setNtbMaxTimeBetweenMeasurementsMicros(long ntbMaxMeasurementTime) {
        this.mNtbMaxMeasurementTime = ntbMaxMeasurementTime;
    }

    /**
     * Builder class used to construct {@link ResponderConfig} objects.
     */
    public static final class Builder {
        private MacAddress mMacAddress;
        private PeerHandle mPeerHandle;
        private @ResponderType int mResponderType = RESPONDER_AP;
        private boolean mSupports80211Mc = true;
        private boolean mSupports80211azNtb = false;
        private @ChannelWidth int mChannelWidth = CHANNEL_WIDTH_20MHZ;
        private int mFrequency = 0;
        private int mCenterFreq0 = 0;
        private int mCenterFreq1 = 0;
        private @PreambleType int mPreamble = PREAMBLE_LEGACY;
        private long mNtbMinMeasurementTime = DEFAULT_NTB_MIN_TIME_BETWEEN_MEASUREMENTS_MICROS;
        private long mNtbMaxMeasurementTime = DEFAULT_NTB_MAX_TIME_BETWEEN_MEASUREMENTS_MICROS;

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
         * Sets the Responder Peer handle.
         *
         * @param peerHandle Peer handle of the resposnde
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         * @hide
         */
        @NonNull
        public Builder setPeerHandle(@NonNull PeerHandle peerHandle) {
            this.mPeerHandle = peerHandle;
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
         * Sets an indication the access point can to respond to the IEEE 802.11az non-trigger
         * based ranging protocol, but, if false, indicates only IEEE 802.11mc or one-sided Wi-Fi
         * RTT is possible.
         *
         * @param supports80211azNtb the ability to support the IEEE 802.11az non-trigger based
         *                           ranging protocol
         * @return the builder to facilitate chaining
         *         {@code builder.setXXX(..).setXXX(..)}.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder set80211azNtbSupported(boolean supports80211azNtb) {
            this.mSupports80211azNtb = supports80211azNtb;
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
         * Sets the responder type, can be {@link #RESPONDER_AP} or {@link #RESPONDER_STA} or
         * {@link #RESPONDER_AWARE}
         *
         * @param responderType the type of the responder, if not set defaults to
         * {@link #RESPONDER_AP}
         * @return the builder to facilitate chaining {@code builder.setXXX(..).setXXX(..)}.
         */
        @NonNull
        public Builder setResponderType(@ResponderType int responderType) {
            if (!isResponderTypeSupported(responderType)) {
                throw new IllegalArgumentException("invalid responder type " + responderType);
            }
            mResponderType = responderType;
            return this;
        }

        /**
         * Sets the minimum time between IEEE 802.11az non-trigger based ranging measurements in
         * microseconds for the responder.
         *
         * Note: This should be a multiple of 100 microseconds as per IEEE 802.11 az standard.
         *
         * @param ntbMinMeasurementTime Minimum time between non-trigger based IEEE 802.11az
         *                              ranging measurements in units of  100 microseconds. Range of
         *                              values (0, 419430400).
         * @hide
         */
        public Builder setNtbMinTimeBetweenMeasurementsMicros(long ntbMinMeasurementTime) {
            if (mNtbMinMeasurementTime == 0 || mNtbMinMeasurementTime >= 419430400) {
                throw new IllegalArgumentException(
                        "Should be a non-zero number less than 419430400 microseconds");
            }
            if (mNtbMinMeasurementTime % 100 != 0) {
                throw new IllegalArgumentException("Should be a multiple of 100 microseconds");
            }
            mNtbMinMeasurementTime = ntbMinMeasurementTime;
            return  this;
        }

        /**
         * Sets the maximum time between IEEE 802.11az non-trigger based ranging measurements in
         * microseconds for the responder.
         *
         * Note: This should be a multiple of 10000 microseconds (10 milliseconds) as per
         * IEEE 802.11 az standard.
         *
         * @param ntbMaxMeasurementTime Maximum time between non-trigger based IEEE 802.11az
         *                              ranging measurements in units of  10000 microseconds. Range
         *                              of values (0, 5242880000).
         * @hide
         */
        public Builder setNtbMaxTimeBetweenMeasurementsMicros(long ntbMaxMeasurementTime) {
            if (mNtbMaxMeasurementTime % 10000 != 0) {
                throw new IllegalArgumentException("Should be a multiple of 10000 microseconds");
            }
            if (mNtbMaxMeasurementTime == 0 || mNtbMaxMeasurementTime >= 5242880000L) {
                throw new IllegalArgumentException(
                        "Should be a non-zero number less than 5242880000 microseconds");
            }
            mNtbMaxMeasurementTime = ntbMaxMeasurementTime;
            return  this;
        }

        /**
         * Build {@link ResponderConfig} given the current configurations made on the builder.
         * @return an instance of {@link ResponderConfig}
         */
        @NonNull
        public ResponderConfig build() {
            if ((mMacAddress == null && mPeerHandle == null)
                    || (mMacAddress != null && mPeerHandle != null)) {
                throw new IllegalArgumentException(
                        "Invalid ResponderConfig - must specify a MAC address or peer handle but "
                                + "not both");
            }
            // For Aware, use supported default values
            if (mResponderType == RESPONDER_AWARE) {
                mSupports80211Mc = true;
                mFrequency = AWARE_BAND_2_DISCOVERY_CHANNEL;
                mChannelWidth = CHANNEL_WIDTH_20MHZ;
                mPreamble = PREAMBLE_HT;
            }
            return new ResponderConfig(this);
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
        dest.writeBoolean(supports80211mc);
        dest.writeBoolean(supports80211azNtb);
        dest.writeInt(channelWidth);
        dest.writeInt(frequency);
        dest.writeInt(centerFreq0);
        dest.writeInt(centerFreq1);
        dest.writeInt(preamble);
        dest.writeLong(mNtbMinMeasurementTime);
        dest.writeLong(mNtbMaxMeasurementTime);
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

            return new ResponderConfig.Builder()
                    .setMacAddress(macAddress)
                    .setPeerHandle(peerHandle)
                    .setResponderType(in.readInt())
                    .set80211mcSupported(in.readBoolean())
                    .set80211azNtbSupported(in.readBoolean())
                    .setChannelWidth(in.readInt())
                    .setFrequencyMhz(in.readInt())
                    .setCenterFreq0Mhz(in.readInt())
                    .setCenterFreq1Mhz(in.readInt())
                    .setPreamble(in.readInt())
                    .setNtbMinTimeBetweenMeasurementsMicros(in.readLong())
                    .setNtbMaxTimeBetweenMeasurementsMicros(in.readLong())
                    .build();
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
                && centerFreq1 == lhs.centerFreq1 && preamble == lhs.preamble
                && supports80211azNtb == lhs.supports80211azNtb
                && mNtbMinMeasurementTime == lhs.mNtbMinMeasurementTime
                && mNtbMaxMeasurementTime == lhs.mNtbMaxMeasurementTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(macAddress, peerHandle, responderType, supports80211mc, channelWidth,
                frequency, centerFreq0, centerFreq1, preamble, supports80211azNtb,
                mNtbMinMeasurementTime, mNtbMaxMeasurementTime);
    }

    @Override
    public String toString() {
        return new StringBuffer("ResponderConfig: macAddress=").append(macAddress)
                .append(", peerHandle=").append(peerHandle == null ? "<null>" : peerHandle.peerId)
                .append(", responderType=").append(responderType)
                .append(", supports80211mc=").append(supports80211mc)
                .append(", channelWidth=").append(channelWidth)
                .append(", frequency=").append(frequency)
                .append(", centerFreq0=").append(centerFreq0)
                .append(", centerFreq1=").append(centerFreq1)
                .append(", preamble=").append(preamble)
                .append(", supports80211azNtb=").append(supports80211azNtb)
                .append(", mNtbMinMeasurementTime ").append(mNtbMinMeasurementTime)
                .append(", mNtbMaxMeasurementTime ").append(mNtbMaxMeasurementTime)
                .toString();
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
