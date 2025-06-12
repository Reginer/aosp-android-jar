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

    @Nullable private final Uri mUri;
    private final int mPeriodInDays;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduledExportSettings that)) return false;
        return mPeriodInDays == that.mPeriodInDays && Objects.equals(mUri, that.mUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mUri, mPeriodInDays);
    }

    private ScheduledExportSettings(@NonNull Parcel in) {
        boolean hasUri = in.readBoolean();
        mUri = hasUri ? Uri.parse(in.readString()) : null;

        mPeriodInDays = in.readInt();
    }

    private ScheduledExportSettings(@Nullable Uri uri, int periodInDays) {
        mUri = uri;
        mPeriodInDays = periodInDays;
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
        dest.writeBoolean(mUri != null);
        if (mUri != null) {
            dest.writeString(mUri.toString());
        }

        dest.writeInt(mPeriodInDays);
    }

    /** Builder for {@link ScheduledExportSettings}. */
    public static final class Builder {
        @Nullable private Uri mUri;
        private int mPeriodInDays = DEFAULT_INT;

        /**
         * Sets the URI to write to when exporting data.
         *
         * <p>If not set, the existing URI will be kept.
         */
        @NonNull
        public Builder setUri(@NonNull Uri uri) {
            Objects.requireNonNull(uri);

            mUri = uri;
            return this;
        }

        /**
         * Sets the period between scheduled exports in days.
         *
         * <p>If not set, the existing period will be kept.
         */
        @NonNull
        public Builder setPeriodInDays(@IntRange(from = 0, to = 30) int periodInDays) {
            if (periodInDays < 0 || periodInDays > 30) {
                throw new IllegalArgumentException("periodInDays should be between 0 and 30");
            }

            mPeriodInDays = periodInDays;
            return this;
        }

        /** Builds a {@link ScheduledExportSettings} object. */
        @NonNull
        public ScheduledExportSettings build() {
            return new ScheduledExportSettings(mUri, mPeriodInDays);
        }
    }
}
