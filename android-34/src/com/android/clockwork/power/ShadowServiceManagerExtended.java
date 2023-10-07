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

package com.android.clockwork.power;

import static android.os.IServiceManager.DUMP_FLAG_PRIORITY_DEFAULT;

import android.os.IBinder;
import android.os.ServiceManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadows.ShadowServiceManager;

import java.util.HashMap;
import java.util.Map;

/**
 * ShadowServiceManagerExtended provides shadow implementations for
 * {@link ServiceManager#addService} and {@link ServiceManager#getService}
 * which register and fetch services from the shadow implementation respectively.
 * <p>
 * ShadowServiceManager implements these two methods but does nothing in the method body.
 * ShadowServiceManagerExtended makes it possible for unit tests to capture any services which
 * are added by code under test. Similarly, it makes it possible for unit tests to add services
 * so that they are available to code under test.
 */
@Implements(value = ServiceManager.class, isInAndroidSdk = false)
public class ShadowServiceManagerExtended extends ShadowServiceManager {
    private static final Map<String, IBinder> EXTRA_SERVICES = new HashMap<>();

    @Implementation
    protected static void addService(String name, IBinder service) {
        addService(name, service, false, DUMP_FLAG_PRIORITY_DEFAULT);
    }

    @Implementation
    protected static void addService(String name, IBinder service, boolean allowIsolated,
            int dumpPriority) {
        EXTRA_SERVICES.put(name, service);
    }

    @Implementation
    protected static IBinder getService(String name) {
        final IBinder iBinder = ShadowServiceManager.getService(name);
        if (iBinder == null) {
            return EXTRA_SERVICES.get(name);
        }
        return iBinder;
    }

    @Resetter
    public static void reset() {
        ShadowServiceManager.reset();
        EXTRA_SERVICES.clear();
    }
}
