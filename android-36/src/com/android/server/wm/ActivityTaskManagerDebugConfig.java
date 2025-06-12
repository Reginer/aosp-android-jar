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

package com.android.server.wm;

/**
 * Common class for the various debug {@link android.util.Log} output configuration relating to
 * activities.
 */
public class ActivityTaskManagerDebugConfig {
    // All output logs relating to acitvities use the {@link #TAG_ATM} string for tagging their log
    // output. This makes it easy to identify the origin of the log message when sifting
    // through a large amount of log output from multiple sources. However, it also makes trying
    // to figure-out the origin of a log message while debugging the activity manager a little
    // painful. By setting this constant to true, log messages from the activity manager package
    // will be tagged with their class names instead fot the generic tag.
    static final boolean TAG_WITH_CLASS_NAME = false;

    // While debugging it is sometimes useful to have the category name of the log appended to the
    // base log tag to make sifting through logs with the same base tag easier. By setting this
    // constant to true, the category name of the log point will be appended to the log tag.
    private static final boolean APPEND_CATEGORY_NAME = false;

    // Default log tag for the activities.
    static final String TAG_ATM = "ActivityTaskManager";

    // Enable all debug log categories.
    static final boolean DEBUG_ALL = false;

    // Enable all debug log categories for activities.
    private static final boolean DEBUG_ALL_ACTIVITIES = DEBUG_ALL || false;

    static final boolean DEBUG_RECENTS = DEBUG_ALL || false;
    static final boolean DEBUG_RECENTS_TRIM_TASKS = DEBUG_RECENTS || false;
    static final boolean DEBUG_ROOT_TASK = DEBUG_ALL || false;
    public static final boolean DEBUG_SWITCH = DEBUG_ALL || false;
    static final boolean DEBUG_TRANSITION = DEBUG_ALL || false;
    static final boolean DEBUG_VISIBILITY = DEBUG_ALL || false;
    static final boolean DEBUG_APP = DEBUG_ALL_ACTIVITIES || false;
    static final boolean DEBUG_IDLE = DEBUG_ALL_ACTIVITIES || false;
    static final boolean DEBUG_RELEASE = DEBUG_ALL_ACTIVITIES || false;
    static final boolean DEBUG_USER_LEAVING = DEBUG_ALL || false;
    static final boolean DEBUG_PERMISSIONS_REVIEW = DEBUG_ALL || false;
    static final boolean DEBUG_RESULTS = DEBUG_ALL || false;
    static final boolean DEBUG_ACTIVITY_STARTS = DEBUG_ALL || false;
    public static final boolean DEBUG_CLEANUP = DEBUG_ALL || false;
    public static final boolean DEBUG_METRICS = DEBUG_ALL || false;

    static final String POSTFIX_APP = APPEND_CATEGORY_NAME ? "_App" : "";
    static final String POSTFIX_CLEANUP = (APPEND_CATEGORY_NAME) ? "_Cleanup" : "";
    static final String POSTFIX_IDLE = APPEND_CATEGORY_NAME ? "_Idle" : "";
    static final String POSTFIX_RELEASE = APPEND_CATEGORY_NAME ? "_Release" : "";
    static final String POSTFIX_USER_LEAVING = APPEND_CATEGORY_NAME ? "_UserLeaving" : "";
    static final String POSTFIX_ADD_REMOVE = APPEND_CATEGORY_NAME ? "_AddRemove" : "";
    public static final String POSTFIX_CONFIGURATION = APPEND_CATEGORY_NAME ? "_Configuration" : "";
    static final String POSTFIX_CONTAINERS = APPEND_CATEGORY_NAME ? "_Containers" : "";
    static final String POSTFIX_FOCUS = APPEND_CATEGORY_NAME ? "_Focus" : "";
    static final String POSTFIX_IMMERSIVE = APPEND_CATEGORY_NAME ? "_Immersive" : "";
    public static final String POSTFIX_LOCKTASK = APPEND_CATEGORY_NAME ? "_LockTask" : "";
    static final String POSTFIX_PAUSE = APPEND_CATEGORY_NAME ? "_Pause" : "";
    static final String POSTFIX_RECENTS = APPEND_CATEGORY_NAME ? "_Recents" : "";
    static final String POSTFIX_SAVED_STATE = APPEND_CATEGORY_NAME ? "_SavedState" : "";
    static final String POSTFIX_ROOT_TASK = APPEND_CATEGORY_NAME ? "_RootTask" : "";
    static final String POSTFIX_STATES = APPEND_CATEGORY_NAME ? "_States" : "";
    public static final String POSTFIX_SWITCH = APPEND_CATEGORY_NAME ? "_Switch" : "";
    static final String POSTFIX_TASKS = APPEND_CATEGORY_NAME ? "_Tasks" : "";
    static final String POSTFIX_TRANSITION = APPEND_CATEGORY_NAME ? "_Transition" : "";
    static final String POSTFIX_VISIBILITY = APPEND_CATEGORY_NAME ? "_Visibility" : "";
    static final String POSTFIX_RESULTS = APPEND_CATEGORY_NAME ? "_Results" : "";
}
