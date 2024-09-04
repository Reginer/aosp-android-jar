/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.safetycenter;

import static android.Manifest.permission.MANAGE_SAFETY_CENTER;
import static android.Manifest.permission.READ_SAFETY_CENTER_STATUS;
import static android.Manifest.permission.SEND_SAFETY_CENTER_UPDATE;
import static android.annotation.SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION;
import static android.os.Build.VERSION_CODES.TIRAMISU;
import static android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

import static java.util.Objects.requireNonNull;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.RemoteException;
import android.safetycenter.config.SafetyCenterConfig;
import android.util.ArrayMap;

import androidx.annotation.RequiresApi;

import com.android.internal.annotations.GuardedBy;
import com.android.modules.utils.build.SdkLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Interface for communicating with the Safety Center, which consolidates UI for security and
 * privacy features on the device.
 *
 * <p>These APIs are intended to be used by the following clients:
 *
 * <ul>
 *   <li>Safety sources represented in Safety Center UI
 *   <li>Dependents on the state of Safety Center UI
 *   <li>Managers of Safety Center UI
 * </ul>
 *
 * @hide
 */
@SystemService(Context.SAFETY_CENTER_SERVICE)
@SystemApi
@RequiresApi(TIRAMISU)
public final class SafetyCenterManager {

    /**
     * Broadcast Action: A broadcast sent by the system to indicate that the value returned by
     * {@link SafetyCenterManager#isSafetyCenterEnabled()} has changed.
     *
     * <p>This broadcast will inform receivers about changes to {@link
     * SafetyCenterManager#isSafetyCenterEnabled()}, should they want to check the new value and
     * enable/disable components accordingly.
     *
     * <p>This broadcast is sent explicitly to safety sources by targeting intents to a specified
     * set of packages in the {@link SafetyCenterConfig}. The receiving components must hold the
     * {@link android.Manifest.permission#SEND_SAFETY_CENTER_UPDATE} permission, and can use a
     * manifest-registered receiver to be woken up by Safety Center.
     *
     * <p>This broadcast is also sent implicitly system-wide. The receiving components must hold the
     * {@link android.Manifest.permission#READ_SAFETY_CENTER_STATUS} permission.
     *
     * <p>This broadcast is not sent out if the device does not support Safety Center.
     *
     * <p class="note">This is a protected intent that can only be sent by the system.
     */
    @SdkConstant(BROADCAST_INTENT_ACTION)
    public static final String ACTION_SAFETY_CENTER_ENABLED_CHANGED =
            "android.safetycenter.action.SAFETY_CENTER_ENABLED_CHANGED";

    /**
     * Broadcast Action: A broadcast sent by the system to indicate that {@link SafetyCenterManager}
     * is requesting data from safety sources regarding their safety state.
     *
     * <p>This broadcast is sent when a user triggers a data refresh from the Safety Center UI or
     * when Safety Center detects that its stored safety information is stale and needs to be
     * updated.
     *
     * <p>This broadcast is sent explicitly to safety sources by targeting intents to a specified
     * set of packages provided by the safety sources in the {@link SafetyCenterConfig}. The
     * receiving components must hold the {@link
     * android.Manifest.permission#SEND_SAFETY_CENTER_UPDATE} permission, and can use a
     * manifest-registered receiver to be woken up by Safety Center.
     *
     * <p>On receiving this broadcast, safety sources should determine their safety state according
     * to the parameters specified in the intent extras (see below) and set {@link SafetySourceData}
     * using {@link #setSafetySourceData}, along with a {@link SafetyEvent} with {@link
     * SafetyEvent#getType()} set to {@link SafetyEvent#SAFETY_EVENT_TYPE_REFRESH_REQUESTED} and
     * {@link SafetyEvent#getRefreshBroadcastId()} set to the value of broadcast intent extra {@link
     * #EXTRA_REFRESH_SAFETY_SOURCES_BROADCAST_ID}. If the safety source is unable to provide data,
     * it can set a {@code null} {@link SafetySourceData}, which will clear any existing {@link
     * SafetySourceData} stored by Safety Center, and Safety Center will fall back to any
     * placeholder data specified in {@link SafetyCenterConfig}.
     *
     * <p class="note">This is a protected intent that can only be sent by the system.
     *
     * <p>Includes the following extras:
     *
     * <ul>
     *   <li>{@link #EXTRA_REFRESH_SAFETY_SOURCES_REQUEST_TYPE}: An int representing the type of
     *       data being requested. Possible values are {@link
     *       #EXTRA_REFRESH_REQUEST_TYPE_FETCH_FRESH_DATA} and {@link
     *       #EXTRA_REFRESH_REQUEST_TYPE_GET_DATA}.
     *   <li>{@link #EXTRA_REFRESH_SAFETY_SOURCE_IDS}: A {@code String[]} of ids representing the
     *       safety sources being requested for data. This extra exists for disambiguation in the
     *       case that a single component is responsible for receiving refresh requests for multiple
     *       safety sources.
     *   <li>{@link #EXTRA_REFRESH_SAFETY_SOURCES_BROADCAST_ID}: An unique identifier for the
     *       refresh request broadcast. This extra should be used to specify {@link
     *       SafetyEvent#getRefreshBroadcastId()} when the safety source responds to the broadcast
     *       using {@link #setSafetySourceData}.
     * </ul>
     */
    @SdkConstant(BROADCAST_INTENT_ACTION)
    public static final String ACTION_REFRESH_SAFETY_SOURCES =
            "android.safetycenter.action.REFRESH_SAFETY_SOURCES";

    /**
     * Used as a {@code String[]} extra field in {@link #ACTION_REFRESH_SAFETY_SOURCES} intents to
     * specify the safety source ids of the safety sources being requested for data by Safety
     * Center.
     *
     * <p>When this extra field is not specified in the intent, it is assumed that Safety Center is
     * requesting data from all safety sources supported by the component receiving the broadcast.
     */
    public static final String EXTRA_REFRESH_SAFETY_SOURCE_IDS =
            "android.safetycenter.extra.REFRESH_SAFETY_SOURCE_IDS";

    /**
     * Used as an {@code int} extra field in {@link #ACTION_REFRESH_SAFETY_SOURCES} intents to
     * specify the type of data request from Safety Center.
     *
     * <p>Possible values are {@link #EXTRA_REFRESH_REQUEST_TYPE_FETCH_FRESH_DATA} and {@link
     * #EXTRA_REFRESH_REQUEST_TYPE_GET_DATA}
     */
    public static final String EXTRA_REFRESH_SAFETY_SOURCES_REQUEST_TYPE =
            "android.safetycenter.extra.REFRESH_SAFETY_SOURCES_REQUEST_TYPE";

    /**
     * Used as a {@code String} extra field in {@link #ACTION_REFRESH_SAFETY_SOURCES} intents to
     * specify a string identifier for the broadcast.
     */
    public static final String EXTRA_REFRESH_SAFETY_SOURCES_BROADCAST_ID =
            "android.safetycenter.extra.REFRESH_SAFETY_SOURCES_BROADCAST_ID";

    /**
     * Used as a {@code String} extra field in {@link Intent#ACTION_SAFETY_CENTER} intents to
     * specify an issue ID to redirect to, if applicable.
     *
     * <p>This extra must be used in conjunction with {@link #EXTRA_SAFETY_SOURCE_ID} as an issue ID
     * does not uniquely identify a {@link SafetySourceIssue}. Otherwise, no redirection will occur.
     */
    public static final String EXTRA_SAFETY_SOURCE_ISSUE_ID =
            "android.safetycenter.extra.SAFETY_SOURCE_ISSUE_ID";

    /**
     * Used as a {@code String} extra field in {@link Intent#ACTION_SAFETY_CENTER} intents to
     * specify a source ID for the {@link SafetySourceIssue} to redirect to, if applicable.
     *
     * <p>This extra must be used in conjunction with {@link #EXTRA_SAFETY_SOURCE_ISSUE_ID}.
     * Otherwise, no redirection will occur.
     */
    public static final String EXTRA_SAFETY_SOURCE_ID =
            "android.safetycenter.extra.SAFETY_SOURCE_ID";

    /**
     * Used as a {@link android.os.UserHandle} extra field in {@link Intent#ACTION_SAFETY_CENTER}
     * intents to specify a user for a given {@link SafetySourceIssue} to redirect to, if
     * applicable.
     *
     * <p>This extra can be used if the same issue ID is created for multiple users (e.g. to
     * disambiguate personal profile vs. managed profiles issues).
     *
     * <p>This extra can be used in conjunction with {@link #EXTRA_SAFETY_SOURCE_ISSUE_ID} and
     * {@link #EXTRA_SAFETY_SOURCE_ID}. Otherwise, the device's primary user will be used.
     */
    public static final String EXTRA_SAFETY_SOURCE_USER_HANDLE =
            "android.safetycenter.extra.SAFETY_SOURCE_USER_HANDLE";

    /**
     * Used as a {@code String} extra field in {@link Intent#ACTION_SAFETY_CENTER} intents to
     * specify the ID for a group of safety sources. If applicable, this will redirect to the
     * group's corresponding subpage in the UI.
     */
    @RequiresApi(UPSIDE_DOWN_CAKE)
    public static final String EXTRA_SAFETY_SOURCES_GROUP_ID =
            "android.safetycenter.extra.SAFETY_SOURCES_GROUP_ID";

    /**
     * Used as an int value for {@link #EXTRA_REFRESH_SAFETY_SOURCES_REQUEST_TYPE} to indicate that
     * the safety source should fetch fresh data relating to their safety state upon receiving a
     * broadcast with intent action {@link #ACTION_REFRESH_SAFETY_SOURCES} and provide it to Safety
     * Center.
     *
     * <p>The term "fresh" here means that the sources should ensure that the safety data is
     * accurate as possible at the time of providing it to Safety Center, even if it involves
     * performing an expensive and/or slow process.
     */
    public static final int EXTRA_REFRESH_REQUEST_TYPE_FETCH_FRESH_DATA = 0;

    /**
     * Used as an int value for {@link #EXTRA_REFRESH_SAFETY_SOURCES_REQUEST_TYPE} to indicate that
     * upon receiving a broadcast with intent action {@link #ACTION_REFRESH_SAFETY_SOURCES}, the
     * safety source should provide data relating to their safety state to Safety Center.
     *
     * <p>If the source already has its safety data cached, it may provide it without triggering a
     * process to fetch state which may be expensive and/or slow.
     */
    public static final int EXTRA_REFRESH_REQUEST_TYPE_GET_DATA = 1;

    /**
     * All possible types of data refresh requests in broadcasts with intent action {@link
     * #ACTION_REFRESH_SAFETY_SOURCES}.
     *
     * @hide
     */
    @IntDef(
            prefix = {"EXTRA_REFRESH_REQUEST_TYPE_"},
            value = {
                EXTRA_REFRESH_REQUEST_TYPE_FETCH_FRESH_DATA,
                EXTRA_REFRESH_REQUEST_TYPE_GET_DATA,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RefreshRequestType {}

    /** Indicates that the Safety Center UI has been opened by the user. */
    public static final int REFRESH_REASON_PAGE_OPEN = 100;

    /** Indicates that the rescan button in the Safety Center UI has been clicked on by the user. */
    public static final int REFRESH_REASON_RESCAN_BUTTON_CLICK = 200;

    /** Indicates that the device was rebooted. */
    public static final int REFRESH_REASON_DEVICE_REBOOT = 300;

    /** Indicates that the device locale was changed. */
    public static final int REFRESH_REASON_DEVICE_LOCALE_CHANGE = 400;

    /** Indicates that the Safety Center feature was enabled. */
    public static final int REFRESH_REASON_SAFETY_CENTER_ENABLED = 500;

    /** Indicates a generic reason for Safety Center refresh. */
    public static final int REFRESH_REASON_OTHER = 600;

    /** Indicates a periodic background refresh. */
    @RequiresApi(UPSIDE_DOWN_CAKE)
    public static final int REFRESH_REASON_PERIODIC = 700;

    /**
     * The reason for requesting a refresh of {@link SafetySourceData} from safety sources.
     *
     * @hide
     */
    @IntDef(
            prefix = {"REFRESH_REASON_"},
            value = {
                REFRESH_REASON_PAGE_OPEN,
                REFRESH_REASON_RESCAN_BUTTON_CLICK,
                REFRESH_REASON_DEVICE_REBOOT,
                REFRESH_REASON_DEVICE_LOCALE_CHANGE,
                REFRESH_REASON_SAFETY_CENTER_ENABLED,
                REFRESH_REASON_OTHER,
                REFRESH_REASON_PERIODIC
            })
    @Retention(RetentionPolicy.SOURCE)
    @TargetApi(UPSIDE_DOWN_CAKE)
    public @interface RefreshReason {}

    /** Listener for changes to {@link SafetyCenterData}. */
    public interface OnSafetyCenterDataChangedListener {

        /**
         * Called when {@link SafetyCenterData} tracked by the manager changes.
         *
         * @param data the updated data
         */
        void onSafetyCenterDataChanged(@NonNull SafetyCenterData data);

        /**
         * Called when the Safety Center should display an error related to changes in its data.
         *
         * @param errorDetails details of an error that should be displayed to the user
         */
        default void onError(@NonNull SafetyCenterErrorDetails errorDetails) {}
    }

    private final Object mListenersLock = new Object();

    @GuardedBy("mListenersLock")
    private final Map<OnSafetyCenterDataChangedListener, ListenerDelegate> mListenersToDelegates =
            new ArrayMap<>();

    @NonNull private final Context mContext;
    @NonNull private final ISafetyCenterManager mService;

    /**
     * Creates a new instance of the {@link SafetyCenterManager}.
     *
     * @param context the {@link Context}
     * @param service the {@link ISafetyCenterManager} service
     * @hide
     */
    public SafetyCenterManager(@NonNull Context context, @NonNull ISafetyCenterManager service) {
        this.mContext = context;
        this.mService = service;
    }

    /**
     * Returns whether the Safety Center feature is enabled.
     *
     * <p>If this returns {@code false}, all the other methods in this class will no-op and/or
     * return default values.
     */
    @RequiresPermission(anyOf = {READ_SAFETY_CENTER_STATUS, SEND_SAFETY_CENTER_UPDATE})
    public boolean isSafetyCenterEnabled() {
        try {
            return mService.isSafetyCenterEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Set the latest {@link SafetySourceData} for a safety source, to be displayed in Safety Center
     * UI.
     *
     * <p>Each {@code safetySourceId} uniquely identifies the {@link SafetySourceData} for the
     * calling user.
     *
     * <p>This call will rewrite any existing {@link SafetySourceData} already set for the given
     * {@code safetySourceId} for the calling user.
     *
     * @param safetySourceId the unique identifier for a safety source in the calling user
     * @param safetySourceData the latest safety data for the safety source in the calling user. If
     *     a safety source does not have any data to set, it can set its {@link SafetySourceData} to
     *     {@code null}, in which case Safety Center will fall back to any placeholder data
     *     specified in the safety source xml configuration.
     * @param safetyEvent the event that triggered the safety source to set safety data
     */
    @RequiresPermission(SEND_SAFETY_CENTER_UPDATE)
    public void setSafetySourceData(
            @NonNull String safetySourceId,
            @Nullable SafetySourceData safetySourceData,
            @NonNull SafetyEvent safetyEvent) {
        requireNonNull(safetySourceId, "safetySourceId cannot be null");
        requireNonNull(safetyEvent, "safetyEvent cannot be null");

        try {
            mService.setSafetySourceData(
                    safetySourceId,
                    safetySourceData,
                    safetyEvent,
                    mContext.getPackageName(),
                    mContext.getUser().getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the latest {@link SafetySourceData} set through {@link #setSafetySourceData} for the
     * given {@code safetySourceId} and calling user.
     *
     * <p>Returns {@code null} if there never was any data sent for the given {@code safetySourceId}
     * and user.
     */
    @RequiresPermission(SEND_SAFETY_CENTER_UPDATE)
    @Nullable
    public SafetySourceData getSafetySourceData(@NonNull String safetySourceId) {
        requireNonNull(safetySourceId, "safetySourceId cannot be null");

        try {
            return mService.getSafetySourceData(
                    safetySourceId, mContext.getPackageName(), mContext.getUser().getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Notifies the Safety Center of an error related to a given safety source.
     *
     * <p>Safety sources should use this API to notify Safety Center when Safety Center requested or
     * expected them to perform an action or provide data, but they were unable to do so.
     *
     * @param safetySourceId the id of the safety source that provided the issue
     * @param safetySourceErrorDetails details of the error that occurred
     */
    @RequiresPermission(SEND_SAFETY_CENTER_UPDATE)
    public void reportSafetySourceError(
            @NonNull String safetySourceId,
            @NonNull SafetySourceErrorDetails safetySourceErrorDetails) {
        requireNonNull(safetySourceId, "safetySourceId cannot be null");
        requireNonNull(safetySourceErrorDetails, "safetySourceErrorDetails cannot be null");

        try {
            mService.reportSafetySourceError(
                    safetySourceId,
                    safetySourceErrorDetails,
                    mContext.getPackageName(),
                    mContext.getUser().getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Requests safety sources to set their latest {@link SafetySourceData} for Safety Center.
     *
     * <p>This API sends a broadcast to all safety sources with action {@link
     * #ACTION_REFRESH_SAFETY_SOURCES}. See {@link #ACTION_REFRESH_SAFETY_SOURCES} for details on
     * how safety sources should respond to receiving these broadcasts.
     *
     * @param refreshReason the reason for the refresh
     */
    @RequiresPermission(MANAGE_SAFETY_CENTER)
    public void refreshSafetySources(@RefreshReason int refreshReason) {
        try {
            mService.refreshSafetySources(refreshReason, mContext.getUser().getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Requests a specific subset of safety sources to set their latest {@link SafetySourceData} for
     * Safety Center.
     *
     * <p>This API sends a broadcast to safety sources with action {@link
     * #ACTION_REFRESH_SAFETY_SOURCES} and {@link #EXTRA_REFRESH_SAFETY_SOURCE_IDS} to specify the
     * IDs of safety sources being requested for data by Safety Center.
     *
     * <p>This API is an overload of {@link #refreshSafetySources(int)} and is used to request data
     * from safety sources that are part of a subpage in the Safety Center UI.
     *
     * @see #refreshSafetySources(int)
     * @param refreshReason the reason for the refresh
     * @param safetySourceIds list of IDs for the safety sources being refreshed
     * @throws UnsupportedOperationException if accessed from a version lower than {@link
     *     UPSIDE_DOWN_CAKE}
     */
    @RequiresPermission(MANAGE_SAFETY_CENTER)
    @RequiresApi(UPSIDE_DOWN_CAKE)
    public void refreshSafetySources(
            @RefreshReason int refreshReason, @NonNull List<String> safetySourceIds) {
        if (!SdkLevel.isAtLeastU()) {
            throw new UnsupportedOperationException(
                    "Method not supported on versions lower than UPSIDE_DOWN_CAKE");
        }

        requireNonNull(safetySourceIds, "safetySourceIds cannot be null");

        try {
            mService.refreshSpecificSafetySources(
                    refreshReason, mContext.getUser().getIdentifier(), safetySourceIds);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Returns the current {@link SafetyCenterConfig}, if available. */
    @RequiresPermission(MANAGE_SAFETY_CENTER)
    @Nullable
    public SafetyCenterConfig getSafetyCenterConfig() {
        try {
            return mService.getSafetyCenterConfig();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the current {@link SafetyCenterData}, assembled from {@link SafetySourceData} from
     * all sources.
     */
    @RequiresPermission(MANAGE_SAFETY_CENTER)
    @NonNull
    public SafetyCenterData getSafetyCenterData() {
        try {
            return mService.getSafetyCenterData(
                    mContext.getPackageName(), mContext.getUser().getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Adds a listener for changes to {@link SafetyCenterData}.
     *
     * @see #removeOnSafetyCenterDataChangedListener(OnSafetyCenterDataChangedListener)
     */
    @RequiresPermission(MANAGE_SAFETY_CENTER)
    public void addOnSafetyCenterDataChangedListener(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OnSafetyCenterDataChangedListener listener) {
        requireNonNull(executor, "executor cannot be null");
        requireNonNull(listener, "listener cannot be null");

        synchronized (mListenersLock) {
            if (mListenersToDelegates.containsKey(listener)) return;

            ListenerDelegate delegate = new ListenerDelegate(executor, listener);
            try {
                mService.addOnSafetyCenterDataChangedListener(
                        delegate, mContext.getPackageName(), mContext.getUser().getIdentifier());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
            mListenersToDelegates.put(listener, delegate);
        }
    }

    /**
     * Removes a listener for changes to {@link SafetyCenterData}.
     *
     * @see #addOnSafetyCenterDataChangedListener(Executor, OnSafetyCenterDataChangedListener)
     */
    @RequiresPermission(MANAGE_SAFETY_CENTER)
    public void removeOnSafetyCenterDataChangedListener(
            @NonNull OnSafetyCenterDataChangedListener listener) {
        requireNonNull(listener, "listener cannot be null");

        synchronized (mListenersLock) {
            ListenerDelegate delegate = mListenersToDelegates.get(listener);
            if (delegate == null) return;

            try {
                mService.removeOnSafetyCenterDataChangedListener(
                        delegate, mContext.getUser().getIdentifier());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
            delegate.markAsRemoved();
            mListenersToDelegates.remove(listener);
        }
    }

    /**
     * Dismiss a Safety Center issue and prevent it from affecting the overall safety status.
     *
     * @param safetyCenterIssueId the target issue ID returned by {@link SafetyCenterIssue#getId()}
     */
    @RequiresPermission(MANAGE_SAFETY_CENTER)
    public void dismissSafetyCenterIssue(@NonNull String safetyCenterIssueId) {
        requireNonNull(safetyCenterIssueId, "safetyCenterIssueId cannot be null");

        try {
            mService.dismissSafetyCenterIssue(
                    safetyCenterIssueId, mContext.getUser().getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Executes the specified Safety Center issue action on the specified Safety Center issue.
     *
     * @param safetyCenterIssueId the target issue ID returned by {@link SafetyCenterIssue#getId()}
     * @param safetyCenterIssueActionId the target action ID returned by {@link
     *     SafetyCenterIssue.Action#getId()}
     */
    @RequiresPermission(MANAGE_SAFETY_CENTER)
    public void executeSafetyCenterIssueAction(
            @NonNull String safetyCenterIssueId, @NonNull String safetyCenterIssueActionId) {
        requireNonNull(safetyCenterIssueId, "safetyCenterIssueId cannot be null");
        requireNonNull(safetyCenterIssueActionId, "safetyCenterIssueActionId cannot be null");

        try {
            mService.executeSafetyCenterIssueAction(
                    safetyCenterIssueId,
                    safetyCenterIssueActionId,
                    mContext.getUser().getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Clears all {@link SafetySourceData} (set by safety sources using {@link
     * #setSafetySourceData}) for testing.
     *
     * <p>Note: This API serves to facilitate CTS testing and should not be used for other purposes.
     */
    @RequiresPermission(MANAGE_SAFETY_CENTER)
    public void clearAllSafetySourceDataForTests() {
        try {
            mService.clearAllSafetySourceDataForTests();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Overrides the {@link SafetyCenterConfig} for testing.
     *
     * <p>When set, the overridden {@link SafetyCenterConfig} will be used instead of the {@link
     * SafetyCenterConfig} parsed from the XML file to read configured safety sources.
     *
     * <p>Note: This API serves to facilitate CTS testing and should not be used to configure safety
     * sources dynamically for production. Once used for testing, the override should be cleared.
     *
     * @see #clearSafetyCenterConfigForTests()
     */
    @RequiresPermission(MANAGE_SAFETY_CENTER)
    public void setSafetyCenterConfigForTests(@NonNull SafetyCenterConfig safetyCenterConfig) {
        requireNonNull(safetyCenterConfig, "safetyCenterConfig cannot be null");

        try {
            mService.setSafetyCenterConfigForTests(safetyCenterConfig);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Clears the override of the {@link SafetyCenterConfig} set for testing.
     *
     * <p>Once cleared, the {@link SafetyCenterConfig} parsed from the XML file will be used to read
     * configured safety sources.
     *
     * <p>Note: This API serves to facilitate CTS testing and should not be used for other purposes.
     *
     * @see #setSafetyCenterConfigForTests(SafetyCenterConfig)
     */
    @RequiresPermission(MANAGE_SAFETY_CENTER)
    public void clearSafetyCenterConfigForTests() {
        try {
            mService.clearSafetyCenterConfigForTests();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static final class ListenerDelegate extends IOnSafetyCenterDataChangedListener.Stub {
        @NonNull private final Executor mExecutor;
        @NonNull private final OnSafetyCenterDataChangedListener mOriginalListener;

        private volatile boolean mRemoved = false;

        private ListenerDelegate(
                @NonNull Executor executor,
                @NonNull OnSafetyCenterDataChangedListener originalListener) {
            mExecutor = executor;
            mOriginalListener = originalListener;
        }

        @Override
        public void onSafetyCenterDataChanged(@NonNull SafetyCenterData safetyCenterData) {
            requireNonNull(safetyCenterData, "safetyCenterData cannot be null");

            final long identity = Binder.clearCallingIdentity();
            try {
                mExecutor.execute(
                        () -> {
                            if (mRemoved) {
                                return;
                            }
                            // The remove call could still complete at this point, but we're ok
                            // with this raciness on separate threads. If the listener is removed on
                            // the same thread as `mExecutor`; the above check should ensure that
                            // the listener won't be called after the remove call.
                            mOriginalListener.onSafetyCenterDataChanged(safetyCenterData);
                        });
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public void onError(@NonNull SafetyCenterErrorDetails safetyCenterErrorDetails) {
            requireNonNull(safetyCenterErrorDetails, "safetyCenterErrorDetails cannot be null");

            final long identity = Binder.clearCallingIdentity();
            try {
                mExecutor.execute(
                        () -> {
                            if (mRemoved) {
                                return;
                            }
                            // The remove call could still complete at this point, but we're ok
                            // with this raciness on separate threads. If the listener is removed on
                            // the same thread as `mExecutor`; the above check should ensure that
                            // the listener won't be called after the remove call.
                            mOriginalListener.onError(safetyCenterErrorDetails);
                        });
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void markAsRemoved() {
            mRemoved = true;
        }
    }
}
