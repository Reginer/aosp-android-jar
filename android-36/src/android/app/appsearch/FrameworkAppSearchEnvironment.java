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
import android.os.Environment;
import android.os.UserHandle;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Contains utility methods for Framework implementation of AppSearch.
 *
 * @hide
 */
public class FrameworkAppSearchEnvironment implements AppSearchEnvironment {

    /**
     * Returns AppSearch directory in the credential encrypted system directory for the given user.
     *
     * <p>This folder should only be accessed after unlock.
     */
    @Override
    public File getAppSearchDir(@NonNull Context unused, @NonNull UserHandle userHandle) {
        // Duplicates the implementation of Environment#getDataSystemCeDirectory
        // TODO(b/191059409): Unhide Environment#getDataSystemCeDirectory and switch to it.
        Objects.requireNonNull(userHandle);
        File systemCeDir = new File(Environment.getDataDirectory(), "system_ce");
        File systemCeUserDir = new File(systemCeDir, String.valueOf(userHandle.getIdentifier()));
        return new File(systemCeUserDir, "appsearch");
    }

    /** Creates context for the user based on the userHandle. */
    @Override
    public Context createContextAsUser(@NonNull Context context, @NonNull UserHandle userHandle) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(userHandle);
        return context.createContextAsUser(userHandle, /* flags= */ 0);
    }

    /** Creates and returns a ThreadPoolExecutor for given parameters. */
    @Override
    public ExecutorService createExecutorService(
            int corePoolSize,
            int maxConcurrency,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            int priority) {
        return new ThreadPoolExecutor(corePoolSize, maxConcurrency, keepAliveTime, unit, workQueue);
    }

    /** Createsand returns an ExecutorService with a single thread. */
    @Override
    public ExecutorService createSingleThreadExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    /** Creates and returns an Executor with cached thread pools. */
    @NonNull
    @Override
    public ExecutorService createCachedThreadPoolExecutor() {
        return Executors.newCachedThreadPool();
    }

    /**
     * Returns a cache directory for creating temporary files like in case of migrating documents.
     */
    @Override
    @Nullable
    public File getCacheDir(@NonNull Context context) {
        // Framework/Android does not have app-specific cache directory.
        return null;
    }

    /** Returns if we can log INFO level logs. */
    @Override
    public boolean isInfoLoggingEnabled() {
        return true;
    }

    /** Returns the {@code EnvironmentType} for this environment. */
    @Override
    public int getEnvironment() {
        return FRAMEWORK_ENVIRONMENT;
    }
}
