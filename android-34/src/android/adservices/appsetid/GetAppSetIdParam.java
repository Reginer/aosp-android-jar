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

package android.adservices.appsetid;

import static android.adservices.appsetid.AppSetIdManager.EMPTY_SDK;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represent input params to the getAppSetId API.
 *
 * @hide
 */
public final class GetAppSetIdParam implements Parcelable {
    private final String mSdkPackageName;
    private final String mAppPackageName;

    private GetAppSetIdParam(@Nullable String sdkPackageName, @NonNull String appPackageName) {
        mSdkPackageName = sdkPackageName;
        mAppPackageName = appPackageName;
    }

    private GetAppSetIdParam(@NonNull Parcel in) {
        mSdkPackageName = in.readString();
        mAppPackageName = in.readString();
    }

    public static final @NonNull Creator<GetAppSetIdParam> CREATOR =
            new Parcelable.Creator<GetAppSetIdParam>() {
                @Override
                public GetAppSetIdParam createFromParcel(Parcel in) {
                    return new GetAppSetIdParam(in);
                }

                @Override
                public GetAppSetIdParam[] newArray(int size) {
                    return new GetAppSetIdParam[size];
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

    /** Builder for {@link GetAppSetIdParam} objects. */
    public static final class Builder {
        private String mSdkPackageName;
        private String mAppPackageName;

        public Builder() {}

        /**
         * Set the Sdk Package Name. When the app calls the AppSetId API directly without using an
         * SDK, don't set this field.
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

        /** Builds a {@link GetAppSetIdParam} instance. */
        public @NonNull GetAppSetIdParam build() {
            if (mSdkPackageName == null) {
                // When Sdk package name is not set, we assume the App calls the AppSetId API
                // directly.
                // We set the Sdk package name to empty to mark this.
                mSdkPackageName = EMPTY_SDK;
            }

            if (mAppPackageName == null || mAppPackageName.isEmpty()) {
                throw new IllegalArgumentException("App PackageName must not be empty or null");
            }

            return new GetAppSetIdParam(mSdkPackageName, mAppPackageName);
        }
    }
}
