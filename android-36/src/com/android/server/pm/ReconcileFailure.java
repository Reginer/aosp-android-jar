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

package com.android.server.pm;

import android.content.pm.PackageManager;

final class ReconcileFailure extends PackageManagerException {
    public static ReconcileFailure ofInternalError(String message, int internalErrorCode) {
        return new ReconcileFailure(message, internalErrorCode);
    }

    private ReconcileFailure(String message, int internalErrorCode) {
        super(PackageManager.INSTALL_FAILED_INTERNAL_ERROR, "Reconcile failed: " + message,
                internalErrorCode);
    }

    ReconcileFailure(int reason, String message) {
        super(reason, "Reconcile failed: " + message);
    }
    ReconcileFailure(PackageManagerException e) {
        this(e.error, e.getMessage());
    }
}
