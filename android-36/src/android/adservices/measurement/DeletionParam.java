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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class to hold deletion related request. This is an internal class for communication between the
 * {@link MeasurementManager} and {@link IMeasurementService} impl.
 *
 * @hide
 */
public final class DeletionParam implements Parcelable {
    private final List<Uri> mOriginUris;
    private final List<Uri> mDomainUris;
    private final Instant mStart;
    private final Instant mEnd;
    private final String mAppPackageName;
    private final String mSdkPackageName;
    @DeletionRequest.DeletionMode private final int mDeletionMode;
    @DeletionRequest.MatchBehavior private final int mMatchBehavior;

    private DeletionParam(@NonNull Builder builder) {
        mOriginUris = builder.mOriginUris;
        mDomainUris = builder.mDomainUris;
        mDeletionMode = builder.mDeletionMode;
        mMatchBehavior = builder.mMatchBehavior;
        mStart = builder.mStart;
        mEnd = builder.mEnd;
        mAppPackageName = builder.mAppPackageName;
        mSdkPackageName = builder.mSdkPackageName;
    }

    /** Unpack an DeletionRequest from a Parcel. */
    private DeletionParam(Parcel in) {
        mAppPackageName = in.readString();
        mSdkPackageName = in.readString();

        mDomainUris = new ArrayList<>();
        in.readTypedList(mDomainUris, Uri.CREATOR);

        mOriginUris = new ArrayList<>();
        in.readTypedList(mOriginUris, Uri.CREATOR);

        boolean hasStart = in.readBoolean();
        if (hasStart) {
            mStart = Instant.parse(in.readString());
        } else {
            mStart = null;
        }

        boolean hasEnd = in.readBoolean();
        if (hasEnd) {
            mEnd = Instant.parse(in.readString());
        } else {
            mEnd = null;
        }

        mDeletionMode = in.readInt();
        mMatchBehavior = in.readInt();
    }

    /** Creator for Parcelable (via reflection). */
    @NonNull
    public static final Parcelable.Creator<DeletionParam> CREATOR =
            new Parcelable.Creator<DeletionParam>() {
                @Override
                public DeletionParam createFromParcel(Parcel in) {
                    return new DeletionParam(in);
                }

                @Override
                public DeletionParam[] newArray(int size) {
                    return new DeletionParam[size];
                }
            };

    /** For Parcelable, no special marshalled objects. */
    public int describeContents() {
        return 0;
    }

    /** For Parcelable, write out to a Parcel in particular order. */
    public void writeToParcel(@NonNull Parcel out, int flags) {
        Objects.requireNonNull(out);
        out.writeString(mAppPackageName);
        out.writeString(mSdkPackageName);

        out.writeTypedList(mDomainUris);

        out.writeTypedList(mOriginUris);

        if (mStart != null) {
            out.writeBoolean(true);
            out.writeString(mStart.toString());
        } else {
            out.writeBoolean(false);
        }

        if (mEnd != null) {
            out.writeBoolean(true);
            out.writeString(mEnd.toString());
        } else {
            out.writeBoolean(false);
        }

        out.writeInt(mDeletionMode);

        out.writeInt(mMatchBehavior);
    }

    /**
     * Publisher/Advertiser Origins for which data should be deleted. These will be matched as-is.
     */
    @NonNull
    public List<Uri> getOriginUris() {
        return mOriginUris;
    }

    /**
     * Publisher/Advertiser domains for which data should be deleted. These will be pattern matched
     * with regex SCHEME://(.*\.|)SITE .
     */
    @NonNull
    public List<Uri> getDomainUris() {
        return mDomainUris;
    }

    /** Deletion mode for matched records. */
    @DeletionRequest.DeletionMode
    public int getDeletionMode() {
        return mDeletionMode;
    }

    /** Match behavior for provided origins/domains. */
    @DeletionRequest.MatchBehavior
    public int getMatchBehavior() {
        return mMatchBehavior;
    }

    /**
     * Instant in time the deletion starts, or {@link java.time.Instant#MIN} if starting at the
     * oldest possible time.
     */
    @NonNull
    public Instant getStart() {
        return mStart;
    }

    /**
     * Instant in time the deletion ends, or {@link java.time.Instant#MAX} if ending at the most
     * recent time.
     */
    @NonNull
    public Instant getEnd() {
        return mEnd;
    }

    /** Package name of the app used for the deletion. */
    @NonNull
    public String getAppPackageName() {
        return mAppPackageName;
    }

    /** Package name of the sdk used for the deletion. */
    @NonNull
    public String getSdkPackageName() {
        return mSdkPackageName;
    }

    /** A builder for {@link DeletionParam}. */
    public static final class Builder {
        private final List<Uri> mOriginUris;
        private final List<Uri> mDomainUris;
        private final Instant mStart;
        private final Instant mEnd;
        private final String mAppPackageName;
        private final String mSdkPackageName;
        @DeletionRequest.DeletionMode private int mDeletionMode;
        @DeletionRequest.MatchBehavior private int mMatchBehavior;

        /**
         * Builder constructor for {@link DeletionParam}.
         *
         * @param originUris see {@link DeletionParam#getOriginUris()}
         * @param domainUris see {@link DeletionParam#getDomainUris()}
         * @param start see {@link DeletionParam#getStart()}
         * @param end see {@link DeletionParam#getEnd()}
         * @param appPackageName see {@link DeletionParam#getAppPackageName()}
         * @param sdkPackageName see {@link DeletionParam#getSdkPackageName()}
         */
        public Builder(
                @NonNull List<Uri> originUris,
                @NonNull List<Uri> domainUris,
                @NonNull Instant start,
                @NonNull Instant end,
                @NonNull String appPackageName,
                @NonNull String sdkPackageName) {
            Objects.requireNonNull(originUris);
            Objects.requireNonNull(domainUris);
            Objects.requireNonNull(start);
            Objects.requireNonNull(end);
            Objects.requireNonNull(appPackageName);
            Objects.requireNonNull(sdkPackageName);

            mOriginUris = originUris;
            mDomainUris = domainUris;
            mStart = start;
            mEnd = end;
            mAppPackageName = appPackageName;
            mSdkPackageName = sdkPackageName;
        }

        /** See {@link DeletionParam#getDeletionMode()}. */
        @NonNull
        public Builder setDeletionMode(@DeletionRequest.DeletionMode int deletionMode) {
            mDeletionMode = deletionMode;
            return this;
        }

        /** See {@link DeletionParam#getDeletionMode()}. */
        @NonNull
        public Builder setMatchBehavior(@DeletionRequest.MatchBehavior int matchBehavior) {
            mMatchBehavior = matchBehavior;
            return this;
        }

        /** Build the DeletionRequest. */
        @NonNull
        public DeletionParam build() {
            return new DeletionParam(this);
        }
    }
}
