/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.content.pm;

import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.UserHandle;

/**
 * @hide
 */
interface ICrossProfileApps {
    void startActivityAsUser(in IApplicationThread caller, in String callingPackage,
            in String callingFeatureId, in ComponentName component, int userId,
            boolean launchMainActivity, in IBinder task, in Bundle options);
    void startActivityAsUserByIntent(in IApplicationThread caller, in String callingPackage,
            in String callingFeatureId, in Intent intent, int userId, in IBinder callingActivity,
            in Bundle options);
    List<UserHandle> getTargetUserProfiles(in String callingPackage);
    boolean canInteractAcrossProfiles(in String callingPackage);
    boolean canRequestInteractAcrossProfiles(in String callingPackage);
    void setInteractAcrossProfilesAppOp(int userId, in String packageName, int newMode);
    boolean canConfigureInteractAcrossProfiles(int userId, in String packageName);
    boolean canUserAttemptToConfigureInteractAcrossProfiles(int userId, in String packageName);
    void resetInteractAcrossProfilesAppOps(int userId, in List<String> packageNames);
    void clearInteractAcrossProfilesAppOps(int userId);
}
