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

package android.adservices.customaudience;

import android.adservices.common.AdSelectionSignals;
import android.adservices.common.AdTechIdentifier;
import android.adservices.customaudience.CustomAudience;
import android.adservices.customaudience.CustomAudienceOverrideCallback;
import android.adservices.customaudience.ICustomAudienceCallback;
import android.net.Uri;

/**
 * Custom audience service.
 *
 * @hide
 */
interface ICustomAudienceService {
    void joinCustomAudience(in CustomAudience customAudience, in String ownerPackageName,
            in ICustomAudienceCallback callback);
    void leaveCustomAudience(in String ownerPackageName, in AdTechIdentifier buyer, in String name,
            in ICustomAudienceCallback callback);

    /**
     * Configures PP api to avoid fetching the biddingLogicJS and trustedBiddingData from a server
     * and instead use the content provided in {@code biddingLogicJS} and {@code trustedBiddingData}
     * for the CA identified by {@code ownerPackageName}, {@code buyer}, {@code name}
     *
     * The call will throw a SecurityException if:
     * the API hasn't been enabled by developer options or by an adb command
     * or if the calling application manifest is not setting Android:debuggable to true.
     * or if the CA hasn't been created by the same app doing invoking this API.
     *
     * The call will fail silently if the CustomAudience has been created by a different app.
     */
    void overrideCustomAudienceRemoteInfo(in String ownerPackageName, in AdTechIdentifier buyer,
            in String name, in String biddingLogicJS, in long biddingLogicJsVersion,
            in AdSelectionSignals trustedBiddingData, in CustomAudienceOverrideCallback callback);

    /**
     * Deletes any override created by calling
     * {@code overrideCustomAudienceRemoteInfo} for the CA identified by
     * {@code ownerPackageName} {@code buyer}, {@code name}.
     *
     * The call will throw a SecurityException if:
     * the API hasn't been enabled by developer options or by an adb command
     * or if the calling application manifest is not setting Android:debuggable to true.
     *
     * The call will fail silently if the CustomAudience has been created by a different app.
     */
    void removeCustomAudienceRemoteInfoOverride(in String ownerPackageName,
            in AdTechIdentifier buyer, in String name, in CustomAudienceOverrideCallback callback);

    /**
     * Deletes any override created by calling
     * {@code overrideCustomAudienceRemoteInfo} from this application.
     *
     * The call will throw a SecurityException if the API hasn't been enabled
     * by developer options or by an adb command and if the calling
     * application manifest is not setting Android:debuggable to true.
     */
    void resetAllCustomAudienceOverrides(in CustomAudienceOverrideCallback callback);
}
