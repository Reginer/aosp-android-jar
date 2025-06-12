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
package com.android.federatedcompute.internal.util;

import android.content.Context;
import android.os.IBinder;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Abstracts how to find and connect the service binder object.
 *
 * @param <T> The type of Service Binder.
 * @hide
 */
public abstract class AbstractServiceBinder<T> {
    /** Get the {@link AbstractServiceBinder} suitable for the configuration. */
    public static <T2> AbstractServiceBinder<T2> getServiceBinderByIntent(
            Context context,
            String serviceIntentAction,
            String servicePackage,
            Function<IBinder, T2> converter) {
        return new AndroidServiceBinder<>(
                context, serviceIntentAction, servicePackage, converter);
    }

    /** Get the {@link AbstractServiceBinder} suitable for the configuration. */
    public static <T2> AbstractServiceBinder<T2> getServiceBinderByServiceName(
            Context context,
            String serviceName,
            String servicePackage,
            Function<IBinder, T2> converter) {
        return new AndroidServiceBinder<>(
                context, serviceName, servicePackage, true, converter);
    }

    /** Get the {@link AbstractServiceBinder} suitable for the configuration. */
    public static <T2> AbstractServiceBinder<T2> getServiceBinderByIntent(
            Context context,
            String serviceIntentAction,
            List<String> servicePackages,
            Function<IBinder, T2> converter) {
        return new AndroidServiceBinder<>(
                context, serviceIntentAction, servicePackages, converter);
    }

    /** Get the {@link AbstractServiceBinder} suitable for the configuration. */
    public static <T2> AbstractServiceBinder<T2> getServiceBinderByIntent(
            Context context,
            String serviceIntentAction,
            List<String> servicePackages,
            int bindFlags,
            Function<IBinder, T2> converter) {
        return new AndroidServiceBinder<>(
                context, serviceIntentAction, servicePackages, bindFlags, converter);
    }

    /** Get the {@link AbstractServiceBinder} suitable for an isolated service by name. */
    public static <T2> AbstractServiceBinder<T2> getIsolatedServiceBinderByServiceName(
            Context context,
            String serviceName,
            String servicePackage,
            String isolatedProcessName,
            int bindFlags,
            Function<IBinder, T2> converter) {
        return new AndroidServiceBinder<>(
                context, serviceName, servicePackage, isolatedProcessName,
                /* enableLookupByName= */ true,
                bindFlags,
                converter);
    }

    /** Get the binder service. */
    public abstract T getService(Executor executor);

    /**
     * The service is in an APK (as opposed to the system service), unbind it from the service to
     * allow the APK process to die.
     */
    public abstract void unbindFromService();
}
