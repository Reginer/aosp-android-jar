/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.app.appsearch.aidl;

import android.annotation.NonNull;
import android.app.appsearch.AppSearchBatchResult;
import android.app.appsearch.ParcelableUtil;
import android.app.appsearch.AppSearchResult;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Map;
import java.util.Objects;

/**
 * Parcelable wrapper around {@link AppSearchBatchResult}.
 *
 * <p>{@link AppSearchBatchResult} can contain any type of key and value, including non-parcelable
 * values. For the specific case of sending {@link AppSearchBatchResult} across Binder, this class
 * wraps an {@link AppSearchBatchResult} that has String keys and Parcelable values. It provides
 * parcelability of the whole structure.
 *
 * @param <ValueType> The type of result object for successful calls. Must be a parcelable type.
 * @hide
 */
public final class AppSearchBatchResultParcel<ValueType> implements Parcelable {
    private final AppSearchBatchResult<String, ValueType> mResult;

    /** Creates a new {@link AppSearchBatchResultParcel} from the given result. */
    public AppSearchBatchResultParcel(@NonNull AppSearchBatchResult<String, ValueType> result) {
        mResult = Objects.requireNonNull(result);
    }

    private AppSearchBatchResultParcel(@NonNull Parcel in) {
        Parcel unmarshallParcel = Parcel.obtain();
        try {
            byte[] dataBlob = Objects.requireNonNull(ParcelableUtil.readBlob(in));
            unmarshallParcel.unmarshall(dataBlob, 0, dataBlob.length);
            unmarshallParcel.setDataPosition(0);
            AppSearchBatchResult.Builder<String, ValueType> builder =
                    new AppSearchBatchResult.Builder<>();
            int size = unmarshallParcel.dataSize();
            while (unmarshallParcel.dataPosition() < size) {
                String key = Objects.requireNonNull(unmarshallParcel.readString());
                builder.setResult(key, (AppSearchResult<ValueType>) AppSearchResultParcel
                        .directlyReadFromParcel(unmarshallParcel));
            }
            mResult = builder.build();
        } finally {
            unmarshallParcel.recycle();
        }
    }

    @NonNull
    public AppSearchBatchResult<String, ValueType> getResult() {
        return mResult;
    }

    /** @hide */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        byte[] bytes;
        // Create a parcel object to serialize results. So that we can use Parcel.writeBlob() to
        // send data. WriteBlob() could take care of whether to pass data via binder directly or
        // Android shared memory if the data is large.
        Parcel data = Parcel.obtain();
        try {
            for (Map.Entry<String, AppSearchResult<ValueType>> entry
                    : mResult.getAll().entrySet()) {
                data.writeString(entry.getKey());
                AppSearchResultParcel.directlyWriteToParcel(data, entry.getValue());
            }
            bytes = data.marshall();
        } finally {
            data.recycle();
        }
        ParcelableUtil.writeBlob(dest, bytes);
    }

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @NonNull
    public static final Creator<AppSearchBatchResultParcel<?>> CREATOR =
            new Creator<AppSearchBatchResultParcel<?>>() {
                @NonNull
                @Override
                public AppSearchBatchResultParcel<?> createFromParcel(@NonNull Parcel in) {
                    return new AppSearchBatchResultParcel<>(in);
                }

                @NonNull
                @Override
                public AppSearchBatchResultParcel<?>[] newArray(int size) {
                    return new AppSearchBatchResultParcel<?>[size];
                }
            };
}
