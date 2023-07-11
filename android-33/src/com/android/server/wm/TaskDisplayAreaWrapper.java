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
import android.view.Display;

/**
 * Wrapper of {@link TaskDisplayArea}.
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public final class TaskDisplayAreaWrapper {
    private final TaskDisplayArea mTaskDisplayArea;

    private TaskDisplayAreaWrapper(TaskDisplayArea taskDisplayArea) {
        mTaskDisplayArea = taskDisplayArea;
    }

    /** @hide */
    public static TaskDisplayAreaWrapper create(@Nullable TaskDisplayArea taskDisplayArea) {
        if (taskDisplayArea == null) return null;
        return new TaskDisplayAreaWrapper(taskDisplayArea);
    }

    /** @hide */
    public TaskDisplayArea getTaskDisplayArea() {
        return mTaskDisplayArea;
    }

    /**
     * Gets the display this {@link TaskDisplayAreaWrapper} is on.
     */
    public Display getDisplay() {
        return mTaskDisplayArea.getDisplayContent().getDisplay();
    }

    @Override
    public String toString() {
        return mTaskDisplayArea.toString();
    }
}
