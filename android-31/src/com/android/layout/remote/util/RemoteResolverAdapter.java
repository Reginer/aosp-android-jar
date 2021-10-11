/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.layout.remote.util;

import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.ide.common.rendering.api.ResourceNamespace.Resolver;
import com.android.layout.remote.api.RemoteNamespaceResolver;
import com.android.tools.layoutlib.annotations.NotNull;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteResolverAdapter implements RemoteNamespaceResolver {
    private final Resolver mDelegate;

    private RemoteResolverAdapter(Resolver delegate) {
        mDelegate = delegate;
    }

    public static RemoteNamespaceResolver create(@NotNull ResourceNamespace.Resolver delegate)
            throws RemoteException {
        assert !(delegate instanceof RemoteResolverAdapter);

        return (RemoteNamespaceResolver) UnicastRemoteObject.exportObject(
                new RemoteResolverAdapter(delegate), 0);
    }

    @Override
    public String remotePrefixToUri(String namespacePrefix) {
        return mDelegate.prefixToUri(namespacePrefix);
    }

    @Override
    public String remoteUriToPrefix(String namespaceUri) {
        return mDelegate.uriToPrefix(namespaceUri);
    }
}
