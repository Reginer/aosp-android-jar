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
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

/** @hide */
@FlaggedApi(FLAG_CLOUD_BACKUP_AND_RESTORE)
public final class RestoreChange implements Parcelable {

    // The data is returned as bytes rather than records to keep the data opaque from the client.
    // As long as the client doesn't parse the data, it doesn't know what type of data this is.
    @NonNull private final byte[] mData;

    public RestoreChange(@NonNull byte[] data) {
        mData = data;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof RestoreChange that) && Arrays.equals(mData, that.mData);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mData);
    }

    private RestoreChange(Parcel in) {
        mData = in.readBlob();
    }

    @NonNull
    public static final Creator<RestoreChange> CREATOR =
            new Creator<>() {
                @Override
                public RestoreChange createFromParcel(Parcel in) {
                    return new RestoreChange(in);
                }

                @Override
                public RestoreChange[] newArray(int size) {
                    return new RestoreChange[size];
                }
            };

    @Nullable
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
