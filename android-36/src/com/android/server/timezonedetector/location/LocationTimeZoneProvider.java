/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.timezonedetector.location;

import static android.app.time.LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_IS_CERTAIN;
import static android.app.time.LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_IS_UNCERTAIN;
import static android.app.time.LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_NOT_PRESENT;
import static android.app.time.LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_NOT_READY;
import static android.service.timezone.TimeZoneProviderEvent.EVENT_TYPE_PERMANENT_FAILURE;
import static android.service.timezone.TimeZoneProviderEvent.EVENT_TYPE_SUGGESTION;
import static android.service.timezone.TimeZoneProviderEvent.EVENT_TYPE_UNCERTAIN;

import static com.android.server.timezonedetector.location.LocationTimeZoneManagerService.debugLog;
import static com.android.server.timezonedetector.location.LocationTimeZoneManagerService.warnLog;
import static com.android.server.timezonedetector.location.LocationTimeZoneProvider.ProviderState.PROVIDER_STATE_DESTROYED;
import static com.android.server.timezonedetector.location.LocationTimeZoneProvider.ProviderState.PROVIDER_STATE_PERM_FAILED;
import static com.android.server.timezonedetector.location.LocationTimeZoneProvider.ProviderState.PROVIDER_STATE_STARTED_CERTAIN;
import static com.android.server.timezonedetector.location.LocationTimeZoneProvider.ProviderState.PROVIDER_STATE_STARTED_INITIALIZING;
import static com.android.server.timezonedetector.location.LocationTimeZoneProvider.ProviderState.PROVIDER_STATE_STARTED_UNCERTAIN;
import static com.android.server.timezonedetector.location.LocationTimeZoneProvider.ProviderState.PROVIDER_STATE_STOPPED;

import android.annotation.ElapsedRealtimeLong;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.time.LocationTimeZoneAlgorithmStatus.ProviderStatus;
import android.os.Handler;
import android.os.SystemClock;
import android.service.timezone.TimeZoneProviderEvent;
import android.service.timezone.TimeZoneProviderStatus;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.timezonedetector.ConfigurationInternal;
import com.android.server.timezonedetector.Dumpable;
import com.android.server.timezonedetector.ReferenceWithHistory;
import com.android.server.timezonedetector.location.LocationTimeZoneProvider.ProviderState.ProviderStateEnum;
import com.android.server.timezonedetector.location.ThreadingDomain.SingleRunnableQueue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A facade used by the {@link LocationTimeZoneProviderController} to interact with a location time
 * zone provider. The provider implementation will typically have logic running in another process.
 *
 * <p>The provider is supplied with a {@link ProviderListener} via {@link
 * #initialize(ProviderListener)}. This starts communication of asynchronous detection / error
 * events back to the {@link LocationTimeZoneProviderController} via the {@link
 * ProviderListener#onProviderStateChange} method. This call must be made on the
 * {@link Handler} thread from the {@link ThreadingDomain} passed to the constructor.
 *
 * <p>This class is also responsible for monitoring the initialization timeout for a provider. i.e.
 * if the provider fails to send its first suggestion within a certain time, this is the component
 * responsible for generating the necessary "uncertain" event.
 *
 * <p>All incoming calls from the controller except for {@link
 * LocationTimeZoneProvider#dump(android.util.IndentingPrintWriter, String[])} will be made on the
 * {@link Handler} thread of the {@link ThreadingDomain} passed to the constructor.
 */
abstract class LocationTimeZoneProvider implements Dumpable {

    /**
     * Listener interface used by the {@link LocationTimeZoneProviderController} to register an
     * interest in provider events.
     */
    interface ProviderListener {
        /**
         * Indicated that a provider changed states. The {@code providerState} indicates which one
         */
        void onProviderStateChange(@NonNull ProviderState providerState);
    }

    /**
     * Listener interface used to log provider events for metrics.
     */
    interface ProviderMetricsLogger {
        /** Logs that a provider changed state. */
        void onProviderStateChanged(@ProviderStateEnum int stateEnum);
    }

    /**
     * Information about the provider's current state.
     */
    static class ProviderState {

        @IntDef(prefix = "PROVIDER_STATE_",
                value = { PROVIDER_STATE_UNKNOWN, PROVIDER_STATE_STARTED_INITIALIZING,
                PROVIDER_STATE_STARTED_CERTAIN, PROVIDER_STATE_STARTED_UNCERTAIN,
                PROVIDER_STATE_STOPPED, PROVIDER_STATE_PERM_FAILED, PROVIDER_STATE_DESTROYED })
        @Retention(RetentionPolicy.SOURCE)
        @Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
        @interface ProviderStateEnum {}

        /**
         * Uninitialized value. Must not be used afte {@link LocationTimeZoneProvider#initialize}.
         */
        static final @ProviderStateEnum int PROVIDER_STATE_UNKNOWN = 0;

        /**
         * The provider is started and has not reported its first event.
         */
        static final @ProviderStateEnum int PROVIDER_STATE_STARTED_INITIALIZING = 1;

        /**
         * The provider is started and most recently reported a "suggestion" event.
         */
        static final @ProviderStateEnum int PROVIDER_STATE_STARTED_CERTAIN = 2;

        /**
         * The provider is started and most recently reported an "uncertain" event.
         */
        static final @ProviderStateEnum int PROVIDER_STATE_STARTED_UNCERTAIN = 3;

        /**
         * The provider is stopped.
         *
         * This is the state after {@link #initialize} is called.
         */
        static final @ProviderStateEnum int PROVIDER_STATE_STOPPED = 4;

        /**
         * The provider has failed and cannot be restarted. This is a terminated state triggered by
         * the provider itself.
         *
         * Providers may enter this state any time after a provider is started.
         */
        static final @ProviderStateEnum int PROVIDER_STATE_PERM_FAILED = 5;

        /**
         * The provider has been destroyed by the controller and cannot be restarted. Similar to
         * {@link #PROVIDER_STATE_PERM_FAILED} except that a provider is set into this state.
         */
        static final @ProviderStateEnum int PROVIDER_STATE_DESTROYED = 6;

        /** The {@link LocationTimeZoneProvider} the state is for. */
        @NonNull public final LocationTimeZoneProvider provider;

        /** The state enum value of the current state. */
        public final @ProviderStateEnum int stateEnum;

        /**
         * The last {@link TimeZoneProviderEvent} received. Only populated when {@link #stateEnum}
         * is either {@link #PROVIDER_STATE_STARTED_CERTAIN} or {@link
         * #PROVIDER_STATE_STARTED_UNCERTAIN}, but it can be {@code null} then too if no event has
         * yet been received.
         */
        @Nullable public final TimeZoneProviderEvent event;

        /**
         * The user configuration associated with the current state. Only and always present when
         * {@link #stateEnum} is one of the started states.
         */
        @Nullable public final ConfigurationInternal currentUserConfiguration;

        /**
         * The time according to the elapsed realtime clock when the provider entered the current
         * state. Included for debugging, not used for equality.
         */
        @ElapsedRealtimeLong
        private final long mStateEntryTimeMillis;

        /**
         * Debug information providing context for the transition to this state. Included for
         * debugging, not used for equality.
         */
        @Nullable private final String mDebugInfo;


        private ProviderState(@NonNull LocationTimeZoneProvider provider,
                @ProviderStateEnum int stateEnum,
                @Nullable TimeZoneProviderEvent event,
                @Nullable ConfigurationInternal currentUserConfiguration,
                @Nullable String debugInfo) {
            this.provider = Objects.requireNonNull(provider);
            this.stateEnum = stateEnum;
            this.event = event;
            this.currentUserConfiguration = currentUserConfiguration;
            this.mStateEntryTimeMillis = SystemClock.elapsedRealtime();
            this.mDebugInfo = debugInfo;
        }

        /** Creates the bootstrap state, uses {@link #PROVIDER_STATE_UNKNOWN}. */
        static ProviderState createStartingState(
                @NonNull LocationTimeZoneProvider provider) {
            return new ProviderState(
                    provider, PROVIDER_STATE_UNKNOWN, null, null, "Initial state");
        }

        /**
         * Create a new state from this state. Validates that the state transition is valid
         * and that the required parameters for the new state are present / absent.
         */
        ProviderState newState(@ProviderStateEnum int newStateEnum,
                @Nullable TimeZoneProviderEvent event,
                @Nullable ConfigurationInternal currentUserConfig,
                @Nullable String debugInfo) {

            // Check valid "from" transitions.
            switch (this.stateEnum) {
                case PROVIDER_STATE_UNKNOWN: {
                    if (newStateEnum != PROVIDER_STATE_STOPPED) {
                        throw new IllegalArgumentException(
                                "Must transition from " + prettyPrintStateEnum(
                                        PROVIDER_STATE_UNKNOWN)
                                        + " to " + prettyPrintStateEnum(PROVIDER_STATE_STOPPED));
                    }
                    break;
                }
                case PROVIDER_STATE_STOPPED:
                case PROVIDER_STATE_STARTED_INITIALIZING:
                case PROVIDER_STATE_STARTED_CERTAIN:
                case PROVIDER_STATE_STARTED_UNCERTAIN: {
                    // These can go to each other or either of PROVIDER_STATE_PERM_FAILED and
                    // PROVIDER_STATE_DESTROYED.
                    break;
                }
                case PROVIDER_STATE_PERM_FAILED:
                case PROVIDER_STATE_DESTROYED: {
                    throw new IllegalArgumentException("Illegal transition out of "
                            + prettyPrintStateEnum(this.stateEnum));
                }
                default: {
                    throw new IllegalArgumentException("Invalid this.stateEnum=" + this.stateEnum);
                }
            }

            // Validate "to" transitions / arguments.
            switch (newStateEnum) {
                case PROVIDER_STATE_UNKNOWN: {
                    throw new IllegalArgumentException("Cannot transition to "
                            + prettyPrintStateEnum(PROVIDER_STATE_UNKNOWN));
                }
                case PROVIDER_STATE_STOPPED: {
                    if (event != null || currentUserConfig != null) {
                        throw new IllegalArgumentException(
                                "Stopped state: event and currentUserConfig must be null"
                                        + ", event=" + event
                                        + ", currentUserConfig=" + currentUserConfig);
                    }
                    break;
                }
                case PROVIDER_STATE_STARTED_INITIALIZING:
                case PROVIDER_STATE_STARTED_CERTAIN:
                case PROVIDER_STATE_STARTED_UNCERTAIN: {
                    if (currentUserConfig == null) {
                        throw new IllegalArgumentException(
                                "Started state: currentUserConfig must not be null");
                    }
                    break;
                }
                case PROVIDER_STATE_PERM_FAILED:
                case PROVIDER_STATE_DESTROYED: {
                    if (event != null || currentUserConfig != null) {
                        throw new IllegalArgumentException(
                                "Terminal state: event and currentUserConfig must be null"
                                        + ", newStateEnum=" + newStateEnum
                                        + ", event=" + event
                                        + ", currentUserConfig=" + currentUserConfig);
                    }
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Unknown newStateEnum=" + newStateEnum);
                }
            }
            return new ProviderState(provider, newStateEnum, event, currentUserConfig, debugInfo);
        }

        /** Returns {@code true} if {@link #stateEnum} is one of the started states. */
        boolean isStarted() {
            return stateEnum == PROVIDER_STATE_STARTED_INITIALIZING
                    || stateEnum == PROVIDER_STATE_STARTED_CERTAIN
                    || stateEnum == PROVIDER_STATE_STARTED_UNCERTAIN;
        }

        /** Returns {@code true} if {@link #stateEnum} is one of the terminated states. */
        boolean isTerminated() {
            return stateEnum == PROVIDER_STATE_PERM_FAILED
                    || stateEnum == PROVIDER_STATE_DESTROYED;
        }

        /**
         * Maps the internal state enum value to one of the status values exposed to the layers
         * above.
         */
        public @ProviderStatus int getProviderStatus() {
            switch (stateEnum) {
                case PROVIDER_STATE_STARTED_INITIALIZING:
                    return PROVIDER_STATUS_NOT_READY;
                case PROVIDER_STATE_STARTED_CERTAIN:
                    return PROVIDER_STATUS_IS_CERTAIN;
                case PROVIDER_STATE_STARTED_UNCERTAIN:
                    return PROVIDER_STATUS_IS_UNCERTAIN;
                case PROVIDER_STATE_PERM_FAILED:
                    // Perm failed means the providers wasn't configured, configured properly,
                    // or has removed itself for other reasons, e.g. turned-down server.
                    return PROVIDER_STATUS_NOT_PRESENT;
                case PROVIDER_STATE_STOPPED:
                case PROVIDER_STATE_DESTROYED:
                    // This is a "safe" default that best describes a provider that isn't in one of
                    // the more obviously mapped states.
                    return PROVIDER_STATUS_NOT_READY;
                case PROVIDER_STATE_UNKNOWN:
                default:
                    throw new IllegalStateException(
                            "Unknown state enum:" + prettyPrintStateEnum(stateEnum));
            }
        }

        /** Returns the status reported by the provider, if available. */
        @Nullable
        TimeZoneProviderStatus getReportedStatus() {
            return event == null ? null : event.getTimeZoneProviderStatus();
        }

        @Override
        public String toString() {
            // this.provider is omitted deliberately to avoid recursion, since the provider holds
            // a reference to its state.
            return "ProviderState{"
                    + "stateEnum=" + prettyPrintStateEnum(stateEnum)
                    + ", event=" + event
                    + ", currentUserConfiguration=" + currentUserConfiguration
                    + ", mStateEntryTimeMillis=" + mStateEntryTimeMillis
                    + ", mDebugInfo=" + mDebugInfo
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ProviderState state = (ProviderState) o;
            return stateEnum == state.stateEnum
                    && Objects.equals(event, state.event)
                    && Objects.equals(currentUserConfiguration, state.currentUserConfiguration);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stateEnum, event, currentUserConfiguration);
        }

        private static String prettyPrintStateEnum(@ProviderStateEnum int state) {
            switch (state) {
                case PROVIDER_STATE_STOPPED:
                    return "Stopped (" + PROVIDER_STATE_STOPPED + ")";
                case PROVIDER_STATE_STARTED_INITIALIZING:
                    return "Started initializing (" + PROVIDER_STATE_STARTED_INITIALIZING + ")";
                case PROVIDER_STATE_STARTED_CERTAIN:
                    return "Started certain (" + PROVIDER_STATE_STARTED_CERTAIN + ")";
                case PROVIDER_STATE_STARTED_UNCERTAIN:
                    return "Started uncertain (" + PROVIDER_STATE_STARTED_UNCERTAIN + ")";
                case PROVIDER_STATE_PERM_FAILED:
                    return "Perm failure (" + PROVIDER_STATE_PERM_FAILED + ")";
                case PROVIDER_STATE_DESTROYED:
                    return "Destroyed (" + PROVIDER_STATE_DESTROYED + ")";
                case PROVIDER_STATE_UNKNOWN:
                default:
                    return "Unknown (" + state + ")";
            }
        }
    }

    @NonNull private final ProviderMetricsLogger mProviderMetricsLogger;
    @NonNull final ThreadingDomain mThreadingDomain;
    @NonNull final Object mSharedLock;
    @NonNull final String mProviderName;

    /**
     * Usually {@code false} but can be set to {@code true} for testing.
     */
    private final boolean mRecordStateChanges;

    @GuardedBy("mSharedLock")
    @NonNull
    private final ArrayList<ProviderState> mRecordedStates = new ArrayList<>(0);

    /**
     * The current state (with history for debugging).
     */
    @GuardedBy("mSharedLock")
    final ReferenceWithHistory<ProviderState> mCurrentState = new ReferenceWithHistory<>(10);

    /**
     * Used for scheduling initialization timeouts, i.e. for providers that have just been started.
     */
    @NonNull private final SingleRunnableQueue mInitializationTimeoutQueue;

    // Non-null and effectively final after initialize() is called.
    ProviderListener mProviderListener;

    @NonNull private final TimeZoneProviderEventPreProcessor mTimeZoneProviderEventPreProcessor;

    /** Creates the instance. */
    LocationTimeZoneProvider(@NonNull ProviderMetricsLogger providerMetricsLogger,
            @NonNull ThreadingDomain threadingDomain,
            @NonNull String providerName,
            @NonNull TimeZoneProviderEventPreProcessor timeZoneProviderEventPreProcessor,
            boolean recordStateChanges) {
        mThreadingDomain = Objects.requireNonNull(threadingDomain);
        mProviderMetricsLogger = Objects.requireNonNull(providerMetricsLogger);
        mInitializationTimeoutQueue = threadingDomain.createSingleRunnableQueue();
        mSharedLock = threadingDomain.getLockObject();
        mProviderName = Objects.requireNonNull(providerName);
        mTimeZoneProviderEventPreProcessor =
                Objects.requireNonNull(timeZoneProviderEventPreProcessor);
        mRecordStateChanges = recordStateChanges;
    }

    /**
     * Initializes the provider. Called before the provider is first used.
     */
    final void initialize(@NonNull ProviderListener providerListener) {
        mThreadingDomain.assertCurrentThread();

        synchronized (mSharedLock) {
            if (mProviderListener != null) {
                throw new IllegalStateException("initialize already called");
            }
            mProviderListener = Objects.requireNonNull(providerListener);
            ProviderState currentState = ProviderState.createStartingState(this);
            currentState = currentState.newState(PROVIDER_STATE_STOPPED, null, null, "initialize");
            setCurrentState(currentState, false);

            boolean initializationSuccess;
            String initializationFailureReason;
            // Guard against uncaught exceptions due to initialization problems.
            try {
                initializationSuccess = onInitialize();
                initializationFailureReason = "onInitialize() returned false";
            } catch (RuntimeException e) {
                warnLog("Unable to initialize the provider due to exception", e);
                initializationSuccess = false;
                initializationFailureReason = "onInitialize() threw exception:" + e.getMessage();
            }

            if (!initializationSuccess) {
                currentState = currentState.newState(PROVIDER_STATE_PERM_FAILED, null, null,
                        "Failed to initialize: " + initializationFailureReason);
                setCurrentState(currentState, true);
            }
        }
    }

    /**
     * Implemented by subclasses to do work during {@link #initialize}.
     *
     * @return returns {@code true} on success, {@code false} if the provider should be considered
     *   "permanently failed" / disabled
     */
    @GuardedBy("mSharedLock")
    abstract boolean onInitialize();

    /**
     * Destroys the provider. Called after the provider is stopped. This instance will not be called
     * again by the {@link LocationTimeZoneProviderController}.
     */
    final void destroy() {
        mThreadingDomain.assertCurrentThread();

        synchronized (mSharedLock) {
            ProviderState currentState = mCurrentState.get();
            if (!currentState.isTerminated()) {
                ProviderState destroyedState =
                        currentState.newState(PROVIDER_STATE_DESTROYED, null, null, "destroy");
                setCurrentState(destroyedState, false);
                onDestroy();
            }
        }
    }

    /**
     * Implemented by subclasses to do work during {@link #destroy()}.
     */
    @GuardedBy("mSharedLock")
    abstract void onDestroy();

    /**
     * Clears recorded state changes.
     */
    final void clearRecordedStates() {
        mThreadingDomain.assertCurrentThread();
        synchronized (mSharedLock) {
            mRecordedStates.clear();
            mRecordedStates.trimToSize();
        }
    }

    /**
     * Returns recorded states.
     */
    final List<ProviderState> getRecordedStates() {
        mThreadingDomain.assertCurrentThread();
        synchronized (mSharedLock) {
            return new ArrayList<>(mRecordedStates);
        }
    }

    /**
     * Sets the current state. If {@code #notifyChanges} is {@code true} and {@code newState} is not
     * equal to the old state, then {@link ProviderListener#onProviderStateChange(ProviderState)}
     * will be called on {@link #mProviderListener}.
     */
    private void setCurrentState(@NonNull ProviderState newState, boolean notifyChanges) {
        mThreadingDomain.assertCurrentThread();
        synchronized (mSharedLock) {
            ProviderState oldState = mCurrentState.get();
            mCurrentState.set(newState);
            onSetCurrentState(newState);
            if (!Objects.equals(newState, oldState)) {
                mProviderMetricsLogger.onProviderStateChanged(newState.stateEnum);
                if (mRecordStateChanges) {
                    mRecordedStates.add(newState);
                }
                if (notifyChanges) {
                    mProviderListener.onProviderStateChange(newState);
                }
            }
        }
    }

    /**
     * Overridden by subclasses to do work during {@link #setCurrentState}.
     */
    @GuardedBy("mSharedLock")
    void onSetCurrentState(ProviderState newState) {
        // Default no-op.
    }

    /**
     * Returns the current state of the provider. This method must be called using the handler
     * thread from the {@link ThreadingDomain}.
     */
    @NonNull
    final ProviderState getCurrentState() {
        mThreadingDomain.assertCurrentThread();
        synchronized (mSharedLock) {
            return mCurrentState.get();
        }
    }

    /**
     * Returns the name of the provider. This method must be called using the handler thread from
     * the {@link ThreadingDomain}.
     */
    final String getName() {
        mThreadingDomain.assertCurrentThread();
        return mProviderName;
    }

    /**
     * Starts the provider. It is an error to call this method except when the {@link
     * #getCurrentState()} is at {@link ProviderState#PROVIDER_STATE_STOPPED}. This method must be
     * called using the handler thread from the {@link ThreadingDomain}.
     */
    final void startUpdates(@NonNull ConfigurationInternal currentUserConfiguration,
            @NonNull Duration initializationTimeout, @NonNull Duration initializationTimeoutFuzz,
            @NonNull Duration eventFilteringAgeThreshold) {
        mThreadingDomain.assertCurrentThread();

        synchronized (mSharedLock) {
            assertCurrentState(PROVIDER_STATE_STOPPED);

            ProviderState currentState = mCurrentState.get();
            ProviderState newState = currentState.newState(PROVIDER_STATE_STARTED_INITIALIZING,
                    null /* event */, currentUserConfiguration, "startUpdates");
            setCurrentState(newState, false);

            Duration delay = initializationTimeout.plus(initializationTimeoutFuzz);
            mInitializationTimeoutQueue.runDelayed(
                    this::handleInitializationTimeout, delay.toMillis());

            onStartUpdates(initializationTimeout, eventFilteringAgeThreshold);
        }
    }

    private void handleInitializationTimeout() {
        mThreadingDomain.assertCurrentThread();

        synchronized (mSharedLock) {
            ProviderState currentState = mCurrentState.get();
            if (currentState.stateEnum == PROVIDER_STATE_STARTED_INITIALIZING) {
                // On initialization timeout the provider becomes uncertain.
                ProviderState newState = currentState.newState(PROVIDER_STATE_STARTED_UNCERTAIN,
                        null /* event */, currentState.currentUserConfiguration,
                        "handleInitializationTimeout");
                setCurrentState(newState, true);
            } else {
                warnLog("handleInitializationTimeout: Initialization timeout triggered when in"
                        + " an unexpected state=" + currentState);
            }
        }
    }

    /**
     * Implemented by subclasses to do work during {@link #startUpdates}. This is where the logic
     * to start the real provider should be implemented.
     *
     * @param initializationTimeout the initialization timeout to pass to the provider
     * @param eventFilteringAgeThreshold the event filtering age threshold to pass to the provider
     */
    abstract void onStartUpdates(@NonNull Duration initializationTimeout,
            @NonNull Duration eventFilteringAgeThreshold);

    /**
     * Stops the provider. It is an error to call this method except when the {@link
     * #getCurrentState()} is one of the started states. This method must be
     * called using the handler thread from the {@link ThreadingDomain}.
     */
    final void stopUpdates() {
        mThreadingDomain.assertCurrentThread();

        synchronized (mSharedLock) {
            assertIsStarted();

            ProviderState currentState = mCurrentState.get();
            ProviderState newState =
                    currentState.newState(PROVIDER_STATE_STOPPED, null, null, "stopUpdates");
            setCurrentState(newState, false);

            cancelInitializationTimeoutIfSet();

            onStopUpdates();
        }
    }

    /**
     * Implemented by subclasses to do work during {@link #stopUpdates}.
     */
    abstract void onStopUpdates();

    /** For subclasses to invoke when a {@link TimeZoneProviderEvent} has been received. */
    final void handleTimeZoneProviderEvent(@NonNull TimeZoneProviderEvent timeZoneProviderEvent) {
        mThreadingDomain.assertCurrentThread();
        Objects.requireNonNull(timeZoneProviderEvent);

        timeZoneProviderEvent =
                mTimeZoneProviderEventPreProcessor.preProcess(timeZoneProviderEvent);

        synchronized (mSharedLock) {
            debugLog("handleTimeZoneProviderEvent: mProviderName=" + mProviderName
                    + ", timeZoneProviderEvent=" + timeZoneProviderEvent);

            ProviderState currentState = mCurrentState.get();
            int eventType = timeZoneProviderEvent.getType();
            switch (currentState.stateEnum) {
                case PROVIDER_STATE_DESTROYED:
                case PROVIDER_STATE_PERM_FAILED: {
                    // After entering a terminated state, there is nothing to do. The remote peer is
                    // supposed to stop sending events after it has reported perm failure.
                    warnLog("handleTimeZoneProviderEvent: Event=" + timeZoneProviderEvent
                            + " received for provider=" + this + " when in terminated state");
                    return;
                }
                case PROVIDER_STATE_STOPPED: {
                    switch (eventType) {
                        case EVENT_TYPE_PERMANENT_FAILURE: {
                            String msg = "handleTimeZoneProviderEvent:"
                                    + " Failure event=" + timeZoneProviderEvent
                                    + " received for stopped provider=" + mProviderName
                                    + ", entering permanently failed state";
                            warnLog(msg);
                            ProviderState newState = currentState.newState(
                                    PROVIDER_STATE_PERM_FAILED, null, null, msg);
                            setCurrentState(newState, true);
                            cancelInitializationTimeoutIfSet();
                            return;
                        }
                        case EVENT_TYPE_SUGGESTION:
                        case EVENT_TYPE_UNCERTAIN: {
                            // Any geolocation-related events received for a stopped provider are
                            // ignored: they should not happen.
                            warnLog("handleTimeZoneProviderEvent:"
                                    + " event=" + timeZoneProviderEvent
                                    + " received for stopped provider=" + this
                                    + ", ignoring");

                            return;
                        }
                        default: {
                            throw new IllegalStateException(
                                    "Unknown eventType=" + timeZoneProviderEvent);
                        }
                    }
                }
                case PROVIDER_STATE_STARTED_INITIALIZING:
                case PROVIDER_STATE_STARTED_CERTAIN:
                case PROVIDER_STATE_STARTED_UNCERTAIN: {
                    switch (eventType) {
                        case EVENT_TYPE_PERMANENT_FAILURE: {
                            String msg = "handleTimeZoneProviderEvent:"
                                    + " Failure event=" + timeZoneProviderEvent
                                    + " received for provider=" + mProviderName
                                    + " in state=" + ProviderState.prettyPrintStateEnum(
                                            currentState.stateEnum)
                                    + ", entering permanently failed state";
                            warnLog(msg);
                            ProviderState newState = currentState.newState(
                                    PROVIDER_STATE_PERM_FAILED, null, null, msg);
                            setCurrentState(newState, true);
                            cancelInitializationTimeoutIfSet();

                            return;
                        }
                        case EVENT_TYPE_UNCERTAIN:
                        case EVENT_TYPE_SUGGESTION: {
                            @ProviderStateEnum int providerStateEnum;
                            if (eventType == EVENT_TYPE_UNCERTAIN) {
                                providerStateEnum = PROVIDER_STATE_STARTED_UNCERTAIN;
                            } else {
                                providerStateEnum = PROVIDER_STATE_STARTED_CERTAIN;
                            }
                            ProviderState newState = currentState.newState(providerStateEnum,
                                    timeZoneProviderEvent, currentState.currentUserConfiguration,
                                    "handleTimeZoneProviderEvent");
                            setCurrentState(newState, true);
                            cancelInitializationTimeoutIfSet();
                            return;
                        }
                        default: {
                            throw new IllegalStateException(
                                    "Unknown eventType=" + timeZoneProviderEvent);
                        }
                    }
                }
                default: {
                    throw new IllegalStateException("Unknown providerType=" + currentState);
                }
            }
        }
    }

    /** For subclasses to invoke when needing to report a temporary failure. */
    final void handleTemporaryFailure(String reason) {
        mThreadingDomain.assertCurrentThread();

        synchronized (mSharedLock) {
            ProviderState currentState = mCurrentState.get();
            switch (currentState.stateEnum) {
                case PROVIDER_STATE_STARTED_INITIALIZING:
                case PROVIDER_STATE_STARTED_UNCERTAIN:
                case PROVIDER_STATE_STARTED_CERTAIN: {
                    // A temporary failure is treated as becoming uncertain.

                    // This is an unusual PROVIDER_STATE_STARTED_UNCERTAIN state because
                    // event == null
                    String debugInfo = "handleTemporaryFailure: reason=" + reason
                            + ", currentState=" + ProviderState.prettyPrintStateEnum(
                                    currentState.stateEnum);
                    ProviderState newState = currentState.newState(PROVIDER_STATE_STARTED_UNCERTAIN,
                            null, currentState.currentUserConfiguration, debugInfo);
                    setCurrentState(newState, true);
                    cancelInitializationTimeoutIfSet();
                    break;
                }
                case PROVIDER_STATE_STOPPED: {
                    debugLog("handleProviderLost reason=" + reason
                            + ", mProviderName=" + mProviderName
                            + ", currentState=" + currentState
                            + ": No state change required, provider is stopped.");
                    break;
                }
                case PROVIDER_STATE_PERM_FAILED:
                case PROVIDER_STATE_DESTROYED: {
                    debugLog("handleProviderLost reason=" + reason
                            + ", mProviderName=" + mProviderName
                            + ", currentState=" + currentState
                            + ": No state change required, provider is terminated.");
                    break;
                }
                default: {
                    throw new IllegalStateException("Unknown currentState=" + currentState);
                }
            }
        }
    }

    @GuardedBy("mSharedLock")
    private void assertIsStarted() {
        ProviderState currentState = mCurrentState.get();
        if (!currentState.isStarted()) {
            throw new IllegalStateException("Required a started state, but was " + currentState);
        }
    }

    @GuardedBy("mSharedLock")
    private void assertCurrentState(@ProviderStateEnum int requiredState) {
        ProviderState currentState = mCurrentState.get();
        if (currentState.stateEnum != requiredState) {
            throw new IllegalStateException(
                    "Required stateEnum=" + requiredState + ", but was " + currentState);
        }
    }

    @VisibleForTesting
    boolean isInitializationTimeoutSet() {
        synchronized (mSharedLock) {
            return mInitializationTimeoutQueue.hasQueued();
        }
    }

    @GuardedBy("mSharedLock")
    private void cancelInitializationTimeoutIfSet() {
        if (mInitializationTimeoutQueue.hasQueued()) {
            mInitializationTimeoutQueue.cancel();
        }
    }

    @VisibleForTesting
    Duration getInitializationTimeoutDelay() {
        synchronized (mSharedLock) {
            return Duration.ofMillis(mInitializationTimeoutQueue.getQueuedDelayMillis());
        }
    }
}
