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

package com.android.server.wallpaper;

import static android.app.WallpaperManager.ORIENTATION_UNKNOWN;
import static android.app.WallpaperManager.getRotatedOrientation;
import static android.view.Display.DEFAULT_DISPLAY;
import static android.view.WindowManager.LARGE_SCREEN_SMALLEST_SCREEN_WIDTH_DP;

import android.app.WallpaperManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.SparseArray;
import android.view.WindowManager;
import android.view.WindowMetrics;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**  A data class for the default display attributes used in wallpaper related operations. */
public final class WallpaperDefaultDisplayInfo {
    /**
     * A data class representing the screen orientations for a foldable device in the folded and
     * unfolded states.
     */
    @VisibleForTesting
    static final class FoldableOrientations {
        @WallpaperManager.ScreenOrientation
        public final int foldedOrientation;
        @WallpaperManager.ScreenOrientation
        public final int unfoldedOrientation;

        FoldableOrientations(int foldedOrientation, int unfoldedOrientation) {
            this.foldedOrientation = foldedOrientation;
            this.unfoldedOrientation = unfoldedOrientation;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof FoldableOrientations that)) return false;
            return foldedOrientation == that.foldedOrientation
                    && unfoldedOrientation == that.unfoldedOrientation;
        }

        @Override
        public int hashCode() {
            return Objects.hash(foldedOrientation, unfoldedOrientation);
        }
    }

    public final SparseArray<Point> defaultDisplaySizes;
    public final boolean isLargeScreen;
    public final boolean isFoldable;
    @VisibleForTesting
    final List<FoldableOrientations> foldableOrientations;

    public WallpaperDefaultDisplayInfo() {
        this.defaultDisplaySizes = new SparseArray<>();
        this.isLargeScreen = false;
        this.isFoldable = false;
        this.foldableOrientations = Collections.emptyList();
    }

    public WallpaperDefaultDisplayInfo(WindowManager windowManager, Resources resources) {
        Set<WindowMetrics> metrics = windowManager.getPossibleMaximumWindowMetrics(DEFAULT_DISPLAY);
        boolean isFoldable = resources.getIntArray(R.array.config_foldedDeviceStates).length > 0;
        if (isFoldable) {
            this.foldableOrientations = getFoldableOrientations(metrics);
        } else {
            this.foldableOrientations = Collections.emptyList();
        }
        this.defaultDisplaySizes = getDisplaySizes(metrics);
        this.isLargeScreen = isLargeScreen(metrics);
        this.isFoldable = isFoldable;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof WallpaperDefaultDisplayInfo that)) return false;
        return isLargeScreen == that.isLargeScreen && isFoldable == that.isFoldable
                && defaultDisplaySizes.contentEquals(that.defaultDisplaySizes)
                && Objects.equals(foldableOrientations, that.foldableOrientations);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(isLargeScreen, isFoldable, foldableOrientations)
                + defaultDisplaySizes.contentHashCode();
    }

    /**
     * Returns the folded orientation corresponds to the {@code unfoldedOrientation} found in
     * {@link #foldableOrientations}. If not found, returns
     * {@link WallpaperManager.ORIENTATION_UNKNOWN}.
     */
    public int getFoldedOrientation(int unfoldedOrientation) {
        for (FoldableOrientations orientations : foldableOrientations) {
            if (orientations.unfoldedOrientation == unfoldedOrientation) {
                return orientations.foldedOrientation;
            }
        }
        return ORIENTATION_UNKNOWN;
    }

    /**
     * Returns the unfolded orientation corresponds to the {@code foldedOrientation} found in
     * {@link #foldableOrientations}. If not found, returns
     * {@link WallpaperManager.ORIENTATION_UNKNOWN}.
     */
    public int getUnfoldedOrientation(int foldedOrientation) {
        for (FoldableOrientations orientations : foldableOrientations) {
            if (orientations.foldedOrientation == foldedOrientation) {
                return orientations.unfoldedOrientation;
            }
        }
        return ORIENTATION_UNKNOWN;
    }

    private static SparseArray<Point> getDisplaySizes(Set<WindowMetrics> displayMetrics) {
        SparseArray<Point> displaySizes = new SparseArray<>();
        for (WindowMetrics metric : displayMetrics) {
            Rect bounds = metric.getBounds();
            Point displaySize = new Point(bounds.width(), bounds.height());
            Point reversedDisplaySize = new Point(displaySize.y, displaySize.x);
            for (Point point : List.of(displaySize, reversedDisplaySize)) {
                int orientation = WallpaperManager.getOrientation(point);
                // don't add an entry if there is already a larger display of the same orientation
                Point display = displaySizes.get(orientation);
                if (display == null || display.x * display.y < point.x * point.y) {
                    displaySizes.put(orientation, point);
                }
            }
        }
        return displaySizes;
    }

    private static boolean isLargeScreen(Set<WindowMetrics> displayMetrics) {
        float smallestWidth = Float.MAX_VALUE;
        for (WindowMetrics metric : displayMetrics) {
            Rect bounds = metric.getBounds();
            smallestWidth = Math.min(smallestWidth, bounds.width() / metric.getDensity());
        }
        return smallestWidth >= LARGE_SCREEN_SMALLEST_SCREEN_WIDTH_DP;
    }

    /**
     * Determines all potential foldable orientations, populating {@code
     * outFoldableOrientationPairs} with pairs of (folded orientation, unfolded orientation). If
     * {@code defaultDisplayMetrics} isn't for foldable, {@code outFoldableOrientationPairs} will
     * not be populated.
     */
    private static List<FoldableOrientations> getFoldableOrientations(
            Set<WindowMetrics> defaultDisplayMetrics) {
        if (defaultDisplayMetrics.size() != 2) {
            return Collections.emptyList();
        }
        List<FoldableOrientations> foldableOrientations = new ArrayList<>();
        float surface = 0;
        int firstOrientation = -1;
        for (WindowMetrics metric : defaultDisplayMetrics) {
            Rect bounds = metric.getBounds();
            Point displaySize = new Point(bounds.width(), bounds.height());

            int orientation = WallpaperManager.getOrientation(displaySize);
            float newSurface = displaySize.x * displaySize.y
                    / (metric.getDensity() * metric.getDensity());
            if (surface <= 0) {
                surface = newSurface;
                firstOrientation = orientation;
            } else {
                FoldableOrientations orientations = (newSurface > surface)
                        ? new FoldableOrientations(firstOrientation, orientation)
                        : new FoldableOrientations(orientation, firstOrientation);
                FoldableOrientations rotatedOrientations = new FoldableOrientations(
                        getRotatedOrientation(orientations.foldedOrientation),
                        getRotatedOrientation(orientations.unfoldedOrientation));
                foldableOrientations.add(orientations);
                foldableOrientations.add(rotatedOrientations);
            }
        }
        return Collections.unmodifiableList(foldableOrientations);
    }
}
