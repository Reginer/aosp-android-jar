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

package android.health.connect.migration;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents the state of the Health Connect UI as Data Migration is happening.
 *
 * @hide
 */
public final class HealthConnectMigrationUiState implements Parcelable {

    /**
     * Starting UI state for the migration process. No UI messaging should happen.
     *
     * @hide
     */
    public static final int MIGRATION_UI_STATE_IDLE = 0;

    /**
     * Migration ready to start. No UI messaging should happen.
     *
     * @hide
     */
    public static final int MIGRATION_UI_STATE_ALLOWED_MIGRATOR_DISABLED = 1;

    /**
     * Migration ready to start but the migrator package became unresponsive to the broadcast. UI
     * messaging: Integration Paused.
     *
     * @hide
     */
    public static final int MIGRATION_UI_STATE_ALLOWED_NOT_STARTED = 2;

    /**
     * Migration was in progress but the migrator package becamse unresponsive to the broadcast, the
     * IN_PROGRESS state timed out and the new state is ALLOWED. UI messaging: Integration Paused.
     *
     * @hide
     */
    public static final int MIGRATION_UI_STATE_ALLOWED_PAUSED = 3;

    /**
     * Migration was in progress but the migrator package stopped handling the SHOW_MIGRATION_INFO
     * intent. The IN_PROGRESS state timed out and the new state is ALLOWED. UI messaging:
     * Integration Cancelled.
     *
     * @hide
     */
    public static final int MIGRATION_UI_STATE_ALLOWED_ERROR = 4;

    /**
     * Migration is in progress. UI messaging: Integration in Progress.
     *
     * @hide
     */
    public static final int MIGRATION_UI_STATE_IN_PROGRESS = 5;

    /**
     * Migration needs the migrator package to be upgraded. UI messaging: App upgrade needed.
     *
     * @hide
     */
    public static final int MIGRATION_UI_STATE_APP_UPGRADE_REQUIRED = 6;

    /**
     * Migration needs the module to be upgraded. UI messaging: Module upgrade needed.
     *
     * @hide
     */
    public static final int MIGRATION_UI_STATE_MODULE_UPGRADE_REQUIRED = 7;

    /**
     * Migration completed after startMigration() was called at least once. UI messaging:
     * Integration complete.
     *
     * @hide
     */
    public static final int MIGRATION_UI_STATE_COMPLETE = 8;

    /**
     * Migration completed from the IDLE state due to timeout. No UI messaging should happen.
     *
     * @hide
     */
    public static final int MIGRATION_UI_STATE_COMPLETE_IDLE = 9;

    /**
     * An unknown migration UI state.
     *
     * @hide
     */
    public static final int MIGRATION_UI_STATE_UNKNOWN = 10;

    private final @Type int mMigrationUiState;

    public @Type int getHealthConnectMigrationUiState() {
        return mMigrationUiState;
    }

    public HealthConnectMigrationUiState(@Type int migrationUiState) {
        this.mMigrationUiState = migrationUiState;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mMigrationUiState);
    }

    @NonNull
    public static final Creator<HealthConnectMigrationUiState> CREATOR =
            new Creator<>() {
                @Override
                public HealthConnectMigrationUiState createFromParcel(Parcel in) {
                    return new HealthConnectMigrationUiState(in);
                }

                @Override
                public HealthConnectMigrationUiState[] newArray(int size) {
                    return new HealthConnectMigrationUiState[size];
                }
            };

    private HealthConnectMigrationUiState(Parcel in) {
        mMigrationUiState = in.readInt();
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        MIGRATION_UI_STATE_IDLE,
        MIGRATION_UI_STATE_ALLOWED_MIGRATOR_DISABLED,
        MIGRATION_UI_STATE_ALLOWED_NOT_STARTED,
        MIGRATION_UI_STATE_ALLOWED_PAUSED,
        MIGRATION_UI_STATE_ALLOWED_ERROR,
        MIGRATION_UI_STATE_IN_PROGRESS,
        MIGRATION_UI_STATE_APP_UPGRADE_REQUIRED,
        MIGRATION_UI_STATE_MODULE_UPGRADE_REQUIRED,
        MIGRATION_UI_STATE_COMPLETE,
        MIGRATION_UI_STATE_COMPLETE_IDLE,
        MIGRATION_UI_STATE_UNKNOWN
    })
    public @interface Type {}
}
