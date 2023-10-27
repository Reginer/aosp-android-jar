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

package android.net.connectivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;

/**
 * Utility providing limited access to module-internal APIs which are only available on Android T+,
 * as this class is only in the bootclasspath on T+ as part of framework-connectivity.
 *
 * R+ module components like Tethering cannot depend on all hidden symbols from
 * framework-connectivity. They only have access to stable API stubs where newer APIs can be
 * accessed after an API level check (enforced by the linter), or to limited hidden symbols in this
 * class which is also annotated with @RequiresApi (so API level checks are also enforced by the
 * linter).
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class TiramisuConnectivityInternalApiUtil {

    /**
     * Get a service binder token for
     * {@link com.android.server.connectivity.wear.CompanionDeviceManagerProxyService}.
     */
    public static IBinder getCompanionDeviceManagerProxyService(Context ctx) {
        final ConnectivityManager cm = ctx.getSystemService(ConnectivityManager.class);
        return cm.getCompanionDeviceManagerProxyService();
    }
}
