/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.wm;

import android.annotation.Nullable;
import android.annotation.SystemApi;

/**
 * Wrapper of {@link com.android.server.wm.ActivityStarter.Request}.
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public final class RequestWrapper {
    private final ActivityStarter.Request mRequest;

    private RequestWrapper(ActivityStarter.Request request) {
        mRequest = request;
    }

    /** @hide */
    public static RequestWrapper create(@Nullable ActivityStarter.Request request) {
        if (request == null) return null;
        return new RequestWrapper(request);
    }

    /** @hide */
    public ActivityStarter.Request getRequest() {
        return mRequest;
    }

    @Override
    public String toString() {
        return mRequest.toString();
    }
}
