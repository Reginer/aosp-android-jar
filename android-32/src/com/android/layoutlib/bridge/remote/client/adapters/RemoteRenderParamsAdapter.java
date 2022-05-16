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

import com.android.ide.common.rendering.api.IImageFactory;
import com.android.ide.common.rendering.api.RenderParams;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.SessionParams;
import com.android.ide.common.rendering.api.SessionParams.Key;
import com.android.layout.remote.api.RemoteAssetRepository;
import com.android.layout.remote.api.RemoteHardwareConfig;
import com.android.layout.remote.api.RemoteLayoutLog;
import com.android.layout.remote.api.RemoteLayoutlibCallback;
import com.android.layout.remote.api.RemoteRenderParams;
import com.android.layout.remote.api.RemoteRenderResources;
import com.android.layout.remote.api.RemoteSessionParams;
import com.android.tools.layoutlib.annotations.NotNull;
import com.android.tools.layoutlib.annotations.Nullable;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteRenderParamsAdapter implements RemoteRenderParams {
    private final RenderParams mDelegate;

    protected RemoteRenderParamsAdapter(@NotNull RenderParams params) {
        mDelegate = params;
    }

    public static RemoteSessionParams create(@NotNull SessionParams params) throws RemoteException {
        return (RemoteSessionParams) UnicastRemoteObject.exportObject(
                new RemoteRenderParamsAdapter(params), 0);
    }

    @Nullable
    @Override
    public String getProjectKey() {
        Object projectKey = mDelegate.getProjectKey();
        // We can not transfer a random object so let's send just a string
        return projectKey != null ? projectKey.toString() : null;
    }

    @Override
    public RemoteHardwareConfig getRemoteHardwareConfig() {
        return new RemoteHardwareConfig(mDelegate.getHardwareConfig());
    }

    @Override
    public int getMinSdkVersion() {
        return mDelegate.getMinSdkVersion();
    }

    @Override
    public int getTargetSdkVersion() {
        return mDelegate.getTargetSdkVersion();
    }

    @Override
    public RemoteRenderResources getRemoteResources() {
        try {
            return RemoteRenderResourcesAdapter.create(mDelegate.getResources());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RemoteAssetRepository getAssets() {
        try {
            return RemoteAssetRepositoryAdapter.create(mDelegate.getAssets());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RemoteLayoutlibCallback getRemoteLayoutlibCallback() {
        try {
            return RemoteLayoutlibCallbackAdapter.create(mDelegate.getLayoutlibCallback());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RemoteLayoutLog getLog() {
        try {
            return RemoteLayoutLogAdapter.create(mDelegate.getLog());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isTransparentBackground() {
        return mDelegate.isTransparentBackground();
    }

    @Override
    public long getTimeout() {
        return mDelegate.getTimeout();
    }

    @Override
    public IImageFactory getImageFactory() {
        return mDelegate.getImageFactory();
    }

    @Override
    public ResourceValue getAppIcon() {
        return mDelegate.getAppIcon();
    }

    @Override
    public String getAppLabel() {
        return mDelegate.getAppLabel();
    }

    @Override
    public String getLocale() {
        return mDelegate.getLocale();
    }

    @Override
    public String getActivityName() {
        return mDelegate.getActivityName();
    }

    @Override
    public boolean isForceNoDecor() {
        return mDelegate.isForceNoDecor();
    }

    @Override
    public boolean isRtlSupported() {
        return mDelegate.isRtlSupported();
    }

    @Override
    public <T> T getFlag(Key<T> key) {
        return mDelegate.getFlag(key);
    }
}
