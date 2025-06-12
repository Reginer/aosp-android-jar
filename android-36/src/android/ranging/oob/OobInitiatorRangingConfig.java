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

package android.ranging.oob;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import android.ranging.RangingConfig;
import android.util.Range;

import com.android.ranging.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the configuration for an Out-of-Band (OOB) initiator in a ranging session.
 * This class includes configuration options such as device handles, security level,
 * ranging mode, and interval range for setting up an OOB initiator ranging session.
 */
@FlaggedApi(Flags.FLAG_RANGING_STACK_ENABLED)
public final class OobInitiatorRangingConfig extends RangingConfig implements Parcelable {

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            SECURITY_LEVEL_BASIC,
            SECURITY_LEVEL_SECURE,
    })
    public @interface SecurityLevel {
    }

    /**
     * Basic security level for the ranging session.
     * <p>Example usage:
     * UWB: Static-STS
     * BLE-CS: Security level one
     */
    public static final int SECURITY_LEVEL_BASIC = 0;

    /**
     * Basic security level for the ranging session.
     * <p>Example usage:
     * UWB: Provisioned-STS
     * BLE-CS: Security level four
     */
    public static final int SECURITY_LEVEL_SECURE = 1;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            RANGING_MODE_AUTO,
            RANGING_MODE_HIGH_ACCURACY,
            RANGING_MODE_HIGH_ACCURACY_PREFERRED,
            RANGING_MODE_FUSED,
    })
    public @interface RangingMode {
    }

    /**
     * Automatic ranging mode. Allows the system to choose the best mode.
     */
    public static final int RANGING_MODE_AUTO = 0;
    /**
     * High accuracy ranging mode. No fallback allowed.
     */
    public static final int RANGING_MODE_HIGH_ACCURACY = 1;
    /**
     * High accuracy ranging mode. Fallback to lower accuracy if high accuracy ranging is not
     * supported by all devices.
     */
    public static final int RANGING_MODE_HIGH_ACCURACY_PREFERRED = 2;
    /**
     * Starts ranging with all the ranging technologies both devices support.
     */
    public static final int RANGING_MODE_FUSED = 3;

    private final List<DeviceHandle> mDeviceHandles;

    private final Range<Duration> mRangingIntervalRange;

    @SecurityLevel
    private final int mSecurityLevel;

    @RangingMode
    private final int mRangingMode;

    private OobInitiatorRangingConfig(Builder builder) {
        setRangingSessionType(RangingConfig.RANGING_SESSION_OOB);
        mDeviceHandles = new ArrayList<>(builder.mDeviceHandles);
        mSecurityLevel = builder.mSecurityLevel;
        mRangingMode = builder.mRangingMode;
        mRangingIntervalRange = new Range<>(builder.mFastestRangingInterval,
                builder.mSlowestRangingInterval);
    }

    private OobInitiatorRangingConfig(Parcel in) {
        setRangingSessionType(in.readInt());
        mDeviceHandles = in.createTypedArrayList(DeviceHandle.CREATOR);
        mSecurityLevel = in.readInt();
        mRangingMode = in.readInt();
        Duration lower = Duration.ofMillis(in.readLong());
        Duration upper = Duration.ofMillis(in.readLong());
        mRangingIntervalRange = new Range<>(lower, upper);

    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(getRangingSessionType());
        dest.writeTypedList(mDeviceHandles);
        dest.writeInt(mSecurityLevel);
        dest.writeInt(mRangingMode);
        dest.writeLong(mRangingIntervalRange.getLower().toMillis());
        dest.writeLong(mRangingIntervalRange.getUpper().toMillis());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<OobInitiatorRangingConfig> CREATOR =
            new Creator<OobInitiatorRangingConfig>() {
                @Override
                public OobInitiatorRangingConfig createFromParcel(Parcel in) {
                    return new OobInitiatorRangingConfig(in);
                }

                @Override
                public OobInitiatorRangingConfig[] newArray(int size) {
                    return new OobInitiatorRangingConfig[size];
                }
            };

    /**
     * Returns the list of DeviceHandles associated with the OOB initiator.
     *
     * @return A list of DeviceHandle objects.
     */
    @NonNull
    public List<DeviceHandle> getDeviceHandles() {
        return mDeviceHandles;
    }

    /**
     * Returns the ranging interval range configuration.
     *
     * @return The {@link Range} associated with this OOB initiator.
     */
    @NonNull
    public Range<Duration> getRangingIntervalRange() {
        return mRangingIntervalRange;
    }

    /**
     * Returns the fastest requested ranging interval.
     *
     * @return The fastest interval.
     */
    @NonNull
    public Duration getFastestRangingInterval() {
        return mRangingIntervalRange.getLower();
    }

    /**
     * Returns the slowest acceptable ranging.
     *
     * @return The slowest interval.
     */
    @NonNull
    public Duration getSlowestRangingInterval() {
        return mRangingIntervalRange.getUpper();
    }

    /**
     * Returns the security level set for the ranging session.
     *
     * @return the security level.
     * <p>Possible values:
     * {@link #SECURITY_LEVEL_BASIC}
     * {@link #SECURITY_LEVEL_SECURE}
     */
    @SecurityLevel
    public int getSecurityLevel() {
        return mSecurityLevel;
    }

    /**
     * Returns the ranging mode for the session.
     *
     * @return the ranging mode.
     * <p>Possible values:
     * {@link #RANGING_MODE_AUTO}
     * {@link #RANGING_MODE_HIGH_ACCURACY}
     * {@link #RANGING_MODE_HIGH_ACCURACY_PREFERRED}
     * {@link #RANGING_MODE_FUSED}
     */

    @RangingMode
    public int getRangingMode() {
        return mRangingMode;
    }

    /**
     * Builder class for creating instances of {@link OobInitiatorRangingConfig}.
     */
    public static final class Builder {
        private final List<DeviceHandle> mDeviceHandles = new ArrayList<>();
        @SecurityLevel
        private int mSecurityLevel = SECURITY_LEVEL_BASIC;
        @RangingMode
        private int mRangingMode = RANGING_MODE_AUTO;

        private Duration mFastestRangingInterval = Duration.ofMillis(100);
        private Duration mSlowestRangingInterval = Duration.ofMillis(5000);

        /**
         * Sets the fastest ranging interval in milliseconds.
         *
         * @param intervalMs The fastest interval in milliseconds.
         *                   Defaults to 100ms
         * @return The Builder instance, for chaining calls.
         */
        @NonNull
        public Builder setFastestRangingInterval(@NonNull Duration intervalMs) {
            this.mFastestRangingInterval = intervalMs;
            return this;
        }

        /**
         * Sets the slowest ranging interval in milliseconds.
         *
         * @param intervalMs The slowest interval in milliseconds.
         *                   Defaults to 5000ms
         * @return The Builder instance, for chaining calls.
         */
        @NonNull
        public Builder setSlowestRangingInterval(@NonNull Duration intervalMs) {
            if (intervalMs.isNegative() || intervalMs.isZero()) {
                throw new IllegalArgumentException("Slowest duration cannot be negative or zero");
            }
            this.mSlowestRangingInterval = intervalMs;
            return this;
        }

        /**
         * Adds a DeviceHandle to the list of devices for the ranging session.
         *
         * @param deviceHandle The DeviceHandle to add.
         * @return The Builder instance.
         */
        @NonNull
        public Builder addDeviceHandle(@NonNull DeviceHandle deviceHandle) {
            mDeviceHandles.add(deviceHandle);
            return this;
        }

        /**
         * Adds a list of DeviceHandle to the list of devices for the ranging session.
         *
         * @param deviceHandles The list of DeviceHandles to add.
         * @return The Builder instance.
         */
        @NonNull
        public Builder addDeviceHandles(@NonNull List<DeviceHandle> deviceHandles) {
            mDeviceHandles.addAll(deviceHandles);
            return this;
        }

        /**
         * Sets the security level for the ranging session.
         *
         * @param securityLevel The security level to set.
         *                      Defaults to {@link #SECURITY_LEVEL_BASIC}
         * @return The Builder instance.
         */
        @NonNull
        public Builder setSecurityLevel(@SecurityLevel int securityLevel) {
            this.mSecurityLevel = securityLevel;
            return this;
        }

        /**
         * Sets the ranging mode for the session.
         *
         * @param rangingMode The ranging mode to set.
         *                    Defaults to {@link #RANGING_MODE_AUTO}
         * @return The Builder instance.
         */
        @NonNull
        public Builder setRangingMode(@RangingMode int rangingMode) {
            this.mRangingMode = rangingMode;
            return this;
        }

        /**
         * Builds an instance of {@link OobInitiatorRangingConfig} with the provided parameters.
         *
         * @return A new OobInitiatorRangingConfig instance.
         */
        @NonNull
        public OobInitiatorRangingConfig build() {
            return new OobInitiatorRangingConfig(this);
        }
    }

    @Override
    public String toString() {
        return "OobInitiatorRangingConfig{ "
                + "mDeviceHandles="
                + mDeviceHandles
                + ", mRangingIntervalRange="
                + mRangingIntervalRange
                + ", mSecurityLevel="
                + mSecurityLevel
                + ", mRangingMode="
                + mRangingMode
                + ", "
                + super.toString()
                + ", "
                + " }";
    }
}
