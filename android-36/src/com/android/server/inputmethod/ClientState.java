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

package com.android.server.inputmethod;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.IBinder;
import android.util.SparseArray;
import android.view.inputmethod.InputBinding;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.inputmethod.IRemoteInputConnection;

final class ClientState {

    @NonNull
    final IInputMethodClientInvoker mClient;
    @NonNull
    final IRemoteInputConnection mFallbackInputConnection;
    final int mUid;
    final int mPid;
    final int mSelfReportedDisplayId;
    @NonNull
    final InputBinding mBinding;
    @NonNull
    final IBinder.DeathRecipient mClientDeathRecipient;

    @GuardedBy("ImfLock.class")
    boolean mSessionRequested;

    @GuardedBy("ImfLock.class")
    boolean mSessionRequestedForAccessibility;

    @GuardedBy("ImfLock.class")
    @Nullable
    InputMethodManagerService.SessionState mCurSession;

    @GuardedBy("ImfLock.class")
    @NonNull
    final SparseArray<InputMethodManagerService.AccessibilitySessionState> mAccessibilitySessions =
            new SparseArray<>();

    ClientState(@NonNull IInputMethodClientInvoker client,
            @NonNull IRemoteInputConnection fallbackInputConnection, int uid, int pid,
            int selfReportedDisplayId, @NonNull IBinder.DeathRecipient clientDeathRecipient) {
        mClient = client;
        mFallbackInputConnection = fallbackInputConnection;
        mUid = uid;
        mPid = pid;
        mSelfReportedDisplayId = selfReportedDisplayId;
        mBinding = new InputBinding(null /* conn */, fallbackInputConnection.asBinder(), mUid,
                mPid);
        mClientDeathRecipient = clientDeathRecipient;
    }

    @Override
    public String toString() {
        return "ClientState{" + Integer.toHexString(System.identityHashCode(this))
                + " mUid=" + mUid + " mPid=" + mPid
                + " mSelfReportedDisplayId=" + mSelfReportedDisplayId + "}";
    }
}
