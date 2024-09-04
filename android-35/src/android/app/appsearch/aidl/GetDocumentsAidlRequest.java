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
import android.app.appsearch.GetByDocumentIdRequest;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import java.util.Objects;

/**
 * Encapsulates a request to make a binder call to retrieve documents from the index.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "GetDocumentsAidlRequestCreator")
public class GetDocumentsAidlRequest extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<GetDocumentsAidlRequest> CREATOR =
            new GetDocumentsAidlRequestCreator();

    // The permission identity of the package that is getting this document.
    @NonNull
    @Field(id = 1, getter = "getCallerAttributionSource")
    private final AppSearchAttributionSource mCallerAttributionSource;

    // The name of the package that owns this document.
    @NonNull
    @Field(id = 2, getter = "getTargetPackageName")
    private final String mTargetPackageName;

    // The name of the package that owns this document.
    @NonNull
    @Field(id = 3, getter = "getDatabaseName")
    private final String mDatabaseName;

    // The request to retrieve by namespace and IDs from the {@link
    // AppSearchSession} database for this document.
    @NonNull
    @Field(id = 4, getter = "getGetByDocumentIdRequest")
    private final GetByDocumentIdRequest mGetByDocumentIdRequest;

    // The Handle of the calling user.
    @NonNull
    @Field(id = 5, getter = "getUserHandle")
    private final UserHandle mUserHandle;

    // The start timestamp of binder call in Millis.
    @Field(id = 6, getter = "getBinderCallStartTimeMillis")
    @ElapsedRealtimeLong
    private final long mBinderCallStartTimeMillis;

    // Whether to query the user's enterprise profile AppSearch instance
    @Field(id = 7, getter = "isForEnterprise")
    private final boolean mIsForEnterprise;

    /**
     * Retrieves documents from the index.
     *
     * @param callerAttributionSource The permission identity of the package that is getting this
     *     document.
     * @param targetPackageName The name of the package that owns this document.
     * @param databaseName The databaseName this document resides in.
     * @param getByDocumentIdRequest The {@link GetByDocumentIdRequest} to retrieve document.
     * @param userHandle Handle of the calling user.
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis.
     * @param isForEnterprise Whether to query the user's enterprise profile AppSearch instance
     */
    @Constructor
    public GetDocumentsAidlRequest(
            @Param(id = 1) @NonNull AppSearchAttributionSource callerAttributionSource,
            @Param(id = 2) @NonNull String targetPackageName,
            @Param(id = 3) @NonNull String databaseName,
            @Param(id = 4) @NonNull GetByDocumentIdRequest getByDocumentIdRequest,
            @Param(id = 5) @NonNull UserHandle userHandle,
            @Param(id = 6) long binderCallStartTimeMillis,
            @Param(id = 7) boolean isForEnterprise) {
        mCallerAttributionSource = Objects.requireNonNull(callerAttributionSource);
        mTargetPackageName = Objects.requireNonNull(targetPackageName);
        mDatabaseName = Objects.requireNonNull(databaseName);
        mGetByDocumentIdRequest = Objects.requireNonNull(getByDocumentIdRequest);
        mUserHandle = Objects.requireNonNull(userHandle);
        mBinderCallStartTimeMillis = binderCallStartTimeMillis;
        mIsForEnterprise = isForEnterprise;
    }

    @NonNull
    public AppSearchAttributionSource getCallerAttributionSource() {
        return mCallerAttributionSource;
    }

    @NonNull
    public String getTargetPackageName() {
        return mTargetPackageName;
    }

    @NonNull
    public String getDatabaseName() {
        return mDatabaseName;
    }

    @NonNull
    public GetByDocumentIdRequest getGetByDocumentIdRequest() {
        return mGetByDocumentIdRequest;
    }

    @NonNull
    public UserHandle getUserHandle() {
        return mUserHandle;
    }

    @ElapsedRealtimeLong
    public long getBinderCallStartTimeMillis() {
        return mBinderCallStartTimeMillis;
    }

    public boolean isForEnterprise() {
        return mIsForEnterprise;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        GetDocumentsAidlRequestCreator.writeToParcel(this, dest, flags);
    }
}
