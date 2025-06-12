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

import static android.app.appsearch.ParcelableUtil.WRITE_PARCEL_MODE_DIRECTLY_WRITE_TO_PARCEL;
import static android.app.appsearch.ParcelableUtil.WRITE_PARCEL_MODE_MARSHALL_WRITE_IN_BLOB;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.AppSearchBatchResult;
import android.app.appsearch.AppSearchBlobHandle;
import android.app.appsearch.AppSearchResult;
import android.app.appsearch.ParcelableUtil;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

import com.android.appsearch.flags.Flags;

import java.util.Map;
import java.util.Objects;

/**
 * Parcelable wrapper around {@link AppSearchBatchResult}.
 *
 * <p>{@link AppSearchBatchResult} can contain any type of key and value, including non-parcelable
 * values. For the specific case of sending {@link AppSearchBatchResult} across Binder, this class
 * wraps an {@link AppSearchBatchResult} and provides parcelability of the whole structure.
 *
 * <p>Compare to deprecated {@link AppSearchBatchResultParcel}, this class could config how to write
 * it to the parcel. Therefore binder objects and {@link ParcelFileDescriptor} is supported in this
 * class. This class could also support general type as KeyType.
 *
 * @see ParcelableUtil#WRITE_PARCEL_MODE_MARSHALL_WRITE_IN_BLOB
 * @see ParcelableUtil#WRITE_PARCEL_MODE_DIRECTLY_WRITE_TO_PARCEL
 * @param <KeyType> The type of the keys for which the results will be reported. We are passing the
 *     class name of the KeyType to parcelable. Do not rename the class of KeyType, since that may
 *     cause compatibility issue for GmsCore.
 * @param <ValueType> The type of result object for successful calls. Must be a parcelable type.
 * @hide
 */
@SafeParcelable.Class(creator = "AppSearchBatchResultParcelV2Creator", creatorIsFinal = false)
public final class AppSearchBatchResultParcelV2<KeyType, ValueType> extends AbstractSafeParcelable {
    private static final String TAG = "AppSearchBatchResultPar";

    @NonNull
    // Provide ClassLoader when read from bundle in getResult() method
    @SuppressWarnings("rawtypes")
    public static final Parcelable.Creator<AppSearchBatchResultParcelV2> CREATOR =
            new AppSearchBatchResultParcelV2Creator() {
                @Override
                public AppSearchBatchResultParcelV2 createFromParcel(Parcel in) {
                    int writeParcelModel = in.readInt();
                    switch (writeParcelModel) {
                        case WRITE_PARCEL_MODE_MARSHALL_WRITE_IN_BLOB:
                            byte[] dataBlob = Objects.requireNonNull(ParcelableUtil.readBlob(in));
                            Parcel unmarshallParcel = Parcel.obtain();
                            try {
                                unmarshallParcel.unmarshall(dataBlob, 0, dataBlob.length);
                                unmarshallParcel.setDataPosition(0);
                                return directlyReadFromParcel(unmarshallParcel);
                            } finally {
                                unmarshallParcel.recycle();
                            }
                        case WRITE_PARCEL_MODE_DIRECTLY_WRITE_TO_PARCEL:
                            return directlyReadFromParcel(in);
                        default:
                            throw new UnsupportedOperationException(
                                    "Cannot write AppSearchBatchResultParcelV2 to Parcel with "
                                            + "unknown model: "
                                            + writeParcelModel);
                    }
                }
            };

    /** The Creator used to directly write to parcel with calling {@link Parcel#writeBlob}. */
    @NonNull
    private static final Parcelable.Creator<AppSearchBatchResultParcelV2> CREATOR_WITHOUT_BLOB =
            new AppSearchBatchResultParcelV2Creator();

    static AppSearchBatchResultParcelV2<?, ?> directlyReadFromParcel(@NonNull Parcel data) {
        return CREATOR_WITHOUT_BLOB.createFromParcel(data);
    }

    static void directlyWriteToParcel(
            @NonNull AppSearchBatchResultParcelV2<?, ?> result, @NonNull Parcel data, int flags) {
        AppSearchBatchResultParcelV2Creator.writeToParcel(result, data, flags);
    }

    /**
     * The flags indicate how we write this object to parcel and read it.
     *
     * @see ParcelableUtil#WRITE_PARCEL_MODE_MARSHALL_WRITE_IN_BLOB
     * @see ParcelableUtil#WRITE_PARCEL_MODE_DIRECTLY_WRITE_TO_PARCEL
     */
    @Field(id = 1)
    @ParcelableUtil.WriteParcelMode
    int mWriteParcelModel;

    @Field(id = 2)
    @NonNull
    final String mKeyClassName;

    // Map stores keys of AppSearchBatchResult. The key will be an integer of a consecutive
    // increasing sequence. Associated with mAppSearchResultValueBundle.
    @Field(id = 3)
    @NonNull
    final Bundle mKeyBundle;

    // Map stores keys of AppSearchBatchResult. The key will be an integer of a consecutive
    // increasing sequence. Associated with mKeyBundle.
    @Field(id = 4)
    @NonNull
    final Bundle mAppSearchResultValueBundle;

    @Nullable private AppSearchBatchResult<KeyType, ValueType> mResultCached;

    @Constructor
    AppSearchBatchResultParcelV2(
            @Param(id = 1) @ParcelableUtil.WriteParcelMode int writeParcelModel,
            @Param(id = 2) String keyClassName,
            @Param(id = 3) Bundle keyBundle,
            @Param(id = 4) Bundle appSearchResultValueBundle) {
        mWriteParcelModel = writeParcelModel;
        mKeyClassName = keyClassName;
        mKeyBundle = keyBundle;
        mAppSearchResultValueBundle = appSearchResultValueBundle;
    }

    /**
     * Creates a new {@link AppSearchBatchResultParcel} from the given {@link AppSearchBatchResult}
     * results which has {@link AppSearchBlobHandle} as keys and {@link ParcelFileDescriptor} as
     * values.
     */
    @SuppressWarnings("unchecked")
    @NonNull
    @FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
    public static AppSearchBatchResultParcelV2<AppSearchBlobHandle, ParcelFileDescriptor>
            fromBlobHandleToPfd(
                    @NonNull
                            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor>
                                    result) {
        Bundle keyAppSearchResultBundle = new Bundle();
        Bundle valueAppSearchResultBundle = new Bundle();
        int i = 0;
        for (Map.Entry<AppSearchBlobHandle, AppSearchResult<ParcelFileDescriptor>> entry :
                result.getAll().entrySet()) {
            AppSearchResultParcelV2<ParcelFileDescriptor> valueAppSearchBinderResultParcel;
            // Create result from value in success case and errorMessage in failure case.
            if (entry.getValue().isSuccess()) {
                valueAppSearchBinderResultParcel =
                        AppSearchResultParcelV2.fromParcelFileDescriptor(
                                entry.getValue().getResultValue());
            } else {
                valueAppSearchBinderResultParcel =
                        AppSearchResultParcelV2.fromFailedResult(entry.getValue());
            }
            keyAppSearchResultBundle.putParcelable(String.valueOf(i), entry.getKey());
            valueAppSearchResultBundle.putParcelable(
                    String.valueOf(i), valueAppSearchBinderResultParcel);
            ++i;
        }
        // We cannot marshall PFD!! We have to directly write this object to parcel.
        return new AppSearchBatchResultParcelV2<>(
                WRITE_PARCEL_MODE_DIRECTLY_WRITE_TO_PARCEL,
                AppSearchBlobHandle.class.getName(),
                keyAppSearchResultBundle,
                valueAppSearchResultBundle);
    }

    /**
     * Creates a new {@link AppSearchBatchResultParcel} from the given {@link AppSearchBatchResult}
     * results which has {@link AppSearchBlobHandle} as keys and {@code Void} as values.
     */
    @NonNull
    @FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
    public static AppSearchBatchResultParcelV2<AppSearchBlobHandle, Void> fromBlobHandleToVoid(
            @NonNull AppSearchBatchResult<AppSearchBlobHandle, Void> result) {
        Bundle keyAppSearchResultBundle = new Bundle();
        Bundle valueAppSearchResultBundle = new Bundle();
        int i = 0;
        for (Map.Entry<AppSearchBlobHandle, AppSearchResult<Void>> entry :
                result.getAll().entrySet()) {
            AppSearchResultParcelV2<ParcelFileDescriptor> valueAppSearchResultParcel;
            // Create result from value in success case and errorMessage in failure case.
            if (entry.getValue().isSuccess()) {
                valueAppSearchResultParcel = AppSearchResultParcelV2.fromVoid();
            } else {
                valueAppSearchResultParcel =
                        AppSearchResultParcelV2.fromFailedResult(entry.getValue());
            }
            keyAppSearchResultBundle.putParcelable(String.valueOf(i), entry.getKey());
            valueAppSearchResultBundle.putParcelable(String.valueOf(i), valueAppSearchResultParcel);
            ++i;
        }
        return new AppSearchBatchResultParcelV2<>(
                WRITE_PARCEL_MODE_MARSHALL_WRITE_IN_BLOB,
                AppSearchBlobHandle.class.getName(),
                keyAppSearchResultBundle,
                valueAppSearchResultBundle);
    }

    /** Gets the {@link AppSearchBatchResult} out of this {@link AppSearchBatchResultParcelV2}. */
    @NonNull
    @SuppressWarnings("unchecked")
    public AppSearchBatchResult<KeyType, ValueType> getResult() {
        if (mResultCached == null) {
            AppSearchBatchResult.Builder<KeyType, ValueType> builder =
                    new AppSearchBatchResult.Builder<>();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    java.lang.Class<?> clazz = java.lang.Class.forName(mKeyClassName);
                    for (String key : mKeyBundle.keySet()) {
                        builder.setResult(
                                (KeyType) mKeyBundle.getParcelable(key, clazz),
                                mAppSearchResultValueBundle
                                        .getParcelable(key, AppSearchResultParcelV2.class)
                                        .getResult());
                    }
                } catch (ClassNotFoundException e) {
                    // Impossible, the key type name should always match the KeyType.
                    throw new RuntimeException("Class not found: " + e.getMessage(), e);
                }
            } else {
                for (String key : mKeyBundle.keySet()) {
                    builder.setResult(
                            mKeyBundle.getParcelable(key),
                            ((AppSearchResultParcelV2)
                                            mAppSearchResultValueBundle.getParcelable(key))
                                    .getResult());
                }
            }

            mResultCached = builder.build();
        }
        return mResultCached;
    }

    /** @hide */
    @Override
    @SuppressWarnings("unchecked")
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mWriteParcelModel);
        switch (mWriteParcelModel) {
            case WRITE_PARCEL_MODE_MARSHALL_WRITE_IN_BLOB:
                byte[] bytes;
                // Create a parcel object to serialize results. So that we can use
                // Parcel.writeBlob() to
                // send data. WriteBlob() could take care of whether to pass data via binder
                // directly or
                // Android shared memory if the data is large.
                Parcel data = Parcel.obtain();
                try {
                    directlyWriteToParcel(this, data, flags);
                    bytes = data.marshall();
                } finally {
                    data.recycle();
                }
                ParcelableUtil.writeBlob(dest, bytes);
                break;
            case WRITE_PARCEL_MODE_DIRECTLY_WRITE_TO_PARCEL:
                directlyWriteToParcel(this, dest, flags);
                break;
            default:
                throw new UnsupportedOperationException(
                        "Cannot read AppSearchBatchResultParcelV2 from Parcel with "
                                + "unknown model: "
                                + mWriteParcelModel);
        }
    }
}
