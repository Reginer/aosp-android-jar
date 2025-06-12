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
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import android.ranging.RangingConfig;

import com.android.ranging.flags.Flags;

/**
 * Represents the configuration for an Out-of-Band (OOB) responder in a ranging session.
 * This class contains configuration and device handle information for establishing
 * a ranging session with an initiator.
 */
@FlaggedApi(Flags.FLAG_RANGING_STACK_ENABLED)
public final class OobResponderRangingConfig extends RangingConfig implements Parcelable {

    private final android.ranging.oob.DeviceHandle mDeviceHandle;

    private OobResponderRangingConfig(Builder builder) {
        setRangingSessionType(RangingConfig.RANGING_SESSION_OOB);
        mDeviceHandle = builder.mDeviceHandle;
    }


    private OobResponderRangingConfig(Parcel in) {
        setRangingSessionType(in.readInt());
        mDeviceHandle = in.readParcelable(
                DeviceHandle.class.getClassLoader(), android.ranging.oob.DeviceHandle.class);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(getRangingSessionType());
        dest.writeParcelable(mDeviceHandle, flags);
    }

    @NonNull
    public static final Creator<OobResponderRangingConfig> CREATOR =
            new Creator<OobResponderRangingConfig>() {
                @Override
                public OobResponderRangingConfig createFromParcel(Parcel in) {
                    return new OobResponderRangingConfig(in);
                }

                @Override
                public OobResponderRangingConfig[] newArray(int size) {
                    return new OobResponderRangingConfig[size];
                }
            };

    /**
     * Returns the DeviceHandle associated with this OOB responder.
     *
     * @return The DeviceHandle of the OOB responder.
     */
    @NonNull
    public android.ranging.oob.DeviceHandle getDeviceHandle() {
        return mDeviceHandle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Builder class for creating instances of {@link OobResponderRangingConfig}.
     */
    public static final class Builder {
        private final DeviceHandle mDeviceHandle;

        /**
         * Constructs a new Builder instance with the specified DeviceHandle.
         *
         * @param deviceHandle The DeviceHandle to associate with this OOB responder.
         */
        public Builder(@NonNull DeviceHandle deviceHandle) {
            mDeviceHandle = deviceHandle;
        }

        /**
         * Builds an instance of {@link OobResponderRangingConfig} with the provided parameters.
         *
         * @return A new OobResponderRangingParams instance.
         */
        @NonNull
        public OobResponderRangingConfig build() {
            return new OobResponderRangingConfig(this);
        }
    }

    @Override
    public String toString() {
        return "OobResponderRangingParams{ "
                + "mDeviceHandle="
                + mDeviceHandle
                + ", "
                + super.toString()
                + ", "
                + " }";
    }
}
