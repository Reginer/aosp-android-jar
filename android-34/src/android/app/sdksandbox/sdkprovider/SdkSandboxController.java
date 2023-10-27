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

import static android.app.sdksandbox.sdkprovider.SdkSandboxController.SDK_SANDBOX_CONTROLLER_SERVICE;

import android.annotation.NonNull;
import android.annotation.SystemService;
import android.app.Activity;
import android.app.sdksandbox.SandboxedSdk;
import android.app.sdksandbox.SandboxedSdkContext;
import android.app.sdksandbox.SandboxedSdkProvider;
import android.app.sdksandbox.SdkSandboxLocalSingleton;
import android.app.sdksandbox.SdkSandboxManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;

import java.util.List;

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
     * Fetches information about Sdks that are loaded in the sandbox.
     *
     * @return List of {@link SandboxedSdk} containing all currently loaded sdks
     * @throws UnsupportedOperationException if the controller is obtained from an unexpected
     *     context. Use {@link SandboxedSdkProvider#getContext()} for the right context
     */
    public @NonNull List<SandboxedSdk> getSandboxedSdks() {
        enforceSandboxedSdkContextInitialization();
        try {
            return mSdkSandboxLocalSingleton
                    .getSdkToServiceCallback()
                    .getSandboxedSdks(((SandboxedSdkContext) mContext).getClientPackageName());
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
        enforceSandboxedSdkContextInitialization();

        return mSdkSandboxActivityRegistry.register(getSdkName(), sdkSandboxActivityHandler);
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
        enforceSandboxedSdkContextInitialization();

        mSdkSandboxActivityRegistry.unregister(sdkSandboxActivityHandler);
    }

    private void enforceSandboxedSdkContextInitialization() {
        if (!(mContext instanceof SandboxedSdkContext)) {
            throw new UnsupportedOperationException(
                    "Only available from the context obtained by calling android.app.sdksandbox"
                            + ".SandboxedSdkProvider#getContext()");
        }
    }

    @NonNull
    private String getSdkName() {
        return ((SandboxedSdkContext) mContext).getSdkName();
    }
}
