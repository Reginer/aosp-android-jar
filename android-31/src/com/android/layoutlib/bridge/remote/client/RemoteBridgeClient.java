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

package com.android.layoutlib.bridge.remote.client;

import com.android.ide.common.rendering.api.Bridge;
import com.android.ide.common.rendering.api.DrawableParams;
import com.android.ide.common.rendering.api.ILayoutLog;
import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.Result;
import com.android.ide.common.rendering.api.SessionParams;
import com.android.layout.remote.api.RemoteBridge;
import com.android.layout.remote.api.RemoteSessionParams;
import com.android.layoutlib.bridge.remote.client.adapters.RemoteDrawableParamsAdapter;
import com.android.layoutlib.bridge.remote.client.adapters.RemoteLayoutLogAdapter;
import com.android.layoutlib.bridge.remote.client.adapters.RemoteRenderSessionAdapter;
import com.android.layoutlib.bridge.remote.client.adapters.RemoteSessionParamsAdapter;
import com.android.tools.layoutlib.annotations.NotNull;

import java.io.File;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;

public class RemoteBridgeClient extends Bridge {
    private final RemoteBridge mDelegate;

    private RemoteBridgeClient(@NotNull RemoteBridge delegate) {
        mDelegate = delegate;
    }

    @NotNull
    public static RemoteBridgeClient getRemoteBridge(int registryPort) throws RemoteException,
            NotBoundException {
        Registry registry = LocateRegistry.getRegistry(registryPort);
        RemoteBridge remoteBridge = (RemoteBridge) registry.lookup(RemoteBridge.class.getName());

        return new RemoteBridgeClient(remoteBridge);
    }

    @Override
    public boolean init(Map<String, String> platformProperties,
            File fontLocation,
            String nativeLibPath,
            String icuDataPath,
            Map<String, Map<String, Integer>> enumValueMap,
            ILayoutLog log) {
        try {
            return mDelegate.init(platformProperties, fontLocation, nativeLibPath, icuDataPath,
                    enumValueMap, RemoteLayoutLogAdapter.create(log));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean dispose() {
        try {
            return mDelegate.dispose();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RenderSession createSession(SessionParams params) {
        try {
            RemoteSessionParams remoteParams = RemoteSessionParamsAdapter.create(params);

            return new RemoteRenderSessionAdapter(mDelegate.createSession(remoteParams));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Result renderDrawable(DrawableParams params) {
        try {
            return mDelegate.renderDrawable(RemoteDrawableParamsAdapter.create(params));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clearResourceCaches(Object projectKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result getViewParent(Object viewObject) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result getViewIndex(Object viewObject) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRtl(String locale) {
        try {
            return mDelegate.isRtl(locale);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
