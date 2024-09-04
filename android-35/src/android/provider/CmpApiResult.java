/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.provider;

import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

/**
 * Used to store the returned results from CloudProviderApis.
 */
class CmpApiResult {
    private String mApi;
    private Cursor mCursor;
    private Bundle mBundle;
    private Point mDimensions;
    private ParcelFileDescriptor mParcelFileDescriptor;
    private AssetFileDescriptor mAssetFileDescriptor;

    CmpApiResult(String api, Cursor c) {
        mApi = api;
        mCursor = c;
    }

    CmpApiResult(String api, AssetFileDescriptor afd, Point dimensions) {
        mApi = api;
        mAssetFileDescriptor = afd;
        mDimensions = dimensions;
    }

    CmpApiResult(String api, Bundle bundle) {
        mApi = api;
        mBundle = bundle;
    }

    CmpApiResult(String api, ParcelFileDescriptor pfd) {
        mApi = api;
        mParcelFileDescriptor = pfd;
    }

    String getApi() {
        return mApi;
    }

    Cursor getCursor() {
        return mCursor;
    }

    Bundle getBundle() {
        return mBundle;
    }

    Point getDimensions() {
        return mDimensions;
    }

    ParcelFileDescriptor getParcelFileDescriptor() {
        return mParcelFileDescriptor;
    }

    AssetFileDescriptor getAssetFileDescriptor() {
        return mAssetFileDescriptor;
    }
}
