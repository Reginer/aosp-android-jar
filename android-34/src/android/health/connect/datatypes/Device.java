/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.health.connect.datatypes;

import static android.health.connect.datatypes.validation.ValidationUtils.validateIntDefValue;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.Set;

/**
 * A physical device (such as phone, watch, scale, or chest strap) which captured associated health
 * data point.
 *
 * <p>Device needs to be populated by users of the API. Metadata fields not provided by clients will
 * remain absent.
 */
public final class Device {
    /**
     * @see Device
     */
    public static final class Builder {
        private String mManufacturer;
        private String mModel;
        @DeviceType private int mType = DEVICE_TYPE_UNKNOWN;

        /** Sets an optional client supplied manufacturer of the device */
        @NonNull
        public Builder setManufacturer(@Nullable String manufacturer) {
            mManufacturer = manufacturer;
            return this;
        }

        /** Sets an optional client supplied model of the device */
        @NonNull
        public Builder setModel(@Nullable String model) {
            mModel = model;
            return this;
        }

        /** Sets an optional client supplied type of the device */
        @NonNull
        public Builder setType(@DeviceType int type) {
            mType = type;
            return this;
        }

        /** Build and return {@link Device} object */
        @NonNull
        public Device build() {
            return new Device(mManufacturer, mModel, mType);
        }
    }

    public static final int DEVICE_TYPE_UNKNOWN = 0;
    public static final int DEVICE_TYPE_WATCH = 1;
    public static final int DEVICE_TYPE_PHONE = 2;
    public static final int DEVICE_TYPE_SCALE = 3;
    public static final int DEVICE_TYPE_RING = 4;
    public static final int DEVICE_TYPE_HEAD_MOUNTED = 5;
    public static final int DEVICE_TYPE_FITNESS_BAND = 6;
    public static final int DEVICE_TYPE_CHEST_STRAP = 7;
    public static final int DEVICE_TYPE_SMART_DISPLAY = 8;

    // Instant records
    private final String mManufacturer;
    private final String mModel;
    @DeviceType private final int mType;

    /**
     * @param manufacturer An optional client supplied manufacturer of the device
     * @param model An optional client supplied model of the device
     * @param type An optional client supplied type of the device
     */
    private Device(String manufacturer, String model, @DeviceType int type) {
        validateIntDefValue(type, Device.VALID_TYPES, DeviceType.class.getSimpleName());
        mManufacturer = manufacturer;
        mModel = model;
        mType = type;
    }

    /**
     * @return The device manufacturer if set, null otherwise
     */
    @Nullable
    public String getManufacturer() {
        return mManufacturer;
    }

    /**
     * @return The device model if set, null otherwise
     */
    @Nullable
    public String getModel() {
        return mModel;
    }

    /**
     * @return The device type if set {@code DEVICE_TYPE_UNKNOWN} otherwise
     */
    @DeviceType
    public int getType() {
        return mType;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param object the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     */
    @Override
    public boolean equals(@NonNull Object object) {
        if (this == object) return true;
        if (object instanceof Device) {
            Device other = (Device) object;
            return this.getType() == other.getType()
                    && Objects.equals(this.getManufacturer(), other.getManufacturer())
                    && Objects.equals(this.getModel(), other.getModel());
        }
        return false;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.getManufacturer(), this.getModel(), this.getType());
    }

    /**
     * Valid set of values for this IntDef. Update this set when add new type or deprecate existing
     * type.
     *
     * @hide
     */
    public static final Set<Integer> VALID_TYPES =
            Set.of(
                    DEVICE_TYPE_UNKNOWN,
                    DEVICE_TYPE_WATCH,
                    DEVICE_TYPE_PHONE,
                    DEVICE_TYPE_SCALE,
                    DEVICE_TYPE_RING,
                    DEVICE_TYPE_HEAD_MOUNTED,
                    DEVICE_TYPE_FITNESS_BAND,
                    DEVICE_TYPE_CHEST_STRAP,
                    DEVICE_TYPE_SMART_DISPLAY);

    /** @hide */
    @IntDef({
        DEVICE_TYPE_UNKNOWN,
        DEVICE_TYPE_WATCH,
        DEVICE_TYPE_PHONE,
        DEVICE_TYPE_SCALE,
        DEVICE_TYPE_RING,
        DEVICE_TYPE_HEAD_MOUNTED,
        DEVICE_TYPE_FITNESS_BAND,
        DEVICE_TYPE_CHEST_STRAP,
        DEVICE_TYPE_SMART_DISPLAY,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface DeviceType {}
}
