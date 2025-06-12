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

package android.health.connect.backuprestore;

import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

/** @hide */
@FlaggedApi(FLAG_CLOUD_BACKUP_AND_RESTORE)
public final class BackupSettings implements Parcelable {

    @NonNull private final byte[] mData;

    public BackupSettings(@NonNull byte[] data) {
        mData = data;
    }

    private BackupSettings(Parcel in) {
        mData = in.readBlob();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BackupSettings that)) return false;
        return Arrays.equals(mData, that.mData);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mData);
    }

    @NonNull
    public static final Creator<BackupSettings> CREATOR =
            new Creator<BackupSettings>() {
                @Override
                public BackupSettings createFromParcel(Parcel in) {
                    return new BackupSettings(in);
                }

                @Override
                public BackupSettings[] newArray(int size) {
                    return new BackupSettings[size];
                }
            };

    @NonNull
    public byte[] getData() {
        return mData;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeBlob(mData);
    }
}
