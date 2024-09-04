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
import android.app.appsearch.ReportUsageRequest;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import java.util.Objects;

/**
 * Encapsulates a request to make a binder call to reports usage of a particular document.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "ReportUsageAidlRequestCreator")
public class ReportUsageAidlRequest extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<ReportUsageAidlRequest> CREATOR =
            new ReportUsageAidlRequestCreator();

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
    @Field(id = 4, getter = "getReportUsageRequest")
    private final ReportUsageRequest mReportUsageRequest;

    @Field(id = 5, getter = "isSystemUsage")
    private final boolean mSystemUsage;

    @NonNull
    @Field(id = 6, getter = "getUserHandle")
    private final UserHandle mUserHandle;

    @Field(id = 7, getter = "getBinderCallStartTimeMillis")
    @ElapsedRealtimeLong
    private final long mBinderCallStartTimeMillis;

    /**
     * Reports usage of a particular document by namespace and id.
     *
     * @param callerAttributionSource The permission identity of the package that owns this
     *     document.
     * @param targetPackageName The name of the package that owns this document.
     * @param databaseName The name of the database to report usage against.
     * @param reportUsageRequest The {@link ReportUsageRequest} to report usage for document.
     * @param systemUsage Whether the usage was reported by a system app against another app's doc.
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     */
    @Constructor
    public ReportUsageAidlRequest(
            @Param(id = 1) @NonNull AppSearchAttributionSource callerAttributionSource,
            @Param(id = 2) @NonNull String targetPackageName,
            @Param(id = 3) @NonNull String databaseName,
            @Param(id = 4) @NonNull ReportUsageRequest reportUsageRequest,
            @Param(id = 5) boolean systemUsage,
            @Param(id = 6) @NonNull UserHandle userHandle,
            @Param(id = 7) @ElapsedRealtimeLong long binderCallStartTimeMillis) {
        mCallerAttributionSource = Objects.requireNonNull(callerAttributionSource);
        mTargetPackageName = Objects.requireNonNull(targetPackageName);
        mDatabaseName = Objects.requireNonNull(databaseName);
        mReportUsageRequest = Objects.requireNonNull(reportUsageRequest);
        mSystemUsage = systemUsage;
        mUserHandle = Objects.requireNonNull(userHandle);
        mBinderCallStartTimeMillis = binderCallStartTimeMillis;
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
    public ReportUsageRequest getReportUsageRequest() {
        return mReportUsageRequest;
    }

    public boolean isSystemUsage() {
        return mSystemUsage;
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
        ReportUsageAidlRequestCreator.writeToParcel(this, dest, flags);
    }
}
