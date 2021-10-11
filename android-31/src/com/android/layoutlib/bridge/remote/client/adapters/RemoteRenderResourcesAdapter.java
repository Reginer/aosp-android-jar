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

import com.android.ide.common.rendering.api.RenderResources;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.StyleResourceValue;
import com.android.layout.remote.api.RemoteRenderResources;
import com.android.layout.remote.api.RemoteResourceValue;
import com.android.tools.layoutlib.annotations.NotNull;
import com.android.tools.layoutlib.annotations.Nullable;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.stream.Collectors;

public class RemoteRenderResourcesAdapter implements RemoteRenderResources {

    private final RenderResources mDelegate;

    private RemoteRenderResourcesAdapter(@NotNull RenderResources delegate) {
        mDelegate = delegate;
    }

    public static RemoteRenderResources create(@NotNull RenderResources resources)
            throws RemoteException {
        return (RemoteRenderResources) UnicastRemoteObject.exportObject(
                new RemoteRenderResourcesAdapter(resources), 0);
    }

    @Override
    public RemoteResourceValue<StyleResourceValue> getDefaultTheme() {
        return RemoteResourceValue.fromResourceValue(mDelegate.getDefaultTheme());
    }

    @Override
    public void applyStyle(RemoteResourceValue<StyleResourceValue> theme, boolean useAsPrimary) {
        mDelegate.applyStyle(theme.toResourceValue(), useAsPrimary);
    }

    @Override
    public void clearStyles() {
        mDelegate.clearStyles();
    }

    @Override
    public List<RemoteResourceValue<StyleResourceValue>> getAllThemes() {
        return mDelegate.getAllThemes().stream().map(
                RemoteResourceValue::fromResourceValue).collect(Collectors.toList());
    }

    @Override
    @Nullable
    public RemoteResourceValue<ResourceValue> getResolvedResource(
            @NotNull ResourceReference reference) {
        return RemoteResourceValue.fromResourceValue(mDelegate.getResolvedResource(reference));
    }

    @Override
    public RemoteResourceValue<ResourceValue> findItemInTheme(ResourceReference attr) {
        return RemoteResourceValue.fromResourceValue(mDelegate.findItemInTheme(attr));
    }

    @Override
    public RemoteResourceValue<ResourceValue> findItemInStyle(
            RemoteResourceValue<StyleResourceValue> style, ResourceReference attr) {
        return RemoteResourceValue.fromResourceValue(
                mDelegate.findItemInStyle(style.toResourceValue(), attr));
    }

    @Override
    public RemoteResourceValue<ResourceValue> resolveValue(
            RemoteResourceValue<ResourceValue> value) {
        return RemoteResourceValue.fromResourceValue(
                mDelegate.resolveResValue(value.toResourceValue()));
    }

    @Override
    public RemoteResourceValue<StyleResourceValue> getParent(
            RemoteResourceValue<StyleResourceValue> style) {
        return RemoteResourceValue.fromResourceValue(mDelegate.getParent(style.toResourceValue()));
    }

    @Override
    @Nullable
    public RemoteResourceValue<StyleResourceValue> getStyle(@NotNull ResourceReference reference) {
        return RemoteResourceValue.fromResourceValue(mDelegate.getStyle(reference));
    }

    @Override
    public RemoteResourceValue<ResourceValue> dereference(
            RemoteResourceValue<ResourceValue> resourceValue) {
        return RemoteResourceValue.fromResourceValue(
                mDelegate.dereference(resourceValue.toResourceValue()));
    }

    @Override
    public RemoteResourceValue<ResourceValue> getUnresolvedResource(ResourceReference reference) {
        return RemoteResourceValue.fromResourceValue(mDelegate.getUnresolvedResource(reference));
    }
}
