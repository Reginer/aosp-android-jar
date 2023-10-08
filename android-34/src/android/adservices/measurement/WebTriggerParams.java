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

/** Class holding trigger registration parameters. */
public final class WebTriggerParams implements Parcelable {
    /** Creator for Paracelable (via reflection). */
    @NonNull
    public static final Creator<WebTriggerParams> CREATOR =
            new Creator<WebTriggerParams>() {
                @Override
                public WebTriggerParams createFromParcel(Parcel in) {
                    return new WebTriggerParams(in);
                }

                @Override
                public WebTriggerParams[] newArray(int size) {
                    return new WebTriggerParams[size];
                }
            };
    /**
     * URI that the Attribution Reporting API sends a request to in order to obtain trigger
     * registration parameters.
     */
    @NonNull private final Uri mRegistrationUri;
    /**
     * Used by the browser to indicate whether the debug key obtained from the registration URI is
     * allowed to be used.
     */
    private final boolean mDebugKeyAllowed;

    private WebTriggerParams(@NonNull Builder builder) {
        mRegistrationUri = builder.mRegistrationUri;
        mDebugKeyAllowed = builder.mDebugKeyAllowed;
    }

    /** Unpack a TriggerRegistration from a Parcel. */
    private WebTriggerParams(@NonNull Parcel in) {
        mRegistrationUri = Uri.CREATOR.createFromParcel(in);
        mDebugKeyAllowed = in.readBoolean();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WebTriggerParams)) return false;
        WebTriggerParams that = (WebTriggerParams) o;
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

    /** A builder for {@link WebTriggerParams}. */
    public static final class Builder {
        /**
         * URI that the Attribution Reporting API sends a request to in order to obtain trigger
         * registration parameters.
         */
        @NonNull private final Uri mRegistrationUri;
        /**
         * Used by the browser to indicate whether the debug key obtained from the registration URI
         * is allowed to be used.
         */
        private boolean mDebugKeyAllowed;

        /**
         * Builder constructor for {@link WebTriggerParams}. {@code mIsDebugKeyAllowed} is assigned
         * false by default.
         *
         * @param registrationUri URI that the Attribution Reporting API sends a request to in order
         *     to obtain trigger registration parameters
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
            mDebugKeyAllowed = debugKeyAllowed;
            return this;
        }

        /**
         * Builds immutable {@link WebTriggerParams}.
         *
         * @return immutable {@link WebTriggerParams}
         */
        @NonNull
        public WebTriggerParams build() {
            return new WebTriggerParams(this);
        }
    }
}
