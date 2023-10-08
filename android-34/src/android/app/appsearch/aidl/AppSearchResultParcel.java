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
import android.app.appsearch.ParcelableUtil;
import android.app.appsearch.AppSearchResult;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Parcelable wrapper around {@link AppSearchResult}.
 *
 * <p>{@link AppSearchResult} can contain any value, including non-parcelable values. For the
 * specific case of sending {@link AppSearchResult} across Binder, this class wraps an
 * {@link AppSearchResult} that contains a parcelable type and provides parcelability of the whole
 * structure.
 *
 * @param <ValueType> The type of result object for successful calls. Must be a parcelable type.
 * @hide
 */
public final class AppSearchResultParcel<ValueType> implements Parcelable {
    private final AppSearchResult<ValueType> mResult;

    /** Creates a new {@link AppSearchResultParcel} from the given result. */
    public AppSearchResultParcel(@NonNull AppSearchResult<ValueType> result) {
        mResult = Objects.requireNonNull(result);
    }

    private AppSearchResultParcel(@NonNull Parcel in) {
        byte[] dataBlob = Objects.requireNonNull(ParcelableUtil.readBlob(in));
        Parcel data = Parcel.obtain();
        try {
            data.unmarshall(dataBlob, 0, dataBlob.length);
            data.setDataPosition(0);
            mResult = (AppSearchResult<ValueType>) directlyReadFromParcel(data);
        } finally {
            data.recycle();
        }
    }

    static AppSearchResult directlyReadFromParcel(@NonNull Parcel data) {
        int resultCode = data.readInt();
        Object resultValue = data.readValue(/*loader=*/ null);
        String errorMessage = data.readString();
        if (resultCode == AppSearchResult.RESULT_OK) {
            return AppSearchResult.newSuccessfulResult(resultValue);
        } else {
            return AppSearchResult.newFailedResult(resultCode, errorMessage);
        }
    }

    @NonNull
    public AppSearchResult<ValueType> getResult() {
        return mResult;
    }

    /** @hide */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        // Serializes the whole object, So that we can use Parcel.writeBlob() to send data.
        // WriteBlob() could take care of whether to pass data via binder directly or Android shared
        // memory if the data is large.
        byte[] bytes;
        Parcel data = Parcel.obtain();
        try {
            directlyWriteToParcel(data, mResult);
            bytes = data.marshall();
        } finally {
            data.recycle();
        }
        ParcelableUtil.writeBlob(dest, bytes);
    }

    static void directlyWriteToParcel(@NonNull Parcel data, @NonNull AppSearchResult result) {
        data.writeInt(result.getResultCode());
        if (result.isSuccess()) {
            data.writeValue(result.getResultValue());
        } else {
            data.writeValue(null);
        }
        data.writeString(result.getErrorMessage());
    }

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @NonNull
    public static final Creator<AppSearchResultParcel<?>> CREATOR =
            new Creator<AppSearchResultParcel<?>>() {
                @NonNull
                @Override
                public AppSearchResultParcel<?> createFromParcel(@NonNull Parcel in) {
                    return new AppSearchResultParcel<>(in);
                }

                @NonNull
                @Override
                public AppSearchResultParcel<?>[] newArray(int size) {
                    return new AppSearchResultParcel<?>[size];
                }
            };
}
