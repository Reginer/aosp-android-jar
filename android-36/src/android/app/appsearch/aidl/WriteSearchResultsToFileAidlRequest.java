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
import android.app.appsearch.SearchSpec;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.UserHandle;

import java.util.Objects;

/**
 * Encapsulates a request to make a binder call to search for documents based on the given
 * specifications and save the results to the given {@link ParcelFileDescriptor}.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "WriteSearchResultsToFileAidlRequestCreator")
public class WriteSearchResultsToFileAidlRequest extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<WriteSearchResultsToFileAidlRequest> CREATOR =
            new WriteSearchResultsToFileAidlRequestCreator();

    @NonNull
    @Field(id = 1, getter = "getCallerAttributionSource")
    private final AppSearchAttributionSource mCallerAttributionSource;

    @NonNull
    @Field(id = 2, getter = "getDatabaseName")
    private final String mDatabaseName;

    @NonNull
    @Field(id = 3, getter = "getParcelFileDescriptor")
    private final ParcelFileDescriptor mParcelFileDescriptor;

    @NonNull
    @Field(id = 4, getter = "getSearchExpression")
    private final String mSearchExpression;

    @NonNull
    @Field(id = 5, getter = "getSearchSpec")
    private final SearchSpec mSearchSpec;

    @NonNull
    @Field(id = 6, getter = "getUserHandle")
    private final UserHandle mUserHandle;

    @Field(id = 7, getter = "getBinderCallStartTimeMillis")
    @ElapsedRealtimeLong
    private final long mBinderCallStartTimeMillis;

    /**
     * Searches a document based on a given specifications.
     *
     * <p>Documents will be save to the given ParcelFileDescriptor
     *
     * @param callerAttributionSource The permission identity of the package to search over.
     * @param databaseName The databaseName this search is for.
     * @param parcelFileDescriptor The ParcelFileDescriptor where documents should be written to.
     * @param searchExpression String to search for.
     * @param searchSpec SearchSpec
     * @param userHandle Handle of the calling user.
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     */
    @Constructor
    public WriteSearchResultsToFileAidlRequest(
            @Param(id = 1) @NonNull AppSearchAttributionSource callerAttributionSource,
            @Param(id = 2) @NonNull String databaseName,
            @Param(id = 3) @NonNull ParcelFileDescriptor parcelFileDescriptor,
            @Param(id = 4) @NonNull String searchExpression,
            @Param(id = 5) @NonNull SearchSpec searchSpec,
            @Param(id = 6) @NonNull UserHandle userHandle,
            @Param(id = 7) @ElapsedRealtimeLong long binderCallStartTimeMillis) {
        mCallerAttributionSource = Objects.requireNonNull(callerAttributionSource);
        mDatabaseName = Objects.requireNonNull(databaseName);
        mParcelFileDescriptor = Objects.requireNonNull(parcelFileDescriptor);
        mSearchExpression = Objects.requireNonNull(searchExpression);
        mSearchSpec = Objects.requireNonNull(searchSpec);
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
    public ParcelFileDescriptor getParcelFileDescriptor() {
        return mParcelFileDescriptor;
    }

    @NonNull
    public String getSearchExpression() {
        return mSearchExpression;
    }

    @NonNull
    public SearchSpec getSearchSpec() {
        return mSearchSpec;
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
        WriteSearchResultsToFileAidlRequestCreator.writeToParcel(this, dest, flags);
    }
}
