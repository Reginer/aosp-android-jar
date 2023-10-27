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

package android.health.connect.migration;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holds application info migration data payload. Used to backfill application info for apps.
 *
 * @hide
 */
@SystemApi
public final class AppInfoMigrationPayload extends MigrationPayload implements Parcelable {

    @NonNull
    public static final Creator<AppInfoMigrationPayload> CREATOR =
            new Creator<>() {
                @Override
                public AppInfoMigrationPayload createFromParcel(Parcel in) {
                    in.readInt(); // Skip the type
                    return new AppInfoMigrationPayload(in);
                }

                @Override
                public AppInfoMigrationPayload[] newArray(int size) {
                    return new AppInfoMigrationPayload[size];
                }
            };

    private final String mPackageName;
    private final String mAppName;
    private final byte[] mAppIcon;

    private AppInfoMigrationPayload(
            @NonNull String packageName, @NonNull String appName, @Nullable byte[] appIcon) {
        mPackageName = packageName;
        mAppName = appName;
        mAppIcon = appIcon;
    }

    AppInfoMigrationPayload(@NonNull Parcel in) {
        mPackageName = in.readString();
        mAppName = in.readString();
        mAppIcon = in.createByteArray();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(TYPE_APP_INFO);

        dest.writeString(mPackageName);
        dest.writeString(mAppName);
        dest.writeByteArray(mAppIcon);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** Returns package name of this app info payload. */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /** Returns application name of this app info payload. */
    @NonNull
    public String getAppName() {
        return mAppName;
    }

    /**
     * Returns icon bitmap encoded as a byte array. The icon is decoded using {@link
     * android.graphics.BitmapFactory#decodeByteArray(byte[], int, int)}.
     */
    @Nullable
    public byte[] getAppIcon() {
        return mAppIcon;
    }

    /** Builder for {@link AppInfoMigrationPayload}. */
    public static final class Builder {
        private String mPackageName;
        private String mAppName;
        private byte[] mAppIcon;

        public Builder(@NonNull String packageName, @NonNull String appName) {
            requireNonNull(packageName);
            requireNonNull(appName);

            mPackageName = packageName;
            mAppName = appName;
        }

        /** Sets the value for {@link AppInfoMigrationPayload#getPackageName()}. */
        @NonNull
        public Builder setPackageName(@NonNull String packageName) {
            requireNonNull(packageName);
            mPackageName = packageName;
            return this;
        }

        /** Sets the value for {@link AppInfoMigrationPayload#getAppName()}. */
        @NonNull
        public Builder setAppName(@NonNull String appName) {
            requireNonNull(appName);
            mAppName = appName;
            return this;
        }

        /** Sets the value for {@link AppInfoMigrationPayload#getAppIcon()}. */
        @NonNull
        public Builder setAppIcon(@Nullable byte[] appIcon) {
            mAppIcon = appIcon;
            return this;
        }

        /**
         * Creates a new instance of {@link AppInfoMigrationPayload} with the specified arguments.
         */
        @NonNull
        public AppInfoMigrationPayload build() {
            return new AppInfoMigrationPayload(mPackageName, mAppName, mAppIcon);
        }
    }
}
