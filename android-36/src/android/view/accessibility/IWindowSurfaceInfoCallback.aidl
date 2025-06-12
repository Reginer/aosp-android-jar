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

package android.view.accessibility;

import android.view.SurfaceControl;

/**
 * Callback for an app to provide AbstractAccessibilityServiceConnection with window and surface
 * information necessary for system_server to take a screenshot of the app window.
 * @hide
 */
oneway interface IWindowSurfaceInfoCallback {

    /**
     * Provide info from ViewRootImpl for taking a screenshot of this app window.
     *
     * @param windowFlags the window flags of ViewRootImpl
     * @param processUid the process (kernel) uid, NOT the user ID, required for
                         SurfaceFlinger screenshots
     * @param surfaceControl the surface of ViewRootImpl
     */
    @RequiresNoPermission
    void provideWindowSurfaceInfo(int windowFlags, int processUid,
            in SurfaceControl surfaceControl);
}
