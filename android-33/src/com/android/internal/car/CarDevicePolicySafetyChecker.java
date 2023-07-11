/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.internal.car;

import static android.app.admin.DevicePolicyManager.OPERATION_CLEAR_APPLICATION_USER_DATA;
import static android.app.admin.DevicePolicyManager.OPERATION_LOGOUT_USER;
import static android.app.admin.DevicePolicyManager.OPERATION_REBOOT;
import static android.app.admin.DevicePolicyManager.OPERATION_REQUEST_BUGREPORT;
import static android.app.admin.DevicePolicyManager.OPERATION_SAFETY_REASON_DRIVING_DISTRACTION;
import static android.app.admin.DevicePolicyManager.OPERATION_SET_APPLICATION_HIDDEN;
import static android.app.admin.DevicePolicyManager.OPERATION_SET_APPLICATION_RESTRICTIONS;
import static android.app.admin.DevicePolicyManager.OPERATION_SET_KEYGUARD_DISABLED;
import static android.app.admin.DevicePolicyManager.OPERATION_SET_LOCK_TASK_FEATURES;
import static android.app.admin.DevicePolicyManager.OPERATION_SET_LOCK_TASK_PACKAGES;
import static android.app.admin.DevicePolicyManager.OPERATION_SET_PACKAGES_SUSPENDED;
import static android.app.admin.DevicePolicyManager.OPERATION_SET_STATUS_BAR_DISABLED;
import static android.app.admin.DevicePolicyManager.OPERATION_SET_SYSTEM_SETTING;
import static android.app.admin.DevicePolicyManager.OPERATION_SWITCH_USER;
import static android.app.admin.DevicePolicyManager.operationToString;

import android.annotation.NonNull;
import android.app.admin.DevicePolicyManager.DevicePolicyOperation;
import android.app.admin.DevicePolicyManagerLiteInternal;
import android.app.admin.DevicePolicySafetyChecker;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.LocalServices;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Integrates {@link android.app.admin.DevicePolicyManager} operations with car UX restrictions.
 *
 * @hide
 */
final class CarDevicePolicySafetyChecker {

    private static final String TAG = CarDevicePolicySafetyChecker.class.getSimpleName();

    private static final boolean DEBUG = false;

    private static final int[] UNSAFE_OPERATIONS = new int[] {
            OPERATION_CLEAR_APPLICATION_USER_DATA,
            OPERATION_LOGOUT_USER,
            OPERATION_REBOOT,
            OPERATION_REQUEST_BUGREPORT,
            OPERATION_SET_APPLICATION_HIDDEN,
            OPERATION_SET_APPLICATION_RESTRICTIONS,
            OPERATION_SET_KEYGUARD_DISABLED,
            OPERATION_SET_LOCK_TASK_FEATURES,
            OPERATION_SET_LOCK_TASK_PACKAGES,
            OPERATION_SET_PACKAGES_SUSPENDED,
            OPERATION_SET_STATUS_BAR_DISABLED,
            OPERATION_SET_SYSTEM_SETTING,
            OPERATION_SWITCH_USER
    };

    private final AtomicBoolean mSafe = new AtomicBoolean(true);

    private final DevicePolicySafetyChecker mCheckerImplementation;
    private final DevicePolicyManagerLiteInternal mDpmi;

    CarDevicePolicySafetyChecker(DevicePolicySafetyChecker checkerImplementation) {
        this(checkerImplementation,
                LocalServices.getService(DevicePolicyManagerLiteInternal.class));
    }

    @VisibleForTesting
    CarDevicePolicySafetyChecker(DevicePolicySafetyChecker checkerImplementation,
            DevicePolicyManagerLiteInternal dpmi) {
        mCheckerImplementation = Objects.requireNonNull(checkerImplementation,
                "DevicePolicySafetyChecker cannot be null");
        mDpmi = Objects.requireNonNull(dpmi, "DevicePolicyManagerLiteInternal cannot be null");
    }

    boolean isDevicePolicyOperationSafe(@DevicePolicyOperation int operation) {
        boolean safe = true;
        boolean globalSafe = mSafe.get();
        if (!globalSafe) {
            for (int unsafeOperation : UNSAFE_OPERATIONS) {
                if (unsafeOperation == operation) {
                    safe = false;
                    break;
                }
            }
        }

        if (DEBUG) {
            Slog.d(TAG, "isDevicePolicyOperationSafe(" + operationToString(operation)
                    + "): " + safe + " (mSafe=" + globalSafe + ")");
        }
        return safe;
    }

    // TODO(b/172376923): override getUnsafeStateException to show error message explaining how to
    // wrap it under CarDevicePolicyManager

    void setSafe(boolean safe) {
        Slog.i(TAG, "Setting safe to " + safe);
        mSafe.set(safe);

        mDpmi.notifyUnsafeOperationStateChanged(mCheckerImplementation,
                OPERATION_SAFETY_REASON_DRIVING_DISTRACTION, /* isSafe= */ safe);
    }

    boolean isSafe() {
        return mSafe.get();
    }

    void dump(@NonNull PrintWriter pw) {
        pw.printf("Safe to run device policy operations: %b\n", mSafe.get());
        pw.printf("Unsafe operations: %s\n", Arrays.stream(UNSAFE_OPERATIONS)
                .mapToObj(o -> operationToString(o)).collect(Collectors.toList()));
    }
}
