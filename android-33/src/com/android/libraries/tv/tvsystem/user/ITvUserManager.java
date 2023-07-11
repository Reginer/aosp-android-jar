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

import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;

import java.util.List;

/**
 * Provides access to hidden {@link android.os.UserManager} APIs.
 * All methods here are user handle aware - they act on the user that owns the passed in Context.
 */
public interface ITvUserManager {
    @Nullable
    UserHandle createManagedProfile(@NonNull String name, @Nullable String[] disallowedPackages)
            throws UserManager.UserOperationException;

    @NonNull
    List<UserHandle> getUserProfiles(boolean enabledOnly);

    @NonNull
    String getUserName();

    void setUserName(@Nullable String name);

    @Nullable
    Bitmap getUserIcon();

    void setUserIcon(@NonNull Bitmap icon);

    boolean isManagedProfile();
}
