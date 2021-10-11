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

import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.Result;
import com.android.layout.remote.util.SerializableImage;
import com.android.tools.layoutlib.annotations.NotNull;

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * Remote version of the {@link RenderSession} class
 */
public interface RemoteRenderSession extends Remote {
    @NotNull
    Result getResult() throws RemoteException;

    @NotNull
    SerializableImage getSerializableImage() throws RemoteException;

    void setSystemTimeNanos(long nanos) throws RemoteException;

    void setSystemBootTimeNanos(long nanos) throws RemoteException;

    void setElapsedFrameTimeNanos(long nanos) throws RemoteException;

    void dispose() throws RemoteException;

    @NotNull
    Result render(long timeout, boolean forceMeasure) throws RemoteException;
}
