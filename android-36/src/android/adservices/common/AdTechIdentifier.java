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

/** An Identifier representing an ad buyer or seller. */
public final class AdTechIdentifier implements Parcelable {
    /** @hide */
    public static final AdTechIdentifier UNSET_AD_TECH_IDENTIFIER =
            AdTechIdentifier.fromString("", false);

    @NonNull private final String mIdentifier;

    private AdTechIdentifier(@NonNull Parcel in) {
        this(in.readString());
    }

    private AdTechIdentifier(@NonNull String adTechIdentifier) {
        this(adTechIdentifier, true);
    }

    private AdTechIdentifier(@NonNull String adTechIdentifier, boolean validate) {
        Objects.requireNonNull(adTechIdentifier, "Input ad tech identifier must not be null");
        if (validate) {
            validate(adTechIdentifier);
        }
        mIdentifier = adTechIdentifier;
    }

    @NonNull
    public static final Creator<AdTechIdentifier> CREATOR =
            new Creator<AdTechIdentifier>() {
                @Override
                public AdTechIdentifier createFromParcel(Parcel in) {
                    Objects.requireNonNull(in);
                    return new AdTechIdentifier(in);
                }

                @Override
                public AdTechIdentifier[] newArray(int size) {
                    return new AdTechIdentifier[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);
        dest.writeString(mIdentifier);
    }

    /**
     * Compares this AdTechIdentifier to the specified object. The result is true if and only if the
     * argument is not null and is a AdTechIdentifier object with the same string form (obtained by
     * calling {@link #toString()}). Note that this method will not perform any eTLD+1 normalization
     * so two AdTechIdentifier objects with the same eTLD+1 could be not equal if the String
     * representations of the objects was not equal.
     *
     * @param o The object to compare this AdTechIdentifier against
     * @return true if the given object represents an AdTechIdentifier equivalent to this
     *     AdTechIdentifier, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof AdTechIdentifier
                && mIdentifier.equals(((AdTechIdentifier) o).toString());
    }

    /**
     * Returns a hash code corresponding to the string representation of this class obtained by
     * calling {@link #toString()}. Note that this method will not perform any eTLD+1 normalization
     * so two AdTechIdentifier objects with the same eTLD+1 could have different hash codes if the
     * underlying string representation was different.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return mIdentifier.hashCode();
    }

    /** @return The identifier in String form. */
    @Override
    @NonNull
    public String toString() {
        return mIdentifier;
    }

    /**
     * Construct an instance of this class from a String.
     *
     * @param source A valid eTLD+1 domain of an ad buyer or seller
     * @return An {@link AdTechIdentifier} object wrapping the given domain
     */
    @NonNull
    public static AdTechIdentifier fromString(@NonNull String source) {
        return AdTechIdentifier.fromString(source, true);
    }

    /**
     * Construct an instance of this class from a String.
     *
     * @param source A valid eTLD+1 domain of an ad buyer or seller.
     * @param validate Construction-time validation is run on the string if and only if this is
     *     true.
     * @return An {@link AdTechIdentifier} class wrapping the given domain.
     * @hide
     */
    @NonNull
    public static AdTechIdentifier fromString(@NonNull String source, boolean validate) {
        return new AdTechIdentifier(source, validate);
    }

    private void validate(String inputString) {
        // TODO(b/238849930) Bring existing validation function here
    }
}
