/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents the state of HealthConnect data as it goes through one of the following operations:
 * <li>Data Restore: fetching and restoring the data either from the cloud or from another device.
 * <li>Data Migration: migrating the data from the app using the data-migration APIs: {@link
 *     HealthConnectManager#startMigration}, {@link HealthConnectManager#writeMigrationData}, and
 *     {@link HealthConnectManager#finishMigration}
 *
 * @hide
 */
@SystemApi
public final class HealthConnectDataState implements Parcelable {
    /**
     * The default idle state of HealthConnect data restore process. This states means that nothing
     * related to the data restore process is undergoing. {@link #getDataRestoreError()} could
     * return an error for previous restoration attempt.
     *
     * <p>See also {@link DataRestoreState}
     *
     * @hide
     */
    @SystemApi public static final int RESTORE_STATE_IDLE = 0;

    /**
     * The HealthConnect data is pending restoration. The system is in the process of fetching /
     * staging the remote data on this device. Once the data has been fetched and staged for
     * restoration an attempt will be made to restore the data. So, this will follow with {@link
     * #RESTORE_STATE_IN_PROGRESS} state.
     *
     * <p>See also {@link DataRestoreState}
     *
     * @hide
     */
    @SystemApi public static final int RESTORE_STATE_PENDING = 1;

    /**
     * The HealthConnect staged data is being restored. On a successful restore the data will be
     * available for use on this device.
     *
     * <p>After the restore process is finished, we'll come back to the {@link #RESTORE_STATE_IDLE}.
     *
     * <p>See also {@link DataRestoreState}
     *
     * @hide
     */
    @SystemApi public static final int RESTORE_STATE_IN_PROGRESS = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RESTORE_STATE_IDLE, RESTORE_STATE_PENDING, RESTORE_STATE_IN_PROGRESS})
    public @interface DataRestoreState {}

    /**
     * No error.
     *
     * @hide
     */
    @SystemApi public static final int RESTORE_ERROR_NONE = 0;

    /**
     * An unknown error caused a failure in restoring the data.
     *
     * <p>This is a non-recoverable error.
     *
     * @hide
     */
    @SystemApi public static final int RESTORE_ERROR_UNKNOWN = 1;

    /**
     * An error was encountered fetching the remote HealthConnect data.
     *
     * <p>This is a non-recoverable error.
     *
     * <p>For instance, this could have been caused by a network issue leading to download failure.
     * In such a case a retry would've been attempted, but eventually that failed as well.
     *
     * @hide
     */
    @SystemApi public static final int RESTORE_ERROR_FETCHING_DATA = 2;

    /**
     * The fetched remote data could not be restored because the current HealthConnect version on
     * the device is behind the staged data version.
     *
     * <p>This is a recoverable error.
     *
     * <p>Until the module has been updated we'll be waiting in the {@link #RESTORE_STATE_PENDING}
     * state. Once the HealthConnect version on the device is updated and rebooted then the restore
     * will be attempted on the same device reboot.
     *
     * @hide
     */
    @SystemApi public static final int RESTORE_ERROR_VERSION_DIFF = 3;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        RESTORE_ERROR_NONE,
        RESTORE_ERROR_UNKNOWN,
        RESTORE_ERROR_FETCHING_DATA,
        RESTORE_ERROR_VERSION_DIFF
    })
    public @interface DataRestoreError {}

    /**
     * The starting default state for the Migration process.
     *
     * <p>We'll begin in this state irrespective of whether there's an app installed that can
     * perform Migration. If there's no such app installed then we stay in this state. However, if
     * an installed app can be upgraded to become Migration-aware, then we'll move to the {@link
     * #MIGRATION_STATE_APP_UPGRADE_REQUIRED} state. Please see {@link
     * #MIGRATION_STATE_APP_UPGRADE_REQUIRED} for more info.
     *
     * <p>See also {@link DataMigrationState}
     *
     * @hide
     */
    @SystemApi public static final int MIGRATION_STATE_IDLE = 0;

    /**
     * This reflects that the app needs an upgrade before it can start the Migration process.
     *
     * <p>This happens when the module finds out that there's an installed app that can perform the
     * Migration process once it has been upgraded to the correct version. Once such an app is
     * available then we'll move back to the {@link #MIGRATION_STATE_IDLE} state.
     *
     * <p>We can come here only from the {@link #MIGRATION_STATE_IDLE} state.
     *
     * <p>See also {@link DataMigrationState}
     *
     * @hide
     */
    @SystemApi public static final int MIGRATION_STATE_APP_UPGRADE_REQUIRED = 1;

    /**
     * This reflects that the module needs an upgrade to handle the Migration process.
     *
     * <p>This happens when the version set by the caller is ahead of the HealthConnect module. Once
     * the module has updated to a version greater or equal to the said set version, then we'll move
     * to the {@link #MIGRATION_STATE_ALLOWED} state from where the Migration process can start.
     *
     * <p>We can come here only from the {@link #MIGRATION_STATE_IDLE} state.
     *
     * <p>See also {@link DataMigrationState}
     *
     * @hide
     */
    @SystemApi public static final int MIGRATION_STATE_MODULE_UPGRADE_REQUIRED = 2;

    /**
     * We are in the process of integrating the data shared by the app using the {@link
     * HealthConnectManager#writeMigrationData} API.
     *
     * <p>We get into this state when the app makes the {@link HealthConnectManager#startMigration}
     * call.
     *
     * <p>We can come here from either {@link #MIGRATION_STATE_ALLOWED} or {@link
     * #MIGRATION_STATE_IDLE} states when {@link HealthConnectManager#startMigration} is called.
     *
     * <p>From here we can go to either {@link #MIGRATION_STATE_ALLOWED} OR {@link
     * #MIGRATION_STATE_COMPLETE} state.
     *
     * <p>All other HealthConnect APIs unrelated to Migration are blocked while we are in this
     * state. For more info on this please see
     *
     * <p>See also {@link DataMigrationState}
     *
     * @hide
     */
    @SystemApi public static final int MIGRATION_STATE_IN_PROGRESS = 3;

    /**
     * The Migration is now allowed and is waiting to start or resume.
     *
     * <p>We can come to this state from any of the following states:
     *
     * <ul>
     *   <li>{@link #MIGRATION_STATE_IDLE} if the module is ready to
     *   <li>{@link #MIGRATION_STATE_MODULE_UPGRADE_REQUIRED} when the module upgrades to the
     *       minimum required version.
     *   <li>{@link #MIGRATION_STATE_IN_PROGRESS} in case of a timeout of 12 hours.
     * </ul>
     *
     * <p>From this state we can go to either {@link #MIGRATION_STATE_IN_PROGRESS} or {@link
     * #MIGRATION_STATE_COMPLETE} (in case of timeout of 15 days).
     *
     * <p>See also {@link DataMigrationState}
     *
     * @hide
     */
    @SystemApi public static final int MIGRATION_STATE_ALLOWED = 4;

    /**
     * This is the final state for the Migration process. We can come here from any other state:
     *
     * <ul>
     *   <li>From {@link #MIGRATION_STATE_IDLE} after a timeout of 30 days.
     *   <li>From {@link #MIGRATION_STATE_MODULE_UPGRADE_REQUIRED} after a timeout of 15 days.
     *   <li>From {@link #MIGRATION_STATE_IN_PROGRESS} when {@link
     *       HealthConnectManager#finishMigration} is called.
     *   <li>From {@link #MIGRATION_STATE_ALLOWED} after a timeout of 15 days.
     * </ul>
     *
     * <p>See also {@link DataMigrationState}
     *
     * @hide
     */
    @SystemApi public static final int MIGRATION_STATE_COMPLETE = 5;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        MIGRATION_STATE_IDLE,
        MIGRATION_STATE_APP_UPGRADE_REQUIRED,
        MIGRATION_STATE_MODULE_UPGRADE_REQUIRED,
        MIGRATION_STATE_IN_PROGRESS,
        MIGRATION_STATE_ALLOWED,
        MIGRATION_STATE_COMPLETE
    })
    public @interface DataMigrationState {}

    private final @DataRestoreState int mDataRestoreState;
    private final @DataRestoreError int mDataRestoreError;
    private final @DataMigrationState int mDataMigrationState;

    /**
     * The state of the HealthConnect data as it goes through the Data Restore process.
     *
     * <p>See also {@link DataRestoreState}
     */
    public @DataRestoreState int getDataRestoreState() {
        return mDataRestoreState;
    }

    /**
     * Get error encountered at the time of calling this API as we try to fetch and restore the
     * remote HealthConnect data.
     *
     * <p>Since we stop at the first encounter of an error there can be only one error at any time.
     *
     * <p>Some of the errors are recoverable while others are non-recoverable. Please see {@link
     * DataRestoreError} for more details on which errors are recoverable and how to recover from
     * them.
     */
    public @DataRestoreError int getDataRestoreError() {
        return mDataRestoreError;
    }

    /**
     * The state of the HealthConnect data as it goes through the Data Migration process.
     *
     * <p>See also {@link DataMigrationState}
     */
    public @DataMigrationState int getDataMigrationState() {
        return mDataMigrationState;
    }

    /** @hide */
    public HealthConnectDataState(
            @DataRestoreState int dataRestoreState,
            @DataRestoreError int dataRestoreError,
            @DataMigrationState int dataMigrationState) {
        this.mDataRestoreState = dataRestoreState;
        this.mDataRestoreError = dataRestoreError;
        this.mDataMigrationState = dataMigrationState;
    }

    @NonNull
    public static final Creator<HealthConnectDataState> CREATOR =
            new Creator<>() {
                @Override
                public HealthConnectDataState createFromParcel(Parcel in) {
                    return new HealthConnectDataState(in);
                }

                @Override
                public HealthConnectDataState[] newArray(int size) {
                    return new HealthConnectDataState[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mDataRestoreState);
        dest.writeInt(mDataRestoreError);
        dest.writeInt(mDataMigrationState);
    }

    private HealthConnectDataState(Parcel in) {
        mDataRestoreState = in.readInt();
        mDataRestoreError = in.readInt();
        mDataMigrationState = in.readInt();
    }
}
