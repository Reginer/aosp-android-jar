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

package com.android.layout.remote.api;

import com.android.ide.common.rendering.api.IImageFactory;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.SessionParams.Key;
import com.android.tools.layoutlib.annotations.Nullable;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteRenderParams extends Remote {
    @Nullable
    String getProjectKey() throws RemoteException;

    RemoteHardwareConfig getRemoteHardwareConfig() throws RemoteException;

    int getMinSdkVersion() throws RemoteException;

    int getTargetSdkVersion() throws RemoteException;

    RemoteRenderResources getRemoteResources() throws RemoteException;

    RemoteAssetRepository getAssets() throws RemoteException;

    RemoteLayoutlibCallback getRemoteLayoutlibCallback() throws RemoteException;

    RemoteLayoutLog getLog() throws RemoteException;

    boolean isTransparentBackground() throws RemoteException;

    long getTimeout() throws RemoteException;

    IImageFactory getImageFactory() throws RemoteException;

    ResourceValue getAppIcon() throws RemoteException;

    String getAppLabel() throws RemoteException;

    String getLocale() throws RemoteException;

    String getActivityName() throws RemoteException;

    boolean isForceNoDecor() throws RemoteException;

    boolean isRtlSupported() throws RemoteException;

    <T> T getFlag(Key<T> key) throws RemoteException;
}
