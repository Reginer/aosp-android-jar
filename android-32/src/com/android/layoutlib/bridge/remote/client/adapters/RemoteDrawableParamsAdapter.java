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

import com.android.ide.common.rendering.api.DrawableParams;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.layout.remote.api.RemoteDrawableParams;
import com.android.tools.layoutlib.annotations.NotNull;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteDrawableParamsAdapter extends RemoteRenderParamsAdapter implements
        RemoteDrawableParams {
    private final DrawableParams mDelegate;

    private RemoteDrawableParamsAdapter(@NotNull DrawableParams drawableParams) {
        super(drawableParams);
        mDelegate = drawableParams;
    }

    @NotNull
    public static RemoteDrawableParams create(@NotNull DrawableParams drawableParams)
            throws RemoteException {
        return (RemoteDrawableParams) UnicastRemoteObject.exportObject(
                new RemoteDrawableParamsAdapter(drawableParams), 0);
    }

    @NotNull
    @Override
    public ResourceValue getDrawable() throws RemoteException {
        return mDelegate.getDrawable();
    }
}
