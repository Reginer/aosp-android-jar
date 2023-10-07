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
import android.car.builtin.annotation.PlatformVersion;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;

import com.android.annotation.AddedIn;

/**
 * Wrapper of {@link ActivityRecord}.
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public final class ActivityRecordWrapper {
    private final ActivityRecord mActivityRecord;

    private ActivityRecordWrapper(ActivityRecord activityRecord) {
        mActivityRecord = activityRecord;
    }

    /** @hide */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public static ActivityRecordWrapper create(@Nullable ActivityRecord activityRecord) {
        if (activityRecord == null) return null;
        return new ActivityRecordWrapper(activityRecord);
    }

    /** @hide */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public ActivityRecord getActivityRecord() {
        return mActivityRecord;
    }

    /**
     * Gets which user this Activity is running for.
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public int getUserId() {
        return mActivityRecord.mUserId;
    }

    /**
     * Gets the actual {@link ComponentName} of this Activity.
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public ComponentName getComponentName() {
        if (mActivityRecord.info == null) return null;
        return mActivityRecord.info.getComponentName();
    }

    /**
     * Gets the {@link TaskDisplayAreaWrapper} where this is located.
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public TaskDisplayAreaWrapper getDisplayArea() {
        return TaskDisplayAreaWrapper.create(mActivityRecord.getDisplayArea());
    }

    /**
     * Returns whether this Activity is not displayed.
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public boolean isNoDisplay() {
        return mActivityRecord.noDisplay;
    }

    /**
     * Gets {@link TaskDisplayAreaWrapper} where the handover Activity is supposed to launch
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public TaskDisplayAreaWrapper getHandoverTaskDisplayArea() {
        return TaskDisplayAreaWrapper.create(mActivityRecord.mHandoverTaskDisplayArea);
    }

    /**
     * Gets {@code displayId} where the handover Activity is supposed to launch
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public int getHandoverLaunchDisplayId() {
        return mActivityRecord.mHandoverLaunchDisplayId;
    }

    /**
     * Returns whether this Activity allows to be embedded in the other Activity.
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public boolean allowingEmbedded() {
        if (mActivityRecord.info == null) return false;
        return (mActivityRecord.info.flags & ActivityInfo.FLAG_ALLOW_EMBEDDED) != 0;
    }

    /**
     * Returns whether the display where this Activity is located is trusted.
     */
    @AddedIn(PlatformVersion.TIRAMISU_0)
    public boolean isDisplayTrusted() {
        return mActivityRecord.getDisplayContent().isTrusted();
    }

    @Override
    public String toString() {
        return mActivityRecord.toString();
    }
}
