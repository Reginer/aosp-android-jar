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
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import java.util.Objects;

/**
 * Encapsulates a request to make a binder call to insert documents into the index.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "PutDocumentsAidlRequestCreator")
public class PutDocumentsAidlRequest extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<PutDocumentsAidlRequest> CREATOR =
            new PutDocumentsAidlRequestCreator();

    @NonNull
    @Field(id = 1, getter = "getCallerAttributionSource")
    private final AppSearchAttributionSource mCallerAttributionSource;

    @NonNull
    @Field(id = 2, getter = "getDatabaseName")
    private final String mDatabaseName;

    @NonNull
    @Field(id = 3, getter = "getDocumentsParcel")
    private final DocumentsParcel mDocumentsParcel;

    @NonNull
    @Field(id = 4, getter = "getUserHandle")
    private final UserHandle mUserHandle;

    @Field(id = 5, getter = "getBinderCallStartTimeMillis")
    @ElapsedRealtimeLong
    private final long mBinderCallStartTimeMillis;

    /**
     * Inserts documents into the index.
     *
     * @param callerAttributionSource The permission identity of the package that owns this
     *     document.
     * @param databaseName The name of the database where this document lives.
     * @param documentsParcel The Parcelable object contains a list of GenericDocument.
     * @param userHandle The Handle of the calling user.
     * @param binderCallStartTimeMillis The start timestamp of binder call in Millis.
     */
    @Constructor
    public PutDocumentsAidlRequest(
            @Param(id = 1) @NonNull AppSearchAttributionSource callerAttributionSource,
            @Param(id = 2) @NonNull String databaseName,
            @Param(id = 3) @NonNull DocumentsParcel documentsParcel,
            @Param(id = 4) @NonNull UserHandle userHandle,
            @Param(id = 5) @ElapsedRealtimeLong long binderCallStartTimeMillis) {
        mCallerAttributionSource = Objects.requireNonNull(callerAttributionSource);
        mDatabaseName = Objects.requireNonNull(databaseName);
        mDocumentsParcel = Objects.requireNonNull(documentsParcel);
        mUserHandle = Objects.requireNonNull(userHandle);
        mBinderCallStartTimeMillis = binderCallStartTimeMillis;
    }

    @NonNull
    public AppSearchAttributionSource getCallerAttributionSource() {
        return mCallerAttributionSource;
    }

    @NonNull
    public String getDatabaseName() {
        return mDatabaseName;
    }

    @NonNull
    public DocumentsParcel getDocumentsParcel() {
        return mDocumentsParcel;
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
        PutDocumentsAidlRequestCreator.writeToParcel(this, dest, flags);
    }
}
