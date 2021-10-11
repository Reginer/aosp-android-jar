/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License") throws RemoteException;
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

import com.android.ide.common.rendering.api.RenderResources;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.StyleResourceValue;
import com.android.tools.layoutlib.annotations.NotNull;
import com.android.tools.layoutlib.annotations.Nullable;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote version of the {@link RenderResources} class
 */
public interface RemoteRenderResources extends Remote {
    RemoteResourceValue<StyleResourceValue> getDefaultTheme() throws RemoteException;

    void applyStyle(RemoteResourceValue<StyleResourceValue> theme, boolean useAsPrimary) throws RemoteException;

    void clearStyles() throws RemoteException;

    List<RemoteResourceValue<StyleResourceValue>> getAllThemes() throws RemoteException;

    @Nullable
    RemoteResourceValue<ResourceValue> getResolvedResource(@NotNull ResourceReference reference) throws RemoteException;

    RemoteResourceValue<ResourceValue> findItemInTheme(ResourceReference attr) throws RemoteException;

    RemoteResourceValue<ResourceValue> findItemInStyle(RemoteResourceValue<StyleResourceValue> style,
            ResourceReference attr)
            throws RemoteException;

    RemoteResourceValue<ResourceValue> resolveValue(RemoteResourceValue<ResourceValue> value) throws RemoteException;

    RemoteResourceValue<StyleResourceValue> getParent(RemoteResourceValue<StyleResourceValue> style) throws RemoteException;

    @Nullable
    RemoteResourceValue<StyleResourceValue> getStyle(@NotNull ResourceReference reference) throws RemoteException;

    RemoteResourceValue<ResourceValue> dereference(RemoteResourceValue<ResourceValue> resourceValue) throws RemoteException;

    RemoteResourceValue<ResourceValue> getUnresolvedResource(ResourceReference reference) throws RemoteException;
}
