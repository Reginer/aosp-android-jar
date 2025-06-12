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

package android.net.wifi.rtt;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiSsid;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.modules.utils.build.SdkLevel;
import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Pre-association security negotiation (PASN) configuration.
 * <p>
 * PASN configuration in IEEE 802.11az focuses on securing the ranging process before a device
 * fully associates with a Wi-Fi network. IEEE 802.11az supports various based AKMs as in
 * {@code AKM_*} for PASN and cipher as in {@code CIPHER_*}. Password is also another input to
 * some base AKMs.
 * <p>
 * Once PASN is initiated, the AP and the client device exchange messages to authenticate each
 * other and establish security keys. This process ensures that only authorized devices can
 * participate in ranging.
 * <p>
 * After successful PASN authentication, ranging operations are performed using the established
 * secure channel. This protects the ranging measurements from eavesdropping and tampering.
 * <p>
 * The keys derived during the PASN process are used to protect the LTFs exchanged during ranging.
 * This ensures that the LTFs are encrypted and authenticated, preventing unauthorized access
 * and manipulation.
 */
@FlaggedApi(Flags.FLAG_SECURE_RANGING)
public final class PasnConfig implements Parcelable {

    /**
     * Various base Authentication and Key Management (AKM) protocol supported by the PASN.
     *
     * @hide
     */
    @IntDef(prefix = {"AKM_"}, flag = true, value = {
            AKM_NONE,
            AKM_PASN,
            AKM_SAE,
            AKM_FT_EAP_SHA256,
            AKM_FT_PSK_SHA256,
            AKM_FT_EAP_SHA384,
            AKM_FT_PSK_SHA384,
            AKM_FILS_EAP_SHA256,
            AKM_FILS_EAP_SHA384})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AkmType {
    }

    /**
     *  No authentication and key management.
     */
    public static final int AKM_NONE = 0;
    /**
     * Pre-association security negotiation (PASN).
     */
    public static final int AKM_PASN = 1 << 0;
    /**
     * Simultaneous authentication of equals (SAE).
     */
    public static final int AKM_SAE = 1 << 1;
    /**
     * Fast BSS Transition (FT) with Extensible Authentication Protocol (EAP) and SHA-256.
     */
    public static final int AKM_FT_EAP_SHA256 = 1 << 2;
    /**
     * Fast BSS Transition (FT) with Pre-Shared Key (PSK) and SHA-256.
     */
    public static final int AKM_FT_PSK_SHA256 = 1 << 3;
    /**
     * Fast BSS Transition (FT) with Extensible Authentication Protocol (EAP) and SHA-384.
     */
    public static final int AKM_FT_EAP_SHA384 = 1 << 4;
    /**
     * Fast BSS Transition (FT) with Pre-Shared Key (PSK) and SHA-384.
     */
    public static final int AKM_FT_PSK_SHA384 = 1 << 5;
    /**
     * Fast Initial Link Setup (FILS) with Extensible Authentication Protocol (EAP) and SHA-256.
     */
    public static final int AKM_FILS_EAP_SHA256 = 1 << 6;
    /**
     * Fast Initial Link Setup (FILS) with Extensible Authentication Protocol (EAP) and SHA-384.
     */
    public static final int AKM_FILS_EAP_SHA384 = 1 << 7;

    /**
     * @hide
     */
    private static final Map<String, Integer> sStringToAkm = new HashMap<>();

    static {
        sStringToAkm.put("None", AKM_NONE);
        sStringToAkm.put("PASN-", AKM_PASN);
        // Transition mode. e.g. "[RSN-SAE+SAE_EXT_KEY-CCMP]"
        sStringToAkm.put("SAE+", AKM_SAE);
        // SAE mode only. e.g. "[RSN-PSK+SAE-CCMP]"
        sStringToAkm.put("SAE-", AKM_SAE);
        sStringToAkm.put("EAP-FILS-SHA256-", AKM_FILS_EAP_SHA256);
        sStringToAkm.put("EAP-FILS-SHA384-", AKM_FILS_EAP_SHA384);
        sStringToAkm.put("FT/EAP-", AKM_FT_EAP_SHA256);
        sStringToAkm.put("FT/PSK-", AKM_FT_PSK_SHA256);
        sStringToAkm.put("EAP-FT-SHA384-", AKM_FT_EAP_SHA384);
        sStringToAkm.put("FT/PSK-SHA384-", AKM_FT_PSK_SHA384);
    }

    /**
     * Pairwise cipher used for encryption.
     *
     * @hide
     */
    @IntDef(prefix = {"CIPHER_"}, flag = true, value = {
            CIPHER_NONE,
            CIPHER_CCMP_128,
            CIPHER_CCMP_256,
            CIPHER_GCMP_128,
            CIPHER_GCMP_256})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Cipher {
    }

    /**
     * No encryption.
     */
    public static final int CIPHER_NONE = 0;
    /**
     * Counter Mode with Cipher Block Chaining Message Authentication Code Protocol (CCMP) with
     * 128-bit key.
     */
    public static final int CIPHER_CCMP_128 = 1 << 0;
    /**
     * Counter Mode with Cipher Block Chaining Message Authentication Code Protocol (CCMP) with
     * 256-bit key.
     */
    public static final int CIPHER_CCMP_256 = 1 << 1;
    /**
     * Galois/Counter Mode Protocol (GCMP) with 128-bit key.
     */
    public static final int CIPHER_GCMP_128 = 1 << 2;
    /**
     * Galois/Counter Mode Protocol (GCMP) with 256-bit key.
     */
    public static final int CIPHER_GCMP_256 = 1 << 3;
    private static final Map<String, Integer> sStringToCipher = new HashMap<>();

    static {
        sStringToCipher.put("None", CIPHER_NONE);
        sStringToCipher.put("-CCMP]", CIPHER_CCMP_128);
        sStringToCipher.put("-CCMP-256]", CIPHER_CCMP_256);
        sStringToCipher.put("-GCMP-128]", CIPHER_GCMP_128);
        sStringToCipher.put("-GCMP-256]", CIPHER_GCMP_256);
    }

    @AkmType
    private final int mBaseAkms;
    @Cipher
    private final int mCiphers;
    private final String mPassword;
    private final WifiSsid mWifiSsid;
    private final byte[] mPasnComebackCookie;

    /**
     * Return base AKMs (Authentication and Key Management).
     */
    public @AkmType int getBaseAkms() {
        return mBaseAkms;
    }

    /**
     * Return pairwise ciphers.
     */
    public @Cipher int getCiphers() {
        return mCiphers;
    }

    /**
     * Get password used by base AKM. If null, password is retrieved from the saved network
     * profile for the PASN authentication. See {@link #getWifiSsid()} on retrieving saved
     * network profile.
     */
    @Nullable
    public String getPassword() {
        return mPassword;
    }

    /**
     * Get Wifi SSID which is used to retrieve saved network profile if {@link #getPassword()}
     * is null. If Wifi SSID and password are not set and there is no saved profile corresponding to
     * the responder, unauthenticated PASN will be used if {@link RangingRequest#getSecurityMode()}
     * allows. See {@code SECURITY_MODE_*} for more details.
     */
    @Nullable
    public WifiSsid getWifiSsid() {
        return mWifiSsid;
    }

    /**
     * Get PASN comeback cookie. See {@link Builder#setPasnComebackCookie(byte[])}.
     **/
    @Nullable
    public byte[] getPasnComebackCookie() {
        return mPasnComebackCookie;
    }


    private PasnConfig(@NonNull Parcel in) {
        mBaseAkms = in.readInt();
        mCiphers = in.readInt();
        mPassword = in.readString();
        mWifiSsid = (SdkLevel.isAtLeastT()) ? in.readParcelable(WifiSsid.class.getClassLoader(),
                WifiSsid.class) : in.readParcelable(WifiSsid.class.getClassLoader());
        mPasnComebackCookie = in.createByteArray();
    }

    public static final @NonNull Creator<PasnConfig> CREATOR = new Creator<PasnConfig>() {
        @Override
        public PasnConfig createFromParcel(Parcel in) {
            return new PasnConfig(in);
        }

        @Override
        public PasnConfig[] newArray(int size) {
            return new PasnConfig[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@androidx.annotation.NonNull Parcel dest, int flags) {
        dest.writeInt(mBaseAkms);
        dest.writeInt(mCiphers);
        dest.writeString(mPassword);
        dest.writeParcelable(mWifiSsid, flags);
        dest.writeByteArray(mPasnComebackCookie);
    }

    /**
     * Convert capability string from {@link ScanResult} to a set of
     * {@code AKM_*} supported by the PASN.
     *
     * @hide
     */
    public @AkmType static int getBaseAkmsFromCapabilities(String capabilities) {
        @AkmType int akms = AKM_NONE;
        if (capabilities == null) return akms;
        for (String akm : sStringToAkm.keySet()) {
            if (capabilities.contains(akm)) {
                akms |= sStringToAkm.get(akm);
            }
        }
        return akms;
    }

    /**
     * Convert capability string from {@link ScanResult} to a set of
     * {@code CIPHER_*}.
     *
     * @hide
     */
    public @Cipher static int getCiphersFromCapabilities(String capabilities) {
        @Cipher int ciphers = CIPHER_NONE;
        if (capabilities == null) return ciphers;
        for (String cipher : sStringToCipher.keySet()) {
            if (capabilities.contains(cipher)) {
                ciphers |= sStringToCipher.get(cipher);
            }
        }
        return ciphers;
    }

    private PasnConfig(Builder builder) {
        mBaseAkms = builder.mBaseAkms;
        mCiphers = builder.mCiphers;
        mPassword = builder.mPassword;
        mWifiSsid = builder.mWifiSsid;
        mPasnComebackCookie = builder.mPasnComebackCookie;
    }

    /**
     * @hide
     */
    public static boolean isAkmRequiresPassword(int akms) {
        return (akms & AKM_SAE) != 0;
    }

    /**
     * Builder for {@link PasnConfig}
     */
    @FlaggedApi(Flags.FLAG_SECURE_RANGING)
    public static final class Builder {
        private final int mBaseAkms;
        private final int mCiphers;
        private String mPassword = null;
        private WifiSsid mWifiSsid = null;
        byte[] mPasnComebackCookie = null;

        /**
         * Builder
         *
         * @param baseAkms The AKMs that PASN is configured to use. PASN will use the most secure
         *                AKM in the configuration.
         * @param ciphers  The CIPHERs that PASN is configured to use. PASN will use the most
         *                 secure CIPHER in the configuration which is applicable to the base AKM
         */
        public Builder(@AkmType int baseAkms, @Cipher int ciphers) {
            mBaseAkms = baseAkms;
            mCiphers = ciphers;
        }

        /**
         * Sets the password if needed by the base AKM of the PASN. If not set, password is
         * retrieved from the saved profile identified by the SSID. See
         * {@link #setWifiSsid(WifiSsid)}.
         *
         * Note: If password and SSID is not set, secure ranging will use unauthenticated PASN.
         *
         * @param password password string
         * @return a reference to this Builder
         */
        @NonNull
        public Builder setPassword(@NonNull String password) {
            Objects.requireNonNull(password, "Password must not be null");
            this.mPassword = password;
            return this;
        }

        /**
         * Sets the Wi-Fi Service Set Identifier (SSID). This is used to get the saved profile to
         * retrieve password if password is not set using {@link #setPassword(String)}.
         *
         * Note: If password and SSID is not set, secure ranging will use unauthenticated PASN.
         *
         * @param wifiSsid Wi-Fi Service Set Identifier (SSID)
         * @return a reference to this Builder
         */
        @NonNull
        public Builder setWifiSsid(@NonNull WifiSsid wifiSsid) {
            Objects.requireNonNull(wifiSsid, "SSID must not be null");
            this.mWifiSsid = wifiSsid;
            return this;
        }

        /**
         * Set PASN comeback cookie. PASN authentication allows the station to provide comeback
         * cookie which was indicated in the {@link RangingResult} by the AP with a deferral time.
         * <p>
         * When an AP receives a large volume of initial PASN Authentication frames, it can use
         * the comeback after field in the PASN Parameters element to indicate a deferral time
         * and optionally provide a comeback cookie which is an opaque sequence of octets. Upon
         * receiving this response, the ranging initiator (STA) must wait for the specified time
         * before retrying secure authentication, presenting the received cookie to the AP. See
         * {@link RangingResult#getPasnComebackCookie()} and
         * {@link RangingResult#getPasnComebackAfterMillis()}.
         *
         * @param pasnComebackCookie an opaque  sequence of octets
         * @return a reference to this Builder
         */
        @NonNull
        public Builder setPasnComebackCookie(@NonNull byte[] pasnComebackCookie) {
            Objects.requireNonNull(pasnComebackCookie, "PASN comeback cookie must not be null");
            if (pasnComebackCookie.length > 255 || pasnComebackCookie.length == 0) {
                throw new IllegalArgumentException("Cookie with invalid length "
                        + pasnComebackCookie.length);
            }
            mPasnComebackCookie = pasnComebackCookie;
            return this;
        }

        /**
         * Builds a {@link PasnConfig} object.
         */
        @NonNull
        public PasnConfig build() {
            return new PasnConfig(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PasnConfig that)) return false;
        return mBaseAkms == that.mBaseAkms && mCiphers == that.mCiphers && Objects.equals(
                mPassword, that.mPassword) && Objects.equals(mWifiSsid, that.mWifiSsid)
                && Arrays.equals(mPasnComebackCookie, that.mPasnComebackCookie);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mBaseAkms, mCiphers, mPassword, mWifiSsid);
        result = 31 * result + Arrays.hashCode(mPasnComebackCookie);
        return result;
    }

    @Override
    public String toString() {
        return "PasnConfig{" + "mBaseAkms=" + mBaseAkms + ", mCiphers=" + mCiphers + ", mPassword='"
                + mPassword + '\'' + ", mWifiSsid=" + mWifiSsid + ", mPasnComebackCookie="
                + Arrays.toString(mPasnComebackCookie) + '}';
    }
}
