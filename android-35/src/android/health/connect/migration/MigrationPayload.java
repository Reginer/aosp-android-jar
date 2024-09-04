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

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A base class for migration payloads. There is no need extend this class, use existing subclasses
 * instead. <br>
 * <br>
 *
 * <p>Steps when adding a new type:
 *
 * <ul>
 *   <li>Create a new class, make sure it extends {@code MigrationPayload}.
 *   <li>Handle the new class in {@link MigrationPayload#CREATOR}.
 *   <li>Handle the new class in {@link MigrationEntity#writeToParcel(android.os.Parcel, int)} and
 *       in {@link MigrationEntity}'s constructor.
 *   <li>Handle the new class in {@link
 *       com.android.server.healthconnect.migration.DataMigrationManager}
 * </ul>
 *
 * Refer to existing subclasses for details.
 *
 * @hide
 */
@SuppressWarnings({"ParcelNotFinal", "ParcelCreator"}) // Can be only extended internally
@SystemApi
public abstract class MigrationPayload implements Parcelable {

    static final int TYPE_PACKAGE_PERMISSIONS = 1;
    static final int TYPE_RECORD = 2;
    static final int TYPE_APP_INFO = 3;
    static final int TYPE_PRIORITY = 4;
    static final int TYPE_METADATA = 5;

    @NonNull
    public static final Parcelable.Creator<MigrationPayload> CREATOR =
            new Creator<>() {
                @Override
                public MigrationPayload createFromParcel(Parcel source) {
                    final int type = source.readInt();
                    switch (type) {
                        case TYPE_PACKAGE_PERMISSIONS:
                            return new PermissionMigrationPayload(source);
                        case TYPE_RECORD:
                            return new RecordMigrationPayload(source);
                        case TYPE_APP_INFO:
                            return new AppInfoMigrationPayload(source);
                        case TYPE_PRIORITY:
                            return new PriorityMigrationPayload(source);
                        case TYPE_METADATA:
                            return new MetadataMigrationPayload(source);
                        default:
                            throw new IllegalStateException("Unexpected payload type: " + type);
                    }
                }

                @Override
                public MigrationPayload[] newArray(int size) {
                    return new MigrationPayload[size];
                }
            };

    /** Package-private constructor - instances and custom subclasses are not allowed. */
    MigrationPayload() {}

    @Override
    public int describeContents() {
        return 0;
    }
}
