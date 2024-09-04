/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.app.sdksandbox.sdkprovider;

import android.annotation.FlaggedApi;

import com.android.sdksandbox.flags.Flags;

import java.util.concurrent.Executor;

/**
 * Used to notify the SDK about changes in the client's {@link
 * android.app.ActivityManager.RunningAppProcessInfo#importance}.
 *
 * <p>When an SDK wants to get notified about changes in client's importance, it should register an
 * implementation of this interface by calling {@link
 * SdkSandboxController#registerSdkSandboxClientImportanceListener(Executor,
 * SdkSandboxClientImportanceListener)}.
 */
@FlaggedApi(Flags.FLAG_SANDBOX_CLIENT_IMPORTANCE_LISTENER)
public interface SdkSandboxClientImportanceListener {
    /**
     * Invoked every time the client transitions from a value <= {@link
     * android.app.ActivityManager.RunningAppProcessInfo#IMPORTANCE_FOREGROUND} to a higher value or
     * vice versa
     *
     * @param isForeground true when the client transitions to {@link
     *     android.app.ActivityManager.RunningAppProcessInfo#IMPORTANCE_FOREGROUND} or lower and
     *     false when it is the other way round.
     */
    @FlaggedApi(Flags.FLAG_SANDBOX_CLIENT_IMPORTANCE_LISTENER)
    void onForegroundImportanceChanged(boolean isForeground);
}
