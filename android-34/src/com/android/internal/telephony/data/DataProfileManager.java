/*
 * Copyright 2021 The Android Open Source Project
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

package com.android.internal.telephony.data;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Telephony;
import android.telephony.Annotation;
import android.telephony.Annotation.NetworkType;
import android.telephony.AnomalyReporter;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.telephony.data.DataProfile;
import android.telephony.data.TrafficDescriptor;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.data.DataConfigManager.DataConfigManagerCallback;
import com.android.internal.telephony.data.DataNetworkController.DataNetworkControllerCallback;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * DataProfileManager manages the all {@link DataProfile}s for the current
 * subscription.
 */
public class DataProfileManager extends Handler {
    private static final boolean VDBG = true;

    /** Event for APN database changed. */
    private static final int EVENT_APN_DATABASE_CHANGED = 2;

    /** Event for SIM refresh. */
    private static final int EVENT_SIM_REFRESH = 3;

    private final Phone mPhone;
    private final String mLogTag;
    private final LocalLog mLocalLog = new LocalLog(128);

    /**
     * Should only be used by update updateDataProfiles() to indicate whether resend IA to modem
     * regardless whether IA changed.
     **/
    private final boolean FORCED_UPDATE_IA = true;
    private final boolean ONLY_UPDATE_IA_IF_CHANGED = false;

    /** Data network controller. */
    private final @NonNull DataNetworkController mDataNetworkController;

    /** Data config manager. */
    private final @NonNull DataConfigManager mDataConfigManager;

    /** Cellular data service. */
    private final @NonNull DataServiceManager mWwanDataServiceManager;

    /**
     * All data profiles for the current carrier. Note only data profiles loaded from the APN
     * database will be stored here. The on-demand data profiles (generated dynamically, for
     * example, enterprise data profiles with differentiator) are not stored here.
     */
    private final @NonNull List<DataProfile> mAllDataProfiles = new ArrayList<>();

    /** The data profile used for initial attach. */
    private @Nullable DataProfile mInitialAttachDataProfile = null;

    /** The preferred data profile used for internet. */
    private @Nullable DataProfile mPreferredDataProfile = null;

    /** The last data profile that's successful for internet connection. */
    private @Nullable DataProfile mLastInternetDataProfile = null;

    /** Preferred data profile set id. */
    private int mPreferredDataProfileSetId = Telephony.Carriers.NO_APN_SET_ID;

    /** Data profile manager callbacks. */
    private final @NonNull Set<DataProfileManagerCallback> mDataProfileManagerCallbacks =
            new ArraySet<>();

    /**
     * Data profile manager callback. This should be only used by {@link DataNetworkController}.
     */
    public abstract static class DataProfileManagerCallback extends DataCallback {
        /**
         * Constructor
         *
         * @param executor The executor of the callback.
         */
        public DataProfileManagerCallback(@NonNull @CallbackExecutor Executor executor) {
            super(executor);
        }

        /**
         * Called when data profiles changed.
         */
        public abstract void onDataProfilesChanged();
    }

    /**
     * Constructor
     *
     * @param phone The phone instance.
     * @param dataNetworkController Data network controller.
     * @param dataServiceManager WWAN data service manager.
     * @param looper The looper to be used by the handler. Currently the handler thread is the
     * phone process's main thread.
     * @param callback Data profile manager callback.
     */
    public DataProfileManager(@NonNull Phone phone,
            @NonNull DataNetworkController dataNetworkController,
            @NonNull DataServiceManager dataServiceManager, @NonNull Looper looper,
            @NonNull DataProfileManagerCallback callback) {
        super(looper);
        mPhone = phone;
        mLogTag = "DPM-" + mPhone.getPhoneId();
        mDataNetworkController = dataNetworkController;
        mWwanDataServiceManager = dataServiceManager;
        mDataConfigManager = dataNetworkController.getDataConfigManager();
        mDataProfileManagerCallbacks.add(callback);
        registerAllEvents();
    }

    /**
     * Register for all events that data network controller is interested.
     */
    private void registerAllEvents() {
        mDataNetworkController.registerDataNetworkControllerCallback(
                new DataNetworkControllerCallback(this::post) {
                    @Override
                    public void onInternetDataNetworkConnected(
                            @NonNull List<DataNetwork> internetNetworks) {
                        DataProfileManager.this.onInternetDataNetworkConnected(internetNetworks);
                    }
                });
        mDataConfigManager.registerCallback(new DataConfigManagerCallback(this::post) {
            @Override
            public void onCarrierConfigChanged() {
                DataProfileManager.this.onCarrierConfigUpdated();
            }
        });
        mPhone.getContext().getContentResolver().registerContentObserver(
                Telephony.Carriers.CONTENT_URI, true, new ContentObserver(this) {
                    @Override
                    public void onChange(boolean selfChange) {
                        super.onChange(selfChange);
                        sendEmptyMessage(EVENT_APN_DATABASE_CHANGED);
                    }
                });
        mPhone.mCi.registerForIccRefresh(this, EVENT_SIM_REFRESH, null);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_SIM_REFRESH:
                log("Update data profiles due to SIM refresh.");
                updateDataProfiles(FORCED_UPDATE_IA);
                break;
            case EVENT_APN_DATABASE_CHANGED:
                log("Update data profiles due to APN db updated.");
                updateDataProfiles(ONLY_UPDATE_IA_IF_CHANGED);
                break;
            default:
                loge("Unexpected event " + msg);
                break;
        }
    }

    /**
     * Called when carrier config was updated.
     */
    private void onCarrierConfigUpdated() {
        log("Update data profiles due to carrier config updated.");
        updateDataProfiles(FORCED_UPDATE_IA);

        //TODO: more works needed to be done here.
    }

    /**
     * Check if there are any Enterprise APN configured by DPC and return a data profile
     * with the same.
     * @return data profile with enterprise ApnSetting if available, else null
     */
    @Nullable private DataProfile getEnterpriseDataProfile() {
        Cursor cursor = mPhone.getContext().getContentResolver().query(
                Telephony.Carriers.DPC_URI, null, null, null, null);
        if (cursor == null) {
            loge("Cannot access APN database through telephony provider.");
            return null;
        }

        DataProfile dataProfile = null;
        while (cursor.moveToNext()) {
            ApnSetting apn = ApnSetting.makeApnSetting(cursor);
            if (apn != null) {
                dataProfile = new DataProfile.Builder()
                        .setApnSetting(apn)
                        .setTrafficDescriptor(new TrafficDescriptor(apn.getApnName(), null))
                        .setPreferred(false)
                        .build();
                if (dataProfile.canSatisfy(NetworkCapabilities.NET_CAPABILITY_ENTERPRISE)) {
                    break;
                }
            }
        }
        cursor.close();
        return dataProfile;
    }
    /**
     * Update all data profiles, including preferred data profile, and initial attach data profile.
     * Also send those profiles down to the modem if needed.
     *
     * @param forceUpdateIa If {@code true}, we should always send IA again to modem.
     */
    private void updateDataProfiles(boolean forceUpdateIa) {
        List<DataProfile> profiles = new ArrayList<>();
        if (mDataConfigManager.isConfigCarrierSpecific()) {
            Cursor cursor = mPhone.getContext().getContentResolver().query(
                    Uri.withAppendedPath(Telephony.Carriers.SIM_APN_URI, "filtered/subId/"
                            + mPhone.getSubId()), null, null, null, Telephony.Carriers._ID);
            if (cursor == null) {
                loge("Cannot access APN database through telephony provider.");
                return;
            }
            boolean isInternetSupported = false;
            while (cursor.moveToNext()) {
                ApnSetting apn = ApnSetting.makeApnSetting(cursor);
                if (apn != null) {
                    DataProfile dataProfile = new DataProfile.Builder()
                            .setApnSetting(apn)
                            .setTrafficDescriptor(new TrafficDescriptor(apn.getApnName(), null))
                            .setPreferred(false)
                            .build();
                    profiles.add(dataProfile);
                    log("Added " + dataProfile);

                    isInternetSupported |= apn.canHandleType(ApnSetting.TYPE_DEFAULT);
                    if (mDataConfigManager.isApnConfigAnomalyReportEnabled()) {
                        checkApnSetting(apn);
                    }
                }
            }
            cursor.close();

            if (!isInternetSupported
                    && !profiles.isEmpty() // APN database has been read successfully
                    && mDataConfigManager.isApnConfigAnomalyReportEnabled()) {
                reportAnomaly("Carrier doesn't support internet.",
                        "9af73e18-b523-4dc5-adab-363eb6613305");
            }
        }

        DataProfile dataProfile;

        if (!profiles.isEmpty()) { // APN database has been read successfully after SIM loaded
            // Check if any of the profile already supports IMS, if not, add the default one.
            dataProfile = profiles.stream()
                    .filter(dp -> dp.canSatisfy(NetworkCapabilities.NET_CAPABILITY_IMS))
                    .findFirst()
                    .orElse(null);
            if (dataProfile == null) {
                profiles.add(new DataProfile.Builder()
                        .setApnSetting(buildDefaultApnSetting("DEFAULT IMS", "ims",
                                ApnSetting.TYPE_IMS))
                        .setTrafficDescriptor(new TrafficDescriptor("ims", null))
                        .build());
                log("Added default IMS data profile.");
            }
        }

        // Check if any of the profile already supports ENTERPRISE, if not, check if DPC has
        // configured one and retrieve the same.
        dataProfile = profiles.stream()
                .filter(dp -> dp.canSatisfy(NetworkCapabilities.NET_CAPABILITY_ENTERPRISE))
                .findFirst()
                .orElse(null);
        if (dataProfile == null) {
            dataProfile = getEnterpriseDataProfile();
            if (dataProfile != null) {
                profiles.add(dataProfile);
                log("Added enterprise profile " + dataProfile);
            }
        }

        // Check if any of the profile already supports EIMS, if not, add the default one.
        dataProfile = profiles.stream()
                .filter(dp -> dp.canSatisfy(NetworkCapabilities.NET_CAPABILITY_EIMS))
                .findFirst()
                .orElse(null);
        if (dataProfile == null) {
            profiles.add(new DataProfile.Builder()
                    .setApnSetting(buildDefaultApnSetting("DEFAULT EIMS", "sos",
                            ApnSetting.TYPE_EMERGENCY))
                    .setTrafficDescriptor(new TrafficDescriptor("sos", null))
                    .build());
            log("Added default EIMS data profile.");
        }

        dedupeDataProfiles(profiles);

        if (mDataConfigManager.isApnConfigAnomalyReportEnabled()) {
            checkDataProfiles(profiles);
        }

        log("Found " + profiles.size() + " data profiles. profiles = " + profiles);

        boolean profilesChanged = false;
        if (mAllDataProfiles.size() != profiles.size() || !mAllDataProfiles.containsAll(profiles)) {
            log("Data profiles changed.");
            mAllDataProfiles.clear();
            mAllDataProfiles.addAll(profiles);
            profilesChanged = true;
        }

        // Reload the latest preferred data profile from either database or config.
        profilesChanged |= updatePreferredDataProfile();

        int setId = getPreferredDataProfileSetId();
        if (setId != mPreferredDataProfileSetId) {
            logl("Changed preferred data profile set id to " + setId);
            mPreferredDataProfileSetId = setId;
            profilesChanged = true;
        }

        updateDataProfilesAtModem();
        updateInitialAttachDataProfileAtModem(forceUpdateIa);

        if (profilesChanged) {
            mDataProfileManagerCallbacks.forEach(callback -> callback.invokeFromExecutor(
                    callback::onDataProfilesChanged));
        }
    }

    /**
     * @return The preferred data profile set id.
     */
    private int getPreferredDataProfileSetId() {
        // Query the preferred APN set id. The set id is automatically set when we set by
        // TelephonyProvider when setting preferred APN in setPreferredDataProfile().
        Cursor cursor = mPhone.getContext().getContentResolver()
                .query(Uri.withAppendedPath(Telephony.Carriers.PREFERRED_APN_SET_URI,
                        String.valueOf(mPhone.getSubId())),
                        new String[] {Telephony.Carriers.APN_SET_ID}, null, null, null);
        // Returns all APNs for the current carrier which have an apn_set_id
        // equal to the preferred APN (if no preferred APN, or if the preferred APN has no set id,
        // the query will return null)
        if (cursor == null) {
            log("getPreferredDataProfileSetId: cursor is null");
            return Telephony.Carriers.NO_APN_SET_ID;
        }

        int setId;
        if (cursor.getCount() < 1) {
            loge("getPreferredDataProfileSetId: no APNs found");
            setId = Telephony.Carriers.NO_APN_SET_ID;
        } else {
            cursor.moveToFirst();
            setId = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.APN_SET_ID));
        }

        cursor.close();
        return setId;
    }

    /**
     * Called when internet data is connected.
     *
     * @param internetNetworks The connected internet data networks.
     */
    private void onInternetDataNetworkConnected(@NonNull List<DataNetwork> internetNetworks) {
        DataProfile defaultProfile = null;
        if (internetNetworks.size() == 1) {
            // Most of the cases there should be only one.
            defaultProfile = internetNetworks.get(0).getDataProfile();
        } else if (internetNetworks.size() > 1) {
            // but in case there are multiple, find the default internet network, and choose the
            // one which has longest life cycle.
            logv("onInternetDataNetworkConnected: mPreferredDataProfile=" + mPreferredDataProfile
                    + " internetNetworks=" + internetNetworks);
            defaultProfile = internetNetworks.stream()
                    .filter(network -> mPreferredDataProfile == null
                            || canPreferredDataProfileSatisfy(
                            network.getAttachedNetworkRequestList()))
                    .map(DataNetwork::getDataProfile)
                    .min(Comparator.comparingLong(DataProfile::getLastSetupTimestamp))
                    .orElse(null);
        }

        // Update a working internet data profile as a future candidate for preferred data profile
        // after APNs are reset to default
        mLastInternetDataProfile = defaultProfile;

        // If the live default internet network is not using the preferred data profile, since
        // brought up a network means it passed sophisticated checks, update the preferred data
        // profile so that this network won't be torn down in future network evaluations.
        if (defaultProfile == null || defaultProfile.equals(mPreferredDataProfile)) return;
        // Save the preferred data profile into database.
        setPreferredDataProfile(defaultProfile);
        updateDataProfiles(ONLY_UPDATE_IA_IF_CHANGED);
    }

    /**
     * Get the preferred data profile for internet data.
     *
     * @return The preferred data profile.
     */
    private @Nullable DataProfile getPreferredDataProfileFromDb() {
        Cursor cursor = mPhone.getContext().getContentResolver().query(
                Uri.withAppendedPath(Telephony.Carriers.PREFERRED_APN_URI,
                        String.valueOf(mPhone.getSubId())), null, null, null,
                Telephony.Carriers.DEFAULT_SORT_ORDER);
        DataProfile dataProfile = null;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                int apnId = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID));
                dataProfile = mAllDataProfiles.stream()
                        .filter(dp -> dp.getApnSetting() != null
                                && dp.getApnSetting().getId() == apnId)
                        .findFirst()
                        .orElse(null);
            }
            cursor.close();
        }
        log("getPreferredDataProfileFromDb: " + dataProfile);
        return dataProfile;
    }

    /**
     * @return The preferred data profile from carrier config.
     */
    private @Nullable DataProfile getPreferredDataProfileFromConfig() {
        // Check if there is configured default preferred data profile.
        String defaultPreferredApn = mDataConfigManager.getDefaultPreferredApn();
        if (!TextUtils.isEmpty(defaultPreferredApn)) {
            return mAllDataProfiles.stream()
                    .filter(dp -> dp.getApnSetting() != null && defaultPreferredApn.equals(
                                    dp.getApnSetting().getApnName()))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * Save the preferred data profile into the database.
     *
     * @param dataProfile The preferred data profile used for internet data. {@code null} to clear
     * the preferred data profile from database.
     */
    private void setPreferredDataProfile(@Nullable DataProfile dataProfile) {
        logl("setPreferredDataProfile: " + dataProfile);

        String subId = Long.toString(mPhone.getSubId());
        Uri uri = Uri.withAppendedPath(Telephony.Carriers.PREFERRED_APN_URI, subId);
        ContentResolver resolver = mPhone.getContext().getContentResolver();
        resolver.delete(uri, null, null);

        if (dataProfile != null && dataProfile.getApnSetting() != null) {
            ContentValues values = new ContentValues();
            // Fill only the id here. TelephonyProvider will pull the rest of key fields and write
            // into the database.
            values.put(Telephony.Carriers.APN_ID, dataProfile.getApnSetting().getId());
            resolver.insert(uri, values);
        }
    }

    /**
     * Reload the latest preferred data profile from either database or the config. This is to
     * make sure the cached {@link #mPreferredDataProfile} is in-sync.
     *
     * @return {@code true} if preferred data profile changed.
     */
    private boolean updatePreferredDataProfile() {
        DataProfile preferredDataProfile;
        if (SubscriptionManager.isValidSubscriptionId(mPhone.getSubId())) {
            preferredDataProfile = getPreferredDataProfileFromDb();
            if (preferredDataProfile == null) {
                preferredDataProfile = getPreferredDataProfileFromConfig();
                if (preferredDataProfile != null) {
                    // Save the carrier specified preferred data profile into database
                    setPreferredDataProfile(preferredDataProfile);
                } else {
                    preferredDataProfile = mAllDataProfiles.stream()
                            .filter(dp -> areDataProfilesSharingApn(dp, mLastInternetDataProfile))
                            .findFirst()
                            .orElse(null);
                    if (preferredDataProfile != null) {
                        log("updatePreferredDataProfile: preferredDB is empty and no carrier "
                                + "default configured, setting preferred to be prev internet DP.");
                        setPreferredDataProfile(preferredDataProfile);
                    }
                }
            }
        } else {
            preferredDataProfile = null;
        }

        for (DataProfile dataProfile : mAllDataProfiles) {
            dataProfile.setPreferred(dataProfile.equals(preferredDataProfile));
        }

        if (!Objects.equals(mPreferredDataProfile, preferredDataProfile)) {
            mPreferredDataProfile = preferredDataProfile;

            logl("Changed preferred data profile to " + mPreferredDataProfile);
            return true;
        }
        return false;
    }

    /**
     * Update the data profile used for initial attach.
     *
     * Note that starting from Android 13 only APNs that supports "IA" type will be used for
     * initial attach. Please update APN configuration file if needed.
     *
     * Some carriers might explicitly require that using "user-added" APN for initial
     * attach. In this case, exception can be configured through
     * {@link CarrierConfigManager#KEY_ALLOWED_INITIAL_ATTACH_APN_TYPES_STRING_ARRAY}.
     *
     * @param forceUpdateIa If {@code true}, we should always send IA again to modem.
     */
    private void updateInitialAttachDataProfileAtModem(boolean forceUpdateIa) {
        DataProfile initialAttachDataProfile = null;

        // Sort the data profiles so the preferred data profile is at the beginning.
        List<DataProfile> allDataProfiles = mAllDataProfiles.stream()
                .sorted(Comparator.comparing((DataProfile dp) -> !dp.equals(mPreferredDataProfile)))
                .collect(Collectors.toList());
        // Search in the order. "IA" type should be the first from getAllowedInitialAttachApnTypes.
        for (int apnType : mDataConfigManager.getAllowedInitialAttachApnTypes()) {
            initialAttachDataProfile = allDataProfiles.stream()
                    .filter(dp -> dp.canSatisfy(DataUtils.apnTypeToNetworkCapability(apnType)))
                    .findFirst()
                    .orElse(null);
            if (initialAttachDataProfile != null) break;
        }

        if (forceUpdateIa || !Objects.equals(mInitialAttachDataProfile, initialAttachDataProfile)) {
            mInitialAttachDataProfile = initialAttachDataProfile;
            logl("Initial attach data profile updated as " + mInitialAttachDataProfile
                    + " or forceUpdateIa= " + forceUpdateIa);
            // TODO: Push the null data profile to modem on new AIDL HAL. Modem should clear the IA
            //  APN, tracking for U b/227579876, now using forceUpdateIa which always push to modem
            if (mInitialAttachDataProfile != null) {
                mWwanDataServiceManager.setInitialAttachApn(mInitialAttachDataProfile,
                        mPhone.getServiceState().getDataRoamingFromRegistration(), null);
            }
        }
    }

    /**
     * Update the data profiles at modem.
     */
    private void updateDataProfilesAtModem() {
        log("updateDataProfilesAtModem: set " + mAllDataProfiles.size() + " data profiles.");
        mWwanDataServiceManager.setDataProfile(mAllDataProfiles,
                mPhone.getServiceState().getDataRoamingFromRegistration(), null);
    }

    /**
     * Create default apn settings for the apn type like emergency, and ims
     *
     * @param entry Entry name
     * @param apn APN name
     * @param apnTypeBitmask APN type
     * @return The APN setting
     */
    private @NonNull ApnSetting buildDefaultApnSetting(@NonNull String entry,
            @NonNull String apn, @Annotation.ApnType int apnTypeBitmask) {
        return new ApnSetting.Builder()
                .setEntryName(entry)
                .setProtocol(ApnSetting.PROTOCOL_IPV4V6)
                .setRoamingProtocol(ApnSetting.PROTOCOL_IPV4V6)
                .setApnName(apn)
                .setApnTypeBitmask(apnTypeBitmask)
                .setCarrierEnabled(true)
                .setApnSetId(Telephony.Carriers.MATCH_ALL_APN_SET_ID)
                .build();
    }

    /**
     * Get the data profile that can satisfy the network request.
     *
     * @param networkRequest The network request.
     * @param networkType The current data network type.
     * @param ignorePermanentFailure {@code true} to ignore {@link ApnSetting#getPermanentFailed()}.
     * This should be set to true for condition-based retry/setup.
     * @return The data profile. {@code null} if can't find any satisfiable data profile.
     */
    public @Nullable DataProfile getDataProfileForNetworkRequest(
            @NonNull TelephonyNetworkRequest networkRequest, @NetworkType int networkType,
            boolean ignorePermanentFailure) {
        ApnSetting apnSetting = null;
        if (networkRequest.hasAttribute(TelephonyNetworkRequest
                .CAPABILITY_ATTRIBUTE_APN_SETTING)) {
            apnSetting = getApnSettingForNetworkRequest(networkRequest, networkType,
                    ignorePermanentFailure);
        }

        TrafficDescriptor.Builder trafficDescriptorBuilder = new TrafficDescriptor.Builder();
        if (networkRequest.hasAttribute(TelephonyNetworkRequest
                .CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_DNN)) {
            if (apnSetting != null) {
                trafficDescriptorBuilder.setDataNetworkName(apnSetting.getApnName());
            }
        }

        if (networkRequest.hasAttribute(
                TelephonyNetworkRequest.CAPABILITY_ATTRIBUTE_TRAFFIC_DESCRIPTOR_OS_APP_ID)) {
            TrafficDescriptor.OsAppId osAppId = networkRequest.getOsAppId();
            if (osAppId != null) {
                trafficDescriptorBuilder.setOsAppId(osAppId.getBytes());
            }
        }

        TrafficDescriptor trafficDescriptor;
        try {
            trafficDescriptor = trafficDescriptorBuilder.build();
        } catch (IllegalArgumentException e) {
            // We reach here when both ApnSetting and trafficDescriptor are null.
            log("Unable to find a data profile for " + networkRequest);
            return null;
        }

        // Instead of building the data profile from APN setting and traffic descriptor on-the-fly,
        // find the existing one from mAllDataProfiles so the last-setup timestamp can be retained.
        // Only create a new one when it can't be found.
        for (DataProfile dataProfile : mAllDataProfiles) {
            if (Objects.equals(apnSetting, dataProfile.getApnSetting())
                    && trafficDescriptor.equals(dataProfile.getTrafficDescriptor())) {
                return dataProfile;
            }
        }

        // When reaching here, it means that we have a valid non-null traffic descriptor, but
        // could not find it in mAllDataProfiles. This could happen on the traffic descriptor
        // capable capabilities like ENTERPRISE.
        DataProfile.Builder profileBuilder = new DataProfile.Builder();
        if (apnSetting != null) {
            profileBuilder.setApnSetting(apnSetting);
        }

        // trafficDescriptor is always non-null when we reach here.
        profileBuilder.setTrafficDescriptor(trafficDescriptor);

        DataProfile dataProfile = profileBuilder.build();
        log("Created data profile " + dataProfile + " for " + networkRequest);
        return dataProfile;
    }

    /**
     * Get the APN setting for the network request.
     *
     * @param networkRequest The network request.
     * @param networkType The current data network type.
     * @param ignorePermanentFailure {@code true} to ignore {@link ApnSetting#getPermanentFailed()}.
     * This should be set to true for condition-based retry/setup.
     * @return The APN setting. {@code null} if can't find any satisfiable data profile.
     */
    private @Nullable ApnSetting getApnSettingForNetworkRequest(
            @NonNull TelephonyNetworkRequest networkRequest, @NetworkType int networkType,
            boolean ignorePermanentFailure) {
        if (!networkRequest.hasAttribute(
                TelephonyNetworkRequest.CAPABILITY_ATTRIBUTE_APN_SETTING)) {
            loge("Network request does not have APN setting attribute.");
            return null;
        }

        // If the preferred data profile can be used, always use it if it can satisfy the network
        // request with current network type (even though it's been marked as permanent failed.)
        if (mPreferredDataProfile != null
                && networkRequest.canBeSatisfiedBy(mPreferredDataProfile)
                && mPreferredDataProfile.getApnSetting() != null
                && mPreferredDataProfile.getApnSetting().canSupportNetworkType(networkType)) {
            if (ignorePermanentFailure || !mPreferredDataProfile.getApnSetting()
                    .getPermanentFailed()) {
                return mPreferredDataProfile.getApnSetting();
            }
            log("The preferred data profile is permanently failed. Only condition based retry "
                    + "can happen.");
            return null;
        }

        // Filter out the data profile that can't satisfy the request.
        // Preferred data profile should be returned in the top of the list.
        List<DataProfile> dataProfiles = mAllDataProfiles.stream()
                .filter(networkRequest::canBeSatisfiedBy)
                // The longest time hasn't used data profile will be in the front so all the data
                // profiles can be tried.
                .sorted(Comparator.comparing(DataProfile::getLastSetupTimestamp))
                .collect(Collectors.toList());
        for (DataProfile dataProfile : dataProfiles) {
            logv("Satisfied profile: " + dataProfile + ", last setup="
                    + DataUtils.elapsedTimeToString(dataProfile.getLastSetupTimestamp()));
        }
        if (dataProfiles.size() == 0) {
            log("Can't find any data profile that can satisfy " + networkRequest);
            return null;
        }

        // Check if the remaining data profiles can used in current data network type.
        dataProfiles = dataProfiles.stream()
                .filter(dp -> dp.getApnSetting() != null
                        && dp.getApnSetting().canSupportNetworkType(networkType))
                .collect(Collectors.toList());
        if (dataProfiles.size() == 0) {
            log("Can't find any data profile for network type "
                    + TelephonyManager.getNetworkTypeName(networkType));
            return null;
        }

        // Check if preferred data profile set id matches.
        dataProfiles = dataProfiles.stream()
                .filter(dp -> dp.getApnSetting() != null
                        && (dp.getApnSetting().getApnSetId()
                        == Telephony.Carriers.MATCH_ALL_APN_SET_ID
                        || dp.getApnSetting().getApnSetId() == mPreferredDataProfileSetId))
                .collect(Collectors.toList());
        if (dataProfiles.size() == 0) {
            log("Can't find any data profile has APN set id matched. mPreferredDataProfileSetId="
                    + mPreferredDataProfileSetId);
            return null;
        }

        // Check if data profiles are permanently failed.
        dataProfiles = dataProfiles.stream()
                .filter(dp -> ignorePermanentFailure || !dp.getApnSetting().getPermanentFailed())
                .collect(Collectors.toList());
        if (dataProfiles.size() == 0) {
            log("The suitable data profiles are all in permanent failed state.");
            return null;
        }

        return dataProfiles.get(0).getApnSetting();
    }

    /**
     * Check if the data profile is essentially the preferred data profile. The non-essential
     * elements include e.g.APN Id.
     *
     * @param dataProfile The data profile to check.
     * @return {@code true} if the data profile is essentially the preferred data profile.
     */
    public boolean isDataProfilePreferred(@NonNull DataProfile dataProfile) {
        return areDataProfilesSharingApn(dataProfile, mPreferredDataProfile);
    }

    /**
     * @param networkRequests The required network requests
     * @return {@code true} if we currently have a preferred data profile that's capable of
     * satisfying the required network requests; {@code false} if we have no preferred, or the
     * preferred cannot satisfy the required requests.
     */
    public boolean canPreferredDataProfileSatisfy(
            @NonNull DataNetworkController.NetworkRequestList networkRequests) {
        return mPreferredDataProfile != null && networkRequests.stream()
                .allMatch(request -> request.canBeSatisfiedBy(mPreferredDataProfile));
    }

    /**
     * Check if there is tethering data profile for certain network type.
     *
     * @param networkType The network type
     * @return {@code true} if tethering data profile is found. {@code false} if no specific profile
     * should used for tethering. In this case, tethering service will use internet network for
     * tethering.
     */
    public boolean isTetheringDataProfileExisting(@NetworkType int networkType) {
        if (mDataConfigManager.isTetheringProfileDisabledForRoaming()
                && mPhone.getServiceState().getDataRoaming()) {
            // Use internet network for tethering.
            return false;
        }
        TelephonyNetworkRequest networkRequest = new TelephonyNetworkRequest(
                new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_DUN)
                        .build(), mPhone);
        return getDataProfileForNetworkRequest(networkRequest, networkType, true) != null;
    }

    /**
     * Dedupe the similar data profiles.
     */
    private void dedupeDataProfiles(@NonNull List<DataProfile> dataProfiles) {
        int i = 0;
        while (i < dataProfiles.size() - 1) {
            DataProfile first = dataProfiles.get(i);
            int j = i + 1;
            while (j < dataProfiles.size()) {
                DataProfile second = dataProfiles.get(j);
                DataProfile merged = mergeDataProfiles(first, second);
                if (merged != null) {
                    log("Created a merged profile " + merged + " from " + first + " and "
                            + second);
                    loge("Merging data profiles will not be supported anymore. Please "
                            + "directly configure the merged profile " + merged + " in the APN "
                            + "config.");
                    dataProfiles.set(i, merged);
                    dataProfiles.remove(j);
                } else {
                    j++;
                }
            }
            i++;
        }
    }

    /**
     * Trigger anomaly report if APN Setting contains invalid info.
     *
     * @param setting The Apn setting to be checked.
     */
    private void checkApnSetting(@NonNull ApnSetting setting) {
        if (setting.canHandleType(ApnSetting.TYPE_MMS)) {
            if (setting.getMmsc() == null) {
                reportAnomaly("MMS is supported but no MMSC configured " + setting,
                        "9af73e18-b523-4dc5-adab-19d86c6a3685");
            } else if (!setting.getMmsc().toString().matches("^https?:\\/\\/.+")) {
                reportAnomaly("Apn config mmsc should start with http but is "
                                + setting.getMmsc(),
                        "9af73e18-b523-4dc5-adab-ec754d959d4d");
            }
            if (!TextUtils.isEmpty(setting.getMmsProxyAddressAsString())
                    && setting.getMmsProxyAddressAsString().matches("^https?:\\/\\/.+")) {
                reportAnomaly("Apn config mmsc_proxy should NOT start with http but is "
                                + setting.getMmsc(), "9af73e18-b523-4dc5-adab-ec754d959d4d");
            }
        }
    }

    /**
     * Trigger anomaly report if any two Apn Settings share the same APN name while having
     * overlapped network types.
     *
     * @param profiles The list of data profiles to be checked.
     */
    private void checkDataProfiles(List<DataProfile> profiles) {
        for (int i = 0; i < profiles.size(); i++) {
            ApnSetting a = profiles.get(i).getApnSetting();
            if (a == null) continue;
            if (// Lingering network is not the default and doesn't cover all the regular networks
                    (int) TelephonyManager.NETWORK_TYPE_BITMASK_UNKNOWN
                    != a.getLingeringNetworkTypeBitmask()
                            && (a.getNetworkTypeBitmask() | a.getLingeringNetworkTypeBitmask())
                    != a.getLingeringNetworkTypeBitmask()) {
                reportAnomaly("Apn[" + a.getApnName() + "] network "
                                + TelephonyManager.convertNetworkTypeBitmaskToString(
                                        a.getNetworkTypeBitmask()) + " should be a subset of "
                                + "the lingering network "
                                + TelephonyManager.convertNetworkTypeBitmaskToString(
                                a.getLingeringNetworkTypeBitmask()),
                        "9af73e18-b523-4dc5-adab-4bb24355d838");
            }
            for (int j = i + 1; j < profiles.size(); j++) {
                ApnSetting b = profiles.get(j).getApnSetting();
                if (b == null) continue;
                String apnNameA = a.getApnName();
                String apnNameB = b.getApnName();
                if (TextUtils.equals(apnNameA, apnNameB)
                        // TelephonyManager.NETWORK_TYPE_BITMASK_UNKNOWN means all network types
                        && (a.getNetworkTypeBitmask()
                        == (int) TelephonyManager.NETWORK_TYPE_BITMASK_UNKNOWN
                        || b.getNetworkTypeBitmask()
                        == (int) TelephonyManager.NETWORK_TYPE_BITMASK_UNKNOWN
                        || (a.getNetworkTypeBitmask() & b.getNetworkTypeBitmask()) != 0)) {
                    reportAnomaly("Found overlapped network type under the APN name "
                                    + a.getApnName(),
                            "9af73e18-b523-4dc5-adab-4bb24555d839");
                }
            }
        }
    }

    /**
     * Merge two data profiles if possible.
     *
     * @param dp1 Data profile 1 to be merged.
     * @param dp2 Data profile 2 to be merged.
     *
     * @return The merged data profile. {@code null} if merging is not possible.
     */
    private static @Nullable DataProfile mergeDataProfiles(
            @NonNull DataProfile dp1, @NonNull DataProfile dp2) {
        Objects.requireNonNull(dp1);
        Objects.requireNonNull(dp2);

        // We don't merge data profiles that have different traffic descriptor.
        if (!Objects.equals(dp1.getTrafficDescriptor(), dp2.getTrafficDescriptor())) return null;

        // If one of the APN setting is null, we don't merge.
        if (dp1.getApnSetting() == null || dp2.getApnSetting() == null) return null;

        // If two APN settings are not similar, we don't merge.
        if (!dp1.getApnSetting().similar(dp2.getApnSetting())) return null;

        // Start to merge APN setting 1 and 2.
        ApnSetting apn1 = dp1.getApnSetting();
        ApnSetting apn2 = dp2.getApnSetting();
        ApnSetting.Builder apnBuilder = new ApnSetting.Builder();

        // Special handling id and entry name. We want to keep the default APN as it could be the
        // preferred APN.
        apnBuilder.setId(apn1.getId());
        apnBuilder.setEntryName(apn1.getEntryName());
        if (apn2.canHandleType(ApnSetting.TYPE_DEFAULT)
                && !apn1.canHandleType(ApnSetting.TYPE_DEFAULT)) {
            apnBuilder.setId(apn2.getId());
            apnBuilder.setEntryName(apn2.getEntryName());
        }

        // Merge the following fields from apn1 and apn2.
        apnBuilder.setProxyAddress(TextUtils.isEmpty(apn2.getProxyAddressAsString())
                ? apn1.getProxyAddressAsString() : apn2.getProxyAddressAsString());
        apnBuilder.setProxyPort(apn2.getProxyPort() == -1
                ? apn1.getProxyPort() : apn2.getProxyPort());
        apnBuilder.setMmsc(apn2.getMmsc() == null ? apn1.getMmsc() : apn2.getMmsc());
        apnBuilder.setMmsProxyAddress(TextUtils.isEmpty(apn2.getMmsProxyAddressAsString())
                ? apn1.getMmsProxyAddressAsString() : apn2.getMmsProxyAddressAsString());
        apnBuilder.setMmsProxyPort(apn2.getMmsProxyPort() == -1
                ? apn1.getMmsProxyPort() : apn2.getMmsProxyPort());
        apnBuilder.setUser(TextUtils.isEmpty(apn2.getUser()) ? apn1.getUser() : apn2.getUser());
        apnBuilder.setPassword(TextUtils.isEmpty(apn2.getPassword())
                ? apn1.getPassword() : apn2.getPassword());
        apnBuilder.setAuthType(apn2.getAuthType() == -1
                ? apn1.getAuthType() : apn2.getAuthType());
        apnBuilder.setApnTypeBitmask(apn1.getApnTypeBitmask() | apn2.getApnTypeBitmask());
        apnBuilder.setMtuV4(apn2.getMtuV4() <= ApnSetting.UNSET_MTU
                ? apn1.getMtuV4() : apn2.getMtuV4());
        apnBuilder.setMtuV6(apn2.getMtuV6() <= ApnSetting.UNSET_MTU
                ? apn1.getMtuV6() : apn2.getMtuV6());
        // legacy properties that don't matter
        apnBuilder.setMvnoType(apn1.getMvnoType());
        apnBuilder.setMvnoMatchData(apn1.getMvnoMatchData());

        // The following fields in apn1 and apn2 should be the same, otherwise ApnSetting.similar()
        // should fail earlier.
        apnBuilder.setApnName(apn1.getApnName());
        apnBuilder.setProtocol(apn1.getProtocol());
        apnBuilder.setRoamingProtocol(apn1.getRoamingProtocol());
        apnBuilder.setCarrierEnabled(apn1.isEnabled());
        apnBuilder.setNetworkTypeBitmask(apn1.getNetworkTypeBitmask());
        apnBuilder.setLingeringNetworkTypeBitmask(apn1.getLingeringNetworkTypeBitmask());
        apnBuilder.setProfileId(apn1.getProfileId());
        apnBuilder.setPersistent(apn1.isPersistent());
        apnBuilder.setMaxConns(apn1.getMaxConns());
        apnBuilder.setWaitTime(apn1.getWaitTime());
        apnBuilder.setMaxConnsTime(apn1.getMaxConnsTime());
        apnBuilder.setApnSetId(apn1.getApnSetId());
        apnBuilder.setCarrierId(apn1.getCarrierId());
        apnBuilder.setSkip464Xlat(apn1.getSkip464Xlat());
        apnBuilder.setAlwaysOn(apn1.isAlwaysOn());

        return new DataProfile.Builder()
                .setApnSetting(apnBuilder.build())
                .setTrafficDescriptor(dp1.getTrafficDescriptor())
                .build();
    }

    /**
     * Check if the provided data profile is still compatible with the current environment. Note
     * this method ignores APN id check and traffic descriptor check. A data profile with traffic
     * descriptor only can always be used in any condition.
     *
     * @param dataProfile The data profile to check.
     * @return {@code true} if the provided data profile can be still used in current environment.
     */
    public boolean isDataProfileCompatible(@NonNull DataProfile dataProfile) {
        if (dataProfile == null) {
            return false;
        }

        if (dataProfile.getApnSetting() == null && dataProfile.getTrafficDescriptor() != null) {
            // A traffic descriptor only data profile can be always used. Traffic descriptors are
            // always generated on the fly instead loaded from the database.
            return true;
        }

        // Check the APN from the profile is compatible and matches preferred data profile set id.
        return mAllDataProfiles.stream()
                .filter(dp -> dp.getApnSetting() != null
                        && (dp.getApnSetting().getApnSetId()
                        == Telephony.Carriers.MATCH_ALL_APN_SET_ID
                        || dp.getApnSetting().getApnSetId() == mPreferredDataProfileSetId))
                .anyMatch(dp -> areDataProfilesSharingApn(dataProfile, dp));
    }

    /**
     * @return {@code true} if both data profiles' APN setting are non-null and essentially the same
     * (non-essential elements include e.g.APN Id).
     */
    public boolean areDataProfilesSharingApn(@Nullable DataProfile a, @Nullable DataProfile b) {
        return a != null
                && b != null
                && a.getApnSetting() != null
                && a.getApnSetting().equals(b.getApnSetting(),
                mPhone.getServiceState().getDataRoamingFromRegistration());
    }

    /**
     * Register the callback for receiving information from {@link DataProfileManager}.
     *
     * @param callback The callback.
     */
    public void registerCallback(@NonNull DataProfileManagerCallback callback) {
        mDataProfileManagerCallbacks.add(callback);
    }

    /**
     * Unregister the previously registered {@link DataProfileManagerCallback}.
     *
     * @param callback The callback to unregister.
     */
    public void unregisterCallback(@NonNull DataProfileManagerCallback callback) {
        mDataProfileManagerCallbacks.remove(callback);
    }

    /**
     * Trigger the anomaly report with the specified UUID.
     *
     * @param anomalyMsg Description of the event
     * @param uuid UUID associated with that event
     */
    private void reportAnomaly(@NonNull String anomalyMsg, @NonNull String uuid) {
        logl(anomalyMsg);
        AnomalyReporter.reportAnomaly(UUID.fromString(uuid), anomalyMsg, mPhone.getCarrierId());
    }

    /**
     * Log debug messages.
     * @param s debug messages
     */
    private void log(@NonNull String s) {
        Rlog.d(mLogTag, s);
    }

    /**
     * Log error messages.
     * @param s error messages
     */
    private void loge(@NonNull String s) {
        Rlog.e(mLogTag, s);
    }

    /**
     * Log verbose messages.
     * @param s debug messages.
     */
    private void logv(@NonNull String s) {
        if (VDBG) Rlog.v(mLogTag, s);
    }

    /**
     * Log debug messages and also log into the local log.
     * @param s debug messages
     */
    private void logl(@NonNull String s) {
        log(s);
        mLocalLog.log(s);
    }

    /**
     * Dump the state of DataProfileManager
     *
     * @param fd File descriptor
     * @param printWriter Print writer
     * @param args Arguments
     */
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println(DataProfileManager.class.getSimpleName() + "-" + mPhone.getPhoneId() + ":");
        pw.increaseIndent();

        pw.println("Data profiles for the current carrier:");
        pw.increaseIndent();
        for (DataProfile dp : mAllDataProfiles) {
            pw.print(dp);
            pw.println(", last setup time: " + DataUtils.elapsedTimeToString(
                    dp.getLastSetupTimestamp()));
        }
        pw.decreaseIndent();

        pw.println("Preferred data profile=" + mPreferredDataProfile);
        pw.println("Preferred data profile from db=" + getPreferredDataProfileFromDb());
        pw.println("Preferred data profile from config=" + getPreferredDataProfileFromConfig());
        pw.println("Preferred data profile set id=" + mPreferredDataProfileSetId);
        pw.println("Last internet data profile=" + mLastInternetDataProfile);
        pw.println("Initial attach data profile=" + mInitialAttachDataProfile);
        pw.println("isTetheringDataProfileExisting=" + isTetheringDataProfileExisting(
                TelephonyManager.NETWORK_TYPE_LTE));
        pw.println("Permanent failed profiles=");
        pw.increaseIndent();
        mAllDataProfiles.stream()
                .filter(dp -> dp.getApnSetting() != null && dp.getApnSetting().getPermanentFailed())
                .forEach(pw::println);
        pw.decreaseIndent();

        pw.println("Local logs:");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
    }
}
