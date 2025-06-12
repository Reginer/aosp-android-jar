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

import android.adservices.common.AdServicesResponse;
import android.adservices.common.AdServicesStatusUtils;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Represent the result from the getAppSetId API.
 *
 * @hide
 */
public final class GetAppSetIdResult extends AdServicesResponse {
    @NonNull private final String mAppSetId;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        SCOPE_APP,
        SCOPE_DEVELOPER,
    })
    public @interface AppSetIdScope {}
    /** The appSetId is scoped to an app. All apps on a device will have a different appSetId. */
    public static final int SCOPE_APP = 1;

    /**
     * The appSetId is scoped to a developer account on an app store. All apps from the same
     * developer on a device will have the same developer scoped appSetId.
     */
    public static final int SCOPE_DEVELOPER = 2;

    private final @AppSetIdScope int mAppSetIdScope;

    private GetAppSetIdResult(
            @AdServicesStatusUtils.StatusCode int resultCode,
            @Nullable String errorMessage,
            @NonNull String appSetId,
            @AppSetIdScope int appSetIdScope) {
        super(resultCode, errorMessage);
        mAppSetId = appSetId;
        mAppSetIdScope = appSetIdScope;
    }

    private GetAppSetIdResult(@NonNull Parcel in) {
        super(in);
        Objects.requireNonNull(in);

        mAppSetId = in.readString();
        mAppSetIdScope = in.readInt();
    }

    public static final @NonNull Creator<GetAppSetIdResult> CREATOR =
            new Parcelable.Creator<GetAppSetIdResult>() {
                @Override
                public GetAppSetIdResult createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new GetAppSetIdResult(in);
                }

                @Override
                public GetAppSetIdResult[] newArray(int size) {
                    return new GetAppSetIdResult[size];
                }
            };

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeInt(mStatusCode);
        out.writeString(mErrorMessage);
        out.writeString(mAppSetId);
        out.writeInt(mAppSetIdScope);
    }

    /**
     * Returns the error message associated with this result.
     *
     * <p>If {@link #isSuccess} is {@code true}, the error message is always {@code null}. The error
     * message may be {@code null} even if {@link #isSuccess} is {@code false}.
     */
    @Nullable
    public String getErrorMessage() {
        return mErrorMessage;
    }

    /** Returns the AppSetId associated with this result. */
    @NonNull
    public String getAppSetId() {
        return mAppSetId;
    }

    /** Returns the AppSetId scope associated with this result. */
    public @AppSetIdScope int getAppSetIdScope() {
        return mAppSetIdScope;
    }

    @Override
    public String toString() {
        return "GetAppSetIdResult{"
                + "mResultCode="
                + mStatusCode
                + ", mErrorMessage='"
                + mErrorMessage
                + '\''
                + ", mAppSetId="
                + mAppSetId
                + ", mAppSetIdScope="
                + mAppSetIdScope
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof GetAppSetIdResult)) {
            return false;
        }

        GetAppSetIdResult that = (GetAppSetIdResult) o;

        return mStatusCode == that.mStatusCode
                && Objects.equals(mErrorMessage, that.mErrorMessage)
                && Objects.equals(mAppSetId, that.mAppSetId)
                && (mAppSetIdScope == that.mAppSetIdScope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mStatusCode, mErrorMessage, mAppSetId, mAppSetIdScope);
    }

    /**
     * Builder for {@link GetAppSetIdResult} objects.
     *
     * @hide
     */
    public static final class Builder {
        private @AdServicesStatusUtils.StatusCode int mStatusCode;
        @Nullable private String mErrorMessage;
        @NonNull private String mAppSetId;
        private @AppSetIdScope int mAppSetIdScope;

        public Builder() {}

        /** Set the Result Code. */
        public @NonNull Builder setStatusCode(@AdServicesStatusUtils.StatusCode int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Set the Error Message. */
        public @NonNull Builder setErrorMessage(@Nullable String errorMessage) {
            mErrorMessage = errorMessage;
            return this;
        }

        /** Set the appSetId. */
        public @NonNull Builder setAppSetId(@NonNull String appSetId) {
            mAppSetId = appSetId;
            return this;
        }

        /** Set the appSetId scope field. */
        public @NonNull Builder setAppSetIdScope(@AppSetIdScope int scope) {
            mAppSetIdScope = scope;
            return this;
        }

        /** Builds a {@link GetAppSetIdResult} instance. */
        public @NonNull GetAppSetIdResult build() {
            if (mAppSetId == null) {
                throw new IllegalArgumentException("appSetId is null");
            }

            return new GetAppSetIdResult(mStatusCode, mErrorMessage, mAppSetId, mAppSetIdScope);
        }
    }
}
