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

package com.android.layoutlib.bridge.remote.client.adapters;

import com.android.ide.common.rendering.api.AdapterBinding;
import com.android.ide.common.rendering.api.IImageFactory;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.common.rendering.api.SessionParams;
import com.android.ide.common.rendering.api.SessionParams.Key;
import com.android.ide.common.rendering.api.SessionParams.RenderingMode;
import com.android.layout.remote.api.RemoteAssetRepository;
import com.android.layout.remote.api.RemoteHardwareConfig;
import com.android.layout.remote.api.RemoteILayoutPullParser;
import com.android.layout.remote.api.RemoteLayoutLog;
import com.android.layout.remote.api.RemoteLayoutlibCallback;
import com.android.layout.remote.api.RemoteRenderResources;
import com.android.layout.remote.api.RemoteSessionParams;
import com.android.tools.layoutlib.annotations.NotNull;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

public class RemoteSessionParamsAdapter extends RemoteRenderParamsAdapter implements RemoteSessionParams {
    private final SessionParams mDelegate;

    private RemoteSessionParamsAdapter(@NotNull SessionParams params) {
        super(params);
        mDelegate = params;
    }

    public static RemoteSessionParams create(@NotNull SessionParams params) throws RemoteException {
        return (RemoteSessionParams) UnicastRemoteObject.exportObject(
                new RemoteSessionParamsAdapter(params), 0);
    }

    @Override
    public RenderingMode getRenderingMode() {
        return mDelegate.getRenderingMode();
    }

    @Override
    public boolean isLayoutOnly() {
        return mDelegate.isLayoutOnly();
    }

    @Override
    public Map<ResourceReference, AdapterBinding> getAdapterBindings() {
        return mDelegate.getAdapterBindings();
    }

    @Override
    public boolean getExtendedViewInfoMode() {
        return mDelegate.getExtendedViewInfoMode();
    }

    @Override
    public int getSimulatedPlatformVersion() {
        return mDelegate.getSimulatedPlatformVersion();
    }

    @Override
    public RemoteILayoutPullParser getLayoutDescription() throws RemoteException {
        return RemoteILayoutPullParserAdapter.create(mDelegate.getLayoutDescription());
    }
}
