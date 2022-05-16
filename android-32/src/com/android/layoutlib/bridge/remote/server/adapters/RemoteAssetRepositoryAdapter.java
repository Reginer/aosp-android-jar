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

import com.android.ide.common.rendering.api.AssetRepository;
import com.android.layout.remote.api.RemoteAssetRepository;
import com.android.layout.remote.util.StreamUtil;
import com.android.tools.layoutlib.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;

public class RemoteAssetRepositoryAdapter extends AssetRepository {
    private final RemoteAssetRepository mDelgate;

    public RemoteAssetRepositoryAdapter(@NotNull RemoteAssetRepository remote) {
        mDelgate = remote;
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public InputStream openAsset(String path, int mode) throws IOException, RemoteException {
        return StreamUtil.getInputStream(mDelgate.openAsset(path, mode));
    }

    @Override
    public InputStream openNonAsset(int cookie, String path, int mode)
            throws IOException, RemoteException {
        return StreamUtil.getInputStream(mDelgate.openNonAsset(cookie, path, mode));
    }
}
