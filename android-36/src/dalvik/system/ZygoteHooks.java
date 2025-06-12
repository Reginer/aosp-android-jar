/*
 * Copyright (C) 2006 The Android Open Source Project
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

package dalvik.system;

import static android.annotation.SystemApi.Client.MODULE_LIBRARIES;

import android.annotation.SystemApi;
import android.icu.util.ULocale;

import dalvik.annotation.compat.VersionCodes;

import libcore.icu.DecimalFormatData;
import libcore.icu.ICU;
import libcore.icu.SimpleDateFormatData;

import java.io.File;
import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.ClassNotFoundException;
import java.lang.NoSuchMethodException;
import java.lang.ReflectiveOperationException;
import java.text.Collator;
import java.util.Locale;

import sun.util.locale.BaseLocale;

/**
 * Provides hooks for the zygote to call back into the runtime to perform
 * parent or child specific initialization..
 *
 * @hide
 */
@SystemApi(client = MODULE_LIBRARIES)
public final class ZygoteHooks {
    private static long token;
    private static Method enableMemoryMappedDataMethod;
    private static boolean inZygoteProcess = true;

    /** All methods are static, no need to instantiate. */
    private ZygoteHooks() {
    }

    /**
     * Called by the zygote when starting up. It marks the point when any thread
     * start should be an error, as only internal daemon threads are allowed there.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static native void startZygoteNoThreadCreation();

    /**
     * Called when the zygote begins preloading classes and data.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void onBeginPreload() {
        com.android.i18n.system.ZygoteHooks.onBeginPreload();

        ICU.initializeCacheInZygote();
        DecimalFormatData.initializeCacheInZygote();
        SimpleDateFormatData.initializeCacheInZygote();
        Collator.getInstance();

        // Look up JaCoCo on the boot classpath, if it exists. This will be used later for enabling
        // memory-mapped Java coverage.
        try {
          Class<?> jacocoOfflineClass = Class.forName("org.jacoco.agent.rt.internal.Offline");
          enableMemoryMappedDataMethod = jacocoOfflineClass.getMethod("enableMemoryMappedData");
        } catch (ClassNotFoundException e) {
          // JaCoCo was not on the boot classpath, so this is not a coverage build.
        } catch (NoSuchMethodException e) {
          // Method was not found in the JaCoCo Offline class. The version of JaCoCo is not
          // compatible with memory-mapped coverage.
          throw new RuntimeException(e);
        }
    }

    /**
     * Called when the zygote has completed preloading classes and data.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void onEndPreload() {
        // TODO(b/395108129): Switch to individual modules profile for preloading HttpEngine for
        // devices from S -> V. This should be a cleaner way to do preloading without having the
        // code live in libcore.
        // This should not exist here but there's no other place to preload HttpEngine for devices
        // predating Android B.
        // SdkExtensionLevel 16 is where `preload` API was introduced to HttpEngine.
        // Explicitly avoid calling this for B+ as it will be called in the ZygoteInit code.
        if (VMRuntime.getSdkExtensionSLevel() >= 16
                && VMRuntime.getSdkVersion() <= VersionCodes.VANILLA_ICE_CREAM) {
            try {
                // Reflection is used here because libcore must not depend explicitly on
                // the connectivity module as this will create a cyclic dependency in the build
                // graph. It's fine to call this method via reflection as it's a single static
                // method.
                Class.forName("android.net.http.HttpEngine").getMethod("preload").invoke(null);
            } catch (ClassNotFoundException
                    | NoSuchMethodException
                    | IllegalAccessException
                    | InvocationTargetException e) {
                // ClassNotFoundException, NoSuchMethodException and IllegalAccessException should
                // in theory never be thrown as we'll make sure that the method exists in the
                // specified class and it's accessible always. Still, we swallow the exception as we
                // don't want to crash the device on this. InvocationTargetException
                // will be thrown as preloading HttpEngine more than once throws an exception.
                // And this should never happen but at the moment, this will keep happening as
                // HttpEngine is being preloaded twice from B+ path and this code path.
                // This should go away once the SdkVersion gets bumped and the codepaths become
                // disjoint.
            }
        }
        com.android.i18n.system.ZygoteHooks.onEndPreload();

        // Clone standard descriptors as originals closed / rebound during zygote post fork.
        FileDescriptor.in.cloneForFork();
        FileDescriptor.out.cloneForFork();
        FileDescriptor.err.cloneForFork();
    }

    /**
     * Called after GC but before fork, it cleans stale cache entries in
     * BaseLocale and Locale, so to avoid the cleaning to happen in every
     * child process.
     */
    private static void cleanLocaleCaches() {
        BaseLocale.cleanCache();
        Locale.cleanCache();

        // Invoke android.icu.impl.locale.BaseLocale.CACHE#cleanStaleEntries() without
        // using a new API on S. LocaleObjectCacheTest should verify this.
        // en_US locale is chosen because it's likely to be cached, and doesn't require a
        // new BaseLocale.
        new ULocale.Builder().setLanguage("en").setRegion("US").build();
    }

    /**
     * Runs several special GCs to try to clean up a few generations of
     * softly- and final-reachable objects, along with any other garbage.
     * This is only useful just before a fork().
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void gcAndFinalize() {
        final VMRuntime runtime = VMRuntime.getRuntime();

        /* runFinalizationSync() lets finalizers be called in Zygote,
         * which doesn't have a HeapWorker thread.
         */
        System.gc();
        runtime.runFinalizationSync();
        cleanLocaleCaches();
        System.gc();
    }

    /**
     * Called by the zygote when startup is finished. It marks the point when it is
     * conceivable that threads would be started again, e.g., restarting daemons.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static native void stopZygoteNoThreadCreation();

    /**
     * Called by the zygote prior to every fork. Each call to {@code preFork}
     * is followed by a matching call to {@link #postForkChild(int, boolean, boolean, String)} on
     * the child process and {@link #postForkCommon()} on both the parent and the child
     * process. {@code postForkCommon} is called after {@code postForkChild} in
     * the child process.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void preFork() {
        Daemons.stop();
        // At this point Daemons have been joined, but they may still be Unregister()ing.
        // We wait for that as part of nativePreFork().
        token = nativePreFork();
    }

    /**
     * Called by the zygote in the system server process after forking. This method is is called
     * before {@code postForkChild} for system server.
     *
     * @param runtimeFlags The flags listed in com.android.internal.os.Zygote passed to the runtime.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void postForkSystemServer(int runtimeFlags) {
        nativePostForkSystemServer(runtimeFlags);
    }

    /**
     * Called by the zygote in the child process after every fork.
     *
     * @param runtimeFlags The runtime flags to apply to the child process.
     * @param isSystemServer Whether the child process is system server.
     * @param isChildZygote Whether the child process is a child zygote.
     * @param instructionSet The instruction set of the child, used to determine
     *                       whether to use a native bridge.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void postForkChild(int runtimeFlags, boolean isSystemServer,
            boolean isChildZygote, String instructionSet) {
        nativePostForkChild(token, runtimeFlags, isSystemServer, isChildZygote, instructionSet);
        if (!isChildZygote) {
          inZygoteProcess = false;
        }

        Math.setRandomSeedInternal(System.currentTimeMillis());

        // Enable memory-mapped coverage if JaCoCo is in the boot classpath. system_server is
        // skipped due to being persistent and having its own coverage writing mechanism.
        // Child zygote processes are also skipped so that file descriptors are not kept open
        // when the child zygote process forks again.
        if (!isSystemServer && !isChildZygote && enableMemoryMappedDataMethod != null) {
          try {
            enableMemoryMappedDataMethod.invoke(null);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
          }
        }
    }

    /**
     * Called by the zygote in both the parent and child processes after
     * every fork. In the child process, this method is called after
     * {@code postForkChild}.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void postForkCommon() {
        // Notify the runtime before creating new threads.
        nativePostZygoteFork();
        Daemons.startPostZygoteFork();
    }

    /**
     * Is it safe to keep all ART daemon threads stopped indefinitely in the zygote?
     * The answer may change from false to true dynamically, but not in the other
     * direction. Only called in Zygote.
     *
     * @return {@code true} if it's safe to keep all ART daemon threads stopped
     *         indefinitely in the zygote; and {@code false} otherwise
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static boolean isIndefiniteThreadSuspensionSafe() {
        return nativeZygoteLongSuspendOk();
    }

    /**
     * Are we still in a zygote?
     * @hide
     */
    public static boolean inZygote() {
      return inZygoteProcess;
    }

    // Hook for SystemServer specific early initialization post-forking.
    private static native void nativePostForkSystemServer(int runtimeFlags);

    private static native long nativePreFork();
    private static native void nativePostZygoteFork();

    // Hook for all child processes post forking.
    private static native void nativePostForkChild(long token, int runtimeFlags,
                                                   boolean isSystemServer, boolean isZygote,
                                                   String instructionSet);

    private static native boolean nativeZygoteLongSuspendOk();
}
