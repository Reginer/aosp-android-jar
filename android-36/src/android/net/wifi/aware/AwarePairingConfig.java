/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.net.wifi.aware;

import static android.net.wifi.aware.Characteristics.WIFI_AWARE_CIPHER_SUITE_NCS_PK_PASN_128;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * The config for the Aware Pairing. Set to
 * {@link PublishConfig.Builder#setPairingConfig(AwarePairingConfig)} and
 * {@link SubscribeConfig.Builder#setPairingConfig(AwarePairingConfig)}.
 * Only valid when {@link Characteristics#isAwarePairingSupported()} is true.
 */
public final class AwarePairingConfig implements Parcelable {

    /**
     * Aware Pairing bootstrapping method opportunistic
     */
    public static final int PAIRING_BOOTSTRAPPING_OPPORTUNISTIC = 1 << 0;

    /**
     * Aware Pairing bootstrapping method pin-code display
     */
    public static final int PAIRING_BOOTSTRAPPING_PIN_CODE_DISPLAY = 1 << 1;

    /**
     * Aware Pairing bootstrapping method passphrase display
     */
    public static final int PAIRING_BOOTSTRAPPING_PASSPHRASE_DISPLAY = 1 << 2;

    /**
     * Aware Pairing bootstrapping method QR-code display
     */
    public static final int PAIRING_BOOTSTRAPPING_QR_DISPLAY = 1 << 3;

    /**
     * Aware Pairing bootstrapping method NFC tag
     */
    public static final int PAIRING_BOOTSTRAPPING_NFC_TAG = 1 << 4;
    /**
     * Aware Pairing bootstrapping method pin-code keypad
     */
    public static final int PAIRING_BOOTSTRAPPING_PIN_CODE_KEYPAD = 1 << 5;
    /**
     * Aware Pairing bootstrapping method passphrase keypad
     */
    public static final int PAIRING_BOOTSTRAPPING_PASSPHRASE_KEYPAD = 1 << 6;
    /**
     * Aware Pairing bootstrapping method QR-code scan
     */
    public static final int PAIRING_BOOTSTRAPPING_QR_SCAN = 1 << 7;
    /**
     * Aware Pairing bootstrapping method NFC reader
     */
    public static final int PAIRING_BOOTSTRAPPING_NFC_READER = 1 << 8;
    /**
     * This is used for the boundary check and should be the max value of the bitmap + 1.
     * @hide
     */
    public static final int PAIRING_BOOTSTRAPPING_MAX = 1 << 9;


    /** @hide */
    @IntDef(flag = true, prefix = {"PAIRING_BOOTSTRAPPING_"}, value = {
            PAIRING_BOOTSTRAPPING_OPPORTUNISTIC,
            PAIRING_BOOTSTRAPPING_PIN_CODE_DISPLAY,
            PAIRING_BOOTSTRAPPING_PASSPHRASE_DISPLAY,
            PAIRING_BOOTSTRAPPING_QR_DISPLAY,
            PAIRING_BOOTSTRAPPING_NFC_TAG,
            PAIRING_BOOTSTRAPPING_PIN_CODE_KEYPAD,
            PAIRING_BOOTSTRAPPING_PASSPHRASE_KEYPAD,
            PAIRING_BOOTSTRAPPING_QR_SCAN,
            PAIRING_BOOTSTRAPPING_NFC_READER

    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface BootstrappingMethod {
    }

    private final boolean mPairingSetup;
    private final boolean mPairingCache;
    private final boolean mPairingVerification;
    private final int mBootstrappingMethods;
    private final int mCipherSuites;

    /**
     * Check if the NPK/NIK cache is support in the config
     */
    public boolean isPairingCacheEnabled() {
        return mPairingCache;
    }

    /**
     * Check if the Aware Pairing setup is support in the config.
     * Setup is the first time for two device establish Aware Pairing
     */
    public boolean isPairingSetupEnabled() {
        return mPairingSetup;
    }

    /**
     * Check if the Aware Pairing verification is support in the config.
     * Verification is for two device already paired and re-establish with cached NPK/NIK
     */
    public boolean isPairingVerificationEnabled() {
        return mPairingVerification;
    }

    /**
     * Get the supported bootstrapping methods in this config. Set of the
     * STATUS_NETWORK_SUGGESTIONS_ values.
     */
    @BootstrappingMethod
    public int getBootstrappingMethods() {
        return mBootstrappingMethods;
    }

    /**
     * Get the supported cipher suites in this config.
     */
    @FlaggedApi(Flags.FLAG_AWARE_PAIRING)
    @Characteristics.WifiAwarePairingCipherSuites
    public int getSupportedCipherSuites() {
        return mCipherSuites;
    }

    /**
     * Verifies that the contents of the AwarePairingConfig are valid
     * @hide
     */
    public boolean assertValid(Characteristics characteristics) {
        if (mBootstrappingMethods < 0 || mBootstrappingMethods >= PAIRING_BOOTSTRAPPING_MAX) {
            return false;
        }
        if ((characteristics.getSupportedPairingCipherSuites() & mCipherSuites) == 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AwarePairingConfig)) return false;
        AwarePairingConfig that = (AwarePairingConfig) o;
        return mPairingSetup == that.mPairingSetup && mPairingCache == that.mPairingCache
                && mPairingVerification == that.mPairingVerification
                && mBootstrappingMethods == that.mBootstrappingMethods
                && mCipherSuites == that.mCipherSuites;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPairingSetup, mPairingCache, mPairingVerification,
                mBootstrappingMethods, mCipherSuites);
    }

    /** @hide */
    public AwarePairingConfig(boolean setup, boolean cache, boolean verification, int method,
            int cipherSuites) {
        mPairingSetup = setup;
        mPairingCache = cache;
        mPairingVerification = verification;
        mBootstrappingMethods = method;
        mCipherSuites = cipherSuites;
    }

    /** @hide */
    protected AwarePairingConfig(@NonNull Parcel in) {
        mPairingSetup = in.readBoolean();
        mPairingCache = in.readBoolean();
        mPairingVerification = in.readBoolean();
        mBootstrappingMethods = in.readInt();
        mCipherSuites = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeBoolean(mPairingSetup);
        dest.writeBoolean(mPairingCache);
        dest.writeBoolean(mPairingVerification);
        dest.writeInt(mBootstrappingMethods);
        dest.writeInt(mCipherSuites);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull public static final Creator<AwarePairingConfig> CREATOR =
            new Creator<AwarePairingConfig>() {
        @Override
        public AwarePairingConfig createFromParcel(@NonNull Parcel in) {
            return new AwarePairingConfig(in);
        }

        @Override
        public AwarePairingConfig[] newArray(int size) {
            return new AwarePairingConfig[size];
        }
    };
    /**
     * Builder used to build {@link AwarePairingConfig} objects.
     */
    public static final class Builder {
        private boolean mPairingSetup = false;
        private boolean mPairingCache = false;
        private boolean mPairingVerification = false;
        private int mBootStrappingMethods = 0;
        private int mCipherSuites = WIFI_AWARE_CIPHER_SUITE_NCS_PK_PASN_128;

        /**
         * Set whether enable the Aware Pairing setup
         * @param enabled true to enable, false otherwise
         * @return the current {@link Builder} builder, enabling chaining of builder methods.
         */
        @NonNull
        public Builder setPairingSetupEnabled(boolean enabled) {
            mPairingSetup = enabled;
            return this;
        }

        /**
         * Set whether enable the Aware Pairing verification
         * @param enabled if set to true will accept Aware Pairing verification request from peer
         *                with cached NPK/NIK, otherwise will reject the request .
         * @return the current {@link Builder} builder, enabling chaining of builder methods.
         */
        @NonNull public Builder setPairingVerificationEnabled(boolean enabled) {
            mPairingVerification = enabled;
            return this;
        }

        /**
         * Set whether enable cache of the NPK/NIK of Aware Pairing setup
         * @param enabled true to enable caching, false otherwise
         * @return the current {@link Builder} builder, enabling chaining of builder methods.
         */
        @NonNull public Builder setPairingCacheEnabled(boolean enabled) {
            mPairingCache = enabled;
            return this;
        }

        /**
         * Set the supported bootstrapping methods
         * @param methods methods supported, set of PAIRING_BOOTSTRAPPING_ values
         * @return the current {@link Builder} builder, enabling chaining of builder methods.
         */
        @NonNull public Builder setBootstrappingMethods(@BootstrappingMethod int methods) {
            mBootStrappingMethods = methods;
            return this;
        }

        /**
         * Set the supported cipher suites. If not set, default will be
         * {@link Characteristics#WIFI_AWARE_CIPHER_SUITE_NCS_PK_PASN_128}
         * @param cipherSuites cipher suites supported
         * @return the current {@link Builder} builder, enabling chaining of builder methods.
         */
        @FlaggedApi(Flags.FLAG_AWARE_PAIRING)
        @NonNull public Builder setSupportedCipherSuites(
                @Characteristics.WifiAwarePairingCipherSuites int cipherSuites) {
            mCipherSuites = cipherSuites;
            return this;
        }

        /**
         * Build {@link AwarePairingConfig} given the current requests made on the
         * builder.
         */
        @NonNull
        public AwarePairingConfig build() {
            return new AwarePairingConfig(mPairingSetup, mPairingCache, mPairingVerification,
                    mBootStrappingMethods, mCipherSuites);
        }
    }
}
