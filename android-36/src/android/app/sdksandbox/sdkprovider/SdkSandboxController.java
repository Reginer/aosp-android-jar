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
package android.app.sdksandbox.sdkprovider;

import static android.app.sdksandbox.SdkSandboxManager.getResultCodeForLoadSdkException;
import static android.app.sdksandbox.sdkprovider.SdkSandboxController.SDK_SANDBOX_CONTROLLER_SERVICE;

import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemService;
import android.app.Activity;
import android.app.sdksandbox.AppOwnedSdkSandboxInterface;
import android.app.sdksandbox.ILoadSdkCallback;
import android.app.sdksandbox.LoadSdkException;
import android.app.sdksandbox.SandboxLatencyInfo;
import android.app.sdksandbox.SandboxedSdk;
import android.app.sdksandbox.SandboxedSdkContext;
import android.app.sdksandbox.SandboxedSdkProvider;
import android.app.sdksandbox.SdkSandboxLocalSingleton;
import android.app.sdksandbox.SdkSandboxManager;
import android.app.sdksandbox.StatsdUtil;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.OutcomeReceiver;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;
import com.android.sdksandbox.flags.Flags;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Controller that is used by SDK loaded in the sandbox to access information provided by the sdk
 * sandbox.
 *
 * <p>It enables the SDK to communicate with other SDKS in the SDK sandbox and know about the state
 * of the sdks that are currently loaded in it.
 *
 * <p>An instance of {@link SdkSandboxController} can be obtained using {@link
 * Context#getSystemService} and {@link SdkSandboxController class}. The {@link Context} can in turn
 * be obtained using {@link android.app.sdksandbox.SandboxedSdkProvider#getContext()}.
 */
@SystemService(SDK_SANDBOX_CONTROLLER_SERVICE)
public class SdkSandboxController {
    public static final String SDK_SANDBOX_CONTROLLER_SERVICE = "sdk_sandbox_controller_service";
    /** @hide */
    public static final String CLIENT_SHARED_PREFERENCES_NAME =
            "com.android.sdksandbox.client_sharedpreferences";

    private static final String TAG = "SdkSandboxController";

    private SdkSandboxLocalSingleton mSdkSandboxLocalSingleton;
    private SdkSandboxActivityRegistry mSdkSandboxActivityRegistry;
    private Context mContext;

    /**
     * Create SdkSandboxController.
     *
     * @hide
     */
    public SdkSandboxController(@NonNull Context context) {
        // When SdkSandboxController is initiated from inside the sdk sandbox process, its private
        // members will be immediately rewritten by the initialize method.
        initialize(context);
    }

    /**
     * Initializes {@link SdkSandboxController} with the given {@code context}.
     *
     * <p>This method is called by the {@link SandboxedSdkContext} to propagate the correct context.
     * For more information check the javadoc on the {@link
     * android.app.sdksandbox.SdkSandboxSystemServiceRegistry}.
     *
     * @hide
     * @see android.app.sdksandbox.SdkSandboxSystemServiceRegistry
     */
    public SdkSandboxController initialize(@NonNull Context context) {
        mContext = context;
        mSdkSandboxLocalSingleton = SdkSandboxLocalSingleton.getExistingInstance();
        mSdkSandboxActivityRegistry = SdkSandboxActivityRegistry.getInstance();
        return this;
    }

    /**
     * Fetches all {@link AppOwnedSdkSandboxInterface} that are registered by the app.
     *
     * @return List of {@link AppOwnedSdkSandboxInterface} containing all currently registered
     *     AppOwnedSdkSandboxInterface.
     * @throws UnsupportedOperationException if the controller is obtained from an unexpected
     *     context. Use {@link SandboxedSdkProvider#getContext()} for the right context
     */
    public @NonNull List<AppOwnedSdkSandboxInterface> getAppOwnedSdkSandboxInterfaces() {
        enforceSandboxedSdkContextInitialization();
        try {
            return mSdkSandboxLocalSingleton
                    .getSdkToServiceCallback()
                    .getAppOwnedSdkSandboxInterfaces(
                            ((SandboxedSdkContext) mContext).getClientPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Fetches information about Sdks that are loaded in the sandbox.
     *
     * @return List of {@link SandboxedSdk} containing all currently loaded sdks
     * @throws UnsupportedOperationException if the controller is obtained from an unexpected
     *     context. Use {@link SandboxedSdkProvider#getContext()} for the right context
     */
    public @NonNull List<SandboxedSdk> getSandboxedSdks() {
        enforceSandboxedSdkContextInitialization();
        SandboxLatencyInfo sandboxLatencyInfo =
                new SandboxLatencyInfo(SandboxLatencyInfo.METHOD_GET_SANDBOXED_SDKS_VIA_CONTROLLER);
        // TODO(b/319659746) : Rename the method to something more generic than using App.
        // TODO(b/321909787) : Use injector to set time in order to write unit tests.
        sandboxLatencyInfo.setTimeAppCalledSystemServer(SystemClock.elapsedRealtime());

        try {
            return mSdkSandboxLocalSingleton
                    .getSdkToServiceCallback()
                    .getSandboxedSdks(
                            ((SandboxedSdkContext) mContext).getClientPackageName(),
                            sandboxLatencyInfo);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Loads SDK in an SDK sandbox java process.
     *
     * <p>Loads SDK library with {@code sdkName} to an SDK sandbox process asynchronously. The
     * caller will be notified through the {@code receiver}.
     *
     * <p>The caller may only load {@code SDKs} the client app depends on into the SDK sandbox.
     *
     * @param sdkName name of the SDK to be loaded.
     * @param params additional parameters to be passed to the SDK in the form of a {@link Bundle}
     *     as agreed between the client and the SDK.
     * @param executor the {@link Executor} on which to invoke the receiver.
     * @param receiver This either receives a {@link SandboxedSdk} on a successful run, or {@link
     *     LoadSdkException}.
     * @throws UnsupportedOperationException if the controller is obtained from an unexpected
     *     context. Use {@link SandboxedSdkProvider#getContext()} for the right context
     */
    public void loadSdk(
            @NonNull String sdkName,
            @NonNull Bundle params,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<SandboxedSdk, LoadSdkException> receiver) {
        enforceSandboxedSdkContextInitialization();
        final LoadSdkReceiverProxy callbackProxy = new LoadSdkReceiverProxy(executor, receiver);
        SandboxLatencyInfo sandboxLatencyInfo =
                new SandboxLatencyInfo(SandboxLatencyInfo.METHOD_LOAD_SDK_VIA_CONTROLLER);
        // TODO(b/319659746) : Rename the method to something more generic than using App.
        // TODO(b/321909787) : Use injector to set time in order to write unit tests.
        sandboxLatencyInfo.setTimeAppCalledSystemServer(SystemClock.elapsedRealtime());

        try {
            mSdkSandboxLocalSingleton
                    .getSdkToServiceCallback()
                    .loadSdk(
                            ((SandboxedSdkContext) mContext).getClientPackageName(),
                            sdkName,
                            sandboxLatencyInfo,
                            params,
                            callbackProxy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns {@link SharedPreferences} containing data synced from the client app.
     *
     * <p>Keys that have been synced by the client app using {@link
     * SdkSandboxManager#addSyncedSharedPreferencesKeys(Set)} can be found in this {@link
     * SharedPreferences}.
     *
     * <p>The returned {@link SharedPreferences} should only be read. Writing to it is not
     * supported.
     *
     * @return {@link SharedPreferences} containing data synced from client app.
     * @throws UnsupportedOperationException if the controller is obtained from an unexpected
     *     context. Use {@link SandboxedSdkProvider#getContext()} for the right context
     */
    @NonNull
    public SharedPreferences getClientSharedPreferences() {
        enforceSandboxedSdkContextInitialization();

        // TODO(b/248214708): We should store synced data in a separate internal storage directory.
        return mContext.getApplicationContext()
                .getSharedPreferences(CLIENT_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Returns an identifier for a {@link SdkSandboxActivityHandler} after registering it.
     *
     * <p>This function registers an implementation of {@link SdkSandboxActivityHandler} created by
     * an SDK and returns an {@link IBinder} which uniquely identifies the passed {@link
     * SdkSandboxActivityHandler} object.
     *
     * <p>If the same {@link SdkSandboxActivityHandler} registered multiple times without
     * unregistering, the same {@link IBinder} token will be returned.
     *
     * @param sdkSandboxActivityHandler is the {@link SdkSandboxActivityHandler} to register.
     * @return {@link IBinder} uniquely identify the passed {@link SdkSandboxActivityHandler}.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @NonNull
    public IBinder registerSdkSandboxActivityHandler(
            @NonNull SdkSandboxActivityHandler sdkSandboxActivityHandler) {
        if (!SdkLevel.isAtLeastU()) {
            throw new UnsupportedOperationException();
        }
        // TODO(b/321909787) : Use injector to set time for unit tests.
        long timeEventStarted = SystemClock.elapsedRealtime();
        enforceSandboxedSdkContextInitialization();

        IBinder token =
                mSdkSandboxActivityRegistry.register(
                        (SandboxedSdkContext) mContext, sdkSandboxActivityHandler);
        logSandboxActivityApiLatency(
                StatsdUtil
                        .SANDBOX_ACTIVITY_EVENT_OCCURRED__METHOD__REGISTER_SDK_SANDBOX_ACTIVITY_HANDLER,
                StatsdUtil.SANDBOX_ACTIVITY_EVENT_OCCURRED__CALL_RESULT__SUCCESS,
                timeEventStarted);
        return token;
    }

    /**
     * Unregister an already registered {@link SdkSandboxActivityHandler}.
     *
     * <p>If the passed {@link SdkSandboxActivityHandler} is registered, it will be unregistered.
     * Otherwise, it will do nothing.
     *
     * <p>After unregistering, SDK can register the same handler object again or create a new one in
     * case it wants a new {@link Activity}.
     *
     * <p>If the {@link IBinder} token of the unregistered handler used to start a {@link Activity},
     * the {@link Activity} will fail to start.
     *
     * @param sdkSandboxActivityHandler is the {@link SdkSandboxActivityHandler} to unregister.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @NonNull
    public void unregisterSdkSandboxActivityHandler(
            @NonNull SdkSandboxActivityHandler sdkSandboxActivityHandler) {
        if (!SdkLevel.isAtLeastU()) {
            throw new UnsupportedOperationException();
        }
        long timeEventStarted = SystemClock.elapsedRealtime();
        enforceSandboxedSdkContextInitialization();

        mSdkSandboxActivityRegistry.unregister(sdkSandboxActivityHandler);
        logSandboxActivityApiLatency(
                StatsdUtil
                        .SANDBOX_ACTIVITY_EVENT_OCCURRED__METHOD__UNREGISTER_SDK_SANDBOX_ACTIVITY_HANDLER,
                StatsdUtil.SANDBOX_ACTIVITY_EVENT_OCCURRED__CALL_RESULT__SUCCESS,
                timeEventStarted);
    }

    /**
     * Returns the package name of the client app.
     *
     * @throws UnsupportedOperationException if the controller is obtained from an unexpected
     *     context. Use {@link SandboxedSdkProvider#getContext()} for the right context.
     */
    @NonNull
    public String getClientPackageName() {
        enforceSandboxedSdkContextInitialization();
        return ((SandboxedSdkContext) mContext).getClientPackageName();
    }

    /**
     * Registers a listener to be notified of changes in the client's {@link
     * android.app.ActivityManager.RunningAppProcessInfo#importance}.
     *
     * @param listener an implementation of {@link SdkSandboxClientImportanceListener} to register.
     * @throws UnsupportedOperationException if the controller is obtained from an unexpected
     *     context. Use {@link SandboxedSdkProvider#getContext()} for the right context.
     */
    @FlaggedApi(Flags.FLAG_SANDBOX_CLIENT_IMPORTANCE_LISTENER)
    public void registerSdkSandboxClientImportanceListener(
            @NonNull @CallbackExecutor Executor executor,
            @NonNull SdkSandboxClientImportanceListener listener) {
        Objects.requireNonNull(executor, "executor should not be null");
        Objects.requireNonNull(listener, "listener should not be null");
        enforceSandboxedSdkContextInitialization();
        SdkSandboxLocalSingleton.getExistingInstance()
                .registerSdkSandboxClientImportanceListener(
                        new SdkSandboxClientImportanceListenerProxy(executor, listener));
    }

    /**
     * Unregisters a listener previously registered using {@link
     * SdkSandboxController#registerSdkSandboxClientImportanceListener(Executor,
     * SdkSandboxClientImportanceListener)}
     *
     * @param listener an implementation of {@link SdkSandboxClientImportanceListener} to
     *     unregister.
     * @throws UnsupportedOperationException if the controller is obtained from an unexpected
     *     context. Use {@link SandboxedSdkProvider#getContext()} for the right context.
     */
    @FlaggedApi(Flags.FLAG_SANDBOX_CLIENT_IMPORTANCE_LISTENER)
    public void unregisterSdkSandboxClientImportanceListener(
            @NonNull SdkSandboxClientImportanceListener listener) {
        Objects.requireNonNull(listener, "listener should not be null");
        enforceSandboxedSdkContextInitialization();
        SdkSandboxLocalSingleton.getExistingInstance()
                .unregisterSdkSandboxClientImportanceListener(listener);
    }

    /** @hide */
    public static class SdkSandboxClientImportanceListenerProxy {
        private final Executor mExecutor;
        public final SdkSandboxClientImportanceListener listener;

        SdkSandboxClientImportanceListenerProxy(
                Executor executor, SdkSandboxClientImportanceListener listener) {
            mExecutor = executor;
            this.listener = listener;
        }

        /** @hide */
        public void onForegroundImportanceChanged(boolean isForeground) {
            mExecutor.execute(() -> listener.onForegroundImportanceChanged(isForeground));
        }
    }

    private void enforceSandboxedSdkContextInitialization() {
        if (!(mContext instanceof SandboxedSdkContext)) {
            throw new UnsupportedOperationException(
                    "Only available from the context obtained by calling android.app.sdksandbox"
                            + ".SandboxedSdkProvider#getContext()");
        }
    }

    private void logLatenciesFromSandbox(SandboxLatencyInfo sandboxLatencyInfo) {
        // TODO(b/319659746) : Rename the method to something more generic than using App.
        sandboxLatencyInfo.setTimeAppReceivedCallFromSystemServer(SystemClock.elapsedRealtime());
        try {
            mSdkSandboxLocalSingleton
                    .getSdkToServiceCallback()
                    .logLatenciesFromSandbox(sandboxLatencyInfo);
        } catch (RemoteException e) {
            Log.e(
                    TAG,
                    "Logging metrics for method "
                            + sandboxLatencyInfo.getMethod()
                            + " failed with exception "
                            + e.getMessage());
        }
    }

    private void logSandboxActivityApiLatency(int method, int callResult, long timeEventStarted) {
        try {
            mSdkSandboxLocalSingleton
                    .getSdkToServiceCallback()
                    .logSandboxActivityApiLatencyFromSandbox(
                            method,
                            callResult,
                            (int) (SystemClock.elapsedRealtime() - timeEventStarted));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private class LoadSdkReceiverProxy extends ILoadSdkCallback.Stub {
        private final Executor mExecutor;
        private final OutcomeReceiver<SandboxedSdk, LoadSdkException> mCallback;

        LoadSdkReceiverProxy(
                Executor executor, OutcomeReceiver<SandboxedSdk, LoadSdkException> callback) {
            mExecutor = executor;
            mCallback = callback;
        }

        @Override
        public void onLoadSdkSuccess(
                SandboxedSdk sandboxedSdk, SandboxLatencyInfo sandboxLatencyInfo) {
            SdkSandboxController.this.logLatenciesFromSandbox(sandboxLatencyInfo);
            mExecutor.execute(() -> mCallback.onResult(sandboxedSdk));
        }

        @Override
        public void onLoadSdkFailure(
                LoadSdkException exception, SandboxLatencyInfo sandboxLatencyInfo) {
            sandboxLatencyInfo.setResultCode(getResultCodeForLoadSdkException(exception));
            SdkSandboxController.this.logLatenciesFromSandbox(sandboxLatencyInfo);
            mExecutor.execute(() -> mCallback.onError(exception));
        }
    }
}
