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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represent input params to the getAdservicesCommonStates API.
 *
 * @hide
 */
public final class GetAdServicesCommonStatesParams implements Parcelable {
    private final String mSdkPackageName;
    private final String mAppPackageName;

    private GetAdServicesCommonStatesParams(
            @Nullable String sdkPackageName, @NonNull String appPackageName) {
        mSdkPackageName = sdkPackageName;
        mAppPackageName = appPackageName;
    }

    private GetAdServicesCommonStatesParams(@NonNull Parcel in) {
        mSdkPackageName = in.readString();
        mAppPackageName = in.readString();
    }

    @NonNull
    public static final Creator<GetAdServicesCommonStatesParams> CREATOR =
            new Creator<GetAdServicesCommonStatesParams>() {
                @Override
                public GetAdServicesCommonStatesParams createFromParcel(Parcel in) {
                    return new GetAdServicesCommonStatesParams(in);
                }

                @Override
                public GetAdServicesCommonStatesParams[] newArray(int size) {
                    return new GetAdServicesCommonStatesParams[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeString(mSdkPackageName);
        out.writeString(mAppPackageName);
    }

    /** Get the Sdk Package Name. This is the package name in the Manifest. */
    @NonNull
    public String getSdkPackageName() {
        return mSdkPackageName;
    }

    /** Get the App PackageName. */
    @NonNull
    public String getAppPackageName() {
        return mAppPackageName;
    }

    /** Builder for {@link GetAdServicesCommonStatesParams} objects. */
    public static final class Builder {
        private String mSdkPackageName;
        private String mAppPackageName;

        public Builder(String appPackageName, String sdkPackageName) {
            mAppPackageName = appPackageName;
            mSdkPackageName = sdkPackageName;
        }

        /**
         * Set the Sdk Package Name. When the app calls the AdId API directly without using an SDK,
         * don't set this field.
         */
        public @NonNull Builder setSdkPackageName(@NonNull String sdkPackageName) {
            mSdkPackageName = sdkPackageName;
            return this;
        }

        /** Set the App PackageName. */
        public @NonNull Builder setAppPackageName(@NonNull String appPackageName) {
            mAppPackageName = appPackageName;
            return this;
        }

        /** Builds a {@link GetAdServicesCommonStatesParams} instance. */
        public @NonNull GetAdServicesCommonStatesParams build() {
            if (mAppPackageName == null || mAppPackageName.isEmpty()) {
                throw new IllegalArgumentException("App PackageName must not be empty or null");
            }

            return new GetAdServicesCommonStatesParams(mSdkPackageName, mAppPackageName);
        }
    }
}
