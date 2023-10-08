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
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/** Class holding source registration parameters. */
public final class WebSourceParams implements Parcelable {
    /** Creator for Paracelable (via reflection). */
    @NonNull
    public static final Parcelable.Creator<WebSourceParams> CREATOR =
            new Parcelable.Creator<WebSourceParams>() {
                @Override
                public WebSourceParams createFromParcel(Parcel in) {
                    return new WebSourceParams(in);
                }

                @Override
                public WebSourceParams[] newArray(int size) {
                    return new WebSourceParams[size];
                }
            };
    /**
     * URI that the Attribution Reporting API sends a request to in order to obtain source
     * registration parameters.
     */
    @NonNull private final Uri mRegistrationUri;
    /**
     * Used by the browser to indicate whether the debug key obtained from the registration URI is
     * allowed to be used
     */
    private final boolean mDebugKeyAllowed;

    private WebSourceParams(@NonNull Builder builder) {
        mRegistrationUri = builder.mRegistrationUri;
        mDebugKeyAllowed = builder.mDebugKeyAllowed;
    }

    /** Unpack a SourceRegistration from a Parcel. */
    private WebSourceParams(@NonNull Parcel in) {
        mRegistrationUri = Uri.CREATOR.createFromParcel(in);
        mDebugKeyAllowed = in.readBoolean();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WebSourceParams)) return false;
        WebSourceParams that = (WebSourceParams) o;
        return mDebugKeyAllowed == that.mDebugKeyAllowed
                && Objects.equals(mRegistrationUri, that.mRegistrationUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mRegistrationUri, mDebugKeyAllowed);
    }

    /** Getter for registration Uri. */
    @NonNull
    public Uri getRegistrationUri() {
        return mRegistrationUri;
    }

    /**
     * Getter for debug allowed/disallowed flag. Its value as {@code true} means to allow parsing
     * debug keys from registration responses and their addition in the generated reports.
     */
    public boolean isDebugKeyAllowed() {
        return mDebugKeyAllowed;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        Objects.requireNonNull(out);
        mRegistrationUri.writeToParcel(out, flags);
        out.writeBoolean(mDebugKeyAllowed);
    }

    /** A builder for {@link WebSourceParams}. */
    public static final class Builder {
        /**
         * URI that the Attribution Reporting API sends a request to in order to obtain source
         * registration parameters.
         */
        @NonNull private final Uri mRegistrationUri;
        /**
         * Used by the browser to indicate whether the debug key obtained from the registration URI
         * is allowed to be used
         */
        private boolean mDebugKeyAllowed;

        /**
         * Builder constructor for {@link WebSourceParams}. {@code mIsDebugKeyAllowed} is assigned
         * false by default.
         *
         * @param registrationUri URI that the Attribution Reporting API sends a request to in order
         *     to obtain source registration parameters.
         */
        public Builder(@NonNull Uri registrationUri) {
            Objects.requireNonNull(registrationUri);
            mRegistrationUri = registrationUri;
            mDebugKeyAllowed = false;
        }

        /**
         * Setter for debug allow/disallow flag. Setting it to true will allow parsing debug keys
         * from registration responses and their addition in the generated reports.
         *
         * @param debugKeyAllowed used by the browser to indicate whether the debug key obtained
         *     from the registration URI is allowed to be used
         * @return builder
         */
        @NonNull
        public Builder setDebugKeyAllowed(boolean debugKeyAllowed) {
            this.mDebugKeyAllowed = debugKeyAllowed;
            return this;
        }

        /**
         * Built immutable {@link WebSourceParams}.
         *
         * @return immutable {@link WebSourceParams}
         */
        @NonNull
        public WebSourceParams build() {
            return new WebSourceParams(this);
        }
    }
}
