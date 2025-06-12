/*
 * Copyright 2024 The Android Open Source Project
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

import static com.android.server.input.InputManagerInternal.BACKUP_CATEGORY_INPUT_GESTURES;

import android.annotation.NonNull;
import android.annotation.UserIdInt;
import android.app.backup.BlobBackupHelper;
import android.util.Slog;

import com.android.server.LocalServices;
import com.android.server.input.InputManagerInternal;

import java.util.HashMap;
import java.util.Map;

public class InputBackupHelper extends BlobBackupHelper {
    private static final String TAG = "InputBackupHelper";   // must be < 23 chars

    // Current version of the blob schema
    private static final int BLOB_VERSION = 1;

    // Key under which the payload blob is stored
    private static final String KEY_INPUT_GESTURES = "input_gestures";

    private final @UserIdInt int mUserId;

    private final @NonNull InputManagerInternal mInputManagerInternal;

    public InputBackupHelper(int userId) {
        super(BLOB_VERSION, KEY_INPUT_GESTURES);
        mUserId = userId;
        mInputManagerInternal = LocalServices.getService(InputManagerInternal.class);
    }

    @Override
    protected byte[] getBackupPayload(String key) {
        Map<Integer, byte[]> payloads;
        try {
            payloads = mInputManagerInternal.getBackupPayload(mUserId);
        } catch (Exception exception) {
            Slog.e(TAG, "Failed to get backup payload for input gestures", exception);
            return null;
        }

        if (KEY_INPUT_GESTURES.equals(key)) {
            return payloads.getOrDefault(BACKUP_CATEGORY_INPUT_GESTURES, null);
        }

        return null;
    }

    @Override
    protected void applyRestoredPayload(String key, byte[] payload) {
        Map<Integer, byte[]> payloads = new HashMap<>();
        if (KEY_INPUT_GESTURES.equals(key)) {
            payloads.put(BACKUP_CATEGORY_INPUT_GESTURES, payload);
        }

        try {
            mInputManagerInternal.applyBackupPayload(payloads, mUserId);
        } catch (Exception exception) {
            Slog.e(TAG, "Failed to apply input backup payload", exception);
        }
    }

}
