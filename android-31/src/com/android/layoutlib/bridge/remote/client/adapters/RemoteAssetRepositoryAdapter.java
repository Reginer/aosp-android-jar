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

import com.android.ide.common.rendering.api.AssetRepository;
import com.android.layout.remote.api.RemoteAssetRepository;
import com.android.layout.remote.util.RemoteInputStream;
import com.android.layout.remote.util.RemoteInputStreamAdapter;
import com.android.tools.layoutlib.annotations.NotNull;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteAssetRepositoryAdapter implements RemoteAssetRepository {
    private final AssetRepository mDelegate;

    private RemoteAssetRepositoryAdapter(@NotNull AssetRepository delegate) {
        mDelegate = delegate;
    }

    static RemoteAssetRepository create(@NotNull AssetRepository delegate) throws RemoteException {
        return (RemoteAssetRepository) UnicastRemoteObject.exportObject(
                new RemoteAssetRepositoryAdapter(delegate), 0);
    }

    @Override
    public RemoteInputStream openAsset(String path, int mode) throws IOException, RemoteException {
        return RemoteInputStreamAdapter.create(mDelegate.openAsset(path, mode));
    }

    @Override
    public RemoteInputStream openNonAsset(int cookie, String path, int mode)
            throws IOException, RemoteException {
        return RemoteInputStreamAdapter.create(mDelegate.openNonAsset(cookie, path, mode));
    }

}
