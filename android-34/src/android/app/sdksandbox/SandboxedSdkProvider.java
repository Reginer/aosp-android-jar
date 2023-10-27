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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.sdksandbox.sdkprovider.SdkSandboxController;
import android.content.Context;
import android.os.Bundle;
import android.view.SurfaceControlViewHost.SurfacePackage;
import android.view.View;

import java.util.Objects;

/**
 * Encapsulates API which SDK sandbox can use to interact with SDKs loaded into it.
 *
 * <p>SDK has to implement this abstract class to generate an entry point for SDK sandbox to be able
 * to call it through.
 */
public abstract class SandboxedSdkProvider {
    private Context mContext;
    private SdkSandboxController mSdkSandboxController;

    /**
     * Sets the SDK {@link Context} which can then be received using {@link
     * SandboxedSdkProvider#getContext()}. This is called before {@link
     * SandboxedSdkProvider#onLoadSdk} is invoked. No operations requiring a {@link Context} should
     * be performed before then, as {@link SandboxedSdkProvider#getContext} will return null until
     * this method has been called.
     *
     * <p>Throws IllegalStateException if a base context has already been set.
     *
     * @param context The new base context.
     */
    public final void attachContext(@NonNull Context context) {
        if (mContext != null) {
            throw new IllegalStateException("Context already set");
        }
        Objects.requireNonNull(context, "Context cannot be null");
        mContext = context;
    }

    /**
     * Return the {@link Context} previously set through {@link SandboxedSdkProvider#attachContext}.
     * This will return null if no context has been previously set.
     */
    @Nullable
    public final Context getContext() {
        return mContext;
    }

    /**
     * Does the work needed for the SDK to start handling requests.
     *
     * <p>This function is called by the SDK sandbox after it loads the SDK.
     *
     * <p>SDK should do any work to be ready to handle upcoming requests. It should not do any
     * long-running tasks here, like I/O and network calls. Doing so can prevent the SDK from
     * receiving requests from the client. Additionally, it should not do initialization that
     * depends on other SDKs being loaded into the SDK sandbox.
     *
     * <p>The SDK should not do any operations requiring a {@link Context} object before this method
     * has been called.
     *
     * @param params list of params passed from the client when it loads the SDK. This can be empty.
     * @return Returns a {@link SandboxedSdk}, passed back to the client. The IBinder used to create
     *     the {@link SandboxedSdk} object will be used by the client to call into the SDK.
     */
    public abstract @NonNull SandboxedSdk onLoadSdk(@NonNull Bundle params) throws LoadSdkException;
    /**
     * Does the work needed for the SDK to free its resources before being unloaded.
     *
     * <p>This function is called by the SDK sandbox manager before it unloads the SDK. The SDK
     * should fail any invocations on the Binder previously returned to the client through {@link
     * SandboxedSdk#getInterface}.
     *
     * <p>The SDK should not do any long-running tasks here, like I/O and network calls.
     */
    public void beforeUnloadSdk() {}

    /**
     * Requests a view to be remotely rendered to the client app process.
     *
     * <p>Returns {@link View} will be wrapped into {@link SurfacePackage}. the resulting {@link
     * SurfacePackage} will be sent back to the client application.
     *
     * <p>The SDK should not do any long-running tasks here, like I/O and network calls. Doing so
     * can prevent the SDK from receiving requests from the client.
     *
     * @param windowContext the {@link Context} of the display which meant to show the view
     * @param params list of params passed from the client application requesting the view
     * @param width The view returned will be laid as if in a window of this width, in pixels.
     * @param height The view returned will be laid as if in a window of this height, in pixels.
     * @return a {@link View} which SDK sandbox pass to the client application requesting the view
     */
    @NonNull
    public abstract View getView(
            @NonNull Context windowContext, @NonNull Bundle params, int width, int height);
}
