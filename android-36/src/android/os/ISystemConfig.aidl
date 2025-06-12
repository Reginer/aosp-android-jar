/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.os;

import android.content.ComponentName;
import android.os.Bundle;
import android.content.pm.SignedPackageParcel;

/**
  * Binder interface to query SystemConfig in the system server.
  * {@hide}
  */
interface ISystemConfig {
    /**
     * @see SystemConfigManager#getDisabledUntilUsedPreinstalledCarrierApps
     */
    List<String> getDisabledUntilUsedPreinstalledCarrierApps();

    /**
     * @see SystemConfigManager#getDisabledUntilUsedPreinstalledCarrierAssociatedApps
     */
    Map getDisabledUntilUsedPreinstalledCarrierAssociatedApps();

    /**
     * @see SystemConfigManager#getDisabledUntilUsedPreinstalledCarrierAssociatedAppEntries
     */
    Map getDisabledUntilUsedPreinstalledCarrierAssociatedAppEntries();

    /**
     * @see SystemConfigManager#getSystemPermissionUids
     */
    int[] getSystemPermissionUids(String permissionName);

    /**
     * @see SystemConfigManager#getEnabledComponentOverrides
     */
    List<ComponentName> getEnabledComponentOverrides(String packageName);

    /**
     * @see SystemConfigManager#getDefaultVrComponents
     */
    List<ComponentName> getDefaultVrComponents();

    /**
     * @see SystemConfigManager#getPreventUserDisablePackages
     */
    List<String> getPreventUserDisablePackages();

    /**
     * @see SystemConfigManager#getEnhancedConfirmationTrustedPackages
     */
    List<SignedPackageParcel> getEnhancedConfirmationTrustedPackages();

    /**
     * @see SystemConfigManager#getEnhancedConfirmationTrustedInstallers
     */
    List<SignedPackageParcel> getEnhancedConfirmationTrustedInstallers();
}
