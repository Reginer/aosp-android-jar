/*
 * Copyright (C) 2007 Esmertec AG.
 * Copyright (C) 2007 The Android Open Source Project
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

package com.google.android.mms;

import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;

/**
 * A generic exception that is thrown by the Mms client.
 */
public class MmsException extends Exception {
    private static final long serialVersionUID = -7323249827281485390L;

    /**
     * Creates a new MmsException.
     */
    @UnsupportedAppUsage
    public MmsException() {
        super();
    }

    /**
     * Creates a new MmsException with the specified detail message.
     *
     * @param message the detail message.
     */
    @UnsupportedAppUsage
    public MmsException(String message) {
        super(message);
    }

    /**
     * Creates a new MmsException with the specified cause.
     *
     * @param cause the cause.
     */
    @UnsupportedAppUsage
    public MmsException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new MmsException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the cause.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public MmsException(String message, Throwable cause) {
        super(message, cause);
    }
}
