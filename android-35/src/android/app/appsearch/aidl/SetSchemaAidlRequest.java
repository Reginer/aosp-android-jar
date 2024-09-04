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
import android.app.appsearch.AppSearchSchema;
import android.app.appsearch.AppSearchSession;
import android.app.appsearch.InternalVisibilityConfig;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import java.util.List;
import java.util.Objects;

/**
 * Encapsulates a request to make a binder call to update the schema of an {@link AppSearchSession}
 * database.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "SetSchemaAidlRequestCreator")
public final class SetSchemaAidlRequest extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<SetSchemaAidlRequest> CREATOR =
            new SetSchemaAidlRequestCreator();

    @NonNull
    @Field(id = 1, getter = "getCallerAttributionSource")
    private final AppSearchAttributionSource mCallerAttributionSource;

    @NonNull
    @Field(id = 2, getter = "getDatabaseName")
    private final String mDatabaseName;

    @NonNull
    @Field(id = 3, getter = "getSchemas")
    private final List<AppSearchSchema> mSchemas;

    @NonNull
    @Field(id = 4, getter = "getVisibilityConfigs")
    private final List<InternalVisibilityConfig> mVisibilityConfigs;

    @Field(id = 5, getter = "isForceOverride")
    private final boolean mForceOverride;

    @Field(id = 6, getter = "getSchemaVersion")
    private final int mSchemaVersion;

    @NonNull
    @Field(id = 7, getter = "getUserHandle")
    private final UserHandle mUserHandle;

    @Field(id = 8, getter = "getBinderCallStartTimeMillis")
    @ElapsedRealtimeLong
    private final long mBinderCallStartTimeMillis;

    @Field(id = 9, getter = "getSchemaMigrationCallType")
    private final int mSchemaMigrationCallType;

    /**
     * Updates the AppSearch schema for this database.
     *
     * @param callerAttributionSource The permission identity of the package that owns this schema.
     * @param databaseName The name of the database where this schema lives.
     * @param schemas List of {@link AppSearchSchema} objects.
     * @param visibilityConfigs List of {@link InternalVisibilityConfig} objects defining the
     *     visibility for the schema types.
     * @param forceOverride Whether to apply the new schema even if it is incompatible. All
     *     incompatible documents will be deleted.
     * @param schemaVersion The overall schema version number of the request.
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     * @param schemaMigrationCallType Indicates how a SetSchema call relative to SchemaMigration
     *     case.
     */
    @Constructor
    public SetSchemaAidlRequest(
            @Param(id = 1) @NonNull AppSearchAttributionSource callerAttributionSource,
            @Param(id = 2) @NonNull String databaseName,
            @Param(id = 3) @NonNull List<AppSearchSchema> schemas,
            @Param(id = 4) @NonNull List<InternalVisibilityConfig> visibilityConfigs,
            @Param(id = 5) boolean forceOverride,
            @Param(id = 6) int schemaVersion,
            @Param(id = 7) @NonNull UserHandle userHandle,
            @Param(id = 8) @ElapsedRealtimeLong long binderCallStartTimeMillis,
            @Param(id = 9) int schemaMigrationCallType) {
        mCallerAttributionSource = Objects.requireNonNull(callerAttributionSource);
        mDatabaseName = Objects.requireNonNull(databaseName);
        mSchemas = Objects.requireNonNull(schemas);
        mVisibilityConfigs = Objects.requireNonNull(visibilityConfigs);
        mForceOverride = forceOverride;
        mSchemaVersion = schemaVersion;
        mUserHandle = Objects.requireNonNull(userHandle);
        mBinderCallStartTimeMillis = binderCallStartTimeMillis;
        mSchemaMigrationCallType = schemaMigrationCallType;
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
    public List<AppSearchSchema> getSchemas() {
        return mSchemas;
    }

    @NonNull
    public List<InternalVisibilityConfig> getVisibilityConfigs() {
        return mVisibilityConfigs;
    }

    public boolean isForceOverride() {
        return mForceOverride;
    }

    public int getSchemaVersion() {
        return mSchemaVersion;
    }

    @NonNull
    public UserHandle getUserHandle() {
        return mUserHandle;
    }

    @ElapsedRealtimeLong
    public long getBinderCallStartTimeMillis() {
        return mBinderCallStartTimeMillis;
    }

    public int getSchemaMigrationCallType() {
        return mSchemaMigrationCallType;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        SetSchemaAidlRequestCreator.writeToParcel(this, dest, flags);
    }
}
