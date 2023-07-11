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

import android.annotation.SystemApi;
import android.app.ActivityOptions;
import android.window.WindowContainerToken;

/**
 * Wrapper of {@link ActivityOptions}.
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public final class ActivityOptionsWrapper {
    private final ActivityOptions mOptions;

    private ActivityOptionsWrapper(ActivityOptions options) {
        mOptions = options;
    }

    /** @hide */
    public static ActivityOptionsWrapper create(ActivityOptions options) {
        if (options == null) return null;
        return new ActivityOptionsWrapper(options);
    }

    /**
     * Gets the underlying {@link ActivityOptions} that is wrapped by this instance.
     */
    // Exposed the original object in order to allow to use the public accessors.
    public ActivityOptions getOptions() {
        return mOptions;
    }

    /**
     * Gets {@link TaskDisplayAreaWrapper} to launch the Activity into
     */
    public TaskDisplayAreaWrapper getLaunchTaskDisplayArea() {
        WindowContainerToken daToken = mOptions.getLaunchTaskDisplayArea();
        if (daToken == null) return null;
        TaskDisplayArea tda = (TaskDisplayArea) WindowContainer.fromBinder(daToken.asBinder());
        return TaskDisplayAreaWrapper.create(tda);
    }

    @Override
    public String toString() {
        return mOptions.toString();
    }
}
