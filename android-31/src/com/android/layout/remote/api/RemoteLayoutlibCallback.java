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
import com.android.ide.common.rendering.api.LayoutlibCallback;
import com.android.ide.common.rendering.api.LayoutlibCallback.ViewAttribute;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.SessionParams.Key;

import java.nio.file.Path;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote version of the {@link LayoutlibCallback} class
 */
public interface RemoteLayoutlibCallback extends Remote {
    Object loadView(String name, Class[] constructorSignature, Object[] constructorArgs)
            throws Exception;

    ResourceReference resolveResourceId(int id) throws RemoteException;

    int getOrGenerateResourceId(ResourceReference resource) throws RemoteException;

    RemoteILayoutPullParser getParser(ResourceValue layoutResource) throws RemoteException;

    Object getAdapterItemValue(ResourceReference adapterView, Object adapterCookie,
            ResourceReference itemRef, int fullPosition, int positionPerType,
            int fullParentPosition, int parentPositionPerType, ResourceReference viewRef,
            ViewAttribute viewAttribute, Object defaultValue) throws RemoteException;

    AdapterBinding getAdapterBinding(ResourceReference adapterViewRef, Object adapterCookie,
            Object viewObject) throws RemoteException;

    RemoteActionBarCallback getActionBarCallback() throws RemoteException;

    <T> T getFlag(Key<T> key) throws RemoteException;

    Path findClassPath(String name) throws RemoteException;

    RemoteXmlPullParser createXmlParserForPsiFile(String fileName) throws RemoteException;

    RemoteXmlPullParser createXmlParserForFile(String fileName) throws RemoteException;

    RemoteXmlPullParser createXmlParser() throws RemoteException;
}
