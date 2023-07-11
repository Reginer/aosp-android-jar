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
import android.content.pm.ActivityInfo;
import android.view.WindowLayout;

/**
 * Wrapper of the parameters of {@code LaunchParamsController.LaunchParamsModifier.onCalculate()}
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public final class CalculateParams {
    private TaskWrapper mTask;
    private WindowLayoutWrapper mLayout;
    private ActivityRecordWrapper mActivity;
    private ActivityRecordWrapper mSource;
    private ActivityOptionsWrapper mOptions;
    private RequestWrapper mRequest;
    private int mPhase;
    private LaunchParamsWrapper mCurrentParams;
    private LaunchParamsWrapper mOutParams;
    private boolean mSupportsMultiDisplay;

    private CalculateParams() {}

    /** @hide */
    public static CalculateParams create(Task task, ActivityInfo.WindowLayout layout,
            ActivityRecord actvity, ActivityRecord source,
            ActivityOptions options, ActivityStarter.Request request, int phase,
            LaunchParamsController.LaunchParams currentParams,
            LaunchParamsController.LaunchParams outParms,
            boolean supportsMultiDisplay) {
        CalculateParams params = new CalculateParams();
        params.mTask = TaskWrapper.create(task);
        params.mLayout = WindowLayoutWrapper.create(layout);
        params.mActivity = ActivityRecordWrapper.create(actvity);
        params.mSource = ActivityRecordWrapper.create(source);
        params.mOptions = ActivityOptionsWrapper.create(options);
        params.mRequest = RequestWrapper.create(request);
        params.mPhase = phase;
        params.mCurrentParams = LaunchParamsWrapper.create(currentParams);
        params.mOutParams = LaunchParamsWrapper.create(outParms);
        params.mSupportsMultiDisplay = supportsMultiDisplay;
        return params;
    }

    /**
     * Gets the {@link TaskWrapper} currently being positioned.
     */
    public TaskWrapper getTask() {
        return mTask;
    }

    /**
     * Gets the specified {@link WindowLayoutWrapper}.
     */
    public WindowLayoutWrapper getWindowLayout() {
        return mLayout;
    }

    /**
     * Gets the {@link ActivityRecordWrapper} currently being positioned.
     */
    public ActivityRecordWrapper getActivity() {
        return mActivity;
    }

    /**
     * Gets the {@link ActivityRecordWrapper} from which activity was started from.
     */
    public ActivityRecordWrapper getSource() {
        return mSource;
    }

    /**
     * Gets the {@link ActivityOptionsWrapper} specified for the activity.
     */
    public ActivityOptionsWrapper getOptions() {
        return mOptions;
    }

    /**
     * Gets the optional {@link RequestWrapper} from the activity starter.
     */
    public RequestWrapper getRequest() {
        return mRequest;
    }

    /**
     * Gets the {@link LaunchParamsController.LaunchParamsModifier.Phase} that the resolution should
     * finish.
     */
    public int getPhase() {
        return mPhase;
    }

    /**
     * Gets the current {@link LaunchParamsWrapper}.
     */
    public LaunchParamsWrapper getCurrentParams() {
        return mCurrentParams;
    }

    /**
     * Gets the resulting {@link LaunchParamsWrapper}.
     */
    public LaunchParamsWrapper getOutParams() {
        return mOutParams;
    }

    /**
     * Returns whether the current system supports the multiple display.
     */
    public boolean supportsMultiDisplay() {
        return mSupportsMultiDisplay;
    }
}
