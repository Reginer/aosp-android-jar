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

package android.adservices.measurement;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.adservices.AdServicesParcelableUtil;

import java.util.Objects;

/**
 * Internal source registration request object to communicate from {@link MeasurementManager} to
 * {@link IMeasurementService}.
 *
 * @hide
 */
public class SourceRegistrationRequestInternal implements Parcelable {
    /** Holds input to measurement source registration calls. */
    @NonNull private final SourceRegistrationRequest mSourceRegistrationRequest;
    /** Caller app package name. */
    @NonNull private final String mAppPackageName;
    /** Calling SDK package name. */
    @NonNull private final String mSdkPackageName;
    /** Time the request was created, in millis since boot excluding time in deep sleep. */
    private final long mBootRelativeRequestTime;
    /** Ad ID value if the permission is granted, null otherwise. */
    @Nullable private final String mAdIdValue;

    private SourceRegistrationRequestInternal(@NonNull Builder builder) {
        mSourceRegistrationRequest = builder.mRegistrationRequest;
        mAppPackageName = builder.mAppPackageName;
        mSdkPackageName = builder.mSdkPackageName;
        mBootRelativeRequestTime = builder.mBootRelativeRequestTime;
        mAdIdValue = builder.mAdIdValue;
    }

    private SourceRegistrationRequestInternal(@NonNull Parcel in) {
        Objects.requireNonNull(in);
        mSourceRegistrationRequest = SourceRegistrationRequest.CREATOR.createFromParcel(in);
        mAppPackageName = in.readString();
        mSdkPackageName = in.readString();
        mBootRelativeRequestTime = in.readLong();
        mAdIdValue = AdServicesParcelableUtil.readNullableFromParcel(in, Parcel::readString);
    }

    /** Holds input to measurement source registration calls from app context. */
    @NonNull
    public SourceRegistrationRequest getSourceRegistrationRequest() {
        return mSourceRegistrationRequest;
    }

    /** Caller app package name. */
    @NonNull
    public String getAppPackageName() {
        return mAppPackageName;
    }

    /** Calling SDK package name. */
    @NonNull
    public String getSdkPackageName() {
        return mSdkPackageName;
    }

    /** Time the request was created, in millis since boot excluding time in deep sleep. */
    @NonNull
    public long getBootRelativeRequestTime() {
        return mBootRelativeRequestTime;
    }

    /** Ad ID value if the permission is granted, null otherwise. */
    @Nullable
    public String getAdIdValue() {
        return mAdIdValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SourceRegistrationRequestInternal)) return false;
        SourceRegistrationRequestInternal that = (SourceRegistrationRequestInternal) o;
        return Objects.equals(mSourceRegistrationRequest, that.mSourceRegistrationRequest)
                && Objects.equals(mAppPackageName, that.mAppPackageName)
                && Objects.equals(mSdkPackageName, that.mSdkPackageName)
                && mBootRelativeRequestTime == that.mBootRelativeRequestTime
                && Objects.equals(mAdIdValue, that.mAdIdValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mSourceRegistrationRequest,
                mAppPackageName,
                mSdkPackageName,
                mBootRelativeRequestTime,
                mAdIdValue);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        Objects.requireNonNull(out);
        mSourceRegistrationRequest.writeToParcel(out, flags);
        out.writeString(mAppPackageName);
        out.writeString(mSdkPackageName);
        out.writeLong(mBootRelativeRequestTime);
        AdServicesParcelableUtil.writeNullableToParcel(out, mAdIdValue, Parcel::writeString);
    }

    /** Builder for {@link SourceRegistrationRequestInternal}. */
    public static final class Builder {
        /** External source registration request from client app SDK. */
        @NonNull private final SourceRegistrationRequest mRegistrationRequest;
        /** Package name of the app used for the registration. Used to determine the registrant. */
        @NonNull private final String mAppPackageName;
        /** Package name of the sdk used for the registration. */
        @NonNull private final String mSdkPackageName;
        /** Time the request was created, in millis since boot excluding time in deep sleep. */
        private final long mBootRelativeRequestTime;
        /** AD ID value if the permission was granted. */
        @Nullable private String mAdIdValue;
        /**
         * Builder constructor for {@link SourceRegistrationRequestInternal}.
         *
         * @param registrationRequest external source registration request
         * @param appPackageName app package name that is calling PP API
         * @param sdkPackageName sdk package name that is calling PP API
         */
        public Builder(
                @NonNull SourceRegistrationRequest registrationRequest,
                @NonNull String appPackageName,
                @NonNull String sdkPackageName,
                long bootRelativeRequestTime) {
            Objects.requireNonNull(registrationRequest);
            Objects.requireNonNull(appPackageName);
            Objects.requireNonNull(sdkPackageName);
            mRegistrationRequest = registrationRequest;
            mAppPackageName = appPackageName;
            mSdkPackageName = sdkPackageName;
            mBootRelativeRequestTime = bootRelativeRequestTime;
        }

        /** Pre-validates parameters and builds {@link SourceRegistrationRequestInternal}. */
        @NonNull
        public SourceRegistrationRequestInternal build() {
            return new SourceRegistrationRequestInternal(this);
        }

        /** See {@link SourceRegistrationRequestInternal#getAdIdValue()}. */
        public SourceRegistrationRequestInternal.Builder setAdIdValue(@Nullable String adIdValue) {
            mAdIdValue = adIdValue;
            return this;
        }
    }

    /** Creator for Parcelable (via reflection). */
    public static final Creator<SourceRegistrationRequestInternal> CREATOR =
            new Creator<>() {
                @Override
                public SourceRegistrationRequestInternal createFromParcel(Parcel in) {
                    return new SourceRegistrationRequestInternal(in);
                }

                @Override
                public SourceRegistrationRequestInternal[] newArray(int size) {
                    return new SourceRegistrationRequestInternal[size];
                }
            };
}
