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

package com.android.car.setupwizardlib.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;
import android.view.View;
import android.view.Window;

import androidx.core.util.Preconditions;

/**
 * Utilities to aid in UI for car setup wizard flow.
 */
public final class CarSetupWizardUiUtils {
    private static final String TAG = CarSetupWizardUiUtils.class.getSimpleName();

    /** Key for immersive mode value passed to 1P apps */
    public static final String IMMERSIVE_MODE_TYPE = "immersiveModeType";

    /** Key indicating whether 1P apps should switch to the new landscape design */
    public static final String EXTRA_NEW_LANDSCAPE_LAYOUT_SUPPORTED =
            "extra_new_landscape_layout_supported";

    /* Setup Wizard Package Name **/
    public static final String SETUP_WIZARD_PACKAGE = "com.google.android.car.setupwizard";

    /** Hide system UI */
    public static void hideSystemUI(Activity activity) {
        maybeHideSystemUI(activity);
    }

    /** Hide system UI
     * @deprecated Use {@code hideSystemUI}
     **/
    @Deprecated
    public static void maybeHideSystemUI(Activity activity) {
        enableImmersiveMode(activity.getWindow());
    }

    /**
    * Set the appropriate immersive mode according to immersiveModeValue
    */
    public static void setWindowImmersiveMode(Window window, String immersiveModeValue) {
        Preconditions.checkNotNull(window);
        ImmersiveModeTypes immersiveModeType;
        try {
            immersiveModeType = ImmersiveModeTypes.valueOf(immersiveModeValue);
        } catch (IllegalArgumentException | NullPointerException e) {
            Log.w(TAG, "Immersive Mode value: " + immersiveModeValue
                    + " not valid, using IMMERSIVE");
            immersiveModeType = ImmersiveModeTypes.IMMERSIVE;
        }
        Log.v(TAG, "Enable " + immersiveModeType + " mode");
        switch (immersiveModeType) {
            case IMMERSIVE:
                enableImmersiveMode(window);
                window.getDecorView().setOnSystemUiVisibilityChangeListener(
                        visibility -> enableImmersiveMode(window));
                break;
            case IMMERSIVE_WITH_STATUS:
                enableImmersiveModeWithStatus(window);
                window.getDecorView().setOnSystemUiVisibilityChangeListener(
                        visibility -> enableImmersiveModeWithStatus(window));
                break;
            case NON_IMMERSIVE:
                disableImmersiveMode(window);
                window.getDecorView().setOnSystemUiVisibilityChangeListener(
                        visibility -> disableImmersiveMode(window));
                break;
            case SYSTEM_DEFAULT:
                //SUW won't change the current immersive mode.
                break;
        }
    }

    /**
     * Enables immersive mode hiding system UI.
     *
     * @param window to apply immersive mode.
     *
     * @deprecated Use {@code setWindowImmersiveMode}
     */
    @Deprecated
    public static void enableImmersiveMode(Window window) {
        if (Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, "enableImmersiveMode");
        }

        Preconditions.checkNotNull(window);

        // See https://developer.android.com/training/system-ui/immersive#EnableFullscreen
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    /**
     * Enables immersive mode hiding only navigation bar.
     *
     * @param window to apply immersive mode.
     */
    private static void enableImmersiveModeWithStatus(Window window) {
        if (Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, "enableImmersiveModeWithStatus");
        }

        Preconditions.checkNotNull(window);

        // See https://developer.android.com/training/system-ui/immersive#EnableFullscreen
        // Enables regular immersive mode.
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        // Hide the nav bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    /**
     * Disables immersive mode hiding system UI and restores the previous colors.
     *
     * @param window the current window instance.
     *
     * @deprecated Use {@code setWindowImmersiveMode}
     */
    @Deprecated
    public static void disableImmersiveMode(Window window) {
        if (Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, "disableImmersiveMode");
        }

        Preconditions.checkNotNull(window);

        // Restores the decor view flags to disable the immersive mode.
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        // Tries to restore colors for nav and status bar from resources.
        Context context = window.getContext();
        if (context == null) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Can't restore colors for navigation and status bar.");
            }
            return;
        }

        // Reads the colors for navigation and status bar from resources.
        final TypedArray typedArray =
                context.obtainStyledAttributes(
                        new int[]{
                                android.R.attr.statusBarColor,
                                android.R.attr.navigationBarColor});
        int statusBarColor = typedArray.getColor(0, 0);
        int navigationBarColor = typedArray.getColor(1, 0);

        window.setStatusBarColor(statusBarColor);
        window.setNavigationBarColor(navigationBarColor);

        typedArray.recycle();
    }

    /** Types of Immersive Mode supported by SUW */
    public enum ImmersiveModeTypes {
        IMMERSIVE,
        IMMERSIVE_WITH_STATUS,
        NON_IMMERSIVE,
        SYSTEM_DEFAULT
    }

    private CarSetupWizardUiUtils() {
    }
}
