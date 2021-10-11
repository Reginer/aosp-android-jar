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

import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.Result;
import com.android.layout.remote.api.RemoteRenderSession;
import com.android.layout.remote.util.SerializableImage;
import com.android.layout.remote.util.SerializableImageImpl;
import com.android.tools.layoutlib.annotations.NotNull;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteRenderSessionAdapter implements RemoteRenderSession {
    private final RenderSession mDelegate;

    private RemoteRenderSessionAdapter(@NotNull RenderSession delegate) {
        mDelegate = delegate;
    }

    public static RemoteRenderSession create(@NotNull RenderSession delegate)
            throws RemoteException {
        return (RemoteRenderSession) UnicastRemoteObject.exportObject(
                new RemoteRenderSessionAdapter(delegate), 0);
    }

    @NotNull
    @Override
    public Result getResult() throws RemoteException {
        return mDelegate.getResult();
    }

    @Override
    public Result render(long timeout, boolean forceMeasure) {
        return mDelegate.render(timeout, forceMeasure);
    }

    @NotNull
    @Override
    public SerializableImage getSerializableImage() throws RemoteException {
        return new SerializableImageImpl(mDelegate.getImage());
    }

    @Override
    public void setSystemTimeNanos(long nanos) {
        mDelegate.setSystemTimeNanos(nanos);
    }

    @Override
    public void setSystemBootTimeNanos(long nanos) {
        mDelegate.setSystemBootTimeNanos(nanos);
    }

    @Override
    public void setElapsedFrameTimeNanos(long nanos) {
        mDelegate.setElapsedFrameTimeNanos(nanos);
    }

    @Override
    public void dispose() {
        mDelegate.dispose();
    }
}
