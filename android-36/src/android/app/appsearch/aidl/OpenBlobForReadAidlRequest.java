/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.annotation.ElapsedRealtimeLong;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.AppSearchBlobHandle;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates a request to make a binder call to open a batch of blob from AppSearch to read.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "OpenBlobForReadAidlRequestCreator")
public class OpenBlobForReadAidlRequest extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<OpenBlobForReadAidlRequest> CREATOR =
            new OpenBlobForReadAidlRequestCreator();

    @NonNull
    @Field(id = 1, getter = "getCallerAttributionSource")
    private final AppSearchAttributionSource mCallerAttributionSource;

    @Nullable
    @Field(id = 2, getter = "getCallingDatabaseName")
    private final String mCallingDatabaseName;

    @NonNull
    @Field(id = 3, getter = "getBlobHandles")
    private final List<AppSearchBlobHandle> mBlobHandles;

    @NonNull
    @Field(id = 4, getter = "getUserHandle")
    private final UserHandle mUserHandle;

    @Field(id = 5, getter = "getBinderCallStartTimeMillis")
    private final long mBinderCallStartTimeMillis;

    /**
     * Retrieves documents from the index.
     *
     * @param callerAttributionSource The permission identity of the package that is getting this
     *     document.
     * @param callingDatabaseName The database name of these blob stored in.
     * @param blobHandles The blobs to read
     * @param userHandle Handle of the calling user.
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis.
     */
    @Constructor
    public OpenBlobForReadAidlRequest(
            @Param(id = 1) @NonNull AppSearchAttributionSource callerAttributionSource,
            @Param(id = 2) @Nullable String callingDatabaseName,
            @Param(id = 3) @NonNull List<AppSearchBlobHandle> blobHandles,
            @Param(id = 4) @NonNull UserHandle userHandle,
            @Param(id = 5) long binderCallStartTimeMillis) {
        mCallerAttributionSource = Objects.requireNonNull(callerAttributionSource);
        mCallingDatabaseName = callingDatabaseName;
        mBlobHandles = Objects.requireNonNull(blobHandles);
        mUserHandle = Objects.requireNonNull(userHandle);
        mBinderCallStartTimeMillis = binderCallStartTimeMillis;
    }

    @NonNull
    public AppSearchAttributionSource getCallerAttributionSource() {
        return mCallerAttributionSource;
    }

    @Nullable
    public String getCallingDatabaseName() {
        return mCallingDatabaseName;
    }

    /**
     * Gets the {@code list} of {@link AppSearchBlobHandle} to open {@link
     * android.os.ParcelFileDescriptor} for read blob.
     */
    @NonNull
    public List<AppSearchBlobHandle> getBlobHandles() {
        return Collections.unmodifiableList(mBlobHandles);
    }

    @NonNull
    public UserHandle getUserHandle() {
        return mUserHandle;
    }

    @ElapsedRealtimeLong
    public long getBinderCallStartTimeMillis() {
        return mBinderCallStartTimeMillis;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        OpenBlobForReadAidlRequestCreator.writeToParcel(this, dest, flags);
    }
}
