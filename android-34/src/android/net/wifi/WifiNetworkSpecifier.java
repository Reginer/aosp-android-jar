/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.net.wifi;

import static android.net.wifi.ScanResult.UNSPECIFIED;

import static com.android.internal.util.Preconditions.checkNotNull;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.MacAddress;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.ScanResult.WifiBand;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PatternMatcher;
import android.text.TextUtils;
import android.util.Pair;

import com.android.modules.utils.build.SdkLevel;

import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Network specifier object used to request a Wi-Fi network. Apps should use the
 * {@link WifiNetworkSpecifier.Builder} class to create an instance.
 * <p>
 * This specifier can be used to request a local-only connection on devices that support concurrent
 * connections (indicated via
 * {@link WifiManager#isStaConcurrencyForLocalOnlyConnectionsSupported()} and if the initiating app
 * targets SDK &ge; {@link android.os.Build.VERSION_CODES#S} or is a system app. These local-only
 * connections may be brought up as a secondary concurrent connection (primary connection will be
 * used for networks with internet connectivity available to the user and all apps).
 * </p>
 * <p>
 * This specifier can also be used to listen for connected Wi-Fi networks on a particular band.
 * Additionally, some devices may support requesting a connection to a particular band. If the
 * device does not support such a request, it will send {@link NetworkCallback#onUnavailable()}
 * upon request to the callback passed to
 * {@link ConnectivityManager#requestNetwork(NetworkRequest, NetworkCallback)} or equivalent.
 * See {@link Builder#build()} for details.
 * </p>
 */
public final class WifiNetworkSpecifier extends NetworkSpecifier implements Parcelable {

    private static final String TAG = "WifiNetworkSpecifier";

    /**
     * Returns the band for a given frequency in MHz.
     * @hide
     */
    @WifiBand public static int getBand(final int freqMHz) {
        if (ScanResult.is24GHz(freqMHz)) {
            return ScanResult.WIFI_BAND_24_GHZ;
        } else if (ScanResult.is5GHz(freqMHz)) {
            return ScanResult.WIFI_BAND_5_GHZ;
        } else if (ScanResult.is6GHz(freqMHz)) {
            return ScanResult.WIFI_BAND_6_GHZ;
        } else if (ScanResult.is60GHz(freqMHz)) {
            return ScanResult.WIFI_BAND_60_GHZ;
        }
        return UNSPECIFIED;
    }

    /**
     * Check the channel in the array is valid.
     * @hide
     */
    public static boolean validateChannelFrequencyInMhz(@NonNull int[] channels) {
        if (channels == null) {
            return false;
        }
        for (int channel : channels) {
            if (ScanResult.convertFrequencyMhzToChannelIfSupported(channel) == UNSPECIFIED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates that the passed band is a valid band
     * @param band the band to check
     * @return true if the band is valid, false otherwise
     * @hide
     */
    public static boolean validateBand(@WifiBand int band) {
        switch (band) {
            case UNSPECIFIED:
            case ScanResult.WIFI_BAND_24_GHZ:
            case ScanResult.WIFI_BAND_5_GHZ:
            case ScanResult.WIFI_BAND_6_GHZ:
            case ScanResult.WIFI_BAND_60_GHZ:
                return true;
            default:
                return false;
        }
    }

    /**
     * Builder used to create {@link WifiNetworkSpecifier} objects.
     */
    public static final class Builder {
        private static final String MATCH_ALL_SSID_PATTERN_PATH = ".*";
        private static final String MATCH_EMPTY_SSID_PATTERN_PATH = "";
        private static final Pair<MacAddress, MacAddress> MATCH_NO_BSSID_PATTERN1 =
                new Pair<>(MacAddress.BROADCAST_ADDRESS, MacAddress.BROADCAST_ADDRESS);
        private static final Pair<MacAddress, MacAddress> MATCH_NO_BSSID_PATTERN2 =
                new Pair<>(WifiManager.ALL_ZEROS_MAC_ADDRESS, MacAddress.BROADCAST_ADDRESS);
        private static final Pair<MacAddress, MacAddress> MATCH_ALL_BSSID_PATTERN =
                new Pair<>(WifiManager.ALL_ZEROS_MAC_ADDRESS, WifiManager.ALL_ZEROS_MAC_ADDRESS);
        private static final MacAddress MATCH_EXACT_BSSID_PATTERN_MASK =
                MacAddress.BROADCAST_ADDRESS;

        /**
         * Set WPA Enterprise type according to certificate security level.
         * This is for backward compatibility in R.
         */
        private static final int WPA3_ENTERPRISE_AUTO = 0;
        /** Set WPA Enterprise type to standard mode only. */
        private static final int WPA3_ENTERPRISE_STANDARD = 1;
        /** Set WPA Enterprise type to 192 bit mode only. */
        private static final int WPA3_ENTERPRISE_192_BIT = 2;

        /**
         * SSID pattern match specified by the app.
         */
        private @Nullable PatternMatcher mSsidPatternMatcher;
        /**
         * BSSID pattern match specified by the app.
         * Pair of <BaseAddress, Mask>.
         */
        private @Nullable Pair<MacAddress, MacAddress> mBssidPatternMatcher;
        /**
         * Whether this is an OWE network or not.
         */
        private boolean mIsEnhancedOpen;
        /**
         * Pre-shared key for use with WPA-PSK networks.
         */
        private @Nullable String mWpa2PskPassphrase;
        /**
         * Pre-shared key for use with WPA3-SAE networks.
         */
        private @Nullable String mWpa3SaePassphrase;
        /**
         * The enterprise configuration details specifying the EAP method,
         * certificates and other settings associated with the WPA/WPA2-Enterprise networks.
         */
        private @Nullable WifiEnterpriseConfig mWpa2EnterpriseConfig;
        /**
         * The enterprise configuration details specifying the EAP method,
         * certificates and other settings associated with the WPA3-Enterprise networks.
         */
        private @Nullable WifiEnterpriseConfig mWpa3EnterpriseConfig;
        /**
         * Indicate what type this WPA3-Enterprise network is.
         */
        private int mWpa3EnterpriseType = WPA3_ENTERPRISE_AUTO;
        /**
         * This is a network that does not broadcast its SSID, so an
         * SSID-specific probe request must be used for scans.
         */
        private boolean mIsHiddenSSID;
        /**
         * The requested band for this connection, or BAND_UNSPECIFIED.
         */
        @WifiBand private int mBand;

        private int[] mChannels;

        public Builder() {
            mSsidPatternMatcher = null;
            mBssidPatternMatcher = null;
            mIsEnhancedOpen = false;
            mWpa2PskPassphrase = null;
            mWpa3SaePassphrase = null;
            mWpa2EnterpriseConfig = null;
            mWpa3EnterpriseConfig = null;
            mIsHiddenSSID = false;
            mBand = UNSPECIFIED;
            mChannels = new int[0];
        }

        /**
         * Set the unicode SSID match pattern to use for filtering networks from scan results.
         * <p>
         * <li>Overrides any previous value set using {@link #setSsid(String)} or
         * {@link #setSsidPattern(PatternMatcher)}.</li>
         *
         * @param ssidPattern Instance of {@link PatternMatcher} containing the UTF-8 encoded
         *                    string pattern to use for matching the network's SSID.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setSsidPattern(@NonNull PatternMatcher ssidPattern) {
            checkNotNull(ssidPattern);
            mSsidPatternMatcher = ssidPattern;
            return this;
        }

        /**
         * Set the unicode SSID for the network.
         * <p>
         * <li>Sets the SSID to use for filtering networks from scan results. Will only match
         * networks whose SSID is identical to the UTF-8 encoding of the specified value.</li>
         * <li>Overrides any previous value set using {@link #setSsid(String)} or
         * {@link #setSsidPattern(PatternMatcher)}.</li>
         *
         * @param ssid The SSID of the network. It must be valid Unicode.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException if the SSID is not valid unicode.
         */
        public @NonNull Builder setSsid(@NonNull String ssid) {
            checkNotNull(ssid);
            final CharsetEncoder unicodeEncoder = StandardCharsets.UTF_8.newEncoder();
            if (!unicodeEncoder.canEncode(ssid)) {
                throw new IllegalArgumentException("SSID is not a valid unicode string");
            }
            mSsidPatternMatcher = new PatternMatcher(ssid, PatternMatcher.PATTERN_LITERAL);
            return this;
        }

        /**
         * Set the BSSID match pattern to use for filtering networks from scan results.
         * Will match all networks with BSSID which satisfies the following:
         * {@code BSSID & mask == baseAddress}.
         * <p>
         * <li>Overrides any previous value set using {@link #setBssid(MacAddress)} or
         * {@link #setBssidPattern(MacAddress, MacAddress)}.</li>
         *
         * @param baseAddress Base address for BSSID pattern.
         * @param mask Mask for BSSID pattern.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setBssidPattern(
                @NonNull MacAddress baseAddress, @NonNull MacAddress mask) {
            checkNotNull(baseAddress);
            checkNotNull(mask);
            mBssidPatternMatcher = Pair.create(baseAddress, mask);
            return this;
        }

        /**
         * Set the BSSID to use for filtering networks from scan results. Will only match network
         * whose BSSID is identical to the specified value.
         * <p>
         * <li>Sets the BSSID to use for filtering networks from scan results. Will only match
         * networks whose BSSID is identical to specified value.</li>
         * <li>Overrides any previous value set using {@link #setBssid(MacAddress)} or
         * {@link #setBssidPattern(MacAddress, MacAddress)}.</li>
         *
         * @param bssid BSSID of the network.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setBssid(@NonNull MacAddress bssid) {
            checkNotNull(bssid);
            mBssidPatternMatcher = Pair.create(bssid, MATCH_EXACT_BSSID_PATTERN_MASK);
            return this;
        }

        /**
         * Specifies whether this represents an Enhanced Open (OWE) network.
         *
         * @param isEnhancedOpen {@code true} to indicate that the network uses enhanced open,
         *                       {@code false} otherwise.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setIsEnhancedOpen(boolean isEnhancedOpen) {
            mIsEnhancedOpen = isEnhancedOpen;
            return this;
        }

        /**
         * Set the ASCII WPA2 passphrase for this network. Needed for authenticating to
         * WPA2-PSK networks.
         *
         * @param passphrase passphrase of the network.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException if the passphrase is not ASCII encodable.
         */
        public @NonNull Builder setWpa2Passphrase(@NonNull String passphrase) {
            checkNotNull(passphrase);
            final CharsetEncoder asciiEncoder = StandardCharsets.US_ASCII.newEncoder();
            if (!asciiEncoder.canEncode(passphrase)) {
                throw new IllegalArgumentException("passphrase not ASCII encodable");
            }
            mWpa2PskPassphrase = passphrase;
            return this;
        }

        /**
         * Set the ASCII WPA3 passphrase for this network. Needed for authenticating to WPA3-SAE
         * networks.
         *
         * @param passphrase passphrase of the network.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException if the passphrase is not ASCII encodable.
         */
        public @NonNull Builder setWpa3Passphrase(@NonNull String passphrase) {
            checkNotNull(passphrase);
            final CharsetEncoder asciiEncoder = StandardCharsets.US_ASCII.newEncoder();
            if (!asciiEncoder.canEncode(passphrase)) {
                throw new IllegalArgumentException("passphrase not ASCII encodable");
            }
            mWpa3SaePassphrase = passphrase;
            return this;
        }

        /**
         * Set the associated enterprise configuration for this network. Needed for authenticating
         * to WPA2-EAP networks. See {@link WifiEnterpriseConfig} for description.
         *
         * @param enterpriseConfig Instance of {@link WifiEnterpriseConfig}.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setWpa2EnterpriseConfig(
                @NonNull WifiEnterpriseConfig enterpriseConfig) {
            checkNotNull(enterpriseConfig);
            mWpa2EnterpriseConfig = new WifiEnterpriseConfig(enterpriseConfig);
            return this;
        }

        /**
         * Set the associated enterprise configuration for this network. Needed for authenticating
         * to WPA3-Enterprise networks (standard and 192-bit security). See
         * {@link WifiEnterpriseConfig} for description. For 192-bit security networks, both the
         * client and CA certificates must be provided, and must be of type of either
         * sha384WithRSAEncryption (OID 1.2.840.113549.1.1.12) or ecdsa-with-SHA384
         * (OID 1.2.840.10045.4.3.3).
         *
         * @deprecated use {@link #setWpa3EnterpriseStandardModeConfig(WifiEnterpriseConfig)} or
         * {@link #setWpa3Enterprise192BitModeConfig(WifiEnterpriseConfig)} to specify
         * WPA3-Enterprise type explicitly.
         *
         * @param enterpriseConfig Instance of {@link WifiEnterpriseConfig}.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        @Deprecated
        public @NonNull Builder setWpa3EnterpriseConfig(
                @NonNull WifiEnterpriseConfig enterpriseConfig) {
            checkNotNull(enterpriseConfig);
            mWpa3EnterpriseConfig = new WifiEnterpriseConfig(enterpriseConfig);
            return this;
        }

        /**
         * Set the associated enterprise configuration for this network. Needed for authenticating
         * to standard WPA3-Enterprise networks. See {@link WifiEnterpriseConfig} for description.
         * For WPA3-Enterprise in 192-bit security mode networks,
         * see {@link #setWpa3Enterprise192BitModeConfig(WifiEnterpriseConfig)} for description.
         *
         * @param enterpriseConfig Instance of {@link WifiEnterpriseConfig}.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setWpa3EnterpriseStandardModeConfig(
                @NonNull WifiEnterpriseConfig enterpriseConfig) {
            checkNotNull(enterpriseConfig);
            mWpa3EnterpriseConfig = new WifiEnterpriseConfig(enterpriseConfig);
            mWpa3EnterpriseType = WPA3_ENTERPRISE_STANDARD;
            return this;
        }

        /**
         * Set the associated enterprise configuration for this network. Needed for authenticating
         * to WPA3-Enterprise in 192-bit security mode networks. See {@link WifiEnterpriseConfig}
         * for description. Both the client and CA certificates must be provided,
         * and must be of type of either sha384WithRSAEncryption with key length of 3072bit or
         * more (OID 1.2.840.113549.1.1.12), or ecdsa-with-SHA384 with key length of 384bit or
         * more (OID 1.2.840.10045.4.3.3).
         *
         * @param enterpriseConfig Instance of {@link WifiEnterpriseConfig}.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         * @throws IllegalArgumentException if the EAP type or certificates do not
         *                                  meet 192-bit mode requirements.
         */
        public @NonNull Builder setWpa3Enterprise192BitModeConfig(
                @NonNull WifiEnterpriseConfig enterpriseConfig) {
            checkNotNull(enterpriseConfig);
            if (enterpriseConfig.getEapMethod() != WifiEnterpriseConfig.Eap.TLS) {
                throw new IllegalArgumentException("The 192-bit mode network type must be TLS");
            }
            if (!WifiEnterpriseConfig.isSuiteBCipherCert(
                    enterpriseConfig.getClientCertificate())) {
                throw new IllegalArgumentException(
                    "The client certificate does not meet 192-bit mode requirements.");
            }
            if (!WifiEnterpriseConfig.isSuiteBCipherCert(
                    enterpriseConfig.getCaCertificate())) {
                throw new IllegalArgumentException(
                    "The CA certificate does not meet 192-bit mode requirements.");
            }

            mWpa3EnterpriseConfig = new WifiEnterpriseConfig(enterpriseConfig);
            mWpa3EnterpriseType = WPA3_ENTERPRISE_192_BIT;
            return this;
        }

        /**
         * Specifies whether this represents a hidden network.
         * <p>
         * <li>Setting this disallows the usage of {@link #setSsidPattern(PatternMatcher)} since
         * hidden networks need to be explicitly probed for.</li>
         * <li>If not set, defaults to false (i.e not a hidden network).</li>
         *
         * @param isHiddenSsid {@code true} to indicate that the network is hidden, {@code false}
         *                     otherwise.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setIsHiddenSsid(boolean isHiddenSsid) {
            mIsHiddenSSID = isHiddenSsid;
            return this;
        }

        /**
         * Specifies the band requested for this network.
         *
         * Only a single band can be requested. An app can file multiple callbacks concurrently
         * if they need to know about multiple bands.
         *
         * @param band The requested band.
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        public @NonNull Builder setBand(@WifiBand int band) {
            if (!validateBand(band)) {
                throw new IllegalArgumentException("Unexpected band in setBand : " + band);
            }
            mBand = band;
            return this;
        }

        /**
         * Specifies the preferred channels for this network. The channels set in the request will
         * be used to optimize the scan and connection.
         * <p>
         * <li>Should only be set to request local-only network</li>
         * <li>If not set, defaults to an empty array and device will do a full band scan.</li>
         *
         * @param channelFreqs an Array of the channels in MHz. The length of the array must not
         *                     exceed {@link WifiManager#getMaxNumberOfChannelsPerNetworkSpecifierRequest()}
         *
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        @NonNull public Builder setPreferredChannelsFrequenciesMhz(@NonNull int[] channelFreqs) {
            Objects.requireNonNull(channelFreqs);
            if (!validateChannelFrequencyInMhz(channelFreqs)) {
                throw new IllegalArgumentException("Invalid channel frequency in the input array");
            }
            mChannels = channelFreqs.clone();
            return this;
        }

        private void setSecurityParamsInWifiConfiguration(
                @NonNull WifiConfiguration configuration) {
            if (!TextUtils.isEmpty(mWpa2PskPassphrase)) { // WPA-PSK network.
                configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_PSK);
                // WifiConfiguration.preSharedKey needs quotes around ASCII password.
                configuration.preSharedKey = "\"" + mWpa2PskPassphrase + "\"";
            } else if (!TextUtils.isEmpty(mWpa3SaePassphrase)) { // WPA3-SAE network.
                configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_SAE);
                // WifiConfiguration.preSharedKey needs quotes around ASCII password.
                configuration.preSharedKey = "\"" + mWpa3SaePassphrase + "\"";
            } else if (mWpa2EnterpriseConfig != null) { // WPA-EAP network
                configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_EAP);
                configuration.enterpriseConfig = mWpa2EnterpriseConfig;
            } else if (mWpa3EnterpriseConfig != null) { // WPA3-Enterprise
                if (mWpa3EnterpriseType == WPA3_ENTERPRISE_AUTO
                        && mWpa3EnterpriseConfig.getEapMethod() == WifiEnterpriseConfig.Eap.TLS
                        && WifiEnterpriseConfig.isSuiteBCipherCert(
                        mWpa3EnterpriseConfig.getClientCertificate())
                        && WifiEnterpriseConfig.isSuiteBCipherCert(
                        mWpa3EnterpriseConfig.getCaCertificate())) {
                    // WPA3-Enterprise in 192-bit security mode
                    configuration.setSecurityParams(
                            WifiConfiguration.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT);
                } else if (mWpa3EnterpriseType == WPA3_ENTERPRISE_192_BIT) {
                    // WPA3-Enterprise in 192-bit security mode
                    configuration.setSecurityParams(
                            WifiConfiguration.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT);
                } else {
                    // WPA3-Enterprise
                    configuration.setSecurityParams(
                            WifiConfiguration.SECURITY_TYPE_EAP_WPA3_ENTERPRISE);
                }
                configuration.enterpriseConfig = mWpa3EnterpriseConfig;
            } else if (mIsEnhancedOpen) { // OWE network
                configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_OWE);
            } else { // Open network
                configuration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_OPEN);
            }
        }

        /**
         * Helper method to build WifiConfiguration object from the builder.
         * @return Instance of {@link WifiConfiguration}.
         */
        private WifiConfiguration buildWifiConfiguration() {
            final WifiConfiguration wifiConfiguration = new WifiConfiguration();
            // WifiConfiguration.SSID needs quotes around unicode SSID.
            if (mSsidPatternMatcher.getType() == PatternMatcher.PATTERN_LITERAL) {
                wifiConfiguration.SSID = "\"" + mSsidPatternMatcher.getPath() + "\"";
            }
            if (mBssidPatternMatcher.second == MATCH_EXACT_BSSID_PATTERN_MASK) {
                wifiConfiguration.BSSID = mBssidPatternMatcher.first.toString();
            }
            setSecurityParamsInWifiConfiguration(wifiConfiguration);
            wifiConfiguration.hiddenSSID = mIsHiddenSSID;
            return wifiConfiguration;
        }

        private boolean hasSetAnyPattern() {
            return mSsidPatternMatcher != null || mBssidPatternMatcher != null;
        }

        private void setMatchAnyPatternIfUnset() {
            if (mSsidPatternMatcher == null) {
                mSsidPatternMatcher = new PatternMatcher(MATCH_ALL_SSID_PATTERN_PATH,
                        PatternMatcher.PATTERN_SIMPLE_GLOB);
            }
            if (mBssidPatternMatcher == null) {
                mBssidPatternMatcher = MATCH_ALL_BSSID_PATTERN;
            }
        }

        private boolean hasSetMatchNonePattern() {
            if (mSsidPatternMatcher.getType() != PatternMatcher.PATTERN_PREFIX
                    && mSsidPatternMatcher.getPath().equals(MATCH_EMPTY_SSID_PATTERN_PATH)) {
                return true;
            }
            if (mBssidPatternMatcher.equals(MATCH_NO_BSSID_PATTERN1)) {
                return true;
            }
            if (mBssidPatternMatcher.equals(MATCH_NO_BSSID_PATTERN2)) {
                return true;
            }
            return false;
        }

        private boolean hasSetMatchAllPattern() {
            if ((mSsidPatternMatcher.match(MATCH_EMPTY_SSID_PATTERN_PATH))
                    && mBssidPatternMatcher.equals(MATCH_ALL_BSSID_PATTERN)) {
                return true;
            }
            return false;
        }

        private void validateSecurityParams() {
            int numSecurityTypes = 0;
            numSecurityTypes += mIsEnhancedOpen ? 1 : 0;
            numSecurityTypes += !TextUtils.isEmpty(mWpa2PskPassphrase) ? 1 : 0;
            numSecurityTypes += !TextUtils.isEmpty(mWpa3SaePassphrase) ? 1 : 0;
            numSecurityTypes += mWpa2EnterpriseConfig != null ? 1 : 0;
            numSecurityTypes += mWpa3EnterpriseConfig != null ? 1 : 0;
            if (numSecurityTypes > 1) {
                throw new IllegalStateException("only one of setIsEnhancedOpen, setWpa2Passphrase,"
                        + "setWpa3Passphrase, setWpa2EnterpriseConfig or setWpa3EnterpriseConfig"
                        + " can be invoked for network specifier");
            }
        }

        /**
         * Create a specifier object used to request a Wi-Fi network. The generated
         * {@link NetworkSpecifier} should be used in
         * {@link NetworkRequest.Builder#setNetworkSpecifier(NetworkSpecifier)} when building
         * the {@link NetworkRequest}.
         *
         *<p>
         * When using with {@link ConnectivityManager#requestNetwork(NetworkRequest,
         * NetworkCallback)} or variants, note that some devices may not support requesting a
         * network with all combinations of specifier members. For example, some devices may only
         * support requesting local-only networks (networks without the
         * {@link NetworkCapabilities#NET_CAPABILITY_INTERNET} capability), or not support
         * requesting a particular band. However, there are no restrictions when using
         * {@link ConnectivityManager#registerNetworkCallback(NetworkRequest, NetworkCallback)}
         * or other similar methods which monitor but do not request networks.
         *
         * If the device can't support a request, the app will receive a call to
         * {@link NetworkCallback#onUnavailable()}.
         *</p>
         *
         *<p>
         * When requesting a local-only network, apps can set a combination of network match params:
         * <li> SSID Pattern using {@link #setSsidPattern(PatternMatcher)} OR Specific SSID using
         * {@link #setSsid(String)}. </li>
         * AND/OR
         * <li> BSSID Pattern using {@link #setBssidPattern(MacAddress, MacAddress)} OR Specific
         * BSSID using {@link #setBssid(MacAddress)} </li>
         * to trigger connection to a network that matches the set params.
         * The system will find the set of networks matching the request and present the user
         * with a system dialog which will allow the user to select a specific Wi-Fi network to
         * connect to or to deny the request.
         *
         * To protect user privacy, some limitations to the ability of matching patterns apply.
         * In particular, when the system brings up a network to satisfy a {@link NetworkRequest}
         * from some app, the system reserves the right to decline matching the SSID pattern to
         * the real SSID of the network for other apps than the app that requested the network, and
         * not send those callbacks even if the SSID matches the requested pattern.
         *</p>
         *
         * For example:
         * To connect to an open network with a SSID prefix of "test" and a BSSID OUI of "10:03:23":
         *
         * <pre>{@code
         * final NetworkSpecifier specifier =
         *      new Builder()
         *      .setSsidPattern(new PatternMatcher("test", PatternMatcher.PATTERN_PREFIX))
         *      .setBssidPattern(MacAddress.fromString("10:03:23:00:00:00"),
         *                       MacAddress.fromString("ff:ff:ff:00:00:00"))
         *      .build()
         * final NetworkRequest request =
         *      new NetworkRequest.Builder()
         *      .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
         *      .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
         *      .setNetworkSpecifier(specifier)
         *      .build();
         * final ConnectivityManager connectivityManager =
         *      context.getSystemService(Context.CONNECTIVITY_SERVICE);
         * final NetworkCallback networkCallback = new NetworkCallback() {
         *      ...
         *      {@literal @}Override
         *      void onAvailable(...) {}
         *      // etc.
         * };
         * connectivityManager.requestNetwork(request, networkCallback);
         * }</pre>
         *
         * @return Instance of {@link NetworkSpecifier}.
         * @throws IllegalStateException on invalid params set.
         */
        public @NonNull WifiNetworkSpecifier build() {
            if (!hasSetAnyPattern() && mBand == UNSPECIFIED) {
                throw new IllegalStateException("one of setSsidPattern/setSsid/setBssidPattern/"
                        + "setBssid/setBand should be invoked for specifier");
            }
            setMatchAnyPatternIfUnset();
            if (hasSetMatchNonePattern()) {
                throw new IllegalStateException("cannot set match-none pattern for specifier");
            }
            if (hasSetMatchAllPattern() && mBand == UNSPECIFIED) {
                throw new IllegalStateException("cannot set match-all pattern for specifier");
            }
            if (mIsHiddenSSID && mSsidPatternMatcher.getType() != PatternMatcher.PATTERN_LITERAL) {
                throw new IllegalStateException("setSsid should also be invoked when "
                        + "setIsHiddenSsid is invoked for network specifier");
            }
            if (mChannels.length != 0 && mBand != UNSPECIFIED) {
                throw new IllegalStateException("cannot setPreferredChannelsFrequencyInMhz with "
                        + "setBand together");
            }
            validateSecurityParams();

            return new WifiNetworkSpecifier(
                    mSsidPatternMatcher,
                    mBssidPatternMatcher,
                    mBand,
                    buildWifiConfiguration(),
                    mChannels);
        }
    }

    /**
     * SSID pattern match specified by the app.
     * @hide
     */
    public final PatternMatcher ssidPatternMatcher;

    /**
     * BSSID pattern match specified by the app.
     * Pair of <BaseAddress, Mask>.
     * @hide
     */
    public final Pair<MacAddress, MacAddress> bssidPatternMatcher;

    /**
     * The band for this Wi-Fi network.
     */
    @WifiBand private final int mBand;

    private final int[] mChannelFreqs;

    /**
     * Security credentials for the network.
     * <p>
     * Note: {@link WifiConfiguration#SSID} & {@link WifiConfiguration#BSSID} fields from
     * WifiConfiguration are not used. Instead we use the {@link #ssidPatternMatcher} &
     * {@link #bssidPatternMatcher} fields embedded directly
     * within {@link WifiNetworkSpecifier}.
     * @hide
     */
    public final WifiConfiguration wifiConfiguration;

    /** @hide */
    public WifiNetworkSpecifier() throws IllegalAccessException {
        throw new IllegalAccessException("Use the builder to create an instance");
    }

    /** @hide */
    public WifiNetworkSpecifier(@NonNull PatternMatcher ssidPatternMatcher,
            @NonNull Pair<MacAddress, MacAddress> bssidPatternMatcher,
            @WifiBand int band,
            @NonNull WifiConfiguration wifiConfiguration,
            @NonNull int[] channelFreqs) {
        checkNotNull(ssidPatternMatcher);
        checkNotNull(bssidPatternMatcher);
        checkNotNull(wifiConfiguration);

        this.ssidPatternMatcher = ssidPatternMatcher;
        this.bssidPatternMatcher = bssidPatternMatcher;
        this.mBand = band;
        this.wifiConfiguration = wifiConfiguration;
        this.mChannelFreqs = channelFreqs;
    }

    /**
     * The band for this Wi-Fi network specifier.
     */
    @WifiBand public int getBand() {
        return mBand;
    }

    /**
     * The preferred channels fot this network specifier.
     * @see Builder#setPreferredChannelsFrequenciesMhz(int[])
     */
    @NonNull public int[] getPreferredChannelFrequenciesMhz() {
        return mChannelFreqs.clone();
    }

    public static final @NonNull Creator<WifiNetworkSpecifier> CREATOR =
            new Creator<WifiNetworkSpecifier>() {
                @Override
                public WifiNetworkSpecifier createFromParcel(Parcel in) {
                    PatternMatcher ssidPatternMatcher = in.readParcelable(/* classLoader */null);
                    MacAddress baseAddress = in.readParcelable(null);
                    MacAddress mask = in.readParcelable(null);
                    Pair<MacAddress, MacAddress> bssidPatternMatcher =
                            Pair.create(baseAddress, mask);
                    int band = in.readInt();
                    WifiConfiguration wifiConfiguration = in.readParcelable(null);
                    int[] mChannels = in.createIntArray();
                    return new WifiNetworkSpecifier(ssidPatternMatcher, bssidPatternMatcher, band,
                            wifiConfiguration, mChannels);
                }

                @Override
                public WifiNetworkSpecifier[] newArray(int size) {
                    return new WifiNetworkSpecifier[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(ssidPatternMatcher, flags);
        dest.writeParcelable(bssidPatternMatcher.first, flags);
        dest.writeParcelable(bssidPatternMatcher.second, flags);
        dest.writeInt(mBand);
        dest.writeParcelable(wifiConfiguration, flags);
        dest.writeIntArray(mChannelFreqs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                ssidPatternMatcher.getPath(), ssidPatternMatcher.getType(), bssidPatternMatcher,
                mBand, wifiConfiguration.allowedKeyManagement, Arrays.hashCode(mChannelFreqs));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WifiNetworkSpecifier)) {
            return false;
        }
        WifiNetworkSpecifier lhs = (WifiNetworkSpecifier) obj;
        return Objects.equals(this.ssidPatternMatcher.getPath(),
                    lhs.ssidPatternMatcher.getPath())
                && Objects.equals(this.ssidPatternMatcher.getType(),
                    lhs.ssidPatternMatcher.getType())
                && Objects.equals(this.bssidPatternMatcher,
                    lhs.bssidPatternMatcher)
                && this.mBand == lhs.mBand
                && Objects.equals(this.wifiConfiguration.allowedKeyManagement,
                    lhs.wifiConfiguration.allowedKeyManagement)
                && Arrays.equals(mChannelFreqs, lhs.mChannelFreqs);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("WifiNetworkSpecifier [")
                .append(", SSID Match pattern=").append(ssidPatternMatcher)
                .append(", BSSID Match pattern=").append(bssidPatternMatcher)
                .append(", SSID=").append(wifiConfiguration.SSID)
                .append(", BSSID=").append(wifiConfiguration.BSSID)
                .append(", band=").append(mBand)
                .append("]")
                .toString();
    }

    /** @hide */
    @Override
    public boolean canBeSatisfiedBy(NetworkSpecifier other) {
        if (other instanceof WifiNetworkAgentSpecifier) {
            return ((WifiNetworkAgentSpecifier) other).satisfiesNetworkSpecifier(this);
        }
        // Specific requests are checked for equality although testing for equality of 2 patterns do
        // not make much sense!
        return equals(other);
    }

    /** @hide */
    @Override
    @Nullable
    public NetworkSpecifier redact() {
        if (!SdkLevel.isAtLeastS()) return this;

        return new Builder().setBand(mBand).build();
    }
}
