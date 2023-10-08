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

package android.app.adservices.consent;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represent a User Consent.
 *
 * @hide
 */
public final class ConsentParcel implements Parcelable {
    /**
     * Consent Api Types.
     *
     * @hide
     */
    @IntDef(value = {UNKNOWN, ALL_API, TOPICS, FLEDGE, MEASUREMENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ConsentApiType {}

    /** The Consent API Type is not set. */
    public static final int UNKNOWN = 0;

    /** The Consent API Type for All API. This is used when there is only 1 consent for all APIs */
    public static final int ALL_API = 1;

    /** The Consent API Type for Topics. */
    public static final int TOPICS = 2;

    /** The Consent API Type for FLEDGE. */
    public static final int FLEDGE = 3;

    /** The Consent API Type for Measurement. */
    public static final int MEASUREMENT = 4;

    private final boolean mIsGiven;
    @ConsentApiType private final int mConsentApiType;

    private ConsentParcel(@NonNull Builder builder) {
        mIsGiven = builder.mIsGiven;
        mConsentApiType = builder.mConsentApiType;
    }

    private ConsentParcel(@NonNull Parcel in) {
        mConsentApiType = in.readInt();
        mIsGiven = in.readBoolean();
    }

    public static final @NonNull Creator<ConsentParcel> CREATOR =
            new Parcelable.Creator<ConsentParcel>() {
                @Override
                public ConsentParcel createFromParcel(Parcel in) {
                    return new ConsentParcel(in);
                }

                @Override
                public ConsentParcel[] newArray(int size) {
                    return new ConsentParcel[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeInt(mConsentApiType);
        out.writeBoolean(mIsGiven);
    }

    /** Get the ConsentApiType. */
    @ConsentApiType
    public int getConsentApiType() {
        return mConsentApiType;
    }

    /** Get the IsGiven. */
    public boolean isIsGiven() {
        return mIsGiven;
    }

    /** Create a REVOKED consent for the consentApiType */
    public static ConsentParcel createRevokedConsent(@ConsentApiType int consentApiType) {
        return new ConsentParcel.Builder()
                .setConsentApiType(consentApiType)
                .setIsGiven(false)
                .build();
    }

    /** Create a GIVEN consent for the consentApiType */
    public static ConsentParcel createGivenConsent(@ConsentApiType int consentApiType) {
        return new ConsentParcel.Builder()
                .setConsentApiType(consentApiType)
                .setIsGiven(true)
                .build();
    }

    /** Builder for {@link ConsentParcel} objects. */
    public static final class Builder {
        @ConsentApiType private int mConsentApiType = UNKNOWN;
        private boolean mIsGiven = false;

        public Builder() {}

        /** Set the ConsentApiType for this request */
        public @NonNull Builder setConsentApiType(@ConsentApiType int consentApiType) {
            mConsentApiType = consentApiType;
            return this;
        }

        /** Set the IsGiven */
        public @NonNull Builder setIsGiven(Boolean isGiven) {
            // null input means isGiven = false
            mIsGiven = isGiven != null ? isGiven : false;
            return this;
        }

        /** Builds a {@link ConsentParcel} instance. */
        public @NonNull ConsentParcel build() {

            if (mConsentApiType == UNKNOWN) {
                throw new IllegalArgumentException("One must set the valid ConsentApiType");
            }

            return new ConsentParcel(this);
        }
    }
}
