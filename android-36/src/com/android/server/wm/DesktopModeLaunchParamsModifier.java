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

import static android.app.WindowConfiguration.WINDOWING_MODE_FREEFORM;
import static android.app.WindowConfiguration.WINDOWING_MODE_FULLSCREEN;
import static android.app.WindowConfiguration.WINDOWING_MODE_UNDEFINED;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_INSTANCE;
import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_INSTANCE_PER_TASK;
import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_TASK;

import static com.android.server.wm.ActivityTaskManagerDebugConfig.TAG_ATM;
import static com.android.server.wm.ActivityTaskManagerDebugConfig.TAG_WITH_CLASS_NAME;
import static com.android.server.wm.DesktopModeHelper.canEnterDesktopMode;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityOptions;
import android.app.WindowConfiguration;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.Slog;
import android.window.DesktopModeFlags;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wm.LaunchParamsController.LaunchParamsModifier;
/**
 * The class that defines default launch params for tasks in desktop mode
 */
class DesktopModeLaunchParamsModifier implements LaunchParamsModifier {

    private static final String TAG =
            TAG_WITH_CLASS_NAME ? "DesktopModeLaunchParamsModifier" : TAG_ATM;

    private static final boolean DEBUG = false;

    private StringBuilder mLogBuilder;

    @NonNull private final Context mContext;

    DesktopModeLaunchParamsModifier(@NonNull Context context) {
        mContext = context;
    }

    @Override
    public int onCalculate(@Nullable Task task, @Nullable ActivityInfo.WindowLayout layout,
            @Nullable ActivityRecord activity, @Nullable ActivityRecord source,
            @Nullable ActivityOptions options, @Nullable ActivityStarter.Request request, int phase,
            @NonNull LaunchParamsController.LaunchParams currentParams,
            @NonNull LaunchParamsController.LaunchParams outParams) {

        initLogBuilder(task, activity);
        int result = calculate(task, layout, activity, source, options, request, phase,
                currentParams, outParams);
        outputLog();
        return result;
    }

    private int calculate(@Nullable Task task, @Nullable ActivityInfo.WindowLayout layout,
            @Nullable ActivityRecord activity, @Nullable ActivityRecord source,
            @Nullable ActivityOptions options, @Nullable ActivityStarter.Request request, int phase,
            @NonNull LaunchParamsController.LaunchParams currentParams,
            @NonNull LaunchParamsController.LaunchParams outParams) {

        if (!canEnterDesktopMode(mContext)) {
            appendLog("desktop mode is not enabled, skipping");
            return RESULT_SKIP;
        }

        if (task == null || !task.isAttached()) {
            appendLog("task null, skipping");
            return RESULT_SKIP;
        }

        if (DesktopModeFlags.DISABLE_DESKTOP_LAUNCH_PARAMS_OUTSIDE_DESKTOP_BUG_FIX.isTrue()
                && !isEnteringDesktopMode(task, options, currentParams)) {
            appendLog("not entering desktop mode, skipping");
            return RESULT_SKIP;
        }

        if (com.android.window.flags.Flags.fixLayoutExistingTask()
                && task.getCreatedByOrganizerTask() != null) {
            appendLog("has created-by-organizer-task, skipping");
            return RESULT_SKIP;
        }

        if (!task.isActivityTypeStandardOrUndefined()) {
            appendLog("not standard or undefined activity type, skipping");
            return RESULT_SKIP;
        }
        if (phase < PHASE_WINDOWING_MODE) {
            appendLog("not in windowing mode or bounds phase, skipping");
            return RESULT_SKIP;
        }

        // Copy over any values
        outParams.set(currentParams);

        if (source != null && source.getTask() != null) {
            final Task sourceTask = source.getTask();
            if (DesktopModeFlags.DISABLE_DESKTOP_LAUNCH_PARAMS_OUTSIDE_DESKTOP_BUG_FIX.isTrue()
                    && isEnteringDesktopMode(sourceTask, options, currentParams)) {
                // If trampoline source is not freeform but we are entering or in desktop mode,
                // ignore the source windowing mode and set the windowing mode to freeform
                outParams.mWindowingMode = WINDOWING_MODE_FREEFORM;
                appendLog("freeform window mode applied to task trampoline");
            } else {
                // In Proto2, trampoline task launches of an existing background task can result in
                // the previous windowing mode to be restored even if the desktop mode state has
                // changed. Let task launches inherit the windowing mode from the source task if
                // available, which should have the desired windowing mode set by WM Shell.
                // See b/286929122.
                outParams.mWindowingMode = sourceTask.getWindowingMode();
                appendLog("inherit-from-source=" + outParams.mWindowingMode);
            }
        }

        if (phase == PHASE_WINDOWING_MODE) {
            return RESULT_CONTINUE;
        }

        if (!currentParams.mBounds.isEmpty()) {
            appendLog("currentParams has bounds set, not overriding");
            return RESULT_SKIP;
        }

        if ((options == null || options.getLaunchBounds() == null) && task.hasOverrideBounds()) {
            if (DesktopModeFlags.DISABLE_DESKTOP_LAUNCH_PARAMS_OUTSIDE_DESKTOP_BUG_FIX.isTrue()) {
                // We are in desktop, return result done to prevent other modifiers from modifying
                // exiting task bounds or resolved windowing mode.
                return RESULT_DONE;
            }
            appendLog("current task has bounds set, not overriding");
            return RESULT_SKIP;
        }

        if (DesktopModeFlags.INHERIT_TASK_BOUNDS_FOR_TRAMPOLINE_TASK_LAUNCHES.isTrue()) {
            ActivityRecord topVisibleFreeformActivity =
                    task.getDisplayContent().getTopMostVisibleFreeformActivity();
            if (shouldInheritExistingTaskBounds(topVisibleFreeformActivity, activity, task)) {
                appendLog("inheriting bounds from existing closing instance");
                outParams.mBounds.set(topVisibleFreeformActivity.getBounds());
                appendLog("final desktop mode task bounds set to %s", outParams.mBounds);
                // Return result done to prevent other modifiers from changing or cascading bounds.
                return RESULT_DONE;
            }
        }

        DesktopModeBoundsCalculator.updateInitialBounds(task, layout, activity, options,
                outParams, this::appendLog);
        appendLog("final desktop mode task bounds set to %s", outParams.mBounds);
        if (options != null && options.getFlexibleLaunchSize()) {
            // Return result done to prevent other modifiers from respecting option bounds and
            // applying further cascading. Since other modifiers are being skipped in this case,
            // this modifier is now also responsible to respecting the options launch windowing
            // mode.
            outParams.mWindowingMode = options.getLaunchWindowingMode();
            return RESULT_DONE;
        }
        return RESULT_CONTINUE;
    }

    /**
     * Returns true if a task is entering desktop mode, due to its windowing mode being freeform or
     * if there exists other freeform tasks on the display.
     */
    @VisibleForTesting
    boolean isEnteringDesktopMode(
            @NonNull Task task,
            @Nullable ActivityOptions options,
            @NonNull LaunchParamsController.LaunchParams currentParams) {
        //  As freeform tasks cannot exist outside of desktop mode, it is safe to assume if
        //  freeform tasks are visible we are in desktop mode and as a result any launching
        //  activity will also enter desktop mode. On this same relationship, we can also assume
        //  if there are not visible freeform tasks but a freeform activity is now launching, it
        //  will force the device into desktop mode.
        return (task.getDisplayContent().getTopMostFreeformActivity() != null
                    && checkSourceWindowModesCompatible(task, options, currentParams))
                || isRequestingFreeformWindowMode(task, options, currentParams);
    }

    private boolean isRequestingFreeformWindowMode(
            @NonNull Task task,
            @Nullable ActivityOptions options,
            @NonNull LaunchParamsController.LaunchParams currentParams) {
        return task.inFreeformWindowingMode()
                || (options != null && options.getLaunchWindowingMode() == WINDOWING_MODE_FREEFORM)
                || (currentParams.hasWindowingMode()
                && currentParams.mWindowingMode == WINDOWING_MODE_FREEFORM);
    }

    /**
     * Returns true is all possible source window modes are compatible with desktop mode.
     */
    private boolean checkSourceWindowModesCompatible(
            @NonNull Task task,
            @Nullable ActivityOptions options,
            @NonNull LaunchParamsController.LaunchParams currentParams) {
        return isCompatibleDesktopWindowingMode(task.getWindowingMode())
                && (options == null
                    || isCompatibleDesktopWindowingMode(options.getLaunchWindowingMode()))
                && isCompatibleDesktopWindowingMode(currentParams.mWindowingMode);
    }

    /**
     * Returns true is the requesting window mode is one that can lead to the activity entering
     * desktop.
     */
    private boolean isCompatibleDesktopWindowingMode(
            @WindowConfiguration.WindowingMode int windowingMode) {
        return switch (windowingMode) {
            case WINDOWING_MODE_UNDEFINED,
                 WINDOWING_MODE_FULLSCREEN,
                 WINDOWING_MODE_FREEFORM -> true;
            default -> false;
        };
    }

    /**
     * Whether the launching task should inherit the task bounds of an existing closing instance.
     */
    private boolean shouldInheritExistingTaskBounds(
            @Nullable ActivityRecord existingTaskActivity,
            @Nullable ActivityRecord launchingActivity,
            @NonNull Task launchingTask) {
        if (existingTaskActivity == null || launchingActivity == null) return false;
        return (existingTaskActivity.packageName == launchingActivity.packageName)
                && isLaunchingNewSingleTask(launchingActivity.launchMode)
                && isClosingExitingInstance(launchingTask.getBaseIntent().getFlags());
    }

    /**
     * Returns true if the launch mode will result in a single new task being created for the
     * activity.
     */
    private boolean isLaunchingNewSingleTask(int launchMode) {
        return launchMode == LAUNCH_SINGLE_TASK
                || launchMode == LAUNCH_SINGLE_INSTANCE
                || launchMode == LAUNCH_SINGLE_INSTANCE_PER_TASK;
    }

    /**
     * Returns true if the intent will result in an existing task instance being closed if a new
     * one appears.
     */
    private boolean isClosingExitingInstance(int intentFlags) {
        return (intentFlags & FLAG_ACTIVITY_CLEAR_TASK) != 0
            || (intentFlags & FLAG_ACTIVITY_MULTIPLE_TASK) == 0;
    }

    private void initLogBuilder(Task task, ActivityRecord activity) {
        if (DEBUG) {
            mLogBuilder = new StringBuilder(
                    "DesktopModeLaunchParamsModifier: task=" + task + " activity=" + activity);
        }
    }

    private void appendLog(String format, Object... args) {
        if (DEBUG) mLogBuilder.append(" ").append(String.format(format, args));
    }

    private void outputLog() {
        if (DEBUG) Slog.d(TAG, mLogBuilder.toString());
    }
}
