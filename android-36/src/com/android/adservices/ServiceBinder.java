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
package com.android.adservices;

import android.annotation.Nullable;
import android.content.Context;
import android.os.IBinder;

import java.util.function.Function;

/**
 * Abstracts how to find and connect the service binder object.
 *
 * @param <T> The type of Service Binder.
 *
 * @hide
 */
public abstract class ServiceBinder<T> {

    // TODO(b/336558146): document when it returns null or throws ServiceUnavailableException
    /** Get the binder service. */
    @Nullable
    public abstract T getService();

    /**
     * The service is in an APK (as opposed to the system service), unbind it from the service to
     * allow the APK process to die.
     */
    public abstract void unbindFromService();

    /** Gets the {@link ServiceBinder} suitable for the configuration. */
    public static <T2> ServiceBinder<T2> getServiceBinder(
            Context context, String serviceIntentAction, Function<IBinder, T2> converter) {
        return new AndroidServiceBinder<>(context, serviceIntentAction, converter);
    }
}
