/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.backup;

import android.annotation.NonNull;
import android.annotation.UserIdInt;
import android.app.backup.BackupRestoreEventLogger.BackupRestoreError;
import android.app.backup.BackupRestoreEventLogger.BackupRestoreDataType;
import android.app.backup.BlobBackupHelper;
import android.util.Slog;

import com.android.server.LocalServices;
import com.android.server.locales.LocaleManagerInternal;

/**
 * Helper for backing up app-specific locales.
 * <p>
 * This helper is used in {@link com.android.server.backup.SystemBackupAgent}
 */
public class AppSpecificLocalesBackupHelper extends BlobBackupHelper {
    private static final String TAG = "AppLocalesBackupHelper";   // must be < 23 chars
    private static final boolean DEBUG = false;

    @BackupRestoreDataType
    private static final String DATA_TYPE_APP_LOCALES = "app_locales:locales";

    @BackupRestoreError
    private static final String ERROR_UNEXPECTED_KEY = "unexpected_key";
    @BackupRestoreError
    private static final String ERROR_BACKUP_FAILED = "backup_failed";
    @BackupRestoreError
    private static final String ERROR_RESTORE_FAILED = "restore_failed";

    // Current version of the blob schema
    private static final int BLOB_VERSION = 1;

    // Key under which the payload blob is stored
    private static final String KEY_APP_LOCALES = "app_locales";

    private final @UserIdInt int mUserId;

    private final @NonNull LocaleManagerInternal mLocaleManagerInternal;

    public AppSpecificLocalesBackupHelper(int userId) {
        super(BLOB_VERSION, KEY_APP_LOCALES);
        mUserId = userId;
        mLocaleManagerInternal = LocalServices.getService(LocaleManagerInternal.class);
    }

    @Override
    protected byte[] getBackupPayload(String key) {
        if (DEBUG) {
            Slog.d(TAG, "Handling backup of " + key);
        }

        byte[] newPayload = null;
        if (KEY_APP_LOCALES.equals(key)) {
            try {
                newPayload = mLocaleManagerInternal.getBackupPayload(mUserId);
                getLogger().logItemsBackedUp(
                        DATA_TYPE_APP_LOCALES,
                        /* count= */ 1);
            } catch (Exception e) {
                // Treat as no data
                Slog.e(TAG, "Couldn't communicate with locale manager", e);
                getLogger().logItemsBackupFailed(
                        DATA_TYPE_APP_LOCALES,
                        /* count= */ 1,
                        ERROR_BACKUP_FAILED);
                newPayload = null;
            }
        } else {
            Slog.w(TAG, "Unexpected backup key " + key);
            getLogger().logItemsBackupFailed(
                    DATA_TYPE_APP_LOCALES,
                    /* count= */ 1,
                    ERROR_UNEXPECTED_KEY);
        }
        return newPayload;
    }

    @Override
    protected void applyRestoredPayload(String key, byte[] payload) {
        if (DEBUG) {
            Slog.d(TAG, "Handling restore of " + key);
        }

        if (KEY_APP_LOCALES.equals(key)) {
            try {
                mLocaleManagerInternal.stageAndApplyRestoredPayload(payload, mUserId);
                getLogger().logItemsRestored(
                        DATA_TYPE_APP_LOCALES,
                        /* count= */ 1);
            } catch (Exception e) {
                Slog.e(TAG, "Couldn't communicate with locale manager", e);
                getLogger().logItemsRestoreFailed(
                        DATA_TYPE_APP_LOCALES,
                        /* count= */ 1,
                        ERROR_RESTORE_FAILED);
            }
        } else {
            Slog.w(TAG, "Unexpected restore key " + key);
            getLogger().logItemsBackupFailed(
                    DATA_TYPE_APP_LOCALES,
                    /* count= */ 1,
                    ERROR_UNEXPECTED_KEY);
        }
    }

}
