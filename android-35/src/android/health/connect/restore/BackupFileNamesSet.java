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

package android.health.connect.restore;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;

import java.util.Objects;
import java.util.Set;

/**
 * Parcelable for passing a set of file names.
 *
 * @hide
 */
public final class BackupFileNamesSet implements Parcelable {
    private final Set<String> mFileNames;

    /** @hide */
    public BackupFileNamesSet(@NonNull Set<String> fileNames) {
        Objects.requireNonNull(fileNames);
        mFileNames = fileNames;
    }

    public Set<String> getFileNames() {
        return mFileNames;
    }

    @NonNull
    public static final Creator<BackupFileNamesSet> CREATOR =
            new Creator<>() {
                @Override
                public BackupFileNamesSet createFromParcel(Parcel in) {
                    return new BackupFileNamesSet(in);
                }

                @Override
                public BackupFileNamesSet[] newArray(int size) {
                    return new BackupFileNamesSet[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    private BackupFileNamesSet(Parcel in) {
        int size = in.readInt();
        mFileNames = new ArraySet<>(size);
        for (int i = 0; i < size; ++i) {
            mFileNames.add(in.readString());
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mFileNames.size());
        for (var fileName : mFileNames) {
            dest.writeString(fileName);
        }
    }
}
