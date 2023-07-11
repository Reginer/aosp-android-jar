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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.annotation.UserIdInt;
import android.car.app.CarActivityManager;
import android.car.builtin.os.UserManagerHelper;
import android.car.builtin.util.Slogf;
import android.car.builtin.view.DisplayHelper;
import android.car.builtin.window.DisplayAreaOrganizerHelper;
import android.content.ComponentName;
import android.hardware.display.DisplayManager;
import android.os.ServiceSpecificException;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Display;

import com.android.internal.annotations.GuardedBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of {@link CarLaunchParamsModifierUpdatable}.
 *
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public final class CarLaunchParamsModifierUpdatableImpl
        implements CarLaunchParamsModifierUpdatable {
    private static final String TAG = "CAR.LAUNCH";
    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);

    private final CarLaunchParamsModifierInterface mBuiltin;
    private final Object mLock = new Object();

    // Always start with USER_SYSTEM as the timing of handleCurrentUserSwitching(USER_SYSTEM) is not
    // guaranteed to be earler than 1st Activity launch.
    @GuardedBy("mLock")
    private int mCurrentDriverUser = UserManagerHelper.USER_SYSTEM;

    // TODO: Switch from tracking displays to tracking display areas instead
    /**
     * This one is for holding all passenger (=profile user) displays which are mostly static unless
     * displays are added / removed. Note that {@link #mDisplayToProfileUserMapping} can be empty
     * while user is assigned and that cannot always tell if specific display is for driver or not.
     */
    @GuardedBy("mLock")
    private final ArrayList<Integer> mPassengerDisplays = new ArrayList<>();

    /** key: display id, value: profile user id */
    @GuardedBy("mLock")
    private final SparseIntArray mDisplayToProfileUserMapping = new SparseIntArray();

    /** key: profile user id, value: display id */
    @GuardedBy("mLock")
    private final SparseIntArray mDefaultDisplayForProfileUser = new SparseIntArray();

    @GuardedBy("mLock")
    private boolean mIsSourcePreferred;

    @GuardedBy("mLock")
    private List<ComponentName> mSourcePreferredComponents;

    /** key: Activity, value: TaskDisplayAreaWrapper */
    @GuardedBy("mLock")
    private final ArrayMap<ComponentName, TaskDisplayAreaWrapper> mPersistentActivities =
            new ArrayMap<>();

    public CarLaunchParamsModifierUpdatableImpl(CarLaunchParamsModifierInterface builtin) {
        mBuiltin = builtin;
    }

    public DisplayManager.DisplayListener getDisplayListener() {
        return mDisplayListener;
    }

    private final DisplayManager.DisplayListener mDisplayListener =
            new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                    // ignore. car service should update whiltelist.
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                    synchronized (mLock) {
                        mPassengerDisplays.remove(Integer.valueOf(displayId));
                        updateProfileUserConfigForDisplayRemovalLocked(displayId);
                    }
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    // ignore
                }
            };

    @GuardedBy("mLock")
    private void updateProfileUserConfigForDisplayRemovalLocked(int displayId) {
        mDisplayToProfileUserMapping.delete(displayId);
        int i = mDefaultDisplayForProfileUser.indexOfValue(displayId);
        if (i >= 0) {
            mDefaultDisplayForProfileUser.removeAt(i);
        }
    }

    /**
     * Sets {@code sourcePreferred} configuration. When {@code sourcePreferred} is enabled and
     * there is no pre-assigned display for the Activity, CarLauncherParamsModifier will launch
     * the Activity in the display of the source. When {@code sourcePreferredComponents} isn't null
     * the {@code sourcePreferred} is applied for the {@code sourcePreferredComponents} only.
     *
     * @param enableSourcePreferred whether to enable sourcePreferred mode
     * @param sourcePreferredComponents null for all components, or the list of components to apply
     */
    public void setSourcePreferredComponents(boolean enableSourcePreferred,
            @Nullable List<ComponentName> sourcePreferredComponents) {
        synchronized (mLock) {
            mIsSourcePreferred = enableSourcePreferred;
            mSourcePreferredComponents = sourcePreferredComponents;
            if (mSourcePreferredComponents != null) {
                Collections.sort(mSourcePreferredComponents);
            }
        }
    }

    /** Notifies user starting. */
    public void handleUserStarting(int startingUser) {
       // Do nothing
    }

    /** Notifies user switching. */
    public void handleCurrentUserSwitching(@UserIdInt int newUserId) {
        synchronized (mLock) {
            mCurrentDriverUser = newUserId;
            mDefaultDisplayForProfileUser.clear();
            mDisplayToProfileUserMapping.clear();
        }
    }

    @GuardedBy("mLock")
    private void removeUserFromAllowlistsLocked(int userId) {
        for (int i = mDisplayToProfileUserMapping.size() - 1; i >= 0; i--) {
            if (mDisplayToProfileUserMapping.valueAt(i) == userId) {
                mDisplayToProfileUserMapping.removeAt(i);
            }
        }
        mDefaultDisplayForProfileUser.delete(userId);
    }

    /** Notifies user stopped. */
    public void handleUserStopped(@UserIdInt int stoppedUser) {
        // Note that the current user is never stopped. It always takes switching into
        // non-current user before stopping the user.
        synchronized (mLock) {
            removeUserFromAllowlistsLocked(stoppedUser);
        }
    }

    /**
     * Sets display allowlist for the {@code userId}. For passenger user, activity will be always
     * launched to a display in the allowlist. If requested display is not in the allowlist, the 1st
     * display in the allowlist will be selected as target display.
     *
     * <p>The allowlist is kept only for profile user. Assigning the current user unassigns users
     * for the given displays.
     */
    public void setDisplayAllowListForUser(@UserIdInt int userId, int[] displayIds) {
        if (DBG) {
            Slogf.d(TAG, "setDisplayAllowlistForUser userId:%d displays:%s",
                    userId, Arrays.toString(displayIds));
        }
        synchronized (mLock) {
            for (int displayId : displayIds) {
                if (!mPassengerDisplays.contains(displayId)) {
                    Slogf.w(TAG, "setDisplayAllowlistForUser called with display:%d"
                            + " not in passenger display list:%s", displayId, mPassengerDisplays);
                    continue;
                }
                if (userId == mCurrentDriverUser) {
                    mDisplayToProfileUserMapping.delete(displayId);
                } else {
                    mDisplayToProfileUserMapping.put(displayId, userId);
                }
                // now the display cannot be a default display for other user
                int i = mDefaultDisplayForProfileUser.indexOfValue(displayId);
                if (i >= 0) {
                    mDefaultDisplayForProfileUser.removeAt(i);
                }
            }
            if (displayIds.length > 0) {
                mDefaultDisplayForProfileUser.put(userId, displayIds[0]);
            } else {
                removeUserFromAllowlistsLocked(userId);
            }
        }
    }

    /**
     * Sets displays assigned to passenger. All other displays will be treated as assigned to
     * driver.
     *
     * <p>The 1st display in the array will be considered as a default display to assign
     * for any non-driver user if there is no display assigned for the user. </p>
     */
    public void setPassengerDisplays(int[] displayIdsForPassenger) {
        if (DBG) {
            Slogf.d(TAG, "setPassengerDisplays displays:%s",
                    Arrays.toString(displayIdsForPassenger));
        }
        synchronized (mLock) {
            for (int id : displayIdsForPassenger) {
                mPassengerDisplays.remove(Integer.valueOf(id));
            }
            // handle removed displays
            for (int i = 0; i < mPassengerDisplays.size(); i++) {
                int displayId = mPassengerDisplays.get(i);
                updateProfileUserConfigForDisplayRemovalLocked(displayId);
            }
            mPassengerDisplays.clear();
            mPassengerDisplays.ensureCapacity(displayIdsForPassenger.length);
            for (int id : displayIdsForPassenger) {
                mPassengerDisplays.add(id);
            }
        }
    }

    /**
     * Calculates {@code outParams} based on the given arguments.
     * See {@code LaunchParamsController.LaunchParamsModifier.onCalculate()} for the detail.
     */
    public int calculate(CalculateParams params) {
        TaskWrapper task = params.getTask();
        ActivityRecordWrapper activity = params.getActivity();
        ActivityRecordWrapper source = params.getSource();
        ActivityOptionsWrapper options = params.getOptions();
        RequestWrapper request = params.getRequest();
        LaunchParamsWrapper currentParams = params.getCurrentParams();
        LaunchParamsWrapper outParams = params.getOutParams();

        int userId;
        if (task != null) {
            userId = task.getUserId();
        } else if (activity != null) {
            userId = activity.getUserId();
        } else {
            Slogf.w(TAG, "onCalculate, cannot decide user");
            return LaunchParamsWrapper.RESULT_SKIP;
        }
        // DisplayArea where user wants to launch the Activity.
        TaskDisplayAreaWrapper originalDisplayArea = currentParams.getPreferredTaskDisplayArea();
        // DisplayArea where CarLaunchParamsModifier targets to launch the Activity.
        TaskDisplayAreaWrapper targetDisplayArea = null;
        if (DBG) {
            Slogf.d(TAG, "onCalculate, userId:%d original displayArea:%s ActivityOptions:%s",
                    userId, originalDisplayArea, options);
        }
        ComponentName activityName = activity.getComponentName();
        decision:
        synchronized (mLock) {
            // If originalDisplayArea is set, respect that before ActivityOptions check.
            if (originalDisplayArea == null) {
                if (options != null) {
                    originalDisplayArea = options.getLaunchTaskDisplayArea();
                    if (originalDisplayArea == null) {
                        originalDisplayArea = mBuiltin.getDefaultTaskDisplayAreaOnDisplay(
                                options.getOptions().getLaunchDisplayId());
                    }
                }
            }
            if (mPersistentActivities.containsKey(activityName)) {
                targetDisplayArea = mPersistentActivities.get(activityName);
            } else if (originalDisplayArea == null  // No specified DA to launch the Activity
                    && mIsSourcePreferred && source != null
                    && (mSourcePreferredComponents == null || Collections.binarySearch(
                    mSourcePreferredComponents, activityName) >= 0)) {
                targetDisplayArea = source.isNoDisplay() ? source.getHandoverTaskDisplayArea()
                        : source.getDisplayArea();
            } else if (originalDisplayArea == null
                    && task == null  // launching as a new task
                    && source != null && !source.isDisplayTrusted()
                    && !source.allowingEmbedded()) {
                if (DBG) {
                    Slogf.d(TAG, "Disallow launch on virtual display for not-embedded activity.");
                }
                targetDisplayArea = mBuiltin.getDefaultTaskDisplayAreaOnDisplay(
                        Display.DEFAULT_DISPLAY);
            }
            if (userId == mCurrentDriverUser) {
                // Respect the existing DisplayArea.
                break decision;
            }
            if (userId == UserManagerHelper.USER_SYSTEM) {
                // This will be only allowed if it has FLAG_SHOW_FOR_ALL_USERS.
                // The flag is not immediately accessible here so skip the check.
                // But other WM policy will enforce it.
                break decision;
            }
            // Now user is a passenger.
            if (mPassengerDisplays.isEmpty()) {
                // No displays for passengers. This could be old user and do not do anything.
                break decision;
            }
            if (targetDisplayArea == null) {
                if (originalDisplayArea != null) {
                    targetDisplayArea = originalDisplayArea;
                } else {
                    targetDisplayArea = mBuiltin.getDefaultTaskDisplayAreaOnDisplay(
                            Display.DEFAULT_DISPLAY);
                }
            }
            Display display = targetDisplayArea.getDisplay();
            if ((display.getFlags() & Display.FLAG_PRIVATE) != 0) {
                // private display should follow its own restriction rule.
                break decision;
            }
            if (DisplayHelper.getType(display) == DisplayHelper.TYPE_VIRTUAL) {
                // TODO(b/132903422) : We need to update this after the bug is resolved.
                // For now, don't change anything.
                break decision;
            }
            int userForDisplay = mDisplayToProfileUserMapping.get(display.getDisplayId(),
                    UserManagerHelper.USER_NULL);
            if (userForDisplay == userId) {
                break decision;
            }
            targetDisplayArea = getAlternativeDisplayAreaForPassengerLocked(
                    userId, activity, request);
        }
        if (targetDisplayArea != null && originalDisplayArea != targetDisplayArea) {
            Slogf.i(TAG, "Changed launching display, user:%d requested display area:%s"
                    + " target display area:", userId, originalDisplayArea, targetDisplayArea);
            outParams.setPreferredTaskDisplayArea(targetDisplayArea);
            return LaunchParamsWrapper.RESULT_DONE;
        } else {
            return LaunchParamsWrapper.RESULT_SKIP;
        }
    }

    @GuardedBy("mLock")
    @Nullable
    private TaskDisplayAreaWrapper getAlternativeDisplayAreaForPassengerLocked(int userId,
            @NonNull ActivityRecordWrapper activtyRecord, @Nullable RequestWrapper request) {
        List<TaskDisplayAreaWrapper> fallbacks = mBuiltin.getFallbackDisplayAreasForActivity(
                activtyRecord, request);
        for (int i = 0, size = fallbacks.size(); i < size; ++i) {
            TaskDisplayAreaWrapper fallbackTda = fallbacks.get(i);
            int userForDisplay = getUserIdForDisplayLocked(fallbackTda.getDisplay().getDisplayId());
            if (userForDisplay == userId) {
                return fallbackTda;
            }
        }
        return fallbackDisplayAreaForUserLocked(userId);
    }

    /**
     * Returns {@code userId} who is allowed to use the given {@code displayId}, or
     * {@code UserHandle.USER_NULL} if the display doesn't exist in the mapping.
     */
    @GuardedBy("mLock")
    private int getUserIdForDisplayLocked(int displayId) {
        return mDisplayToProfileUserMapping.get(displayId, UserManagerHelper.USER_NULL);
    }

    /**
     * Return a {@link TaskDisplayAreaWrapper} that can be used if a source display area is
     * not found. First check the default display for the user. If it is absent select
     * the first passenger display if present.  If both are absent return {@code null}
     *
     * @param userId ID of the active user
     * @return {@link TaskDisplayAreaWrapper} that is recommended when a display area is
     *     not specified
     */
    @GuardedBy("mLock")
    @Nullable
    private TaskDisplayAreaWrapper fallbackDisplayAreaForUserLocked(@UserIdInt int userId) {
        int displayIdForUserProfile = mDefaultDisplayForProfileUser.get(userId,
                Display.INVALID_DISPLAY);
        if (displayIdForUserProfile != Display.INVALID_DISPLAY) {
            int displayId = mDefaultDisplayForProfileUser.get(userId);
            return mBuiltin.getDefaultTaskDisplayAreaOnDisplay(displayId);
        }
        if (!mPassengerDisplays.isEmpty()) {
            int displayId = mPassengerDisplays.get(0);
            return mBuiltin.getDefaultTaskDisplayAreaOnDisplay(displayId);
        }
        return null;
    }

    /**
     * See {@link CarActivityManager#setPersistentActivity(android.content.ComponentName,int, int)}
     */
    public int setPersistentActivity(ComponentName activity, int displayId, int featureId) {
        if (DBG) {
            Slogf.d(TAG, "setPersistentActivity: activity=%s, displayId=%d, featureId=%d",
                    activity, displayId, featureId);
        }
        if (featureId == DisplayAreaOrganizerHelper.FEATURE_UNDEFINED) {
            synchronized (mLock) {
                TaskDisplayAreaWrapper removed = mPersistentActivities.remove(activity);
                if (removed == null) {
                    throw new ServiceSpecificException(
                            CarActivityManager.ERROR_CODE_ACTIVITY_NOT_FOUND,
                            "Failed to remove " + activity.toShortString());
                }
                return CarActivityManager.RESULT_SUCCESS;
            }
        }
        TaskDisplayAreaWrapper tda = mBuiltin.findTaskDisplayArea(displayId, featureId);
        if (tda == null) {
            throw new IllegalArgumentException("Unknown display=" + displayId
                    + " or feature=" + featureId);
        }
        synchronized (mLock) {
            mPersistentActivities.put(activity, tda);
        }
        return CarActivityManager.RESULT_SUCCESS;
    }
}