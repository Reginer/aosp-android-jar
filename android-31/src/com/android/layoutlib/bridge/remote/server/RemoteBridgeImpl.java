/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.layoutlib.bridge.remote.server;

import com.android.ide.common.rendering.api.DrawableParams;
import com.android.ide.common.rendering.api.RenderParams;
import com.android.ide.common.rendering.api.Result;
import com.android.ide.common.rendering.api.SessionParams;
import com.android.layout.remote.api.RemoteBridge;
import com.android.layout.remote.api.RemoteDrawableParams;
import com.android.layout.remote.api.RemoteLayoutLog;
import com.android.layout.remote.api.RemoteRenderParams;
import com.android.layout.remote.api.RemoteRenderSession;
import com.android.layout.remote.api.RemoteSessionParams;
import com.android.layoutlib.bridge.Bridge;
import com.android.layoutlib.bridge.remote.server.adapters.RemoteAssetRepositoryAdapter;
import com.android.layoutlib.bridge.remote.server.adapters.RemoteILayoutPullParserAdapter;
import com.android.layoutlib.bridge.remote.server.adapters.RemoteLayoutLogAdapter;
import com.android.layoutlib.bridge.remote.server.adapters.RemoteLayoutlibCallbackAdapter;
import com.android.layoutlib.bridge.remote.server.adapters.RemoteRenderResourcesAdapter;
import com.android.layoutlib.bridge.remote.server.adapters.RemoteRenderSessionAdapter;
import com.android.tools.layoutlib.annotations.NotNull;

import java.io.File;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Remote {@link Bridge} implementation. This class is the remote entry point for the server. Its
 * responsibility is to receive the remote calls and apply the required transformations to convert
 * it into a regular call to the {@link Bridge} class.
 */
public class RemoteBridgeImpl implements RemoteBridge {
    private Bridge mBridge = new Bridge();

    /**
     * The project keys are used as key for some caches. They are usually expected to remain in
     * memory so WeakReferences are used in the caches.
     * Because in the remote bridge we do not have a real pointer to the object, we keep the strings
     * in memory until they are cleared.
     */
    private Map<String, String> mCachedProjectKeys = new HashMap<>();

    @Override
    public boolean init(Map<String, String> platformProperties, File fontLocation,
            String nativeLibPath, String icuDataPath,
            Map<String, Map<String, Integer>> enumValueMap, RemoteLayoutLog log) {
        return mBridge.init(platformProperties, fontLocation, nativeLibPath, icuDataPath,
                enumValueMap, log != null ? new RemoteLayoutLogAdapter(log) : null);
    }

    @Override
    public boolean dispose() {
        return mBridge.dispose();
    }

    private static void setupRenderParams(@NotNull RenderParams params,
            @NotNull RemoteRenderParams remoteParams) throws RemoteException {
        params.setAssetRepository(new RemoteAssetRepositoryAdapter(remoteParams.getAssets()));
        params.setActivityName(remoteParams.getActivityName());
        params.setAppIcon(remoteParams.getAppIcon());
        params.setAppLabel(remoteParams.getAppLabel());
        params.setTimeout(remoteParams.getTimeout());
        params.setLocale(remoteParams.getLocale());
        if (remoteParams.isForceNoDecor()) {
            params.setForceNoDecor();
        }
        params.setRtlSupport(remoteParams.isRtlSupported());
        if (remoteParams.isTransparentBackground()) {
            params.setTransparentBackground();
        }
        params.setImageFactory(remoteParams.getImageFactory());
        // TODO: Also unpack remote flags and pass them to RenderParams
    }

    @NotNull
    @Override
    public RemoteRenderSession createSession(@NotNull RemoteSessionParams remoteParams) {
        try {
            String projectKey = mCachedProjectKeys.putIfAbsent(remoteParams.getProjectKey(),
                    remoteParams.getProjectKey());

            // Unpack the remote params and convert it into the local SessionParams.
            SessionParams params = new SessionParams(
                    new RemoteILayoutPullParserAdapter(remoteParams.getLayoutDescription()),
                    remoteParams.getRenderingMode(), projectKey,
                    remoteParams.getRemoteHardwareConfig().getHardwareConfig(),
                    new RemoteRenderResourcesAdapter(remoteParams.getRemoteResources()),
                    new RemoteLayoutlibCallbackAdapter(remoteParams.getRemoteLayoutlibCallback()),
                    remoteParams.getMinSdkVersion(), remoteParams.getTargetSdkVersion(),
                    new RemoteLayoutLogAdapter(remoteParams.getLog()));
            setupRenderParams(params, remoteParams);
            return RemoteRenderSessionAdapter.create(mBridge.createSession(params));

        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public Result renderDrawable(@NotNull RemoteDrawableParams remoteParams) {
        try {
            String projectKey = mCachedProjectKeys.putIfAbsent(remoteParams.getProjectKey(),
                    remoteParams.getProjectKey());

            DrawableParams params = new DrawableParams(
                    remoteParams.getDrawable(),
                    projectKey,
                    remoteParams.getRemoteHardwareConfig().getHardwareConfig(),
                    new RemoteRenderResourcesAdapter(remoteParams.getRemoteResources()),
                    new RemoteLayoutlibCallbackAdapter(remoteParams.getRemoteLayoutlibCallback()),
                    remoteParams.getMinSdkVersion(), remoteParams.getTargetSdkVersion(),
                    new RemoteLayoutLogAdapter(remoteParams.getLog()));
            setupRenderParams(params, remoteParams);
            return mBridge.renderDrawable(params);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clearResourceCaches(String projectKey) {
        mCachedProjectKeys.remove(projectKey);
        mBridge.clearResourceCaches(projectKey);
    }

    @Override
    public boolean isRtl(String locale) {
        return mBridge.isRtl(locale);
    }
}
