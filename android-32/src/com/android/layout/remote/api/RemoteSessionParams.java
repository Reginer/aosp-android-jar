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

import com.android.ide.common.rendering.api.AdapterBinding;
import com.android.ide.common.rendering.api.IImageFactory;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.common.rendering.api.SessionParams;
import com.android.ide.common.rendering.api.SessionParams.Key;
import com.android.ide.common.rendering.api.SessionParams.RenderingMode;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * Remote version of the {@link SessionParams} class
 */
public interface RemoteSessionParams extends RemoteRenderParams {
    RenderingMode getRenderingMode() throws RemoteException;

    boolean isLayoutOnly() throws RemoteException;

    Map<ResourceReference, AdapterBinding> getAdapterBindings() throws RemoteException;

    boolean getExtendedViewInfoMode() throws RemoteException;

    int getSimulatedPlatformVersion() throws RemoteException;

    RemoteILayoutPullParser getLayoutDescription() throws RemoteException;
}
