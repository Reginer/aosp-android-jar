/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.content;

import android.annotation.NonNull;
import android.content.res.Configuration;

/**
 * The set of callback APIs that are common to all application components
 * ({@link android.app.Activity}, {@link android.app.Service},
 * {@link ContentProvider}, and {@link android.app.Application}).
 *
 * <p class="note"><strong>Note:</strong> You should also implement the {@link
 * ComponentCallbacks2} interface, which provides the {@link
 * ComponentCallbacks2#onTrimMemory} callback to help your app manage its memory usage more
 * effectively.</p>
 */
public interface ComponentCallbacks {
    /**
     * Called by the system when the device configuration changes while your
     * component is running.  Note that, unlike activities, other components
     * are never restarted when a configuration changes: they must always deal
     * with the results of the change, such as by re-retrieving resources.
     *
     * <p>At the time that this function has been called, your Resources
     * object will have been updated to return resource values matching the
     * new configuration.
     *
     * <p>For more information, read <a href="{@docRoot}guide/topics/resources/runtime-changes.html"
     * >Handling Runtime Changes</a>.
     *
     * @param newConfig The new device configuration.
     */
    void onConfigurationChanged(@NonNull Configuration newConfig);

    /**
     * This is called when the overall system is running low on memory, and
     * actively running processes should trim their memory usage.  While
     * the exact point at which this will be called is not defined, generally
     * it will happen when all background process have been killed.
     * That is, before reaching the point of killing processes hosting
     * service and foreground UI that we would like to avoid killing.
     *
     * @deprecated Since API level 14 this is superseded by
     *             {@link ComponentCallbacks2#onTrimMemory}.
     *             Since API level 34 this is never called.
     *             If you're overriding ComponentCallbacks2#onTrimMemory and
     *             your minSdkVersion is greater than API 14, you can provide
     *             an empty implementation for this method.
     */
    @Deprecated
    void onLowMemory();
}
