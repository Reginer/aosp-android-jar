/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.backup;

import android.annotation.Nullable;
import android.app.backup.BlobBackupHelper;

import com.android.server.LocalServices;

/** A {@link android.app.backup.BlobBackupHelper} for Wear */
public class WearBackupHelper extends BlobBackupHelper {

    private static final int BLOB_VERSION = 1;
    private static final String KEY_WEAR_BACKUP = "wear";
    @Nullable private final WearBackupInternal mWearBackupInternal;

    public WearBackupHelper() {
        super(BLOB_VERSION, KEY_WEAR_BACKUP);
        mWearBackupInternal = LocalServices.getService(WearBackupInternal.class);
    }

    @Override
    protected byte[] getBackupPayload(String key) {
        return KEY_WEAR_BACKUP.equals(key) && mWearBackupInternal != null
                ? mWearBackupInternal.getBackupPayload(getLogger())
                : null;
    }

    @Override
    protected void applyRestoredPayload(String key, byte[] payload) {
        if (KEY_WEAR_BACKUP.equals(key) && mWearBackupInternal != null) {
            mWearBackupInternal.applyRestoredPayload(payload);
        }
    }
}
