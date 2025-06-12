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

package android.app.sdksandbox;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.util.ArrayMap;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import java.util.Map;

/**
 * Maintains a set of services which {@code Manager} classes should have a {@link
 * SandboxedSdkContext} in case they are running inside a {@code sdk_sandbox} process.
 *
 * <p>This class is required to work around the fact that {@link
 * android.content.ContextWrapper#getSystemService(String)} delegates the call to the base context,
 * and reimplementing this method in the {@link SandboxedSdkContext} will require accessing hidden
 * APIs, which is forbidden for Mainline modules.
 *
 * <p>Manager classes that want to behave differently in case they are initiated from inside a
 * {@code sdk_sandbox} process are expected to call {@link #registerServiceMutator(String,
 * ServiceMutator)} in their initialization code, e.g. {@link
 * android.adservices.AdServicesFrameworkInitializer#registerServiceWrappers()}. When a {@code SDK}
 * running inside a {@code sdk_sandbox} process requests a "sdk sandbox aware" manager via {@link
 * SandboxedSdkContext#getSystemService(String)} the code inside {@link
 * SandboxedSdkContext#getSystemService(String)} will use the registered {@link ServiceMutator} to
 * set the correct context.
 *
 * @hide
 */
// TODO(b/242889021): limit this class only to Android T, on U+ we should implement the proper
//  platform support.
public final class SdkSandboxSystemServiceRegistry {

    @VisibleForTesting
    public SdkSandboxSystemServiceRegistry() {}

    private static final Object sLock = new Object();

    @GuardedBy("sLock")
    private static SdkSandboxSystemServiceRegistry sInstance = null;

    /** Returns an instance of {@link SdkSandboxSystemServiceRegistry}. */
    @NonNull
    public static SdkSandboxSystemServiceRegistry getInstance() {
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new SdkSandboxSystemServiceRegistry();
            }
            return sInstance;
        }
    }

    @GuardedBy("mServiceMutators")
    private final Map<String, ServiceMutator> mServiceMutators = new ArrayMap<>();

    /**
     * Adds a {@code mutator} for the service with given {@code serviceName}.
     *
     * <p>This {@code mutator} will be applied inside the {@link
     * SandboxedSdkContext#getSystemService(String)} method.
     */
    public void registerServiceMutator(
            @NonNull String serviceName, @NonNull ServiceMutator mutator) {
        synchronized (mServiceMutators) {
            mServiceMutators.put(serviceName, mutator);
        }
    }

    /**
     * Returns a {@link ServiceMutator} for the given {@code serviceName}, or {@code null} if this
     * {@code serviceName} doesn't have a mutator registered.
     */
    @Nullable
    public ServiceMutator getServiceMutator(@NonNull String serviceName) {
        synchronized (mServiceMutators) {
            return mServiceMutators.get(serviceName);
        }
    }

    /**
     * A functional interface representing a method on a {@code Manager} class to set the context.
     *
     * <p>This interface is required in order to break the circular dependency between {@code
     * framework-sdsksandbox} and {@code framework-adservices} build targets.
     */
    public interface ServiceMutator {

        /** Sets a {@code context} on the given {@code service}. */
        @NonNull
        Object setContext(@NonNull Object service, @NonNull Context context);
    }
}
