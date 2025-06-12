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

package android.ranging.raw;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import android.ranging.RangingConfig;

import com.android.ranging.flags.Flags;

/**
 * Represents the configuration for a raw ranging session initiated by a responder device.
 * This class holds a {@link android.ranging.raw.RawRangingDevice} object that participates in the
 * session.
 */
@FlaggedApi(Flags.FLAG_RANGING_STACK_ENABLED)
public final class RawResponderRangingConfig extends RangingConfig implements Parcelable {

    private final android.ranging.raw.RawRangingDevice mRawRangingDevice;

    private RawResponderRangingConfig(Builder builder) {
        setRangingSessionType(RangingConfig.RANGING_SESSION_RAW);
        mRawRangingDevice = builder.mRawRangingDevice;
    }

    private RawResponderRangingConfig(Parcel in) {
        setRangingSessionType(in.readInt());
        mRawRangingDevice = in.readParcelable(
                android.ranging.raw.RawRangingDevice.class.getClassLoader());
    }

    @NonNull
    public static final Creator<RawResponderRangingConfig> CREATOR =
            new Creator<RawResponderRangingConfig>() {
                @Override
                public RawResponderRangingConfig createFromParcel(Parcel in) {
                    return new RawResponderRangingConfig(in);
                }

                @Override
                public RawResponderRangingConfig[] newArray(int size) {
                    return new RawResponderRangingConfig[size];
                }
            };

    /**
     * Returns the {@link android.ranging.raw.RawRangingDevice} participating in this session as the
     * responder.
     *
     * @return the raw ranging device.
     */
    @NonNull
    public android.ranging.raw.RawRangingDevice getRawRangingDevice() {
        return mRawRangingDevice;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(getRangingSessionType());
        dest.writeParcelable(mRawRangingDevice, flags);
    }

    /**
     * Builder class for constructing instances of {@link RawResponderRangingParams}.
     */
    public static final class Builder {
        private RawRangingDevice mRawRangingDevice;

        /**
         * Sets the {@link android.ranging.raw.RawRangingDevice} for this responder session.
         *
         * @param rangingDevice the raw ranging device.
         * @return this {@link Builder} instance.
         */
        @NonNull
        public Builder setRawRangingDevice(
                @NonNull android.ranging.raw.RawRangingDevice rangingDevice) {
            mRawRangingDevice = rangingDevice;
            return this;
        }

        /**
         * Builds and returns a new {@link RawResponderRangingParams} instance.
         *
         * @return a configured instance of {@link RawResponderRangingParams}.
         */
        @NonNull
        public RawResponderRangingConfig build() {
            return new RawResponderRangingConfig(this);
        }
    }

    @Override
    public String toString() {
        return "RawResponderRangingParams{ "
                + "mRawRangingDevice="
                + mRawRangingDevice
                + ", "
                + super.toString()
                + ", "
                + " }";
    }
}




