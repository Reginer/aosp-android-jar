/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.server.wm;

import static com.android.server.wm.WindowManagerDebugConfig.DEBUG_UNKNOWN_APP_VISIBILITY;
import static com.android.server.wm.WindowManagerDebugConfig.TAG_WITH_CLASS_NAME;
import static com.android.server.wm.WindowManagerDebugConfig.TAG_WM;

import android.annotation.NonNull;
import android.util.ArrayMap;
import android.util.Slog;

import java.io.PrintWriter;

/**
 * Manages the set of {@link ActivityRecord}s for which we don't know yet whether it's visible or
 * not. This happens when starting an activity while the lockscreen is showing. In that case, the
 * keyguard flags an app might set influence it's visibility, so we wait until this is resolved to
 * start the transition to avoid flickers.
 */
class UnknownAppVisibilityController {

    private static final String TAG = TAG_WITH_CLASS_NAME ? "UnknownAppVisibility" : TAG_WM;

    /**
     * We are currently waiting until the app is done resuming.
     */
    private static final int UNKNOWN_STATE_WAITING_RESUME = 1;

    /**
     * The activity has finished resuming, and we are waiting on the next relayout.
     */
    private static final int UNKNOWN_STATE_WAITING_RELAYOUT = 2;

    /**
     * The client called {@link Session#relayout} with the appropriate Keyguard flags and we are
     * waiting until activity manager has updated the visibilities of all the apps.
     */
    private static final int UNKNOWN_STATE_WAITING_VISIBILITY_UPDATE = 3;

    // Set of apps for which we don't know yet whether it's visible or not, depending on what kind
    // of lockscreen flags the app might set during its first relayout.
    private final ArrayMap<ActivityRecord, Integer> mUnknownApps = new ArrayMap<>();

    private final WindowManagerService mService;

    private final DisplayContent mDisplayContent;

    UnknownAppVisibilityController(WindowManagerService service, DisplayContent displayContent) {
        mService = service;
        mDisplayContent = displayContent;
    }

    boolean allResolved() {
        return mUnknownApps.isEmpty();
    }

    boolean isVisibilityUnknown(ActivityRecord r) {
        if (mUnknownApps.isEmpty()) {
            return false;
        }
        return mUnknownApps.containsKey(r);
    }

    void clear() {
        mUnknownApps.clear();
    }

    String getDebugMessage() {
        final StringBuilder builder = new StringBuilder();
        for (int i = mUnknownApps.size() - 1; i >= 0; i--) {
            builder.append("app=").append(mUnknownApps.keyAt(i))
                    .append(" state=").append(mUnknownApps.valueAt(i));
            if (i != 0) {
                builder.append(' ');
            }
        }
        return builder.toString();
    }

    void appRemovedOrHidden(@NonNull ActivityRecord activity) {
        if (mUnknownApps.isEmpty()) {
            return;
        }
        if (DEBUG_UNKNOWN_APP_VISIBILITY) {
            Slog.d(TAG, "App removed or hidden activity=" + activity);
        }
        mUnknownApps.remove(activity);
    }

    /**
     * Notifies that {@param activity} has been launched behind Keyguard, and we need to wait until
     * it is resumed and relaid out to resolve the visibility.
     */
    void notifyLaunched(@NonNull ActivityRecord activity) {
        if (DEBUG_UNKNOWN_APP_VISIBILITY) {
            Slog.d(TAG, "App launched activity=" + activity);
        }
        // If the activity was started with launchTaskBehind, the lifecycle will goes to paused
        // directly, and the process will pass onResume, so we don't need to waiting resume for it.
        if (!activity.mLaunchTaskBehind) {
            mUnknownApps.put(activity, UNKNOWN_STATE_WAITING_RESUME);
        } else {
            mUnknownApps.put(activity, UNKNOWN_STATE_WAITING_RELAYOUT);
        }
    }

    /**
     * Notifies that {@param activity} has finished resuming.
     */
    void notifyAppResumedFinished(@NonNull ActivityRecord activity) {
        if (mUnknownApps.isEmpty()) {
            return;
        }
        final Integer state = mUnknownApps.get(activity);
        if (state != null && state == UNKNOWN_STATE_WAITING_RESUME) {
            if (DEBUG_UNKNOWN_APP_VISIBILITY) {
                Slog.d(TAG, "App resume finished activity=" + activity);
            }
            mUnknownApps.put(activity, UNKNOWN_STATE_WAITING_RELAYOUT);
        }
    }

    /**
     * Notifies that {@param activity} has relaid out.
     */
    void notifyRelayouted(@NonNull ActivityRecord activity) {
        if (mUnknownApps.isEmpty()) {
            return;
        }
        final Integer state = mUnknownApps.get(activity);
        if (state == null) {
            return;
        }
        if (DEBUG_UNKNOWN_APP_VISIBILITY) {
            Slog.d(TAG, "App relayouted appWindow=" + activity);
        }
        if (state == UNKNOWN_STATE_WAITING_RELAYOUT || activity.mStartingWindow != null) {
            mUnknownApps.put(activity, UNKNOWN_STATE_WAITING_VISIBILITY_UPDATE);
            mDisplayContent.notifyKeyguardFlagsChanged();
            notifyVisibilitiesUpdated();
        } else if (state == UNKNOWN_STATE_WAITING_RESUME
                && !activity.isState(ActivityRecord.State.RESUMED)) {
            Slog.d(TAG, "UAVC: skip waiting for non-resumed relayouted " + activity);
            mUnknownApps.remove(activity);
        }
    }

    private void notifyVisibilitiesUpdated() {
        if (DEBUG_UNKNOWN_APP_VISIBILITY) {
            Slog.d(TAG, "Visibility updated DONE");
        }
        boolean changed = false;
        for (int i = mUnknownApps.size() - 1; i >= 0; i--) {
            if (mUnknownApps.valueAt(i) == UNKNOWN_STATE_WAITING_VISIBILITY_UPDATE) {
                mUnknownApps.removeAt(i);
                changed = true;
            }
        }
        if (changed) {
            mService.mWindowPlacerLocked.performSurfacePlacement();
        }
    }

    void dump(PrintWriter pw, String prefix) {
        if (mUnknownApps.isEmpty()) {
            return;
        }
        pw.println(prefix + "Unknown visibilities:");
        for (int i = mUnknownApps.size() - 1; i >= 0; i--) {
            pw.println(prefix + "  app=" + mUnknownApps.keyAt(i)
                    + " state=" + mUnknownApps.valueAt(i));
        }
    }
}
