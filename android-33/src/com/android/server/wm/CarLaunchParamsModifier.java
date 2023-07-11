/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.server.wm.ActivityStarter.Request;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to control the assignment of a display for Car while launching a Activity.
 *
 * <p>This one controls which displays users are allowed to launch.
 * The policy should be passed from car service through
 * {@link com.android.internal.car.ICarServiceHelper} binder interfaces. If no policy is set,
 * this module will not change anything for launch process.</p>
 *
 * <p> The policy can only affect which display passenger users can use. Current user, assumed
 * to be a driver user, is allowed to launch any display always.</p>
 *
 * @hide
 */
public final class CarLaunchParamsModifier implements LaunchParamsController.LaunchParamsModifier {

    private final Context mContext;

    private DisplayManager mDisplayManager;  // set only from init()
    private ActivityTaskManagerService mAtm;  // set only from init()

    private CarLaunchParamsModifierUpdatable mUpdatable;

    // getFallbackDisplayAreasForActivity() can return the most 3 {@link TaskDisplayAreaWrapper}.
    private final ArrayList<TaskDisplayAreaWrapper> mFallBackDisplayAreaList = new ArrayList<>(3);

    /** Constructor. Can be constructed any time. */
    public CarLaunchParamsModifier(Context context) {
        // This can be very early stage. So postpone interaction with other system until init.
        mContext = context;
    }

    public void setUpdatable(CarLaunchParamsModifierUpdatable updatable) {
        mUpdatable = updatable;
    }

    public CarLaunchParamsModifierInterface getBuiltinInterface() {
        return mBuiltinInterface;
    }

    /**
     * Initializes all internal stuffs. This should be called only after ATMS, DisplayManagerService
     * are ready.
     */
    public void init() {
        mAtm = (ActivityTaskManagerService) ActivityTaskManager.getService();
        LaunchParamsController controller = mAtm.mTaskSupervisor.getLaunchParamsController();
        controller.registerModifier(this);
        mDisplayManager = mContext.getSystemService(DisplayManager.class);
        mDisplayManager.registerDisplayListener(mUpdatable.getDisplayListener(),
                new Handler(Looper.getMainLooper()));
    }

    /** Notifies user switching. */
    public void handleUserStarting(@UserIdInt int startingUserId) {
        mUpdatable.handleUserStarting(startingUserId);
    }

    /** Notifies user switching. */
    public void handleCurrentUserSwitching(@UserIdInt int newUserId) {
        mUpdatable.handleCurrentUserSwitching(newUserId);
    }

    /** Notifies user stopped. */
    public void handleUserStopped(@UserIdInt int stoppedUser) {
        mUpdatable.handleUserStopped(stoppedUser);
    }

    /**
     * Decides display to assign while an Activity is launched.
     *
     * <p>For current user (=driver), launching to any display is allowed as long as system
     * allows it.</p>
     *
     * <p>For private display, do not change anything as private display has its own logic.</p>
     *
     * <p>For passenger displays, only run in allowed displays. If requested display is not
     * allowed, change to the 1st allowed display.</p>
     */
    @Override
    @Result
    public int onCalculate(@Nullable Task task, @Nullable ActivityInfo.WindowLayout layout,
            @Nullable ActivityRecord activity, @Nullable ActivityRecord source,
            ActivityOptions options, @Nullable Request request, int phase,
            LaunchParamsController.LaunchParams currentParams,
            LaunchParamsController.LaunchParams outParams) {
        CalculateParams params = CalculateParams.create(task, layout, activity, source,
                options, request, phase, currentParams, outParams, mAtm.mSupportsMultiDisplay);
        return mUpdatable.calculate(params);
    }

    @Nullable
    private TaskDisplayAreaWrapper getDefaultTaskDisplayAreaOnDisplay(int displayId) {
        if (displayId == Display.INVALID_DISPLAY) {
            return null;
        }
        DisplayContent dc = mAtm.mRootWindowContainer.getDisplayContentOrCreate(displayId);
        if (dc == null) {
            return null;
        }
        return TaskDisplayAreaWrapper.create(dc.getDefaultTaskDisplayArea());
    }

    /**
     * Calculates the default {@link TaskDisplayAreaWrapper} for a task. We attempt to put
     * the activity within the same display area if possible. The strategy is to find the display
     * in the following order:
     *
     * <ol>
     *     <li>The display area of the top activity from the launching process will be used</li>
     *     <li>The display area of the top activity from the real launching process will be used
     *     </li>
     *     <li>Default display area from the associated root window container.</li>
     * </ol>
     * @param activityRecordWrapper the activity being started
     * @param requestWrapper optional {@link RequestWrapper} made to start the activity record
     * @return the list of {@link TaskDisplayAreaWrapper} to house the task
     */
    private List<TaskDisplayAreaWrapper> getFallbackDisplayAreasForActivity(
            @NonNull ActivityRecordWrapper activityRecordWrapper,
            @Nullable RequestWrapper requestWrapper) {
        ActivityRecord activityRecord = activityRecordWrapper.getActivityRecord();
        Request request = requestWrapper != null ? requestWrapper.getRequest() : null;
        mFallBackDisplayAreaList.clear();

        WindowProcessController controllerFromLaunchingRecord = mAtm.getProcessController(
                activityRecord.launchedFromPid, activityRecord.launchedFromUid);
        TaskDisplayArea displayAreaForLaunchingRecord = controllerFromLaunchingRecord == null
                ? null : controllerFromLaunchingRecord.getTopActivityDisplayArea();
        if (displayAreaForLaunchingRecord != null) {
            mFallBackDisplayAreaList.add(
                    TaskDisplayAreaWrapper.create(displayAreaForLaunchingRecord));
        }

        WindowProcessController controllerFromProcess = mAtm.getProcessController(
                activityRecord.getProcessName(), activityRecord.getUid());
        TaskDisplayArea displayAreaForRecord = controllerFromProcess == null ? null
                : controllerFromProcess.getTopActivityDisplayArea();
        if (displayAreaForRecord != null) {
            mFallBackDisplayAreaList.add(TaskDisplayAreaWrapper.create(displayAreaForRecord));
        }

        WindowProcessController controllerFromRequest =
                request == null ? null : mAtm.getProcessController(request.realCallingPid,
                        request.realCallingUid);
        TaskDisplayArea displayAreaFromSourceProcess = controllerFromRequest == null ? null
                : controllerFromRequest.getTopActivityDisplayArea();
        if (displayAreaFromSourceProcess != null) {
            mFallBackDisplayAreaList.add(
                    TaskDisplayAreaWrapper.create(displayAreaFromSourceProcess));
        }
        return mFallBackDisplayAreaList;
    }

    @Nullable
    private TaskDisplayAreaWrapper findTaskDisplayArea(int displayId, int featureId) {
        DisplayContent display = mAtm.mRootWindowContainer.getDisplayContentOrCreate(displayId);
        if (display == null) {
            return null;
        }
        TaskDisplayArea tda = display.getItemFromTaskDisplayAreas(
                displayArea -> displayArea.mFeatureId == featureId ? displayArea : null);
        return TaskDisplayAreaWrapper.create(tda);
    }

    private final CarLaunchParamsModifierInterface mBuiltinInterface
            = new CarLaunchParamsModifierInterface() {
        @Nullable
        @Override
        public TaskDisplayAreaWrapper findTaskDisplayArea(int displayId, int featureId) {
            return CarLaunchParamsModifier.this.findTaskDisplayArea(displayId, featureId);
        }

        @Nullable
        @Override
        public TaskDisplayAreaWrapper getDefaultTaskDisplayAreaOnDisplay(int displayId) {
            return CarLaunchParamsModifier.this.getDefaultTaskDisplayAreaOnDisplay(displayId);
        }

        @NonNull
        @Override
        public List<TaskDisplayAreaWrapper> getFallbackDisplayAreasForActivity(
                @NonNull ActivityRecordWrapper activityRecord, @Nullable RequestWrapper request) {
            return CarLaunchParamsModifier.this.getFallbackDisplayAreasForActivity(
                    activityRecord, request);
        }
    };
}
