/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.net.wifi.p2p;

import android.annotation.NonNull;
import android.compat.annotation.UnsupportedAppUsage;
import android.net.wifi.OuiKeyedData;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;

import java.util.Collections;
import java.util.List;

/**
 * A class representing a Wi-Fi p2p provisional discovery request/response
 * See {@link #WifiP2pProvDiscEvent} for supported types
 *
 * @hide
 */
public class WifiP2pProvDiscEvent {

    private static final String TAG = "WifiP2pProvDiscEvent";

    public static final int WPS_PBC_REQ     = 1;
    public static final int WPS_PBC_RSP     = 2;
    public static final int WPS_ENTER_PIN   = 3;
    public static final int WPS_SHOW_PIN    = 4;
    public static final int PAIRING_BOOTSTRAPPING_OPPORTUNISTIC_REQ = 5;
    public static final int PAIRING_BOOTSTRAPPING_OPPORTUNISTIC_RSP = 6;
    public static final int PAIRING_BOOTSTRAPPING_ENTER_PIN = 7;
    public static final int PAIRING_BOOTSTRAPPING_SHOW_PIN = 8;
    public static final int PAIRING_BOOTSTRAPPING_ENTER_PASSPHRASE = 9;
    public static final int PAIRING_BOOTSTRAPPING_SHOW_PASSPHRASE = 10;

    /*
     * One of WPS_PBC_REQ, WPS_PBC_RSP, WPS_ENTER_PIN, WPS_SHOW_PIN,
     * PAIRING_BOOTSTRAPPING_OPPORTUNISTIC_REQ, PAIRING_BOOTSTRAPPING_OPPORTUNISTIC_RSP,
     * PAIRING_BOOTSTRAPPING_ENTER_PIN, PAIRING_BOOTSTRAPPING_SHOW_PIN,
     * PAIRING_BOOTSTRAPPING_ENTER_PASSPHRASE or PAIRING_BOOTSTRAPPING_SHOW_PASSPHRASE.
     */
    @UnsupportedAppUsage
    public int event;

    @UnsupportedAppUsage
    public WifiP2pDevice device;

    /*
     * Valid when event = WPS_SHOW_PIN
     */
    @UnsupportedAppUsage
    public String wpsPin;

    /*
     * Valid when event = PAIRING_BOOTSTRAPPING_SHOW_PIN or PAIRING_BOOTSTRAPPING_SHOW_PASSPHRASE.
     */
    @UnsupportedAppUsage
    public String pairingPinOrPassphrase;

    /** List of {@link OuiKeyedData} providing vendor-specific configuration data. */
    private @NonNull List<OuiKeyedData> mVendorData = Collections.emptyList();

    /**
     * Return the vendor-provided configuration data, if it exists. See also {@link
     * #setVendorData(List)}
     *
     * @return Vendor configuration data, or empty list if it does not exist.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @NonNull
    public List<OuiKeyedData> getVendorData() {
        if (!SdkLevel.isAtLeastV()) {
            throw new UnsupportedOperationException();
        }
        return mVendorData;
    }

    @UnsupportedAppUsage
    public WifiP2pProvDiscEvent() {
        device = new WifiP2pDevice();
    }

    /**
     * Set additional vendor-provided configuration data.
     *
     * @param vendorData List of {@link android.net.wifi.OuiKeyedData} containing the
     *                   vendor-provided configuration data. Note that multiple elements with
     *                   the same OUI are allowed.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @NonNull
    public void setVendorData(@NonNull List<OuiKeyedData> vendorData) {
        if (!SdkLevel.isAtLeastV()) {
            throw new UnsupportedOperationException();
        }
        if (vendorData == null) {
            throw new IllegalArgumentException("setVendorData received a null value");
        }
        mVendorData = vendorData;
    }

    /**
     * @param string formats supported include
     *
     *  P2P-PROV-DISC-PBC-REQ 42:fc:89:e1:e2:27
     *  P2P-PROV-DISC-PBC-RESP 02:12:47:f2:5a:36
     *  P2P-PROV-DISC-ENTER-PIN 42:fc:89:e1:e2:27
     *  P2P-PROV-DISC-SHOW-PIN 42:fc:89:e1:e2:27 44490607
     *
     *  Note: The events formats can be looked up in the wpa_supplicant code
     * @hide
     */
    public WifiP2pProvDiscEvent(String string) throws IllegalArgumentException {
        String[] tokens = string.split(" ");

        if (tokens.length < 2) {
            throw new IllegalArgumentException("Malformed event " + string);
        }

        if (tokens[0].endsWith("PBC-REQ")) {
            event = WPS_PBC_REQ;
        } else if (tokens[0].endsWith("PBC-RESP")) {
            event = WPS_PBC_RSP;
        } else if (tokens[0].endsWith("ENTER-PIN")) {
            event = WPS_ENTER_PIN;
        } else if (tokens[0].endsWith("SHOW-PIN")) {
            event = WPS_SHOW_PIN;
        } else {
            throw new IllegalArgumentException("Malformed event " + string);
        }


        device = new WifiP2pDevice();
        device.deviceAddress = tokens[1];

        if (event == WPS_SHOW_PIN) {
            wpsPin = tokens[2];
        }
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append(device);
        sbuf.append("\n event: ").append(event);
        sbuf.append("\n wpsPin: ").append(wpsPin);
        sbuf.append("\n PairingPinOrPassphrase: ").append(pairingPinOrPassphrase);
        sbuf.append("\n vendorData: ").append(mVendorData);
        return sbuf.toString();
    }
}
