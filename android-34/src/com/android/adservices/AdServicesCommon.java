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
package com.android.adservices;

import android.adservices.adid.AdIdProviderService;
import android.adservices.appsetid.AppSetIdProviderService;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

import java.util.List;

/**
 * Common constants for AdServices
 *
 * @hide
 */
public class AdServicesCommon {
    private AdServicesCommon() {}

    /** Intent action to discover the Topics service in the APK. */
    public static final String ACTION_TOPICS_SERVICE = "android.adservices.TOPICS_SERVICE";

    /** Intent action to discover the Custom Audience service in the APK. */
    public static final String ACTION_CUSTOM_AUDIENCE_SERVICE =
            "android.adservices.customaudience.CUSTOM_AUDIENCE_SERVICE";

    /** Intent action to discover the AdSelection service in the APK. */
    public static final String ACTION_AD_SELECTION_SERVICE =
            "android.adservices.adselection.AD_SELECTION_SERVICE";

    /** Intent action to discover the Measurement service in the APK. */
    public static final String ACTION_MEASUREMENT_SERVICE =
            "android.adservices.MEASUREMENT_SERVICE";

    /** Intent action to discover the AdId service in the APK. */
    public static final String ACTION_ADID_SERVICE = "android.adservices.ADID_SERVICE";

    /** Intent action to discover the AdId Provider service. */
    public static final String ACTION_ADID_PROVIDER_SERVICE = AdIdProviderService.SERVICE_INTERFACE;

    /** Intent action to discover the AppSetId service in the APK. */
    public static final String ACTION_APPSETID_SERVICE = "android.adservices.APPSETID_SERVICE";

    /** Intent action to discover the AppSetId Provider service. */
    public static final String ACTION_APPSETID_PROVIDER_SERVICE =
            AppSetIdProviderService.SERVICE_INTERFACE;

    /** Intent action to discover the AdServicesCommon service in the APK. */
    public static final String ACTION_AD_SERVICES_COMMON_SERVICE =
            "android.adservices.AD_SERVICES_COMMON_SERVICE";

    // Used to differentiate between AdServices APK package name and AdExtServices APK package name.
    // The AdExtServices APK package name suffix is android.ext.services.
    public static final String ADSERVICES_APK_PACKAGE_NAME_SUFFIX = "android.adservices";

    // Suffix for the ExtServices APEX Package name. Used to figure out the installed apex version.
    public static final String EXTSERVICES_APEX_NAME_SUFFIX = "android.extservices";

    /** The package name of the active AdServices APK on this device. */
    public static ServiceInfo resolveAdServicesService(
            List<ResolveInfo> intentResolveInfos, String intentAction) {
        if (intentResolveInfos == null || intentResolveInfos.isEmpty()) {
            LogUtil.e(
                    "Failed to find resolveInfo for adServices service. Intent action: "
                            + intentAction);
            return null;
        }

        // On T+ devices, we may have two versions of the services present due to
        // b/263904312.
        if (intentResolveInfos.size() > 2) {
            StringBuilder intents = new StringBuilder("");
            for (ResolveInfo intentResolveInfo : intentResolveInfos) {
                if (intentResolveInfo != null && intentResolveInfo.serviceInfo != null) {
                    intents.append(intentResolveInfo.serviceInfo.packageName);
                }
            }
            LogUtil.e("Found multiple services " + intents + " for " + intentAction);
            return null;
        }

        // On T+ devices, only use the service that comes from AdServices APK. The package name of
        // AdService is com.[google.]android.adservices while the package name of AdExtServices APK
        // is com.[google.]android.ext.adservices.
        ServiceInfo serviceInfo = null;

        // We have already checked if there are 0 OR more than 2 services returned.
        switch (intentResolveInfos.size()) {
            case 2:
                // In the case of 2, always use the one from AdServicesApk.
                if (intentResolveInfos.get(0) != null
                        && intentResolveInfos.get(0).serviceInfo != null
                        && intentResolveInfos.get(0).serviceInfo.packageName != null
                        && intentResolveInfos
                                .get(0)
                                .serviceInfo
                                .packageName
                                .contains(ADSERVICES_APK_PACKAGE_NAME_SUFFIX)) {
                    serviceInfo = intentResolveInfos.get(0).serviceInfo;
                } else if (intentResolveInfos.get(1) != null
                        && intentResolveInfos.get(1).serviceInfo != null
                        && intentResolveInfos.get(1).serviceInfo.packageName != null
                        && intentResolveInfos
                                .get(1)
                                .serviceInfo
                                .packageName
                                .contains(ADSERVICES_APK_PACKAGE_NAME_SUFFIX)) {
                    serviceInfo = intentResolveInfos.get(1).serviceInfo;
                }
                break;

            case 1:
                serviceInfo = intentResolveInfos.get(0).serviceInfo;
                break;
        }
        return serviceInfo;
    }
}
