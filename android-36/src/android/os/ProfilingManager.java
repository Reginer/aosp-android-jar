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

import static android.os.ProfilingTrigger.TriggerType;

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
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * <p>
 * This class allows the caller to:
 * <ul>
 * <li>Request profiling and listen for results. Profiling types supported are: system traces,
 *     java heap dumps, heap profiles, and stack traces.</li>
 * <li>Register triggers for the system to capture profiling on the apps behalf.</li>
 * </ul>
 * </p>
 *
 * <p>
 * The {@link #requestProfiling} API can be used to begin profiling. Profiling may be ended manually
 * using the CancellationSignal provided in the request, or as a result of a timeout. The timeout
 * may be either the system default or caller defined in the parameter bundle for select types.
 * </p>
 *
 * <p>
 * The profiling results are delivered to the requesting app's data directory and a pointer to the
 * file will be received using the app provided listeners.
 * </p>
 *
 * <p>
 * Apps can provide listeners in one or both of two ways:
 * <ul>
 * <li>A request-specific listener included with the request. This will trigger only with a result
 *     from the request it was provided with.</li>
 * <li>A global listener provided by {@link #registerForAllProfilingResults}. This will be triggered
 *     for all results belonging to your app. This listener is the only way to receive results from
 *     system triggered profiling instances set up with {@link #addProfilingTriggers}.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Requests are rate limited and not guaranteed to be filled. Rate limiting can be disabled for
 * local testing of {@link #requestProfiling} using the shell command
 * {@code device_config put profiling_testing rate_limiter.disabled true}
 * </p>
 *
 * <p>
 * For local testing, profiling results can be accessed more easily by enabling debug mode. This
 * will retain output files in a temporary system directory, which is accessible by adb shell. The
 * locations of the retained files will be available in logcat. The behavior and command varies by
 * version:
 * <ul>
 * <li>For Android versions 16 and above, debug mode will retain both unredacted (where applicable)
 * and redacted results in the temporary directory. It can be enabled with the shell command
 * {@code device_config put profiling_testing delete_temporary_results.disabled true} and disabled
 * be setting that same value back to false.
 * </li>
 * <li>For Android version 15, debug mode will retain only the unredacted result (where applicable)
 * in the temporary directory. It can be enabled with the shell command
 * {@code device_config put profiling_testing delete_unredacted_trace.disabled true} and disabled
 * be setting that same value back to false.
 * </li>
 * </ul>
 * </p>
 *
 * <p>
 * In order to test profiling triggers, enable testing mode for your app with the shell command
 * {@code device_config put profiling_testing system_triggered_profiling.testing_package_name
 * com.your.app} which will:
 * <ul>
 * <li>Ensure that a background trace is running.</li>
 * <li>Allow all triggers for the provided package name to pass the system level rate limiter.
 *     This mode will continue until manually stopped with the shell command
 *     {@code device_config delete profiling_testing system_triggered_profiling.testing_package_name}.
 *     </li>
 * </ul>
 * </p>
 *
 * <p>
 * Results are redacted and contain specific information about the requesting process only.
 * </p>
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

    /* Begin not-public API defined keys/values. */
    /**
     * Can only be used with profiling type heap profile, stack sampling, or system trace.
     * Value of type int.
     * @hide
     */
    public static final String KEY_DURATION_MS = "KEY_DURATION_MS";

    /**
     * Can only be used with profiling type heap profile. Value of type long.
     * @hide
     */
    public static final String KEY_SAMPLING_INTERVAL_BYTES = "KEY_SAMPLING_INTERVAL_BYTES";

    /**
     * Can only be used with profiling type heap profile. Value of type boolean.
     * @hide
     */
    public static final String KEY_TRACK_JAVA_ALLOCATIONS = "KEY_TRACK_JAVA_ALLOCATIONS";

    /**
     * Can only be used with profiling type stack sampling. Value of type int.
     * @hide
     */
    public static final String KEY_FREQUENCY_HZ = "KEY_FREQUENCY_HZ";

    /**
     * Can be used with all profiling types. Value of type int.
     * @hide
     */
    public static final String KEY_SIZE_KB = "KEY_SIZE_KB";

    /**
     * Can be used with profiling type system trace.
     * Value of type int must be one of:
     * {@link VALUE_BUFFER_FILL_POLICY_DISCARD}
     * {@link VALUE_BUFFER_FILL_POLICY_RING_BUFFER}
     * @hide
     */
    public static final String KEY_BUFFER_FILL_POLICY = "KEY_BUFFER_FILL_POLICY";

    /** @hide */
    public static final int VALUE_BUFFER_FILL_POLICY_DISCARD = 1;

    /** @hide */
    public static final int VALUE_BUFFER_FILL_POLICY_RING_BUFFER = 2;
    /* End not-public API defined keys/values. */

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

    /** @hide */
    @VisibleForTesting
    @GuardedBy("mLock")
    public IProfilingService mProfilingService;

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
     * <p class="note">
     *   Note: use of this API directly is not recommended for most use cases.
     *   Consider using the higher level wrappers provided by AndroidX that will construct the
     *   request correctly, supporting available options with simplified request parameters
     * </p>
     *
     * <p>
     *   Both a listener and an executor must be set at the time of the request for the request to
     *   be considered for fulfillment. Listener/executor pairs can be set in this method, with
     *   {@link #registerForAllProfilingResults}, or both. The listener and executor must be set
     *   together, in the same call. If no listener and executor combination is set, the request
     *   will be discarded and no callback will be received.
     * </p>
     *
     * <p>
     *   Requests will be rate limited and are not guaranteed to be filled.
     * </p>
     *
     * <p>
     *   There might be a delay before profiling begins.
     *   For continuous profiling types (system tracing, stack sampling, and heap profiling),
     *   we recommend starting the collection early and stopping it with {@code cancellationSignal}
     *   immediately after the area of interest to ensure that the section you want profiled is
     *   captured.
     *   For heap dumps, we recommend testing locally to ensure that the heap dump is collected at
     *   the proper time.
     * </p>
     *
     * @param profilingType Type of profiling to collect.
     * @param parameters Bundle of request related parameters. If the bundle contains any
     *                  unrecognized parameters, the request will be fail with
     *                  {@link android.os.ProfilingResult#ERROR_FAILED_INVALID_REQUEST}. If the
     *                  values for the parameters are out of supported range, the closest possible
     *                  in range value will be chosen.
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

                final IProfilingService service = getOrCreateIProfilingServiceLocked(false);
                if (service == null) {
                    executor.execute(() -> listener.accept(
                            new ProfilingResult(ProfilingResult.ERROR_UNKNOWN, null, tag,
                                "ProfilingService is not available",
                                Flags.systemTriggeredProfilingNew()
                                        ? ProfilingTrigger.TRIGGER_TYPE_NONE : 0)));
                    if (DEBUG) Log.d(TAG, "ProfilingService is not available");
                    return;
                }

                String packageName = mContext.getPackageName();
                if (packageName == null) {
                    executor.execute(() -> listener.accept(
                            new ProfilingResult(ProfilingResult.ERROR_UNKNOWN, null, tag,
                                    "Failed to resolve package name",
                                    Flags.systemTriggeredProfilingNew()
                                            ? ProfilingTrigger.TRIGGER_TYPE_NONE : 0)));
                    if (DEBUG) Log.d(TAG, "Failed to resolve package name.");
                    return;
                }

                // For key, use most and least significant bits so we can create an identical UUID
                // after passing over binder.
                service.requestProfiling(profilingType, parameters, tag,
                        key.getMostSignificantBits(), key.getLeastSignificantBits(),
                        packageName);
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
                                "Binder exception processing request",
                                Flags.systemTriggeredProfilingNew()
                                        ? ProfilingTrigger.TRIGGER_TYPE_NONE : 0)));
                throw new RuntimeException("Unable to request profiling.");
            }
        }
    }

    /**
     * Register a listener to be called for all profiling results for this uid. Listeners set here
     * will be called in addition to any provided with the request.
     *
     * <p class="note"> Note: If a callback attempt fails (for example, because your app is killed
     * while a trace is in progress) re-delivery may be attempted using a listener added via this
     * method. </p>
     *
     * @param executor The executor to call back with.
     * @param listener Listener to be triggered with result.
     */
    public void registerForAllProfilingResults(
            @NonNull Executor executor,
            @NonNull Consumer<ProfilingResult> listener) {
        synchronized (mLock) {
            // Only notify {@link mProfilingService} of a general listener being added if it already
            // exists as registering it also handles the notifying.
            boolean shouldNotifyService = mProfilingService != null;

            if (getOrCreateIProfilingServiceLocked(true) == null) {
                // If the binder object was not successfully registered then this listener will
                // not ever be triggered.
                executor.execute(() -> listener.accept(new ProfilingResult(
                        ProfilingResult.ERROR_UNKNOWN, null, null,
                        "Binder exception processing request",
                        Flags.systemTriggeredProfilingNew()
                                ? ProfilingTrigger.TRIGGER_TYPE_NONE : 0)));
                return;
            }
            mCallbacks.add(new ProfilingRequestCallbackWrapper(executor, listener, null));

            if (shouldNotifyService) {
                // Notify service that a general listener was added. General listeners are also used
                // for queued callbacks if any are waiting.
                try {
                    mProfilingService.generalListenerAdded();
                } catch (RemoteException e) {
                    // Do nothing. Binder callback is already registered, but service won't know
                    // there is a general listener so queued callbacks won't occur.
                    Log.d(TAG, "Exception notifying service of general callback,"
                            + " queued callbacks will not occur.", e);
                }
            }
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

    /**
     * Register the provided list of triggers for this process.
     *
     * Profiling triggers are system triggered events that an app can register interest in receiving
     * profiling of. There is no guarantee that these triggers will be filled. Results, if
     * available, will be delivered only to a global listener added using
     * {@link #registerForAllProfilingResults}.
     *
     * Only one of each trigger type can be added at a time.
     * <ul>
     * <li>If the provided list contains a trigger type that is already registered then the new one
     *     will replace the existing one.</li>
     * <li>If the provided list contains more than one trigger object for a trigger type then only
     *     one will be kept.</li>
     * </ul>
     */
    @FlaggedApi(Flags.FLAG_SYSTEM_TRIGGERED_PROFILING_NEW)
    public void addProfilingTriggers(@NonNull List<ProfilingTrigger> triggers) {
        synchronized (mLock) {
            if (triggers.isEmpty()) {
                // No triggers are being added, nothing to do.
                if (DEBUG) Log.d(TAG, "Trying to add an empty list of triggers.");
                return;
            }

            final IProfilingService service = getOrCreateIProfilingServiceLocked(false);
            if (service == null) {
                // If we can't access service then we can't do anything. Return.
                if (DEBUG) Log.d(TAG, "ProfilingService is not available, triggers will be lost.");
                return;
            }

            String packageName = mContext.getPackageName();
            if (packageName == null) {
                if (DEBUG) Log.d(TAG, "Failed to resolve package name.");
                return;
            }

            try {
                service.addProfilingTriggers(toValueParcelList(triggers), packageName);
            } catch (RemoteException e) {
                if (DEBUG) Log.d(TAG, "Binder exception processing request", e);
                throw new RuntimeException("Unable to add profiling triggers.");
            }
        }
    }

    @FlaggedApi(Flags.FLAG_SYSTEM_TRIGGERED_PROFILING_NEW)
    private List<ProfilingTriggerValueParcel> toValueParcelList(
            List<ProfilingTrigger> triggerList) {
        List<ProfilingTriggerValueParcel> triggerValueParcelList =
                new ArrayList<ProfilingTriggerValueParcel>();

        for (int i = 0; i < triggerList.size(); i++) {
            triggerValueParcelList.add(triggerList.get(i).toValueParcel());
        }

        return triggerValueParcelList;
    }

    /** Remove triggers for this process with trigger types in the provided list. */
    @FlaggedApi(Flags.FLAG_SYSTEM_TRIGGERED_PROFILING_NEW)
    public void removeProfilingTriggersByType(@NonNull @TriggerType int[] triggers) {
        synchronized (mLock) {
            if (triggers.length == 0) {
                // No triggers are being removed, nothing to do.
                if (DEBUG) Log.d(TAG, "Trying to remove an empty list of triggers.");
                return;
            }

            final IProfilingService service = getOrCreateIProfilingServiceLocked(false);
            if (service == null) {
                // If we can't access service then we can't do anything. Return.
                if (DEBUG) {
                    Log.d(TAG, "ProfilingService is not available, triggers will not be removed.");
                }
                return;
            }

            String packageName = mContext.getPackageName();
            if (packageName == null) {
                if (DEBUG) Log.d(TAG, "Failed to resolve package name.");
                return;
            }

            try {
                service.removeProfilingTriggers(triggers, packageName);
            } catch (RemoteException e) {
                if (DEBUG) Log.d(TAG, "Binder exception processing request", e);
                throw new RuntimeException("Unable to remove profiling triggers.");
            }
        }
    }

    /** Remove all triggers for this process. */
    @FlaggedApi(Flags.FLAG_SYSTEM_TRIGGERED_PROFILING_NEW)
    public void clearProfilingTriggers() {
        synchronized (mLock) {
            final IProfilingService service = getOrCreateIProfilingServiceLocked(false);
            if (service == null) {
                // If we can't access service then we can't do anything. Return.
                if (DEBUG) {
                    Log.d(TAG, "ProfilingService is not available, triggers will not be removed.");
                }
                return;
            }

            String packageName = mContext.getPackageName();
            if (packageName == null) {
                if (DEBUG) Log.d(TAG, "Failed to resolve package name.");
                return;
            }

            try {
                service.clearProfilingTriggers(packageName);
            } catch (RemoteException e) {
                if (DEBUG) Log.d(TAG, "Binder exception processing request", e);
                throw new RuntimeException("Unable to clear profiling triggers.");
            }
        }
    }

    /** @hide */
    @VisibleForTesting
    @GuardedBy("mLock")
    public @Nullable IProfilingService getOrCreateIProfilingServiceLocked(
            boolean isGeneralListener) {
        // We only register the callback with registerResultsCallback once per binder object, and we
        // only create one binder object per ProfilingManager instance. If the object already exists
        // then it was successfully created and registered previously so we can just return it.
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
            mProfilingService.registerResultsCallback(isGeneralListener,
                    new IProfilingResultCallback.Stub() {

                        /**
                         * Called by {@link ProfilingService} when a result is ready,
                         * both for success and failure.
                         */
                        @Override
                        public void sendResult(@Nullable String resultFile, long keyMostSigBits,
                                long keyLeastSigBits, int status, @Nullable String tag,
                                @Nullable String error, int triggerType) {
                            synchronized (mLock) {
                                if (mCallbacks.isEmpty()) {
                                    // This shouldn't happen - no callbacks, nowhere to report this
                                    // result.
                                    if (DEBUG) Log.d(TAG, "No callbacks");
                                    mProfilingService = null;
                                    return;
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
                                        // At most 1 listener can have a key matching this result:
                                        // the one registered with the request, remove that one
                                        // only.
                                        if (removeListenerPos == -1) {
                                            removeListenerPos = i;
                                        } else {
                                            // This should never happen.
                                            if (DEBUG) {
                                                Log.d(TAG,
                                                        "More than 1 listener with the same key");
                                            }
                                        }
                                    } else if (wrapper.mKey != null) {
                                        // If the key is not null, and doesn't matched the result
                                        // key, then this key belongs to another request and should
                                        // not be triggered.
                                        continue;
                                    }

                                    // TODO: b/337017299 - check resultFile is valid before
                                    // returning Now trigger the callback for any listener that
                                    // doesn't belong to another request.
                                    wrapper.mExecutor.execute(() -> wrapper.mListener.accept(
                                            new ProfilingResult(overrideStatusToError
                                                    ? ProfilingResult.ERROR_UNKNOWN : status,
                                                    getAppFileDir() + resultFile, tag, error,
                                                    triggerType)));
                                }

                                // Remove the single listener that was tied to the request, if
                                // applicable.
                                if (removeListenerPos != -1) {
                                    mCallbacks.remove(removeListenerPos);
                                }
                            }
                        }

                        /**
                         * Called by {@link ProfilingService} when a trace is ready and needs to be
                         * copied to callers internal storage.
                         *
                         * This method will open a new file and pass back the FileDescriptor for
                         * ProfilingService to write to via a new binder call.
                         *
                         * Takes in key most/least significant bits which represent the key that
                         * will be used to associate this back to a profiling session which will
                         * write to the generated file.
                         */
                        @Override
                        public void generateFile(String filePathRelative, String fileName,
                                long keyMostSigBits, long keyLeastSigBits) {
                            synchronized (mLock) {
                                String filePathAbsolute = getAppFileDir() + filePathRelative;
                                try {
                                    // Ensure the profiling directory exists. Create it if it
                                    // doesn't.
                                    final File profilingDir = new File(filePathAbsolute);
                                    if (!profilingDir.exists()) {
                                        profilingDir.mkdir();
                                    }

                                    // Create the profiling file for the output to be written to.
                                    final File profilingFile = new File(
                                            filePathAbsolute + fileName);
                                    profilingFile.createNewFile();
                                    if (!profilingFile.exists()) {
                                        // Failed to create output file. Result may be lost.
                                        if (DEBUG) Log.d(TAG, "Output file couldn't be created");
                                        return;
                                    }

                                    // Wrap the new output file in a {@link ParcelFileDescriptor} to
                                    // send back to {@link ProfilingService} to write to.
                                    ParcelFileDescriptor pfd = ParcelFileDescriptor.open(
                                            profilingFile,
                                            ParcelFileDescriptor.MODE_READ_WRITE);
                                    IProfilingService service =
                                            getOrCreateIProfilingServiceLocked(false);

                                    if (service == null) {
                                        // Unable to send file descriptor because we have nowhere to
                                        // send it to. Result may be lost. Close descriptor and
                                        // delete file.
                                        if (DEBUG) Log.d(TAG, "Unable to send file descriptor");
                                        tryToCleanupGeneratedFile(pfd, profilingFile);
                                        return;
                                    }

                                    try {
                                        // Send the file descriptor to service to write to.
                                        service.receiveFileDescriptor(pfd, keyMostSigBits,
                                                keyLeastSigBits);
                                    } catch (RemoteException e) {
                                        // If we failed to send it, try to clean it up as it won't
                                        // be used.
                                        if (DEBUG) {
                                            Log.d(TAG, "Failed sending file descriptor to service",
                                                    e);
                                        }
                                        tryToCleanupGeneratedFile(pfd, profilingFile);
                                    }
                                } catch (Exception e) {
                                    // Failure prepping output file. Result may be lost.
                                    if (DEBUG) Log.d(TAG, "Exception preparing file", e);
                                    return;
                                }
                            }
                        }

                        /**
                         * Attempt to clean up the files created for service by closing the file
                         * descriptor and deleting the file. This is intended for error cases where
                         * the descriptor could not be sent. If it was successfully sent, service
                         * will handle closing it and requesting a delete if necessary.
                         */
                        private void tryToCleanupGeneratedFile(ParcelFileDescriptor fileDescriptor,
                                File file) {
                            if (fileDescriptor != null) {
                                try {
                                    fileDescriptor.close();
                                } catch (IOException e) {
                                    // Nothing else we can do, ignore.
                                    if (DEBUG) Log.d(TAG, "Failed to cleanup file descriptor", e);
                                }
                            }

                            if (file != null) {
                                try {
                                    file.delete();
                                } catch (SecurityException e) {
                                    // Nothing else we can do, ignore.
                                    if (DEBUG) Log.d(TAG, "Failed to cleanup file", e);
                                }
                            }
                        }

                        /**
                         * Delete a file. To be used only for files created by {@link generateFile}.
                         */
                        @Override
                        public void deleteFile(String relativeFilePathAndName) {
                            try {
                                Files.delete(Path.of(getAppFileDir() + relativeFilePathAndName));
                            } catch (Exception exception) {
                                if (DEBUG) Log.e(TAG, "Failed to delete file.", exception);
                            }
                        }

                        private String getAppFileDir() {
                            return mContext.getFilesDir().getPath();
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
