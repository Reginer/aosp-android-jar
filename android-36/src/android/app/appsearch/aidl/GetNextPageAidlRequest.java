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
import android.app.appsearch.AppSearchSchema;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import java.util.Objects;

/**
 * Encapsulates a request to make a binder call to fetch the next page of results of a previously
 * executed search.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "GetNextPageAidlRequestCreator")
public class GetNextPageAidlRequest extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<GetNextPageAidlRequest> CREATOR =
            new GetNextPageAidlRequestCreator();

    @NonNull
    @Field(id = 1, getter = "getCallerAttributionSource")
    private final AppSearchAttributionSource mCallerAttributionSource;

    @Nullable
    @Field(id = 2, getter = "getDatabaseName")
    private final String mDatabaseName;

    @Field(id = 3, getter = "getNextPageToken")
    private final long mNextPageToken;

    @Field(id = 4, getter = "getJoinType")
    @AppSearchSchema.StringPropertyConfig.JoinableValueType
    private final int mJoinType;

    @NonNull
    @Field(id = 5, getter = "getUserHandle")
    private final UserHandle mUserHandle;

    @Field(id = 6, getter = "getBinderCallStartTimeMillis")
    @ElapsedRealtimeLong
    private final long mBinderCallStartTimeMillis;

    @Field(id = 7, getter = "isForEnterprise")
    private final boolean mIsForEnterprise;

    /**
     * Fetches the next page of results of a previously executed search. Results can be empty if
     * next-page token is invalid or all pages have been returned.
     *
     * @param callerAttributionSource The permission identity of the package to persist to disk for.
     * @param databaseName The nullable databaseName this search for. The databaseName will be null
     *     if the search is a global search.
     * @param nextPageToken The token of pre-loaded results of previously executed search.
     * @param joinType the type of join performed. 0 if no join is performed
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param isForEnterprise Whether to use the user's enterprise profile AppSearch instance
     */
    @Constructor
    public GetNextPageAidlRequest(
            @Param(id = 1) @NonNull AppSearchAttributionSource callerAttributionSource,
            @Param(id = 2) @Nullable String databaseName,
            @Param(id = 3) long nextPageToken,
            @Param(id = 4) @AppSearchSchema.StringPropertyConfig.JoinableValueType int joinType,
            @Param(id = 5) @NonNull UserHandle userHandle,
            @Param(id = 6) @ElapsedRealtimeLong long binderCallStartTimeMillis,
            @Param(id = 7) boolean isForEnterprise) {
        mCallerAttributionSource = Objects.requireNonNull(callerAttributionSource);
        mDatabaseName = databaseName;
        mNextPageToken = nextPageToken;
        mJoinType = joinType;
        mUserHandle = Objects.requireNonNull(userHandle);
        mBinderCallStartTimeMillis = binderCallStartTimeMillis;
        mIsForEnterprise = isForEnterprise;
    }

    @NonNull
    public AppSearchAttributionSource getCallerAttributionSource() {
        return mCallerAttributionSource;
    }

    @Nullable
    public String getDatabaseName() {
        return mDatabaseName;
    }

    public long getNextPageToken() {
        return mNextPageToken;
    }

    @AppSearchSchema.StringPropertyConfig.JoinableValueType
    public int getJoinType() {
        return mJoinType;
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
        GetNextPageAidlRequestCreator.writeToParcel(this, dest, flags);
    }
}
