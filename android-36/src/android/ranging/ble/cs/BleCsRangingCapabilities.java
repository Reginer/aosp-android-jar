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

package android.ranging.ble.cs;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.bluetooth.BluetoothAdapter;
import android.os.Parcel;
import android.os.Parcelable;
import android.ranging.RangingCapabilities.TechnologyCapabilities;
import android.ranging.RangingManager;

import com.android.ranging.flags.Flags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the capabilities of the Bluetooth-based Channel Sounding (CS) ranging.
 */
@FlaggedApi(Flags.FLAG_RANGING_CS_ENABLED)
public final class BleCsRangingCapabilities implements Parcelable, TechnologyCapabilities {
    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE_USE})
    @IntDef({
            CS_SECURITY_LEVEL_ONE,
            CS_SECURITY_LEVEL_FOUR,
    })
    public @interface SecurityLevel {
    }

    /**
     * Security Level 1:
     * Either CS tone or CS RTT..
     */
    public static final int CS_SECURITY_LEVEL_ONE = 1;

    /**
     * Security Level 4:
     * 10 ns CS RTT accuracy and CS tones with the addition of CS RTT sounding sequence or random
     * sequence payloads, and support of the Normalized Attack Detector Metric requirements.
     */
    public static final int CS_SECURITY_LEVEL_FOUR = 4;

    private final List<Integer> mSupportedSecurityLevels;
    private final String mBluetoothAddress;

    /**
     * Returns a list of the supported security levels.
     *
     * @return a {@link Set} of integers representing the security levels,
     * where each level is one of {@link SecurityLevel}.
     */
    @NonNull
    @SecurityLevel
    public Set<Integer> getSupportedSecurityLevels() {
        return new HashSet<>(mSupportedSecurityLevels);
    }

    private BleCsRangingCapabilities(Builder builder) {
        mSupportedSecurityLevels = builder.mSupportedSecurityLevels;
        mBluetoothAddress = builder.mBluetoothAddress;
    }

    private BleCsRangingCapabilities(Parcel in) {
        mSupportedSecurityLevels = new ArrayList<>();
        in.readList(mSupportedSecurityLevels, Integer.class.getClassLoader(), Integer.class);
        mBluetoothAddress = in.readString();
    }

    @NonNull
    public static final Creator<BleCsRangingCapabilities> CREATOR =
            new Creator<BleCsRangingCapabilities>() {
                @Override
                public BleCsRangingCapabilities createFromParcel(Parcel in) {
                    return new BleCsRangingCapabilities(in);
                }

                @Override
                public BleCsRangingCapabilities[] newArray(int size) {
                    return new BleCsRangingCapabilities[size];
                }
            };

    /**
     * Get the device's bluetooth address.
     * Internal usage only. Clients of the ranging API should use
     * {@link BluetoothAdapter#getAddress()} instead.
     *
     * @hide
     */
    @NonNull
    public String getBluetoothAddress() {
        return mBluetoothAddress;
    }

    /**
     * @hide
     */
    @Override
    public int getTechnology() {
        return RangingManager.BLE_CS;
    }

    /**
     * @hide
     */
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeList(mSupportedSecurityLevels);
        dest.writeString(mBluetoothAddress);
    }

    /**
     * Builder class for {@link BleCsRangingCapabilities}.
     * This class provides a fluent API for constructing instances of
     * {@link BleCsRangingCapabilities}.
     *
     * @hide
     */
    public static final class Builder {
        private List<Integer> mSupportedSecurityLevels;
        private String mBluetoothAddress;

        /**
         * Set supported security levels to the capabilities.
         *
         * @param supportedSecurityLevels the supported security levels {@link SecurityLevel}.
         * @return this {@link Builder} instance for chaining calls.
         * TODO(b/361634062): Make this a set in the API to match CS API.
         */
        @NonNull
        public Builder setSupportedSecurityLevels(List<Integer> supportedSecurityLevels) {
            this.mSupportedSecurityLevels = supportedSecurityLevels;
            return this;
        }

        /**
         * Set the device's bluetooth address.
         *
         * @param address of the device.
         * @return this {@link Builder} instance for chaining calls.
         */
        @NonNull
        public Builder setBluetoothAddress(String address) {
            this.mBluetoothAddress = address;
            return this;
        }

        /**
         * Builds and returns a {@link BleCsRangingCapabilities} instance.
         *
         * @return a new {@link BleCsRangingCapabilities} object.
         */
        @NonNull
        public BleCsRangingCapabilities build() {
            return new BleCsRangingCapabilities(this);
        }
    }

    @Override
    public String toString() {
        return "BleCsRangingCapabilities{ "
                + "mBluetoothAddress="
                + mBluetoothAddress
                + ", mSupportedSecurityLevels="
                + mSupportedSecurityLevels
                + " }";
    }
}
