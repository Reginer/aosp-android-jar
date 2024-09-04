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
import android.app.appsearch.functions.ExecuteAppFunctionRequest;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import java.util.Objects;

/**
 * Encapsulates a request to make a binder call to execute an app function.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "ExecuteAppFunctionAidlRequestCreator")
public final class ExecuteAppFunctionAidlRequest extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<ExecuteAppFunctionAidlRequest> CREATOR =
            new ExecuteAppFunctionAidlRequestCreator();

    @NonNull
    @Field(id = 1, getter = "getClientRequest")
    private final ExecuteAppFunctionRequest mClientRequest;

    @NonNull
    @Field(id = 2, getter = "getCallerAttributionSource")
    private final AppSearchAttributionSource mCallerAttributionSource;

    @Field(id = 3, getter = "getUserHandle")
    private final UserHandle mUserHandle;

    @Field(id = 4, getter = "getBinderCallStartTimeMillis")
    @ElapsedRealtimeLong
    private final long mBinderCallStartTimeMillis;

    @Constructor
    public ExecuteAppFunctionAidlRequest(
            @Param(id = 1) @NonNull ExecuteAppFunctionRequest clientRequest,
            @Param(id = 2) @NonNull AppSearchAttributionSource callerAttributionSource,
            @Param(id = 3) @NonNull UserHandle userHandle,
            @Param(id = 4) long binderCallStartTimeMillis) {
        mClientRequest = Objects.requireNonNull(clientRequest);
        mCallerAttributionSource = Objects.requireNonNull(callerAttributionSource);
        mUserHandle = Objects.requireNonNull(userHandle);
        mBinderCallStartTimeMillis = binderCallStartTimeMillis;
    }

    /** Returns the original request created by the client. */
    @NonNull
    public ExecuteAppFunctionRequest getClientRequest() {
        return mClientRequest;
    }

    @NonNull
    public AppSearchAttributionSource getCallerAttributionSource() {
        return mCallerAttributionSource;
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
        ExecuteAppFunctionAidlRequestCreator.writeToParcel(this, dest, flags);
    }
}
