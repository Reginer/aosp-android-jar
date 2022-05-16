/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.setupwizardlib.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.ConcurrentHashMap;

/** A registry of singleton-like helpers, which can be injected by the application for testing. */
public class CarHelperRegistry {

    /**
     * Interface that creates the helper.  Typically a constructor that takes a context.
     *
     * @param <H> The helper class.
     */
    public interface HelperCreator<H> {

        /** Returns an instance of the helper class.  */
        @NonNull
        H createHelper(@NonNull Context appContext);
    }

    private static final CarHelperRegistry GLOBAL_REGISTRY = new CarHelperRegistry();

    private final ConcurrentHashMap<Class<?>, Object> mMap = new ConcurrentHashMap<>();

    /**
     * Query the application context for an injected registry, or return the global registry if one
     * doesn't exist. Normal runs would not have an injected registry and therefore use the global
     * one, while test runs will typically inject a registry so it can inject individual helpers for
     * testing.
     *
     * @param context The context to get the registry from.
     * @return The registry injected by the application context if one exists, or the global
     *     registry.
     */
    @NonNull
    @VisibleForTesting
    static synchronized CarHelperRegistry getRegistry(@NonNull Context context) {
        final Context applicationContext = context.getApplicationContext();
        if (applicationContext instanceof CarHelperInjectionContext) {
            return ((CarHelperInjectionContext) applicationContext).getCarHelperRegistry();
        }
        return GLOBAL_REGISTRY;
    }

    /**
     * Get the helper from the registry if it exists, or create and put one into the registry if it
     * does not exist.
     *
     * <p>Since helpers are singleton-like, the context passed to the creator is always the
     * application context so it doesn't leak local contexts pass their lifecycles.
     *
     * @param context The context to create the helper with. This can be any context as this method
     *     will call {@link Context#getApplicationContext()} to ensure the application context is
     *     used.
     * @param cls The helper class.
     * @param creator The method to create the helper. Typically if the helper has a constructor
     *     that takes a context, this creator will be {@code MyHelper::new}.
     * @param <H> The helper class.
     * @return The helper in the registry, either existing or newly registered.
     */
    @NonNull
    public static <H> H getOrCreateWithAppContext(
            @NonNull Context context, @NonNull Class<H> cls, @NonNull HelperCreator<H> creator) {
        return CarHelperRegistry.getRegistry(context)
                .getOrCreateHelper(context.getApplicationContext(), cls, creator);
    }

    /**
     * Retrieve a helper from the registry.
     *
     * @param cls The helper class.
     * @param <H> The helper class.
     * @return The helper of the given {@code cls}, or null if no helper of {@code cls} is
     *     registered.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    @VisibleForTesting
    <H> H getHelper(@NonNull Class<H> cls) {
        return (H) mMap.get(cls);
    }

    /** @see #getOrCreateWithAppContext(Context, Class, HelperCreator) */
    @NonNull
    @VisibleForTesting
    <H> H getOrCreateHelper(
            @NonNull Context appContext, @NonNull Class<H> cls, @NonNull HelperCreator<H> creator) {
        // Synchronize on the class to ensure only creator is only called once (per classloader)
        synchronized (cls) {
            H helper = getHelper(cls);
            if (helper == null) {
                helper = creator.createHelper(appContext);
                putHelper(cls, helper);
            }
            return helper;
        }
    }

    @VisibleForTesting
    int size() {
        return mMap.size();
    }

    /**
     * Put a helper for the given {@code cls} into the registry. Outside of this class, this should
     * only be used to inject helper for testing purposes. Singletons should use {@link
     * #getOrCreateWithAppContext(Context, Class, HelperCreator)} to create the instance which will
     *
     * @param cls The helper class.
     * @param helper The helper instance to add to the registry.
     * @param <H> The helper class.
     */
    public <H> void putHelper(@NonNull Class<H> cls, @NonNull H helper) {
        mMap.put(cls, helper);
    }
}
