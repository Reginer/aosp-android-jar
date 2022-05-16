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

import com.android.ide.common.rendering.api.ILayoutPullParser;
import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.layout.remote.api.RemoteILayoutPullParser;
import com.android.tools.layoutlib.annotations.NotNull;

import java.rmi.RemoteException;

public class RemoteILayoutPullParserAdapter extends RemoteXmlPullParserAdapter
        implements ILayoutPullParser {
    public RemoteILayoutPullParserAdapter(@NotNull RemoteILayoutPullParser delegate) {
        super(delegate);
    }

    @Override
    public Object getViewCookie() {
        try {
            return ((RemoteILayoutPullParser) mDelegate).getViewCookie();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResourceNamespace getLayoutNamespace() {
        try {
            return ((RemoteILayoutPullParser) mDelegate).getLayoutNamespace();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
