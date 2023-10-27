/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.app.sdksandbox;

import static android.app.sdksandbox.SdkSandboxManager.SDK_SANDBOX_SERVICE;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.annotation.TestApi;
import android.app.Activity;
import android.app.sdksandbox.sdkprovider.SdkSandboxActivityHandler;
import android.app.sdksandbox.sdkprovider.SdkSandboxController;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.OutcomeReceiver;
import android.os.RemoteException;
import android.util.Log;
import android.view.SurfaceControlViewHost.SurfacePackage;

import androidx.annotation.RequiresApi;

import com.android.internal.annotations.GuardedBy;
import com.android.modules.utils.build.SdkLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Provides APIs to load {@link android.content.pm.SharedLibraryInfo#TYPE_SDK_PACKAGE SDKs} into the
 * SDK sandbox process, and then interact with them.
 *
 * <p>SDK sandbox is a java process running in a separate uid range. Each app may have its own SDK
 * sandbox process.
 *
 * <p>The app first needs to declare SDKs it depends on in its manifest using the {@code
 * <uses-sdk-library>} tag. Apps may only load SDKs they depend on into the SDK sandbox.
 *
 * @see android.content.pm.SharedLibraryInfo#TYPE_SDK_PACKAGE
 * @see <a href="https://developer.android.com/design-for-safety/ads/sdk-runtime">SDK Runtime design
 *     proposal</a>
 */
@SystemService(SDK_SANDBOX_SERVICE)
public final class SdkSandboxManager {

    /**
     * Use with {@link Context#getSystemService(String)} to retrieve an {@link SdkSandboxManager}
     * for interacting with the SDKs belonging to this client application.
     */
    public static final String SDK_SANDBOX_SERVICE = "sdk_sandbox";

    /**
     * SDK sandbox process is not available.
     *
     * <p>This indicates that the SDK sandbox process is not available, either because it has died,
     * disconnected or was not created in the first place.
     */
    public static final int SDK_SANDBOX_PROCESS_NOT_AVAILABLE = 503;

    /**
     * SDK not found.
     *
     * <p>This indicates that client application tried to load a non-existing SDK by calling {@link
     * SdkSandboxManager#loadSdk(String, Bundle, Executor, OutcomeReceiver)}.
     */
    public static final int LOAD_SDK_NOT_FOUND = 100;

    /**
     * SDK is already loaded.
     *
     * <p>This indicates that client application tried to reload the same SDK by calling {@link
     * SdkSandboxManager#loadSdk(String, Bundle, Executor, OutcomeReceiver)} after being
     * successfully loaded.
     */
    public static final int LOAD_SDK_ALREADY_LOADED = 101;

    /**
     * SDK error after being loaded.
     *
     * <p>This indicates that the SDK encountered an error during post-load initialization. The
     * details of this can be obtained from the Bundle returned in {@link LoadSdkException} through
     * the {@link OutcomeReceiver} passed in to {@link SdkSandboxManager#loadSdk}.
     */
    public static final int LOAD_SDK_SDK_DEFINED_ERROR = 102;

    /**
     * SDK sandbox is disabled.
     *
     * <p>This indicates that the SDK sandbox is disabled. Any subsequent attempts to load SDKs in
     * this boot will also fail.
     */
    public static final int LOAD_SDK_SDK_SANDBOX_DISABLED = 103;

    /**
     * Internal error while loading SDK.
     *
     * <p>This indicates a generic internal error happened while applying the call from client
     * application.
     */
    public static final int LOAD_SDK_INTERNAL_ERROR = 500;

    /**
     * Action name for the intent which starts {@link Activity} in SDK sandbox.
     *
     * <p>System services would know if the intent is created to start {@link Activity} in sandbox
     * by comparing the action of the intent to the value of this field.
     *
     * <p>This intent should contain an extra param with key equals to {@link
     * #EXTRA_SANDBOXED_ACTIVITY_HANDLER} and value equals to the {@link IBinder} that identifies
     * the {@link SdkSandboxActivityHandler} that registered before by an SDK. If the extra param is
     * missing, the {@link Activity} will fail to start.
     *
     * @hide
     */
    @SdkConstant(SdkConstant.SdkConstantType.ACTIVITY_INTENT_ACTION)
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String ACTION_START_SANDBOXED_ACTIVITY =
            "android.app.sdksandbox.action.START_SANDBOXED_ACTIVITY";

    /**
     * The key for an element in {@link Activity} intent extra params, the value is an {@link
     * SdkSandboxActivityHandler} registered by an SDK.
     *
     * @hide
     */
    public static final String EXTRA_SANDBOXED_ACTIVITY_HANDLER =
            "android.app.sdksandbox.extra.SANDBOXED_ACTIVITY_HANDLER";

    private static final String TAG = "SdkSandboxManager";

    /** @hide */
    @IntDef(
            value = {
                LOAD_SDK_NOT_FOUND,
                LOAD_SDK_ALREADY_LOADED,
                LOAD_SDK_SDK_DEFINED_ERROR,
                LOAD_SDK_SDK_SANDBOX_DISABLED,
                LOAD_SDK_INTERNAL_ERROR,
                SDK_SANDBOX_PROCESS_NOT_AVAILABLE
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LoadSdkErrorCode {}

    /** Internal error while requesting a {@link SurfacePackage}.
     *
     * <p>This indicates a generic internal error happened while requesting a
     * {@link SurfacePackage}.
     */
    public static final int REQUEST_SURFACE_PACKAGE_INTERNAL_ERROR = 700;

    /**
     * SDK is not loaded while requesting a {@link SurfacePackage}.
     *
     * <p>This indicates that the SDK for which the {@link SurfacePackage} is being requested is not
     * loaded, either because the sandbox died or because it was not loaded in the first place.
     */
    public static final int REQUEST_SURFACE_PACKAGE_SDK_NOT_LOADED = 701;

    /** @hide */
    @IntDef(
            prefix = "REQUEST_SURFACE_PACKAGE_",
            value = {
                REQUEST_SURFACE_PACKAGE_INTERNAL_ERROR,
                REQUEST_SURFACE_PACKAGE_SDK_NOT_LOADED
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RequestSurfacePackageErrorCode {}

    /**
     * SDK sandbox is disabled.
     *
     * <p>{@link SdkSandboxManager} APIs are hidden. Attempts at calling them will result in {@link
     * UnsupportedOperationException}.
     */
    public static final int SDK_SANDBOX_STATE_DISABLED = 0;

    /**
     * SDK sandbox is enabled.
     *
     * <p>App can use {@link SdkSandboxManager} APIs to load {@code SDKs} it depends on into the
     * corresponding SDK sandbox process.
     */
    public static final int SDK_SANDBOX_STATE_ENABLED_PROCESS_ISOLATION = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = "SDK_SANDBOX_STATUS_", value = {
            SDK_SANDBOX_STATE_DISABLED,
            SDK_SANDBOX_STATE_ENABLED_PROCESS_ISOLATION,
    })
    public @interface SdkSandboxState {}

    /**
     * The name of key to be used in the Bundle fields of {@link #requestSurfacePackage(String,
     * Bundle, Executor, OutcomeReceiver)}, its value should define the integer width of the {@link
     * SurfacePackage} in pixels.
     */
    public static final String EXTRA_WIDTH_IN_PIXELS =
            "android.app.sdksandbox.extra.WIDTH_IN_PIXELS";
    /**
     * The name of key to be used in the Bundle fields of {@link #requestSurfacePackage(String,
     * Bundle, Executor, OutcomeReceiver)}, its value should define the integer height of the {@link
     * SurfacePackage} in pixels.
     */
    public static final String EXTRA_HEIGHT_IN_PIXELS =
            "android.app.sdksandbox.extra.HEIGHT_IN_PIXELS";
    /**
     * The name of key to be used in the Bundle fields of {@link #requestSurfacePackage(String,
     * Bundle, Executor, OutcomeReceiver)}, its value should define the integer ID of the logical
     * display to display the {@link SurfacePackage}.
     */
    public static final String EXTRA_DISPLAY_ID = "android.app.sdksandbox.extra.DISPLAY_ID";

    /**
     * The name of key to be used in the Bundle fields of {@link #requestSurfacePackage(String,
     * Bundle, Executor, OutcomeReceiver)}, its value should present the token returned by {@link
     * android.view.SurfaceView#getHostToken()} once the {@link android.view.SurfaceView} has been
     * added to the view hierarchy. Only a non-null value is accepted to enable ANR reporting.
     */
    public static final String EXTRA_HOST_TOKEN = "android.app.sdksandbox.extra.HOST_TOKEN";

    /**
     * The name of key in the Bundle which is passed to the {@code onResult} function of the {@link
     * OutcomeReceiver} which is field of {@link #requestSurfacePackage(String, Bundle, Executor,
     * OutcomeReceiver)}, its value presents the requested {@link SurfacePackage}.
     */
    public static final String EXTRA_SURFACE_PACKAGE =
            "android.app.sdksandbox.extra.SURFACE_PACKAGE";

    private final ISdkSandboxManager mService;
    private final Context mContext;

    @GuardedBy("mLifecycleCallbacks")
    private final ArrayList<SdkSandboxProcessDeathCallbackProxy> mLifecycleCallbacks =
            new ArrayList<>();

    private final SharedPreferencesSyncManager mSyncManager;

    /** @hide */
    public SdkSandboxManager(@NonNull Context context, @NonNull ISdkSandboxManager binder) {
        mContext = Objects.requireNonNull(context, "context should not be null");
        mService = Objects.requireNonNull(binder, "binder should not be null");
        // TODO(b/239403323): There can be multiple package in the same app process
        mSyncManager = SharedPreferencesSyncManager.getInstance(context, binder);
    }

    /** Returns the current state of the availability of the SDK sandbox feature. */
    @SdkSandboxState
    public static int getSdkSandboxState() {
        return SDK_SANDBOX_STATE_ENABLED_PROCESS_ISOLATION;
    }

    /**
     * Stops the SDK sandbox process corresponding to the app.
     *
     * @hide
     */
    @TestApi
    @RequiresPermission("com.android.app.sdksandbox.permission.STOP_SDK_SANDBOX")
    public void stopSdkSandbox() {
        try {
            mService.stopSdkSandbox(mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Adds a callback which gets registered for SDK sandbox lifecycle events, such as SDK sandbox
     * death. If the sandbox has not yet been created when this is called, the request will be
     * stored until a sandbox is created, at which point it is activated for that sandbox. Multiple
     * callbacks can be added to detect death and will not be removed when the sandbox dies.
     *
     * @param callbackExecutor the {@link Executor} on which to invoke the callback
     * @param callback the {@link SdkSandboxProcessDeathCallback} which will receive SDK sandbox
     *     lifecycle events.
     */
    public void addSdkSandboxProcessDeathCallback(
            @NonNull @CallbackExecutor Executor callbackExecutor,
            @NonNull SdkSandboxProcessDeathCallback callback) {
        Objects.requireNonNull(callbackExecutor, "callbackExecutor should not be null");
        Objects.requireNonNull(callback, "callback should not be null");

        synchronized (mLifecycleCallbacks) {
            final SdkSandboxProcessDeathCallbackProxy callbackProxy =
                    new SdkSandboxProcessDeathCallbackProxy(callbackExecutor, callback);
            try {
                mService.addSdkSandboxProcessDeathCallback(
                        mContext.getPackageName(),
                        /*timeAppCalledSystemServer=*/ System.currentTimeMillis(),
                        callbackProxy);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
            mLifecycleCallbacks.add(callbackProxy);
        }
    }

    /**
     * Removes an {@link SdkSandboxProcessDeathCallback} that was previously added using {@link
     * SdkSandboxManager#addSdkSandboxProcessDeathCallback(Executor,
     * SdkSandboxProcessDeathCallback)}
     *
     * @param callback the {@link SdkSandboxProcessDeathCallback} which was previously added using
     *     {@link SdkSandboxManager#addSdkSandboxProcessDeathCallback(Executor,
     *     SdkSandboxProcessDeathCallback)}
     */
    public void removeSdkSandboxProcessDeathCallback(
            @NonNull SdkSandboxProcessDeathCallback callback) {
        Objects.requireNonNull(callback, "callback should not be null");
        synchronized (mLifecycleCallbacks) {
            for (int i = mLifecycleCallbacks.size() - 1; i >= 0; i--) {
                final SdkSandboxProcessDeathCallbackProxy callbackProxy =
                        mLifecycleCallbacks.get(i);
                if (callbackProxy.callback == callback) {
                    try {
                        mService.removeSdkSandboxProcessDeathCallback(
                                mContext.getPackageName(),
                                /*timeAppCalledSystemServer=*/ System.currentTimeMillis(),
                                callbackProxy);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                    mLifecycleCallbacks.remove(i);
                }
            }
        }
    }

    /**
     * Loads SDK in an SDK sandbox java process.
     *
     * <p>Loads SDK library with {@code sdkName} to an SDK sandbox process asynchronously. The
     * caller will be notified through the {@code receiver}.
     *
     * <p>The caller should already declare {@code SDKs} it depends on in its manifest using {@code
     * <uses-sdk-library>} tag. The caller may only load {@code SDKs} it depends on into the SDK
     * sandbox.
     *
     * <p>When the client application loads the first SDK, a new SDK sandbox process will be
     * created. If a sandbox has already been created for the client application, additional SDKs
     * will be loaded into the same sandbox.
     *
     * <p>This API may only be called while the caller is running in the foreground. Calls from the
     * background will result in returning {@link LoadSdkException} in the {@code receiver}.
     *
     * @param sdkName name of the SDK to be loaded.
     * @param params additional parameters to be passed to the SDK in the form of a {@link Bundle}
     *     as agreed between the client and the SDK.
     * @param executor the {@link Executor} on which to invoke the receiver.
     * @param receiver This either receives a {@link SandboxedSdk} on a successful run, or {@link
     *     LoadSdkException}.
     */
    public void loadSdk(
            @NonNull String sdkName,
            @NonNull Bundle params,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<SandboxedSdk, LoadSdkException> receiver) {
        Objects.requireNonNull(sdkName, "sdkName should not be null");
        Objects.requireNonNull(params, "params should not be null");
        Objects.requireNonNull(executor, "executor should not be null");
        Objects.requireNonNull(receiver, "receiver should not be null");
        final LoadSdkReceiverProxy callbackProxy =
                new LoadSdkReceiverProxy(executor, receiver, mService);

        IBinder appProcessToken;
        // Context.getProcessToken() only exists on U+.
        if (SdkLevel.isAtLeastU()) {
            appProcessToken = mContext.getProcessToken();
        } else {
            appProcessToken = null;
        }
        try {
            mService.loadSdk(
                    mContext.getPackageName(),
                    appProcessToken,
                    sdkName,
                    /*timeAppCalledSystemServer=*/ System.currentTimeMillis(),
                    params,
                    callbackProxy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Fetches information about SDKs that are loaded in the sandbox.
     *
     * @return List of {@link SandboxedSdk} containing all currently loaded SDKs.
     */
    public @NonNull List<SandboxedSdk> getSandboxedSdks() {
        try {
            return mService.getSandboxedSdks(
                    mContext.getPackageName(),
                    /*timeAppCalledSystemServer=*/ System.currentTimeMillis());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Unloads an SDK that has been previously loaded by the caller.
     *
     * <p>It is not guaranteed that the memory allocated for this SDK will be freed immediately. All
     * subsequent calls to {@link #requestSurfacePackage(String, Bundle, Executor, OutcomeReceiver)}
     * for the given {@code sdkName} will fail.
     *
     * <p>This API may only be called while the caller is running in the foreground. Calls from the
     * background will result in a {@link SecurityException} being thrown.
     *
     * @param sdkName name of the SDK to be unloaded.
     */
    public void unloadSdk(@NonNull String sdkName) {
        Objects.requireNonNull(sdkName, "sdkName should not be null");
        try {
            mService.unloadSdk(
                    mContext.getPackageName(),
                    sdkName,
                    /*timeAppCalledSystemServer=*/ System.currentTimeMillis());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Sends a request for a surface package to the SDK.
     *
     * <p>After the client application receives a signal about a successful SDK loading, and has
     * added a {@link android.view.SurfaceView} to the view hierarchy, it may asynchronously request
     * a {@link SurfacePackage} to render a view from the SDK.
     *
     * <p>When the {@link SurfacePackage} is ready, the {@link OutcomeReceiver#onResult} callback of
     * the passed {@code receiver} will be invoked. This callback will contain a {@link Bundle}
     * object, which will contain the key {@link SdkSandboxManager#EXTRA_SURFACE_PACKAGE} whose
     * associated value is the requested {@link SurfacePackage}.
     *
     * <p>The passed {@code params} must contain the following keys: {@link
     * SdkSandboxManager#EXTRA_WIDTH_IN_PIXELS}, {@link SdkSandboxManager#EXTRA_HEIGHT_IN_PIXELS},
     * {@link SdkSandboxManager#EXTRA_DISPLAY_ID} and {@link SdkSandboxManager#EXTRA_HOST_TOKEN}. If
     * any of these keys are missing or invalid, an {@link IllegalArgumentException} will be thrown.
     *
     * <p>This API may only be called while the caller is running in the foreground. Calls from the
     * background will result in returning RequestSurfacePackageException in the {@code receiver}.
     *
     * @param sdkName name of the SDK loaded into the SDK sandbox.
     * @param params the parameters which the client application passes to the SDK.
     * @param callbackExecutor the {@link Executor} on which to invoke the callback
     * @param receiver This either returns a {@link Bundle} on success which will contain the key
     *     {@link SdkSandboxManager#EXTRA_SURFACE_PACKAGE} with a {@link SurfacePackage} value, or
     *     {@link RequestSurfacePackageException} on failure.
     * @throws IllegalArgumentException if {@code params} does not contain all required keys.
     * @see android.app.sdksandbox.SdkSandboxManager#EXTRA_WIDTH_IN_PIXELS
     * @see android.app.sdksandbox.SdkSandboxManager#EXTRA_HEIGHT_IN_PIXELS
     * @see android.app.sdksandbox.SdkSandboxManager#EXTRA_DISPLAY_ID
     * @see android.app.sdksandbox.SdkSandboxManager#EXTRA_HOST_TOKEN
     */
    public void requestSurfacePackage(
            @NonNull String sdkName,
            @NonNull Bundle params,
            @NonNull @CallbackExecutor Executor callbackExecutor,
            @NonNull OutcomeReceiver<Bundle, RequestSurfacePackageException> receiver) {
        Objects.requireNonNull(sdkName, "sdkName should not be null");
        Objects.requireNonNull(params, "params should not be null");
        Objects.requireNonNull(callbackExecutor, "callbackExecutor should not be null");
        Objects.requireNonNull(receiver, "receiver should not be null");
        try {
            int width = params.getInt(EXTRA_WIDTH_IN_PIXELS, -1); // -1 means invalid width
            if (width <= 0) {
                throw new IllegalArgumentException(
                        "Field params should have the entry for the key ("
                                + EXTRA_WIDTH_IN_PIXELS
                                + ") with positive integer value");
            }

            int height = params.getInt(EXTRA_HEIGHT_IN_PIXELS, -1); // -1 means invalid height
            if (height <= 0) {
                throw new IllegalArgumentException(
                        "Field params should have the entry for the key ("
                                + EXTRA_HEIGHT_IN_PIXELS
                                + ") with positive integer value");
            }

            int displayId = params.getInt(EXTRA_DISPLAY_ID, -1); // -1 means invalid displayId
            if (displayId < 0) {
                throw new IllegalArgumentException(
                        "Field params should have the entry for the key ("
                                + EXTRA_DISPLAY_ID
                                + ") with integer >= 0");
            }

            IBinder hostToken = params.getBinder(EXTRA_HOST_TOKEN);
            if (hostToken == null) {
                throw new IllegalArgumentException(
                        "Field params should have the entry for the key ("
                                + EXTRA_HOST_TOKEN
                                + ") with not null IBinder value");
            }

            final RequestSurfacePackageReceiverProxy callbackProxy =
                    new RequestSurfacePackageReceiverProxy(callbackExecutor, receiver, mService);

            mService.requestSurfacePackage(
                    mContext.getPackageName(),
                    sdkName,
                    hostToken,
                    displayId,
                    width,
                    height,
                    /*timeAppCalledSystemServer=*/ System.currentTimeMillis(),
                    params,
                    callbackProxy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Starts an {@link Activity} in the SDK sandbox.
     *
     * <p>This function will start a new {@link Activity} in the same task of the passed {@code
     * fromActivity} and pass it to the SDK that shared the passed {@code sdkActivityToken} that
     * identifies a request from that SDK to stat this {@link Activity}.
     *
     * <p>The {@link Activity} will not start in the following cases:
     *
     * <ul>
     *   <li>The App calling this API is in the background.
     *   <li>The passed {@code sdkActivityToken} does not map to a request for an {@link Activity}
     *       form the SDK that shared it with the caller app.
     *   <li>The SDK that shared the passed {@code sdkActivityToken} removed its request for this
     *       {@link Activity}.
     *   <li>The sandbox {@link Activity} is already created.
     * </ul>
     *
     * @param fromActivity the {@link Activity} will be used to start the new sandbox {@link
     *     Activity} by calling {@link Activity#startActivity(Intent)} against it.
     * @param sdkActivityToken the identifier that is shared by the SDK which requests the {@link
     *     Activity}.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void startSdkSandboxActivity(
            @NonNull Activity fromActivity, @NonNull IBinder sdkActivityToken) {
        if (!SdkLevel.isAtLeastU()) {
            throw new UnsupportedOperationException();
        }
        Intent intent = new Intent();
        intent.setAction(ACTION_START_SANDBOXED_ACTIVITY);
        intent.setPackage(mContext.getPackageManager().getSdkSandboxPackageName());

        Bundle params = new Bundle();
        params.putBinder(EXTRA_SANDBOXED_ACTIVITY_HANDLER, sdkActivityToken);
        intent.putExtras(params);

        fromActivity.startActivity(intent);
    }

    /**
     * A callback for tracking events SDK sandbox death.
     *
     * <p>The callback can be added using {@link
     * SdkSandboxManager#addSdkSandboxProcessDeathCallback(Executor,
     * SdkSandboxProcessDeathCallback)} and removed using {@link
     * SdkSandboxManager#removeSdkSandboxProcessDeathCallback(SdkSandboxProcessDeathCallback)}
     */
    public interface SdkSandboxProcessDeathCallback {
        /**
         * Notifies the client application that the SDK sandbox has died. The sandbox could die for
         * various reasons, for example, due to memory pressure on the system, or a crash in the
         * sandbox.
         *
         * The system will automatically restart the sandbox process if it died due to a crash.
         * However, the state of the sandbox will be lost - so any SDKs that were loaded previously
         * would have to be loaded again, using {@link SdkSandboxManager#loadSdk(String, Bundle,
         * Executor, OutcomeReceiver)} to continue using them.
         */
        void onSdkSandboxDied();
    }

    /** @hide */
    private static class SdkSandboxProcessDeathCallbackProxy
            extends ISdkSandboxProcessDeathCallback.Stub {
        private final Executor mExecutor;
        public final SdkSandboxProcessDeathCallback callback;

        SdkSandboxProcessDeathCallbackProxy(
                Executor executor, SdkSandboxProcessDeathCallback lifecycleCallback) {
            mExecutor = executor;
            callback = lifecycleCallback;
        }

        @Override
        public void onSdkSandboxDied() {
            mExecutor.execute(() -> callback.onSdkSandboxDied());
        }
    }

    /**
     * Adds keys to set of keys being synced from app's default {@link SharedPreferences} to the SDK
     * sandbox.
     *
     * <p>Synced data will be available for SDKs to read using the {@link
     * SdkSandboxController#getClientSharedPreferences()} API.
     *
     * <p>To stop syncing any key that has been added using this API, use {@link
     * #removeSyncedSharedPreferencesKeys(Set)}.
     *
     * <p>The sync breaks if the app restarts and user must call this API again to rebuild the pool
     * of keys for syncing.
     *
     * <p>Note: This class does not support use across multiple processes.
     *
     * @param keys set of keys that will be synced to Sandbox.
     */
    public void addSyncedSharedPreferencesKeys(@NonNull Set<String> keys) {
        Objects.requireNonNull(keys, "keys cannot be null");
        for (String key : keys) {
            if (key == null) {
                throw new IllegalArgumentException("keys cannot contain null");
            }
        }
        mSyncManager.addSharedPreferencesSyncKeys(keys);
    }

    /**
     * Removes keys from set of keys that have been added using {@link
     * #addSyncedSharedPreferencesKeys(Set)}
     *
     * <p>Removed keys will be erased from the SDK sandbox if they have been synced already.
     *
     * @param keys set of key names that should no longer be synced to Sandbox.
     */
    public void removeSyncedSharedPreferencesKeys(@NonNull Set<String> keys) {
        for (String key : keys) {
            if (key == null) {
                throw new IllegalArgumentException("keys cannot contain null");
            }
        }
        mSyncManager.removeSharedPreferencesSyncKeys(keys);
    }

    /**
     * Returns the set keys that are being synced from app's default {@link SharedPreferences} to
     * the SDK sandbox.
     */
    @NonNull
    public Set<String> getSyncedSharedPreferencesKeys() {
        return mSyncManager.getSharedPreferencesSyncKeys();
    }

    /** @hide */
    private static class LoadSdkReceiverProxy extends ILoadSdkCallback.Stub {
        private final Executor mExecutor;
        private final OutcomeReceiver<SandboxedSdk, LoadSdkException> mCallback;
        private final ISdkSandboxManager mService;

        LoadSdkReceiverProxy(
                Executor executor,
                OutcomeReceiver<SandboxedSdk, LoadSdkException> callback,
                ISdkSandboxManager service) {
            mExecutor = executor;
            mCallback = callback;
            mService = service;
        }

        @Override
        public void onLoadSdkSuccess(SandboxedSdk sandboxedSdk, long timeSystemServerCalledApp) {
            logLatencyFromSystemServerToApp(timeSystemServerCalledApp);
            mExecutor.execute(() -> mCallback.onResult(sandboxedSdk));
        }

        @Override
        public void onLoadSdkFailure(LoadSdkException exception, long timeSystemServerCalledApp) {
            logLatencyFromSystemServerToApp(timeSystemServerCalledApp);
            mExecutor.execute(() -> mCallback.onError(exception));
        }

        private void logLatencyFromSystemServerToApp(long timeSystemServerCalledApp) {
            try {
                mService.logLatencyFromSystemServerToApp(
                        ISdkSandboxManager.LOAD_SDK,
                        // TODO(b/242832156): Add Injector class for testing
                        (int) (System.currentTimeMillis() - timeSystemServerCalledApp));
            } catch (RemoteException e) {
                Log.w(
                        TAG,
                        "Remote exception while calling logLatencyFromSystemServerToApp."
                                + "Error: "
                                + e.getMessage());
            }
        }
    }

    /** @hide */
    private static class RequestSurfacePackageReceiverProxy
            extends IRequestSurfacePackageCallback.Stub {
        private final Executor mExecutor;
        private final OutcomeReceiver<Bundle, RequestSurfacePackageException> mReceiver;
        private final ISdkSandboxManager mService;

        RequestSurfacePackageReceiverProxy(
                Executor executor,
                OutcomeReceiver<Bundle, RequestSurfacePackageException> receiver,
                ISdkSandboxManager service) {
            mExecutor = executor;
            mReceiver = receiver;
            mService = service;
        }

        @Override
        public void onSurfacePackageReady(
                SurfacePackage surfacePackage,
                int surfacePackageId,
                Bundle params,
                long timeSystemServerCalledApp) {
            logLatencyFromSystemServerToApp(timeSystemServerCalledApp);
            mExecutor.execute(
                    () -> {
                        params.putParcelable(EXTRA_SURFACE_PACKAGE, surfacePackage);
                        mReceiver.onResult(params);
                    });
        }

        @Override
        public void onSurfacePackageError(
                int errorCode, String errorMsg, long timeSystemServerCalledApp) {
            logLatencyFromSystemServerToApp(timeSystemServerCalledApp);
            mExecutor.execute(
                    () ->
                            mReceiver.onError(
                                    new RequestSurfacePackageException(errorCode, errorMsg)));
        }

        private void logLatencyFromSystemServerToApp(long timeSystemServerCalledApp) {
            try {
                mService.logLatencyFromSystemServerToApp(
                        ISdkSandboxManager.REQUEST_SURFACE_PACKAGE,
                        // TODO(b/242832156): Add Injector class for testing
                        (int) (System.currentTimeMillis() - timeSystemServerCalledApp));
            } catch (RemoteException e) {
                Log.w(
                        TAG,
                        "Remote exception while calling logLatencyFromSystemServerToApp."
                                + "Error: "
                                + e.getMessage());
            }
        }
    }

    /**
     * Return the AdServicesManager
     *
     * @hide
     */
    public IBinder getAdServicesManager() {
        try {
            return mService.getAdServicesManager();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
