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

package android.health.connect.exportimport;

import static android.health.connect.Constants.DEFAULT_INT;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Settings for configuring the scheduled export service.
 *
 * @hide
 */
public final class ScheduledExportSettings implements Parcelable {
    @NonNull
    public static final Creator<ScheduledExportSettings> CREATOR =
            new Creator<>() {
                @Override
                public ScheduledExportSettings createFromParcel(Parcel in) {
                    return new ScheduledExportSettings(in);
                }

                @Override
                public ScheduledExportSettings[] newArray(int size) {
                    return new ScheduledExportSettings[size];
                }
            };

    @Nullable private final byte[] mSecretKey;
    @Nullable private final byte[] mSalt;
    @Nullable private final Uri mUri;
    private final int mPeriodInDays;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduledExportSettings that)) return false;
        return mPeriodInDays == that.mPeriodInDays
                && Arrays.equals(mSecretKey, that.mSecretKey)
                && Arrays.equals(mSalt, that.mSalt)
                && Objects.equals(mUri, that.mUri);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mUri, mPeriodInDays);
        result = 31 * result + Arrays.hashCode(mSecretKey);
        result = 31 * result + Arrays.hashCode(mSalt);
        return result;
    }

    /**
     * Returns a {@link ScheduledExportSettings} to update the secret key and salt used for
     * encrypting the exported data.
     */
    public static ScheduledExportSettings withSecretKey(
            @NonNull byte[] secretKey, @NonNull byte[] salt) {
        Objects.requireNonNull(secretKey);
        Objects.requireNonNull(salt);

        return new ScheduledExportSettings(secretKey, salt, null, DEFAULT_INT);
    }

    /**
     * Returns a {@link ScheduledExportSettings} to update the URI to write to when exporting data.
     */
    public static ScheduledExportSettings withUri(@NonNull Uri uri) {
        Objects.requireNonNull(uri);

        return new ScheduledExportSettings(null, null, uri, DEFAULT_INT);
    }

    /**
     * Returns a {@link ScheduledExportSettings} to update the period in days between scheduled
     * exports.
     */
    public static ScheduledExportSettings withPeriodInDays(
            @IntRange(from = 0, to = 30) int periodInDays) {
        if (periodInDays < 0 || periodInDays > 30) {
            throw new IllegalArgumentException("periodInDays should be between 0 and 30");
        }

        return new ScheduledExportSettings(null, null, null, periodInDays);
    }

    private ScheduledExportSettings(@NonNull Parcel in) {
        boolean hasSecretKey = in.readBoolean();
        if (hasSecretKey) {
            int length = in.readInt();
            mSecretKey = new byte[length];
            in.readByteArray(mSecretKey);
        } else {
            mSecretKey = null;
        }

        boolean hasSalt = in.readBoolean();
        if (hasSalt) {
            int length = in.readInt();
            mSalt = new byte[length];
            in.readByteArray(mSalt);
        } else {
            mSalt = null;
        }

        boolean hasUri = in.readBoolean();
        mUri = hasUri ? Uri.parse(in.readString()) : null;

        mPeriodInDays = in.readInt();
    }

    private ScheduledExportSettings(
            @Nullable byte[] secretKey,
            @Nullable byte[] salt,
            @Nullable Uri uri,
            int periodInDays) {
        mSecretKey = secretKey;
        mSalt = salt;
        mUri = uri;
        mPeriodInDays = periodInDays;
    }

    /**
     * Returns the secret key to use for encrypting the exported data or null to keep the existing
     * secret key.
     */
    @Nullable
    public byte[] getSecretKey() {
        return mSecretKey;
    }

    /**
     * Returns the random salt used to generate the secret key or null to keep the existing salt.
     */
    @Nullable
    public byte[] getSalt() {
        return mSalt;
    }

    /** Returns the URI to write to when exporting data or null to keep the existing URI. */
    @Nullable
    public Uri getUri() {
        return mUri;
    }

    /**
     * Returns the period between scheduled exports in days or {@code DEFAULT_INT} to keep the
     * existing period.
     */
    public int getPeriodInDays() {
        return mPeriodInDays;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeBoolean(mSecretKey != null);
        if (mSecretKey != null) {
            dest.writeInt(mSecretKey.length);
            dest.writeByteArray(mSecretKey);
        }

        dest.writeBoolean(mSalt != null);
        if (mSalt != null) {
            dest.writeInt(mSalt.length);
            dest.writeByteArray(mSalt);
        }

        dest.writeBoolean(mUri != null);
        if (mUri != null) {
            dest.writeString(mUri.toString());
        }

        dest.writeInt(mPeriodInDays);
    }
}
