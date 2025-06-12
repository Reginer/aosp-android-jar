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

package android.health.connect;

import android.annotation.NonNull;
import android.health.connect.aidl.ReadRecordsRequestParcel;
import android.health.connect.datatypes.Record;
import android.os.OutcomeReceiver;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * A Base class to represent a request for {@link
 * HealthConnectManager#readRecords(ReadRecordsRequest, Executor, OutcomeReceiver)}
 *
 * @param <T> the type of the Record for the request
 */
public abstract class ReadRecordsRequest<T extends Record> {
    /** Record class for record type identifier of the ReadRecordsRequest */
    private final Class<T> mRecordType;

    /** @hide */
    protected ReadRecordsRequest(@NonNull Class<T> recordType) {
        Objects.requireNonNull(recordType);
        mRecordType = recordType;
    }

    /** Returns record type on which read is to be performed */
    @NonNull
    public Class<T> getRecordType() {
        return mRecordType;
    }

    /** @hide */
    @SuppressWarnings("HiddenAbstractMethod")
    abstract ReadRecordsRequestParcel toReadRecordsRequestParcel();
}
