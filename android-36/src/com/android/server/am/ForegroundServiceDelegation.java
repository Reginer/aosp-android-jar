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

package com.android.server.am;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ForegroundServiceDelegationOptions;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

/**
 * A foreground service delegate which has client options and connection callback.
 */
public class ForegroundServiceDelegation {
    public final IBinder mBinder = new Binder();
    @NonNull
    public final ForegroundServiceDelegationOptions mOptions;
    @Nullable
    public final ServiceConnection mConnection;

    public ForegroundServiceDelegation(@NonNull ForegroundServiceDelegationOptions options,
            @Nullable ServiceConnection connection) {
        mOptions = options;
        mConnection = connection;
    }
}
