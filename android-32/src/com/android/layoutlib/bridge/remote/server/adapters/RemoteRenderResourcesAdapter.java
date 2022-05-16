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

package com.android.layoutlib.bridge.remote.server.adapters;

import com.android.ide.common.rendering.api.ILayoutLog;
import com.android.ide.common.rendering.api.RenderResources;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.StyleResourceValue;
import com.android.layout.remote.api.RemoteRenderResources;
import com.android.layout.remote.api.RemoteResourceValue;
import com.android.tools.layoutlib.annotations.NotNull;

import java.rmi.RemoteException;
import java.util.List;
import java.util.stream.Collectors;

public class RemoteRenderResourcesAdapter extends RenderResources {
    private final RemoteRenderResources mDelegate;

    public RemoteRenderResourcesAdapter(@NotNull RemoteRenderResources remoteRenderResources) {
        mDelegate = remoteRenderResources;
    }

    @Override
    public void setLogger(ILayoutLog logger) {
        // Ignored for remote operations.
    }

    @Override
    public StyleResourceValue getDefaultTheme() {
        try {
            return mDelegate.getDefaultTheme().toResourceValue();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void applyStyle(StyleResourceValue theme, boolean useAsPrimary) {
        try {
            mDelegate.applyStyle(RemoteResourceValue.fromResourceValue(theme), useAsPrimary);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clearStyles() {
        try {
            mDelegate.clearStyles();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<StyleResourceValue> getAllThemes() {
        try {
            return mDelegate.getAllThemes().stream()
                    .map(RemoteResourceValue::toResourceValue)
                    .collect(Collectors.toList());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResourceValue findItemInTheme(ResourceReference attr) {
        try {
            return mDelegate.findItemInTheme(attr).toResourceValue();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResourceValue findItemInStyle(StyleResourceValue style, ResourceReference attr) {
        try {
            return mDelegate.findItemInStyle(RemoteResourceValue.fromResourceValue(style), attr)
                    .toResourceValue();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResourceValue dereference(ResourceValue resourceValue) {
        try {
            return mDelegate.dereference(RemoteResourceValue.fromResourceValue(resourceValue))
                    .toResourceValue();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /** Returns a resource by namespace, type and name. The returned resource is unresolved. */
    @Override
    public ResourceValue getUnresolvedResource(ResourceReference reference) {
        try {
            return mDelegate.getUnresolvedResource(reference).toResourceValue();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResourceValue resolveResValue(ResourceValue value) {
        try {
            return mDelegate.resolveValue(RemoteResourceValue.fromResourceValue(value))
                    .toResourceValue();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StyleResourceValue getParent(StyleResourceValue style) {
        try {
            return mDelegate.getParent(RemoteResourceValue.fromResourceValue(style))
                    .toResourceValue();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StyleResourceValue getStyle(ResourceReference reference) {
        try {
            return mDelegate.getStyle(reference).toResourceValue();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
