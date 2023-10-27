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
import android.health.connect.RecordIdFilter;
import android.health.connect.internal.datatypes.utils.RecordMapper;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/** @hide */
public final class RecordIdFiltersParcel implements Parcelable {
    public static final Creator<RecordIdFiltersParcel> CREATOR =
            new Creator<RecordIdFiltersParcel>() {
                @Override
                public RecordIdFiltersParcel createFromParcel(Parcel in) {
                    return new RecordIdFiltersParcel(in);
                }

                @Override
                public RecordIdFiltersParcel[] newArray(int size) {
                    return new RecordIdFiltersParcel[size];
                }
            };

    private final List<RecordIdFilter> mRecordIdFilters;

    public RecordIdFiltersParcel(List<RecordIdFilter> recordIdFilters) {
        mRecordIdFilters = recordIdFilters;
    }

    private RecordIdFiltersParcel(Parcel in) {
        int size = in.readInt();
        mRecordIdFilters = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = in.readString();
            String clientRecordId = in.readString();
            // A RecordId Filter can be built either only with an id or client record id and the
            // other will be null.
            if (id != null) {
                mRecordIdFilters.add(
                        RecordIdFilter.fromId(
                                RecordMapper.getInstance()
                                        .getRecordIdToExternalRecordClassMap()
                                        .get(in.readInt()),
                                id));
            } else {
                mRecordIdFilters.add(
                        RecordIdFilter.fromClientRecordId(
                                RecordMapper.getInstance()
                                        .getRecordIdToExternalRecordClassMap()
                                        .get(in.readInt()),
                                clientRecordId));
            }
        }
    }

    public List<RecordIdFilter> getRecordIdFilters() {
        return mRecordIdFilters;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mRecordIdFilters.size());
        mRecordIdFilters.forEach(
                (recordId -> {
                    dest.writeString(recordId.getId());
                    dest.writeString(recordId.getClientRecordId());
                    dest.writeInt(
                            RecordMapper.getInstance().getRecordType(recordId.getRecordType()));
                }));
    }
}
