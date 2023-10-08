/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.adservices.common;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * This class holds JSON that will be passed into a JavaScript function during ad selection. Its
 * contents are not used by <a
 * href="https://developer.android.com/design-for-safety/privacy-sandbox/fledge">FLEDGE</a> platform
 * code, but are merely validated and then passed to the appropriate JavaScript ad selection
 * function.
 */
public final class AdSelectionSignals implements Parcelable {

    public static final AdSelectionSignals EMPTY = fromString("{}");

    @NonNull private final String mSignals;

    private AdSelectionSignals(@NonNull Parcel in) {
        this(in.readString());
    }

    private AdSelectionSignals(@NonNull String adSelectionSignals) {
        this(adSelectionSignals, true);
    }

    private AdSelectionSignals(@NonNull String adSelectionSignals, boolean validate) {
        Objects.requireNonNull(adSelectionSignals);
        if (validate) {
            validate(adSelectionSignals);
        }
        mSignals = adSelectionSignals;
    }

    @NonNull
    public static final Creator<AdSelectionSignals> CREATOR =
            new Creator<AdSelectionSignals>() {
                @Override
                public AdSelectionSignals createFromParcel(Parcel in) {
                    Objects.requireNonNull(in);
                    return new AdSelectionSignals(in);
                }

                @Override
                public AdSelectionSignals[] newArray(int size) {
                    return new AdSelectionSignals[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mSignals);
    }

    /**
     * Compares this AdSelectionSignals to the specified object. The result is true if and only if
     * the argument is not null and is a AdSelectionSignals object with the same string form
     * (obtained by calling {@link #toString()}). Note that this method will not perform any JSON
     * normalization so two AdSelectionSignals objects with the same JSON could be not equal if the
     * String representations of the objects was not equal.
     *
     * @param o The object to compare this AdSelectionSignals against
     * @return true if the given object represents an AdSelectionSignals equivalent to this
     *     AdSelectionSignals, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof AdSelectionSignals
                && mSignals.equals(((AdSelectionSignals) o).toString());
    }

    /**
     * Returns a hash code corresponding to the string representation of this class obtained by
     * calling {@link #toString()}. Note that this method will not perform any JSON normalization so
     * two AdSelectionSignals objects with the same JSON could have different hash codes if the
     * underlying string representation was different.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return mSignals.hashCode();
    }

    /** @return The String form of the JSON wrapped by this class. */
    @Override
    @NonNull
    public String toString() {
        return mSignals;
    }

    /**
     * Creates an AdSelectionSignals from a given JSON in String form.
     *
     * @param source Any valid JSON string to create the AdSelectionSignals with.
     * @return An AdSelectionSignals object wrapping the given String.
     */
    @NonNull
    public static AdSelectionSignals fromString(@NonNull String source) {
        return new AdSelectionSignals(source, true);
    }

    /**
     * Creates an AdSelectionSignals from a given JSON in String form.
     *
     * @param source Any valid JSON string to create the AdSelectionSignals with.
     * @param validate Construction-time validation is run on the string if and only if this is
     *     true.
     * @return An AdSelectionSignals object wrapping the given String.
     * @hide
     */
    @NonNull
    public static AdSelectionSignals fromString(@NonNull String source, boolean validate) {
        return new AdSelectionSignals(source, validate);
    }

    /**
     * @return the signal's String form data size in bytes.
     * @hide
     */
    public int getSizeInBytes() {
        return this.mSignals.getBytes().length;
    }

    private void validate(String inputString) {
        // TODO(b/238849930) Bring the existing validation function in here
    }
}
