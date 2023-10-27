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

package android.app.sdksandbox.sdkprovider;

import android.annotation.NonNull;
import android.app.Activity;
import android.os.Build;
import android.os.IBinder;
import android.view.View;

import androidx.annotation.RequiresApi;

/**
 * This is used to notify the SDK when an {@link Activity} is created for it.
 *
 * <p>When an SDK wants to start an {@link Activity}, it should register an implementation of this
 * class by calling {@link
 * SdkSandboxController#registerSdkSandboxActivityHandler(SdkSandboxActivityHandler)} that will
 * return an {@link android.os.IBinder} identifier for the registered {@link
 * SdkSandboxActivityHandler} to The SDK.
 *
 * <p>The SDK should be notified about the {@link Activity} creation by calling {@link
 * SdkSandboxActivityHandler#onActivityCreated(Activity)} which happens when the caller app calls
 * {@link android.app.sdksandbox.SdkSandboxManager#startSdkSandboxActivity(Activity, IBinder)} using
 * the same {@link IBinder} identifier for the registered {@link SdkSandboxActivityHandler}.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public interface SdkSandboxActivityHandler {
    /**
     * Notifies SDK when an {@link Activity} gets created.
     *
     * <p>This function is called synchronously from the main thread of the {@link Activity} that is
     * getting created.
     *
     * <p>SDK is expected to call {@link Activity#setContentView(View)} to the passed {@link
     * Activity} object to populate the view.
     *
     * @param activity the {@link Activity} gets created
     */
    void onActivityCreated(@NonNull Activity activity);
}
