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

package android.view;

import android.annotation.FlaggedApi;

/**
 * Class for XR-specific window properties to put in application manifests.
 */
@FlaggedApi(android.xr.Flags.FLAG_XR_MANIFEST_ENTRIES)
public final class XrWindowProperties {
    /** @hide */
    private XrWindowProperties() {}

    /**
     * Application and Activity level
     * {@link android.content.pm.PackageManager.Property PackageManager.Property} for an app to
     * inform the system of the activity launch mode in XR. When it is declared at the application
     * level, all activities are set to the defined value, unless it is overridden at the activity
     * level.
     *
     * <p>The default value is {@link #XR_ACTIVITY_START_MODE_UNDEFINED}.
     *
     * <p>The available values are:
     * <ul>
     *   <li>{@link #XR_ACTIVITY_START_MODE_FULL_SPACE_UNMANAGED}
     *   <li>{@link #XR_ACTIVITY_START_MODE_FULL_SPACE_MANAGED}
     *   <li>{@link #XR_ACTIVITY_START_MODE_HOME_SPACE}
     *   <li>{@link #XR_ACTIVITY_START_MODE_UNDEFINED}
     * </ul>
     *
     * <p><b>Syntax:</b>
     * <pre>
     * &lt;application&gt;
     *   &lt;property
     *     android:name="android.window.PROPERTY_ACTIVITY_XR_START_MODE"
     *     android:value="XR_ACTIVITY_START_MODE_FULL_SPACE_UNMANAGED|
     *                    XR_ACTIVITY_START_MODE_FULL_SPACE_MANAGED|
     *                    XR_ACTIVITY_START_MODE_HOME_SPACE|
     *                    XR_ACTIVITY_START_MODE_UNDEFINED"/&gt;
     * &lt;/application&gt;
     * </pre>
     */
    @FlaggedApi(android.xr.Flags.FLAG_XR_MANIFEST_ENTRIES)
    public static final String PROPERTY_XR_ACTIVITY_START_MODE =
            "android.window.PROPERTY_XR_ACTIVITY_START_MODE";

    /**
     * Defines the value to launch an activity in unmanaged full space mode in XR, where the
     * activity itself is rendering the space and controls its own scene graph. This should be used
     * for all activities that use OpenXR to render.
     *
     * @see #PROPERTY_XR_ACTIVITY_START_MODE
     */
    @FlaggedApi(android.xr.Flags.FLAG_XR_MANIFEST_ENTRIES)
    public static final String XR_ACTIVITY_START_MODE_FULL_SPACE_UNMANAGED =
            "XR_ACTIVITY_START_MODE_FULL_SPACE_UNMANAGED";

    /**
     * The default value if not specified. If used, the actual launching mode will be determined by
     * the system based on the launching activity's current mode and the launching flags.  When
     * {@link #PROPERTY_XR_ACTIVITY_START_MODE} is used at the application level, apps can use this
     * value to reset at individual activity level.
     *
     * @see #PROPERTY_XR_ACTIVITY_START_MODE
     */
    @FlaggedApi(android.xr.Flags.FLAG_XR_MANIFEST_ENTRIES)
    public static final String XR_ACTIVITY_START_MODE_UNDEFINED =
            "XR_ACTIVITY_START_MODE_UNDEFINED";

    /**
     * Defines the value to launch an activity in
     * <a href="https://developer.android.com/develop/xr/jetpack-xr-sdk/transition-home-space-to-full-space">managed
     * full space mode</a> in XR, where the system is rendering the activity from a scene graph.
     *
     * @see #PROPERTY_XR_ACTIVITY_START_MODE
     */
    @FlaggedApi(android.xr.Flags.FLAG_XR_MANIFEST_ENTRIES)
    public static final String XR_ACTIVITY_START_MODE_FULL_SPACE_MANAGED =
            "XR_ACTIVITY_START_MODE_FULL_SPACE_MANAGED";

    /**
     * Defines the value to launch an activity in
     * <a href="https://developer.android.com/develop/xr/jetpack-xr-sdk/transition-home-space-to-full-space">home
     * space mode</a> in XR.
     *
     * @see #PROPERTY_XR_ACTIVITY_START_MODE
     */
    @FlaggedApi(android.xr.Flags.FLAG_XR_MANIFEST_ENTRIES)
    public static final String XR_ACTIVITY_START_MODE_HOME_SPACE =
            "XR_ACTIVITY_START_MODE_HOME_SPACE";

    /**
     * Application and Activity level
     * {@link android.content.pm.PackageManager.Property PackageManager.Property} for an app to
     * inform the system of the type of safety boundary recommended for the activity. When it is
     * declared at the application level, all activities are set to the defined value, unless it is
     * overridden at the activity level. When not declared, the system will not enforce any
     * recommendations for a type of safety boundary and will continue to use the type that is
     * currently in use.
     *
     * <p>The default value is {@link #XR_BOUNDARY_TYPE_NO_RECOMMENDATION}.
     *
     * <p>The available values are:
     * <ul>
     *   <li>{@link #XR_BOUNDARY_TYPE_LARGE}
     *   <li>{@link #XR_BOUNDARY_TYPE_NO_RECOMMENDATION}
     * </ul>
     *
     * <p><b>Syntax:</b>
     * <pre>
     * &lt;application&gt;
     *   &lt;property
     *     android:name="android.window.PROPERTY_XR_BOUNDARY_TYPE_RECOMMENDED"
     *     android:value="XR_BOUNDARY_TYPE_LARGE|
     *                    XR_BOUNDARY_TYPE_NO_RECOMMENDATION"/&gt;
     * &lt;/application&gt;
     * </pre>
     */
    @FlaggedApi(android.xr.Flags.FLAG_XR_MANIFEST_ENTRIES)
    public static final String PROPERTY_XR_BOUNDARY_TYPE_RECOMMENDED =
            "android.window.PROPERTY_XR_BOUNDARY_TYPE_RECOMMENDED";

    /**
     * Defines the value to launch an activity with no recommendations for the type of safety
     * boundary. The system will continue to use the type of safety boundary that is currently
     * in use.
     *
     * @see #PROPERTY_XR_BOUNDARY_TYPE_RECOMMENDED
     */
    @FlaggedApi(android.xr.Flags.FLAG_XR_MANIFEST_ENTRIES)
    public static final String XR_BOUNDARY_TYPE_NO_RECOMMENDATION =
            "XR_BOUNDARY_TYPE_NO_RECOMMENDATION";

    /**
     * Defines the value to launch an activity with a large boundary recommended. This is useful for
     * activities which expect users to be moving around. The system will ask the user to use a
     * larger size for their safety boundary and check that their space is clear, if the larger
     * size is not already in use. This larger size will be determined by the system.
     *
     * @see #PROPERTY_XR_BOUNDARY_TYPE_RECOMMENDED
     */
    @FlaggedApi(android.xr.Flags.FLAG_XR_MANIFEST_ENTRIES)
    public static final String XR_BOUNDARY_TYPE_LARGE = "XR_BOUNDARY_TYPE_LARGE";

    /**
     * Application and Activity level
     * {@link android.content.pm.PackageManager.Property PackageManager.Property} to inform the
     * system if it should play a system provided default animation when the app requests to enter
     * or exit <a
     * href="https://developer.android.com/develop/xr/jetpack-xr-sdk/transition-home-space-to-full-space">managed
     * full space mode</a> in XR. When set to {@code true}, the system provided default animation is
     * not played and the app is responsible for playing a custom enter or exit animation. When it
     * is declared at the application level, all activities are set to the defined value, unless it
     * is overridden at the activity level.
     *
     * <p>The default value is {@code false}.
     *
     * <p><b>Syntax:</b>
     * <pre>
     * &lt;application&gt;
     *   &lt;property
     *     android:name="android.window.PROPERTY_XR_USES_CUSTOM_FULL_SPACE_MANAGED_ANIMATION"
     *     android:value="false|true"/&gt;
     * &lt;/application&gt;
     * </pre>
     */
    @FlaggedApi(android.xr.Flags.FLAG_XR_MANIFEST_ENTRIES)
    public static final String PROPERTY_XR_USES_CUSTOM_FULL_SPACE_MANAGED_ANIMATION =
            "android.window.PROPERTY_XR_USES_CUSTOM_FULL_SPACE_MANAGED_ANIMATION";
}
