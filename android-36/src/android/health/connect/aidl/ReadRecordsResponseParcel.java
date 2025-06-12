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
import android.health.connect.HealthConnectManager;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A parcel to carry response to {@link HealthConnectManager#readRecords}
 *
 * @hide
 */
public class ReadRecordsResponseParcel implements Parcelable {
    /** RecordsParcel read from {@link HealthConnectManager#readRecords} */
    private final RecordsParcel mRecordsParcel;

    /** page token to be used as a token for the next read request */
    private final long mPageToken;

    public ReadRecordsResponseParcel(@NonNull RecordsParcel recordsParcel, long pageToken) {
        mRecordsParcel = recordsParcel;
        mPageToken = pageToken;
    }

    protected ReadRecordsResponseParcel(Parcel in) {
        mRecordsParcel =
                in.readParcelable(RecordsParcel.class.getClassLoader(), RecordsParcel.class);
        mPageToken = in.readLong();
    }

    @NonNull
    public RecordsParcel getRecordsParcel() {
        return mRecordsParcel;
    }

    public long getPageToken() {
        return mPageToken;
    }

    public static final Creator<ReadRecordsResponseParcel> CREATOR =
            new Creator<ReadRecordsResponseParcel>() {
                @Override
                public ReadRecordsResponseParcel createFromParcel(Parcel in) {
                    return new ReadRecordsResponseParcel(in);
                }

                @Override
                public ReadRecordsResponseParcel[] newArray(int size) {
                    return new ReadRecordsResponseParcel[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(mRecordsParcel, 0);
        dest.writeLong(mPageToken);
    }
}
