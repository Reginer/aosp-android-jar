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

package android.safetycenter;

import android.safetycenter.IOnSafetyCenterDataChangedListener;
import android.safetycenter.SafetyCenterData;
import android.safetycenter.SafetyEvent;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceErrorDetails;
import android.safetycenter.config.SafetyCenterConfig;
import java.util.List;

/**
 * AIDL Interface for communicating with the Safety Center, which consolidates UI for security and
 * privacy features on the device.
 *
 * These APIs are intended to be used by the following clients:
 * <ul>
 *     <li>Safety sources represented in Safety Center UI
 *     <li>Dependents on the state of Safety Center UI
 *     <li>Managers of Safety Center UI
 * </ul>
 *
 * @hide
 */
interface ISafetyCenterManager {
    /** Returns whether the Safety Center feature is enabled. */
    boolean isSafetyCenterEnabled();

    /**
     * Sets the latest SafetySourceData for the given safetySourceId and user to be displayed in
     * SafetyCenter UI.
     */
    void setSafetySourceData(
            String sourceId,
            in SafetySourceData safetySourceData,
            in SafetyEvent safetyEvent,
            String packageName,
            int userId);

    /** Returns the latest SafetySourceData set for the given safetySourceId and user. */
    SafetySourceData getSafetySourceData(
            String safetySourceId,
            String packageName,
            int userId);

    /**
     * Notifies the SafetyCenter of an error related to a given safety source.
     *
     * <p>Safety sources should use this API to notify SafetyCenter when SafetyCenter requested or
     * expected them to perform an action or provide data, but they were unable to do so.
     */
    void reportSafetySourceError(
            String safetySourceId,
            in SafetySourceErrorDetails safetySourceErrorDetails,
            String packageName,
            int userId);

    /** Requests safety sources to set their latest SafetySourceData for Safety Center. */
    void refreshSafetySources(int refreshReason, int userId);

    /**
    * Requests a specific subset of safety sources to set their latest SafetySourceData for
    * Safety Center.
    */
    void refreshSpecificSafetySources(int refreshReason, int userId, in List<String> safetySourceIds);

    /** Returns the current SafetyCenterConfig, if available. */
    SafetyCenterConfig getSafetyCenterConfig();

    /**
     * Returns the current SafetyCenterData, assembled from the SafetySourceData from all sources.
     */
    SafetyCenterData getSafetyCenterData(String packageName, int userId);

    void addOnSafetyCenterDataChangedListener(
            IOnSafetyCenterDataChangedListener listener,
            String packageName,
            int userId);

    void removeOnSafetyCenterDataChangedListener(
            IOnSafetyCenterDataChangedListener listener,
            int userId);

    /**
     * Dismiss a Safety Center issue and prevent it affecting the overall safety status.
     */
    void dismissSafetyCenterIssue(String issueId, int userId);

    /** Executes the specified Safety Center issue action on the specified Safety Center issue. */
    void executeSafetyCenterIssueAction(
            String safetyCenterIssueId,
            String safetyCenterIssueActionId,
            int userId);

    /**
     * Clears all SafetySourceData (set by safety sources using setSafetySourceData) for testing.
     *
     * <p>Note: This API serves to facilitate CTS testing and should not be used for other purposes.
     */
    void clearAllSafetySourceDataForTests();

    /**
     * Overrides the SafetyCenterConfig for testing.
     *
     * <p>When set, the overridden SafetyCenterConfig will be used instead of the
     * SafetyCenterConfig parsed from the XML file to read configured safety sources.
     *
     * <p>Note: This API serves to facilitate CTS testing and should not be used to configure safety
     * sources dynamically for production. Once used for testing, the override should be cleared.
     *
     * See clearSafetyCenterConfigForTests.
     */
    void setSafetyCenterConfigForTests(in SafetyCenterConfig safetyCenterConfig);

    /**
     * Clears the override of the SafetyCenterConfig set for testing.
     *
     * <p>Note: This API serves to facilitate CTS testing and should not be used for other purposes.
     *
     * See setSafetyCenterConfigForTests(SafetyCenterConfig).
     */
    void clearSafetyCenterConfigForTests();
}