/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.server.wm.utils;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.graphics.Rect;
import android.view.Surface;


/**
 * Utility methods to handle insets represented as rects.
 */
public class InsetUtils {

    private InsetUtils() {
    }

    /**
     * Transforms insets given in one rotation into insets in a different rotation.
     *
     * @param inOutInsets the insets to transform, is set to the transformed insets
     * @param rotationDelta the delta between the new and old rotation.
     *                      Must be one of Surface.ROTATION_0/90/180/270.
     */
    public static void rotateInsets(Rect inOutInsets, int rotationDelta) {
        final Rect r = inOutInsets;
        switch (rotationDelta) {
            case Surface.ROTATION_0:
                return;
            case Surface.ROTATION_90:
                r.set(r.top, r.right, r.bottom, r.left);
                break;
            case Surface.ROTATION_180:
                r.set(r.right, r.bottom, r.left, r.top);
                break;
            case Surface.ROTATION_270:
                r.set(r.bottom, r.left, r.top, r.right);
                break;
            default:
                throw new IllegalArgumentException("Unknown rotation: " + rotationDelta);
        }
    }

    /**
     * Adds {@code insetsToAdd} to {@code inOutInsets}.
     */
    public static void addInsets(Rect inOutInsets, Rect insetsToAdd) {
        inOutInsets.left += insetsToAdd.left;
        inOutInsets.top += insetsToAdd.top;
        inOutInsets.right += insetsToAdd.right;
        inOutInsets.bottom += insetsToAdd.bottom;
    }

    /**
     * Calculates the insets from the {@code outerFrame} to the {@code innerFrame}.
     *
     * Note that if a side of the outer frame is not actually on the outside, the inset for that
     * side will be clamped to zero.
     *
     * @param outerFrame the reference frame from which the insets are calculated
     * @param innerFrame the inset frame, to which the insets are calculated,
     *                   or null to clear the insets.
     * @param outInsets is set to the result of the inset calculation.
     */
    public static void insetsBetweenFrames(@NonNull Rect outerFrame, @Nullable Rect innerFrame,
            @NonNull Rect outInsets) {
        if (innerFrame == null) {
            outInsets.setEmpty();
            return;
        }
        final int w = outerFrame.width();
        final int h = outerFrame.height();
        outInsets.set(
                Math.min(w, Math.max(0, innerFrame.left - outerFrame.left)),
                Math.min(h, Math.max(0, innerFrame.top - outerFrame.top)),
                Math.min(w, Math.max(0, outerFrame.right - innerFrame.right)),
                Math.min(h, Math.max(0, outerFrame.bottom - innerFrame.bottom)));
    }
}
