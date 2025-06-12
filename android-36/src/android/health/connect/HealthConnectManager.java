/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.health.connect;

import static android.health.connect.Constants.DEFAULT_LONG;
import static android.health.connect.Constants.MAXIMUM_PAGE_SIZE;
import static android.health.connect.HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
import static android.health.connect.HealthPermissions.MANAGE_HEALTH_PERMISSIONS;
import static android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA;

import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE;
import static com.android.healthfitness.flags.Flags.FLAG_IMMEDIATE_EXPORT;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;

import android.Manifest;
import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.annotation.TestApi;
import android.annotation.UserHandleAware;
import android.annotation.WorkerThread;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.accesslog.AccessLogsResponseParcel;
import android.health.connect.aidl.ActivityDatesRequestParcel;
import android.health.connect.aidl.ActivityDatesResponseParcel;
import android.health.connect.aidl.AggregateDataRequestParcel;
import android.health.connect.aidl.AggregateDataResponseParcel;
import android.health.connect.aidl.ApplicationInfoResponseParcel;
import android.health.connect.aidl.DeleteUsingFiltersRequestParcel;
import android.health.connect.aidl.GetPriorityResponseParcel;
import android.health.connect.aidl.HealthConnectExceptionParcel;
import android.health.connect.aidl.IAccessLogsResponseCallback;
import android.health.connect.aidl.IActivityDatesResponseCallback;
import android.health.connect.aidl.IAggregateRecordsResponseCallback;
import android.health.connect.aidl.IApplicationInfoResponseCallback;
import android.health.connect.aidl.IChangeLogsResponseCallback;
import android.health.connect.aidl.IDataStagingFinishedCallback;
import android.health.connect.aidl.IEmptyResponseCallback;
import android.health.connect.aidl.IGetChangeLogTokenCallback;
import android.health.connect.aidl.IGetChangesForBackupResponseCallback;
import android.health.connect.aidl.IGetHealthConnectDataStateCallback;
import android.health.connect.aidl.IGetHealthConnectMigrationUiStateCallback;
import android.health.connect.aidl.IGetPriorityResponseCallback;
import android.health.connect.aidl.IGetSettingsForBackupResponseCallback;
import android.health.connect.aidl.IHealthConnectService;
import android.health.connect.aidl.IInsertRecordsResponseCallback;
import android.health.connect.aidl.IMedicalDataSourceResponseCallback;
import android.health.connect.aidl.IMedicalDataSourcesResponseCallback;
import android.health.connect.aidl.IMedicalResourceListParcelResponseCallback;
import android.health.connect.aidl.IMedicalResourceTypeInfosCallback;
import android.health.connect.aidl.IMedicalResourcesResponseCallback;
import android.health.connect.aidl.IMigrationCallback;
import android.health.connect.aidl.IReadMedicalResourcesResponseCallback;
import android.health.connect.aidl.IReadRecordsResponseCallback;
import android.health.connect.aidl.IRecordTypeInfoResponseCallback;
import android.health.connect.aidl.InsertRecordsResponseParcel;
import android.health.connect.aidl.MedicalResourceListParcel;
import android.health.connect.aidl.ReadRecordsResponseParcel;
import android.health.connect.aidl.RecordIdFiltersParcel;
import android.health.connect.aidl.RecordTypeInfoResponseParcel;
import android.health.connect.aidl.RecordsParcel;
import android.health.connect.aidl.UpdatePriorityRequestParcel;
import android.health.connect.aidl.UpsertMedicalResourceRequestsParcel;
import android.health.connect.backuprestore.GetChangesForBackupResponse;
import android.health.connect.backuprestore.GetSettingsForBackupResponse;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogTokenResponse;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.AggregationType;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.FhirResource;
import android.health.connect.datatypes.FhirVersion;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.Record;
import android.health.connect.exportimport.ExportImportDocumentProvider;
import android.health.connect.exportimport.IImportStatusCallback;
import android.health.connect.exportimport.IQueryDocumentProvidersCallback;
import android.health.connect.exportimport.IScheduledExportStatusCallback;
import android.health.connect.exportimport.ImportStatus;
import android.health.connect.exportimport.ScheduledExportSettings;
import android.health.connect.exportimport.ScheduledExportStatus;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.utils.InternalExternalRecordConverter;
import android.health.connect.migration.HealthConnectMigrationUiState;
import android.health.connect.migration.MigrationEntity;
import android.health.connect.migration.MigrationEntityParcel;
import android.health.connect.migration.MigrationException;
import android.health.connect.restore.StageRemoteDataException;
import android.health.connect.restore.StageRemoteDataRequest;
import android.net.Uri;
import android.os.Binder;
import android.os.OutcomeReceiver;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

import com.android.healthfitness.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * This class provides APIs to interact with the centralized HealthConnect storage maintained by the
 * system.
 *
 * <p>HealthConnect is an offline, on-device storage that unifies data from multiple devices and
 * apps into an ecosystem featuring.
 *
 * <ul>
 *   <li>APIs to insert data of various types into the system.
 * </ul>
 *
 * <p>The basic unit of data in HealthConnect is represented as a {@link Record} object, which is
 * the base class for all the other data types such as {@link
 * android.health.connect.datatypes.StepsRecord}.
 */
@SystemService(Context.HEALTHCONNECT_SERVICE)
public class HealthConnectManager {
    /**
     * Used in conjunction with {@link android.content.Intent#ACTION_VIEW_PERMISSION_USAGE} to
     * launch UI to show an app’s health permission rationale/data policy.
     *
     * <p><b>Note:</b> Used by apps to define an intent filter in conjunction with {@link
     * android.content.Intent#ACTION_VIEW_PERMISSION_USAGE} that the HC UI can link out to.
     */
    // We use intent.category prefix to be compatible with HealthPermissions strings definitions.
    @SdkConstant(SdkConstant.SdkConstantType.INTENT_CATEGORY)
    public static final String CATEGORY_HEALTH_PERMISSIONS =
            "android.intent.category.HEALTH_PERMISSIONS";

    /**
     * Activity action: Launch UI to manage (e.g. grant/revoke) health permissions.
     *
     * <p>Shows a list of apps which request at least one permission of the Health permission group.
     *
     * <p>Input: {@link android.content.Intent#EXTRA_PACKAGE_NAME} string extra with the name of the
     * app requesting the action. Optional: Adding package name extras launches a UI to manager
     * (e.g. grant/revoke) for this app.
     */
    @SdkConstant(SdkConstant.SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_HEALTH_PERMISSIONS =
            "android.health.connect.action.MANAGE_HEALTH_PERMISSIONS";

    /**
     * Activity action: Launch UI to share the route associated with an exercise session.
     *
     * <p>Input: caller must provide `String` extra EXTRA_SESSION_ID
     *
     * <p>Result will be delivered via [Activity.onActivityResult] with `ExerciseRoute`
     * EXTRA_EXERCISE_ROUTE.
     */
    @SdkConstant(SdkConstant.SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REQUEST_EXERCISE_ROUTE =
            "android.health.connect.action.REQUEST_EXERCISE_ROUTE";

    /**
     * A string ID of a session to be used with {@link #ACTION_REQUEST_EXERCISE_ROUTE}.
     *
     * <p>This is used to specify route of which exercise session we want to request.
     */
    public static final String EXTRA_SESSION_ID = "android.health.connect.extra.SESSION_ID";

    /**
     * An exercise route requested via {@link #ACTION_REQUEST_EXERCISE_ROUTE}.
     *
     * <p>This is returned for a successful request to access a route associated with an exercise
     * session.
     */
    public static final String EXTRA_EXERCISE_ROUTE = "android.health.connect.extra.EXERCISE_ROUTE";

    /**
     * Activity action: Launch UI to show and manage (e.g. grant/revoke) health permissions.
     *
     * <p>Input: {@link android.content.Intent#EXTRA_PACKAGE_NAME} string extra with the name of the
     * app requesting the action must be present. An app can open only its own page.
     *
     * <p>Input: caller must provide `String[]` extra [EXTRA_PERMISSIONS]
     *
     * <p>Result will be delivered via [Activity.onActivityResult] with `String[]`
     * [EXTRA_PERMISSIONS] and `int[]` [EXTRA_PERMISSION_GRANT_RESULTS], similar to
     * [Activity.onRequestPermissionsResult]
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstant.SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REQUEST_HEALTH_PERMISSIONS =
            "android.health.connect.action.REQUEST_HEALTH_PERMISSIONS";

    /**
     * Activity action: Launch UI to health connect home settings screen.
     *
     * <p>shows a list of recent apps that accessed (e.g. read/write) health data and allows the
     * user to access health permissions and health data.
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstant.SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_HEALTH_HOME_SETTINGS =
            "android.health.connect.action.HEALTH_HOME_SETTINGS";

    /**
     * Activity action: Launch UI to show and manage (e.g. delete/export) health data.
     *
     * <p>shows a list of health data categories and actions to manage (e.g. delete/export) health
     * data.
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstant.SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_HEALTH_DATA =
            "android.health.connect.action.MANAGE_HEALTH_DATA";

    /**
     * Activity action: Display information regarding migration - e.g. asking the user to take some
     * action (e.g. update the system) so that migration can take place.
     *
     * <p><b>Note:</b> Callers of the migration APIs must handle this intent.
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstant.SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_SHOW_MIGRATION_INFO =
            "android.health.connect.action.SHOW_MIGRATION_INFO";

    /**
     * Broadcast Action: Health Connect is ready to accept migrated data.
     *
     * <p class="note">This broadcast is explicitly sent to Health Connect migration aware
     * applications to prompt them to start/continue HC data migration. Migration aware applications
     * are those that both hold {@code android.permission.MIGRATE_HEALTH_CONNECT_DATA} and handle
     * {@code android.health.connect.action.SHOW_MIGRATION_INFO}.
     *
     * <p class="note">This is a protected intent that can only be sent by the system.
     *
     * @hide
     */
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    @SystemApi
    public static final String ACTION_HEALTH_CONNECT_MIGRATION_READY =
            "android.health.connect.action.HEALTH_CONNECT_MIGRATION_READY";

    /**
     * Unknown download state considered to be the default download state.
     *
     * <p>See also {@link #updateDataDownloadState}
     *
     * @hide
     */
    @SystemApi public static final int DATA_DOWNLOAD_STATE_UNKNOWN = 0;

    /**
     * Indicates that the download has started.
     *
     * <p>See also {@link #updateDataDownloadState}
     *
     * @hide
     */
    @SystemApi public static final int DATA_DOWNLOAD_STARTED = 1;

    /**
     * Indicates that the download is being retried.
     *
     * <p>See also {@link #updateDataDownloadState}
     *
     * @hide
     */
    @SystemApi public static final int DATA_DOWNLOAD_RETRY = 2;

    /**
     * Indicates that the download has failed.
     *
     * <p>See also {@link #updateDataDownloadState}
     *
     * @hide
     */
    @SystemApi public static final int DATA_DOWNLOAD_FAILED = 3;

    /**
     * Indicates that the download has completed.
     *
     * <p>See also {@link HealthConnectManager#updateDataDownloadState}
     *
     * @hide
     */
    @SystemApi public static final int DATA_DOWNLOAD_COMPLETE = 4;

    /**
     * Activity action: Launch activity exported by client application that handles onboarding to
     * Health Connect.
     *
     * <p>Health Connect will invoke this intent whenever the user attempts to connect an app that
     * has exported an activity that responds to this intent. The launched activity is responsible
     * for making permission requests and any other prerequisites for connecting to Health Connect.
     *
     * <p class="note">Applications exporting an activity that is launched by this intent must also
     * guard it with {@link HealthPermissions#START_ONBOARDING} so that only the system can launch
     * it.
     *
     * @hide
     */
    @SdkConstant(SdkConstant.SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_SHOW_ONBOARDING =
            "android.health.connect.action.SHOW_ONBOARDING";

    private static final String TAG = "HealthConnectManager";
    private static final String HEALTH_PERMISSION_PREFIX = "android.permission.health.";

    @SuppressWarnings("NullAway.Init") // TODO(b/317029272): fix this suppression
    private static volatile Set<String> sHealthPermissions;

    private final Context mContext;
    private final IHealthConnectService mService;
    private final InternalExternalRecordConverter mInternalExternalRecordConverter;

    /** @hide */
    HealthConnectManager(@NonNull Context context, @NonNull IHealthConnectService service) {
        mContext = context;
        mService = service;
        mInternalExternalRecordConverter = InternalExternalRecordConverter.getInstance();
    }

    /**
     * Grant a runtime permission to an application which the application does not already have. The
     * permission must have been requested by the application. If the application is not allowed to
     * hold the permission, a {@link java.lang.SecurityException} is thrown. If the package or
     * permission is invalid, a {@link java.lang.IllegalArgumentException} is thrown.
     *
     * <p><b>Note:</b> This API sets {@code PackageManager.FLAG_PERMISSION_USER_SET}.
     *
     * @hide
     */
    @RequiresPermission(MANAGE_HEALTH_PERMISSIONS)
    @UserHandleAware
    public void grantHealthPermission(@NonNull String packageName, @NonNull String permissionName) {
        try {
            mService.grantHealthPermission(packageName, permissionName, mContext.getUser());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Revoke a health permission that was previously granted by {@link
     * #grantHealthPermission(String, String)} The permission must have been requested by the
     * application. If the application is not allowed to hold the permission, a {@link
     * java.lang.SecurityException} is thrown. If the package or permission is invalid, a {@link
     * java.lang.IllegalArgumentException} is thrown.
     *
     * <p><b>Note:</b> This API sets {@code PackageManager.FLAG_PERMISSION_USER_SET} or {@code
     * PackageManager.FLAG_PERMISSION_USER_FIXED} based on the number of revocations of a particular
     * permission for a package.
     *
     * @hide
     */
    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    @RequiresPermission(MANAGE_HEALTH_PERMISSIONS)
    @UserHandleAware
    public void revokeHealthPermission(
            @NonNull String packageName, @NonNull String permissionName, @Nullable String reason) {
        try {
            mService.revokeHealthPermission(
                    packageName, permissionName, reason, mContext.getUser());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Revokes all health permissions that were previously granted by {@link
     * #grantHealthPermission(String, String)} If the package is invalid, a {@link
     * java.lang.IllegalArgumentException} is thrown.
     *
     * @hide
     */
    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    @RequiresPermission(MANAGE_HEALTH_PERMISSIONS)
    @UserHandleAware
    public void revokeAllHealthPermissions(@NonNull String packageName, @Nullable String reason) {
        try {
            mService.revokeAllHealthPermissions(packageName, reason, mContext.getUser());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns a list of health permissions that were previously granted by {@link
     * #grantHealthPermission(String, String)}.
     *
     * @hide
     */
    @RequiresPermission(MANAGE_HEALTH_PERMISSIONS)
    @UserHandleAware
    public List<String> getGrantedHealthPermissions(@NonNull String packageName) {
        try {
            return mService.getGrantedHealthPermissions(packageName, mContext.getUser());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns permission flags for the given package name and Health permissions.
     *
     * <p>This is equivalent to calling {@link PackageManager#getPermissionFlags(String, String,
     * UserHandle)} for each provided permission except it throws an exception for non-Health or
     * undeclared permissions. Flag masks listed in {@link PackageManager#MASK_PERMISSION_FLAGS_ALL}
     * can be used to check the flag values.
     *
     * <p>Returned flags for invalid, non-Health or undeclared permissions are equal to zero.
     *
     * @return a map which contains all requested permissions as keys and corresponding flags as
     *     values.
     * @throws IllegalArgumentException if the package doesn't exist, any of the permissions are not
     *     Health permissions or not declared by the app.
     * @throws NullPointerException if any of the arguments is {@code null}.
     * @throws SecurityException if the caller doesn't possess {@code
     *     android.permission.MANAGE_HEALTH_PERMISSIONS}.
     * @hide
     */
    @RequiresPermission(MANAGE_HEALTH_PERMISSIONS)
    @UserHandleAware
    public Map<String, Integer> getHealthPermissionsFlags(
            @NonNull String packageName, @NonNull List<String> permissions) {
        try {
            return mService.getHealthPermissionsFlags(packageName, mContext.getUser(), permissions);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sets/clears {@link PackageManager#FLAG_PERMISSION_USER_FIXED} for given health permissions.
     *
     * @param value whether to set or clear the flag, {@code true} means set, {@code false} - clear.
     * @throws IllegalArgumentException if the package doesn't exist, any of the permissions are not
     *     Health permissions or not declared by the app.
     * @throws NullPointerException if any of the arguments is {@code null}.
     * @throws SecurityException if the caller doesn't possess {@code
     *     android.permission.MANAGE_HEALTH_PERMISSIONS}.
     * @hide
     */
    @RequiresPermission(MANAGE_HEALTH_PERMISSIONS)
    @UserHandleAware
    public void setHealthPermissionsUserFixedFlagValue(
            @NonNull String packageName, @NonNull List<String> permissions, boolean value) {
        try {
            mService.setHealthPermissionsUserFixedFlagValue(
                    packageName, mContext.getUser(), permissions, value);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the date from which an app have access to the historical health data. Returns null if
     * the package doesn't have historical access date.
     *
     * @hide
     */
    @RequiresPermission(HealthPermissions.MANAGE_HEALTH_PERMISSIONS)
    @UserHandleAware
    @Nullable
    public Instant getHealthDataHistoricalAccessStartDate(@NonNull String packageName) {
        try {
            long dateMilli =
                    mService.getHistoricalAccessStartDateInMilliseconds(
                            packageName, mContext.getUser());
            if (dateMilli == DEFAULT_LONG) {
                return null;
            } else {
                return Instant.ofEpochMilli(dateMilli);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Inserts {@code records} into the HealthConnect database. The records returned in {@link
     * InsertRecordsResponse} contains the unique IDs of the input records. The values are in same
     * order as {@code records}. In case of an error or a permission failure the HealthConnect
     * service, {@link OutcomeReceiver#onError} will be invoked with a {@link
     * HealthConnectException}.
     *
     * @param records list of records to be inserted.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @throws RuntimeException for internal errors
     */
    public void insertRecords(
            @NonNull List<Record> records,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<InsertRecordsResponse, HealthConnectException> callback) {
        Objects.requireNonNull(records);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            // Unset any set ids for insert. This is to prevent random string ids from creating
            // illegal argument exception.
            records.forEach((record) -> record.getMetadata().setId(""));
            List<RecordInternal<?>> recordInternals =
                    records.stream()
                            .map(
                                    record ->
                                            record.toRecordInternal()
                                                    .setPackageName(mContext.getPackageName()))
                            .collect(Collectors.toList());
            mService.insertRecords(
                    mContext.getAttributionSource(),
                    new RecordsParcel(recordInternals),
                    new IInsertRecordsResponseCallback.Stub() {
                        @Override
                        public void onResult(InsertRecordsResponseParcel parcel) {
                            Binder.clearCallingIdentity();
                            executor.execute(
                                    () ->
                                            callback.onResult(
                                                    new InsertRecordsResponse(
                                                            toExternalRecordsWithUuids(
                                                                    recordInternals,
                                                                    parcel.getUids()))));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get aggregations corresponding to {@code request}.
     *
     * @param <T> Result type of the aggregation.
     *     <p>Note:
     *     <p>This type is embedded in the {@link AggregationType} as {@link AggregationType} are
     *     typed in nature.
     *     <p>Only {@link AggregationType}s that are of same type T can be queried together
     * @param request request for different aggregation.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @see AggregateRecordsResponse#get
     */
    @SuppressWarnings("unchecked")
    public <T> void aggregate(
            @NonNull AggregateRecordsRequest<T> request,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull
                    OutcomeReceiver<AggregateRecordsResponse<T>, HealthConnectException> callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.aggregateRecords(
                    mContext.getAttributionSource(),
                    new AggregateDataRequestParcel(request),
                    new IAggregateRecordsResponseCallback.Stub() {
                        @Override
                        public void onResult(AggregateDataResponseParcel parcel) {
                            Binder.clearCallingIdentity();
                            try {
                                executor.execute(
                                        () ->
                                                callback.onResult(
                                                        (AggregateRecordsResponse<T>)
                                                                parcel.getAggregateDataResponse()));
                            } catch (Exception exception) {
                                callback.onError(
                                        new HealthConnectException(
                                                HealthConnectException.ERROR_INTERNAL));
                            }
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (ClassCastException classCastException) {
            returnError(
                    executor,
                    new HealthConnectExceptionParcel(
                            new HealthConnectException(HealthConnectException.ERROR_INTERNAL)),
                    callback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get aggregations corresponding to {@code request}. Use this API if results are to be grouped
     * by concrete intervals of time, for example 5 Hrs, 10 Hrs etc.
     *
     * @param <T> Result type of the aggregation.
     *     <p>Note:
     *     <p>This type is embedded in the {@link AggregationType} as {@link AggregationType} are
     *     typed in nature.
     *     <p>Only {@link AggregationType}s that are of same type T can be queried together
     * @param request request for different aggregation.
     * @param duration Duration on which to group by results
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @see HealthConnectManager#aggregateGroupByPeriod
     */
    @SuppressWarnings("unchecked")
    public <T> void aggregateGroupByDuration(
            @NonNull AggregateRecordsRequest<T> request,
            @NonNull Duration duration,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull
                    OutcomeReceiver<
                                    List<AggregateRecordsGroupedByDurationResponse<T>>,
                                    HealthConnectException>
                            callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(duration);
        if (duration.toMillis() < 1) {
            throw new IllegalArgumentException("Duration should be at least 1 millisecond");
        }
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.aggregateRecords(
                    mContext.getAttributionSource(),
                    new AggregateDataRequestParcel(request, duration),
                    new IAggregateRecordsResponseCallback.Stub() {
                        @Override
                        public void onResult(AggregateDataResponseParcel parcel) {
                            Binder.clearCallingIdentity();
                            List<AggregateRecordsGroupedByDurationResponse<T>> result =
                                    new ArrayList<>();
                            for (AggregateRecordsGroupedByDurationResponse<?>
                                    aggregateRecordsGroupedByDurationResponse :
                                            parcel.getAggregateDataResponseGroupedByDuration()) {
                                result.add(
                                        (AggregateRecordsGroupedByDurationResponse<T>)
                                                aggregateRecordsGroupedByDurationResponse);
                            }
                            executor.execute(() -> callback.onResult(result));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (ClassCastException classCastException) {
            returnError(
                    executor,
                    new HealthConnectExceptionParcel(
                            new HealthConnectException(HealthConnectException.ERROR_INTERNAL)),
                    callback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get aggregations corresponding to {@code request}. Use this API if results are to be grouped
     * by number of days. This API handles changes in {@link ZoneOffset} when computing the data on
     * a per-day basis.
     *
     * @param <T> Result type of the aggregation.
     *     <p>Note:
     *     <p>This type is embedded in the {@link AggregationType} as {@link AggregationType} are
     *     typed in nature.
     *     <p>Only {@link AggregationType}s that are of same type T can be queried together
     * @param request Request for different aggregation.
     * @param period Period on which to group by results
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @see AggregateRecordsGroupedByPeriodResponse#get
     * @see HealthConnectManager#aggregateGroupByDuration
     */
    @SuppressWarnings("unchecked")
    public <T> void aggregateGroupByPeriod(
            @NonNull AggregateRecordsRequest<T> request,
            @NonNull Period period,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull
                    OutcomeReceiver<
                                    List<AggregateRecordsGroupedByPeriodResponse<T>>,
                                    HealthConnectException>
                            callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(period);
        if (period == Period.ZERO) {
            throw new IllegalArgumentException("Period duration should be at least a day");
        }
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.aggregateRecords(
                    mContext.getAttributionSource(),
                    new AggregateDataRequestParcel(request, period),
                    new IAggregateRecordsResponseCallback.Stub() {
                        @Override
                        public void onResult(AggregateDataResponseParcel parcel) {
                            Binder.clearCallingIdentity();
                            List<AggregateRecordsGroupedByPeriodResponse<T>> result =
                                    new ArrayList<>();
                            for (AggregateRecordsGroupedByPeriodResponse<?>
                                    aggregateRecordsGroupedByPeriodResponse :
                                            parcel.getAggregateDataResponseGroupedByPeriod()) {
                                result.add(
                                        (AggregateRecordsGroupedByPeriodResponse<T>)
                                                aggregateRecordsGroupedByPeriodResponse);
                            }

                            executor.execute(() -> callback.onResult(result));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (ClassCastException classCastException) {
            returnError(
                    executor,
                    new HealthConnectExceptionParcel(
                            new HealthConnectException(HealthConnectException.ERROR_INTERNAL)),
                    callback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Deletes records based on the {@link DeleteUsingFiltersRequest}. This is only to be used by
     * health connect controller APK(s). Ids that don't exist will be ignored.
     *
     * @param request Request based on which to perform delete operation
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @hide
     */
    @SystemApi
    @RequiresPermission(MANAGE_HEALTH_PERMISSIONS)
    public void deleteRecords(
            @NonNull DeleteUsingFiltersRequest request,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Void, HealthConnectException> callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.deleteUsingFilters(
                    mContext.getAttributionSource(),
                    new DeleteUsingFiltersRequestParcel(request),
                    new IEmptyResponseCallback.Stub() {
                        @Override
                        public void onResult() {
                            executor.execute(() -> callback.onResult(null));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException remoteException) {
            remoteException.rethrowFromSystemServer();
        }
    }

    /**
     * Deletes records based on {@link RecordIdFilter}.
     *
     * <p>Deletions are performed in a transaction i.e. either all will be deleted or none
     *
     * @param recordIds recordIds on which to perform delete operation.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @throws IllegalArgumentException if {@code recordIds is empty}
     */
    public void deleteRecords(
            @NonNull List<RecordIdFilter> recordIds,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Void, HealthConnectException> callback) {
        Objects.requireNonNull(recordIds);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        if (recordIds.isEmpty()) {
            throw new IllegalArgumentException("record ids can't be empty");
        }

        try {
            mService.deleteUsingFiltersForSelf(
                    mContext.getAttributionSource(),
                    new DeleteUsingFiltersRequestParcel(
                            new RecordIdFiltersParcel(recordIds), mContext.getPackageName()),
                    new IEmptyResponseCallback.Stub() {
                        @Override
                        public void onResult() {
                            executor.execute(() -> callback.onResult(null));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException remoteException) {
            remoteException.rethrowFromSystemServer();
        }
    }

    /**
     * Deletes records based on the {@link TimeRangeFilter}.
     *
     * <p>Deletions are performed in a transaction i.e. either all will be deleted or none
     *
     * @param recordType recordType to perform delete operation on.
     * @param timeRangeFilter time filter based on which to delete the records.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     */
    public void deleteRecords(
            @NonNull Class<? extends Record> recordType,
            @NonNull TimeRangeFilter timeRangeFilter,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Void, HealthConnectException> callback) {
        Objects.requireNonNull(recordType);
        Objects.requireNonNull(timeRangeFilter);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.deleteUsingFiltersForSelf(
                    mContext.getAttributionSource(),
                    new DeleteUsingFiltersRequestParcel(
                            new DeleteUsingFiltersRequest.Builder()
                                    .addDataOrigin(
                                            new DataOrigin.Builder()
                                                    .setPackageName(mContext.getPackageName())
                                                    .build())
                                    .addRecordType(recordType)
                                    .setTimeRangeFilter(timeRangeFilter)
                                    .build()),
                    new IEmptyResponseCallback.Stub() {
                        @Override
                        public void onResult() {
                            executor.execute(() -> callback.onResult(null));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException remoteException) {
            remoteException.rethrowFromSystemServer();
        }
    }

    /**
     * Get change logs post the time when {@code token} was generated.
     *
     * @param changeLogsRequest The token from {@link HealthConnectManager#getChangeLogToken}.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @see HealthConnectManager#getChangeLogToken
     */
    public void getChangeLogs(
            @NonNull ChangeLogsRequest changeLogsRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<ChangeLogsResponse, HealthConnectException> callback) {
        Objects.requireNonNull(changeLogsRequest);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.getChangeLogs(
                    mContext.getAttributionSource(),
                    changeLogsRequest,
                    new IChangeLogsResponseCallback.Stub() {
                        @Override
                        public void onResult(ChangeLogsResponse parcel) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onResult(parcel));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (ClassCastException invalidArgumentException) {
            callback.onError(
                    new HealthConnectException(
                            HealthConnectException.ERROR_INVALID_ARGUMENT,
                            invalidArgumentException.getMessage()));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Get token for {HealthConnectManager#getChangeLogs}. Changelogs requested corresponding to
     * this token will be post the time this token was generated by the system all items that match
     * the given filters.
     *
     * <p>Tokens from this request are to be passed to {HealthConnectManager#getChangeLogs}
     *
     * @param request A request to get changelog token
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     */
    public void getChangeLogToken(
            @NonNull ChangeLogTokenRequest request,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<ChangeLogTokenResponse, HealthConnectException> callback) {
        try {
            mService.getChangeLogToken(
                    mContext.getAttributionSource(),
                    request,
                    new IGetChangeLogTokenCallback.Stub() {
                        @Override
                        public void onResult(ChangeLogTokenResponse parcel) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onResult(parcel));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Fetch the data priority order of the contributing {@link DataOrigin} for {@code
     * dataCategory}.
     *
     * @param dataCategory {@link HealthDataCategory} for which to get the priority order
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @hide
     */
    @SystemApi
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    public void fetchDataOriginsPriorityOrder(
            @HealthDataCategory.Type int dataCategory,
            @NonNull Executor executor,
            @NonNull
                    OutcomeReceiver<FetchDataOriginsPriorityOrderResponse, HealthConnectException>
                            callback) {
        try {
            mService.getCurrentPriority(
                    dataCategory,
                    new IGetPriorityResponseCallback.Stub() {
                        @Override
                        public void onResult(GetPriorityResponseParcel response) {
                            Binder.clearCallingIdentity();
                            executor.execute(
                                    () -> callback.onResult(response.getPriorityResponse()));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Updates the priority order of the apps as per {@code request}
     *
     * @param request new priority order update request
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @hide
     */
    @SystemApi
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    public void updateDataOriginPriorityOrder(
            @NonNull UpdateDataOriginPriorityOrderRequest request,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Void, HealthConnectException> callback) {
        try {
            mService.updatePriority(
                    new UpdatePriorityRequestParcel(request),
                    new IEmptyResponseCallback.Stub() {
                        @Override
                        public void onResult() {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onResult(null));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Retrieves {@link RecordTypeInfoResponse} for each RecordType.
     *
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @hide
     */
    @SystemApi
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    public void queryAllRecordTypesInfo(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull
                    OutcomeReceiver<
                                    Map<Class<? extends Record>, RecordTypeInfoResponse>,
                                    HealthConnectException>
                            callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.queryAllRecordTypesInfo(
                    new IRecordTypeInfoResponseCallback.Stub() {
                        @Override
                        public void onResult(RecordTypeInfoResponseParcel parcel) {
                            Binder.clearCallingIdentity();
                            executor.execute(
                                    () -> callback.onResult(parcel.getRecordTypeInfoResponses()));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns currently set auto delete period for this user.
     *
     * <p>If you are calling this function for the first time after a user unlock, this might take
     * some time so consider calling this on a thread.
     *
     * @return Auto delete period in days, 0 is returned if auto delete period is not set.
     * @throws RuntimeException for internal errors
     * @hide
     */
    @SystemApi
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    @IntRange(from = 0, to = 7300)
    public int getRecordRetentionPeriodInDays() {
        try {
            return mService.getRecordRetentionPeriodInDays(mContext.getUser());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sets auto delete period (for all the records to be automatically deleted) for this user.
     *
     * <p>Note: The max value of auto delete period can be 7300 i.e. ~20 years
     *
     * @param days Auto period to be set in days. Use 0 to unset this value.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @throws RuntimeException for internal errors
     * @throws IllegalArgumentException if {@code days} is not between 0 and 7300
     * @hide
     */
    @SystemApi
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    public void setRecordRetentionPeriodInDays(
            @IntRange(from = 0, to = 7300) int days,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Void, HealthConnectException> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        if (days < 0 || days > 7300) {
            throw new IllegalArgumentException("days should be between " + 0 + " and " + 7300);
        }

        try {
            mService.setRecordRetentionPeriodInDays(
                    days,
                    mContext.getUser(),
                    new IEmptyResponseCallback.Stub() {
                        @Override
                        public void onResult() {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onResult(null));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns a list of access logs with package name and its access time for each record type.
     *
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @hide
     */
    @SystemApi
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    public void queryAccessLogs(
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<List<AccessLog>, HealthConnectException> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.queryAccessLogs(
                    mContext.getPackageName(),
                    new IAccessLogsResponseCallback.Stub() {
                        @Override
                        public void onResult(AccessLogsResponseParcel parcel) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onResult(parcel.getAccessLogs()));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * API to read records based on {@link ReadRecordsRequestUsingFilters} or {@link
     * ReadRecordsRequestUsingIds}
     *
     * <p>Number of records returned by this API will depend based on below factors:
     *
     * <p>When an app with read permission allowed calls the API from background then it will be
     * able to read only its own inserted records and will not get records inserted by other apps.
     * This may be less than the total records present for the record type.
     *
     * <p>When an app with read permission allowed calls the API from foreground then it will be
     * able to read all records for the record type.
     *
     * <p>App with only write permission but no read permission allowed will be able to read only
     * its own inserted records both when in foreground or background.
     *
     * <p>An app without both read and write permissions will not be able to read any record and the
     * API will throw Security Exception.
     *
     * @param request Read request based on {@link ReadRecordsRequestUsingFilters} or {@link
     *     ReadRecordsRequestUsingIds}
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @throws IllegalArgumentException if request page size set is more than 5000 in {@link
     *     ReadRecordsRequestUsingFilters}
     * @throws SecurityException if app without read or write permission tries to read.
     */
    public <T extends Record> void readRecords(
            @NonNull ReadRecordsRequest<T> request,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<ReadRecordsResponse<T>, HealthConnectException> callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.readRecords(
                    mContext.getAttributionSource(),
                    request.toReadRecordsRequestParcel(),
                    getReadCallback(executor, callback));
        } catch (RemoteException remoteException) {
            remoteException.rethrowFromSystemServer();
        }
    }

    /**
     * Updates {@code records} into the HealthConnect database. In case of an error or a permission
     * failure the HealthConnect service, {@link OutcomeReceiver#onError} will be invoked with a
     * {@link HealthConnectException}.
     *
     * <p>In case the input record to be updated does not exist in the database or the caller is not
     * the owner of the record then {@link HealthConnectException#ERROR_INVALID_ARGUMENT} will be
     * thrown.
     *
     * @param records list of records to be updated.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @throws IllegalArgumentException if at least one of the records is missing both
     *     ClientRecordID and UUID.
     */
    public void updateRecords(
            @NonNull List<Record> records,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Void, HealthConnectException> callback) {
        Objects.requireNonNull(records);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            List<RecordInternal<?>> recordInternals =
                    records.stream().map(Record::toRecordInternal).collect(Collectors.toList());
            // Verify if the input record has clientRecordId or UUID.
            for (RecordInternal<?> recordInternal : recordInternals) {
                if ((recordInternal.getClientRecordId() == null
                                || recordInternal.getClientRecordId().isEmpty())
                        && recordInternal.getUuid() == null) {
                    throw new IllegalArgumentException(
                            "At least one of the records is missing both ClientRecordID"
                                    + " and UUID. RecordType of the input: "
                                    + recordInternal.getRecordType());
                }
            }

            mService.updateRecords(
                    mContext.getAttributionSource(),
                    new RecordsParcel(recordInternals),
                    new IEmptyResponseCallback.Stub() {
                        @Override
                        public void onResult() {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onResult(null));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            Binder.clearCallingIdentity();
                            callback.onError(exception.getHealthConnectException());
                        }
                    });
        } catch (ArithmeticException
                | ClassCastException
                | IllegalArgumentException invalidArgumentException) {
            throw new IllegalArgumentException(invalidArgumentException);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns information, represented by {@code ApplicationInfoResponse}, for all the packages
     * that have contributed to the health connect DB. If the application is does not have
     * permissions to query other packages, a {@link java.lang.SecurityException} is thrown.
     *
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @hide
     */
    @SystemApi
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    public void getContributorApplicationsInfo(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<ApplicationInfoResponse, HealthConnectException> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.getContributorApplicationsInfo(
                    new IApplicationInfoResponseCallback.Stub() {
                        @Override
                        public void onResult(ApplicationInfoResponseParcel parcel) {
                            Binder.clearCallingIdentity();
                            executor.execute(
                                    () ->
                                            callback.onResult(
                                                    new ApplicationInfoResponse(
                                                            parcel.getAppInfoList())));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });

        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Stages all HealthConnect remote data and returns any errors in a callback. Errors encountered
     * for all the files are shared in the provided callback. Any authorization / permissions
     * related error is reported to the callback with an empty file name.
     *
     * <p>The staged data will later be restored (integrated) into the existing Health Connect data.
     * Any existing data will not be affected by the staged data.
     *
     * <p>The file names passed should be the same as the ones on the original device that were
     * backed up or are being transferred directly.
     *
     * <p>If a file already exists in the staged data then it will be replaced. However, note that
     * staging data is a one time process. And if the staged data has already been processed then
     * any attempt to stage data again will be silently ignored.
     *
     * <p>The caller is responsible for closing the original file descriptors. The file descriptors
     * are duplicated and the originals may be closed by the application at any time after this API
     * returns.
     *
     * <p>The caller should update the data download states using {@link #updateDataDownloadState}
     * before calling this API.
     *
     * @param pfdsByFileName The map of file names and their {@link ParcelFileDescriptor}s.
     * @param executor The {@link Executor} on which to invoke the callback.
     * @param callback The callback which will receive the outcome of this call.
     * @hide
     */
    @SystemApi
    @UserHandleAware
    @RequiresPermission(Manifest.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA)
    public void stageAllHealthConnectRemoteData(
            @NonNull Map<String, ParcelFileDescriptor> pfdsByFileName,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Void, StageRemoteDataException> callback)
            throws NullPointerException {
        Objects.requireNonNull(pfdsByFileName);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.stageAllHealthConnectRemoteData(
                    new StageRemoteDataRequest(pfdsByFileName),
                    mContext.getUser(),
                    new IDataStagingFinishedCallback.Stub() {
                        @Override
                        public void onResult() {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onResult(null));
                        }

                        @Override
                        public void onError(StageRemoteDataException stageRemoteDataException) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onError(stageRemoteDataException));
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Copies all HealthConnect backup data in the passed FDs.
     *
     * <p>The shared data must later be sent for Backup to cloud or another device.
     *
     * <p>We are responsible for closing the original file descriptors. The caller must not close
     * the FD before that.
     *
     * @param pfdsByFileName The map of file names and their {@link ParcelFileDescriptor}s.
     * @hide
     */
    public void getAllDataForBackup(@NonNull Map<String, ParcelFileDescriptor> pfdsByFileName) {
        Objects.requireNonNull(pfdsByFileName);

        try {
            mService.getAllDataForBackup(
                    new StageRemoteDataRequest(pfdsByFileName), mContext.getUser());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the names of all HealthConnect Backup files
     *
     * @hide
     */
    public Set<String> getAllBackupFileNames(boolean forDeviceToDevice) {
        try {
            return mService.getAllBackupFileNames(forDeviceToDevice).getFileNames();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Deletes all previously staged HealthConnect data from the disk. For testing purposes only.
     *
     * <p>This deletes only the staged data leaving any other Health Connect data untouched.
     *
     * @hide
     */
    @TestApi
    @UserHandleAware
    public void deleteAllStagedRemoteData() throws NullPointerException {
        try {
            mService.deleteAllStagedRemoteData(mContext.getUser());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Allows setting lower rate limits in tests.
     *
     * @hide
     */
    @TestApi
    public void setLowerRateLimitsForTesting(boolean enabled) throws NullPointerException {
        try {
            mService.setLowerRateLimitsForTesting(enabled);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Updates the download state of the Health Connect data.
     *
     * <p>The data should've been downloaded and the corresponding download states updated before
     * the app calls {@link #stageAllHealthConnectRemoteData}. Once {@link
     * #stageAllHealthConnectRemoteData} has been called the downloaded state becomes {@link
     * #DATA_DOWNLOAD_COMPLETE} and future attempts to update the download state are ignored.
     *
     * <p>The only valid order of state transition are:
     *
     * <ul>
     *   <li>{@link #DATA_DOWNLOAD_STARTED} to {@link #DATA_DOWNLOAD_COMPLETE}
     *   <li>{@link #DATA_DOWNLOAD_STARTED} to {@link #DATA_DOWNLOAD_RETRY} to {@link
     *       #DATA_DOWNLOAD_COMPLETE}
     *   <li>{@link #DATA_DOWNLOAD_STARTED} to {@link #DATA_DOWNLOAD_FAILED}
     *   <li>{@link #DATA_DOWNLOAD_STARTED} to {@link #DATA_DOWNLOAD_RETRY} to {@link
     *       #DATA_DOWNLOAD_FAILED}
     * </ul>
     *
     * <p>Note that it's okay if some states are missing in of the sequences above but the order has
     * to be one of the above.
     *
     * <p>Only one app will have the permission to call this API so it is assured that no one else
     * will be able to update this state.
     *
     * @param downloadState The download state which needs to be purely from {@link
     *     DataDownloadState}
     * @hide
     */
    @SystemApi
    @UserHandleAware
    @RequiresPermission(Manifest.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA)
    public void updateDataDownloadState(@DataDownloadState int downloadState) {
        try {
            mService.updateDataDownloadState(downloadState);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Asynchronously returns the current UI state of Health Connect as it goes through the
     * Data-Migration process. In case there was an error reading the data on the disk the error
     * will be returned in the callback.
     *
     * <p>See also {@link HealthConnectMigrationUiState} object describing the HealthConnect UI
     * state.
     *
     * @param executor The {@link Executor} on which to invoke the callback.
     * @param callback The callback which will receive the current {@link
     *     HealthConnectMigrationUiState} or the {@link HealthConnectException}.
     * @hide
     */
    @UserHandleAware
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    public void getHealthConnectMigrationUiState(
            @NonNull Executor executor,
            @NonNull
                    OutcomeReceiver<HealthConnectMigrationUiState, HealthConnectException>
                            callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.getHealthConnectMigrationUiState(
                    new IGetHealthConnectMigrationUiStateCallback.Stub() {
                        @Override
                        public void onResult(HealthConnectMigrationUiState migrationUiState) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onResult(migrationUiState));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            Binder.clearCallingIdentity();
                            executor.execute(
                                    () -> callback.onError(exception.getHealthConnectException()));
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Asynchronously returns the current state of the Health Connect data as it goes through the
     * Data-Restore and/or the Data-Migration process. In case there was an error reading the data
     * on the disk the error will be returned in the callback.
     *
     * <p>See also {@link HealthConnectDataState} object describing the HealthConnect state.
     *
     * @param executor The {@link Executor} on which to invoke the callback.
     * @param callback The callback which will receive the current {@link HealthConnectDataState} or
     *     the {@link HealthConnectException}.
     * @hide
     */
    @SystemApi
    @UserHandleAware
    @RequiresPermission(
            anyOf = {
                MANAGE_HEALTH_DATA_PERMISSION,
                Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA
            })
    public void getHealthConnectDataState(
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<HealthConnectDataState, HealthConnectException> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.getHealthConnectDataState(
                    new IGetHealthConnectDataStateCallback.Stub() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onResult(healthConnectDataState));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            Binder.clearCallingIdentity();
                            executor.execute(
                                    () -> callback.onError(exception.getHealthConnectException()));
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns a list of unique dates for which the DB has at least one entry.
     *
     * @param recordTypes List of record types classes for which to get the activity dates.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @throws java.lang.IllegalArgumentException If the record types list is empty.
     * @hide
     */
    @SystemApi
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    public void queryActivityDates(
            @NonNull List<Class<? extends Record>> recordTypes,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<List<LocalDate>, HealthConnectException> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        Objects.requireNonNull(recordTypes);

        if (recordTypes.isEmpty()) {
            throw new IllegalArgumentException("Record types list can not be empty");
        }

        try {
            mService.getActivityDates(
                    new ActivityDatesRequestParcel(recordTypes),
                    new IActivityDatesResponseCallback.Stub() {
                        @Override
                        public void onResult(ActivityDatesResponseParcel parcel) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onResult(parcel.getDates()));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });

        } catch (RemoteException exception) {
            exception.rethrowFromSystemServer();
        }
    }

    /**
     * Marks the start of the migration and block API calls.
     *
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @hide
     */
    @RequiresPermission(Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA)
    @SystemApi
    public void startMigration(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Void, MigrationException> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.startMigration(
                    mContext.getPackageName(), wrapMigrationCallback(executor, callback));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Marks the end of the migration.
     *
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @hide
     */
    @RequiresPermission(Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA)
    @SystemApi
    public void finishMigration(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Void, MigrationException> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.finishMigration(
                    mContext.getPackageName(), wrapMigrationCallback(executor, callback));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Writes data to the module database.
     *
     * @param entities List of {@link MigrationEntity} to migrate.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @hide
     */
    @RequiresPermission(Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA)
    @SystemApi
    public void writeMigrationData(
            @NonNull List<MigrationEntity> entities,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Void, MigrationException> callback) {

        Objects.requireNonNull(entities);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.writeMigrationData(
                    mContext.getPackageName(),
                    new MigrationEntityParcel(entities),
                    wrapMigrationCallback(executor, callback));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sets the minimum version on which the module will inform the migrator package of its
     * migration readiness.
     *
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA)
    public void insertMinDataMigrationSdkExtensionVersion(
            int requiredSdkExtension,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Void, MigrationException> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.insertMinDataMigrationSdkExtensionVersion(
                    mContext.getPackageName(),
                    requiredSdkExtension,
                    wrapMigrationCallback(executor, callback));

        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Configures the settings for the scheduled export of Health Connect data.
     *
     * @param settings Settings to use for the scheduled export. Use null to clear the settings.
     * @throws RuntimeException for internal errors
     * @hide
     */
    @SuppressWarnings("NullAway") // TODO: b/178748627 - fix this suppression.
    @WorkerThread
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    public void configureScheduledExport(@Nullable ScheduledExportSettings settings) {
        try {
            mService.configureScheduledExport(settings, mContext.getUser());
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    /**
     * Queries the status of a scheduled export.
     *
     * @throws RuntimeException for internal errors
     * @hide
     */
    @WorkerThread
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    public void getScheduledExportStatus(
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<ScheduledExportStatus, HealthConnectException> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.getScheduledExportStatus(
                    mContext.getUser(),
                    new IScheduledExportStatusCallback.Stub() {
                        @Override
                        public void onResult(ScheduledExportStatus status) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onResult(status));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    /**
     * Queries the status of a data import.
     *
     * @throws RuntimeException for internal errors
     * @hide
     */
    @WorkerThread
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    public void getImportStatus(
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<ImportStatus, HealthConnectException> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.getImportStatus(
                    mContext.getUser(),
                    new IImportStatusCallback.Stub() {
                        @Override
                        public void onResult(ImportStatus status) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onResult(status));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    /**
     * Imports the given compressed database file.
     *
     * @throws RuntimeException for internal errors
     * @hide
     */
    @WorkerThread
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    public void runImport(
            @NonNull Uri file,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Void, HealthConnectException> callback) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.runImport(
                    mContext.getUser(),
                    file,
                    new IEmptyResponseCallback.Stub() {
                        @Override
                        public void onResult() {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onResult(null));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    /**
     * Triggers an immediate export of health connect data.
     *
     * @throws RuntimeException for internal errors
     * @hide
     */
    @WorkerThread
    @FlaggedApi(FLAG_IMMEDIATE_EXPORT)
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    public void runImmediateExport(
            @NonNull Uri file,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Void, HealthConnectException> callback) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.runImmediateExport(
                    file,
                    new IEmptyResponseCallback.Stub() {
                        @Override
                        public void onResult() {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onResult(null));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns currently set period between scheduled exports for this user.
     *
     * <p>If you are calling this function for the first time after a user unlock, this might take
     * some time so consider calling this on a thread.
     *
     * @return Period between scheduled exports in days, 0 is returned if period between scheduled
     *     exports is not set.
     * @throws RuntimeException for internal errors
     * @hide
     */
    @WorkerThread
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    @IntRange(from = 0, to = 30)
    public int getScheduledExportPeriodInDays() {
        try {
            return mService.getScheduledExportPeriodInDays(mContext.getUser());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Queries the document providers available to be used for export/import.
     *
     * @throws RuntimeException for internal errors
     * @hide
     */
    @WorkerThread
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    public void queryDocumentProviders(
            @NonNull Executor executor,
            @NonNull
                    OutcomeReceiver<List<ExportImportDocumentProvider>, HealthConnectException>
                            callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.queryDocumentProviders(
                    mContext.getUser(),
                    new IQueryDocumentProvidersCallback.Stub() {
                        @Override
                        public void onResult(List<ExportImportDocumentProvider> providers) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onResult(providers));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Record> IReadRecordsResponseCallback.Stub getReadCallback(
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<ReadRecordsResponse<T>, HealthConnectException> callback) {
        return new IReadRecordsResponseCallback.Stub() {
            @Override
            public void onResult(ReadRecordsResponseParcel parcel) {
                Binder.clearCallingIdentity();
                try {
                    List<T> externalRecords =
                            (List<T>)
                                    mInternalExternalRecordConverter.getExternalRecords(
                                            parcel.getRecordsParcel().getRecords());
                    executor.execute(
                            () ->
                                    callback.onResult(
                                            new ReadRecordsResponse<>(
                                                    externalRecords, parcel.getPageToken())));
                } catch (ClassCastException castException) {
                    HealthConnectException healthConnectException =
                            new HealthConnectException(
                                    HealthConnectException.ERROR_INTERNAL,
                                    castException.getMessage());
                    returnError(
                            executor,
                            new HealthConnectExceptionParcel(healthConnectException),
                            callback);
                }
            }

            @Override
            public void onError(HealthConnectExceptionParcel exception) {
                returnError(executor, exception, callback);
            }
        };
    }

    private List<Record> toExternalRecordsWithUuids(
            List<RecordInternal<?>> recordInternals, List<String> uuids) {
        int i = 0;
        List<Record> records = new ArrayList<>();

        for (RecordInternal<?> recordInternal : recordInternals) {
            recordInternal.setUuid(uuids.get(i++));
            records.add(recordInternal.toExternalRecord());
        }

        return records;
    }

    private static <RES, ERR extends Throwable> void returnResult(
            Executor executor, @Nullable RES result, OutcomeReceiver<RES, ERR> callback) {
        Binder.clearCallingIdentity();
        executor.execute(() -> callback.onResult(result));
    }

    private void returnError(
            Executor executor,
            HealthConnectExceptionParcel exception,
            OutcomeReceiver<?, HealthConnectException> callback) {
        Binder.clearCallingIdentity();
        executor.execute(() -> callback.onError(exception.getHealthConnectException()));
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        DATA_DOWNLOAD_STATE_UNKNOWN,
        DATA_DOWNLOAD_STARTED,
        DATA_DOWNLOAD_RETRY,
        DATA_DOWNLOAD_FAILED,
        DATA_DOWNLOAD_COMPLETE
    })
    public @interface DataDownloadState {}

    /**
     * Returns {@code true} if the given permission protects access to health connect data.
     *
     * @hide
     */
    @SystemApi
    public static boolean isHealthPermission(
            @NonNull Context context, @NonNull final String permission) {
        if (!permission.startsWith(HEALTH_PERMISSION_PREFIX)) {
            return false;
        }
        return getHealthPermissions(context).contains(permission);
    }

    /**
     * Returns an <b>immutable</b> set of health permissions defined within the module and belonging
     * to {@link android.health.connect.HealthPermissions#HEALTH_PERMISSION_GROUP}.
     *
     * <p><b>Note:</b> If we, for some reason, fail to retrieve these, we return an empty set rather
     * than crashing the device. This means the health permissions infra will be inactive.
     *
     * @hide
     */
    @NonNull
    @SystemApi
    public static Set<String> getHealthPermissions(@NonNull Context context) {
        if (sHealthPermissions != null) {
            return sHealthPermissions;
        }

        PackageInfo packageInfo;
        try {
            final PackageManager pm = context.getApplicationContext().getPackageManager();
            final PermissionGroupInfo permGroupInfo =
                    pm.getPermissionGroupInfo(
                            android.health.connect.HealthPermissions.HEALTH_PERMISSION_GROUP,
                            /* flags= */ 0);
            packageInfo =
                    pm.getPackageInfo(
                            permGroupInfo.packageName,
                            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "Health permission group or HC package not found", ex);
            sHealthPermissions = Collections.emptySet();
            return sHealthPermissions;
        }

        Set<String> permissions = new HashSet<>();
        for (PermissionInfo perm : packageInfo.permissions) {
            if (HealthPermissions.isValidHealthPermission(perm)) {
                permissions.add(perm.name);
            }
        }
        sHealthPermissions = Collections.unmodifiableSet(permissions);
        return sHealthPermissions;
    }

    @NonNull
    private static IMigrationCallback wrapMigrationCallback(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<Void, MigrationException> callback) {
        return new IMigrationCallback.Stub() {
            @Override
            public void onSuccess() {
                Binder.clearCallingIdentity();
                executor.execute(() -> callback.onResult(null));
            }

            @Override
            public void onError(MigrationException exception) {
                Binder.clearCallingIdentity();
                executor.execute(() -> callback.onError(exception));
            }
        };
    }

    /**
     * Inserts or updates a list of {@link MedicalResource}s into the HealthConnect database using
     * {@link UpsertMedicalResourceRequest}.
     *
     * <p>For each {@link UpsertMedicalResourceRequest}, one {@link MedicalResource} will be
     * returned. The returned list of {@link MedicalResource}s will be in the same order as the
     * {@code requests}.
     *
     * <p>Note that a {@link MedicalDataSource} needs to be created using {@link
     * #createMedicalDataSource} before any {@link MedicalResource}s can be upserted for this
     * source.
     *
     * <p>Medical data is represented using the <a href="https://hl7.org/fhir/">Fast Healthcare
     * Interoperability Resources (FHIR)</a> standard. The FHIR resource provided in {@link
     * UpsertMedicalResourceRequest#getData()} is expected to be valid FHIR in JSON representation
     * for the specified {@link UpsertMedicalResourceRequest#getFhirVersion()} according to the <a
     * href="https://hl7.org/fhir/resourcelist.html">FHIR spec</a>. Structural validation checks
     * such as resource structure, field types and presence of required fields are performed, but
     * these checks may not cover all FHIR spec requirements and may change in future versions.
     *
     * <p>Data written to Health Connect should be for a single individual only. However, the API
     * allows for multiple Patient resources to be written to account for the possibility of
     * multiple Patient resources being present in one individual's medical record.
     *
     * <p>Each {@link UpsertMedicalResourceRequest} also has to meet the following requirements.
     *
     * <ul>
     *   <li>The FHIR resource contains an "id" and "resourceType" field.
     *   <li>The FHIR resource type is in our accepted list of resource types. See {@link
     *       FhirResource} for the accepted types.
     *   <li>The FHIR resource does not contain any "contained" resources.
     *   <li>The resource can be mapped to one of the READ_MEDICAL_DATA_ {@link HealthPermissions}
     *       categories.
     *   <li>The {@link UpsertMedicalResourceRequest#getDataSourceId()} is valid.
     *   <li>The {@link UpsertMedicalResourceRequest#getFhirVersion()} matches the {@link
     *       FhirVersion} of the {@link MedicalDataSource}.
     * </ul>
     *
     * <p>If any request contains invalid {@link MedicalDataSource} IDs, the API will throw an
     * {@link IllegalArgumentException}, and none of the {@code requests} will be upserted into the
     * HealthConnect database.
     *
     * <p>If any request is deemed invalid for any other reasons, the caller will receive an
     * exception with code {@link HealthConnectException#ERROR_INVALID_ARGUMENT} via {@code
     * callback.onError()}, and none of the {@code requests} will be upserted into the HealthConnect
     * database.
     *
     * <p>If data for any {@link UpsertMedicalResourceRequest} fails to be upserted, then no data
     * from any {@code requests} will be upserted into the database.
     *
     * <p>The uniqueness of each request is calculated comparing the combination of {@link
     * UpsertMedicalResourceRequest#getDataSourceId() data source id}, FHIR resource type and FHIR
     * resource ID extracted from the provided {@link UpsertMedicalResourceRequest#getData() data}.
     * If the above combination does not match with an existing one in Health Connect, then a new
     * {@link MedicalResource} is inserted, otherwise the existing one is updated.
     *
     * @param requests List of upsert requests.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @throws IllegalArgumentException if any {@code requests} contains invalid {@link
     *     MedicalDataSource} IDs.
     */
    // Suppress missing because API flagged out.
    // TODO: b/355156275 - remove suppression once API not flagged out.
    @SuppressWarnings({"MissingPermission"})
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    @RequiresPermission(WRITE_MEDICAL_DATA)
    public void upsertMedicalResources(
            @NonNull List<UpsertMedicalResourceRequest> requests,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<List<MedicalResource>, HealthConnectException> callback) {
        Objects.requireNonNull(requests);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        if (requests.isEmpty()) {
            returnResult(executor, List.of(), callback);
            return;
        }

        try {
            if (Flags.phrUpsertFixUseSharedMemory()) {
                mService.upsertMedicalResourcesFromRequestsParcel(
                        mContext.getAttributionSource(),
                        new UpsertMedicalResourceRequestsParcel(requests),
                        new IMedicalResourceListParcelResponseCallback.Stub() {
                            @Override
                            public void onResult(
                                    MedicalResourceListParcel medicalResourceListParcel) {
                                returnResult(
                                        executor,
                                        medicalResourceListParcel.getMedicalResources(),
                                        callback);
                            }

                            @Override
                            public void onError(HealthConnectExceptionParcel exception) {
                                returnError(executor, exception, callback);
                            }
                        });
            } else {
                mService.upsertMedicalResources(
                        mContext.getAttributionSource(),
                        requests,
                        new IMedicalResourcesResponseCallback.Stub() {
                            @Override
                            public void onResult(List<MedicalResource> medicalResources) {
                                returnResult(executor, medicalResources, callback);
                            }

                            @Override
                            public void onError(HealthConnectExceptionParcel exception) {
                                returnError(executor, exception, callback);
                            }
                        });
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Reads {@link MedicalResource}s based on a list of {@link MedicalResourceId}s.
     *
     * <p>The number and order of medical resources returned by this API is not guaranteed. The
     * number will depend on the factors below:
     *
     * <ul>
     *   <li>If an empty list of {@code ids} is provided, an empty list will be returned.
     *   <li>If the size of {@code ids} is more than 5000, the API will throw an {@link
     *       IllegalArgumentException}.
     *   <li>If any ID in {@code ids} is invalid, the API will throw an {@link
     *       IllegalArgumentException}.
     *   <li>If any ID in {@code ids} does not exist, no medical resource will be returned for that
     *       ID.
     *   <li>Callers will only get medical resources they are permitted to get. See below.
     * </ul>
     *
     * Being permitted to read medical resources is dependent on the following logic, in priority
     * order, earlier statements take precedence.
     *
     * <ol>
     *   <li>A caller with the system permission can get any medical resources in the foreground or
     *       background.
     *   <li>A caller without any read or write permissions for health data will not be able to get
     *       any medical resources and receive an exception with code {@link
     *       HealthConnectException#ERROR_SECURITY} via {@code callback.onError()}, even for medical
     *       resources the caller has created.
     *   <li>Callers can get medical resources they have created, whether this method is called in
     *       the foreground or background. Note this only applies if the caller has at least one
     *       read or write permission for health data.
     *   <li>For any given medical resource, a caller can get that medical resource in the
     *       foreground if the caller has the corresponding read permission, or in the background if
     *       it also has {@link
     *       android.health.connect.HealthPermissions#READ_HEALTH_DATA_IN_BACKGROUND}.
     *   <li>In all other cases the caller is not permitted to get the given medical resource and it
     *       will not be returned.
     * </ol>
     *
     * <p>Each returned {@link MedicalResource} has passed the Health Connect FHIR validation checks
     * at write time, but is not guaranteed to meet all requirements of the <a
     * href="https://hl7.org/fhir/resourcelist.html">Fast Healthcare Interoperability Resources
     * (FHIR) spec</a>. If required, clients should perform their own checks on the data.
     *
     * @param ids Identifiers on which to perform read operation.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @throws IllegalArgumentException if the size of {@code ids} is more than 5000 or if any id is
     *     invalid.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public void readMedicalResources(
            @NonNull List<MedicalResourceId> ids,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<List<MedicalResource>, HealthConnectException> callback) {
        Objects.requireNonNull(ids);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        if (ids.isEmpty()) {
            returnResult(executor, List.of(), callback);
            return;
        }

        if (ids.size() > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "The number of requested IDs must be <= " + MAXIMUM_PAGE_SIZE);
        }

        try {
            mService.readMedicalResourcesByIds(
                    mContext.getAttributionSource(),
                    ids,
                    new IReadMedicalResourcesResponseCallback.Stub() {
                        @Override
                        public void onResult(ReadMedicalResourcesResponse response) {
                            returnResult(executor, response.getMedicalResources(), callback);
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Reads {@link MedicalResource}s based on {@link ReadMedicalResourcesInitialRequest} or {@link
     * ReadMedicalResourcesPageRequest}.
     *
     * <p>Being permitted to read medical resources is dependent on the following logic, in priority
     * order, earlier statements take precedence.
     *
     * <ol>
     *   <li>A caller with the system permission can get any medical resources in the foreground or
     *       background.
     *   <li>A caller without any read or write permissions for health data will not be able to get
     *       any medical resources and receive an exception with code {@link
     *       HealthConnectException#ERROR_SECURITY} via {@code callback.onError()}, even for medical
     *       resources the caller has created.
     *   <li>Callers can get medical resources they have created, whether this method is called in
     *       the foreground or background. Note this only applies if the caller has at least one
     *       read or write permission for health data.
     *   <li>For any given medical resource, a caller can get that medical resource in the
     *       foreground if the caller has the corresponding read permission, or in the background if
     *       it also has {@link
     *       android.health.connect.HealthPermissions#READ_HEALTH_DATA_IN_BACKGROUND}.
     *   <li>In all other cases the caller is not permitted to get the given medical resource and it
     *       will not be returned.
     * </ol>
     *
     * @param request The read request {@link ReadMedicalResourcesInitialRequest} or {@link
     *     ReadMedicalResourcesPageRequest}.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @throws IllegalArgumentException if {@code request} has set page size to be less than 1 or
     *     more than 5000; or if contains unsupported medical resource type or invalid {@link
     *     MedicalDataSource} IDs when using {@link ReadMedicalResourcesInitialRequest}.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public void readMedicalResources(
            @NonNull ReadMedicalResourcesRequest request,
            @NonNull Executor executor,
            @NonNull
                    OutcomeReceiver<ReadMedicalResourcesResponse, HealthConnectException>
                            callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.readMedicalResourcesByRequest(
                    mContext.getAttributionSource(),
                    request.toParcel(),
                    new IReadMedicalResourcesResponseCallback.Stub() {
                        @Override
                        public void onResult(ReadMedicalResourcesResponse response) {
                            returnResult(executor, response, callback);
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Deletes {@link MedicalResource}s based on given filters in {@link
     * DeleteMedicalResourcesRequest}.
     *
     * <p>Regarding permissions:
     *
     * <ul>
     *   <li>Only apps with the system permission can delete data written by apps other than
     *       themselves.
     *   <li>Deletes are permitted in the foreground or background.
     * </ul>
     *
     * @param request The delete request.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @throws IllegalArgumentException if {@code request} contains unsupported medical resource
     *     types or invalid {@link MedicalDataSource} IDs.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    // Suppress missing because API flagged out. "RequiresPermission" is also needed because
    // @RequiresPermission generates javadoc for the flagged out permission.
    // TODO: b/355156275 - remove suppression once API not flagged out.
    @SuppressWarnings({"MissingPermission", "RequiresPermission"})
    @RequiresPermission(anyOf = {WRITE_MEDICAL_DATA, MANAGE_HEALTH_DATA_PERMISSION})
    public void deleteMedicalResources(
            @NonNull DeleteMedicalResourcesRequest request,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Void, HealthConnectException> callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.deleteMedicalResourcesByRequest(
                    mContext.getAttributionSource(),
                    request,
                    new IEmptyResponseCallback.Stub() {
                        @Override
                        public void onResult() {
                            returnResult(executor, null, callback);
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Deletes a list of {@link MedicalResource}s by the provided list of {@link
     * MedicalResourceId}s.
     *
     * <ul>
     *   <li>If any ID in {@code ids} is invalid, the API will throw an {@link
     *       IllegalArgumentException}, and nothing will be deleted.
     *   <li>If any ID in {@code ids} does not exist, that ID will be ignored, while deletion on
     *       other IDs will be performed.
     * </ul>
     *
     * <p>Regarding permissions:
     *
     * <ul>
     *   <li>Only apps with the system permission can delete data written by apps other than
     *       themselves.
     *   <li>Deletes are permitted in the foreground or background.
     * </ul>
     *
     * @param ids The ids to delete.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    // Suppress missing because API flagged out. "RequiresPermission" is also needed because
    // @RequiresPermission generates javadoc for the flagged out permission.
    // TODO: b/355156275 - remove suppression once API not flagged out.
    @SuppressWarnings({"MissingPermission", "RequiresPermission"})
    @RequiresPermission(anyOf = {WRITE_MEDICAL_DATA, MANAGE_HEALTH_DATA_PERMISSION})
    public void deleteMedicalResources(
            @NonNull List<MedicalResourceId> ids,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Void, HealthConnectException> callback) {
        Objects.requireNonNull(ids);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        if (ids.isEmpty()) {
            returnResult(executor, null, callback);
            return;
        }

        try {
            mService.deleteMedicalResourcesByIds(
                    mContext.getAttributionSource(),
                    ids,
                    new IEmptyResponseCallback.Stub() {
                        @Override
                        public void onResult() {
                            returnResult(executor, null, callback);
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Retrieves information about all medical resource types and returns a list of {@link
     * MedicalResourceTypeInfo}.
     *
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @hide
     */
    @SystemApi
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    @RequiresPermission(MANAGE_HEALTH_DATA_PERMISSION)
    public void queryAllMedicalResourceTypeInfos(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull
                    OutcomeReceiver<List<MedicalResourceTypeInfo>, HealthConnectException>
                            callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.queryAllMedicalResourceTypeInfos(
                    new IMedicalResourceTypeInfosCallback.Stub() {
                        @Override
                        public void onResult(List<MedicalResourceTypeInfo> response) {
                            Binder.clearCallingIdentity();
                            executor.execute(() -> callback.onResult(response));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Creates a {@link MedicalDataSource} in HealthConnect based on the {@link
     * CreateMedicalDataSourceRequest} request values.
     *
     * <p>Medical data is represented using the <a href="https://hl7.org/fhir/">Fast Healthcare
     * Interoperability Resources (FHIR)</a> standard.
     *
     * <p>A {@link MedicalDataSource} needs to be created before any {@link MedicalResource}s for
     * that source can be inserted. Separate {@link MedicalDataSource}s should be created for
     * medical records coming from different sources (e.g. different FHIR endpoints, different
     * healthcare systems), unless the data has been reconciled and all records have a unique
     * combination of resource type and resource id.
     *
     * <p>The {@link CreateMedicalDataSourceRequest.Builder#setDisplayName display name} must be
     * unique per app, and {@link CreateMedicalDataSourceRequest.Builder#setFhirVersion} FHIR
     * version} must be a version supported by Health Connect, as documented on the {@link
     * FhirVersion}. See {@link CreateMedicalDataSourceRequest.Builder#setFhirBaseUri} for more
     * details on the FHIR base URI.
     *
     * @param request Creation request.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @throws IllegalArgumentException if {@code request} contains a FHIR base URI or display name
     *     exceeding the character limits, or an unsupported FHIR version.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    // Suppress missing because API flagged out.
    // TODO: b/355156275 - remove suppression once API not flagged out.
    @SuppressWarnings({"MissingPermission"})
    @RequiresPermission(WRITE_MEDICAL_DATA)
    public void createMedicalDataSource(
            @NonNull CreateMedicalDataSourceRequest request,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<MedicalDataSource, HealthConnectException> callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.createMedicalDataSource(
                    mContext.getAttributionSource(),
                    request,
                    new IMedicalDataSourceResponseCallback.Stub() {
                        @Override
                        public void onResult(MedicalDataSource dataSource) {
                            returnResult(executor, dataSource, callback);
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns {@link MedicalDataSource}s for the provided list of IDs.
     *
     * <p>The number and order of medical data sources returned by this API is not guaranteed. The
     * number will depend on the factors below:
     *
     * <ul>
     *   <li>If an empty list of {@code ids} is provided, an empty list will be returned.
     *   <li>If any ID in {@code ids} is invalid, the caller will receive an exception with code
     *       {@link HealthConnectException#ERROR_INVALID_ARGUMENT} via {@code callback.onError()}.
     *   <li>If any ID in {@code ids} does not exist, no data source will be returned for that ID.
     *   <li>Callers will only get data sources they are permitted to get. See below.
     * </ul>
     *
     * <p>There is no specific read permission for getting data sources. Instead, permission to read
     * data sources is based on whether the caller has permission to read the data currently linked
     * to that data source. Being permitted to get data sources is dependent on the following logic,
     * in priority order, earlier statements take precedence.
     *
     * <ol>
     *   <li>A caller with the system permission can get any data source in the foreground or
     *       background.
     *   <li>A caller without any read or write permissions for health data will not be able to get
     *       any medical data sources and receive an exception with code {@link
     *       HealthConnectException#ERROR_SECURITY} via {@code callback.onError()}, even for data
     *       sources the caller has created.
     *   <li>Callers can get data sources they have created, whether this method is called in the
     *       foreground or background. Note this only applies if the caller has at least one read or
     *       write permission for health data.
     *   <li>For any given data source, a caller can get that data source in the foreground if the
     *       caller has permission to read any of the data linked to that data source. For clarity,
     *       the does not allow it to get an empty data source.
     *   <li>For any given data source, a caller can get that data source in the background if it
     *       has both permission to read any of the data linked to that data source, and {@link
     *       android.health.connect.HealthPermissions#READ_HEALTH_DATA_IN_BACKGROUND}.
     *   <li>In all other cases the caller is not permitted to get the given data source and it will
     *       not be returned.
     * </ol>
     *
     * @param ids Identifiers for data sources to get.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @throws IllegalArgumentException if the size of {@code ids} is more than 5000.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public void getMedicalDataSources(
            @NonNull List<String> ids,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<List<MedicalDataSource>, HealthConnectException> callback) {
        Objects.requireNonNull(ids);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        if (ids.isEmpty()) {
            returnResult(executor, List.of(), callback);
            return;
        }

        if (ids.size() > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "The number of requested IDs must be <= " + MAXIMUM_PAGE_SIZE);
        }

        try {
            mService.getMedicalDataSourcesByIds(
                    mContext.getAttributionSource(),
                    ids,
                    new IMedicalDataSourcesResponseCallback.Stub() {
                        @Override
                        public void onResult(List<MedicalDataSource> result) {
                            returnResult(executor, result, callback);
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the requested {@link MedicalDataSource}s.
     *
     * <p>Number of data sources returned by this API will depend based on below factors:
     *
     * <ul>
     *   <li>If an empty {@link GetMedicalDataSourcesRequest#getPackageNames() list of package
     *       names} is passed, all permitted data sources from all apps will be returned. See below.
     *   <li>If any package name in the {@link GetMedicalDataSourcesRequest#getPackageNames() list
     *       of package names} is invalid, the API will throw an {@link IllegalArgumentException}.
     *   <li>If a non-empty {@link GetMedicalDataSourcesRequest#getPackageNames() list of package
     *       names} is specified in the request, then only the permitted data sources created by
     *       those packages will be returned. See below.
     * </ul>
     *
     * <p>There is no specific read permission for getting data sources. Instead permission to read
     * data sources is based on whether the caller has permission to read the data currently linked
     * to that data source. Being permitted to get data sources is dependent on the following logic,
     * in priority order, earlier statements take precedence.
     *
     * <ol>
     *   <li>A caller with the system permission can get any data source in the foreground or
     *       background.
     *   <li>A caller without any read or write permissions for health data will not be able to get
     *       any medical data sources and receive an exception with code {@link
     *       HealthConnectException#ERROR_SECURITY} via {@code callback.onError()}, even for data
     *       sources the caller has created.
     *   <li>Callers can get data sources they have created, whether this method is called in the
     *       foreground or background. Note this only applies if the caller has at least one read or
     *       write permission for health data.
     *   <li>For any given data source, a caller can get that data source in the foreground if the
     *       caller has permission to read any of the data linked to that data source. For clarity,
     *       the does not allow it to get an empty data source.
     *   <li>For any given data source, a caller can get that data source in the background if it
     *       has both permission to read any of the data linked to that data source, and {@link
     *       android.health.connect.HealthPermissions#READ_HEALTH_DATA_IN_BACKGROUND}.
     *   <li>In all other cases the caller is not permitted to get the given data source and it will
     *       not be returned.
     * </ol>
     *
     * @param request the request for which data sources to return.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     * @throws IllegalArgumentException if {@code request} contains invalid package names.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public void getMedicalDataSources(
            @NonNull GetMedicalDataSourcesRequest request,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<List<MedicalDataSource>, HealthConnectException> callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.getMedicalDataSourcesByRequest(
                    mContext.getAttributionSource(),
                    request,
                    new IMedicalDataSourcesResponseCallback.Stub() {
                        @Override
                        public void onResult(List<MedicalDataSource> result) {
                            returnResult(executor, result, callback);
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Deletes a {@link MedicalDataSource} and all data linked to it.
     *
     * <p>If the provided data source {@code id} is either invalid, or does not exist, or owned by
     * another apps, the caller will receive an exception with code {@link
     * HealthConnectException#ERROR_INVALID_ARGUMENT} via {@code callback.onError()}.
     *
     * <p>Regarding permissions:
     *
     * <ul>
     *   <li>Only apps with the system permission can delete data written by apps other than
     *       themselves.
     *   <li>Deletes are permitted in the foreground or background.
     * </ul>
     *
     * @param id The id of the data source to delete.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    // Suppress missing because API flagged out. "RequiresPermission" is also needed because
    // @RequiresPermission generates javadoc for the flagged out permission.
    // TODO: b/355156275 - remove suppression once API not flagged out.
    @SuppressWarnings({"MissingPermission", "RequiresPermission"})
    @RequiresPermission(anyOf = {WRITE_MEDICAL_DATA, MANAGE_HEALTH_DATA_PERMISSION})
    public void deleteMedicalDataSourceWithData(
            @NonNull String id,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Void, HealthConnectException> callback) {

        Objects.requireNonNull(id);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        try {
            mService.deleteMedicalDataSourceWithData(
                    mContext.getAttributionSource(),
                    id,
                    new IEmptyResponseCallback.Stub() {
                        @Override
                        public void onResult() {
                            returnResult(executor, null, callback);
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * The changeToken returned by the previous call should be passed in to resume the upload. A
     * null or empty changeToken means we are doing a fresh backup, and should start from the
     * beginning.
     *
     * <p>If the changeToken is not found, it means that HealthConnect can no longer resume the
     * backup from this point, and will respond with an Exception. The caller should restart the
     * backup in this case.
     *
     * <p>If no changes are returned by the API, this means that the client has synced all changes
     * as of now.
     *
     * @hide
     */
    @SuppressWarnings("NullAway") // TODO: b/178748627 - fix this suppression.
    @FlaggedApi(FLAG_CLOUD_BACKUP_AND_RESTORE)
    public void getChangesForBackup(
            @Nullable String changeToken,
            @NonNull Executor executor,
            @NonNull
                    OutcomeReceiver<GetChangesForBackupResponse, HealthConnectException> callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.getChangesForBackup(
                    changeToken,
                    new IGetChangesForBackupResponseCallback.Stub() {
                        @Override
                        public void onResult(GetChangesForBackupResponse response) {
                            returnResult(executor, response, callback);
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns all user settings bundled as a single byte array.
     *
     * @hide
     */
    @FlaggedApi(FLAG_CLOUD_BACKUP_AND_RESTORE)
    public void getSettingsForBackup(
            @NonNull Executor executor,
            @NonNull
                    OutcomeReceiver<GetSettingsForBackupResponse, HealthConnectException>
                            callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.getSettingsForBackup(
                    new IGetSettingsForBackupResponseCallback.Stub() {
                        @Override
                        public void onResult(GetSettingsForBackupResponse response) {
                            returnResult(executor, response, callback);
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            returnError(executor, exception, callback);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
