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

import com.android.ide.common.rendering.api.ActionBarCallback;
import com.android.ide.common.rendering.api.ActionBarCallback.HomeButtonStyle;
import com.android.ide.common.rendering.api.ResourceReference;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import android.annotation.NonNull;

/**
 * Remote version of the {@link ActionBarCallback} class.
 */
public interface RemoteActionBarCallback extends Remote {
    @NonNull
    List<ResourceReference> getMenuIds() throws RemoteException;

    boolean getSplitActionBarWhenNarrow() throws RemoteException;

    int getNavigationMode() throws RemoteException;

    String getSubTitle() throws RemoteException;

    HomeButtonStyle getHomeButtonStyle() throws RemoteException;

    boolean isOverflowPopupNeeded() throws RemoteException;
}
