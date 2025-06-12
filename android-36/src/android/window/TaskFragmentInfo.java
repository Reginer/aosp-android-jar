/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.window;

import static android.app.WindowConfiguration.WindowingMode;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.TestApi;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores information about a particular TaskFragment.
 * @hide
 */
@TestApi
public final class TaskFragmentInfo implements Parcelable {

    /**
     * Client assigned unique token in {@link TaskFragmentCreationParams#getFragmentToken()} to
     * create this TaskFragment with.
     */
    @NonNull
    private final IBinder mFragmentToken;

    @NonNull
    private final WindowContainerToken mToken;

    @NonNull
    private final Configuration mConfiguration = new Configuration();

    /** The number of the running activities in the TaskFragment. */
    private final int mRunningActivityCount;

    /** Whether this TaskFragment is visible on the window hierarchy. */
    private final boolean mIsVisible;

    /**
     * List of Activity tokens that are children of this TaskFragment. It only contains Activities
     * that belong to the organizer process for security.
     */
    @NonNull
    private final List<IBinder> mActivities = new ArrayList<>();

    /**
     * List of Activity tokens that were explicitly requested to be launched in this TaskFragment.
     * It only contains Activities that belong to the organizer process for security.
     */
    @NonNull
    private final List<IBinder> mInRequestedTaskFragmentActivities = new ArrayList<>();

    /** Relative position of the fragment's top left corner in the parent container. */
    private final Point mPositionInParent = new Point();

    /**
     * Whether the last running activity in the TaskFragment was finished due to clearing task while
     * launching an activity in the host Task.
     */
    private final boolean mIsTaskClearedForReuse;

    /**
     * Whether the last running activity in the TaskFragment was reparented to a different Task
     * because it is entering PiP.
     */
    private final boolean mIsTaskFragmentClearedForPip;

    /**
     * Whether the last running activity of the TaskFragment was removed because it was reordered to
     * front of the Task.
     */
    private final boolean mIsClearedForReorderActivityToFront;

    /**
     * The maximum {@link android.content.pm.ActivityInfo.WindowLayout#minWidth} and
     * {@link android.content.pm.ActivityInfo.WindowLayout#minHeight} aggregated from the
     * TaskFragment's child activities.
     */
    @NonNull
    private final Point mMinimumDimensions = new Point();

    private final boolean mIsTopNonFishingChild;

    /** @hide */
    public TaskFragmentInfo(
            @NonNull IBinder fragmentToken, @NonNull WindowContainerToken token,
            @NonNull Configuration configuration, int runningActivityCount,
            boolean isVisible, @NonNull List<IBinder> activities,
            @NonNull List<IBinder> inRequestedTaskFragmentActivities,
            @NonNull Point positionInParent, boolean isTaskClearedForReuse,
            boolean isTaskFragmentClearedForPip, boolean isClearedForReorderActivityToFront,
            @NonNull Point minimumDimensions, boolean isTopNonFinishingChild) {
        mFragmentToken = requireNonNull(fragmentToken);
        mToken = requireNonNull(token);
        mConfiguration.setTo(configuration);
        mRunningActivityCount = runningActivityCount;
        mIsVisible = isVisible;
        mActivities.addAll(activities);
        mInRequestedTaskFragmentActivities.addAll(inRequestedTaskFragmentActivities);
        mPositionInParent.set(positionInParent);
        mIsTaskClearedForReuse = isTaskClearedForReuse;
        mIsTaskFragmentClearedForPip = isTaskFragmentClearedForPip;
        mIsClearedForReorderActivityToFront = isClearedForReorderActivityToFront;
        mMinimumDimensions.set(minimumDimensions);
        mIsTopNonFishingChild = isTopNonFinishingChild;
    }

    @NonNull
    public IBinder getFragmentToken() {
        return mFragmentToken;
    }

    @NonNull
    public WindowContainerToken getToken() {
        return mToken;
    }

    @NonNull
    public Configuration getConfiguration() {
        return mConfiguration;
    }

    public boolean isEmpty() {
        return mRunningActivityCount == 0;
    }

    public boolean hasRunningActivity() {
        return mRunningActivityCount > 0;
    }

    public int getRunningActivityCount() {
        return mRunningActivityCount;
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    @NonNull
    public List<IBinder> getActivities() {
        return mActivities;
    }

    @NonNull
    public List<IBinder> getActivitiesRequestedInTaskFragment() {
        return mInRequestedTaskFragmentActivities;
    }

    /** Returns the relative position of the fragment's top left corner in the parent container. */
    @NonNull
    public Point getPositionInParent() {
        return mPositionInParent;
    }

    public boolean isTaskClearedForReuse() {
        return mIsTaskClearedForReuse;
    }

    /** @hide */
    public boolean isTaskFragmentClearedForPip() {
        return mIsTaskFragmentClearedForPip;
    }

    /** @hide */
    public boolean isClearedForReorderActivityToFront() {
        return mIsClearedForReorderActivityToFront;
    }

    @WindowingMode
    public int getWindowingMode() {
        return mConfiguration.windowConfiguration.getWindowingMode();
    }

    /**
     * Returns the minimum width this TaskFragment can be resized to.
     * Client side must not {@link WindowContainerTransaction#setRelativeBounds}
     * that {@link Rect#width()} is shorter than the reported value.
     * @hide pending unhide
     */
    public int getMinimumWidth() {
        return mMinimumDimensions.x;
    }

    /**
     * Returns the minimum width this TaskFragment can be resized to.
     * Client side must not {@link WindowContainerTransaction#setRelativeBounds}
     * that {@link Rect#height()} is shorter than the reported value.
     * @hide pending unhide
     */
    public int getMinimumHeight() {
        return mMinimumDimensions.y;
    }

    /**
     * Indicates that this TaskFragment is the top non-finishing child of its parent container
     * among all Activities and TaskFragment siblings.
     *
     * @hide
     */
    public boolean isTopNonFinishingChild() {
        return mIsTopNonFishingChild;
    }

    /**
     * Returns {@code true} if the parameters that are important for task fragment organizers are
     * equal between this {@link TaskFragmentInfo} and {@param that}.
     * Note that this method is usually called with
     * {@link com.android.server.wm.WindowOrganizerController#configurationsAreEqualForOrganizer(
     * Configuration, Configuration)} to determine if this {@link TaskFragmentInfo} should
     * be dispatched to the client.
     */
    public boolean equalsForTaskFragmentOrganizer(@Nullable TaskFragmentInfo that) {
        if (that == null) {
            return false;
        }

        return mFragmentToken.equals(that.mFragmentToken)
                && mToken.equals(that.mToken)
                && mRunningActivityCount == that.mRunningActivityCount
                && mIsVisible == that.mIsVisible
                && getWindowingMode() == that.getWindowingMode()
                && mActivities.equals(that.mActivities)
                && mInRequestedTaskFragmentActivities.equals(
                        that.mInRequestedTaskFragmentActivities)
                && mPositionInParent.equals(that.mPositionInParent)
                && mIsTaskClearedForReuse == that.mIsTaskClearedForReuse
                && mIsTaskFragmentClearedForPip == that.mIsTaskFragmentClearedForPip
                && mIsClearedForReorderActivityToFront == that.mIsClearedForReorderActivityToFront
                && mMinimumDimensions.equals(that.mMinimumDimensions)
                && mIsTopNonFishingChild == that.mIsTopNonFishingChild;
    }

    private TaskFragmentInfo(Parcel in) {
        mFragmentToken = in.readStrongBinder();
        mToken = in.readTypedObject(WindowContainerToken.CREATOR);
        mConfiguration.readFromParcel(in);
        mRunningActivityCount = in.readInt();
        mIsVisible = in.readBoolean();
        in.readBinderList(mActivities);
        in.readBinderList(mInRequestedTaskFragmentActivities);
        mPositionInParent.readFromParcel(in);
        mIsTaskClearedForReuse = in.readBoolean();
        mIsTaskFragmentClearedForPip = in.readBoolean();
        mIsClearedForReorderActivityToFront = in.readBoolean();
        mMinimumDimensions.readFromParcel(in);
        mIsTopNonFishingChild = in.readBoolean();
    }

    /** @hide */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStrongBinder(mFragmentToken);
        dest.writeTypedObject(mToken, flags);
        mConfiguration.writeToParcel(dest, flags);
        dest.writeInt(mRunningActivityCount);
        dest.writeBoolean(mIsVisible);
        dest.writeBinderList(mActivities);
        dest.writeBinderList(mInRequestedTaskFragmentActivities);
        mPositionInParent.writeToParcel(dest, flags);
        dest.writeBoolean(mIsTaskClearedForReuse);
        dest.writeBoolean(mIsTaskFragmentClearedForPip);
        dest.writeBoolean(mIsClearedForReorderActivityToFront);
        mMinimumDimensions.writeToParcel(dest, flags);
        dest.writeBoolean(mIsTopNonFishingChild);
    }

    @NonNull
    public static final Creator<TaskFragmentInfo> CREATOR =
            new Creator<TaskFragmentInfo>() {
                @Override
                public TaskFragmentInfo createFromParcel(Parcel in) {
                    return new TaskFragmentInfo(in);
                }

                @Override
                public TaskFragmentInfo[] newArray(int size) {
                    return new TaskFragmentInfo[size];
                }
            };

    @Override
    public String toString() {
        return "TaskFragmentInfo{"
                + " fragmentToken=" + mFragmentToken
                + " token=" + mToken
                + " runningActivityCount=" + mRunningActivityCount
                + " isVisible=" + mIsVisible
                + " activities=" + mActivities
                + " inRequestedTaskFragmentActivities" + mInRequestedTaskFragmentActivities
                + " positionInParent=" + mPositionInParent
                + " isTaskClearedForReuse=" + mIsTaskClearedForReuse
                + " isTaskFragmentClearedForPip=" + mIsTaskFragmentClearedForPip
                + " mIsClearedForReorderActivityToFront=" + mIsClearedForReorderActivityToFront
                + " minimumDimensions=" + mMinimumDimensions
                + " isTopNonFinishingChild=" + mIsTopNonFishingChild
                + "}";
    }

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }
}
