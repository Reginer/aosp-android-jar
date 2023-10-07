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
import android.car.builtin.annotation.PlatformVersion;
import android.content.pm.ActivityInfo;
import android.view.WindowLayout;

import com.android.annotation.AddedIn;

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
    @AddedIn(PlatformVersion.TIRAMISU_0)
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
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public TaskWrapper getTask() {
        return mTask;
    }

    /**
     * Gets the specified {@link WindowLayoutWrapper}.
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public WindowLayoutWrapper getWindowLayout() {
        return mLayout;
    }

    /**
     * Gets the {@link ActivityRecordWrapper} currently being positioned.
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public ActivityRecordWrapper getActivity() {
        return mActivity;
    }

    /**
     * Gets the {@link ActivityRecordWrapper} from which activity was started from.
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public ActivityRecordWrapper getSource() {
        return mSource;
    }

    /**
     * Gets the {@link ActivityOptionsWrapper} specified for the activity.
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public ActivityOptionsWrapper getOptions() {
        return mOptions;
    }

    /**
     * Gets the optional {@link RequestWrapper} from the activity starter.
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public RequestWrapper getRequest() {
        return mRequest;
    }

    /**
     * Gets the {@link LaunchParamsController.LaunchParamsModifier.Phase} that the resolution should
     * finish.
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public int getPhase() {
        return mPhase;
    }

    /**
     * Gets the current {@link LaunchParamsWrapper}.
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public LaunchParamsWrapper getCurrentParams() {
        return mCurrentParams;
    }

    /**
     * Gets the resulting {@link LaunchParamsWrapper}.
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public LaunchParamsWrapper getOutParams() {
        return mOutParams;
    }

    /**
     * Returns whether the current system supports the multiple display.
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public boolean supportsMultiDisplay() {
        return mSupportsMultiDisplay;
    }
}
