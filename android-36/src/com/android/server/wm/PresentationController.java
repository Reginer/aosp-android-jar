/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static android.view.WindowManager.LayoutParams.TYPE_PRESENTATION;
import static android.view.WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION;

import static com.android.internal.protolog.WmProtoLogGroups.WM_ERROR;
import static com.android.window.flags.Flags.enablePresentationForConnectedDisplays;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.hardware.display.DisplayManager;
import android.util.SparseArray;
import android.view.WindowManager.LayoutParams.WindowType;

import com.android.internal.protolog.ProtoLog;
import com.android.internal.protolog.WmProtoLogGroups;

/**
 * Manages presentation windows.
 */
class PresentationController implements DisplayManager.DisplayListener {

    private static class Presentation {
        @NonNull final WindowState mWin;
        @NonNull final WindowContainerListener mPresentationListener;
        // This is the task which started this presentation. This shouldn't be null in most cases
        // because the intended usage of the Presentation API is that an activity that started a
        // presentation should control the UI and lifecycle of the presentation window.
        // However, the API doesn't necessarily requires a host activity to exist (e.g. a background
        // service can launch a presentation), so this can be null.
        @Nullable final Task mHostTask;
        @Nullable final WindowContainerListener mHostTaskListener;

        Presentation(@NonNull WindowState win,
                @NonNull WindowContainerListener presentationListener,
                @Nullable Task hostTask,
                @Nullable WindowContainerListener hostTaskListener) {
            mWin = win;
            mPresentationListener = presentationListener;
            mHostTask = hostTask;
            mHostTaskListener = hostTaskListener;
        }

        @Override
        public String toString() {
            return "{win: " + mWin.getName() + ", display: " + mWin.getDisplayId()
                    + ", hostTask: " + (mHostTask != null ? mHostTask.getName() : null) + "}";
        }
    }

    private final SparseArray<Presentation> mPresentations = new SparseArray();

    @Nullable
    private Presentation getPresentation(@Nullable WindowState win) {
        if (win == null) return null;
        for (int i = 0; i < mPresentations.size(); i++) {
            final Presentation presentation = mPresentations.valueAt(i);
            if (win == presentation.mWin) return presentation;
        }
        return null;
    }

    private boolean hasPresentationWindow(int displayId) {
        return mPresentations.contains(displayId);
    }

    boolean isPresentationVisible(int displayId) {
        final Presentation presentation = mPresentations.get(displayId);
        return presentation != null && presentation.mWin.mToken.isVisibleRequested();
    }

    boolean canPresent(@NonNull WindowState win, @NonNull DisplayContent displayContent) {
        return canPresent(win, displayContent, win.mAttrs.type, win.getUid());
    }

    /**
     * Checks if a presentation window can be shown on the given display.
     * If the given |win| is empty, a new presentation window is being created.
     * If the given |win| is not empty, the window already exists as presentation, and we're
     * revalidate if the |win| is still qualified to be shown.
     */
    boolean canPresent(@Nullable WindowState win, @NonNull DisplayContent displayContent,
            @WindowType int type, int uid) {
        if (type == TYPE_PRIVATE_PRESENTATION) {
            // Private presentations can only be created on private displays.
            return displayContent.isPrivate();
        }

        if (type != TYPE_PRESENTATION) {
            return false;
        }

        if (!enablePresentationForConnectedDisplays()) {
            return displayContent.getDisplay().isPublicPresentation();
        }

        boolean allDisplaysArePresenting = true;
        for (int i = 0; i < displayContent.mWmService.mRoot.mChildren.size(); i++) {
            final DisplayContent dc = displayContent.mWmService.mRoot.mChildren.get(i);
            if (displayContent.mDisplayId != dc.mDisplayId
                    && !mPresentations.contains(dc.mDisplayId)) {
                allDisplaysArePresenting = false;
                break;
            }
        }
        if (allDisplaysArePresenting) {
            // All displays can't present simultaneously.
            return false;
        }

        final int displayId = displayContent.mDisplayId;
        if (hasPresentationWindow(displayId)
                && win != null && win != mPresentations.get(displayId).mWin) {
            // A display can't have multiple presentations.
            return false;
        }

        Task hostTask = null;
        final Presentation presentation = getPresentation(win);
        if (presentation != null) {
            hostTask = presentation.mHostTask;
        } else if (win == null) {
            final Task globallyFocusedTask =
                    displayContent.mWmService.mRoot.getTopDisplayFocusedRootTask();
            if (globallyFocusedTask != null && uid == globallyFocusedTask.effectiveUid) {
                hostTask = globallyFocusedTask;
            }
        }
        if (hostTask != null && displayId == hostTask.getDisplayId()) {
            // A presentation can't cover its own host task.
            return false;
        }
        if (hostTask == null && !displayContent.getDisplay().isPublicPresentation()) {
            // A globally focused host task on a different display is needed to show a
            // presentation on a non-presenting display.
            return false;
        }

        return true;
    }

    boolean shouldOccludeActivities(int displayId) {
        // All activities on the presenting display must be hidden so that malicious apps can't do
        // tap jacking (b/391466268).
        // For now, this should only be applied to external displays because presentations can only
        // be shown on them.
        // TODO(b/390481621): Disallow a presentation from covering its controlling activity so that
        // the presentation won't stop its controlling activity.
        return enablePresentationForConnectedDisplays() && isPresentationVisible(displayId);
    }

    void onPresentationAdded(@NonNull WindowState win, int uid) {
        final int displayId = win.getDisplayId();
        ProtoLog.v(WmProtoLogGroups.WM_DEBUG_PRESENTATION, "Presentation added to display %d: %s",
                displayId, win);
        win.mWmService.mDisplayManagerInternal.onPresentation(displayId, /*isShown=*/ true);

        final WindowContainerListener presentationWindowListener = new WindowContainerListener() {
            @Override
            public void onRemoved() {
                if (!hasPresentationWindow(displayId)) {
                    ProtoLog.e(WM_ERROR, "Failed to remove presentation on"
                            + "non-presenting display %d: %s", displayId, win);
                    return;
                }
                final Presentation presentation = mPresentations.get(displayId);
                win.mToken.unregisterWindowContainerListener(presentation.mPresentationListener);
                if (presentation.mHostTask != null) {
                    presentation.mHostTask.unregisterWindowContainerListener(
                            presentation.mHostTaskListener);
                }
                mPresentations.remove(displayId);
                win.mWmService.mDisplayManagerInternal.onPresentation(displayId, false /*isShown*/);
            }
        };
        win.mToken.registerWindowContainerListener(presentationWindowListener);

        Task hostTask = null;
        if (enablePresentationForConnectedDisplays()) {
            final Task globallyFocusedTask =
                    win.mWmService.mRoot.getTopDisplayFocusedRootTask();
            if (globallyFocusedTask != null && uid == globallyFocusedTask.effectiveUid) {
                hostTask = globallyFocusedTask;
            }
        }
        WindowContainerListener hostTaskListener = null;
        if (hostTask != null) {
            hostTaskListener = new WindowContainerListener() {
                public void onDisplayChanged(DisplayContent dc) {
                    final Presentation presentation = mPresentations.get(dc.getDisplayId());
                    if (presentation != null && !canPresent(presentation.mWin, dc)) {
                        removePresentation(dc.mDisplayId, "host task moved to display "
                                + dc.getDisplayId());
                    }
                }

                public void onRemoved() {
                    removePresentation(win.getDisplayId(), "host task removed");
                }
            };
            hostTask.registerWindowContainerListener(hostTaskListener);
        }

        mPresentations.put(displayId, new Presentation(win, presentationWindowListener, hostTask,
                hostTaskListener));
    }

    void removePresentation(int displayId, @NonNull String reason) {
        final Presentation presentation = mPresentations.get(displayId);
        if (enablePresentationForConnectedDisplays() && presentation != null) {
            ProtoLog.v(WmProtoLogGroups.WM_DEBUG_PRESENTATION, "Removing Presentation %s for "
                    + "reason %s", mPresentations.get(displayId), reason);
            final WindowState win = presentation.mWin;
            win.mWmService.mAtmService.mH.post(() -> {
                synchronized (win.mWmService.mGlobalLock) {
                    win.removeIfPossible();
                }
            });
        }
    }

    @Override
    public void onDisplayAdded(int displayId) {}

    @Override
    public void onDisplayRemoved(int displayId) {
        removePresentation(displayId, "display removed " + displayId);
    }

    @Override
    public void onDisplayChanged(int displayId) {}
}
