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
 * limitations under the License
 */
package com.android.libraries.tv.tvsystem.user;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.internal.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TvUserManager implements ITvUserManager {
    private final UserManager mUserManager;
    private final int mUserId;

    private final Set<String> mRequiredSystemApps = new HashSet<>();
    private final Set<String> mDisallowedSystemApps = new HashSet<>();

    public TvUserManager(Context context) {
        mUserId = context.getUserId();
        mUserManager = context.getSystemService(UserManager.class);
        populateSystemAppsList(context.getResources());
    }

    private void populateSystemAppsList(@NonNull Resources resources) {
        Collections.addAll(mRequiredSystemApps,
                resources.getStringArray(R.array.required_apps_managed_profile));
        Collections.addAll(mRequiredSystemApps,
                resources.getStringArray(R.array.vendor_required_apps_managed_profile));

        Collections.addAll(mDisallowedSystemApps,
                resources.getStringArray(R.array.disallowed_apps_managed_profile));
        Collections.addAll(mDisallowedSystemApps,
                resources.getStringArray(R.array.vendor_disallowed_apps_managed_profile));
    }

    @Override
    public UserHandle createManagedProfile(@NonNull String name,
            @Nullable String[] disallowedPackages) {
        UserInfo profileInfo = mUserManager.createProfileForUser(name,
                UserManager.USER_TYPE_PROFILE_MANAGED, 0 /* flags */, mUserId,
                filterDisallowedPackages(disallowedPackages));
        if (profileInfo != null) {
            return profileInfo.getUserHandle();
        }

        return null;
    }

    @NonNull
    private String[] filterDisallowedPackages(@Nullable String[] disallowedPackages) {
        List<String> packages = new ArrayList<>();

        if (disallowedPackages != null) {
            Collections.addAll(packages, disallowedPackages);
        }

        packages.addAll(mDisallowedSystemApps);
        packages.removeAll(mRequiredSystemApps);

        return packages.toArray(new String[0]);
    }

    @Override
    @NonNull
    public List<UserHandle> getUserProfiles(boolean enabledOnly) {
        int[] userIds = mUserManager.getProfileIds(mUserId, enabledOnly);
        List<UserHandle> result = new ArrayList<>(userIds.length);
        for (int userId : userIds) {
            result.add(UserHandle.of(userId));
        }
        return result;
    }

    @Override
    @NonNull
    public String getUserName() {
        return mUserManager.getUserInfo(mUserId).name;
    }

    @Override
    public void setUserName(@Nullable String name) {
        mUserManager.setUserName(mUserId, name);
    }

    @Override
    @Nullable
    public Bitmap getUserIcon() {
        return mUserManager.getUserIcon(mUserId);
    }

    @Override
    public void setUserIcon(@NonNull Bitmap icon) {
        mUserManager.setUserIcon(mUserId, icon);
    }

    @Override
    public boolean isManagedProfile() {
        return mUserManager.isManagedProfile(mUserId);
    }
}
