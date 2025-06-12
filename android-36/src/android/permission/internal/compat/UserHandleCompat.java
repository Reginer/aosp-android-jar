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

package android.permission.internal.compat;

import android.annotation.UserIdInt;
import android.os.UserHandle;

/**
 * Helper for accessing features in {@link UserHandle}.
 */
public final class UserHandleCompat {
    /**
     * A user ID to indicate all users on the device.
     */
    public static final int USER_ALL = UserHandle.ALL.getIdentifier();

    /**
     * A user ID to indicate an undefined user of the device.
     *
     * @see UserHandle#USER_NULL
     */
    public static final @UserIdInt int USER_NULL = -10000;

    /**
     * A user ID to indicate the "system" user of the device.
     */
    public static final int USER_SYSTEM = UserHandle.SYSTEM.getIdentifier();

    private UserHandleCompat() {}

    /**
     * Get the user ID of a given UID.
     *
     * @param uid the UID
     * @return the user ID
     */
    @UserIdInt
    public static int getUserId(int uid) {
        return UserHandle.getUserHandleForUid(uid).getIdentifier();
    }

    /**
     * Get the UID from the give user ID and app ID
     *
     * @param userId the user ID
     * @param appId the app ID
     * @return the UID
     */
    public static int getUid(@UserIdInt int userId, int appId) {
        return UserHandle.of(userId).getUid(appId);
    }
}
