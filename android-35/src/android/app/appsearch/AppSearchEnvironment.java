/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.app.appsearch;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.UserHandle;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An interface which exposes environment specific methods for AppSearch.
 *
 * @hide
 */
public interface AppSearchEnvironment {

    /** Returns the directory to initialize appsearch based on the environment. */
    File getAppSearchDir(@NonNull Context context, @Nullable UserHandle userHandle);

    /** Returns the correct context for the user based on the environment. */
    Context createContextAsUser(@NonNull Context context, @NonNull UserHandle userHandle);

    /** Returns an ExecutorService based on given parameters. */
    ExecutorService createExecutorService(
            int corePoolSize,
            int maxConcurrency,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            int priority);

    /** Returns an ExecutorService with a single thread. */
    ExecutorService createSingleThreadExecutor();

    /**
     * Returns a cache directory for creating temporary files like in case of migrating documents.
     */
    @Nullable
    File getCacheDir(@NonNull Context context);

    /** Invalid UID constant duplicated for code-sync with GMSCore */
    int getInvalidUid();

    /** Creates and returns an Executor with cached thread pools. */
    @NonNull
    ExecutorService createCachedThreadPoolExecutor();

    /** Returns if we can log INFO level logs. */
    boolean isInfoLoggingEnabled();
}
