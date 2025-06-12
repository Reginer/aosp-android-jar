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

package android.net.wifi.p2p;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A class representing Wi-Fi Direct pairing bootstrapping configuration.
 *
 * @see android.net.wifi.p2p.WifiP2pConfig
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@FlaggedApi(Flags.FLAG_WIFI_DIRECT_R2)
public final class WifiP2pPairingBootstrappingConfig implements Parcelable {

    /**
     * Pairing bootstrapping method opportunistic
     */
    public static final int PAIRING_BOOTSTRAPPING_METHOD_OPPORTUNISTIC = 1 << 0;

    /**
     * Pairing bootstrapping method display pin-code - The pin-code is displayed on the connection
     * initiating device. The user enters the displayed pin-code on the other device.
     */
    public static final int PAIRING_BOOTSTRAPPING_METHOD_DISPLAY_PINCODE = 1 << 1;

    /**
     * Pairing bootstrapping method display passphrase - The passphrase is displayed on the
     * connection initiating device. The user enters the displayed passphrase on the other device.
     */
    public static final int PAIRING_BOOTSTRAPPING_METHOD_DISPLAY_PASSPHRASE = 1 << 2;

    /**
     * Pairing bootstrapping method keypad pin-code - The pin-code is displayed on the other
     * device. The user enters the displayed pin-code on the connection initiating device.
     */
    public static final int PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PINCODE = 1 << 3;

    /**
     * Pairing bootstrapping method keypad passphrase - The passphrase is displayed on the other
     * device. The user enters the displayed passphrase on the connection initiating device.
     */
    public static final int PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PASSPHRASE = 1 << 4;

    /**
     * Pairing bootstrapping done out of band (For example: Over Bluetooth LE.
     * Refer Wi-Fi Alliance Wi-Fi Direct R2 specification Section 3.9 for the details).
     */
    public static final int PAIRING_BOOTSTRAPPING_METHOD_OUT_OF_BAND = 1 << 5;


    /** @hide */
    @IntDef(flag = true, prefix = {"PAIRING_BOOTSTRAPPING_METHOD_"}, value = {
            PAIRING_BOOTSTRAPPING_METHOD_OPPORTUNISTIC,
            PAIRING_BOOTSTRAPPING_METHOD_DISPLAY_PINCODE,
            PAIRING_BOOTSTRAPPING_METHOD_DISPLAY_PASSPHRASE,
            PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PINCODE,
            PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PASSPHRASE,
            PAIRING_BOOTSTRAPPING_METHOD_OUT_OF_BAND,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PairingBootstrappingMethod {
    }
    /** One of the {@code PAIRING_BOOTSTRAPPING_METHOD_*}. */
    private int mPairingBootstrappingMethod;

    /**
     * Password for pairing setup, if {@code mPairingBootstrappingMethod} uses
     * {@link #PAIRING_BOOTSTRAPPING_METHOD_DISPLAY_PINCODE},
     * {@link #PAIRING_BOOTSTRAPPING_METHOD_DISPLAY_PASSPHRASE} or
     * {@link #PAIRING_BOOTSTRAPPING_METHOD_OUT_OF_BAND}.
     * Must be set to null for {@link #PAIRING_BOOTSTRAPPING_METHOD_OPPORTUNISTIC},
     * {@link #PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PINCODE}
     * or {@link #PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PASSPHRASE}.
     */
    @Nullable private String mPassword;

    private static boolean isValidPairingBootstrappingMethod(@WifiP2pPairingBootstrappingConfig
            .PairingBootstrappingMethod int method) {
        switch (method) {
            case PAIRING_BOOTSTRAPPING_METHOD_OPPORTUNISTIC:
            case PAIRING_BOOTSTRAPPING_METHOD_DISPLAY_PINCODE:
            case PAIRING_BOOTSTRAPPING_METHOD_DISPLAY_PASSPHRASE:
            case PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PINCODE:
            case PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PASSPHRASE:
            case PAIRING_BOOTSTRAPPING_METHOD_OUT_OF_BAND:
                return true;
            default:
                return false;
        }
    }

    /** @hide */
    public int getPairingBootstrappingMethod() {
        return mPairingBootstrappingMethod;
    }

    /** @hide */
    public String getPairingBootstrappingPassword() {
        return mPassword;
    }

    /** @hide */
    public void setPairingBootstrappingPassword(@NonNull String password) {
        mPassword = password;
    }

    /**
     * Constructor for a WifiP2pPairingBootstrappingConfig.
     * @param method One of the {@code PAIRING_BOOTSTRAPPING_METHOD_*}.
     * @param password Password or PIN for pairing setup. if {@code method} is
     *                 {@link #PAIRING_BOOTSTRAPPING_METHOD_DISPLAY_PINCODE}, the password must be
     *                 a string containing 4 or more digits (0-9). For example: "1234", "56789". if
     *                 {@code method} is {@link #PAIRING_BOOTSTRAPPING_METHOD_DISPLAY_PASSPHRASE}
     *                 or {@link #PAIRING_BOOTSTRAPPING_METHOD_OUT_OF_BAND}, the password must be a
     *                 UTF-8 string of minimum of 1 character.
     *                 The password must be set to null if the
     *                 {@code method} is {@link #PAIRING_BOOTSTRAPPING_METHOD_OPPORTUNISTIC},
     *                 {@link #PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PINCODE} or
     *                 {@link #PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PASSPHRASE}.
     *
     * @throws IllegalArgumentException if the input pairing bootstrapping method is not
     * one of the {@code PAIRING_BOOTSTRAPPING_METHOD_*}.
     * @throws IllegalArgumentException if a non-null password is set for pairing bootstrapping
     * method {@link #PAIRING_BOOTSTRAPPING_METHOD_OPPORTUNISTIC},
     * {@link #PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PINCODE} or
     * {@link #PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PASSPHRASE}.
     */
    public WifiP2pPairingBootstrappingConfig(
            @WifiP2pPairingBootstrappingConfig.PairingBootstrappingMethod int method,
            @Nullable String password) {
        if (!isValidPairingBootstrappingMethod(method)) {
            throw new IllegalArgumentException("Invalid PairingBootstrappingMethod =" + method);
        }
        mPairingBootstrappingMethod = method;
        if (!TextUtils.isEmpty(password)
                && (method == PAIRING_BOOTSTRAPPING_METHOD_OPPORTUNISTIC
                || method == PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PINCODE
                || method == PAIRING_BOOTSTRAPPING_METHOD_KEYPAD_PASSPHRASE)) {
            throw new IllegalArgumentException("Password is not required for =" + method);
        }
        mPassword = password;
    }

    /**
     * Generates a string of all the defined elements.
     *
     * @return a compiled string representing all elements
     */
    public String toString() {
        StringBuilder sbuf = new StringBuilder("WifiP2pPairingBootstrappingConfig:");
        sbuf.append("\n BootstrappingMethod: ").append(mPairingBootstrappingMethod);
        return sbuf.toString();
    }

    /** Implement the Parcelable interface */
    public int describeContents() {
        return 0;
    }

    /**
     * Copy Constructor
     *
     * @hide
     */
    public WifiP2pPairingBootstrappingConfig(@NonNull WifiP2pPairingBootstrappingConfig source) {
        if (source != null) {
            mPairingBootstrappingMethod = source.mPairingBootstrappingMethod;
            mPassword = source.mPassword;
        }
    }

    /** Implement the Parcelable interface */
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mPairingBootstrappingMethod);
        dest.writeString(mPassword);
    }

    /** Implement the Parcelable interface */
    public static final @android.annotation.NonNull
            Creator<WifiP2pPairingBootstrappingConfig> CREATOR =
            new Creator<WifiP2pPairingBootstrappingConfig>() {
                public WifiP2pPairingBootstrappingConfig createFromParcel(Parcel in) {
                    int pairingBootstrappingMethod = in.readInt();
                    String password = in.readString();
                    WifiP2pPairingBootstrappingConfig config =
                            new WifiP2pPairingBootstrappingConfig(
                                pairingBootstrappingMethod, password);
                    return config;
                }

                public WifiP2pPairingBootstrappingConfig[] newArray(int size) {
                    return new WifiP2pPairingBootstrappingConfig[size];
                }
            };
}
