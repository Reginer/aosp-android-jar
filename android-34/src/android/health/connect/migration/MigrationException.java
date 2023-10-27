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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * An exception thrown when an error encountered during migration.
 *
 * @hide
 */
@SystemApi
public final class MigrationException extends RuntimeException implements Parcelable {

    @NonNull
    public static final Creator<MigrationException> CREATOR =
            new Creator<>() {
                @Override
                public MigrationException createFromParcel(Parcel in) {
                    return new MigrationException(in);
                }

                @Override
                public MigrationException[] newArray(int size) {
                    return new MigrationException[size];
                }
            };

    /** An internal error occurred during migration. Retrying should resolve the problem. */
    public static final int ERROR_INTERNAL = 1;

    /**
     * An error occurred during migration of an entity, {@link #getFailedEntityId()} is guaranteed
     * to be not null.
     */
    public static final int ERROR_MIGRATE_ENTITY = 2;

    /**
     * Indicates that the module does not accept migration data anymore, the caller should stop the
     * migration process altogether.
     */
    public static final int ERROR_MIGRATION_UNAVAILABLE = 3;

    @ErrorCode private final int mErrorCode;
    private final String mFailedEntityId;

    public MigrationException(
            @Nullable String message, @ErrorCode int errorCode, @Nullable String failedEntityId) {
        super(message);

        mErrorCode = errorCode;
        mFailedEntityId = failedEntityId;
    }

    private MigrationException(@NonNull Parcel in) {
        super(in.readString());

        mErrorCode = in.readInt();
        mFailedEntityId = in.readString();
    }

    /** Returns the migration error code. */
    @ErrorCode
    public int getErrorCode() {
        return mErrorCode;
    }

    /**
     * Returns an optional id of the first failed entity, populated when the error code is {@link
     * MigrationException#ERROR_MIGRATE_ENTITY}.
     */
    @Nullable
    public String getFailedEntityId() {
        return mFailedEntityId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(getMessage());
        dest.writeInt(mErrorCode);
        dest.writeString(mFailedEntityId);
    }

    /**
     * List of possible error codes returned by the migration APIs.
     *
     * @hide
     */
    @IntDef({
        ERROR_INTERNAL,
        ERROR_MIGRATE_ENTITY,
        ERROR_MIGRATION_UNAVAILABLE,
    })
    public @interface ErrorCode {}
}
