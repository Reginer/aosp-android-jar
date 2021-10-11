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
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseIntArray;
import android.view.Display;
import android.window.WindowContainerToken;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
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
 */
public final class CarLaunchParamsModifier implements LaunchParamsController.LaunchParamsModifier {

    private static final String TAG = "CAR.LAUNCH";
    private static final boolean DBG = false;

    private final Context mContext;

    private DisplayManager mDisplayManager;  // set only from init()
    private ActivityTaskManagerService mAtm;  // set only from init()

    private final Object mLock = new Object();

    // Always start with USER_SYSTEM as the timing of handleCurrentUserSwitching(USER_SYSTEM) is not
    // guaranteed to be earler than 1st Activity launch.
    @GuardedBy("mLock")
    private int mCurrentDriverUser = UserHandle.USER_SYSTEM;

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


    @VisibleForTesting
    final DisplayManager.DisplayListener mDisplayListener =
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

    private void updateProfileUserConfigForDisplayRemovalLocked(int displayId) {
        mDisplayToProfileUserMapping.delete(displayId);
        int i = mDefaultDisplayForProfileUser.indexOfValue(displayId);
        if (i >= 0) {
            mDefaultDisplayForProfileUser.removeAt(i);
        }
    }

    /** Constructor. Can be constructed any time. */
    public CarLaunchParamsModifier(Context context) {
        // This can be very early stage. So postpone interaction with other system until init.
        mContext = context;
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
        mDisplayManager.registerDisplayListener(mDisplayListener,
                new Handler(Looper.getMainLooper()));
    }

    /**
     * Sets sourcePreferred configuration. When sourcePreferred is enabled and there is no pre-
     * assigned display for the Activity, CarLauncherParamsModifier will launch the Activity in
     * the display of the source. When sourcePreferredComponents isn't null the sourcePreferred
     * is applied for the sourcePreferredComponents only.
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

    /** Notifies user switching. */
    public void handleCurrentUserSwitching(int newUserId) {
        synchronized (mLock) {
            mCurrentDriverUser = newUserId;
            mDefaultDisplayForProfileUser.clear();
            mDisplayToProfileUserMapping.clear();
        }
    }

    private void removeUserFromAllowlistsLocked(int userId) {
        for (int i = mDisplayToProfileUserMapping.size() - 1; i >= 0; i--) {
            if (mDisplayToProfileUserMapping.valueAt(i) == userId) {
                mDisplayToProfileUserMapping.removeAt(i);
            }
        }
        mDefaultDisplayForProfileUser.delete(userId);
    }

    /** Notifies user stopped. */
    public void handleUserStopped(int stoppedUser) {
        // Note that the current user is never stopped. It always takes switching into
        // non-current user before stopping the user.
        synchronized (mLock) {
            removeUserFromAllowlistsLocked(stoppedUser);
        }
    }

    /**
     * Sets display allowlist for the userId. For passenger user, activity will be always launched
     * to a display in the allowlist. If requested display is not in the allowlist, the 1st display
     * in the allowlist will be selected as target display.
     *
     * <p>The allowlist is kept only for profile user. Assigning the current user unassigns users
     * for the given displays.
     */
    public void setDisplayAllowListForUser(int userId, int[] displayIds) {
        if (DBG) {
            Slog.d(TAG, "setDisplayAllowlistForUser userId:" + userId
                    + " displays:" + displayIds);
        }
        synchronized (mLock) {
            for (int displayId : displayIds) {
                if (!mPassengerDisplays.contains(displayId)) {
                    Slog.w(TAG, "setDisplayAllowlistForUser called with display:" + displayId
                            + " not in passenger display list:" + mPassengerDisplays);
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
            Slog.d(TAG, "setPassengerDisplays displays:" + displayIdsForPassenger);
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
        int userId;
        if (task != null) {
            userId = task.mUserId;
        } else if (activity != null) {
            userId = activity.mUserId;
        } else {
            Slog.w(TAG, "onCalculate, cannot decide user");
            return RESULT_SKIP;
        }
        // DisplayArea where user wants to launch the Activity.
        TaskDisplayArea originalDisplayArea = currentParams.mPreferredTaskDisplayArea;
        // DisplayArea where CarLaunchParamsModifier targets to launch the Activity.
        TaskDisplayArea targetDisplayArea = null;
        if (DBG) {
            Slog.d(TAG, "onCalculate, userId:" + userId
                    + " original displayArea:" + originalDisplayArea
                    + " ActivityOptions:" + options);
        }
        // If originalDisplayArea is set, respect that before ActivityOptions check.
        if (originalDisplayArea == null) {
            if (options != null) {
                WindowContainerToken daToken = options.getLaunchTaskDisplayArea();
                if (daToken != null) {
                    originalDisplayArea = (TaskDisplayArea) WindowContainer.fromBinder(
                            daToken.asBinder());
                } else {
                    int originalDisplayId = options.getLaunchDisplayId();
                    if (originalDisplayId != Display.INVALID_DISPLAY) {
                        originalDisplayArea = getDefaultTaskDisplayAreaOnDisplay(originalDisplayId);
                    }
                }
            }
        }
        decision:
        synchronized (mLock) {
            if (originalDisplayArea == null  // No specified DisplayArea to launch the Activity
                    && mIsSourcePreferred && source != null
                    && (mSourcePreferredComponents == null || Collections.binarySearch(
                            mSourcePreferredComponents, activity.info.getComponentName()) >= 0)) {
                targetDisplayArea = source.noDisplay ? source.mHandoverTaskDisplayArea
                        : source.getDisplayArea();
            } else if (originalDisplayArea == null
                    && task == null  // launching as a new task
                    && source != null && !source.getDisplayContent().isTrusted()
                    && ((activity.info.flags & ActivityInfo.FLAG_ALLOW_EMBEDDED) == 0)) {
                if (DBG) {
                    Slog.d(TAG, "Disallow launch on virtual display for not-embedded activity.");
                }
                targetDisplayArea = getDefaultTaskDisplayAreaOnDisplay(Display.DEFAULT_DISPLAY);
            }
            if (userId == mCurrentDriverUser) {
                // Respect the existing DisplayArea.
                break decision;
            }
            if (userId == UserHandle.USER_SYSTEM) {
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
                    targetDisplayArea = getDefaultTaskDisplayAreaOnDisplay(Display.DEFAULT_DISPLAY);
                }
            }
            Display display = targetDisplayArea.mDisplayContent.getDisplay();
            if ((display.getFlags() & Display.FLAG_PRIVATE) != 0) {
                // private display should follow its own restriction rule.
                break decision;
            }
            if (display.getType() == Display.TYPE_VIRTUAL) {
                // TODO(b/132903422) : We need to update this after the bug is resolved.
                // For now, don't change anything.
                break decision;
            }
            int userForDisplay = mDisplayToProfileUserMapping.get(display.getDisplayId(),
                    UserHandle.USER_NULL);
            if (userForDisplay == userId) {
                break decision;
            }
            targetDisplayArea = getAlternativeDisplayAreaForPassengerLocked(
                    userId, activity, request);
        }
        if (targetDisplayArea != null && originalDisplayArea != targetDisplayArea) {
            Slog.i(TAG, "Changed launching display, user:" + userId
                    + " requested display area:" + originalDisplayArea
                    + " target display area:" + targetDisplayArea);
            outParams.mPreferredTaskDisplayArea = targetDisplayArea;
            return RESULT_DONE;
        } else {
            return RESULT_SKIP;
        }
    }

    @Nullable
    private TaskDisplayArea getAlternativeDisplayAreaForPassengerLocked(int userId,
            @NonNull ActivityRecord activityRecord, @Nullable Request request) {
        TaskDisplayArea sourceDisplayArea = sourceDisplayArea(userId, activityRecord, request);

        return sourceDisplayArea != null ? sourceDisplayArea : fallbackDisplayArea(userId);
    }

    @VisibleForTesting
    @Nullable
    TaskDisplayArea getDefaultTaskDisplayAreaOnDisplay(int displayId) {
        DisplayContent dc = mAtm.mRootWindowContainer.getDisplayContentOrCreate(displayId);
        if (dc == null) {
            return null;
        }
        return dc.getDefaultTaskDisplayArea();
    }

    /**
     * Calculates the {@link TaskDisplayArea} for the source of the request. The source is
     * calculated implicitly from the request or the activity record.
     *
     * @param userId ID of the current active user
     * @param activityRecord {@link ActivityRecord} that is to be shown
     * @param request {@link Request} data for showing the {@link ActivityRecord}
     * @return {@link TaskDisplayArea} First non {@code null} candidate display area that is allowed
     * for the user.  It is allowed if the display has been added to the profile mapping.
     */
    @Nullable
    private TaskDisplayArea sourceDisplayArea(int userId, @NonNull ActivityRecord activityRecord,
            @Nullable Request request) {
        List<WindowProcessController> candidateControllers = candidateControllers(activityRecord,
                request);

        for (int i = 0; i < candidateControllers.size(); i++) {
            WindowProcessController controller = candidateControllers.get(i);
            TaskDisplayArea candidate = controller.getTopActivityDisplayArea();
            int displayId = candidate != null ? candidate.getDisplayId() : Display.INVALID_DISPLAY;
            int userForDisplay = mDisplayToProfileUserMapping.get(displayId, UserHandle.USER_NULL);
            if (userForDisplay == userId) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Calculates a list of {@link WindowProcessController} that can calculate the
     * {@link TaskDisplayArea} to house the {@link ActivityRecord}. Controllers are calculated since
     * calculating the display can be expensive. The list is ordered in the
     * following way
     * <ol>
     *     <li>Controller for the activity record from the process name and app uid</li>
     *     <li>Controller for the activity that is launching the given record</li>
     *     <li>Controller for the actual process that is launching the record</li>
     * </ol>
     *
     * @param activityRecord {@link ActivityRecord} that is to be shown
     * @param request {@link Request} data for showing the {@link ActivityRecord}
     * @return {@link List} of {@link WindowProcessController} ordered by preference to be shown
     */
    private List<WindowProcessController> candidateControllers(
            @NonNull ActivityRecord activityRecord, @Nullable Request request) {
        WindowProcessController firstController = mAtm.getProcessController(
                activityRecord.getProcessName(), activityRecord.getUid());

        WindowProcessController secondController = mAtm.getProcessController(
                activityRecord.getLaunchedFromPid(), activityRecord.getLaunchedFromUid());

        WindowProcessController thirdController = request == null ? null :
                mAtm.getProcessController(request.realCallingPid, request.realCallingUid);

        List<WindowProcessController> candidates = new ArrayList<>(3);

        if (firstController != null) {
            candidates.add(firstController);
        }
        if (secondController != null) {
            candidates.add(secondController);
        }
        if (thirdController != null) {
            candidates.add(thirdController);
        }

        return candidates;
    }

    /**
     * Return a {@link TaskDisplayArea} that can be used if a source display area is not found.
     * First check the default display for the user. If it is absent select the first passenger
     * display if present.  If both are absent return {@code null}
     *
     * @param userId ID of the active user
     * @return {@link TaskDisplayArea} that is recommended when a display area is not specified
     */
    @Nullable
    private TaskDisplayArea fallbackDisplayArea(int userId) {
        int displayIdForUserProfile = mDefaultDisplayForProfileUser.get(userId,
                Display.INVALID_DISPLAY);
        if (displayIdForUserProfile != Display.INVALID_DISPLAY) {
            int displayId = mDefaultDisplayForProfileUser.get(userId);
            return getDefaultTaskDisplayAreaOnDisplay(displayId);
        }

        if (!mPassengerDisplays.isEmpty()) {
            int displayId = mPassengerDisplays.get(0);
            return getDefaultTaskDisplayAreaOnDisplay(displayId);
        }

        return null;
    }

}
