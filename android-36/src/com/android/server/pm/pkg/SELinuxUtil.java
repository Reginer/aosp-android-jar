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

package com.android.server.pm.pkg;

import com.android.internal.pm.pkg.SEInfoUtil;

/**
 * Utility methods that need to be used in application space.
 * @hide
 */
public final class SELinuxUtil {

    /** Append to existing seinfo label for instant apps @hide */
    private static final String INSTANT_APP_STR = SEInfoUtil.INSTANT_APP_STR;

    /** Append to existing seinfo when modifications are complete @hide */
    public static final String COMPLETE_STR = SEInfoUtil.COMPLETE_STR;

    /** @hide */
    public static String getSeinfoUser(PackageUserState userState) {
        if (userState.isInstantApp()) {
           return INSTANT_APP_STR + COMPLETE_STR;
        }
        return COMPLETE_STR;
    }
}
