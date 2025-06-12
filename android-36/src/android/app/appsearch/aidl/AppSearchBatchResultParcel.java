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
import android.annotation.Nullable;
import android.app.appsearch.AppSearchBatchResult;
import android.app.appsearch.AppSearchResult;
import android.app.appsearch.ParcelableUtil;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.GenericDocumentParcel;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Bundle;
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
 * @deprecated This class is deprecated, you should use {@link AppSearchBatchResultParcelV2}.
 * @param <ValueType> The type of result object for successful calls. Must be a parcelable type.
 * @hide
 */
@Deprecated
@SafeParcelable.Class(creator = "AppSearchBatchResultParcelCreator", creatorIsFinal = false)
public final class AppSearchBatchResultParcel<ValueType> extends AbstractSafeParcelable {

    @NonNull
    @SuppressWarnings("rawtypes")
    public static final Parcelable.Creator<AppSearchBatchResultParcel> CREATOR =
            new AppSearchBatchResultParcelCreator() {
                @Override
                public AppSearchBatchResultParcel createFromParcel(Parcel in) {
                    byte[] dataBlob = Objects.requireNonNull(ParcelableUtil.readBlob(in));
                    Parcel unmarshallParcel = Parcel.obtain();
                    try {
                        unmarshallParcel.unmarshall(dataBlob, 0, dataBlob.length);
                        unmarshallParcel.setDataPosition(0);
                        int size = unmarshallParcel.dataSize();
                        Bundle inputBundle = new Bundle();
                        while (unmarshallParcel.dataPosition() < size) {
                            String key = Objects.requireNonNull(unmarshallParcel.readString());
                            AppSearchResultParcel appSearchResultParcel =
                                    AppSearchResultParcel.directlyReadFromParcel(unmarshallParcel);
                            inputBundle.putParcelable(key, appSearchResultParcel);
                        }
                        return new AppSearchBatchResultParcel(inputBundle);
                    } finally {
                        unmarshallParcel.recycle();
                    }
                }
            };

    // Map between String Key and AppSearchResultParcel Value.
    @Field(id = 1)
    @NonNull
    final Bundle mAppSearchResultBundle;

    @Nullable private AppSearchBatchResult<String, ValueType> mResultCached;

    @Constructor
    AppSearchBatchResultParcel(@Param(id = 1) Bundle appSearchResultBundle) {
        mAppSearchResultBundle = appSearchResultBundle;
    }

    /**
     * Creates a new {@link AppSearchBatchResultParcel} from the given {@link GenericDocumentParcel}
     * results.
     */
    @SuppressWarnings("unchecked")
    public static AppSearchBatchResultParcel<GenericDocumentParcel>
            fromStringToGenericDocumentParcel(
                    @NonNull AppSearchBatchResult<String, GenericDocumentParcel> result) {
        Bundle appSearchResultBundle = new Bundle();
        for (Map.Entry<String, AppSearchResult<GenericDocumentParcel>> entry :
                result.getAll().entrySet()) {
            AppSearchResultParcel<GenericDocumentParcel> appSearchResultParcel;
            // Create result from value in success case and errorMessage in
            // failure case.
            if (entry.getValue().isSuccess()) {
                GenericDocumentParcel genericDocumentParcel =
                        Objects.requireNonNull(entry.getValue().getResultValue());
                appSearchResultParcel =
                        AppSearchResultParcel.fromGenericDocumentParcel(genericDocumentParcel);
            } else {
                appSearchResultParcel = AppSearchResultParcel.fromFailedResult(entry.getValue());
            }
            appSearchResultBundle.putParcelable(entry.getKey(), appSearchResultParcel);
        }
        return new AppSearchBatchResultParcel<>(appSearchResultBundle);
    }

    /** Creates a new {@link AppSearchBatchResultParcel} from the given {@link Void} results. */
    @SuppressWarnings("unchecked")
    public static AppSearchBatchResultParcel<Void> fromStringToVoid(
            @NonNull AppSearchBatchResult<String, Void> result) {
        Bundle appSearchResultBundle = new Bundle();
        for (Map.Entry<String, AppSearchResult<Void>> entry : result.getAll().entrySet()) {
            AppSearchResultParcel<Void> appSearchResultParcel;
            // Create result from value in success case and errorMessage in
            // failure case.
            if (entry.getValue().isSuccess()) {
                appSearchResultParcel = AppSearchResultParcel.fromVoid();
            } else {
                appSearchResultParcel = AppSearchResultParcel.fromFailedResult(entry.getValue());
            }
            appSearchResultBundle.putParcelable(entry.getKey(), appSearchResultParcel);
        }
        return new AppSearchBatchResultParcel<>(appSearchResultBundle);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public AppSearchBatchResult<String, ValueType> getResult() {
        if (mResultCached == null) {
            AppSearchBatchResult.Builder<String, ValueType> builder =
                    new AppSearchBatchResult.Builder<>();
            for (String key : mAppSearchResultBundle.keySet()) {
                builder.setResult(
                        key,
                        mAppSearchResultBundle
                                .getParcelable(key, AppSearchResultParcel.class)
                                .getResult());
            }
            mResultCached = builder.build();
        }
        return mResultCached;
    }

    /** @hide */
    @Override
    @SuppressWarnings("unchecked")
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        byte[] bytes;
        // Create a parcel object to serialize results. So that we can use Parcel.writeBlob() to
        // send data. WriteBlob() could take care of whether to pass data via binder directly or
        // Android shared memory if the data is large.
        Parcel data = Parcel.obtain();
        try {
            for (String key : mAppSearchResultBundle.keySet()) {
                data.writeString(key);
                AppSearchResultParcel<ValueType> appSearchResultParcel =
                        mAppSearchResultBundle.getParcelable(key, AppSearchResultParcel.class);
                AppSearchResultParcel.directlyWriteToParcel(appSearchResultParcel, data, flags);
            }
            bytes = data.marshall();
        } finally {
            data.recycle();
        }
        ParcelableUtil.writeBlob(dest, bytes);
    }
}
