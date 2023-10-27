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
import android.health.connect.datatypes.Record;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.utils.InternalExternalRecordConverter;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holds record migration data payload along with any migration-specific overrides.
 *
 * @hide
 */
@SystemApi
public final class RecordMigrationPayload extends MigrationPayload implements Parcelable {

    @NonNull
    public static final Creator<RecordMigrationPayload> CREATOR =
            new Creator<>() {
                @Override
                public RecordMigrationPayload createFromParcel(Parcel in) {
                    in.readInt(); // Skip the type
                    return new RecordMigrationPayload(in);
                }

                @Override
                public RecordMigrationPayload[] newArray(int size) {
                    return new RecordMigrationPayload[size];
                }
            };

    private final RecordInternal<?> mRecordInternal;
    @Nullable private Record mRecord;

    private RecordMigrationPayload(
            @NonNull String originPackageName,
            @NonNull String originAppName,
            @NonNull Record record) {
        mRecordInternal = record.toRecordInternal();
        mRecordInternal.setPackageName(originPackageName);
        mRecordInternal.setAppName(originAppName);

        mRecord = record;
    }

    RecordMigrationPayload(@NonNull Parcel in) {
        mRecordInternal =
                InternalExternalRecordConverter.getInstance().newInternalRecord(in.readInt());
        mRecordInternal.populateUsing(in);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(TYPE_RECORD);

        dest.writeInt(mRecordInternal.getRecordType());
        mRecordInternal.writeToParcel(dest);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** Returns origin package name associated with this payload. */
    @NonNull
    public String getOriginPackageName() {
        return mRecordInternal.getPackageName();
    }

    /** Returns origin application name associated with this payload. */
    @NonNull
    public String getOriginAppName() {
        return mRecordInternal.getAppName();
    }

    /** Returns {@link Record} associated with this payload. */
    @NonNull
    public Record getRecord() {
        if (mRecord == null) {
            mRecord = mRecordInternal.toExternalRecord();
        }

        return mRecord;
    }

    /** @hide */
    public RecordInternal<?> getRecordInternal() {
        return mRecordInternal;
    }

    /** Builder for {@link RecordMigrationPayload}. */
    public static final class Builder {
        private String mOriginPackageName;
        private String mOriginAppName;
        private Record mRecord;

        /**
         * @param originPackageName package name of the application authored the record.
         * @param originAppName name of the application authored the record.
         * @param record a record to migrate.
         */
        public Builder(
                @NonNull String originPackageName,
                @NonNull String originAppName,
                @NonNull Record record) {
            requireNonNull(originPackageName);
            requireNonNull(originAppName);
            requireNonNull(record);

            mOriginPackageName = originPackageName;
            mOriginAppName = originAppName;
            mRecord = record;
        }

        /** Sets the value for {@link RecordMigrationPayload#getOriginPackageName()}. */
        @NonNull
        public Builder setOriginPackageName(@NonNull String originPackageName) {
            requireNonNull(originPackageName);
            mOriginPackageName = originPackageName;
            return this;
        }

        /** Sets the value for {@link RecordMigrationPayload#getOriginAppName()} ()}. */
        @NonNull
        public Builder setOriginAppName(@NonNull String originAppName) {
            requireNonNull(originAppName);
            mOriginAppName = originAppName;
            return this;
        }

        /** Sets the value for {@link RecordMigrationPayload#getRecord()} ()}. */
        @NonNull
        public Builder setRecord(@NonNull Record record) {
            requireNonNull(record);
            mRecord = record;
            return this;
        }

        /**
         * Creates a new instance of {@link RecordMigrationPayload} with the specified arguments.
         */
        @NonNull
        public RecordMigrationPayload build() {
            return new RecordMigrationPayload(mOriginPackageName, mOriginAppName, mRecord);
        }
    }
}
