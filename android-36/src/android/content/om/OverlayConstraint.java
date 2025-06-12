/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.content.om;

import android.annotation.IntDef;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Objects;

/**
 * Constraint for enabling a RRO. Currently this can be a displayId or a deviceId, i.e.,
 * the overlay would be applied only when a target package is running on the given displayId
 * or deviceId.
 *
 * @hide
 */
public final class OverlayConstraint implements Parcelable {

    /**
     * Constraint type for enabling a RRO for a specific display id. For contexts associated with
     * the default display, this would be {@link android.view.Display#DEFAULT_DISPLAY}, and
     * for contexts associated with a virtual display, this would be the id of the virtual display.
     */
    public static final int TYPE_DISPLAY_ID = 0;

    /**
     * Constraint type for enabling a RRO for a specific device id. For contexts associated with
     * the default device, this would be {@link android.content.Context#DEVICE_ID_DEFAULT}, and
     * for contexts associated with virtual device, this would be the id of the virtual device.
     */
    public static final int TYPE_DEVICE_ID = 1;

    @IntDef(prefix = "TYPE_", value = {
            TYPE_DISPLAY_ID,
            TYPE_DEVICE_ID,
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface ConstraintType {
    }

    @ConstraintType
    private final int mType;
    private final int mValue;

    public OverlayConstraint(int type, int value) {
        if (type != TYPE_DEVICE_ID && type != TYPE_DISPLAY_ID) {
            throw new IllegalArgumentException(
                    "Type must be either TYPE_DISPLAY_ID or TYPE_DEVICE_ID");
        }
        if (value < 0) {
            throw new IllegalArgumentException("Value must be greater than 0");
        }
        this.mType = type;
        this.mValue = value;
    }

    private OverlayConstraint(Parcel in) {
        this(in.readInt(), in.readInt());
    }

    /**
     * Returns the type of the constraint.
     */
    public int getType() {
        return mType;
    }

    /**
     * Returns the value of the constraint.
     */
    public int getValue() {
        return mValue;
    }

    @Override
    public String toString() {
        return "{type: " + typeToString(mType) + ", value: " + mValue + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OverlayConstraint that)) {
            return false;
        }
        return mType == that.mType && mValue == that.mValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mType, mValue);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mType);
        dest.writeInt(mValue);
    }

    public static final Creator<OverlayConstraint> CREATOR = new Creator<>() {
        @Override
        public OverlayConstraint createFromParcel(Parcel in) {
            return new OverlayConstraint(in);
        }

        @Override
        public OverlayConstraint[] newArray(int size) {
            return new OverlayConstraint[size];
        }
    };

    /**
     * Returns a string description for a list of constraints.
     */
    public static String constraintsToString(final List<OverlayConstraint> overlayConstraints) {
        if (overlayConstraints == null || overlayConstraints.isEmpty()) {
            return "None";
        }
        return "[" + TextUtils.join(",", overlayConstraints) + "]";
    }

    private static String typeToString(@ConstraintType int type) {
        return type == TYPE_DEVICE_ID ? "DEVICE_ID" : "DISPLAY_ID";
    }
}
