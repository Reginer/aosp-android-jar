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
import android.content.pm.ActivityInfo;

/**
 * Wrapper of {@link android.content.pm.ActivityInfo.WindowLayout}.
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public final class WindowLayoutWrapper {
    private final ActivityInfo.WindowLayout mLayout;

    private WindowLayoutWrapper(ActivityInfo.WindowLayout layout) {
        mLayout = layout;
    }

    /** @hide */
    public static WindowLayoutWrapper create(@Nullable ActivityInfo.WindowLayout layout) {
        if (layout == null) return null;
        return new WindowLayoutWrapper(layout);
    }

    @Override
    public String toString() {
        return mLayout.toString();
    }
}
