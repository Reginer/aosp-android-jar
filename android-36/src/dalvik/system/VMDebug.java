/*
 * Copyright (C) 2007 The Android Open Source Project
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
import android.annotation.UserIdInt;
import android.compat.annotation.UnsupportedAppUsage;

import libcore.util.NonNull;
import libcore.util.Nullable;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import dalvik.annotation.optimization.FastNative;

/**
 * Provides access to some VM-specific debug features. Though this class and
 * many of its members are public, this class is meant to be wrapped in a more
 * friendly way for use by application developers. On the Android platform, the
 * recommended way to access this functionality is through the class
 * <code>android.os.Debug</code>.
 *
 * @hide
 */
@SystemApi(client = MODULE_LIBRARIES)
public final class VMDebug {
    /**
     * flag for startMethodTracing(), which adds the results from
     * startAllocCounting to the trace key file.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    // Must match android.os.Debug.TRACE_COUNT_ALLOCS.
    public static final int TRACE_COUNT_ALLOCS = 1;

    /* constants for getAllocCount */
    private static final int KIND_ALLOCATED_OBJECTS     = 1<<0;
    private static final int KIND_ALLOCATED_BYTES       = 1<<1;
    private static final int KIND_FREED_OBJECTS         = 1<<2;
    private static final int KIND_FREED_BYTES           = 1<<3;
    private static final int KIND_GC_INVOCATIONS        = 1<<4;
    private static final int KIND_CLASS_INIT_COUNT      = 1<<5;
    private static final int KIND_CLASS_INIT_TIME       = 1<<6;
    private static final int KIND_EXT_ALLOCATED_OBJECTS = 1<<12;
    private static final int KIND_EXT_ALLOCATED_BYTES   = 1<<13;
    private static final int KIND_EXT_FREED_OBJECTS     = 1<<14;
    private static final int KIND_EXT_FREED_BYTES       = 1<<15;

    /**
     * Constant for {@link #getAllocCount(int)}
     * to get the number of all allocated objects.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int KIND_GLOBAL_ALLOCATED_OBJECTS =
        KIND_ALLOCATED_OBJECTS;

    /**
     * Constant for {@link #getAllocCount(int)}
     * to get the cumulative size of all objects allocated.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int KIND_GLOBAL_ALLOCATED_BYTES =
        KIND_ALLOCATED_BYTES;

    /**
     * Constant for {@link #getAllocCount(int)}
     * to get the number of freed objects.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int KIND_GLOBAL_FREED_OBJECTS =
        KIND_FREED_OBJECTS;

    /**
     * Constant for {@link #getAllocCount(int)}
     * to get the cumulative size of all freed objects.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int KIND_GLOBAL_FREED_BYTES =
        KIND_FREED_BYTES;

    /**
     * Constant for {@link #getAllocCount(int)}
     * to get the number of times an allocation triggered a blocking GC.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int KIND_GLOBAL_GC_INVOCATIONS =
        KIND_GC_INVOCATIONS;

    /**
     * Constant for {@link #getAllocCount(int)}
     * to get the number of initialized classes.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int KIND_GLOBAL_CLASS_INIT_COUNT =
        KIND_CLASS_INIT_COUNT;

    /**
     * Constant for {@link #getAllocCount(int)}
     * to get the cumulative time spent in class initialization.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int KIND_GLOBAL_CLASS_INIT_TIME =
        KIND_CLASS_INIT_TIME;

    /**
     * Constant for {@link #getAllocCount(int)}
     * to get the number of all allocated objects for current thread.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int KIND_THREAD_ALLOCATED_OBJECTS =
        KIND_ALLOCATED_OBJECTS << 16;

    /**
     * Constant for {@link #getAllocCount(int)}
     * to get the cumulative size of all objects allocated for current thread.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int KIND_THREAD_ALLOCATED_BYTES =
        KIND_ALLOCATED_BYTES << 16;

    /**
     * Constant for {@link #getAllocCount(int)}
     * to get the number of times an allocation triggered a blocking GC for current thread.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int KIND_THREAD_GC_INVOCATIONS =
        KIND_GC_INVOCATIONS << 16;

    /**
     * Constant for {@link #getAllocCount(int)} to get all possible stats.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static final int KIND_ALL_COUNTS = 0xffffffff;

    /* all methods are static */
    private VMDebug() {}

    /**
     * Request JDWP agent to suspend all Java Thread and send VM_START.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static native void suspendAllAndSendVmStart();

    /**
     * Returns the time since the last known debugger activity.
     *
     * @return the time in milliseconds, or -1 if the debugger is not connected
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @FastNative
    public static native long lastDebuggerActivity();

    /**
     * Determines if debugging is enabled in this VM.  If debugging is not
     * enabled, a debugger cannot be attached.
     *
     * @return true if debugging is enabled
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @FastNative
    public static native boolean isDebuggingEnabled();

    /**
     * Determines if a debugger is currently attached.
     *
     * @return true if (and only if) a debugger is connected
     *
     * @hide
     */
    @UnsupportedAppUsage
    @SystemApi(client = MODULE_LIBRARIES)
    @FastNative
    public static native boolean isDebuggerConnected();

    /**
     * Returns an array of strings that identify VM features.  This is
     * used by DDMS to determine what sorts of operations the VM can
     * perform.
     *
     * @return array of strings identifying VM features
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static native String[] getVmFeatureList();

    /**
     * Start method tracing, specifying a file name as well as a default
     * buffer size. See <a
     * href="{@docRoot}guide/developing/tools/traceview.html"> Running the
     * Traceview Debugging Program</a> for information about reading
     * trace files.
     *
     * <p>You can use either a fully qualified path and
     * name, or just a name. If only a name is specified, the file will
     * be created under the /sdcard/ directory. If a name is not given,
     * the default is /sdcard/dmtrace.trace.</p>
     *
     * @param traceFileName   name to give the trace file
     * @param bufferSize      the maximum size of both files combined. If passed
     *                        as {@code 0}, it defaults to 8MB.
     * @param flags           flags to control method tracing. The only one that
     *                        is currently defined is {@link #TRACE_COUNT_ALLOCS}.
     * @param samplingEnabled if true, sample profiling is enabled. Otherwise,
     *                        method instrumentation is used.
     * @param intervalUs      the time between samples in microseconds when
     *                        sampling is enabled.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void startMethodTracing(String traceFileName, int bufferSize, int flags, boolean samplingEnabled, int intervalUs) {
        startMethodTracingFilename(traceFileName, checkBufferSize(bufferSize), flags, samplingEnabled, intervalUs);
    }

    /**
     * Like {@link #startMethodTracing(String, int, int)}, but taking an already-opened
     * {@code FileDescriptor} in which the trace is written.  The file name is also
     * supplied simply for logging.  Makes a dup of the file descriptor.
     * Streams tracing data to the file if streamingOutput is true.
     *
     * @param traceFileName   name to give the trace file
     * @param fd              already opened {@code FileDescriptor} in which trace is written
     * @param bufferSize      the maximum size of both files combined. If passed
     *                        as {@code 0}, it defaults to 8MB.
     * @param flags           flags to control method tracing. The only one that
     *                        is currently defined is {@link #TRACE_COUNT_ALLOCS}.
     * @param samplingEnabled if true, sample profiling is enabled. Otherwise,
     *                        method instrumentation is used.
     * @param intervalUs      the time between samples in microseconds when
     *                        sampling is enabled.
     * @param streamingOutput streams tracing data to the duped {@code fd} file descriptor
     *                        if {@code streamingOutput} is {@code true}.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void startMethodTracing(String traceFileName, FileDescriptor fd, int bufferSize,
                                          int flags, boolean samplingEnabled, int intervalUs,
                                          boolean streamingOutput) {
        if (fd == null) {
            throw new NullPointerException("fd == null");
        }
        startMethodTracingFd(traceFileName, fd.getInt$(), checkBufferSize(bufferSize), flags,
                             samplingEnabled, intervalUs, streamingOutput);
    }

    /**
     * Starts method tracing without a backing file.  When {@link #stopMethodTracing()}
     * is called, the result is sent directly to DDMS.  (If DDMS is not
     * attached when tracing ends, the profiling data will be discarded.)
     *
     * @param bufferSize      the maximum size of both files combined. If passed
     *                        as {@code 0}, it defaults to 8MB.
     * @param flags           flags to control method tracing. The only one that
     *                        is currently defined is {@link #TRACE_COUNT_ALLOCS}.
     * @param samplingEnabled if true, sample profiling is enabled. Otherwise,
     *                        method instrumentation is used.
     * @param intervalUs      the time between samples in microseconds when
     *                        sampling is enabled.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void startMethodTracingDdms(int bufferSize, int flags, boolean samplingEnabled, int intervalUs) {
        startMethodTracingDdmsImpl(checkBufferSize(bufferSize), flags, samplingEnabled, intervalUs);
    }

    private static int checkBufferSize(int bufferSize) {
        if (bufferSize == 0) {
            // Default to 8MB per the documentation.
            bufferSize = 8 * 1024 * 1024;
        }
        if (bufferSize < 1024) {
            throw new IllegalArgumentException("buffer size < 1024: " + bufferSize);
        }
        return bufferSize;
    }

    private static native void startMethodTracingDdmsImpl(int bufferSize, int flags, boolean samplingEnabled, int intervalUs);
    private static native void startMethodTracingFd(String traceFileName, int fd, int bufferSize,
            int flags, boolean samplingEnabled, int intervalUs, boolean streamingOutput);
    private static native void startMethodTracingFilename(String traceFileName, int bufferSize, int flags, boolean samplingEnabled, int intervalUs);

    /**
     * Determine whether method tracing is currently active and what type is
     * active.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static native int getMethodTracingMode();

    /**
     * Stops method tracing.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static native void stopMethodTracing();

    /**
     * Get an indication of thread CPU usage. The value returned indicates the
     * amount of time that the current thread has spent executing code or
     * waiting for certain types of I/O.
     * <p>
     * The time is expressed in nanoseconds, and is only meaningful when
     * compared to the result from an earlier call. Note that nanosecond
     * resolution does not imply nanosecond accuracy.
     *
     * @return the CPU usage. A value of -1 means the system does not support
     *         this feature.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @FastNative
    public static native long threadCpuTimeNanos();

    /**
     * Starts counting the number and aggregate size of memory allocations.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static native void startAllocCounting();

    /**
     * Stops counting the number and aggregate size of memory allocations.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static native void stopAllocCounting();

    /**
     * Returns information on the number of objects allocated by the runtime between a
     * {@link #startAllocCounting() start} and {@link #stopAllocCounting() stop}.
     *
     * @param kind either {@code KIND_GLOBAL_*} or {@code KIND_THREAD_*}.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static native int getAllocCount(int kind);

    /**
     * Resets counting the number and aggregate size of memory allocations for the given kinds.
     *
     * @param kinds a union of {@code KIND_GLOBAL_*} and {@code KIND_THREAD_*}.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static native void resetAllocCount(int kinds);

    /**
     * Represents the location of a java method within a process'
     * memory.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static class ExecutableMethodFileOffsets {
        private final @NonNull String mContainerPath;
        private final long mContainerOffset;
        private final long mMethodOffset;

        private ExecutableMethodFileOffsets(
                @NonNull String containerPath, long containerOffset, long methodOffset) {
            this.mContainerPath = containerPath;
            this.mContainerOffset = containerOffset;
            this.mMethodOffset = methodOffset;
        }

        /**
         * The OS path of the containing file (could be virtual).
         *
         * @hide
         */
        @SystemApi(client = MODULE_LIBRARIES)
        public @NonNull String getContainerPath() {
            return mContainerPath;
        }

        /**
         * The offset of the containing file within the process' memory.
         *
         * @hide
         */
        @SystemApi(client = MODULE_LIBRARIES)
        public long getContainerOffset() {
            return mContainerOffset;
        }

        /**
         * The offset of the method within the containing file.
         *
         * @hide
         */
        @SystemApi(client = MODULE_LIBRARIES)
        public long getMethodOffset() {
            return mMethodOffset;
        }
    }

    private static native @Nullable ExecutableMethodFileOffsets
        getExecutableMethodFileOffsetsNative(Method javaMethod);

    /**
     * Fetches offset information about the location of the native executable code within the
     * running process' memory.
     *
     * @param javaMethod method for which info is to be identified.
     * @return {@link ExecutableMethodFileOffsets} containing offset information for the specified
     *         method, or null if the method is not AOT compiled.
     * @throws RuntimeException for unexpected failures in ART retrieval of info.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static @Nullable ExecutableMethodFileOffsets getExecutableMethodFileOffsets(
            @NonNull Method javaMethod) {
        return getExecutableMethodFileOffsetsNative(javaMethod);
    }

    /**
     * This method exists for binary compatibility.  It was part of
     * the allocation limits API which was removed in Android 3.0 (Honeycomb).
     *
     * @hide
     */
    @Deprecated
    public static int setAllocationLimit(int limit) {
        return -1;
    }

    /**
     * This method exists for binary compatibility.  It was part of
     * the allocation limits API which was removed in Android 3.0 (Honeycomb).
     *
     * @hide
     */
    @Deprecated
    public static int setGlobalAllocationLimit(int limit) {
        return -1;
    }

    /**
     * Count the number of instructions executed between two points.
     *
     * @hide
     */
    @Deprecated
    public static void startInstructionCounting() {}

    /**
     *
     * @hide
     */
    @Deprecated
    public static void stopInstructionCounting() {}

    /**
     *
     * @hide
     */
    @Deprecated
    public static void getInstructionCount(int[] counts) {}

    /**
     *
     * @hide
     */
    @Deprecated
    public static void resetInstructionCount() {}

    /**
     * Dumps a list of loaded class to the log file.
     *
     * @param flags a union of {@link android.os.Debug.SHOW_FULL_DETAIL},
     *    {@link android.os.Debug.SHOW_CLASSLOADER}, and {@link android.os.Debug.SHOW_INITIALIZED}.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @FastNative
    public static native void printLoadedClasses(int flags);

    /**
     * Gets the number of loaded classes.
     *
     * @return the number of loaded classes
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @FastNative
    public static native int getLoadedClassCount();

    /**
     * Dumps "hprof" data to the specified file.  This may cause a GC.
     *
     * The VM may create a temporary file in the same directory.
     *
     * @param filename Full pathname of output file (e.g. "/sdcard/dump.hprof").
     * @throws UnsupportedOperationException if the VM was built without
     *         HPROF support.
     * @throws IOException if an error occurs while opening or writing files.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void dumpHprofData(String filename) throws IOException {
        if (filename == null) {
            throw new NullPointerException("filename == null");
        }
        dumpHprofData(filename, null);
    }

    /**
     * Collects "hprof" heap data and sends it to DDMS.  This may cause a GC.
     *
     * @throws UnsupportedOperationException if the VM was built without
     *         HPROF support.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static native void dumpHprofDataDdms();

    /**
     * Dumps "hprof" heap data to a file, by name or descriptor.
     *
     * @param fileName Name of output file.  If fd is non-null, the
     *        file name is only used in log messages (and may be null).
     * @param fd Descriptor of open file that will receive the output.
     *        If this is null, the fileName is used instead.
     * @throws {@link IOException} if an error occurs while opening or writing files.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void dumpHprofData(String fileName, FileDescriptor fd)
            throws IOException {
       dumpHprofData(fileName, fd != null ? fd.getInt$() : -1);
    }

    private static native void dumpHprofData(String fileName, int fd)
            throws IOException;

    /**
     * Dumps the contents of the VM reference tables (e.g. JNI locals and
     * globals) to the log file.
     *
     * @hide
     */
    @UnsupportedAppUsage
    @SystemApi(client = MODULE_LIBRARIES)
    public static native void dumpReferenceTables();

    /**
     * Counts the instances of a class.
     * It is the caller's responsibility to do GC if they don't want unreachable
     * objects to get counted.
     *
     * @param klass the class to be counted.
     * @param assignable if true, any instance whose class is assignable to
     *                   {@code klass}, as defined by {@link Class#isAssignableFrom},
     *                   is counted. If false, only instances whose class is
     *                   equal to {@code klass} are counted.
     * @return the number of matching instances.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static native long countInstancesOfClass(Class klass, boolean assignable);

    /**
     * Counts the instances of classes.
     * It is the caller's responsibility to do GC if they don't want unreachable
     * objects to get counted.
     *
     * @param classes the classes to be counted.
     * @param assignable if true, any instance whose class is assignable to
     *                   {@code classes[i]}, as defined by {@link Class#isAssignableFrom},
     *                   is counted. If false, only instances whose class is
     *                   equal to {@code classes[i]} are counted.
     * @return an array containing the number of matching instances. The value
     *         for index {@code i} is the number of instances of
     *         the class {@code classes[i]}
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static native long[] countInstancesOfClasses(Class[] classes, boolean assignable);

    /* Map from the names of the runtime stats supported by getRuntimeStat() to their IDs */
    private static final HashMap<String, Integer> runtimeStatsMap = new HashMap<>();

    static {
        runtimeStatsMap.put("art.gc.gc-count", 0);
        runtimeStatsMap.put("art.gc.gc-time", 1);
        runtimeStatsMap.put("art.gc.bytes-allocated", 2);
        runtimeStatsMap.put("art.gc.bytes-freed", 3);
        runtimeStatsMap.put("art.gc.blocking-gc-count", 4);
        runtimeStatsMap.put("art.gc.blocking-gc-time", 5);
        runtimeStatsMap.put("art.gc.gc-count-rate-histogram", 6);
        runtimeStatsMap.put("art.gc.blocking-gc-count-rate-histogram", 7);
        runtimeStatsMap.put("art.gc.objects-allocated", 8);
        runtimeStatsMap.put("art.gc.total-time-waiting-for-gc", 9);
        runtimeStatsMap.put("art.gc.pre-oome-gc-count", 10);
    }

    /**
     * Returns the value of a particular runtime statistic or {@code null} if no
     * such runtime statistic exists.
     *
     * @param statName the name of the runtime statistic to look up.
     *
     * @return the value of the runtime statistic.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static String getRuntimeStat(String statName) {
        if (statName == null) {
            throw new NullPointerException("statName == null");
        }
        Integer statId = runtimeStatsMap.get(statName);
        if (statId != null) {
            return getRuntimeStatInternal(statId);
        }
        return null;
    }

    /**
     * Returns a map of the names/values of the runtime statistics
     * that {@link #getRuntimeStat()} supports.
     *
     * @return a map of the names/values of the supported runtime statistics.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static Map<String, String> getRuntimeStats() {
        HashMap<String, String> map = new HashMap<>();
        String[] values = getRuntimeStatsInternal();
        for (String name : runtimeStatsMap.keySet()) {
            int id = runtimeStatsMap.get(name);
            String value = values[id];
            map.put(name, value);
        }
        return map;
    }

    private static native String getRuntimeStatInternal(int statId);
    private static native String[] getRuntimeStatsInternal();

    /**
     * Attaches an agent to the VM.
     *
     * @param agent       The path to the agent .so file plus optional agent arguments.
     * @param classLoader The classloader to use as a loading context.
     *
     * @throws IOException if an error occurs while opening {@code agent} file.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void attachAgent(String agent, ClassLoader classLoader) throws IOException {
        nativeAttachAgent(agent, classLoader);
    }

    private static native void nativeAttachAgent(String agent, ClassLoader classLoader)
            throws IOException;

    /**
     * Exempts a class from any future non-SDK API access checks.
     * Methods declared in the class will be allowed to perform
     * reflection/JNI against the framework completely unrestricted.
     * Note that this does not affect uses of non-SDK APIs that the class links against.
     * Note that this does not affect methods declared outside this class, e.g.
     * inherited from a superclass or an implemented interface.
     *
     * @param klass The class whose methods should be exempted.
     *
     * @hide
     */
    @UnsupportedAppUsage
    public static native void allowHiddenApiReflectionFrom(Class<?> klass);

    /**
     * Sets the number of frames recorded for allocation tracking.
     *
     * @param stackDepth The number of frames captured for each stack trace.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static native void setAllocTrackerStackDepth(int stackDepth);

    /**
     * Called every time the Process is renamed (via android.os.Process)
     * This is a no-op on host.
     *
     * @param processName The new name of the process.
     *
     * @hide
     **/
    @SystemApi(client = MODULE_LIBRARIES)
    public static native void setCurrentProcessName(@NonNull String processName);

    /**
     * Called every time an app is added to the current Process
     * This is a no-op on host.
     *
     * @param packageName The package name of the added app.
     *
     * @hide
     **/
    @SystemApi(client = MODULE_LIBRARIES)
    public static native void addApplication(@NonNull String packageName);

    /**
     * Called every time an app is removed from the current Process
     * This is a no-op on host.
     *
     * @param packageName The package name of the removed app.
     *
     * @hide
     **/
    @SystemApi(client = MODULE_LIBRARIES)
    public static native void removeApplication(@NonNull String packageName);

    /**
     * Called as soon as Zygote specialize and knows the Android User ID.
     * This is a no-op on host.
     *
     * @param userId The Android User ID.
     *
     * @hide
     **/
    @SystemApi(client = MODULE_LIBRARIES)
    public static native void setUserId(@UserIdInt int userId);

    /**
     * Signal to ART we are waiting for the debugger
     * This is a no-op on host.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static native void setWaitingForDebugger(boolean waiting);


    /**
     * A class to encapsulate different modes of trace output. Currently traceFileName and file
     * descriptor are supported.
     *
     * @hide
     */
   @SystemApi(client = MODULE_LIBRARIES)
    public static class TraceDestination {
        private final String traceFileName;
        private final FileDescriptor fd;

        private TraceDestination(String traceFileName, FileDescriptor fd) {
            this.traceFileName = traceFileName;
            this.fd = fd;
        }

        /** @hide */
        public int getFd() {
            if (fd != null) {
                return fd.getInt$();
            } else {
                return -1;
            }
        }

        /** @hide */
        public String getFileName() {
            return traceFileName;
        }

        /**
         * Returns a TraceDestination that uses a fileName. Use this to provide a fileName to dump
         * the generated trace.
         *
         * @hide
         */
        @SystemApi(client = MODULE_LIBRARIES)
        public static @NonNull TraceDestination fromFileName(@NonNull String traceFileName) {
            return new TraceDestination(traceFileName,  null);
        }

        /**
         * Returns a TraceDestination that uses a file descriptor. Use this to provide a file
         * descriptor to dump the generated trace.
         *
         * @hide
         */
        @SystemApi(client = MODULE_LIBRARIES)
        public static @NonNull TraceDestination fromFileDescriptor(@NonNull FileDescriptor fd) {
            return new TraceDestination(null, fd);
        }
    }

    /**
     * Start an ART trace of executed dex methods. This uses a circular buffer to store entries
     * so it will only hold the most recently executed ones. The tracing is not precise.
     * If a low overhead tracing is already in progress then this request is ignored but an error
     * will be logged. The ongoing trace will not be impacted. For example, if there are two calls
     * to {@link #startLowOverheadTraceForAllMethods} without a {@link #stopLowOverheadTrace} in
     * between, the second request is ignored after logging an error. The first one will continue to
     * trace until the next {@link #stopLowOverheadTrace} call.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void startLowOverheadTraceForAllMethods() {
        startLowOverheadTraceForAllMethodsImpl();
    }

    /**
     * Stop an ongoing ART trace of executed dex methods. If there is no ongoing trace then this
     * request is ignored and an error will be logged.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void stopLowOverheadTrace() {
        stopLowOverheadTraceImpl();
    }

    /**
     * Dump the collected trace into the trace file provided.
     *
     * The trace destination can be a file descriptor or a file name. If the file name or the file
     * descriptor aren't valid we log an error and ignore the request. When a filename is provided
     * and a file already exists this method unlinks the file and creates a new file for writing.
     * This method unlinks instead of overwriting to prevent any problems if there are other uses
     * of the file. If the file does not exist then a new file is created. If for any reason the
     * file cannot be created then this method logs an error and the method returns.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void dumpLowOverheadTrace(@NonNull TraceDestination traceOutput) {
        if (traceOutput.getFd() == -1) {
            dumpLowOverheadTraceImpl(traceOutput.getFileName());
        } else {
            dumpLowOverheadTraceFdImpl(traceOutput.getFd());
        }
    }

    /**
     * Start an ART trace of executed dex methods that execute longer than a set threshold.
     * The threshold is defined by ART and isn't configurable. The tracing will be active
     * for a maximum of trace_duration_ns passed to this function. If another trace (started by
     * {@link #startLowOverheadTraceForAllMethods} /
     * {@link #startLowOverheadTraceForLongRunningMethods} / {@link #startMethodTracing}) is running
     * then this request is ignored and an error is logged.
     *
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public static void startLowOverheadTraceForLongRunningMethods(long traceDurationNs) {
        startLowOverheadTraceForLongRunningMethodsImpl(traceDurationNs);
    }


    private static native void startLowOverheadTraceForAllMethodsImpl();
    private static native void stopLowOverheadTraceImpl();
    private static native void dumpLowOverheadTraceImpl(String traceFileName);
    private static native void dumpLowOverheadTraceFdImpl(int fd);
    private static native void startLowOverheadTraceForLongRunningMethodsImpl(long traceDuration);

}
