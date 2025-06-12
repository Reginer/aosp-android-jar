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
package android.net.wifi;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;

import com.android.wifi.flags.Flags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Supports to parse 2 types of Wifi Uri
 *
 * <p>1. Standard Wi-Fi Easy Connect (DPP) bootstrapping information or 2. ZXing reader library's
 * Wi-Fi Network config format described in
 * https://github.com/zxing/zxing/wiki/Barcode-Contents#wi-fi-network-config-android-ios-11
 *
 * <p>ZXing reader library's Wi-Fi Network config format example:
 *
 * <p>WIFI:T:WPA;S:mynetwork;P:mypass;;
 *
 * <p>parameter Example Description T WPA Authentication type; can be WEP, WPA, SAE or 'nopass' for
 * no password. Or, omit for no password. S mynetwork Network SSID. Required. Enclose in double
 * quotes if it is an ASCII name, but could be interpreted as hex (i.e. "ABCD") P mypass Password,
 * ignored if T is "nopass" (in which case it may be omitted). Enclose in double quotes if it is an
 * ASCII name, but could be interpreted as hex (i.e. "ABCD") H true Optional. True if the network
 * SSID is hidden.
 * @hide
 */
@FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
@SystemApi
public class WifiUriParser {
    static final String TAG = "WifiUriParser";
    static final String SCHEME_DPP = "DPP";
    static final String SCHEME_ZXING_WIFI_NETWORK_CONFIG = "WIFI";
    static final String PREFIX_DPP = "DPP:";
    static final String PREFIX_ZXING_WIFI_NETWORK_CONFIG = "WIFI:";

    static final String PREFIX_DPP_PUBLIC_KEY = "K:";
    static final String PREFIX_DPP_INFORMATION = "I:";

    static final String PREFIX_ZXING_SECURITY = "T:";
    static final String PREFIX_ZXING_SSID = "S:";
    static final String PREFIX_ZXING_PASSWORD = "P:";
    static final String PREFIX_ZXING_HIDDEN_SSID = "H:";
    static final String PREFIX_ZXING_TRANSITION_DISABLE = "R:";

    static final String DELIMITER_QR_CODE = ";";
    static final char URI_DELIMITER_CHAR = ';';

    // Ignores password if security is SECURITY_NO_PASSWORD or absent
    static final String SECURITY_NO_PASSWORD = "nopass"; // open network or OWE
    static final String SECURITY_WEP = "WEP";
    static final String SECURITY_WPA_PSK = "WPA";
    static final String SECURITY_SAE = "SAE";

    private static final String SECURITY_ADB = "ADB";

    private WifiUriParser() {}

    /**
     * @hide
     */
    @VisibleForTesting
    public static boolean mockableIsFlagNewUriParsingForEscapeCharacterEnabled() {
        return Flags.newUriParsingForEscapeCharacter();
    }

    /**
     * Returns parsed result from given uri.
     *
     * @param uri URI of the configuration that was obtained out of band(QR code scanning, BLE).
     * @throws IllegalArgumentException when parse failed.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @NonNull
    public static UriParserResults parseUri(@NonNull String uri) {
        if (TextUtils.isEmpty(uri)) {
            throw new IllegalArgumentException("Empty Wifi Uri");
        }

        if (uri.startsWith(PREFIX_DPP)) {
            return parseWifiDppUri(uri);
        } else if (uri.startsWith(PREFIX_ZXING_WIFI_NETWORK_CONFIG)) {
            return parseZxingWifiUriParser(uri);
        } else {
            throw new IllegalArgumentException("Unsupport scheme (Not start with "
                    + PREFIX_DPP + "/" + PREFIX_ZXING_WIFI_NETWORK_CONFIG + ")");
        }
    }

    /** Parses Wi-Fi Easy Connect (DPP) Wifi Uri string */
    private static UriParserResults parseWifiDppUri(String uri) throws IllegalArgumentException {
        List<String> keyValueList = getKeyValueList(uri, PREFIX_DPP, DELIMITER_QR_CODE);

        String publicKey = getValueOrNull(keyValueList, PREFIX_DPP_PUBLIC_KEY);
        if (TextUtils.isEmpty(publicKey)) {
            throw new IllegalArgumentException("Invalid format, publicKey is empty");
        }

        String information = getValueOrNull(keyValueList, PREFIX_DPP_INFORMATION);

        return new UriParserResults(UriParserResults.URI_SCHEME_DPP, publicKey, information, null);
    }

    /** Parses ZXing reader library's Wi-Fi Network config format */
    private static UriParserResults parseZxingWifiUriParser(@NonNull String uri)
            throws IllegalArgumentException {
        WifiConfiguration config = null;
        String security = null;
        String ssid = null;
        String password = null;
        String hiddenSsidString = null;
        String transitionDisabledValue = null;
        if (mockableIsFlagNewUriParsingForEscapeCharacterEnabled()) {
            String wifiQr = uri.substring(PREFIX_ZXING_WIFI_NETWORK_CONFIG.length());
            Pair<Integer, String> zxingUriElement;
            int start = 0;
            while (start < wifiQr.length()) {
                String value = wifiQr.substring(start);
                char ch = wifiQr.charAt(start);
                if (value.startsWith(PREFIX_ZXING_SSID)) {
                    zxingUriElement = getZxingUriElement(value, PREFIX_ZXING_SSID);
                    ssid = zxingUriElement.second;
                    start = start + zxingUriElement.first + PREFIX_ZXING_SSID.length();
                } else if (value.startsWith(PREFIX_ZXING_SECURITY)) {
                    zxingUriElement = getZxingUriElement(value, PREFIX_ZXING_SECURITY);
                    security = zxingUriElement.second;
                    start = start + zxingUriElement.first + PREFIX_ZXING_SECURITY.length();
                } else if (value.startsWith(PREFIX_ZXING_PASSWORD)) {
                    zxingUriElement = getZxingUriElement(value, PREFIX_ZXING_PASSWORD);
                    password = zxingUriElement.second;
                    start = start + zxingUriElement.first + PREFIX_ZXING_PASSWORD.length();
                } else if (value.startsWith(PREFIX_ZXING_HIDDEN_SSID)) {
                    zxingUriElement = getZxingUriElement(value, PREFIX_ZXING_HIDDEN_SSID);
                    hiddenSsidString = zxingUriElement.second;
                    start = start + zxingUriElement.first
                            + PREFIX_ZXING_HIDDEN_SSID.length();
                } else if (value.startsWith(PREFIX_ZXING_TRANSITION_DISABLE)) {
                    zxingUriElement = getZxingUriElement(value,
                            PREFIX_ZXING_TRANSITION_DISABLE);
                    transitionDisabledValue = zxingUriElement.second;
                    start = start + zxingUriElement.first
                            + PREFIX_ZXING_TRANSITION_DISABLE.length();
                } else if (Character.isWhitespace(ch) || ch == URI_DELIMITER_CHAR) {
                    // Skip space and DELIMITER_QR_CODE in URI when detecting prefix
                    start++;
                } else {
                    zxingUriElement = getZxingUriElement(value, "" /* empty prefix */);
                    String unsupportedUriPrefix = zxingUriElement.second;
                    Log.i(TAG, "UnsupportedUriPrefix Found:" + unsupportedUriPrefix);
                    start = start + zxingUriElement.first;
                }
            }
        } else {
            List<String> keyValueList =
                    getKeyValueList(uri, PREFIX_ZXING_WIFI_NETWORK_CONFIG, DELIMITER_QR_CODE);
            security = getValueOrNull(keyValueList, PREFIX_ZXING_SECURITY);
            ssid = getValueOrNull(keyValueList, PREFIX_ZXING_SSID);
            password = getValueOrNull(keyValueList, PREFIX_ZXING_PASSWORD);
            hiddenSsidString = getValueOrNull(keyValueList, PREFIX_ZXING_HIDDEN_SSID);
            transitionDisabledValue = getValueOrNull(keyValueList,
                    PREFIX_ZXING_TRANSITION_DISABLE);

            // "\", ";", "," and ":" are escaped with a backslash "\", should remove at first
            security = removeBackSlash(security);
            ssid = removeBackSlash(ssid);
            password = removeBackSlash(password);
        }
        boolean hiddenSsid = "true".equalsIgnoreCase(hiddenSsidString);
        boolean isTransitionDisabled = "1".equalsIgnoreCase(transitionDisabledValue);
        if (isValidConfig(security, ssid, password)) {
            config = generateWifiConfiguration(
                        security, ssid, password, hiddenSsid, WifiConfiguration.INVALID_NETWORK_ID,
                        isTransitionDisabled);
        }

        if (config == null) {
            throw new IllegalArgumentException("Invalid format, can't generate WifiConfiguration");
        }
        return new UriParserResults(UriParserResults.URI_SCHEME_ZXING_WIFI_NETWORK_CONFIG,
                null, null, config);
    }

    private static Pair<Integer, String> getZxingUriElement(@NonNull String uriSubString,
            @NonNull String prefixString) {
        StringBuilder sb = new StringBuilder();
        int numberOfCharHandled = 0;
        boolean isPreviousCharEscaped = false;
        for (int i = prefixString.length(); i < uriSubString.length(); i++) {
            char ch = uriSubString.charAt(i);
            numberOfCharHandled++;
            if (ch == '\\') {
                if (isPreviousCharEscaped) {
                    // The '\' is the part of the uri field
                    sb.append(ch);
                    isPreviousCharEscaped = false;
                    continue;
                }
                isPreviousCharEscaped = true;
            } else if (ch == URI_DELIMITER_CHAR) {
                if (!isPreviousCharEscaped) {
                    break;
                }
                sb.append(ch);
                isPreviousCharEscaped = false;
            } else {
                sb.append(ch);
                isPreviousCharEscaped = false;
            }
        }
        return new Pair<>(numberOfCharHandled, sb.toString());
    }

    /**
     * Splits key/value pairs from uri
     *
     * @param uri the Wifi Uri raw string
     * @param prefixUri the string before all key/value pairs in uri
     * @param delimiter the string to split key/value pairs, can't contain a backslash
     * @return a list contains string of key/value (e.g. K:key1)
     */
    private static List<String> getKeyValueList(String uri, String prefixUri, String delimiter) {
        String keyValueString = uri.substring(prefixUri.length());

        // Should not treat \delimiter as a delimiter
        String regex = "(?<!\\\\)" + Pattern.quote(delimiter);

        return Arrays.asList(keyValueString.split(regex));
    }

    private static String getValueOrNull(List<String> keyValueList, String prefix) {
        for (String keyValue : keyValueList) {
            String strippedKeyValue = keyValue.stripLeading();
            if (strippedKeyValue.startsWith(prefix)) {
                return strippedKeyValue.substring(prefix.length());
            }
        }

        return null;
    }

    @VisibleForTesting
    static String removeBackSlash(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean backSlash = false;
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch != '\\') {
                sb.append(ch);
                backSlash = false;
            } else {
                if (backSlash) {
                    sb.append(ch);
                    backSlash = false;
                    continue;
                }

                backSlash = true;
            }
        }

        return sb.toString();
    }

    private static String addQuotation(String input) {
        if (TextUtils.isEmpty(input)) {
            return "";
        }

        if (!mockableIsFlagNewUriParsingForEscapeCharacterEnabled()) {
            if (input.length() >= 2 && input.startsWith("\"") && input.endsWith("\"")) {
                return input;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(input).append("\"");
        return sb.toString();
    }

    private static boolean isValidConfig(String security, String ssid, String preSharedKey) {
        if (!TextUtils.isEmpty(security) && !SECURITY_NO_PASSWORD.equals(security)) {
            if (TextUtils.isEmpty(preSharedKey)) {
                return false;
            }
        }

        if (TextUtils.isEmpty(ssid)) {
            return false;
        }

        return true;
    }

    /**
     * This is a simplified method from {@code WifiConfigController.getConfig()}
     *
     * @return WifiConfiguration from parsing result
     */
    private static WifiConfiguration generateWifiConfiguration(
            String security, String ssid, String preSharedKey, boolean hiddenSsid, int networkId,
            boolean isTransitionDisabled) {
        final WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = addQuotation(ssid);
        wifiConfiguration.hiddenSSID = hiddenSsid;
        wifiConfiguration.networkId = networkId;

        if (TextUtils.isEmpty(security) || SECURITY_NO_PASSWORD.equals(security)) {
            wifiConfiguration.setSecurityParams(
                    Arrays.asList(
                            SecurityParams.createSecurityParamsBySecurityType(
                                    WifiConfiguration.SECURITY_TYPE_OPEN),
                            SecurityParams.createSecurityParamsBySecurityType(
                                    WifiConfiguration.SECURITY_TYPE_OWE)));
            return wifiConfiguration;
        }

        if (security.startsWith(SECURITY_WEP)) {
            wifiConfiguration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_WEP);

            // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
            final int length = preSharedKey.length();
            if ((length == 10 || length == 26 || length == 58)
                    && preSharedKey.matches("[0-9A-Fa-f]*")) {
                wifiConfiguration.wepKeys[0] = preSharedKey;
            } else {
                wifiConfiguration.wepKeys[0] = addQuotation(preSharedKey);
            }
        } else if (security.startsWith(SECURITY_WPA_PSK)) {
            List<SecurityParams> securityParamsList = new ArrayList<>();
            SecurityParams scannedSecurityParam = SecurityParams.createSecurityParamsBySecurityType(
                            WifiConfiguration.SECURITY_TYPE_PSK);
            securityParamsList.add(scannedSecurityParam);
            if (isTransitionDisabled) {
                scannedSecurityParam.setEnabled(false);
                securityParamsList.add(
                        SecurityParams.createSecurityParamsBySecurityType(
                                WifiConfiguration.SECURITY_TYPE_SAE));
            }
            wifiConfiguration.setSecurityParams(securityParamsList);

            if (preSharedKey.matches("[0-9A-Fa-f]{64}")) {
                wifiConfiguration.preSharedKey = preSharedKey;
            } else {
                wifiConfiguration.preSharedKey = addQuotation(preSharedKey);
            }
        } else if (security.startsWith(SECURITY_SAE)) {
            wifiConfiguration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_SAE);
            if (preSharedKey.length() != 0) {
                wifiConfiguration.preSharedKey = addQuotation(preSharedKey);
            }
        } else if (security.startsWith(SECURITY_ADB)) {
            Log.i(TAG, "Security key: ADB, the ssid and passphrase should NOT add quotation");
            wifiConfiguration.SSID = ssid;
            if (preSharedKey.length() != 0) {
                wifiConfiguration.preSharedKey = preSharedKey;
            }
        } else {
            throw new IllegalArgumentException("Unsupported security");
        }

        return wifiConfiguration;
    }
}
