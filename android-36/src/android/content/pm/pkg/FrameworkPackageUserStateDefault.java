/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.content.pm.pkg;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.pm.PackageManager;
import android.content.pm.overlay.OverlayPaths;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @hide
 * @deprecated Unused by framework.
 */
@Deprecated
class FrameworkPackageUserStateDefault implements FrameworkPackageUserState {

    @Override
    public int getEnabledState() {
        return PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
    }

    @Override
    public int getInstallReason() {
        return PackageManager.INSTALL_REASON_UNKNOWN;
    }

    @NonNull
    @Override
    public Map<String, OverlayPaths> getSharedLibraryOverlayPaths() {
        return Collections.emptyMap();
    }

    @Override
    public int getUninstallReason() {
        return PackageManager.UNINSTALL_REASON_UNKNOWN;
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @NonNull
    @Override
    public Set<String> getDisabledComponents() {
        return Collections.emptySet();
    }

    @NonNull
    @Override
    public Set<String> getEnabledComponents() {
        return Collections.emptySet();
    }

    @Override
    public long getCeDataInode() {
        return 0;
    }

    @Override
    public int getDistractionFlags() {
        return 0;
    }

    @Nullable
    @Override
    public String getHarmfulAppWarning() {
        return null;
    }

    @Nullable
    @Override
    public String getLastDisableAppCaller() {
        return null;
    }

    @Nullable
    @Override
    public OverlayPaths getOverlayPaths() {
        return null;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean isInstantApp() {
        return false;
    }

    @Override
    public boolean isNotLaunched() {
        return false;
    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public boolean isSuspended() {
        return false;
    }

    @Override
    public boolean isVirtualPreload() {
        return false;
    }

    @Nullable
    @Override
    public String getSplashScreenTheme() {
        return null;
    }

    @Override
    public boolean isComponentEnabled(String componentName) {
        return false;
    }

    @Override
    public boolean isComponentDisabled(String componentName) {
        return false;
    }

    @Nullable
    @Override
    public OverlayPaths getAllOverlayPaths() {
        return null;
    }
}
