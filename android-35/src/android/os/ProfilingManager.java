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
package android.os;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.profiling.Flags;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * API for apps to request and listen for app specific profiling.
 */
@FlaggedApi(Flags.FLAG_TELEMETRY_APIS)
public final class ProfilingManager {
    private static final String TAG = ProfilingManager.class.getSimpleName();
    private static final boolean DEBUG = false;

    /** Profiling type for {@link #requestProfiling} to request a java heap dump. */
    public static final int PROFILING_TYPE_JAVA_HEAP_DUMP = 1;

    /** Profiling type for {@link #requestProfiling} to request a heap profile. */
    public static final int PROFILING_TYPE_HEAP_PROFILE = 2;

    /** Profiling type for {@link #requestProfiling} to request a stack sample. */
    public static final int PROFILING_TYPE_STACK_SAMPLING = 3;

    /** Profiling type for {@link #requestProfiling} to request a system trace. */
    public static final int PROFILING_TYPE_SYSTEM_TRACE = 4;

    /* Begin public API defined keys. */
    /* End public API defined keys. */

    /* Begin not-public API defined keys. */
    /**
     * Can only be used with profiling type heap profile, stack sampling, or system trace.
     * Value of type int.
     * @hide */
    public static final String KEY_DURATION_MS = "KEY_DURATION_MS";

    /**
     * Can only be used with profiling type heap profile. Value of type long.
     * @hide */
    public static final String KEY_SAMPLING_INTERVAL_BYTES = "KEY_SAMPLING_INTERVAL_BYTES";

    /**
     * Can only be used with profiling type heap profile. Value of type boolean.
     * @hide */
    public static final String KEY_TRACK_JAVA_ALLOCATIONS = "KEY_TRACK_JAVA_ALLOCATIONS";

    /**
     * Can only be used with profiling type stack sampling. Value of type int.
     * @hide */
    public static final String KEY_FREQUENCY_HZ = "KEY_FREQUENCY_HZ";

    /**
     * Can be used with all profiling types. Value of type int.
     * @hide */
    public static final String KEY_SIZE_KB = "KEY_SIZE_KB";
    /* End not-public API defined keys. */

    /**
     * @hide *
     */
    @IntDef(
        prefix = {"PROFILING_TYPE_"},
        value = {
            PROFILING_TYPE_JAVA_HEAP_DUMP,
            PROFILING_TYPE_HEAP_PROFILE,
            PROFILING_TYPE_STACK_SAMPLING,
            PROFILING_TYPE_SYSTEM_TRACE,
        })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProfilingType {}

    private final Object mLock = new Object();
    private final Context mContext;

    /** @hide */
    @VisibleForTesting
    @GuardedBy("mLock")
    public final ArrayList<ProfilingRequestCallbackWrapper> mCallbacks = new ArrayList<>();

    @GuardedBy("mLock")
    private IProfilingService mProfilingService;

    /**
     * Constructor for ProfilingManager.
     *
     * @hide
     */
    public ProfilingManager(Context context) {
        mContext = context;
    }

    /**
     * Request system profiling.
     *
     * <p class="note"> Note: use of this API directly is not recommended for most use cases.
     * Please use the higher level wrappers provided by androidx that will construct the request
     * correctly based on available options and simplified user provided request parameters.</p>
     *
     * <p class="note"> Note: requests are not guaranteed to be filled.</p>
     *
     * <p class="note"> Note: Both a listener and executor must be set for the request to be
     * considered for fulfillment.
     * Listeners can be set in this method, with {@link #registerForAllProfilingResults}, or both.
     * If no listener and executor is set the request will be discarded.</p>
     *
     * @param profilingType Type of profiling to collect.
     * @param parameters Bundle of request related parameters. If the bundle contains any
     *                  unrecognized parameters, the request will be fail with
     *                  {@link #ProfilingResult#ERROR_FAILED_INVALID_REQUEST}. If the values for
     *                  the parameters are out of supported range, the closest possible in range
     *                  value will be chosen.
     *                  Use of androidx wrappers is recommended over generating this directly.
     * @param tag Caller defined data to help identify the output.
     *                  The first 20 alphanumeric characters, plus dashes, will be lowercased
     *                  and included in the output filename.
     * @param cancellationSignal for caller requested cancellation.
     *                  Results will be returned if available.
     *                  If this is null, the requesting app will not be able to stop the collection.
     *                  The collection will stop after timing out with either the provided
     *                  configurations or with system defaults
     * @param executor  The executor to call back with.
     *                  Will only be used for the listener provided in this method.
     *                  If this is null, and no global executor and listener combinations are
     *                  registered at the time of the request, the request will be dropped.
     * @param listener  Listener to be triggered with result. Any global listeners registered via
     *                  {@link #registerForAllProfilingResults} will also be triggered. If this is
     *                  null, and no global listener and executor combinations are registered at
     *                  the time of the request, the request will be dropped.
     */
    public void requestProfiling(
            @ProfilingType int profilingType,
            @Nullable Bundle parameters,
            @Nullable String tag,
            @Nullable CancellationSignal cancellationSignal,
            @Nullable Executor executor,
            @Nullable Consumer<ProfilingResult> listener) {
        synchronized (mLock) {
            try {
                final UUID key = UUID.randomUUID();

                if (executor != null && listener != null) {
                    // Listeners are provided, store them.
                    mCallbacks.add(new ProfilingRequestCallbackWrapper(executor, listener, key));
                } else if (mCallbacks.isEmpty()) {
                    // No listeners have been registered by any path, toss the request.
                    throw new IllegalArgumentException(
                            "No listeners have been registered. Request has been discarded.");
                }
                // If neither case above was hit, app wide listeners were provided. Continue.

                final IProfilingService service = getIProfilingServiceLocked();
                if (service == null) {
                    executor.execute(() -> listener.accept(
                            new ProfilingResult(ProfilingResult.ERROR_UNKNOWN, null, tag,
                                "ProfilingService is not available")));
                    if (DEBUG) Log.d(TAG, "ProfilingService is not available");
                    return;
                }

                // For key, use most and least significant bits so we can create an identical UUID
                // after passing over binder.
                service.requestProfiling(profilingType, parameters,
                        mContext.getFilesDir().getPath(), tag,
                        key.getMostSignificantBits(), key.getLeastSignificantBits());
                if (cancellationSignal != null) {
                    cancellationSignal.setOnCancelListener(
                            () -> {
                                synchronized (mLock) {
                                    try {
                                        service.requestCancel(key.getMostSignificantBits(),
                                                key.getLeastSignificantBits());
                                    } catch (RemoteException e) {
                                        // Ignore, request in flight already and we can't stop it.
                                    }
                                }
                            }
                    );
                }
            } catch (RemoteException e) {
                if (DEBUG) Log.d(TAG, "Binder exception processing request", e);
                executor.execute(() -> listener.accept(
                        new ProfilingResult(ProfilingResult.ERROR_UNKNOWN, null, tag,
                                "Binder exception processing request")));
                throw new RuntimeException("Unable to request profiling.");
            }
        }
    }

    /**
     * Register a listener to be called for all profiling results for this uid. Listeners set here
     * will be called in addition to any provided with the request.
     *
     * @param executor The executor to call back with.
     * @param listener Listener to be triggered with result.
     */
    public void registerForAllProfilingResults(
            @NonNull Executor executor,
            @NonNull Consumer<ProfilingResult> listener) {
        synchronized (mLock) {
            if (getIProfilingServiceLocked() == null) {
                // If the binder object was not successfully registered then this listener will
                // not ever be triggered.
                executor.execute(() -> listener.accept(new ProfilingResult(
                        ProfilingResult.ERROR_UNKNOWN, null, null,
                        "Binder exception processing request")));
                return;
            }
            mCallbacks.add(new ProfilingRequestCallbackWrapper(executor, listener, null));
        }
    }

    /**
     * Unregister a listener that was to be called for all profiling results. If no listener is
     * provided, all listeners for this process that were not submitted with a profiling request
     * will be removed.
     *
     * @param listener Listener to unregister and no longer be triggered with the results.
     *                 Null to remove all global listeners for this uid.
     */
    public void unregisterForAllProfilingResults(
            @Nullable Consumer<ProfilingResult> listener) {
        synchronized (mLock) {
            if (mCallbacks.isEmpty()) {
                // No callbacks, nothing to remove.
                return;
            }

            if (listener == null) {
                // Remove all global listeners.
                ArrayList<ProfilingRequestCallbackWrapper> listenersToRemove = new ArrayList<>();
                for (int i = 0; i < mCallbacks.size(); i++) {
                    ProfilingRequestCallbackWrapper wrapper = mCallbacks.get(i);
                    // Only remove global listeners which are not tied to a specific request. These
                    // can be identified by checking that they do not have an associated key.
                    if (wrapper.mKey == null) {
                        listenersToRemove.add(wrapper);
                    }
                }
                mCallbacks.removeAll(listenersToRemove);
            } else {
                // Remove the provided listener only.
                for (int i = 0; i < mCallbacks.size(); i++) {
                    ProfilingRequestCallbackWrapper wrapper = mCallbacks.get(i);
                    if (listener.equals(wrapper.mListener)) {
                        mCallbacks.remove(i);
                        return;
                    }
                }
            }
        }
    }

    @GuardedBy("mLock")
    private @Nullable IProfilingService getIProfilingServiceLocked() {
        if (mProfilingService != null) {
            return mProfilingService;
        }
        mProfilingService = IProfilingService.Stub.asInterface(
                ProfilingFrameworkInitializer.getProfilingServiceManager()
                    .getProfilingServiceRegisterer().get());
        if (mProfilingService == null) {
            // Service is not accessible, all requests will fail.
            return mProfilingService;
        }
        try {
            mProfilingService.registerResultsCallback(new IProfilingResultCallback.Stub() {

                /**
                 * Called by {@link ProfilingService} when a result is ready,
                 * both for success and failure.
                 *
                 * @return whether there are additional callbacks backed by this binder object.
                 */
                @Override
                public boolean sendResult(@Nullable String resultFile, long keyMostSigBits,
                        long keyLeastSigBits, int status, @Nullable String tag,
                        @Nullable String error) {
                    synchronized (mLock) {
                        if (mCallbacks.isEmpty()) {
                            // This shouldn't happen - no callbacks, nowhere to report this result.
                            if (DEBUG) Log.d(TAG, "No callbacks");
                            mProfilingService = null;
                            return false;
                        }

                        // This shouldn't be true, but if the file is null ensure the status
                        // represents a failure.
                        final boolean overrideStatusToError = resultFile == null
                                && status == ProfilingResult.ERROR_NONE;

                        UUID key = new UUID(keyMostSigBits, keyLeastSigBits);
                        int removeListenerPos = -1;
                        for (int i = 0; i < mCallbacks.size(); i++) {
                            ProfilingRequestCallbackWrapper wrapper = mCallbacks.get(i);
                            if (key.equals(wrapper.mKey)) {
                                // At most 1 listener can have a key matching this result: the one
                                // registered with the request, remove that one only.
                                if (removeListenerPos == -1) {
                                    removeListenerPos = i;
                                } else {
                                    // This should never happen.
                                    if (DEBUG) Log.d(TAG, "More than 1 listener with the same key");
                                }
                            } else if (wrapper.mKey != null) {
                                // If the key is not null, and doesn't matched the result key, then
                                // this key belongs to another request and should not be triggered.
                                continue;
                            }

                            // TODO: b/337017299 - check resultFile is valid before returning
                            // Now trigger the callback for any listener that doesn't belong to
                            // another request.
                            wrapper.mExecutor.execute(() -> wrapper.mListener.accept(
                                    new ProfilingResult(overrideStatusToError
                                            ? ProfilingResult.ERROR_UNKNOWN : status,
                                            resultFile, tag, error)));
                        }

                        // Remove the single listener that was tied to the request, if applicable.
                        if (removeListenerPos != -1) {
                            mCallbacks.remove(removeListenerPos);
                        }

                        if (mCallbacks.isEmpty()) {
                            mProfilingService = null;
                            return false;
                        }
                        return true;
                    }
                }

                /**
                 * Called by {@link ProfilingService} when a trace is ready and need to be copied
                 * to callers internal storage.
                 *
                 * This method will open a new file and pass back the FileDescriptor for
                 * ProfilingService to write to.
                 */
                @Override
                public ParcelFileDescriptor generateFile(String filePathAbsolute, String fileName) {
                    try {
                        // Ensure the profiling directory exists. Create it if it doesn't.
                        final File profilingDir = new File(filePathAbsolute);
                        if (!profilingDir.exists()) {
                            profilingDir.mkdir();
                        }

                        // Create the profiling file for the output to be written to.
                        final File profilingFile = new File(filePathAbsolute + fileName);
                        profilingFile.createNewFile();
                        if (!profilingFile.exists()) {
                            // Failed to create output file. Result will be lost.
                            if (DEBUG) Log.d(TAG, "Output file couldn't be created");
                            return null;
                        }

                        // Wrap the new output file in a {@link ParcelFileDescriptor} and
                        // pass back to {@link ProfilingService} to write to.
                        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(profilingFile,
                                ParcelFileDescriptor.MODE_READ_WRITE);
                        return pfd;
                    } catch (Exception e) {
                        // Failure prepping output file. Result will be lost.
                        if (DEBUG) Log.d(TAG, "Exception preparing file", e);
                        return null;
                    }
                }
            });
        } catch (RemoteException e) {
            if (DEBUG) Log.d(TAG, "Exception registering service callback", e);
            throw new RuntimeException("Unable to register profiling result callback."
                    + " All Profiling requests will fail.");
        }
        return mProfilingService;
    }

    private static final class ProfilingRequestCallbackWrapper {
        /** executor provided with callback request */
        final @NonNull Executor mExecutor;

        /** listener provided with callback request */
        final @NonNull Consumer<ProfilingResult> mListener;

        /**
         * Unique key generated with each profiling request {@link #requestProfiling}, but not with
         * requests to register a listener only {@link #registerForAllProfilingResults}.
         *
         * Key is used to match the result with the listener added with the request so that it can
         * removed after being triggered while the general registered callbacks remain active.
         */
        final @Nullable UUID mKey;

        ProfilingRequestCallbackWrapper(@NonNull Executor executor,
                @NonNull Consumer<ProfilingResult> listener,
                @Nullable UUID key) {
            mExecutor = executor;
            mListener = listener;
            mKey = key;
        }
    }
}
