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

package android.health.connect.aidl;

import android.annotation.NonNull;
import android.health.connect.changelog.ChangeLogsResponse.DeletedLog;
import android.health.connect.internal.ParcelUtils;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Parcelable} that reads and writes {@link DeletedLog}s.
 *
 * @hide
 */
public final class DeletedLogsParcel implements Parcelable {

    @NonNull
    public static final Creator<DeletedLogsParcel> CREATOR =
            new Creator<>() {
                @Override
                public DeletedLogsParcel createFromParcel(Parcel in) {
                    return new DeletedLogsParcel(in);
                }

                @Override
                public DeletedLogsParcel[] newArray(int size) {
                    return new DeletedLogsParcel[size];
                }
            };

    private final List<DeletedLog> mDeletedLogs;

    public DeletedLogsParcel(@NonNull List<DeletedLog> deletedLogs) {
        mDeletedLogs = deletedLogs;
    }

    private DeletedLogsParcel(@NonNull Parcel in) {
        in = ParcelUtils.getParcelForSharedMemoryIfRequired(in);
        int size = in.readInt();
        mDeletedLogs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = in.readString();
            long time = in.readLong();
            mDeletedLogs.add(new DeletedLog(id, time));
        }
    }

    @NonNull
    public List<DeletedLog> getDeletedLogs() {
        return mDeletedLogs;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        ParcelUtils.putToRequiredMemory(dest, flags, this::writeToParcelInternal);
    }

    private void writeToParcelInternal(@NonNull Parcel dest) {
        dest.writeInt(mDeletedLogs.size());
        for (DeletedLog deletedLog : mDeletedLogs) {
            dest.writeString(deletedLog.getDeletedRecordId());
            dest.writeLong(deletedLog.getDeletedTime().toEpochMilli());
        }
    }
}
