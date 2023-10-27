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

package android.health.connect.changelog;

import android.annotation.NonNull;
import android.health.connect.HealthConnectManager;
import android.health.connect.aidl.RecordsParcel;
import android.health.connect.datatypes.Record;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.utils.InternalExternalRecordConverter;
import android.os.Parcel;
import android.os.Parcelable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Response class for {@link HealthConnectManager#getChangeLogs} This is the response to clients
 * fetching changes
 */
public final class ChangeLogsResponse implements Parcelable {
    private final List<Record> mUpsertedRecords;
    private final List<DeletedLog> mDeletedLogs;
    private final String mNextChangesToken;
    private final boolean mHasMorePages;

    /**
     * Response for {@link HealthConnectManager#getChangeLogs}
     *
     * @hide
     */
    public ChangeLogsResponse(
            @NonNull RecordsParcel upsertedRecords,
            @NonNull List<DeletedLog> deletedLogs,
            @NonNull String nextChangesToken,
            boolean hasMorePages) {
        Objects.requireNonNull(upsertedRecords);
        Objects.requireNonNull(deletedLogs);
        Objects.requireNonNull(nextChangesToken);

        mUpsertedRecords =
                InternalExternalRecordConverter.getInstance()
                        .getExternalRecords(upsertedRecords.getRecords());
        mDeletedLogs = deletedLogs;
        mNextChangesToken = nextChangesToken;
        mHasMorePages = hasMorePages;
    }

    private ChangeLogsResponse(Parcel in) {
        mUpsertedRecords =
                InternalExternalRecordConverter.getInstance()
                        .getExternalRecords(
                                in.readParcelable(
                                                RecordsParcel.class.getClassLoader(),
                                                RecordsParcel.class)
                                        .getRecords());
        int size = in.readInt();
        List<DeletedLog> deletedLogs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = in.readString();
            long time = in.readLong();
            deletedLogs.add(new DeletedLog(id, time));
        }
        mDeletedLogs = deletedLogs;
        mNextChangesToken = in.readString();
        mHasMorePages = in.readBoolean();
    }

    @NonNull
    public static final Creator<ChangeLogsResponse> CREATOR =
            new Creator<ChangeLogsResponse>() {
                @Override
                public ChangeLogsResponse createFromParcel(Parcel in) {
                    return new ChangeLogsResponse(in);
                }

                @Override
                public ChangeLogsResponse[] newArray(int size) {
                    return new ChangeLogsResponse[size];
                }
            };

    /**
     * Returns records that have been updated or inserted post the time when the given token was
     * generated.
     *
     * <p>Clients can use the last modified time of the record to check when the record was
     * modified.
     */
    @NonNull
    public List<Record> getUpsertedRecords() {
        return mUpsertedRecords;
    }

    /**
     * Returns delete logs for records that have been deleted post the time when the token was
     * requested from {@link HealthConnectManager#getChangeLogToken}
     *
     * <p>This contains record id of deleted record and the timestamp when the record was deleted.
     */
    @NonNull
    public List<DeletedLog> getDeletedLogs() {
        return mDeletedLogs;
    }

    /** Returns token for future reads using {@link HealthConnectManager#getChangeLogs} */
    @NonNull
    public String getNextChangesToken() {
        return mNextChangesToken;
    }

    /** Returns whether there are more pages available for read */
    public boolean hasMorePages() {
        return mHasMorePages;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        List<RecordInternal<?>> recordInternal = new ArrayList<>();
        for (Record record : mUpsertedRecords) {
            recordInternal.add(record.toRecordInternal());
        }
        dest.writeParcelable(new RecordsParcel(recordInternal), 0);
        dest.writeInt(mDeletedLogs.size());
        for (DeletedLog deletedLog : mDeletedLogs) {
            dest.writeString(deletedLog.getDeletedRecordId());
            dest.writeLong(deletedLog.getDeletedTime().toEpochMilli());
        }
        dest.writeString(mNextChangesToken);
        dest.writeBoolean(mHasMorePages);
    }

    /** A class to represent a delete log in ChangeLogsResponse */
    public static final class DeletedLog {
        private final String mDeletedRecordId;
        private final Instant mDeletedTime;

        public DeletedLog(@NonNull String deletedRecordId, long deletedTime) {
            Objects.requireNonNull(deletedRecordId);
            mDeletedRecordId = deletedRecordId;
            mDeletedTime = Instant.ofEpochMilli(deletedTime);
        }

        /** Returns record id of the record deleted */
        @NonNull
        public String getDeletedRecordId() {
            return mDeletedRecordId;
        }

        /** Returns timestamp when the record was deleted */
        @NonNull
        public Instant getDeletedTime() {
            return mDeletedTime;
        }
    }
}
