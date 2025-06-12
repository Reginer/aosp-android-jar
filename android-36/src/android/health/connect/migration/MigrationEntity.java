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
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A class to represent a migration entity.
 *
 * @hide
 */
@SystemApi
public final class MigrationEntity implements Parcelable {

    @NonNull
    public static final Creator<MigrationEntity> CREATOR =
            new Creator<>() {
                @Override
                public MigrationEntity createFromParcel(Parcel in) {
                    return new MigrationEntity(in);
                }

                @Override
                public MigrationEntity[] newArray(int size) {
                    return new MigrationEntity[size];
                }
            };

    private final String mEntityId;
    private final MigrationPayload mPayload;

    /**
     * @param entityId a stable identifier of the entity, must be unique within the entire
     *     migration. Duplicated entities are ignored.
     * @param payload a payload of the entity to migrate.
     */
    public MigrationEntity(@NonNull String entityId, @NonNull MigrationPayload payload) {
        requireNonNull(entityId);
        requireNonNull(payload);

        mEntityId = entityId;
        mPayload = payload;
    }

    private MigrationEntity(@NonNull Parcel in) {
        mEntityId = in.readString();

        mPayload =
                in.readParcelable(MigrationPayload.class.getClassLoader(), MigrationPayload.class);
    }

    /**
     * Returns a stable identifier of the entity, unique within the entire migration. Duplicated
     * entities are ignored.
     */
    @NonNull
    public String getEntityId() {
        return mEntityId;
    }

    /** Returns a payload of the entity to migrate. */
    @NonNull
    public MigrationPayload getPayload() {
        return mPayload;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mEntityId);
        dest.writeParcelable(mPayload, 0);
    }
}
