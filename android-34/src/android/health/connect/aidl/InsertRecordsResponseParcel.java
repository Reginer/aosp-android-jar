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

import java.util.List;

/**
 * A parcel to carry response to {@link HealthConnectManager#insertRecords}
 *
 * @hide
 */
public class InsertRecordsResponseParcel implements Parcelable {
    /**
     * List of UIDs for each record that was inserted. Order of UIDs is same as the order of Records
     * in {@link HealthConnectManager#insertRecords}
     */
    @NonNull private final List<String> mUids;

    public InsertRecordsResponseParcel(@NonNull List<String> uids) {
        mUids = uids;
    }

    protected InsertRecordsResponseParcel(Parcel in) {
        mUids = in.createStringArrayList();
    }

    @NonNull
    public List<String> getUids() {
        return mUids;
    }

    @NonNull
    public static final Creator<InsertRecordsResponseParcel> CREATOR =
            new Creator<InsertRecordsResponseParcel>() {
                @Override
                public InsertRecordsResponseParcel createFromParcel(Parcel in) {
                    return new InsertRecordsResponseParcel(in);
                }

                @Override
                public InsertRecordsResponseParcel[] newArray(int size) {
                    return new InsertRecordsResponseParcel[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStringList(mUids);
    }
}
