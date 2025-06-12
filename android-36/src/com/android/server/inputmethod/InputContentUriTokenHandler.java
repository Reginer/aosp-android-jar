/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.server.inputmethod;

import android.annotation.NonNull;
import android.annotation.UserIdInt;
import android.app.UriGrantsManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.inputmethod.IInputContentUriToken;
import com.android.server.LocalServices;
import com.android.server.uri.UriGrantsManagerInternal;

final class InputContentUriTokenHandler extends IInputContentUriToken.Stub {

    @NonNull
    private final Uri mUri;
    private final int mSourceUid;
    @NonNull
    private final String mTargetPackage;
    @UserIdInt
    private final int mSourceUserId;
    @UserIdInt
    private final int mTargetUserId;

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private IBinder mPermissionOwnerToken = null;

    InputContentUriTokenHandler(@NonNull Uri contentUri, int sourceUid,
            @NonNull String targetPackage, @UserIdInt int sourceUserId,
            @UserIdInt int targetUserId) {
        mUri = contentUri;
        mSourceUid = sourceUid;
        mTargetPackage = targetPackage;
        mSourceUserId = sourceUserId;
        mTargetUserId = targetUserId;
    }

    @Override
    public void take() {
        synchronized (mLock) {
            if (mPermissionOwnerToken != null) {
                // Permission is already granted.
                return;
            }

            mPermissionOwnerToken = LocalServices.getService(UriGrantsManagerInternal.class)
                    .newUriPermissionOwner("InputContentUriTokenHandler");

            doTakeLocked(mPermissionOwnerToken);
        }
    }

    private void doTakeLocked(@NonNull IBinder permissionOwner) {
        final long origId = Binder.clearCallingIdentity();
        try {
            try {
                UriGrantsManager.getService().grantUriPermissionFromOwner(
                        permissionOwner, mSourceUid, mTargetPackage, mUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION, mSourceUserId, mTargetUserId);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    @Override
    public void release() {
        synchronized (mLock) {
            if (mPermissionOwnerToken == null) {
                return;
            }
            try {
                LocalServices.getService(UriGrantsManagerInternal.class)
                        .revokeUriPermissionFromOwner(mPermissionOwnerToken, mUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION, mSourceUserId);
            } finally {
                mPermissionOwnerToken = null;
            }
        }
    }

    /**
     * If permissions are not released explicitly via {@link #release()}, release automatically
     * whenever there are no more references to this object.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            release();
        } finally {
            super.finalize();
        }
    }
}
