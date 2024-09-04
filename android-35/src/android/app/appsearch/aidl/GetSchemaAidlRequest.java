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
 * Encapsulates a request to make a binder call to get the schema for a given database.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "GetSchemaAidlRequestCreator")
public class GetSchemaAidlRequest extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<GetSchemaAidlRequest> CREATOR =
            new GetSchemaAidlRequestCreator();

    @NonNull
    @Field(id = 1, getter = "getCallerAttributionSource")
    private final AppSearchAttributionSource mCallerAttributionSource;

    @NonNull
    @Field(id = 2, getter = "getTargetPackageName")
    private final String mTargetPackageName;

    @NonNull
    @Field(id = 3, getter = "getDatabaseName")
    private final String mDatabaseName;

    @NonNull
    @Field(id = 4, getter = "getUserHandle")
    private final UserHandle mUserHandle;

    @Field(id = 5, getter = "getBinderCallStartTimeMillis")
    @ElapsedRealtimeLong
    private final long mBinderCallStartTimeMillis;

    @Field(id = 6, getter = "isForEnterprise")
    private final boolean mIsForEnterprise;

    /**
     * Retrieves the AppSearch schema for this database.
     *
     * @param callerAttributionSource The permission identity of the package making this call.
     * @param targetPackageName The name of the package that owns the schema.
     * @param databaseName The name of the database to retrieve.
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     */
    @Constructor
    public GetSchemaAidlRequest(
            @Param(id = 1) @NonNull AppSearchAttributionSource callerAttributionSource,
            @Param(id = 2) @NonNull String targetPackageName,
            @Param(id = 3) @NonNull String databaseName,
            @Param(id = 4) @NonNull UserHandle userHandle,
            @Param(id = 5) @ElapsedRealtimeLong long binderCallStartTimeMillis,
            @Param(id = 6) boolean isForEnterprise) {
        mCallerAttributionSource = Objects.requireNonNull(callerAttributionSource);
        mTargetPackageName = Objects.requireNonNull(targetPackageName);
        mDatabaseName = Objects.requireNonNull(databaseName);
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
        GetSchemaAidlRequestCreator.writeToParcel(this, dest, flags);
    }
}
