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
import android.util.ExceptionUtils;

final class PrepareFailure extends PackageManagerException {

    public String mConflictingPackage;
    public String mConflictingPermission;

    PrepareFailure(int error) {
        super(error, "Failed to prepare for install.");
    }

    PrepareFailure(int error, String detailMessage) {
        super(error, detailMessage);
    }

    public static PrepareFailure ofInternalError(String detailMessage, int internalErrorCode) {
        return new PrepareFailure(PackageManager.INSTALL_FAILED_INTERNAL_ERROR, detailMessage,
                internalErrorCode);
    }

    private PrepareFailure(int error, String message, int internalErrorCode) {
        super(error, message, internalErrorCode);
    }

    PrepareFailure(String message, Exception e) {
        super(e instanceof PackageManagerException ? ((PackageManagerException) e).error
                        : PackageManager.INSTALL_FAILED_INTERNAL_ERROR,
                ExceptionUtils.getCompleteMessage(message, e));
    }

    PrepareFailure conflictsWithExistingPermission(String conflictingPermission,
            String conflictingPackage) {
        mConflictingPermission = conflictingPermission;
        mConflictingPackage = conflictingPackage;
        return this;
    }
}
