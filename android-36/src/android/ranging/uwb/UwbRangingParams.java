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

package android.ranging.uwb;

import static android.ranging.raw.RawRangingDevice.UPDATE_RATE_NORMAL;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;
import android.ranging.raw.RawRangingDevice.RangingUpdateRate;

import com.android.ranging.flags.Flags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Objects;

/**
 * UwbRangingParams encapsulates the parameters required for a UWB ranging session.
 */
@FlaggedApi(Flags.FLAG_RANGING_STACK_ENABLED)
public final class UwbRangingParams implements Parcelable {

    private final int mSessionId;

    private final int mSubSessionId;

    private UwbRangingParams(Parcel in) {
        mSessionId = in.readInt();
        mSubSessionId = in.readInt();
        mConfigId = in.readInt();
        mDeviceAddress = in.readParcelable(UwbAddress.class.getClassLoader());
        mSessionKeyInfo = in.readBlob();
        mSubSessionKeyInfo = in.readBlob();
        mComplexChannel = in.readParcelable(UwbComplexChannel.class.getClassLoader());
        mRangingUpdateRate = in.readInt();
        mPeerAddress = in.readParcelable(UwbAddress.class.getClassLoader());
        mSlotDurationMillis = in.readInt();
    }

    @NonNull
    public static final Creator<UwbRangingParams> CREATOR = new Creator<UwbRangingParams>() {
        @Override
        public UwbRangingParams createFromParcel(Parcel in) {
            return new UwbRangingParams(in);
        }

        @Override
        public UwbRangingParams[] newArray(int size) {
            return new UwbRangingParams[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@androidx.annotation.NonNull Parcel dest, int flags) {
        dest.writeInt(mSessionId);
        dest.writeInt(mSubSessionId);
        dest.writeInt(mConfigId);
        dest.writeParcelable(mDeviceAddress, flags);
        dest.writeBlob(mSessionKeyInfo);
        dest.writeBlob(mSubSessionKeyInfo);
        dest.writeParcelable(mComplexChannel, flags);
        dest.writeInt(mRangingUpdateRate);
        dest.writeParcelable(mPeerAddress, flags);
        dest.writeInt(mSlotDurationMillis);
    }

    /**
     * Defines the roles that a device can assume within a UWB ranging session.
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE_USE})
    @IntDef({
            CONFIG_UNICAST_DS_TWR,
            CONFIG_MULTICAST_DS_TWR,
            CONFIG_PROVISIONED_UNICAST_DS_TWR,
            CONFIG_PROVISIONED_MULTICAST_DS_TWR,
            CONFIG_PROVISIONED_INDIVIDUAL_MULTICAST_DS_TWR,
            CONFIG_PROVISIONED_UNICAST_DS_TWR_VERY_FAST,
    })
    public @interface ConfigId {
    }

    /**
     * FiRa-defined unicast {@code STATIC STS DS-TWR} ranging, deferred mode, ranging interval
     * Fast (120ms), Normal (240ms), Infrequent (600ms)
     */
    public static final int CONFIG_UNICAST_DS_TWR = 1;

    /**
     * FiRa-defined multicast {@code STATIC STS DS-TWR} ranging, deferred mode, ranging interval
     * Fast (120ms), Normal (200ms), Infrequent (600ms)
     */
    public static final int CONFIG_MULTICAST_DS_TWR = 2;
    /** Same as {@code CONFIG_UNICAST_DS_TWR}, except P-STS security mode is enabled. */
    public static final int CONFIG_PROVISIONED_UNICAST_DS_TWR = 3;
    /** Same as {@code CONFIG_MULTICAST_DS_TWR}, except P-STS security mode is enabled. */
    public static final int CONFIG_PROVISIONED_MULTICAST_DS_TWR = 4;
    /**
     * Same as {@code CONFIG_UNICAST_DS_TWR}, except P-STS individual controlee key mode is
     * enabled.
     */
    public static final int CONFIG_PROVISIONED_INDIVIDUAL_MULTICAST_DS_TWR = 5;

    /** Same as {@code CONFIG_ID_3}, except fast ranging interval is 96 milliseconds. */
    public static final int CONFIG_PROVISIONED_UNICAST_DS_TWR_VERY_FAST = 6;

    /** Sub session id not applicable. */
    public static final int SUB_SESSION_UNDEFINED = -1;

    @ConfigId
    private final int mConfigId;

    private final UwbAddress mDeviceAddress;

    private final byte[] mSessionKeyInfo;

    private final byte[] mSubSessionKeyInfo;

    private final UwbComplexChannel mComplexChannel;

    private final UwbAddress mPeerAddress;

    /**
     * Defines slot supported slot durations.
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DURATION_1_MS,
            DURATION_2_MS,
    })
    public @interface SlotDuration {
    }

    /** 1 millisecond slot duration */
    public static final int DURATION_1_MS = 1;

    /** 2 millisecond slot duration */
    public static final int DURATION_2_MS = 2;

    @RangingUpdateRate
    private final int mRangingUpdateRate;

    @SlotDuration
    private final int mSlotDurationMillis;

    private UwbRangingParams(Builder builder) {
        mSessionId = builder.mSessionId;
        mSubSessionId = builder.mSubSessionId;
        mConfigId = builder.mConfigId;
        mDeviceAddress = builder.mDeviceAddress;
        mSessionKeyInfo = builder.mSessionKeyInfo;
        mSubSessionKeyInfo = builder.mSubSessionKeyInfo;
        mComplexChannel = builder.mComplexChannel;
        mPeerAddress = builder.mPeerAddress;
        mRangingUpdateRate = builder.mRangingUpdateRate;
        mSlotDurationMillis = builder.mSlotDuration;
    }

    /**
     * Gets the session ID associated with this ranging session.
     *
     * @return The session ID as an integer.
     */
    public int getSessionId() {
        return mSessionId;
    }

    /**
     * Gets the sub-session ID if applicable for the session.
     *
     * @return The sub-session ID as an integer or {@link #SUB_SESSION_UNDEFINED} if not applicable.
     */
    public int getSubSessionId() {
        return mSubSessionId;
    }

    /**
     * Gets the configuration ID associated with this session.
     *
     * @return The configuration ID as an integer.
     */
    @ConfigId
    public int getConfigId() {
        return mConfigId;
    }

    /**
     * Gets the UWB address of the device.
     *
     * @return The {@link UwbAddress} of the device.
     */
    @NonNull
    public UwbAddress getDeviceAddress() {
        return mDeviceAddress;
    }

    /**
     * Gets session key information, if available.
     *
     * @return A byte array containing session key info, or null if not available.
     */
    @Nullable
    public byte[] getSessionKeyInfo() {
        return mSessionKeyInfo == null ? null : Arrays.copyOf(mSessionKeyInfo,
                mSessionKeyInfo.length);
    }

    /**
     * Gets sub-session key information, if available.
     *
     * @return A byte array containing sub-session key info, or null if not available.
     */
    @Nullable
    public byte[] getSubSessionKeyInfo() {
        return mSubSessionKeyInfo == null ? null : Arrays.copyOf(mSubSessionKeyInfo,
                mSubSessionKeyInfo.length);
    }

    /**
     * Gets the complex channel information for this session.
     *
     * @return A {@link UwbComplexChannel} object containing channel and preamble index.
     */
    @NonNull
    public UwbComplexChannel getComplexChannel() {
        return mComplexChannel;
    }

    /**
     * Returns the UwbAddress of the peer device.
     *
     * @return A {@link UwbAddress} corresponding to the peer device to range with.
     */
    @NonNull
    public UwbAddress getPeerAddress() {
        return mPeerAddress;
    }

    /**
     * Returns the update rate for ranging operations.
     *
     * @return The ranging update rate.
     */
    @RangingUpdateRate
    public int getRangingUpdateRate() {
        return mRangingUpdateRate;
    }

    /**
     * Returns slot duration of the session.
     *
     * @return the slot duration.
     */
    @SlotDuration
    public int getSlotDuration() {
        return mSlotDurationMillis;
    }


    /**
     * Builder class for creating instances of {@link UwbRangingParams}
     */
    public static final class Builder {
        private int mSessionId;
        private int mSubSessionId = SUB_SESSION_UNDEFINED;
        private int mConfigId;
        private UwbAddress mDeviceAddress = null;
        private byte[] mSessionKeyInfo = null;
        private byte[] mSubSessionKeyInfo = null;
        private UwbComplexChannel mComplexChannel = new UwbComplexChannel.Builder().build();
        private UwbAddress mPeerAddress = null;
        @RangingUpdateRate
        private int mRangingUpdateRate = UPDATE_RATE_NORMAL;
        @SlotDuration
        private int mSlotDuration = DURATION_2_MS;

        /**
         * Constructs a new {@link Builder} for creating a ranging session.
         *
         * @param sessionId     A unique identifier for the session.
         * @param configId      The configuration ID for the ranging parameters.
         * @param deviceAddress The {@link UwbAddress} representing the device's address.
         *                      Must be non-null.
         * @param peerAddress   The {@link UwbAddress} of the peer device.
         *                      Must be non-null.
         * @throws IllegalArgumentException if either {@code deviceAddress} or {@code peerAddress}
         *                                  is null.
         */
        public Builder(int sessionId, @ConfigId int configId, @NonNull UwbAddress deviceAddress,
                @NonNull UwbAddress peerAddress) {
            Objects.requireNonNull(deviceAddress);
            Objects.requireNonNull(peerAddress);
            mSessionId = sessionId;
            mConfigId = configId;
            mDeviceAddress = deviceAddress;
            mPeerAddress = peerAddress;
        }

        /**
         * Sets the sub-session ID for the ranging session.
         *
         * @param subSessionId the sub-session ID, which should be a unique identifier for the
         *                     sub-session.
         * @return this Builder instance for method chaining.
         */
        @NonNull
        public Builder setSubSessionId(int subSessionId) {
            mSubSessionId = subSessionId;
            return this;
        }

        /**
         * Sets the session key information for secure ranging.
         *
         * @param sessionKeyInfo a byte array containing session key information.
         * @return this Builder instance.
         * @throws IllegalArgumentException if the provided byte array is null.
         */
        @NonNull
        public Builder setSessionKeyInfo(@NonNull byte[] sessionKeyInfo) {
            Objects.requireNonNull(sessionKeyInfo);
            mSessionKeyInfo = Arrays.copyOf(sessionKeyInfo, sessionKeyInfo.length);
            return this;
        }

        /**
         * Sets the sub-session key information for secure ranging.
         *
         * @param subSessionKeyInfo a byte array containing sub-session key information.
         * @return this Builder instance.
         * @throws IllegalArgumentException if the provided map is null.
         */
        @NonNull
        public Builder setSubSessionKeyInfo(@NonNull byte[] subSessionKeyInfo) {
            Objects.requireNonNull(subSessionKeyInfo);
            mSubSessionKeyInfo = Arrays.copyOf(subSessionKeyInfo, subSessionKeyInfo.length);
            return this;
        }

        /**
         * Sets the complex channel configuration for the ranging session.
         *
         * @param complexChannel a non-null {@link UwbComplexChannel} instance representing the
         *                       channel and preamble configuration. For better performance always
         *                       use a random preamble index for each ranging session.
         * @return this Builder instance.
         * @throws IllegalArgumentException if the provided complex channel is null.
         */
        @NonNull
        public Builder setComplexChannel(@NonNull UwbComplexChannel complexChannel) {
            mComplexChannel = complexChannel;
            return this;
        }

        /**
         * Sets the ranging update rate for the session.
         * <p> Defaults to {@link RangingUpdateRate#UPDATE_RATE_NORMAL}.
         *
         * @param rate the ranging update rate, defined as one of the constants in
         *             {@link RangingUpdateRate}.
         * @return this Builder instance.
         */
        @NonNull
        public Builder setRangingUpdateRate(@RangingUpdateRate int rate) {
            mRangingUpdateRate = rate;
            return this;
        }

        /**
         * Sets the slot duration in milliseconds for the ranging session.
         * <p> Defaults to {@link #DURATION_2_MS}.
         *
         * @param durationMs the slot duration {@link SlotDuration}
         * @return this Builder instance.
         * @throws IllegalArgumentException if the provided duration is out of range.
         */
        @NonNull
        public Builder setSlotDuration(@SlotDuration int durationMs) {
            mSlotDuration = durationMs;
            return this;
        }

        /**
         * Builds a new instance of {@link UwbRangingParams}.
         *
         * @return a new instance of {@link UwbRangingParams} created using the current state of
         * the builder.
         */
        @NonNull
        public UwbRangingParams build() {
            return new UwbRangingParams(this);
        }
    }

    @Override
    public String toString() {
        return "UwbRangingParams{ "
                + "mSessionId="
                + mSessionId
                + ", mSubSessionId="
                + mSubSessionId
                + ", mConfigId="
                + mConfigId
                + ", mDeviceAddress="
                + mDeviceAddress
                + ", mSessionKeyInfo="
                + Arrays.toString(mSessionKeyInfo)
                + ", mSubSessionKeyInfo="
                + Arrays.toString(mSubSessionKeyInfo)
                + ", mComplexChannel="
                + mComplexChannel
                + ", mPeerAddress="
                + mPeerAddress
                + ", mRangingUpdateRate="
                + mRangingUpdateRate
                + ", mSlotDurationMillis="
                + mSlotDurationMillis
                + " }";
    }
}
