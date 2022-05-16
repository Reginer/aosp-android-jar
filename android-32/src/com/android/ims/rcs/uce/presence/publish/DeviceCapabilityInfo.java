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

package com.android.ims.rcs.uce.presence.publish;

import static android.telephony.ims.RcsContactUceCapability.SOURCE_TYPE_CACHED;

import android.content.Context;
import android.net.Uri;
import android.telecom.TelecomManager;
import android.telephony.AccessNetworkConstants;
import android.telephony.ims.ImsRegistrationAttributes;
import android.telephony.ims.RcsContactPresenceTuple;
import android.telephony.ims.RcsContactPresenceTuple.ServiceCapabilities;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.RcsContactUceCapability.CapabilityMechanism;
import android.telephony.ims.RcsContactUceCapability.OptionsBuilder;
import android.telephony.ims.RcsContactUceCapability.PresenceBuilder;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.feature.MmTelFeature.MmTelCapabilities;
import android.util.IndentingPrintWriter;
import android.util.ArraySet;
import android.util.LocalLog;
import android.util.Log;

import com.android.ims.rcs.uce.util.FeatureTags;
import com.android.ims.rcs.uce.util.UceUtils;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stores the device's capabilities information.
 */
public class DeviceCapabilityInfo {
    private static final String LOG_TAG = UceUtils.getLogPrefix() + "DeviceCapabilityInfo";

    private final int mSubId;

    private final LocalLog mLocalLog = new LocalLog(UceUtils.LOG_SIZE);

    // FT overrides to add to the IMS registration, which will be added to the existing
    // capabilities.
    private final Set<String> mOverrideAddFeatureTags = new ArraySet<>();

    // FT overrides to remove from the existing IMS registration, which will remove the related
    // capabilities.
    private final Set<String> mOverrideRemoveFeatureTags = new ArraySet<>();

    // Tracks capability status based on the IMS registration.
    private PublishServiceDescTracker mServiceCapRegTracker;

    // The feature tags associated with the last IMS registration update.
    private Set<String> mLastRegistrationFeatureTags = Collections.emptySet();
    // The feature tags associated with the last IMS registration update, which also include
    // overrides
    private Set<String> mLastRegistrationOverrideFeatureTags = Collections.emptySet();

    // The mmtel feature is registered or not
    private boolean mMmtelRegistered;

    // The network type which ims mmtel registers on.
    private int mMmtelNetworkRegType;

    // The list of the mmtel associated uris
    private List<Uri> mMmtelAssociatedUris = Collections.emptyList();

    // The rcs feature is registered or not
    private boolean mRcsRegistered;

    // The list of the rcs associated uris
    private List<Uri> mRcsAssociatedUris = Collections.emptyList();

    // Whether or not presence is reported as capable
    private boolean mPresenceCapable;

    // The network type which ims rcs registers on.
    private int mRcsNetworkRegType;

    // The MMTel capabilities of this subscription Id
    private MmTelFeature.MmTelCapabilities mMmTelCapabilities;

    // Whether the settings are changed or not
    private int mTtyPreferredMode;
    private boolean mAirplaneMode;
    private boolean mMobileData;
    private boolean mVtSetting;

    public DeviceCapabilityInfo(int subId, String[] capToRegistrationMap) {
        mSubId = subId;
        mServiceCapRegTracker = PublishServiceDescTracker.fromCarrierConfig(capToRegistrationMap);
        reset();
    }

    /**
     * Reset all the status.
     */
    public synchronized void reset() {
        logd("reset");
        mMmtelRegistered = false;
        mMmtelNetworkRegType = AccessNetworkConstants.TRANSPORT_TYPE_INVALID;
        mRcsRegistered = false;
        mRcsNetworkRegType = AccessNetworkConstants.TRANSPORT_TYPE_INVALID;
        mTtyPreferredMode = TelecomManager.TTY_MODE_OFF;
        mAirplaneMode = false;
        mMobileData = true;
        mVtSetting = true;
        mMmTelCapabilities = new MmTelCapabilities();
        mMmtelAssociatedUris = Collections.EMPTY_LIST;
        mRcsAssociatedUris = Collections.EMPTY_LIST;
    }

    /**
     * Update the capability registration tracker feature tag override mapping.
     * @return if true, this has caused a change in the Feature Tags associated with the device
     * and a new PUBLISH should be generated.
     */
    public synchronized boolean updateCapabilityRegistrationTrackerMap(String[] newMap) {
        Set<String> oldTags = mServiceCapRegTracker.copyRegistrationFeatureTags();
        mServiceCapRegTracker = PublishServiceDescTracker.fromCarrierConfig(newMap);
        mServiceCapRegTracker.updateImsRegistration(mLastRegistrationOverrideFeatureTags);
        boolean changed = !oldTags.equals(mServiceCapRegTracker.copyRegistrationFeatureTags());
        if (changed) logi("Carrier Config Change resulted in associated FT list change");
        return changed;
    }

    public synchronized boolean isImsRegistered() {
        return mMmtelRegistered || mRcsRegistered;
    }

    /**
     * Update the status that IMS MMTEL is registered.
     */
    public synchronized void updateImsMmtelRegistered(int type) {
        StringBuilder builder = new StringBuilder();
        builder.append("IMS MMTEL registered: original state=").append(mMmtelRegistered)
                .append(", changes type from ").append(mMmtelNetworkRegType)
                .append(" to ").append(type);
        logi(builder.toString());

        if (!mMmtelRegistered) {
            mMmtelRegistered = true;
        }

        if (mMmtelNetworkRegType != type) {
            mMmtelNetworkRegType = type;
        }
    }

    /**
     * Update the status that IMS MMTEL is unregistered.
     */
    public synchronized void updateImsMmtelUnregistered() {
        logi("IMS MMTEL unregistered: original state=" + mMmtelRegistered);
        if (mMmtelRegistered) {
            mMmtelRegistered = false;
        }
        mMmtelNetworkRegType = AccessNetworkConstants.TRANSPORT_TYPE_INVALID;
    }

    /**
     * Update the MMTel associated URIs which are provided by the IMS service.
     */
    public synchronized void updateMmTelAssociatedUri(Uri[] uris) {
        int originalSize = mMmtelAssociatedUris.size();
        if (uris != null) {
            mMmtelAssociatedUris = Arrays.stream(uris)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            mMmtelAssociatedUris.clear();
        }
        int currentSize = mMmtelAssociatedUris.size();
        logd("updateMmTelAssociatedUri: size from " + originalSize + " to " + currentSize);
    }

    /**
     * Get the MMTEL associated URI. When there are multiple uris in the list, take the first uri.
     * Return null if the list of the MMTEL associated uri is empty.
     */
    public synchronized Uri getMmtelAssociatedUri() {
        if (!mMmtelAssociatedUris.isEmpty()) {
            return mMmtelAssociatedUris.get(0);
        }
        return null;
    }

    /**
     * Update the status that IMS RCS is registered.
     * @return true if the IMS registration status changed, false if it did not.
     */
    public synchronized boolean updateImsRcsRegistered(ImsRegistrationAttributes attr) {
        StringBuilder builder = new StringBuilder();
        builder.append("IMS RCS registered: original state=").append(mRcsRegistered)
                .append(", changes type from ").append(mRcsNetworkRegType)
                .append(" to ").append(attr.getTransportType());
        logi(builder.toString());

        boolean changed = false;
        if (!mRcsRegistered) {
            mRcsRegistered = true;
            changed = true;
        }

        if (mRcsNetworkRegType != attr.getTransportType()) {
            mRcsNetworkRegType = attr.getTransportType();
            changed = true;
        }

        mLastRegistrationFeatureTags = attr.getFeatureTags();
        changed |= updateRegistration(mLastRegistrationFeatureTags);

        return changed;
    }

    /**
     * Update the status that IMS RCS is unregistered.
     */
    public synchronized boolean updateImsRcsUnregistered() {
        logi("IMS RCS unregistered: original state=" + mRcsRegistered);
        boolean changed = false;
        if (mRcsRegistered) {
            mRcsRegistered = false;
            changed = true;
        }
        mRcsNetworkRegType = AccessNetworkConstants.TRANSPORT_TYPE_INVALID;
        return changed;
    }

    /**
     * Update the RCS associated URIs which is provided by the IMS service.
     */
    public synchronized void updateRcsAssociatedUri(Uri[] uris) {
        int originalSize = mRcsAssociatedUris.size();
        if (uris != null) {
            mRcsAssociatedUris = Arrays.stream(uris)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            mRcsAssociatedUris.clear();
        }
        int currentSize = mRcsAssociatedUris.size();
        logd("updateRcsAssociatedUri: size from " + originalSize + " to " + currentSize);
    }

    /**
     * Get the RCS associated URI. When there are multiple uris in the list, take the first uri.
     * Return null if the list of the RCS associated uri is empty.
     */
    public synchronized Uri getRcsAssociatedUri() {
        if (!mRcsAssociatedUris.isEmpty()) {
            return mRcsAssociatedUris.get(0);
        }
        return null;
    }

    /**
     * Get the IMS associated URI. It will first get the uri of MMTEL if it is not empty, otherwise
     * it will try to get the uri of RCS. The null will be returned if both MMTEL and RCS are empty.
     */
    public synchronized Uri getImsAssociatedUri() {
        if (!mRcsAssociatedUris.isEmpty()) {
            return mRcsAssociatedUris.get(0);
        } else if (!mMmtelAssociatedUris.isEmpty()) {
            return mMmtelAssociatedUris.get(0);
        } else {
            return null;
        }
    }

    public synchronized boolean addRegistrationOverrideCapabilities(Set<String> featureTags) {
        logd("override - add: " + featureTags);
        mOverrideRemoveFeatureTags.removeAll(featureTags);
        mOverrideAddFeatureTags.addAll(featureTags);
        // Call with the last feature tags so that the new ones will be potentially picked up.
        return updateRegistration(mLastRegistrationFeatureTags);
    };

    public synchronized boolean removeRegistrationOverrideCapabilities(Set<String> featureTags) {
        logd("override - remove: " + featureTags);
        mOverrideAddFeatureTags.removeAll(featureTags);
        mOverrideRemoveFeatureTags.addAll(featureTags);
        // Call with the last feature tags so that the new ones will be potentially picked up.
        return updateRegistration(mLastRegistrationFeatureTags);
    };

    public synchronized boolean clearRegistrationOverrideCapabilities() {
        logd("override - clear");
        mOverrideAddFeatureTags.clear();
        mOverrideRemoveFeatureTags.clear();
        // Call with the last feature tags so that base tags will be restored
        return updateRegistration(mLastRegistrationFeatureTags);
    };

    /**
     * Update the IMS registration tracked by the PublishServiceDescTracker if needed.
     * @return true if the registration changed, else otherwise.
     */
    private boolean updateRegistration(Set<String> baseTags) {
        Set<String> updatedTags = updateImsRegistrationFeatureTags(baseTags);
        if (!mLastRegistrationOverrideFeatureTags.equals(updatedTags)) {
            mLastRegistrationOverrideFeatureTags = updatedTags;
            mServiceCapRegTracker.updateImsRegistration(updatedTags);
            return true;
        }
        return false;
    }

    /**
     * Combine IMS registration with overrides to produce a new feature tag Set.
     * @return true if the IMS registration changed, false otherwise.
     */
    private synchronized Set<String> updateImsRegistrationFeatureTags(Set<String> featureTags) {
        Set<String> tags = new ArraySet<>(featureTags);
        tags.addAll(mOverrideAddFeatureTags);
        tags.removeAll(mOverrideRemoveFeatureTags);
        return tags;
    }

    /**
     * Update the TTY preferred mode.
     * @return {@code true} if tty preferred mode is changed, {@code false} otherwise.
     */
    public synchronized boolean updateTtyPreferredMode(int ttyMode) {
        if (mTtyPreferredMode != ttyMode) {
            logd("TTY preferred mode changes from " + mTtyPreferredMode + " to " + ttyMode);
            mTtyPreferredMode = ttyMode;
            return true;
        }
        return false;
    }

    /**
     * Update airplane mode state.
     * @return {@code true} if the airplane mode is changed, {@code false} otherwise.
     */
    public synchronized boolean updateAirplaneMode(boolean state) {
        if (mAirplaneMode != state) {
            logd("Airplane mode changes from " + mAirplaneMode + " to " + state);
            mAirplaneMode = state;
            return true;
        }
        return false;
    }

    /**
     * Update mobile data setting.
     * @return {@code true} if the mobile data setting is changed, {@code false} otherwise.
     */
    public synchronized boolean updateMobileData(boolean mobileData) {
        if (mMobileData != mobileData) {
            logd("Mobile data changes from " + mMobileData + " to " + mobileData);
            mMobileData = mobileData;
            return true;
        }
        return false;
    }

    /**
     * Update VT setting.
     * @return {@code true} if vt setting is changed, {@code false}.otherwise.
     */
    public synchronized boolean updateVtSetting(boolean vtSetting) {
        if (mVtSetting != vtSetting) {
            logd("VT setting changes from " + mVtSetting + " to " + vtSetting);
            mVtSetting = vtSetting;
            return true;
        }
        return false;
    }

    /**
     * Update the MMTEL capabilities if the capabilities is changed.
     * @return {@code true} if the mmtel capabilities are changed, {@code false} otherwise.
     */
    public synchronized boolean updateMmtelCapabilitiesChanged(MmTelCapabilities capabilities) {
        if (capabilities == null) {
            return false;
        }
        boolean oldVolteAvailable = isVolteAvailable(mMmtelNetworkRegType, mMmTelCapabilities);
        boolean oldVoWifiAvailable = isVoWifiAvailable(mMmtelNetworkRegType, mMmTelCapabilities);
        boolean oldVtAvailable = isVtAvailable(mMmtelNetworkRegType, mMmTelCapabilities);
        boolean oldViWifiAvailable = isViWifiAvailable(mMmtelNetworkRegType, mMmTelCapabilities);
        boolean oldCallComposerAvailable = isCallComposerAvailable(mMmTelCapabilities);

        boolean volteAvailable = isVolteAvailable(mMmtelNetworkRegType, capabilities);
        boolean voWifiAvailable = isVoWifiAvailable(mMmtelNetworkRegType, capabilities);
        boolean vtAvailable = isVtAvailable(mMmtelNetworkRegType, capabilities);
        boolean viWifiAvailable = isViWifiAvailable(mMmtelNetworkRegType, capabilities);
        boolean callComposerAvailable = isCallComposerAvailable(capabilities);

        logd("updateMmtelCapabilitiesChanged: from " + mMmTelCapabilities + " to " + capabilities);

        // Update to the new mmtel capabilities
        mMmTelCapabilities = deepCopyCapabilities(capabilities);

        if (oldVolteAvailable != volteAvailable
                || oldVoWifiAvailable != voWifiAvailable
                || oldVtAvailable != vtAvailable
                || oldViWifiAvailable != viWifiAvailable
                || oldCallComposerAvailable != callComposerAvailable) {
            return true;
        }
        return false;
    }

    public synchronized void updatePresenceCapable(boolean isCapable) {
        mPresenceCapable = isCapable;
    }

    public synchronized boolean isPresenceCapable() {
        return mPresenceCapable;
    }

    private boolean isVolteAvailable(int networkRegType, MmTelCapabilities capabilities) {
        return (networkRegType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                && capabilities.isCapable(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE);
    }

    private boolean isVoWifiAvailable(int networkRegType, MmTelCapabilities capabilities) {
        return (networkRegType == AccessNetworkConstants.TRANSPORT_TYPE_WLAN)
                && capabilities.isCapable(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE);
    }

    private boolean isVtAvailable(int networkRegType, MmTelCapabilities capabilities) {
        return (networkRegType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                && capabilities.isCapable(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO);
    }

    private boolean isViWifiAvailable(int networkRegType, MmTelCapabilities capabilities) {
        return (networkRegType == AccessNetworkConstants.TRANSPORT_TYPE_WLAN)
                && capabilities.isCapable(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO);
    }

    private boolean isCallComposerAvailable(MmTelCapabilities capabilities) {
        return capabilities.isCapable(
                MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_CALL_COMPOSER);
    }

    /**
     * Get the device's capabilities.
     */
    public synchronized RcsContactUceCapability getDeviceCapabilities(
            @CapabilityMechanism int mechanism, Context context) {
        switch (mechanism) {
            case RcsContactUceCapability.CAPABILITY_MECHANISM_PRESENCE:
                return getPresenceCapabilities(context);
            case RcsContactUceCapability.CAPABILITY_MECHANISM_OPTIONS:
                return getOptionsCapabilities(context);
            default:
                logw("getDeviceCapabilities: invalid mechanism " + mechanism);
                return null;
        }
    }

    // Get the device's capabilities with the PRESENCE mechanism.
    private RcsContactUceCapability getPresenceCapabilities(Context context) {
        Uri uri = PublishUtils.getDeviceContactUri(context, mSubId, this);
        if (uri == null) {
            logw("getPresenceCapabilities: uri is empty");
            return null;
        }
        Set<ServiceDescription> capableFromReg =
                mServiceCapRegTracker.copyRegistrationCapabilities();

        PresenceBuilder presenceBuilder = new PresenceBuilder(uri,
                RcsContactUceCapability.SOURCE_TYPE_CACHED,
                RcsContactUceCapability.REQUEST_RESULT_FOUND);
        // RCS presence tag (added to all presence documents)
        ServiceDescription presDescription = getCustomizedDescription(
                ServiceDescription.SERVICE_DESCRIPTION_PRESENCE, capableFromReg);
        addCapability(presenceBuilder, presDescription.getTupleBuilder(), uri);
        capableFromReg.remove(presDescription);

        // mmtel
        ServiceDescription voiceDescription = getCustomizedDescription(
                ServiceDescription.SERVICE_DESCRIPTION_MMTEL_VOICE, capableFromReg);
        ServiceDescription vtDescription = getCustomizedDescription(
                ServiceDescription.SERVICE_DESCRIPTION_MMTEL_VOICE_VIDEO, capableFromReg);
        ServiceDescription descToUse = (hasVolteCapability() && hasVtCapability()) ?
                vtDescription : voiceDescription;
        ServiceCapabilities servCaps = new ServiceCapabilities.Builder(
                hasVolteCapability(), hasVtCapability())
                .addSupportedDuplexMode(ServiceCapabilities.DUPLEX_MODE_FULL).build();
        addCapability(presenceBuilder, descToUse.getTupleBuilder()
                .setServiceCapabilities(servCaps), uri);
        capableFromReg.remove(voiceDescription);
        capableFromReg.remove(vtDescription);

        // call composer via mmtel
        ServiceDescription composerDescription = getCustomizedDescription(
                ServiceDescription.SERVICE_DESCRIPTION_CALL_COMPOSER_MMTEL, capableFromReg);
        if (hasCallComposerCapability()) {
            addCapability(presenceBuilder, composerDescription.getTupleBuilder(), uri);
        }
        capableFromReg.remove(composerDescription);

        // External features can only be found using registration states from other components.
        // Count these features as capable and include in PIDF XML if they are registered.
        for (ServiceDescription capability : capableFromReg) {
            addCapability(presenceBuilder, capability.getTupleBuilder(), uri);
        }

        return presenceBuilder.build();
    }

    /**
     * Search the refSet for the ServiceDescription that matches the service-id && version and
     * return that or return the reference if there is no match.
     */
    private ServiceDescription getCustomizedDescription(ServiceDescription reference,
            Set<ServiceDescription> refSet) {
        return refSet.stream().filter(s -> s.serviceId.equals(reference.serviceId)
                && s.version.equals(reference.version)).findFirst().orElse(reference);
    }

    // Get the device's capabilities with the OPTIONS mechanism.
    private RcsContactUceCapability getOptionsCapabilities(Context context) {
        Uri uri = PublishUtils.getDeviceContactUri(context, mSubId, this);
        if (uri == null) {
            logw("getOptionsCapabilities: uri is empty");
            return null;
        }

        Set<String> capableFromReg = mServiceCapRegTracker.copyRegistrationFeatureTags();

        OptionsBuilder optionsBuilder = new OptionsBuilder(uri, SOURCE_TYPE_CACHED);
        optionsBuilder.setRequestResult(RcsContactUceCapability.REQUEST_RESULT_FOUND);
        FeatureTags.addFeatureTags(optionsBuilder, hasVolteCapability(), hasVtCapability(),
                isPresenceCapable(), hasCallComposerCapability(), capableFromReg);
        return optionsBuilder.build();
    }

    private void addCapability(RcsContactUceCapability.PresenceBuilder presenceBuilder,
            RcsContactPresenceTuple.Builder tupleBuilder, Uri contactUri) {
        presenceBuilder.addCapabilityTuple(tupleBuilder.setContactUri(contactUri).build());
    }

    // Check if the device has the VoLTE capability
    private synchronized boolean hasVolteCapability() {
        return overrideCapability(FeatureTags.FEATURE_TAG_MMTEL, mMmTelCapabilities != null
                && mMmTelCapabilities.isCapable(MmTelCapabilities.CAPABILITY_TYPE_VOICE));
    }

    // Check if the device has the VT capability
    private synchronized boolean hasVtCapability() {
        return overrideCapability(FeatureTags.FEATURE_TAG_VIDEO, mMmTelCapabilities != null
                && mMmTelCapabilities.isCapable(MmTelCapabilities.CAPABILITY_TYPE_VIDEO));
    }

    // Check if the device has the Call Composer capability
    private synchronized boolean hasCallComposerCapability() {
        return overrideCapability(FeatureTags.FEATURE_TAG_CALL_COMPOSER_VIA_TELEPHONY,
                mMmTelCapabilities != null && mMmTelCapabilities.isCapable(
                        MmTelCapabilities.CAPABILITY_TYPE_CALL_COMPOSER));
    }

    /**
     * @return the overridden value for the provided feature tag or the original capability if there
     * is no override.
     */
    private synchronized boolean overrideCapability(String featureTag, boolean originalCap) {
        if (mOverrideRemoveFeatureTags.contains(featureTag)) {
            return false;
        }

        if (mOverrideAddFeatureTags.contains(featureTag)) {
            return true;
        }

        return originalCap;
    }

    private synchronized MmTelCapabilities deepCopyCapabilities(MmTelCapabilities capabilities) {
        MmTelCapabilities mmTelCapabilities = new MmTelCapabilities();
        if (capabilities.isCapable(MmTelCapabilities.CAPABILITY_TYPE_VOICE)) {
            mmTelCapabilities.addCapabilities(MmTelCapabilities.CAPABILITY_TYPE_VOICE);
        }
        if (capabilities.isCapable(MmTelCapabilities.CAPABILITY_TYPE_VIDEO)) {
            mmTelCapabilities.addCapabilities(MmTelCapabilities.CAPABILITY_TYPE_VIDEO);
        }
        if (capabilities.isCapable(MmTelCapabilities.CAPABILITY_TYPE_UT)) {
            mmTelCapabilities.addCapabilities(MmTelCapabilities.CAPABILITY_TYPE_UT);
        }
        if (capabilities.isCapable(MmTelCapabilities.CAPABILITY_TYPE_SMS)) {
            mmTelCapabilities.addCapabilities(MmTelCapabilities.CAPABILITY_TYPE_SMS);
        }
        if (capabilities.isCapable(MmTelCapabilities.CAPABILITY_TYPE_CALL_COMPOSER)) {
            mmTelCapabilities.addCapabilities(MmTelCapabilities.CAPABILITY_TYPE_CALL_COMPOSER);
        }
        return mmTelCapabilities;
    }

    private void logd(String log) {
        Log.d(LOG_TAG, getLogPrefix().append(log).toString());
        mLocalLog.log("[D] " + log);
    }

    private void logi(String log) {
        Log.i(LOG_TAG, getLogPrefix().append(log).toString());
        mLocalLog.log("[I] " + log);
    }

    private void logw(String log) {
        Log.w(LOG_TAG, getLogPrefix().append(log).toString());
        mLocalLog.log("[W] " + log);
    }

    private StringBuilder getLogPrefix() {
        StringBuilder builder = new StringBuilder("[");
        builder.append(mSubId);
        builder.append("] ");
        return builder;
    }

    public void dump(PrintWriter printWriter) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println("DeviceCapabilityInfo :");
        pw.increaseIndent();

        mServiceCapRegTracker.dump(pw);

        pw.println("Log:");
        pw.increaseIndent();
        mLocalLog.dump(pw);
        pw.decreaseIndent();

        pw.decreaseIndent();
    }
}
