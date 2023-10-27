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
import android.annotation.Nullable;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.InputEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Class to hold input to measurement source registration calls from web context. */
public final class WebSourceRegistrationRequest implements Parcelable {
    private static final String ANDROID_APP_SCHEME = "android-app";
    private static final int WEB_SOURCE_PARAMS_MAX_COUNT = 20;

    /** Creator for Paracelable (via reflection). */
    @NonNull
    public static final Parcelable.Creator<WebSourceRegistrationRequest> CREATOR =
            new Parcelable.Creator<WebSourceRegistrationRequest>() {
                @Override
                public WebSourceRegistrationRequest createFromParcel(Parcel in) {
                    return new WebSourceRegistrationRequest(in);
                }

                @Override
                public WebSourceRegistrationRequest[] newArray(int size) {
                    return new WebSourceRegistrationRequest[size];
                }
            };
    /** Registration info to fetch sources. */
    @NonNull private final List<WebSourceParams> mWebSourceParams;

    /** Top level origin of publisher. */
    @NonNull private final Uri mTopOriginUri;

    /**
     * User Interaction {@link InputEvent} used by the AttributionReporting API to distinguish
     * clicks from views.
     */
    @Nullable private final InputEvent mInputEvent;

    /**
     * App destination of the source. It is the android app {@link Uri} where corresponding
     * conversion is expected. At least one of app destination or web destination is required.
     */
    @Nullable private final Uri mAppDestination;

    /**
     * Web destination of the source. It is the website {@link Uri} where corresponding conversion
     * is expected. At least one of app destination or web destination is required.
     */
    @Nullable private final Uri mWebDestination;

    /** Verified destination by the caller. This is where the user actually landed. */
    @Nullable private final Uri mVerifiedDestination;

    private WebSourceRegistrationRequest(@NonNull Builder builder) {
        mWebSourceParams = builder.mWebSourceParams;
        mInputEvent = builder.mInputEvent;
        mTopOriginUri = builder.mTopOriginUri;
        mAppDestination = builder.mAppDestination;
        mWebDestination = builder.mWebDestination;
        mVerifiedDestination = builder.mVerifiedDestination;
    }

    private WebSourceRegistrationRequest(@NonNull Parcel in) {
        Objects.requireNonNull(in);
        ArrayList<WebSourceParams> sourceRegistrations = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            in.readList(sourceRegistrations, WebSourceParams.class.getClassLoader());
        } else {
            in.readList(
                    sourceRegistrations,
                    WebSourceParams.class.getClassLoader(),
                    WebSourceParams.class);
        }
        mWebSourceParams = sourceRegistrations;
        mTopOriginUri = Uri.CREATOR.createFromParcel(in);
        if (in.readBoolean()) {
            mInputEvent = InputEvent.CREATOR.createFromParcel(in);
        } else {
            mInputEvent = null;
        }
        if (in.readBoolean()) {
            mAppDestination = Uri.CREATOR.createFromParcel(in);
        } else {
            mAppDestination = null;
        }
        if (in.readBoolean()) {
            mWebDestination = Uri.CREATOR.createFromParcel(in);
        } else {
            mWebDestination = null;
        }
        if (in.readBoolean()) {
            mVerifiedDestination = Uri.CREATOR.createFromParcel(in);
        } else {
            mVerifiedDestination = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WebSourceRegistrationRequest)) return false;
        WebSourceRegistrationRequest that = (WebSourceRegistrationRequest) o;
        return Objects.equals(mWebSourceParams, that.mWebSourceParams)
                && Objects.equals(mTopOriginUri, that.mTopOriginUri)
                && Objects.equals(mInputEvent, that.mInputEvent)
                && Objects.equals(mAppDestination, that.mAppDestination)
                && Objects.equals(mWebDestination, that.mWebDestination)
                && Objects.equals(mVerifiedDestination, that.mVerifiedDestination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mWebSourceParams,
                mTopOriginUri,
                mInputEvent,
                mAppDestination,
                mWebDestination,
                mVerifiedDestination);
    }

    /** Getter for source params. */
    @NonNull
    public List<WebSourceParams> getSourceParams() {
        return mWebSourceParams;
    }

    /** Getter for top origin Uri. */
    @NonNull
    public Uri getTopOriginUri() {
        return mTopOriginUri;
    }

    /** Getter for input event. */
    @Nullable
    public InputEvent getInputEvent() {
        return mInputEvent;
    }

    /**
     * Getter for the app destination. It is the android app {@link Uri} where corresponding
     * conversion is expected. At least one of app destination or web destination is required.
     */
    @Nullable
    public Uri getAppDestination() {
        return mAppDestination;
    }

    /**
     * Getter for web destination. It is the website {@link Uri} where corresponding conversion is
     * expected. At least one of app destination or web destination is required.
     */
    @Nullable
    public Uri getWebDestination() {
        return mWebDestination;
    }

    /** Getter for verified destination. */
    @Nullable
    public Uri getVerifiedDestination() {
        return mVerifiedDestination;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        Objects.requireNonNull(out);
        out.writeList(mWebSourceParams);
        mTopOriginUri.writeToParcel(out, flags);

        if (mInputEvent != null) {
            out.writeBoolean(true);
            mInputEvent.writeToParcel(out, flags);
        } else {
            out.writeBoolean(false);
        }
        if (mAppDestination != null) {
            out.writeBoolean(true);
            mAppDestination.writeToParcel(out, flags);
        } else {
            out.writeBoolean(false);
        }
        if (mWebDestination != null) {
            out.writeBoolean(true);
            mWebDestination.writeToParcel(out, flags);
        } else {
            out.writeBoolean(false);
        }
        if (mVerifiedDestination != null) {
            out.writeBoolean(true);
            mVerifiedDestination.writeToParcel(out, flags);
        } else {
            out.writeBoolean(false);
        }
    }

    /** Builder for {@link WebSourceRegistrationRequest}. */
    public static final class Builder {
        /** Registration info to fetch sources. */
        @NonNull private final List<WebSourceParams> mWebSourceParams;
        /** Top origin {@link Uri} of publisher. */
        @NonNull private final Uri mTopOriginUri;
        /**
         * User Interaction InputEvent used by the AttributionReporting API to distinguish clicks
         * from views.
         */
        @Nullable private InputEvent mInputEvent;
        /**
         * App destination of the source. It is the android app {@link Uri} where corresponding
         * conversion is expected.
         */
        @Nullable private Uri mAppDestination;
        /**
         * Web destination of the source. It is the website {@link Uri} where corresponding
         * conversion is expected.
         */
        @Nullable private Uri mWebDestination;
        /**
         * Verified destination by the caller. If available, sources should be checked against it.
         */
        @Nullable private Uri mVerifiedDestination;

        /**
         * Builder constructor for {@link WebSourceRegistrationRequest}.
         *
         * @param webSourceParams source parameters containing source registration parameters, the
         *     list should not be empty
         * @param topOriginUri source publisher {@link Uri}
         */
        public Builder(@NonNull List<WebSourceParams> webSourceParams, @NonNull Uri topOriginUri) {
            Objects.requireNonNull(webSourceParams);
            Objects.requireNonNull(topOriginUri);
            if (webSourceParams.isEmpty() || webSourceParams.size() > WEB_SOURCE_PARAMS_MAX_COUNT) {
                throw new IllegalArgumentException(
                        "web source params size is not within bounds, size: "
                                + webSourceParams.size());
            }
            mWebSourceParams = webSourceParams;
            mTopOriginUri = topOriginUri;
        }

        /**
         * Setter for input event.
         *
         * @param inputEvent User Interaction InputEvent used by the AttributionReporting API to
         *     distinguish clicks from views.
         * @return builder
         */
        @NonNull
        public Builder setInputEvent(@Nullable InputEvent inputEvent) {
            mInputEvent = inputEvent;
            return this;
        }

        /**
         * Setter for app destination. It is the android app {@link Uri} where corresponding
         * conversion is expected. At least one of app destination or web destination is required.
         *
         * @param appDestination app destination {@link Uri}
         * @return builder
         */
        @NonNull
        public Builder setAppDestination(@Nullable Uri appDestination) {
            if (appDestination != null) {
                String scheme = appDestination.getScheme();
                Uri destination;
                if (scheme == null) {
                    destination = Uri.parse(ANDROID_APP_SCHEME + "://" + appDestination);
                } else if (!scheme.equals(ANDROID_APP_SCHEME)) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "appDestination scheme must be %s " + "or null. Received: %s",
                                    ANDROID_APP_SCHEME, scheme));
                } else {
                    destination = appDestination;
                }
                mAppDestination = destination;
            }
            return this;
        }

        /**
         * Setter for web destination. It is the website {@link Uri} where corresponding conversion
         * is expected. At least one of app destination or web destination is required.
         *
         * @param webDestination web destination {@link Uri}
         * @return builder
         */
        @NonNull
        public Builder setWebDestination(@Nullable Uri webDestination) {
            if (webDestination != null) {
                validateScheme("Web destination", webDestination);
                mWebDestination = webDestination;
            }
            return this;
        }

        /**
         * Setter for verified destination.
         *
         * @param verifiedDestination verified destination
         * @return builder
         */
        @NonNull
        public Builder setVerifiedDestination(@Nullable Uri verifiedDestination) {
            mVerifiedDestination = verifiedDestination;
            return this;
        }

        /** Pre-validates parameters and builds {@link WebSourceRegistrationRequest}. */
        @NonNull
        public WebSourceRegistrationRequest build() {
            return new WebSourceRegistrationRequest(this);
        }
    }

    private static void validateScheme(String name, Uri uri) throws IllegalArgumentException {
        if (uri.getScheme() == null) {
            throw new IllegalArgumentException(name + " must have a scheme.");
        }
    }
}
