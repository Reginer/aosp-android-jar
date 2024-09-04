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

package android.adservices.measurement;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Internal source registration request object to communicate from {@link MeasurementManager} to
 * {@link IMeasurementService}.
 *
 * @hide
 */
public class WebSourceRegistrationRequestInternal implements Parcelable {
    /** Creator for Parcelable (via reflection). */
    public static final Parcelable.Creator<WebSourceRegistrationRequestInternal> CREATOR =
            new Parcelable.Creator<WebSourceRegistrationRequestInternal>() {
                @Override
                public WebSourceRegistrationRequestInternal createFromParcel(Parcel in) {
                    return new WebSourceRegistrationRequestInternal(in);
                }

                @Override
                public WebSourceRegistrationRequestInternal[] newArray(int size) {
                    return new WebSourceRegistrationRequestInternal[size];
                }
            };
    /** Holds input to measurement source registration calls from web context. */
    @NonNull private final WebSourceRegistrationRequest mSourceRegistrationRequest;
    /** Holds app package info of where the request is coming from. */
    @NonNull private final String mAppPackageName;
    /** Holds sdk package info of where the request is coming from. */
    @NonNull private final String mSdkPackageName;
    /** Time the request was created, as millis since boot excluding time in deep sleep. */
    private final long mRequestTime;
    /** AD ID Permission Granted. */
    private final boolean mIsAdIdPermissionGranted;

    private WebSourceRegistrationRequestInternal(@NonNull Builder builder) {
        mSourceRegistrationRequest = builder.mSourceRegistrationRequest;
        mAppPackageName = builder.mAppPackageName;
        mSdkPackageName = builder.mSdkPackageName;
        mRequestTime = builder.mRequestTime;
        mIsAdIdPermissionGranted = builder.mIsAdIdPermissionGranted;
    }

    private WebSourceRegistrationRequestInternal(Parcel in) {
        Objects.requireNonNull(in);
        mSourceRegistrationRequest = WebSourceRegistrationRequest.CREATOR.createFromParcel(in);
        mAppPackageName = in.readString();
        mSdkPackageName = in.readString();
        mRequestTime = in.readLong();
        mIsAdIdPermissionGranted = in.readBoolean();
    }

    /** Getter for {@link #mSourceRegistrationRequest}. */
    public WebSourceRegistrationRequest getSourceRegistrationRequest() {
        return mSourceRegistrationRequest;
    }

    /** Getter for {@link #mAppPackageName}. */
    public String getAppPackageName() {
        return mAppPackageName;
    }

    /** Getter for {@link #mSdkPackageName}. */
    public String getSdkPackageName() {
        return mSdkPackageName;
    }

    /** Getter for {@link #mRequestTime}. */
    public long getRequestTime() {
        return mRequestTime;
    }

    /** Getter for {@link #mIsAdIdPermissionGranted}. */
    public boolean isAdIdPermissionGranted() {
        return mIsAdIdPermissionGranted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WebSourceRegistrationRequestInternal)) return false;
        WebSourceRegistrationRequestInternal that = (WebSourceRegistrationRequestInternal) o;
        return Objects.equals(mSourceRegistrationRequest, that.mSourceRegistrationRequest)
                && Objects.equals(mAppPackageName, that.mAppPackageName)
                && Objects.equals(mSdkPackageName, that.mSdkPackageName)
                && mRequestTime == that.mRequestTime
                && mIsAdIdPermissionGranted == that.mIsAdIdPermissionGranted;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mSourceRegistrationRequest,
                mAppPackageName,
                mSdkPackageName,
                mIsAdIdPermissionGranted);
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
        out.writeLong(mRequestTime);
        out.writeBoolean(mIsAdIdPermissionGranted);
    }

    /** Builder for {@link WebSourceRegistrationRequestInternal}. */
    public static final class Builder {
        /** External source registration request from client app SDK. */
        @NonNull private final WebSourceRegistrationRequest mSourceRegistrationRequest;
        /** Package name of the app used for the registration. Used to determine the registrant. */
        @NonNull private final String mAppPackageName;
        /** Package name of the sdk used for the registration. */
        @NonNull private final String mSdkPackageName;
        /** Time the request was created, as millis since boot excluding time in deep sleep. */
        private final long mRequestTime;
        /** AD ID Permission Granted. */
        private boolean mIsAdIdPermissionGranted;
        /**
         * Builder constructor for {@link WebSourceRegistrationRequestInternal}.
         *
         * @param sourceRegistrationRequest external source registration request
         * @param appPackageName app package name that is calling PP API
         * @param sdkPackageName sdk package name that is calling PP API
         */
        public Builder(
                @NonNull WebSourceRegistrationRequest sourceRegistrationRequest,
                @NonNull String appPackageName,
                @NonNull String sdkPackageName,
                long requestTime) {
            Objects.requireNonNull(sourceRegistrationRequest);
            Objects.requireNonNull(appPackageName);
            Objects.requireNonNull(sdkPackageName);
            mSourceRegistrationRequest = sourceRegistrationRequest;
            mAppPackageName = appPackageName;
            mSdkPackageName = sdkPackageName;
            mRequestTime = requestTime;
        }

        /** Pre-validates parameters and builds {@link WebSourceRegistrationRequestInternal}. */
        @NonNull
        public WebSourceRegistrationRequestInternal build() {
            return new WebSourceRegistrationRequestInternal(this);
        }

        /** See {@link WebSourceRegistrationRequestInternal#isAdIdPermissionGranted()}. */
        public WebSourceRegistrationRequestInternal.Builder setAdIdPermissionGranted(
                boolean isAdIdPermissionGranted) {
            mIsAdIdPermissionGranted = isAdIdPermissionGranted;
            return this;
        }
    }
}
