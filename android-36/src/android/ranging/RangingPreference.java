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

package android.ranging;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;
import android.ranging.oob.OobInitiatorRangingConfig;
import android.ranging.oob.OobResponderRangingConfig;
import android.ranging.raw.RawInitiatorRangingConfig;
import android.ranging.raw.RawResponderRangingConfig;

import com.android.ranging.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Represents the configuration preferences for a ranging session.
 *
 * <p>The {@code RangingPreference} class allows users to specify various parameters
 * required for a ranging session, including ranging parameters, sensor fusion settings,
 * and data notification configurations. It provides a {@link Builder} to construct
 * an instance with custom configurations.</p>
 */
@FlaggedApi(Flags.FLAG_RANGING_STACK_ENABLED)
public final class RangingPreference implements Parcelable {

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DEVICE_ROLE_RESPONDER,
            DEVICE_ROLE_INITIATOR,
    })
    public @interface DeviceRole {
    }

    /** The device that responds to a session. */
    public static final int DEVICE_ROLE_RESPONDER = 0;
    /** The device that initiates the session. */
    public static final int DEVICE_ROLE_INITIATOR = 1;

    @DeviceRole
    private final int mDeviceRole;
    private final RangingConfig mRangingParams;

    private final SessionConfig mSessionConfig;

    private RangingPreference(Builder builder) {
        mDeviceRole = builder.mDeviceRole;
        mRangingParams = builder.mRangingConfig;
        mSessionConfig = builder.mSessionConfig;
    }

    private RangingPreference(Parcel in) {
        mDeviceRole = in.readInt();
        mRangingParams = in.readParcelable(
                RangingConfig.class.getClassLoader(),
                RangingConfig.class);
        mSessionConfig = in.readParcelable(
                SessionConfig.class.getClassLoader(), SessionConfig.class);
    }

    @NonNull
    public static final Creator<RangingPreference> CREATOR = new Creator<>() {
        @Override
        public RangingPreference createFromParcel(Parcel in) {
            return new RangingPreference(in);
        }

        @Override
        public RangingPreference[] newArray(int size) {
            return new RangingPreference[size];
        }
    };

    /**
     * Returns the device role.
     */
    @DeviceRole
    public int getDeviceRole() {
        return mDeviceRole;
    }

    /**
     * Returns the ranging parameters associated with this preference.
     *
     * @return the {@link RangingConfig} or {@code null} if not set.
     */
    @Nullable
    public RangingConfig getRangingParams() {
        return mRangingParams;
    }

    /**
     * Returns the ranging session configuration params.
     *
     * @return a non-null {@link SessionConfig} instance.
     */
    @NonNull
    public SessionConfig getSessionConfig() {
        return mSessionConfig;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mDeviceRole);
        dest.writeParcelable(mRangingParams, flags);
        dest.writeParcelable(mSessionConfig, flags);
    }

    /**
     * Builder for creating instances of {@code RangingPreference}.
     *
     * <p>This Builder class provides a flexible way to construct a {@link RangingPreference}
     * instance by setting required and optional parameters. It ensures that all necessary
     * parameters are specified and provides default values for optional configurations.</p>
     *
     * <p>Example usage:</p>
     *
     * <pre>{@code
     * RangingPreference rangingPreference = new RangingPreference.Builder(DEVICE_ROLE_RESPONDER,
     *     new RawResponderRangingParams.Builder()
     *         .setRawRangingDevice(
     *             new RawRangingDevice.Builder()
     *                 .setRangingDevice(
     *                     new RangingDevice.Builder()
     *                         .build())
     *                .setBleRssiRangingParams(
     *                new BleRssiRangingParams.Builder("AA:BB:CC:00:11:22")
     *                    .build())
     *            .build())
     *        build())
     *    .build();
     * }</pre>
     */
    public static final class Builder {
        @DeviceRole
        private final int mDeviceRole;
        private final RangingConfig mRangingConfig;
        private SessionConfig mSessionConfig = new SessionConfig.Builder().build();

        /**
         * Creates a Builder instance with the required device role and {@link RangingConfig}.
         *
         * @param role          the role of the device in {@link DeviceRole}
         * @param rangingConfig the {@link RangingConfig} to use.
         *                      Needs to be an instance of one of the following:
         *                      <ul>
         *                         <li>{@link RawResponderRangingConfig}</li>
         *                         <li>{@link RawInitiatorRangingConfig}</li>
         *                         <li>{@link OobResponderRangingConfig}</li>
         *                         <li>{@link OobInitiatorRangingConfig}</li>
         *                      </ul>
         * @throws NullPointerException if {@code rangingParams} is null.
         */
        public Builder(@DeviceRole int role, @NonNull RangingConfig rangingConfig) {
            Objects.requireNonNull(rangingConfig);
            mDeviceRole = role;
            mRangingConfig = rangingConfig;
        }

        /**
         * Sets the configuration parameters for the ranging session policy.
         *
         * <p>This method allows specifying additional configuration parameters encapsulated in
         * {@link SessionConfig} for fine-tuning the behavior of the ranging session.
         *
         * @param config the {@link SessionConfig}.
         * @return this {@link Builder} instance.
         * @throws NullPointerException if {@code params} is null.
         */
        @NonNull
        public Builder setSessionConfig(@NonNull SessionConfig config) {
            Objects.requireNonNull(config);
            mSessionConfig = config;
            return this;
        }

        /**
         * Builds the {@code RangingPreference} instance.
         *
         * <p>If the {@link SessionConfig} is not set, default instances will be used.
         *
         * @return a new {@code RangingPreference} instance.
         */
        @NonNull
        public RangingPreference build() {
            return new RangingPreference(this);
        }
    }

    @Override
    public String toString() {
        return "RangingPreference{ "
                + "mDeviceRole="
                + mDeviceRole
                + ", mRangingParameters="
                + mRangingParams
                + ", mSessionConfig="
                + mSessionConfig
                + " }";
    }
}
