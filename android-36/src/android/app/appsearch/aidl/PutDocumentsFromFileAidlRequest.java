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
import android.app.appsearch.stats.SchemaMigrationStats;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.UserHandle;

import java.util.Objects;

/**
 * Encapsulates a request to make a binder call to insert documents from the given file into the
 * index.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "PutDocumentsFromFileAidlRequestCreator")
public class PutDocumentsFromFileAidlRequest extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<PutDocumentsFromFileAidlRequest> CREATOR =
            new PutDocumentsFromFileAidlRequestCreator();

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
    @Field(id = 4, getter = "getUserHandle")
    private final UserHandle mUserHandle;

    @NonNull
    @Field(id = 5, getter = "getSchemaMigrationStats")
    private final SchemaMigrationStats mSchemaMigrationStats;

    @Field(id = 6, getter = "getTotalLatencyStartTimeMillis")
    @ElapsedRealtimeLong
    private final long mTotalLatencyStartTimeMillis;

    @Field(id = 7, getter = "getBinderCallStartTimeMillis")
    @ElapsedRealtimeLong
    private final long mBinderCallStartTimeMillis;

    /**
     * Inserts documents from the given file into the index.
     *
     * <p>This method does not dispatch change notifications for the individual documents being
     * inserted, so it is only appropriate to use for batch upload situations where a broader change
     * notification will indicate what has changed, like schema migration.
     *
     * @param callerAttributionSource The permission identity of the package that owns this
     *     document.
     * @param databaseName The name of the database where this document lives.
     * @param parcelFileDescriptor The ParcelFileDescriptor where documents should be read from.
     * @param userHandle Handle of the calling user.
     * @param schemaMigrationStats the Parcelable contains SchemaMigrationStats information.
     * @param totalLatencyStartTimeMillis start timestamp to calculate total migration latency in
     *     Millis
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     */
    @Constructor
    public PutDocumentsFromFileAidlRequest(
            @Param(id = 1) @NonNull AppSearchAttributionSource callerAttributionSource,
            @Param(id = 2) @NonNull String databaseName,
            @Param(id = 3) @NonNull ParcelFileDescriptor parcelFileDescriptor,
            @Param(id = 4) @NonNull UserHandle userHandle,
            @Param(id = 5) @NonNull SchemaMigrationStats schemaMigrationStats,
            @Param(id = 6) @ElapsedRealtimeLong long totalLatencyStartTimeMillis,
            @Param(id = 7) @ElapsedRealtimeLong long binderCallStartTimeMillis) {
        mCallerAttributionSource = Objects.requireNonNull(callerAttributionSource);
        mDatabaseName = Objects.requireNonNull(databaseName);
        mParcelFileDescriptor = Objects.requireNonNull(parcelFileDescriptor);
        mUserHandle = Objects.requireNonNull(userHandle);
        mSchemaMigrationStats = Objects.requireNonNull(schemaMigrationStats);
        mTotalLatencyStartTimeMillis = totalLatencyStartTimeMillis;
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
    public UserHandle getUserHandle() {
        return mUserHandle;
    }

    @NonNull
    public SchemaMigrationStats getSchemaMigrationStats() {
        return mSchemaMigrationStats;
    }

    public long getTotalLatencyStartTimeMillis() {
        return mTotalLatencyStartTimeMillis;
    }

    public long getBinderCallStartTimeMillis() {
        return mBinderCallStartTimeMillis;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        PutDocumentsFromFileAidlRequestCreator.writeToParcel(this, dest, flags);
    }
}
