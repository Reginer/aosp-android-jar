/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.setupwizardlib;

import android.annotation.IntDef;

/**
 * Defines the constants used for the communication between the client and service in setting
 * the initial lock.
 */
public interface InitialLockSetupConstants {

    /**
     * The library version. All relevant changes should bump this version number and ensure
     * all relevant parts of the interface handle backwards compatibility.
     */
    int LIBRARY_VERSION = 1;

    /**
     * Lock types supported by the InitialLockSetupService.
     */
    @IntDef({
            LockTypes.PASSWORD,
            LockTypes.PIN,
            LockTypes.PATTERN,
            LockTypes.NONE
    })
    @interface LockTypes {
        int PASSWORD = 0;
        int PIN = 1;
        int PATTERN = 2;
        int NONE = 3;
    }

    /**
     * Result codes from validating a lock. No flags (0) indicates success.
     */
    @IntDef(flag = true, value = {
            ValidateLockFlags.INVALID_LENGTH,
            ValidateLockFlags.INVALID_BAD_SYMBOLS,
            ValidateLockFlags.INVALID_LACKS_COMPLEXITY,
            ValidateLockFlags.INVALID_GENERIC
    })
    @interface ValidateLockFlags {
        int INVALID_LENGTH = 1 << 0;
        int INVALID_BAD_SYMBOLS = 1 << 1;
        int INVALID_LACKS_COMPLEXITY = 1 << 2;
        int INVALID_GENERIC = 1 << 3;
    }

    /**
     * Result codes from attempting to set a lock.
     */
    @IntDef({
            SetLockCodes.SUCCESS,
            SetLockCodes.FAIL_LOCK_EXISTS,
            SetLockCodes.FAIL_LOCK_INVALID,
            SetLockCodes.FAIL_LOCK_GENERIC
    })
    @interface SetLockCodes {
        int SUCCESS = 1;
        int FAIL_LOCK_EXISTS = -1;
        int FAIL_LOCK_INVALID = -2;
        int FAIL_LOCK_GENERIC = -3;
    }

    /** PasswordComplexity as defined in DevicePolicyManager. */
    @IntDef({
        PasswordComplexity.PASSWORD_COMPLEXITY_NONE,
        PasswordComplexity.PASSWORD_COMPLEXITY_LOW,
        PasswordComplexity.PASSWORD_COMPLEXITY_MEDIUM,
        PasswordComplexity.PASSWORD_COMPLEXITY_HIGH,
    })
    @interface PasswordComplexity {
        int PASSWORD_COMPLEXITY_NONE = 0;
        int PASSWORD_COMPLEXITY_LOW = 0x10000;
        int PASSWORD_COMPLEXITY_MEDIUM = 0x30000;
        int PASSWORD_COMPLEXITY_HIGH = 0x50000;
    }
}

