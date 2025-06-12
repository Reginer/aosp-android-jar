/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.server.display.mode;

import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.util.LongSparseArray;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayAddress;
import android.view.DisplayEventReceiver;

import androidx.annotation.VisibleForTesting;

import java.util.HashSet;
import java.util.Set;

final class ModeChangeObserver {
    private static final String TAG = "ModeChangeObserver";

    private final VotesStorage mVotesStorage;
    private final DisplayModeDirector.Injector mInjector;

    @SuppressWarnings("unused")
    @VisibleForTesting
    DisplayEventReceiver mModeChangeListener;
    @VisibleForTesting
    DisplayManager.DisplayListener mDisplayListener;
    private final LongSparseArray<Set<Integer>> mRejectedModesMap =
            new LongSparseArray<>();
    private final LongSparseArray<Integer> mPhysicalIdToLogicalIdMap = new LongSparseArray<>();
    private final Looper mLooper;
    private final Handler mHandler;

    /**
     * Observer for display mode changes.
     * This class observes display mode rejections and updates the vote storage
     * for rejected modes vote accordingly.
     */
    ModeChangeObserver(VotesStorage votesStorage, DisplayModeDirector.Injector injector,
                    Looper looper) {
        mVotesStorage = votesStorage;
        mInjector = injector;
        mLooper = looper;
        mHandler = new Handler(mLooper);
    }

    /**
     * Start observing display mode changes.
     */
    void observe() {
        updatePhysicalIdToLogicalIdMap();
        mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
                updateVoteForDisplay(displayId);
            }

            @Override
            public void onDisplayRemoved(int displayId) {
                int oldPhysicalDisplayIdIndex = mPhysicalIdToLogicalIdMap.indexOfValue(displayId);
                if (oldPhysicalDisplayIdIndex < 0) {
                    Slog.e(TAG, "Removed display not found");
                    return;
                }
                long oldPhysicalDisplayId =
                        mPhysicalIdToLogicalIdMap.keyAt(oldPhysicalDisplayIdIndex);
                mPhysicalIdToLogicalIdMap.delete(oldPhysicalDisplayId);
                mRejectedModesMap.delete(oldPhysicalDisplayId);
                mVotesStorage.updateVote(displayId, Vote.PRIORITY_REJECTED_MODES, null);
            }

            @Override
            public void onDisplayChanged(int displayId) {
                int oldPhysicalDisplayIdIndex = mPhysicalIdToLogicalIdMap.indexOfValue(displayId);
                if (oldPhysicalDisplayIdIndex < 0) {
                    Slog.e(TAG, "Changed display not found");
                    return;
                }
                long oldPhysicalDisplayId =
                        mPhysicalIdToLogicalIdMap.keyAt(oldPhysicalDisplayIdIndex);
                mPhysicalIdToLogicalIdMap.delete(oldPhysicalDisplayId);

                updateVoteForDisplay(displayId);
            }
        };
        mInjector.registerDisplayListener(mDisplayListener, mHandler,
                    DisplayManager.EVENT_TYPE_DISPLAY_ADDED
                            | DisplayManager.EVENT_TYPE_DISPLAY_CHANGED
                            | DisplayManager.EVENT_TYPE_DISPLAY_REMOVED);
        mModeChangeListener = new DisplayEventReceiver(mLooper) {
            @Override
            public void onModeRejected(long physicalDisplayId, int modeId) {
                Slog.d(TAG, "Mode Rejected event received");
                updateRejectedModesListByDisplay(physicalDisplayId, modeId);
                if (mPhysicalIdToLogicalIdMap.indexOfKey(physicalDisplayId) < 0) {
                    Slog.d(TAG, "Rejected Modes Vote will be updated after display is added");
                    return;
                }
                mVotesStorage.updateVote(mPhysicalIdToLogicalIdMap.get(physicalDisplayId),
                        Vote.PRIORITY_REJECTED_MODES,
                        Vote.forRejectedModes(mRejectedModesMap.get(physicalDisplayId)));
            }
        };
    }

    private void updateVoteForDisplay(int displayId) {
        Display display = mInjector.getDisplay(displayId);
        if (display == null) {
            // We can occasionally get a display added or changed event for a display that was
            // subsequently removed, which means this returns null. Check this case and bail
            // out early; if it gets re-attached we will eventually get another call back for it.
            Slog.e(TAG, "Added or Changed display has disappeared");
            return;
        }
        DisplayAddress address = display.getAddress();
        if (address instanceof DisplayAddress.Physical physical) {
            long physicalDisplayId = physical.getPhysicalDisplayId();
            mPhysicalIdToLogicalIdMap.put(physicalDisplayId, displayId);
            Set<Integer> modes = mRejectedModesMap.get(physicalDisplayId);
            mVotesStorage.updateVote(displayId, Vote.PRIORITY_REJECTED_MODES,
                    modes != null ? Vote.forRejectedModes(modes) : null);
        }
    }

    private void updatePhysicalIdToLogicalIdMap() {
        Display[] displays = mInjector.getDisplays();

        for (Display display : displays) {
            if (display == null) {
                continue;
            }
            DisplayAddress address = display.getAddress();
            if (address instanceof DisplayAddress.Physical physical) {
                mPhysicalIdToLogicalIdMap.put(physical.getPhysicalDisplayId(),
                        display.getDisplayId());
            }
        }
    }

    private void updateRejectedModesListByDisplay(long rejectedModePhysicalDisplayId,
                                                    int rejectedModeId) {
        Set<Integer> alreadyRejectedModes =
                mRejectedModesMap.get(rejectedModePhysicalDisplayId);
        if (alreadyRejectedModes == null) {
            alreadyRejectedModes = new HashSet<>();
            mRejectedModesMap.put(rejectedModePhysicalDisplayId,
                    alreadyRejectedModes);
        }
        alreadyRejectedModes.add(rejectedModeId);
    }
}
