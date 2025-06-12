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
 * Encapsulates a request to make a binder call to invalidate the next-page token so that no more
 * results of the related search can be returned.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "InvalidateNextPageTokenAidlRequestCreator")
public class InvalidateNextPageTokenAidlRequest extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<InvalidateNextPageTokenAidlRequest> CREATOR =
            new InvalidateNextPageTokenAidlRequestCreator();

    @NonNull
    @Field(id = 1, getter = "getCallerAttributionSource")
    private final AppSearchAttributionSource mCallerAttributionSource;

    @Field(id = 2, getter = "getNextPageToken")
    private final long mNextPageToken;

    @NonNull
    @Field(id = 3, getter = "getUserHandle")
    private final UserHandle mUserHandle;

    @Field(id = 4, getter = "getBinderCallStartTimeMillis")
    @ElapsedRealtimeLong
    private final long mBinderCallStartTimeMillis;

    @Field(id = 5, getter = "isForEnterprise")
    private final boolean mIsForEnterprise;

    /**
     * Invalidates the next-page token so that no more results of the related search can be
     * returned.
     *
     * @param callerAttributionSource The permission identity of the package to persist to disk for.
     * @param nextPageToken The token of pre-loaded results of previously executed search to be
     *     invalidated.
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param isForEnterprise Whether to user the user's enterprise profile AppSearch instance
     */
    @Constructor
    public InvalidateNextPageTokenAidlRequest(
            @Param(id = 1) @NonNull AppSearchAttributionSource callerAttributionSource,
            @Param(id = 2) long nextPageToken,
            @Param(id = 3) @NonNull UserHandle userHandle,
            @Param(id = 4) @ElapsedRealtimeLong long binderCallStartTimeMillis,
            @Param(id = 5) boolean isForEnterprise) {
        mCallerAttributionSource = Objects.requireNonNull(callerAttributionSource);
        mNextPageToken = nextPageToken;
        mUserHandle = Objects.requireNonNull(userHandle);
        mBinderCallStartTimeMillis = binderCallStartTimeMillis;
        mIsForEnterprise = isForEnterprise;
    }

    @NonNull
    public AppSearchAttributionSource getCallerAttributionSource() {
        return mCallerAttributionSource;
    }

    public long getNextPageToken() {
        return mNextPageToken;
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
        InvalidateNextPageTokenAidlRequestCreator.writeToParcel(this, dest, flags);
    }
}
