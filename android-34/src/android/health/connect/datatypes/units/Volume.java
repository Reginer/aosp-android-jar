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

package android.health.connect.datatypes.units;

import android.annotation.NonNull;

import java.util.Objects;

/** Represents a unit of volume. Supported units: liters */
public final class Volume implements Comparable<Volume> {
    private final double mInLiters;

    private Volume(double value) {
        mInLiters = value;
    }

    /**
     * Creates a Volume object with the specified value in liters.
     *
     * @param value value to be set as liters.
     */
    @NonNull
    public static Volume fromLiters(double value) {
        return new Volume(value);
    }

    /** Returns volume in liters */
    public double getInLiters() {
        return mInLiters;
    }

    /**
     * Compares this object with the specified object for order. Returns a negative integer, zero,
     * or a positive integer as this object is less than, equal to, or greater than the specified
     * object.
     *
     * @param other the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is less than, equal
     *     to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException if the specified object's type prevents it from being compared to
     *     this object.
     */
    @Override
    public int compareTo(@NonNull Volume other) {
        return Double.compare(this.mInLiters, other.mInLiters);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param object the reference object with which to compare.
     * @return {@code true} if this object is the same as the object argument; {@code false}
     *     otherwise.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object instanceof Volume) {
            Volume other = (Volume) object;
            return this.getInLiters() == other.getInLiters();
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
        return Objects.hash(this.getInLiters());
    }

    /**
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return mInLiters + " L";
    }
}
