// Copyright 2015 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.net.impl;

import android.content.Context;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;

import org.jni_zero.CalledByNative;
import org.jni_zero.JNINamespace;
import org.jni_zero.NativeMethods;

import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.base.metrics.ScopedSysTraceEvent;
import org.chromium.net.NetworkChangeNotifier;
import org.chromium.net.httpflags.BaseFeature;
import org.chromium.net.httpflags.Flags;
import org.chromium.net.httpflags.HttpFlagsLoader;
import org.chromium.net.httpflags.ResolvedFlags;
import org.chromium.net.telemetry.Hash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.concurrent.GuardedBy;

/** CronetLibraryLoader loads and initializes native library on init thread. */
@JNINamespace("cronet")
@VisibleForTesting
public class CronetLibraryLoader {
    // Synchronize initialization.
    private static final Object sLoadLock = new Object();

    @GuardedBy("sLoadLock")
    private static boolean sInitialized;

    private static final String LIBRARY_NAME = "mainlinecronet." + ImplVersion.getCronetVersion();
    @VisibleForTesting public static final String TAG = CronetLibraryLoader.class.getSimpleName();
    // Thread used for initialization work and processing callbacks for
    // long-lived global singletons. This thread lives forever as things like
    // the global singleton NetworkChangeNotifier live on it and are never killed.
    private static final HandlerThread sInitThread = new HandlerThread("CronetInit");
    // Block calling native methods until this ConditionVariable opens to indicate loadLibrary()
    // is completed and native methods have been registered.
    private static final ConditionVariable sWaitForLibLoad = new ConditionVariable();

    private static final ConditionVariable sHttpFlagsLoaded = new ConditionVariable();
    private static ResolvedFlags sHttpFlags;

    /**
     * A subset of {@code CronetLogger.CronetInitializedInfo} that this class is responsible for
     * populating.
     */
    public static final class CronetInitializedInfo {
        public int httpFlagsLatencyMillis = -1;
        public Boolean httpFlagsSuccessful;
        public List<Long> httpFlagsNames;
        public List<Long> httpFlagsValues;
    }

    private static CronetInitializedInfo sInitializedInfo;

    @VisibleForTesting public static final String LOG_FLAG_NAME = "Cronet_log_me";

    /**
     * Ensure that native library is loaded and initialized. Can be called from any thread, the load
     * and initialization is performed on init thread.
     *
     * @return True if the library was initialized as part of this call, false if it was already
     *     initialized.
     */
    public static boolean ensureInitialized(
            Context applicationContext, final CronetEngineBuilderImpl builder) {
        return ensureInitialized(applicationContext, builder, /* libAlreadyLoaded= */ false);
    }

    /**
     * This method will be called by the Zygote pre-fork to preload the native code. Which means
     * that this will be dead code in Chromium but it will be used in AOSP.
     */
    public static void preload() {
        loadLibrary();
    }

    private static void loadLibrary() {
        System.loadLibrary(LIBRARY_NAME);
    }

    public static boolean ensureInitialized(
            Context applicationContext,
            final CronetEngineBuilderImpl builder,
            boolean libAlreadyLoaded) {
        try (var traceEvent = ScopedSysTraceEvent.scoped("CronetLibraryLoader#ensureInitialized")) {
            synchronized (sLoadLock) {
                if (sInitialized) return false;
                ContextUtils.initApplicationContext(applicationContext);
                // The init thread may already be running if a previous initialization attempt
                // failed. In this case there is no need to spin it up again.
                //
                // Note: if we never succeed in loading the library, the init thread will end up
                // blocking on `sWaitForLibLoad` forever. Obviously this is suboptimal, but given
                // this is not supposed to fail, it's arguably benign.
                if (!sInitThread.isAlive()) {
                    try (var startInitThreadTraceEvent =
                            ScopedSysTraceEvent.scoped(
                                    "CronetLibraryLoader#ensureInitialized starting init thread")) {
                        sInitThread.start();
                        postToInitThread(
                                () -> {
                                    initializeOnInitThread();
                                });
                    }
                }
                if (!libAlreadyLoaded) {
                    try (var loadLibTraceEvent =
                            ScopedSysTraceEvent.scoped(
                                    "CronetLibraryLoader#ensureInitialized loading native"
                                            + " library")) {
                        if (builder.libraryLoader() != null) {
                            builder.libraryLoader().loadLibrary(LIBRARY_NAME);
                        } else {
                            loadLibrary();
                        }
                    }
                }
                try (var nativeInitTraceEvent =
                        ScopedSysTraceEvent.scoped(
                                "CronetLibraryLoader#ensureInitialized calling nativeInit")) {
                    CronetLibraryLoaderJni.get().nativeInit();
                }
                String implVersion = ImplVersion.getCronetVersion();
                if (!implVersion.equals(CronetLibraryLoaderJni.get().getCronetVersion())) {
                    throw new RuntimeException(
                            String.format(
                                    "Expected Cronet version number %s, "
                                            + "actual version number %s.",
                                    implVersion, CronetLibraryLoaderJni.get().getCronetVersion()));
                }
                Log.i(
                        TAG,
                        "Cronet version: %s, arch: %s",
                        implVersion,
                        System.getProperty("os.arch"));
                setNativeLoggingLevel();
                sWaitForLibLoad.open();
                sInitialized = true;
                return true;
            }
        }
    }

    private static void setNativeLoggingLevel() {
        // The constants used here should be kept in sync with logging::LogMessage::~LogMessage().
        final String nativeLogTag = "chromium";
        int loggingLevel;
        // TODO: this way of enabling VLOG is a hack - it doesn't make a ton of sense because
        // logging::LogMessage() will still log VLOG() at the Android INFO log level, not DEBUG or
        // VERBOSE; also this doesn't make it possible to use advanced filters like --vmodule. See
        // https://crbug.com/1488393 for a proposed alternative.
        if (Log.isLoggable(nativeLogTag, Log.VERBOSE)) {
            loggingLevel = -2; // VLOG(2)
        } else if (Log.isLoggable(nativeLogTag, Log.DEBUG)) {
            loggingLevel = -1; // VLOG(1)
        } else {
            // Use the default log level, which logs everything except VLOG(). Skip the
            // setMinLogLevel() call to avoid paying for an unnecessary JNI transition.
            return;
        }
        CronetLibraryLoaderJni.get().setMinLogLevel(loggingLevel);
    }

    /** Returns {@code true} if running on the initialization thread. */
    private static boolean onInitThread() {
        return sInitThread.getLooper() == Looper.myLooper();
    }

    /**
     * Runs Cronet initialization tasks on the init thread. Ensures that HTTP flags are loaded, the
     * NetworkChangeNotifier is initialzied and the init thread native MessageLoop is initialized.
     */
    static void initializeOnInitThread() {
        try (var traceEvent =
                ScopedSysTraceEvent.scoped("CronetLibraryLoader#initializeOnInitThread")) {
            assert onInitThread();
            assert sInitializedInfo == null;
            sInitializedInfo = new CronetInitializedInfo();

            try (var httpFlagsTraceEvent =
                    ScopedSysTraceEvent.scoped(
                            "CronetLibraryLoader#initializeOnInitThread loading HTTP flags")) {
                var httpFlagsLoadingStartUptimeMillis = SystemClock.uptimeMillis();
                var applicationContext = ContextUtils.getApplicationContext();
                // Load HTTP flags. This is a potentially expensive call, so we do this in parallel
                // with library loading in the hope of minimizing impact on Cronet initialization
                // latency.
                assert sHttpFlags == null;
                Flags flags;
                if (!CronetManifest.shouldReadHttpFlags(applicationContext)) {
                    Log.d(TAG, "Not loading HTTP flags because they are disabled in the manifest");
                    flags = null;
                } else {
                    flags = HttpFlagsLoader.load(applicationContext);
                    sInitializedInfo.httpFlagsSuccessful = flags != null;
                }
                sHttpFlags =
                        ResolvedFlags.resolve(
                                flags != null ? flags : Flags.newBuilder().build(),
                                applicationContext.getPackageName(),
                                ImplVersion.getCronetVersion());
                // Stop the timer immediately *before* we unblock the thread that may be waiting on
                // us. This matters more than you may think, because in the (likely) case the
                // waiting thread is higher priority than us, we may get preempted as soon as we
                // unblock, adding misleading delays to the timer. See https://crbug.com/346546533.
                sInitializedInfo.httpFlagsLatencyMillis =
                        (int) (SystemClock.uptimeMillis() - httpFlagsLoadingStartUptimeMillis);
            }
            sHttpFlagsLoaded.open();
            ResolvedFlags.Value logMe = sHttpFlags.flags().get(LOG_FLAG_NAME);
            if (logMe != null) {
                Log.i(TAG, "HTTP flags log line: %s", logMe.getStringValue());
            }
            populateCronetInitializedHttpFlagNamesValues();

            NetworkChangeNotifier.init();
            // Registers to always receive network notifications. Note
            // that this call is fine for Cronet because Cronet
            // embedders do not have API access to create network change
            // observers. Existing observers in the net stack do not
            // perform expensive work.
            NetworkChangeNotifier.registerToReceiveNotificationsAlways();

            try (var libLoadTraceEvent =
                    ScopedSysTraceEvent.scoped(
                            "CronetLibraryLoader#initializeOnInitThread waiting on library load")) {
                // Wait for loadLibrary() to complete so JNI is registered.
                sWaitForLibLoad.block();
            }

            try (var nativeInitTraceEvent =
                    ScopedSysTraceEvent.scoped(
                            "CronetLibraryLoader#ensureInitialized calling"
                                    + " cronetInitOnInitThread")) {
                // registerToReceiveNotificationsAlways() is called before the native
                // NetworkChangeNotifierAndroid is created, so as to avoid receiving
                // the undesired initial network change observer notification, which
                // will cause active requests to fail with ERR_NETWORK_CHANGED.
                CronetLibraryLoaderJni.get().cronetInitOnInitThread();
            }
        }
    }

    private static void populateCronetInitializedHttpFlagNamesValues() {
        // Make sure the order is deterministic - this may potentially make it easier to
        // deduplicate/aggregate the log entries down the line, by preventing two log entries from
        // being treated as different even though they have the same set of flag names and values.
        // Note we need to pair up the names and values before we do this, as we need the order to
        // be consistent between the two.
        var hashedNamesValues = new ArrayList<Pair<Long, Long>>();
        for (var flag : sHttpFlags.flags().entrySet()) {
            hashedNamesValues.add(
                    new Pair<Long, Long>(
                            Hash.hash(flag.getKey()),
                            hashHttpFlagValueForLogging(flag.getValue())));
        }
        Collections.sort(hashedNamesValues, (left, right) -> left.first.compareTo(right.first));

        sInitializedInfo.httpFlagsNames = new ArrayList<Long>();
        sInitializedInfo.httpFlagsValues = new ArrayList<Long>();
        for (var hashedNameValue : hashedNamesValues) {
            sInitializedInfo.httpFlagsNames.add(hashedNameValue.first);
            sInitializedInfo.httpFlagsValues.add(hashedNameValue.second);
        }
    }

    private static long hashHttpFlagValueForLogging(ResolvedFlags.Value value) {
        switch (value.getType()) {
            case BOOL:
                return value.getBoolValue() ? 1 : 0;
            case INT:
                return value.getIntValue();
            case FLOAT:
                // Converting to double first to avoid precision issues (e.g. 42.5 would end up as
                // 42500001792 instead of 42500000000 otherwise)
                return Math.round((double) value.getFloatValue() * 1_000_000_000d);
            case STRING:
                return Hash.hash(value.getStringValue());
            case BYTES:
                return Hash.hash(value.getBytesValue().toByteArray());
            default:
                throw new IllegalArgumentException(
                        "Unexpected flag value type: " + value.getClass().getName());
        }
    }

    /**
     * Retrieves the initialization info for logging. Only safe to call after the init thread has
     * become ready.
     */
    public static CronetInitializedInfo getCronetInitializedInfo() {
        assert sInitializedInfo != null;
        return sInitializedInfo;
    }

    /** Run {@code r} on the initialization thread. */
    public static void postToInitThread(Runnable r) {
        if (onInitThread()) {
            r.run();
        } else {
            new Handler(sInitThread.getLooper()).post(r);
        }
    }

    /**
     * Returns the HTTP flags that apply to this instance of the Cronet library.
     *
     * <p>Never returns null: if HTTP flags were not loaded, will return an empty set of flags.
     *
     * <p>This function will deadlock if {@link #ensureInitialized} is not called.
     */
    public static ResolvedFlags getHttpFlags() {
        // To avoid trace spam (and because tracing is not free) we want to trace only if we are
        // about to block on the condition variable. Unfortunately, there is no way to read the
        // current value of a ConditionVariable (counter-intuitively, block() with a zero timeout
        // blocks indefinitely instead of returning immediately). So instead we use the nullness of
        // `sHttpFlags` as an hint as to whether we are about to block or not. This is obviously
        // racy, but the Java memory model guarantees defined behavior even in this case, so we're
        // fine as long as we don't rely on the result for correctness.
        if (sHttpFlags == null) {
            try (var traceEvent =
                    ScopedSysTraceEvent.scoped(
                            "CronetLibraryLoader#getHttpFlags waiting for HTTP flags load")) {
                sHttpFlagsLoaded.block();
            }
        } else {
            // Make sure HTTP flags have truly finished loading (memory barrier). Due to how the
            // Java memory model works, the above null check is not sufficient as it does not
            // prevent reordering.
            sHttpFlagsLoaded.block();
        }
        return sHttpFlags;
    }

    /**
     * Called by native Cronet library early initialization code to obtain the values of native
     * base::Feature overrides that will be applied for the entire lifetime of the Cronet native
     * library.
     *
     * <p>Note that this call sits in the critical path of native library initialization, as
     * practically no Chromium native code can run until base::Feature values have settled.
     *
     * @return The base::Feature overrides as a binary serialized {@link
     *     org.chromium.net.httpflags.BaseFeatureOverrides} proto.
     */
    @CalledByNative
    private static byte[] getBaseFeatureOverrides() {
        return BaseFeature.getOverrides(getHttpFlags()).toByteArray();
    }

    /**
     * Called from native library to get default user agent constructed using application context.
     * May be called on any thread.
     *
     * <p>Expects that ContextUtils.initApplicationContext() was called already either by some
     * testing framework or an embedder constructing a Java CronetEngine via
     * CronetEngine.Builder.build().
     */
    @CalledByNative
    private static String getDefaultUserAgent() {
        return UserAgent.getDefault();
    }

    /**
     * Called from native library to ensure that library is initialized.
     * May be called on any thread, but initialization is performed on
     * this.sInitThread.
     *
     * Expects that ContextUtils.initApplicationContext() was called already
     * either by some testing framework or an embedder constructing a Java
     * CronetEngine via CronetEngine.Builder.build().
     *
     * TODO(mef): In the long term this should be changed to some API with
     * lower overhead like CronetEngine.Builder.loadNativeCronet().
     */
    @CalledByNative
    private static void ensureInitializedFromNative() {
        // The application context must already be initialized
        // using ContextUtils.initApplicationContext().
        Context applicationContext = ContextUtils.getApplicationContext();
        assert applicationContext != null;
        ensureInitialized(applicationContext, null, /* libAlreadyLoaded= */ true);
    }

    @CalledByNative
    private static void setNetworkThreadPriorityOnNetworkThread(int priority) {
        Log.d(TAG, "Setting network thread priority to " + priority);
        Process.setThreadPriority(priority);
    }

    @NativeMethods
    interface Natives {
        // Native methods are implemented in cronet_library_loader.cc.
        void nativeInit();

        void cronetInitOnInitThread();

        String getCronetVersion();

        void setMinLogLevel(int loggingLevel);
    }
}
