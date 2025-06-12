/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.apphibernation;

import android.apphibernation.HibernationStats;

/**
 * Binder interface to communicate with AppHibernationService.
 * @hide
 */
interface IAppHibernationService {
    boolean isHibernatingForUser(String packageName, int userId);
    void setHibernatingForUser(String packageName, int userId, boolean isHibernating);
    boolean isHibernatingGlobally(String packageName);
    void setHibernatingGlobally(String packageName, boolean isHibernating);
    List<String> getHibernatingPackagesForUser(int userId);
    Map<String, HibernationStats> getHibernationStatsForUser(in List<String> packageNames,
            int userId);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.MANAGE_APP_HIBERNATION)")
    boolean isOatArtifactDeletionEnabled();
}