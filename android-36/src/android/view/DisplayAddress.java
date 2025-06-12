/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.view;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

/** Display identifier that is stable across reboots.
 *
 * @hide
 */
public abstract class DisplayAddress implements Parcelable {
    /**
     * Creates an address for a physical display given its stable ID.
     *
     * A physical display ID is stable if the display can be identified using EDID information.
     *
     * @param physicalDisplayId A physical display ID.
     * @return The {@link Physical} address.
     * @see com.android.server.display.DisplayControl#getPhysicalDisplayIds
     */
    @NonNull
    public static Physical fromPhysicalDisplayId(long physicalDisplayId) {
        return new Physical(physicalDisplayId);
    }

    /**
     * Creates an address for a physical display given its port and model.
     *
     * @param port A port in the range [0, 255].
     * @param model A positive integer, or {@code null} if the model cannot be identified.
     * @return The {@link Physical} address.
     */
    @NonNull
    public static Physical fromPortAndModel(int port, Long model) {
        return new Physical(port, model);
    }

    /**
     * Creates an address for a network display given its MAC address.
     *
     * @param macAddress A MAC address in colon notation.
     * @return The {@link Network} address.
     */
    @NonNull
    public static Network fromMacAddress(String macAddress) {
        return new Network(macAddress);
    }

    /**
     * Address for a physically connected display.
     *
     * A {@link Physical} address is represented by a 64-bit identifier combining the port and model
     * of a display. The port, located in the least significant byte, uniquely identifies a physical
     * connector on the device for display output like eDP or HDMI. The model, located in the upper
     * bits, uniquely identifies a display model across manufacturers by encoding EDID information.
     * While the port is always stable, the model may not be available if EDID identification is not
     * supported by the platform, in which case the address is not unique.
     */
    public static final class Physical extends DisplayAddress {
        private static final long UNKNOWN_MODEL = 0;
        private static final int MODEL_SHIFT = 8;

        private final long mPhysicalDisplayId;

        /**
         * Stable display ID combining port and model.
         *
         * @return An ID in the range [0, 2^64) interpreted as signed.
         * @see com.android.server.display.DisplayControl#getPhysicalDisplayIds
         */
        public long getPhysicalDisplayId() {
            return mPhysicalDisplayId;
        }

        /**
         * Physical port to which the display is connected.
         *
         * @return A port in the range [0, 255].
         */
        public int getPort() {
            return (int) (mPhysicalDisplayId & 0xFF);
        }

        /**
         * Model identifier unique across manufacturers.
         *
         * @return A positive integer, or {@code null} if the model cannot be identified.
         */
        @Nullable
        public Long getModel() {
            final long model = mPhysicalDisplayId >>> MODEL_SHIFT;
            return model == UNKNOWN_MODEL ? null : model;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof Physical
                    && mPhysicalDisplayId == ((Physical) other).mPhysicalDisplayId;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder("{")
                    .append("port=").append(getPort());

            final Long model = getModel();
            if (model != null) {
                builder.append(", model=0x").append(Long.toHexString(model));
            }

            return builder.append("}").toString();
        }

        @Override
        public int hashCode() {
            return Long.hashCode(mPhysicalDisplayId);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeLong(mPhysicalDisplayId);
        }

        /**
         * This method is meant to check to see if the ports match
         * @param a1 Address to compare
         * @param a2 Address to compare
         *
         * @return true if the arguments have the same port, and at least one does not specify
         *         a model.
         */
        public static boolean isPortMatch(DisplayAddress a1, DisplayAddress a2) {
            // Both displays must be of type Physical
            if (!(a1 instanceof Physical && a2 instanceof Physical)) {
                return false;
            }
            Physical p1 = (Physical) a1;
            Physical p2 = (Physical) a2;

            // If both addresses specify a model, fallback to a basic match check (which
            // also checks the port).
            if (p1.getModel() != null && p2.getModel() != null) {
                return p1.equals(p2);
            }
            return p1.getPort() == p2.getPort();
        }

        private Physical(long physicalDisplayId) {
            mPhysicalDisplayId = physicalDisplayId;
        }

        private Physical(int port, Long model) {
            if (port < 0 || port > 255) {
                throw new IllegalArgumentException("The port should be in the interval [0, 255]");
            }
            mPhysicalDisplayId = Integer.toUnsignedLong(port)
                    | (model == null ? UNKNOWN_MODEL : (model << MODEL_SHIFT));
        }

        public static final @NonNull Parcelable.Creator<Physical> CREATOR =
                new Parcelable.Creator<Physical>() {
                    @Override
                    public Physical createFromParcel(Parcel in) {
                        return new Physical(in.readLong());
                    }

                    @Override
                    public Physical[] newArray(int size) {
                        return new Physical[size];
                    }
                };
    }

    /**
     * Address for a network-connected display.
     */
    public static final class Network extends DisplayAddress {
        private final String mMacAddress;

        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof Network && mMacAddress.equals(((Network) other).mMacAddress);
        }

        @Override
        public String toString() {
            return mMacAddress;
        }

        @Override
        public int hashCode() {
            return mMacAddress.hashCode();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeString(mMacAddress);
        }

        private Network(String macAddress) {
            mMacAddress = macAddress;
        }

        public static final @NonNull Parcelable.Creator<Network> CREATOR =
                new Parcelable.Creator<Network>() {
                    @Override
                    public Network createFromParcel(Parcel in) {
                        return new Network(in.readString());
                    }

                    @Override
                    public Network[] newArray(int size) {
                        return new Network[size];
                    }
                };
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
