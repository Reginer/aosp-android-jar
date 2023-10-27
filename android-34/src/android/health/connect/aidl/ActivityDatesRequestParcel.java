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
import android.health.connect.datatypes.Record;
import android.health.connect.internal.datatypes.utils.RecordMapper;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A parcel for activity dates request containing list of record classes to query for.
 *
 * @hide
 */
public final class ActivityDatesRequestParcel implements Parcelable {
    @android.annotation.NonNull
    public static final Creator<ActivityDatesRequestParcel> CREATOR =
            new Creator<ActivityDatesRequestParcel>() {
                @Override
                public ActivityDatesRequestParcel createFromParcel(Parcel in) {
                    return new ActivityDatesRequestParcel(in);
                }

                @Override
                public ActivityDatesRequestParcel[] newArray(int size) {
                    return new ActivityDatesRequestParcel[size];
                }
            };

    private final List<Integer> mRecordTypes;

    public ActivityDatesRequestParcel(@NonNull List<Class<? extends Record>> recordTypes) {
        Objects.requireNonNull(recordTypes);
        RecordMapper recordMapper = RecordMapper.getInstance();
        mRecordTypes =
                recordTypes.stream().map(recordMapper::getRecordType).collect(Collectors.toList());
    }

    private ActivityDatesRequestParcel(Parcel in) {
        int size = in.readInt();
        mRecordTypes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            mRecordTypes.add(in.readInt());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written. May be 0 or {@link
     *     #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mRecordTypes.size());
        for (Integer recordTypeId : mRecordTypes) {
            dest.writeInt(recordTypeId);
        }
    }

    /** Returns a list of record types from this parcel. */
    @NonNull
    public List<Class<? extends Record>> getRecordTypes() {
        final Map<Integer, Class<? extends Record>> mRecordIdToExternalRecordClassMap =
                RecordMapper.getInstance().getRecordIdToExternalRecordClassMap();
        return mRecordTypes.stream()
                .map(mRecordIdToExternalRecordClassMap::get)
                .collect(Collectors.toList());
    }
}
