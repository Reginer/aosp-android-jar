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

import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.Result;
import com.android.ide.common.rendering.api.ViewInfo;
import com.android.layout.remote.api.RemoteRenderSession;
import com.android.tools.layoutlib.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;

public class RemoteRenderSessionAdapter extends RenderSession {
    private final RemoteRenderSession mDelegate;

    public RemoteRenderSessionAdapter(@NotNull RemoteRenderSession delegate) {
        mDelegate = delegate;
    }

    @Override
    public Result getResult() {
        try {
            return mDelegate.getResult();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BufferedImage getImage() {
        try {
            return mDelegate.getSerializableImage().getBufferedImage();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setSystemTimeNanos(long nanos) {
        try {
            mDelegate.setSystemTimeNanos(nanos);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setSystemBootTimeNanos(long nanos) {
        try {
            mDelegate.setSystemBootTimeNanos(nanos);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setElapsedFrameTimeNanos(long nanos) {
        try {
            mDelegate.setElapsedFrameTimeNanos(nanos);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Result render(long timeout, boolean forceMeasure) {
        try {
            return mDelegate.render(timeout, forceMeasure);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ViewInfo> getRootViews() {
        // TODO
        return Collections.emptyList();
    }

    @Override
    public List<ViewInfo> getSystemRootViews() {
        // TODO
        return Collections.emptyList();
    }

    @Override
    public void dispose() {
        try {
            mDelegate.dispose();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
