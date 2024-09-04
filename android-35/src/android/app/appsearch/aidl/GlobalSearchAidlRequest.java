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
import android.os.Parcelable;
import android.os.UserHandle;

import java.util.Objects;

/**
 * Encapsulates a request to make a binder call to search over all permitted databases in the
 * AppSearch index.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "GlobalSearchAidlRequestCreator")
public class GlobalSearchAidlRequest extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<GlobalSearchAidlRequest> CREATOR =
            new GlobalSearchAidlRequestCreator();

    @NonNull
    @Field(id = 1, getter = "getCallerAttributionSource")
    private final AppSearchAttributionSource mCallerAttributionSource;

    @NonNull
    @Field(id = 2, getter = "getSearchExpression")
    private final String mSearchExpression;

    @NonNull
    @Field(id = 3, getter = "getSearchSpec")
    private final SearchSpec mSearchSpec;

    @NonNull
    @Field(id = 4, getter = "getUserHandle")
    private final UserHandle mUserHandle;

    @Field(id = 5, getter = "getBinderCallStartTimeMillis")
    @ElapsedRealtimeLong
    private final long mBinderCallStartTimeMillis;

    @Field(id = 6, getter = "isForEnterprise")
    private final boolean mIsForEnterprise;

    /**
     * Executes a global search, i.e. over all permitted databases, against the AppSearch index and
     * returns results.
     *
     * @param callerAttributionSource The permission identity of the package making the search.
     * @param searchExpression String to search for
     * @param searchSpec SearchSpec
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param isForEnterprise Whether to use the user's enterprise profile AppSearch instance
     */
    @Constructor
    public GlobalSearchAidlRequest(
            @Param(id = 1) @NonNull AppSearchAttributionSource callerAttributionSource,
            @Param(id = 2) @NonNull String searchExpression,
            @Param(id = 3) @NonNull SearchSpec searchSpec,
            @Param(id = 4) @NonNull UserHandle userHandle,
            @Param(id = 5) @ElapsedRealtimeLong long binderCallStartTimeMillis,
            @Param(id = 6) boolean isForEnterprise) {
        mCallerAttributionSource = Objects.requireNonNull(callerAttributionSource);
        mSearchExpression = Objects.requireNonNull(searchExpression);
        mSearchSpec = Objects.requireNonNull(searchSpec);
        mUserHandle = Objects.requireNonNull(userHandle);
        mBinderCallStartTimeMillis = binderCallStartTimeMillis;
        mIsForEnterprise = isForEnterprise;
    }

    @NonNull
    public AppSearchAttributionSource getCallerAttributionSource() {
        return mCallerAttributionSource;
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

    public boolean isForEnterprise() {
        return mIsForEnterprise;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        GlobalSearchAidlRequestCreator.writeToParcel(this, dest, flags);
    }
}
