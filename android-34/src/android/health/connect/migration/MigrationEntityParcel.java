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
import android.health.connect.internal.ParcelUtils;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** @hide */
public final class MigrationEntityParcel implements Parcelable {
    @NonNull
    public static final Creator<MigrationEntityParcel> CREATOR =
            new Creator<>() {
                @Override
                public MigrationEntityParcel createFromParcel(Parcel in) {
                    return new MigrationEntityParcel(in);
                }

                @Override
                public MigrationEntityParcel[] newArray(int size) {
                    return new MigrationEntityParcel[size];
                }
            };

    private final List<MigrationEntity> mMigrationEntityList;

    private MigrationEntityParcel(@NonNull Parcel in) {
        in = ParcelUtils.getParcelForSharedMemoryIfRequired(in);
        mMigrationEntityList = new ArrayList<>();
        in.readParcelableList(
                mMigrationEntityList,
                MigrationEntity.class.getClassLoader(),
                MigrationEntity.class);
    }

    public MigrationEntityParcel(@NonNull List<MigrationEntity> migrationEntities) {
        Objects.requireNonNull(migrationEntities);
        mMigrationEntityList = migrationEntities;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        ParcelUtils.putToRequiredMemory(dest, flags, this::writeToMigrationEntityParcel);
    }

    private void writeToMigrationEntityParcel(@NonNull Parcel dest) {
        dest.writeParcelableList(mMigrationEntityList, 0);
    }

    @NonNull
    public List<MigrationEntity> getMigrationEntities() {
        return mMigrationEntityList;
    }
}
