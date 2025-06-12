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

package android.adservices.common;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * AdServicesStates exposed to system apps/services through the enableAdServices API. The bits
 * stored in this parcel can change frequently based on user interaction with the Ads settings page.
 *
 * @hide
 */
@SystemApi
public final class AdServicesStates implements Parcelable {

    @NonNull
    public static final Creator<AdServicesStates> CREATOR =
            new Parcelable.Creator<>() {
                @Override
                public AdServicesStates createFromParcel(Parcel in) {
                    return new AdServicesStates(in);
                }

                @Override
                public AdServicesStates[] newArray(int size) {
                    return new AdServicesStates[size];
                }
            };

    private final boolean mIsPrivacySandboxUiEnabled;
    private final boolean mIsPrivacySandboxUiRequest;
    private final boolean mIsU18Account;
    private final boolean mIsAdultAccount;
    private final boolean mIsAdIdEnabled;

    private AdServicesStates(
            boolean isPrivacySandboxUiEnabled,
            boolean isPrivacySandboxUiRequest,
            boolean isU18Account,
            boolean isAdultAccount,
            boolean isAdIdEnabled) {
        mIsPrivacySandboxUiEnabled = isPrivacySandboxUiEnabled;
        mIsPrivacySandboxUiRequest = isPrivacySandboxUiRequest;
        mIsU18Account = isU18Account;
        mIsAdultAccount = isAdultAccount;
        mIsAdIdEnabled = isAdIdEnabled;
    }

    private AdServicesStates(@NonNull Parcel in) {
        mIsPrivacySandboxUiEnabled = in.readBoolean();
        mIsPrivacySandboxUiRequest = in.readBoolean();
        mIsU18Account = in.readBoolean();
        mIsAdultAccount = in.readBoolean();
        mIsAdIdEnabled = in.readBoolean();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeBoolean(mIsPrivacySandboxUiEnabled);
        out.writeBoolean(mIsPrivacySandboxUiRequest);
        out.writeBoolean(mIsU18Account);
        out.writeBoolean(mIsAdultAccount);
        out.writeBoolean(mIsAdIdEnabled);
    }

    /** Returns whether the privacy sandbox UI is visible from the settings app. */
    @NonNull
    public boolean isPrivacySandboxUiEnabled() {
        return mIsPrivacySandboxUiEnabled;
    }

    /**
     * Returns whether the API call was the byproduct of a privacy sandbox UI request from the
     * settings app.
     */
    @NonNull
    public boolean isPrivacySandboxUiRequest() {
        return mIsPrivacySandboxUiRequest;
    }

    /** Returns whether Advertising ID is enabled. */
    @NonNull
    public boolean isAdIdEnabled() {
        return mIsAdIdEnabled;
    }

    /**
     * Determines whether the user account is eligible for the U18 (under 18) privacy sandbox, in
     * which all ads relevance personalized Ads APIs are * permanently disabled and the ad
     * measurement API can be enabled/disabled by the user. An account is considered a U18 account
     * if privacy sandbox has received signals that the user is a minor.
     */
    @NonNull
    public boolean isU18Account() {
        return mIsU18Account;
    }

    /**
     * Determines whether the user account is eligible for the adult or full-fledged privacy
     * sandbox, in which all Ads APIs can be * enabled/disabled by the user. An account is
     * considered an adult account if privacy sandbox has received signals that the user is an
     * adult.
     */
    @NonNull
    public boolean isAdultAccount() {
        return mIsAdultAccount;
    }

    /** Builder for {@link AdServicesStates} objects. */
    public static final class Builder {
        private boolean mIsPrivacySandboxUiEnabled;
        private boolean mIsPrivacySandboxUiRequest;
        private boolean mIsU18Account;
        private boolean mIsAdultAccount;
        private boolean mIsAdIdEnabled;

        public Builder() {
        }

        /** Set if the privacy sandbox UX entry point is enabled. */
        @NonNull
        public Builder setPrivacySandboxUiEnabled(boolean isPrivacySandboxUiEnabled) {
            mIsPrivacySandboxUiEnabled = isPrivacySandboxUiEnabled;
            return this;
        }

        /** Set if the API call was the result of a privacy sandbox UX entry point request. */
        @NonNull
        public Builder setPrivacySandboxUiRequest(boolean isPrivacySandboxUiRequest) {
            mIsPrivacySandboxUiRequest = isPrivacySandboxUiRequest;
            return this;
        }

        /** Set if the device is currently running under a U18 account. */
        @NonNull
        public Builder setU18Account(boolean isU18Account) {
            mIsU18Account = isU18Account;
            return this;
        }

        /** Set if the device is currently running under an adult account. */
        @NonNull
        public Builder setAdultAccount(boolean isAdultAccount) {
            mIsAdultAccount = isAdultAccount;
            return this;
        }

        /** Set if user has opt-in/out of Advertising ID. */
        @NonNull
        public Builder setAdIdEnabled(boolean isAdIdEnabled) {
            mIsAdIdEnabled = isAdIdEnabled;
            return this;
        }

        /** Builds a {@link AdServicesStates} instance. */
        @NonNull
        public AdServicesStates build() {
            return new AdServicesStates(
                    mIsPrivacySandboxUiEnabled,
                    mIsPrivacySandboxUiRequest,
                    mIsU18Account,
                    mIsAdultAccount,
                    mIsAdIdEnabled);
        }
    }
}
