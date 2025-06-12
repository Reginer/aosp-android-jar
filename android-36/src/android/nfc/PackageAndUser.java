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

package android.nfc;

import android.annotation.UserIdInt;

import java.util.Objects;

/**
 * @hide
 */
public class PackageAndUser {
    @UserIdInt private final int mUserId;
    private String mPackage;

    public PackageAndUser(@UserIdInt int userId, String pkg) {
        mUserId = userId;
        mPackage = pkg;
    }

    /**
     * @hide
     */
    public int describeContents() {
        return 0;
    }

    @UserIdInt
    public int getUserId() {
        return mUserId;
    }

    public String getPackage() {
        return mPackage;
    }

    @Override
    public String toString() {
        return mPackage + " for user id: " + mUserId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof PackageAndUser) {
            PackageAndUser other = (PackageAndUser) obj;
            return other.getUserId() == mUserId
                    && Objects.equals(other.getPackage(), mPackage);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (mPackage == null) {
            return mUserId;
        }
        return mPackage.hashCode() + mUserId;
    }
}
