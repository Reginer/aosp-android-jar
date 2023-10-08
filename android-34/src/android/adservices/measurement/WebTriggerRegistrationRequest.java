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
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Class to hold input to measurement trigger registration calls from web context. */
public final class WebTriggerRegistrationRequest implements Parcelable {
    private static final int WEB_TRIGGER_PARAMS_MAX_COUNT = 20;

    /** Creator for Paracelable (via reflection). */
    @NonNull
    public static final Parcelable.Creator<WebTriggerRegistrationRequest> CREATOR =
            new Parcelable.Creator<WebTriggerRegistrationRequest>() {
                @Override
                public WebTriggerRegistrationRequest createFromParcel(Parcel in) {
                    return new WebTriggerRegistrationRequest(in);
                }

                @Override
                public WebTriggerRegistrationRequest[] newArray(int size) {
                    return new WebTriggerRegistrationRequest[size];
                }
            };
    /** Registration info to fetch sources. */
    @NonNull private final List<WebTriggerParams> mWebTriggerParams;

    /** Destination {@link Uri}. */
    @NonNull private final Uri mDestination;

    private WebTriggerRegistrationRequest(@NonNull Builder builder) {
        mWebTriggerParams = builder.mWebTriggerParams;
        mDestination = builder.mDestination;
    }

    private WebTriggerRegistrationRequest(Parcel in) {
        Objects.requireNonNull(in);
        ArrayList<WebTriggerParams> webTriggerParams = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            in.readList(webTriggerParams, WebTriggerParams.class.getClassLoader());
        } else {
            in.readList(
                    webTriggerParams,
                    WebTriggerParams.class.getClassLoader(),
                    WebTriggerParams.class);
        }
        mWebTriggerParams = webTriggerParams;
        mDestination = Uri.CREATOR.createFromParcel(in);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WebTriggerRegistrationRequest)) return false;
        WebTriggerRegistrationRequest that = (WebTriggerRegistrationRequest) o;
        return Objects.equals(mWebTriggerParams, that.mWebTriggerParams)
                && Objects.equals(mDestination, that.mDestination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mWebTriggerParams, mDestination);
    }

    /** Getter for trigger params. */
    @NonNull
    public List<WebTriggerParams> getTriggerParams() {
        return mWebTriggerParams;
    }

    /** Getter for destination. */
    @NonNull
    public Uri getDestination() {
        return mDestination;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        Objects.requireNonNull(out);
        out.writeList(mWebTriggerParams);
        mDestination.writeToParcel(out, flags);
    }

    /** Builder for {@link WebTriggerRegistrationRequest}. */
    public static final class Builder {
        /**
         * Registration info to fetch triggers. Maximum 20 registrations allowed at once, to be in
         * sync with Chrome platform.
         */
        @NonNull private List<WebTriggerParams> mWebTriggerParams;
        /** Top level origin of publisher app. */
        @NonNull private final Uri mDestination;

        /**
         * Builder constructor for {@link WebTriggerRegistrationRequest}.
         *
         * @param webTriggerParams contains trigger registration parameters, the list should not be
         *     empty
         * @param destination trigger destination {@link Uri}
         */
        public Builder(@NonNull List<WebTriggerParams> webTriggerParams, @NonNull Uri destination) {
            Objects.requireNonNull(webTriggerParams);
            if (webTriggerParams.isEmpty()
                    || webTriggerParams.size() > WEB_TRIGGER_PARAMS_MAX_COUNT) {
                throw new IllegalArgumentException(
                        "web trigger params size is not within bounds, size: "
                                + webTriggerParams.size());
            }

            Objects.requireNonNull(destination);
            if (destination.getScheme() == null) {
                throw new IllegalArgumentException("Destination origin must have a scheme.");
            }
            mWebTriggerParams = webTriggerParams;
            mDestination = destination;

        }

        /** Pre-validates parameters and builds {@link WebTriggerRegistrationRequest}. */
        @NonNull
        public WebTriggerRegistrationRequest build() {
            return new WebTriggerRegistrationRequest(this);
        }
    }
}
