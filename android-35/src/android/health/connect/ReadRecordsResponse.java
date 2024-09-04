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
import android.health.connect.datatypes.Record;

import java.util.List;

/**
 * Response containing list of Records for {@link HealthConnectManager#readRecords}.
 *
 * @param <T> the type of the Record for Read record Response
 */
public class ReadRecordsResponse<T extends Record> {
    private final List<T> mRecords;
    private final long mNextPageToken;

    /**
     * @param records List of records of type T
     * @param nextPageToken the token value of the read result which can be used as input token for
     *     next read request.
     * @hide
     */
    public ReadRecordsResponse(@NonNull List<T> records, long nextPageToken) {
        mRecords = records;
        mNextPageToken = nextPageToken;
    }

    @NonNull
    public List<T> getRecords() {
        return mRecords;
    }

    /**
     * Returns a page token to read the next page of the result. -1 if there are no more pages
     * available.
     */
    public long getNextPageToken() {
        return mNextPageToken;
    }
}
