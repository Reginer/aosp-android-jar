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

package com.android.server.wm;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED;
import static android.content.pm.ActivityInfo.isFixedOrientation;
import static android.content.pm.ActivityInfo.isFixedOrientationLandscape;
import static android.content.pm.ActivityInfo.isFixedOrientationPortrait;
import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

import static com.android.internal.policy.SystemBarUtils.getDesktopViewAppHeaderHeightPx;
import static com.android.internal.policy.DesktopModeCompatUtils.shouldExcludeCaptionFromAppBounds;
import static com.android.server.wm.LaunchParamsUtil.applyLayoutGravity;
import static com.android.server.wm.LaunchParamsUtil.calculateLayoutBounds;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityOptions;
import android.content.pm.ActivityInfo.WindowLayout;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.SystemProperties;
import android.util.Size;
import android.view.Gravity;
import android.window.DesktopModeFlags;

import java.util.function.Consumer;

/**
 * Calculates the value of the {@link LaunchParamsController.LaunchParams} bounds for the
 * {@link DesktopModeLaunchParamsModifier}.
 */
public final class DesktopModeBoundsCalculator {

    public static final float DESKTOP_MODE_INITIAL_BOUNDS_SCALE = SystemProperties
            .getInt("persist.wm.debug.desktop_mode_initial_bounds_scale", 75) / 100f;
    public static final int DESKTOP_MODE_LANDSCAPE_APP_PADDING = SystemProperties
            .getInt("persist.wm.debug.desktop_mode_landscape_app_padding", 25);

    /**
     * Proportion of window height top offset with respect to bottom offset, used for central task
     * positioning. Should be kept in sync with constant in
     * {@link com.android.wm.shell.desktopmode.DesktopTaskPosition}
     */
    public static final float WINDOW_HEIGHT_PROPORTION = 0.375f;

    /**
     * Updates launch bounds for an activity with respect to its activity options, window layout,
     * android manifest and task configuration.
     */
    static void updateInitialBounds(@NonNull Task task, @Nullable WindowLayout layout,
            @Nullable ActivityRecord activity, @Nullable ActivityOptions options,
            @NonNull LaunchParamsController.LaunchParams outParams,
            @NonNull Consumer<String> logger) {
        // Use stable frame instead of raw frame to avoid launching freeform windows on top of
        // stable insets, which usually are system widgets such as sysbar & navbar.
        final Rect stableBounds = new Rect();
        task.getDisplayArea().getStableRect(stableBounds);

        final boolean hasFullscreenOverride = activity != null
                && activity.mAppCompatController.getAspectRatioOverrides().hasFullscreenOverride();
        // If the options bounds size is flexible and no fullscreen override has been applied,
        // update size with calculated desired size.
        final boolean updateOptionBoundsSize = options != null
                && options.getFlexibleLaunchSize() && !hasFullscreenOverride;
        // If cascading is also enabled, the position of the options bounds must be respected
        // during the size update.
        final boolean shouldRespectOptionPosition =
                updateOptionBoundsSize && DesktopModeFlags.ENABLE_CASCADING_WINDOWS.isTrue();
        final int captionHeight = activity != null && shouldExcludeCaptionFromAppBounds(
                activity.info, task.isResizeable(), activity.mOptOutEdgeToEdge)
                        ? getDesktopViewAppHeaderHeightPx(activity.mWmService.mContext) : 0;

        if (options != null && options.getLaunchBounds() != null
                && !updateOptionBoundsSize) {
            outParams.mBounds.set(options.getLaunchBounds());
            logger.accept("inherit-from-options=" + outParams.mBounds);
        } else if (layout != null) {
            final int verticalGravity = layout.gravity & Gravity.VERTICAL_GRAVITY_MASK;
            final int horizontalGravity = layout.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
            if (layout.hasSpecifiedSize()) {
                calculateLayoutBounds(stableBounds, layout, outParams.mBounds,
                        calculateIdealSize(stableBounds, DESKTOP_MODE_INITIAL_BOUNDS_SCALE));
                applyLayoutGravity(verticalGravity, horizontalGravity, outParams.mBounds,
                        stableBounds);
                logger.accept("layout specifies sizes, inheriting size and applying gravity");
            } else if (verticalGravity > 0 || horizontalGravity > 0) {
                outParams.mBounds.set(calculateInitialBounds(task, activity, stableBounds, options,
                        shouldRespectOptionPosition, captionHeight));
                applyLayoutGravity(verticalGravity, horizontalGravity, outParams.mBounds,
                        stableBounds);
                logger.accept("layout specifies gravity, applying desired bounds and gravity");
                logger.accept("respecting option bounds cascaded position="
                        + shouldRespectOptionPosition);
            }
        } else {
            outParams.mBounds.set(calculateInitialBounds(task, activity, stableBounds, options,
                    shouldRespectOptionPosition, captionHeight));
            logger.accept("layout not specified, applying desired bounds");
            logger.accept("respecting option bounds cascaded position="
                    + shouldRespectOptionPosition);
        }
        if (updateOptionBoundsSize && captionHeight != 0) {
            outParams.mAppBounds.set(outParams.mBounds);
            outParams.mAppBounds.top += captionHeight;
            logger.accept("excluding caption height from app bounds");
        }
    }

    /**
     * Calculates the initial bounds required for an application to fill a scale of the display
     * bounds without any letterboxing. This is done by taking into account the applications
     * fullscreen size, aspect ratio, orientation and resizability to calculate an area that is
     * compatible with the applications previous configuration. These bounds are then cascaded to
     * either the center or to a cascading position supplied by Shell via the option bounds.
     */
    @NonNull
    private static Rect calculateInitialBounds(@NonNull Task task,
            @NonNull ActivityRecord activity, @NonNull Rect stableBounds,
            @Nullable ActivityOptions options, boolean shouldRespectOptionPosition,
            int captionHeight
    ) {
        // Display bounds not taking into account insets.
        final TaskDisplayArea displayArea = task.getDisplayArea();
        final Rect screenBounds = displayArea.getBounds();
        final Size idealSize = calculateIdealSize(screenBounds, DESKTOP_MODE_INITIAL_BOUNDS_SCALE);
        if (!DesktopModeFlags.ENABLE_WINDOWING_DYNAMIC_INITIAL_BOUNDS.isTrue()) {
            return centerInScreen(idealSize, screenBounds);
        }
        if (activity.mAppCompatController.getAspectRatioOverrides()
                .hasFullscreenOverride()) {
            // If the activity has a fullscreen override applied, it should be treated as
            // resizeable and match the device orientation. Thus the ideal size can be
            // applied.
            return centerInScreen(idealSize, screenBounds);
        }
        final DesktopAppCompatAspectRatioPolicy desktopAppCompatAspectRatioPolicy =
                activity.mAppCompatController.getDesktopAspectRatioPolicy();
        final int stableBoundsOrientation = stableBounds.height() >= stableBounds.width()
                ? ORIENTATION_PORTRAIT : ORIENTATION_LANDSCAPE;
        int activityOrientation = getActivityConfigurationOrientation(
                activity, task, stableBoundsOrientation);
        // Use orientation mismatch to resolve aspect ratio to match fixed orientation letterboxing
        // policy in {@link ActivityRecord.resolveFixedOrientationConfiguration}
        final boolean hasOrientationMismatch = stableBoundsOrientation != activityOrientation;
        float appAspectRatio = desktopAppCompatAspectRatioPolicy.calculateAspectRatio(
                task, hasOrientationMismatch);
        final float tdaWidth = stableBounds.width();
        final float tdaHeight = stableBounds.height();
        final Size initialSize = switch (stableBoundsOrientation) {
            case ORIENTATION_LANDSCAPE -> {
                // Device in landscape orientation.
                if (appAspectRatio == 0) {
                    appAspectRatio = 1;
                }
                if (canChangeAspectRatio(desktopAppCompatAspectRatioPolicy, task)) {
                    if (hasOrientationMismatch) {
                        // For portrait resizeable activities, respect apps fullscreen width but
                        // apply ideal size height.
                        yield new Size((int) ((tdaHeight / appAspectRatio) + 0.5f),
                                idealSize.getHeight());
                    }
                    // For landscape resizeable activities, simply apply ideal size.
                    yield idealSize;
                }
                // If activity is unresizeable, regardless of orientation, calculate maximum size
                // (within the ideal size) maintaining original aspect ratio.
                yield maximizeSizeGivenAspectRatio(activityOrientation, idealSize, appAspectRatio,
                        captionHeight);
            }
            case ORIENTATION_PORTRAIT -> {
                // Device in portrait orientation.
                final int customPortraitWidthForLandscapeApp = screenBounds.width()
                        - (DESKTOP_MODE_LANDSCAPE_APP_PADDING * 2);
                if (canChangeAspectRatio(desktopAppCompatAspectRatioPolicy, task)) {
                    if (hasOrientationMismatch) {
                        if (appAspectRatio == 0) {
                            appAspectRatio = tdaWidth / (tdaWidth - 1);
                        }
                        // For landscape resizeable activities, respect apps fullscreen height and
                        // apply custom app width.
                        yield new Size(customPortraitWidthForLandscapeApp,
                                (int) ((tdaWidth / appAspectRatio) + 0.5f));
                    }
                    // For portrait resizeable activities, simply apply ideal size.
                    yield idealSize;
                }
                if (appAspectRatio == 0) {
                    appAspectRatio = 1;
                }
                if (hasOrientationMismatch) {
                    // For landscape unresizeable activities, apply custom app width to ideal size
                    // and calculate maximum size with this area while maintaining original aspect
                    // ratio.
                    yield maximizeSizeGivenAspectRatio(activityOrientation,
                            new Size(customPortraitWidthForLandscapeApp, idealSize.getHeight()),
                            appAspectRatio, captionHeight);
                }
                // For portrait unresizeable activities, calculate maximum size (within the ideal
                // size) maintaining original aspect ratio.
                yield maximizeSizeGivenAspectRatio(activityOrientation, idealSize, appAspectRatio,
                        captionHeight);
            }
            default -> idealSize;
        };
        if (shouldRespectOptionPosition) {
            return respectShellCascading(initialSize, stableBounds, options.getLaunchBounds());
        }
        return centerInScreen(initialSize, screenBounds);
    }

    /**
     * Whether the activity's aspect ratio can be changed or if it should be maintained as if it was
     * unresizeable.
     */
    private static boolean canChangeAspectRatio(
            @NonNull DesktopAppCompatAspectRatioPolicy desktopAppCompatAspectRatioPolicy,
            @NonNull Task task) {
        return task.isResizeable()
                && !desktopAppCompatAspectRatioPolicy.hasMinAspectRatioOverride(task);
    }

    private static @Configuration.Orientation int getActivityConfigurationOrientation(
            @NonNull ActivityRecord activity, @NonNull Task task,
            @Configuration.Orientation int stableBoundsOrientation) {
        final int activityOrientation = activity.getOverrideOrientation();
        final DesktopAppCompatAspectRatioPolicy desktopAppCompatAspectRatioPolicy =
                activity.mAppCompatController.getDesktopAspectRatioPolicy();
        if ((desktopAppCompatAspectRatioPolicy.shouldApplyUserMinAspectRatioOverride(task)
                && (!isFixedOrientation(activityOrientation)
                    || activityOrientation == SCREEN_ORIENTATION_LOCKED))
                || isFixedOrientationPortrait(activityOrientation)) {
            // If a user aspect ratio override should be applied, treat the activity as portrait if
            // it has not specified a fix orientation.
            return ORIENTATION_PORTRAIT;
        }
        // If activity orientation is undefined inherit task orientation.
        return isFixedOrientationLandscape(activityOrientation)
                ?  ORIENTATION_LANDSCAPE : stableBoundsOrientation;
    }

    /**
     * Calculates the largest size that can fit in a given area while maintaining a specific aspect
     * ratio.
     */
    // TODO(b/400617906): Merge duplicate initial bounds calculations to shared class.
    @NonNull
    private static Size maximizeSizeGivenAspectRatio(
            @Configuration.Orientation int orientation,
            @NonNull Size targetArea,
            float aspectRatio,
            int captionHeight
    ) {
        final int targetHeight = targetArea.getHeight() - captionHeight;
        final int targetWidth = targetArea.getWidth();
        final int finalHeight;
        final int finalWidth;
        if (orientation == ORIENTATION_PORTRAIT) {
            // Portrait activity.
            // Calculate required width given ideal height and aspect ratio.
            int tempWidth = (int) (targetHeight / aspectRatio);
            if (tempWidth <= targetWidth) {
                // If the calculated width does not exceed the ideal width, overall size is within
                // ideal size and can be applied.
                finalHeight = targetHeight;
                finalWidth = tempWidth;
            } else {
                // Applying target height cause overall size to exceed ideal size when maintain
                // aspect ratio. Instead apply ideal width and calculate required height to respect
                // aspect ratio.
                finalWidth = targetWidth;
                finalHeight = (int) (finalWidth * aspectRatio);
            }
        } else {
            // Landscape activity.
            // Calculate required width given ideal height and aspect ratio.
            int tempWidth = (int) (targetHeight * aspectRatio);
            if (tempWidth <= targetWidth) {
                // If the calculated width does not exceed the ideal width, overall size is within
                // ideal size and can be applied.
                finalHeight = targetHeight;
                finalWidth = tempWidth;
            } else {
                // Applying target height cause overall size to exceed ideal size when maintain
                // aspect ratio. Instead apply ideal width and calculate required height to respect
                // aspect ratio.
                finalWidth = targetWidth;
                finalHeight = (int) (finalWidth / aspectRatio);
            }
        }
        return new Size(finalWidth, finalHeight + captionHeight);
    }

    /**
     * Calculates the desired initial bounds for applications in desktop windowing. This is done as
     * a scale of the screen bounds.
     */
    @NonNull
    private static Size calculateIdealSize(@NonNull Rect screenBounds, float scale) {
        final int width = (int) (screenBounds.width() * scale);
        final int height = (int) (screenBounds.height() * scale);
        return new Size(width, height);
    }

    /**
     * Adjusts bounds to be positioned in the middle of the screen.
     */
    @NonNull
    static Rect centerInScreen(@NonNull Size desiredSize,
            @NonNull Rect screenBounds) {
        final int heightOffset = (int)
                ((screenBounds.height() - desiredSize.getHeight()) * WINDOW_HEIGHT_PROPORTION);
        final int widthOffset = (screenBounds.width() - desiredSize.getWidth()) / 2;
        final Rect resultBounds = new Rect(0, 0,
                desiredSize.getWidth(), desiredSize.getHeight());
        resultBounds.offset(screenBounds.left + widthOffset, screenBounds.top + heightOffset);
        return resultBounds;
    }

    /**
     * Calculates final initial bounds based on the task's desired size and the cascading anchor
     * point extracted from the option bounds. Cascading behaviour should be kept in sync with
     * logic in {@link com.android.wm.shell.desktopmode.DesktopTaskPosition}.
     */
    @NonNull
    private static Rect respectShellCascading(@NonNull Size desiredSize, @NonNull Rect stableBounds,
            @NonNull Rect optionBounds) {
        final boolean leftAligned = stableBounds.left == optionBounds.left;
        final boolean rightAligned = stableBounds.right == optionBounds.right;
        final boolean topAligned = stableBounds.top == optionBounds.top;
        final boolean bottomAligned = stableBounds.bottom == optionBounds.bottom;
        if (leftAligned && topAligned) {
            // Bounds cascaded to top left, respect top left position anchor point.
            return new Rect(
                    optionBounds.left,
                    optionBounds.top,
                    optionBounds.left + desiredSize.getWidth(),
                    optionBounds.top + desiredSize.getHeight()
            );
        }
        if (leftAligned && bottomAligned) {
            // Bounds cascaded to bottom left, respect bottom left position anchor point.
            return new Rect(
                    optionBounds.left,
                    optionBounds.bottom - desiredSize.getHeight(),
                    optionBounds.left + desiredSize.getWidth(),
                    optionBounds.bottom
            );
        }
        if (rightAligned && topAligned) {
            // Bounds cascaded to top right, respect top right position anchor point.
            return new Rect(
                    optionBounds.right - desiredSize.getWidth(),
                    optionBounds.top,
                    optionBounds.right,
                    optionBounds.top + desiredSize.getHeight()
            );
        }
        if (rightAligned && bottomAligned) {
            // Bounds cascaded to bottom right, respect bottom right position anchor point.
            return new Rect(
                    optionBounds.right - desiredSize.getWidth(),
                    optionBounds.bottom - desiredSize.getHeight(),
                    optionBounds.right,
                    optionBounds.bottom);
        }
        // Bounds not cascaded to any corner, default to center position.
        return centerInScreen(desiredSize, stableBounds);
    }
}
