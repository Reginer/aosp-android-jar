/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.adservices;

import android.adservices.common.AdServicesStatusUtils;

/**
 * Result Code for log call result.
 *
 * @hide
 * @deprecated use {@link AdServicesStatusUtils} instead.
 */
public class ResultCode {
    /** Result code for success */
    public static final int RESULT_OK = 0;
    /** Result code for fail. */
    public static final int RESULT_INTERNAL_ERROR = 1;
    /** Result code for unauthorized API call. */
    public static final int RESULT_UNAUTHORIZED_CALL = 2;
    /** Result code for invalid argument. */
    public static final int RESULT_INVALID_ARGUMENT = 3;
    /** Result code for I/O error. */
    public static final int RESULT_IO_ERROR = 4;
    /** Result code for Rate Limit Reached. */
    public static final int RESULT_RATE_LIMIT_REACHED = 5;
}
