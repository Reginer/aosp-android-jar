/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.net;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Metadata sent by captive portals, see https://www.ietf.org/id/draft-ietf-capport-api-03.txt.
 * @hide
 */
@SystemApi
public final class CaptivePortalData implements Parcelable {
    private final long mRefreshTimeMillis;
    @Nullable
    private final Uri mUserPortalUrl;
    @Nullable
    private final Uri mVenueInfoUrl;
    private final boolean mIsSessionExtendable;
    private final long mByteLimit;
    private final long mExpiryTimeMillis;
    private final boolean mCaptive;
    private final String mVenueFriendlyName;
    private final int mVenueInfoUrlSource;
    private final int mUserPortalUrlSource;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"CAPTIVE_PORTAL_DATA_SOURCE_"}, value = {
            CAPTIVE_PORTAL_DATA_SOURCE_OTHER,
            CAPTIVE_PORTAL_DATA_SOURCE_PASSPOINT})
    public @interface CaptivePortalDataSource {}

    /**
     * Source of information: Other (default)
     */
    public static final int CAPTIVE_PORTAL_DATA_SOURCE_OTHER = 0;

    /**
     * Source of information: Wi-Fi Passpoint
     */
    public static final int CAPTIVE_PORTAL_DATA_SOURCE_PASSPOINT = 1;

    private CaptivePortalData(long refreshTimeMillis, Uri userPortalUrl, Uri venueInfoUrl,
            boolean isSessionExtendable, long byteLimit, long expiryTimeMillis, boolean captive,
            CharSequence venueFriendlyName, int venueInfoUrlSource, int userPortalUrlSource) {
        mRefreshTimeMillis = refreshTimeMillis;
        mUserPortalUrl = userPortalUrl;
        mVenueInfoUrl = venueInfoUrl;
        mIsSessionExtendable = isSessionExtendable;
        mByteLimit = byteLimit;
        mExpiryTimeMillis = expiryTimeMillis;
        mCaptive = captive;
        mVenueFriendlyName = venueFriendlyName == null ? null : venueFriendlyName.toString();
        mVenueInfoUrlSource = venueInfoUrlSource;
        mUserPortalUrlSource = userPortalUrlSource;
    }

    private CaptivePortalData(Parcel p) {
        this(p.readLong(), p.readParcelable(null), p.readParcelable(null), p.readBoolean(),
                p.readLong(), p.readLong(), p.readBoolean(), p.readString(), p.readInt(),
                p.readInt());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(mRefreshTimeMillis);
        dest.writeParcelable(mUserPortalUrl, 0);
        dest.writeParcelable(mVenueInfoUrl, 0);
        dest.writeBoolean(mIsSessionExtendable);
        dest.writeLong(mByteLimit);
        dest.writeLong(mExpiryTimeMillis);
        dest.writeBoolean(mCaptive);
        dest.writeString(mVenueFriendlyName);
        dest.writeInt(mVenueInfoUrlSource);
        dest.writeInt(mUserPortalUrlSource);
    }

    /**
     * A builder to create new {@link CaptivePortalData}.
     */
    public static class Builder {
        private long mRefreshTime;
        private Uri mUserPortalUrl;
        private Uri mVenueInfoUrl;
        private boolean mIsSessionExtendable;
        private long mBytesRemaining = -1;
        private long mExpiryTime = -1;
        private boolean mCaptive;
        private CharSequence mVenueFriendlyName;
        private @CaptivePortalDataSource int mVenueInfoUrlSource = CAPTIVE_PORTAL_DATA_SOURCE_OTHER;
        private @CaptivePortalDataSource int mUserPortalUrlSource =
                CAPTIVE_PORTAL_DATA_SOURCE_OTHER;

        /**
         * Create an empty builder.
         */
        public Builder() {}

        /**
         * Create a builder copying all data from existing {@link CaptivePortalData}.
         */
        public Builder(@Nullable CaptivePortalData data) {
            if (data == null) return;
            setRefreshTime(data.mRefreshTimeMillis)
                    .setUserPortalUrl(data.mUserPortalUrl, data.mUserPortalUrlSource)
                    .setVenueInfoUrl(data.mVenueInfoUrl, data.mVenueInfoUrlSource)
                    .setSessionExtendable(data.mIsSessionExtendable)
                    .setBytesRemaining(data.mByteLimit)
                    .setExpiryTime(data.mExpiryTimeMillis)
                    .setCaptive(data.mCaptive)
                    .setVenueFriendlyName(data.mVenueFriendlyName);
        }

        /**
         * Set the time at which data was last refreshed, as per {@link System#currentTimeMillis()}.
         */
        @NonNull
        public Builder setRefreshTime(long refreshTime) {
            mRefreshTime = refreshTime;
            return this;
        }

        /**
         * Set the URL to be used for users to login to the portal, if captive.
         */
        @NonNull
        public Builder setUserPortalUrl(@Nullable Uri userPortalUrl) {
            return setUserPortalUrl(userPortalUrl, CAPTIVE_PORTAL_DATA_SOURCE_OTHER);
        }

        /**
         * Set the URL to be used for users to login to the portal, if captive, and the source of
         * the data, see {@link CaptivePortalDataSource}
         */
        @NonNull
        public Builder setUserPortalUrl(@Nullable Uri userPortalUrl,
                @CaptivePortalDataSource int source) {
            mUserPortalUrl = userPortalUrl;
            mUserPortalUrlSource = source;
            return this;
        }

        /**
         * Set the URL that can be used by users to view information about the network venue.
         */
        @NonNull
        public Builder setVenueInfoUrl(@Nullable Uri venueInfoUrl) {
            return setVenueInfoUrl(venueInfoUrl, CAPTIVE_PORTAL_DATA_SOURCE_OTHER);
        }

        /**
         * Set the URL that can be used by users to view information about the network venue, and
         * the source of the data, see {@link CaptivePortalDataSource}
         */
        @NonNull
        public Builder setVenueInfoUrl(@Nullable Uri venueInfoUrl,
                @CaptivePortalDataSource int source) {
            mVenueInfoUrl = venueInfoUrl;
            mVenueInfoUrlSource = source;
            return this;
        }

        /**
         * Set whether the portal supports extending a user session on the portal URL page.
         */
        @NonNull
        public Builder setSessionExtendable(boolean sessionExtendable) {
            mIsSessionExtendable = sessionExtendable;
            return this;
        }

        /**
         * Set the number of bytes remaining on the network before the portal closes.
         */
        @NonNull
        public Builder setBytesRemaining(long bytesRemaining) {
            mBytesRemaining = bytesRemaining;
            return this;
        }

        /**
         * Set the time at the session will expire, as per {@link System#currentTimeMillis()}.
         */
        @NonNull
        public Builder setExpiryTime(long expiryTime) {
            mExpiryTime = expiryTime;
            return this;
        }

        /**
         * Set whether the network is captive (portal closed).
         */
        @NonNull
        public Builder setCaptive(boolean captive) {
            mCaptive = captive;
            return this;
        }

        /**
         * Set the venue friendly name.
         */
        @NonNull
        public Builder setVenueFriendlyName(@Nullable CharSequence venueFriendlyName) {
            mVenueFriendlyName = venueFriendlyName;
            return this;
        }

        /**
         * Create a new {@link CaptivePortalData}.
         */
        @NonNull
        public CaptivePortalData build() {
            return new CaptivePortalData(mRefreshTime, mUserPortalUrl, mVenueInfoUrl,
                    mIsSessionExtendable, mBytesRemaining, mExpiryTime, mCaptive,
                    mVenueFriendlyName, mVenueInfoUrlSource,
                    mUserPortalUrlSource);
        }
    }

    /**
     * Get the time at which data was last refreshed, as per {@link System#currentTimeMillis()}.
     */
    public long getRefreshTimeMillis() {
        return mRefreshTimeMillis;
    }

    /**
     * Get the URL to be used for users to login to the portal, or extend their session if
     * {@link #isSessionExtendable()} is true.
     */
    @Nullable
    public Uri getUserPortalUrl() {
        return mUserPortalUrl;
    }

    /**
     * Get the URL that can be used by users to view information about the network venue.
     */
    @Nullable
    public Uri getVenueInfoUrl() {
        return mVenueInfoUrl;
    }

    /**
     * Indicates whether the user portal URL can be used to extend sessions, when the user is logged
     * in and the session has a time or byte limit.
     */
    public boolean isSessionExtendable() {
        return mIsSessionExtendable;
    }

    /**
     * Get the remaining bytes on the captive portal session, at the time {@link CaptivePortalData}
     * was refreshed. This may be different from the limit currently enforced by the portal.
     * @return The byte limit, or -1 if not set.
     */
    public long getByteLimit() {
        return mByteLimit;
    }

    /**
     * Get the time at the session will expire, as per {@link System#currentTimeMillis()}.
     * @return The expiry time, or -1 if unset.
     */
    public long getExpiryTimeMillis() {
        return mExpiryTimeMillis;
    }

    /**
     * Get whether the network is captive (portal closed).
     */
    public boolean isCaptive() {
        return mCaptive;
    }

    /**
     * Get the information source of the Venue URL
     * @return The source that the Venue URL was obtained from
     */
    public @CaptivePortalDataSource int getVenueInfoUrlSource() {
        return mVenueInfoUrlSource;
    }

    /**
     * Get the information source of the user portal URL
     * @return The source that the user portal URL was obtained from
     */
    public @CaptivePortalDataSource int getUserPortalUrlSource() {
        return mUserPortalUrlSource;
    }

    /**
     * Get the venue friendly name
     */
    @Nullable
    public CharSequence getVenueFriendlyName() {
        return mVenueFriendlyName;
    }

    @NonNull
    public static final Creator<CaptivePortalData> CREATOR = new Creator<CaptivePortalData>() {
        @Override
        public CaptivePortalData createFromParcel(Parcel source) {
            return new CaptivePortalData(source);
        }

        @Override
        public CaptivePortalData[] newArray(int size) {
            return new CaptivePortalData[size];
        }
    };

    @Override
    public int hashCode() {
        return Objects.hash(mRefreshTimeMillis, mUserPortalUrl, mVenueInfoUrl,
                mIsSessionExtendable, mByteLimit, mExpiryTimeMillis, mCaptive, mVenueFriendlyName,
                mVenueInfoUrlSource, mUserPortalUrlSource);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof CaptivePortalData)) return false;
        final CaptivePortalData other = (CaptivePortalData) obj;
        return mRefreshTimeMillis == other.mRefreshTimeMillis
                && Objects.equals(mUserPortalUrl, other.mUserPortalUrl)
                && Objects.equals(mVenueInfoUrl, other.mVenueInfoUrl)
                && mIsSessionExtendable == other.mIsSessionExtendable
                && mByteLimit == other.mByteLimit
                && mExpiryTimeMillis == other.mExpiryTimeMillis
                && mCaptive == other.mCaptive
                && Objects.equals(mVenueFriendlyName, other.mVenueFriendlyName)
                && mVenueInfoUrlSource == other.mVenueInfoUrlSource
                && mUserPortalUrlSource == other.mUserPortalUrlSource;
    }

    @Override
    public String toString() {
        return "CaptivePortalData {"
                + "refreshTime: " + mRefreshTimeMillis
                + ", userPortalUrl: " + mUserPortalUrl
                + ", venueInfoUrl: " + mVenueInfoUrl
                + ", isSessionExtendable: " + mIsSessionExtendable
                + ", byteLimit: " + mByteLimit
                + ", expiryTime: " + mExpiryTimeMillis
                + ", captive: " + mCaptive
                + ", venueFriendlyName: " + mVenueFriendlyName
                + ", venueInfoUrlSource: " + mVenueInfoUrlSource
                + ", userPortalUrlSource: " + mUserPortalUrlSource
                + "}";
    }
}
