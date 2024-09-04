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
package android.adservices.common;

import static android.adservices.common.ConsentStatus.ConsentStatusCode;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.adservices.flags.Flags;

import java.util.Objects;

/**
 * Represent the common states from the getAdservicesCommonStates API.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_GET_ADSERVICES_COMMON_STATES_API_ENABLED)
public final class AdServicesCommonStates implements Parcelable {
    @ConsentStatusCode private final int mMeasurementState;
    @ConsentStatusCode private final int mPaState;

    /**
     * Creates an object which represents the result from the getAdservicesCommonStates API.
     *
     * @param measurementState a {@link ConsentStatusCode} int indicating whether meansurement is
     *     allowed
     * @param paState a {@link ConsentStatusCode} indicating whether fledge is allowed
     */
    private AdServicesCommonStates(
            @ConsentStatusCode int measurementState, @ConsentStatusCode int paState) {
        this.mMeasurementState = measurementState;
        this.mPaState = paState;
    }

    private AdServicesCommonStates(@NonNull Parcel in) {
        this.mMeasurementState = in.readInt();
        this.mPaState = in.readInt();
    }

    @NonNull
    public static final Creator<AdServicesCommonStates> CREATOR =
            new Creator<AdServicesCommonStates>() {
                @Override
                public AdServicesCommonStates createFromParcel(Parcel in) {
                    Objects.requireNonNull(in);
                    return new AdServicesCommonStates(in);
                }

                @Override
                public AdServicesCommonStates[] newArray(int size) {
                    return new AdServicesCommonStates[size];
                }
            };

    /** describe contents for parcel */
    public int describeContents() {
        return 0;
    }

    /** write contents for parcel */
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeInt(mMeasurementState);
        out.writeInt(mPaState);
    }

    /** Get the measurement allowed state. */
    @ConsentStatusCode
    public int getMeasurementState() {
        return mMeasurementState;
    }

    /** Get the fledge allowed state. */
    @ConsentStatusCode
    public int getPaState() {
        return mPaState;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof AdServicesCommonStates)) return false;
        AdServicesCommonStates adservicesCommonStates = (AdServicesCommonStates) object;
        return getMeasurementState() == adservicesCommonStates.getMeasurementState()
                && getPaState() == adservicesCommonStates.getPaState();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMeasurementState(), getPaState());
    }

    @Override
    public String toString() {
        return "AdservicesCommonStates{"
                + "mMeasurementState="
                + mMeasurementState
                + ", mPaState="
                + mPaState
                + '}';
    }

    /**
     * Builder for {@link AdServicesCommonStates} objects.
     *
     * @hide
     */
    public static final class Builder {
        @ConsentStatusCode private int mMeasurementState;
        @ConsentStatusCode private int mPaState;

        public Builder() {}

        /** Set the measurement allowed by the getAdServicesCommonStates API */
        @NonNull
        public AdServicesCommonStates.Builder setMeasurementState(
                @ConsentStatusCode int measurementState) {
            mMeasurementState = measurementState;
            return this;
        }

        /** Set the pa allowed by the getAdServicesCommonStates API. */
        @NonNull
        public AdServicesCommonStates.Builder setPaState(@ConsentStatusCode int paState) {
            mPaState = paState;
            return this;
        }

        /** Builds a {@link AdServicesCommonStates} instance. */
        @NonNull
        public AdServicesCommonStates build() {
            return new AdServicesCommonStates(mMeasurementState, mPaState);
        }
    }
}
