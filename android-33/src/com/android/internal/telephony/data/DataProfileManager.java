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
import com.android.internal.telephony.data.DataNetworkController.DataNetworkControllerCallback;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * DataProfileManager manages the all {@link DataProfile}s for the current
 * subscription.
 */
public class DataProfileManager extends Handler {
    private static final boolean VDBG = true;

    /** Event for data config updated. */
    private static final int EVENT_DATA_CONFIG_UPDATED = 1;

    /** Event for APN database changed. */
    private static final int EVENT_APN_DATABASE_CHANGED = 2;

    /** Event for SIM refresh. */
    private static final int EVENT_SIM_REFRESH = 3;

    private final Phone mPhone;
    private final String mLogTag;
    private final LocalLog mLocalLog = new LocalLog(128);

    /** Data network controller. */
    private final @NonNull DataNetworkController mDataNetworkController;

    /** Data config manager. */
    private final @NonNull DataConfigManager mDataConfigManager;

    /** Cellular data service. */
    private final @NonNull DataServiceManager mWwanDataServiceManager;

    /** All data profiles for the current carrier. */
    private final @NonNull List<DataProfile> mAllDataProfiles = new ArrayList<>();

    /** The data profile used for initial attach. */
    private @Nullable DataProfile mInitialAttachDataProfile = null;

    /** The preferred data profile used for internet. */
    private @Nullable DataProfile mPreferredDataProfile = null;

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
                            @NonNull List<DataProfile> dataProfiles) {
                        DataProfileManager.this.onInternetDataNetworkConnected(dataProfiles);
                    }});
        mDataConfigManager.registerForConfigUpdate(this, EVENT_DATA_CONFIG_UPDATED);
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
            case EVENT_DATA_CONFIG_UPDATED:
                onDataConfigUpdated();
                break;
            case EVENT_SIM_REFRESH:
                log("Update data profiles due to SIM refresh.");
                updateDataProfiles();
                break;
            case EVENT_APN_DATABASE_CHANGED:
                log("Update data profiles due to APN db updated.");
                updateDataProfiles();
                break;
            default:
                loge("Unexpected event " + msg);
                break;
        }
    }

    /**
     * Called when data config was updated.
     */
    private void onDataConfigUpdated() {
        log("Update data profiles due to config updated.");
        updateDataProfiles();

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
     */
    private void updateDataProfiles() {
        List<DataProfile> profiles = new ArrayList<>();
        if (mDataConfigManager.isConfigCarrierSpecific()) {
            Cursor cursor = mPhone.getContext().getContentResolver().query(
                    Uri.withAppendedPath(Telephony.Carriers.SIM_APN_URI, "filtered/subId/"
                            + mPhone.getSubId()), null, null, null, Telephony.Carriers._ID);
            if (cursor == null) {
                loge("Cannot access APN database through telephony provider.");
                return;
            }

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
                }
            }
            cursor.close();
        }

        // Check if any of the profile already supports ENTERPRISE, if not, check if DPC has
        // configured one and retrieve the same.
        DataProfile dataProfile = profiles.stream()
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
        updateInitialAttachDataProfileAtModem();

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
     * @param dataProfiles The connected internet data networks' profiles.
     */
    private void onInternetDataNetworkConnected(@NonNull List<DataProfile> dataProfiles) {
        // If there is already a preferred data profile set, then we don't need to do anything.
        if (mPreferredDataProfile != null) return;

        // If there is no preferred data profile, then we should use one of the data profiles,
        // which is good for internet, as the preferred data profile.

        // Most of the cases there should be only one, but in case there are multiple, choose the
        // one which has longest life cycle.
        DataProfile dataProfile = dataProfiles.stream()
                .max(Comparator.comparingLong(DataProfile::getLastSetupTimestamp).reversed())
                .orElse(null);
        // Save the preferred data profile into database.
        setPreferredDataProfile(dataProfile);
        updateDataProfiles();
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
        log("setPreferredDataProfile: " + dataProfile);

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
     */
    private void updateInitialAttachDataProfileAtModem() {
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

        if (!Objects.equals(mInitialAttachDataProfile, initialAttachDataProfile)) {
            mInitialAttachDataProfile = initialAttachDataProfile;
            logl("Initial attach data profile updated as " + mInitialAttachDataProfile);
            // TODO: Push the null data profile to modem on new AIDL HAL. Modem should clear the IA
            //  APN.
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
     * @return The data profile. {@code null} if can't find any satisfiable data profile.
     */
    public @Nullable DataProfile getDataProfileForNetworkRequest(
            @NonNull TelephonyNetworkRequest networkRequest, @NetworkType int networkType) {
        ApnSetting apnSetting = null;
        if (networkRequest.hasAttribute(TelephonyNetworkRequest
                .CAPABILITY_ATTRIBUTE_APN_SETTING)) {
            apnSetting = getApnSettingForNetworkRequest(networkRequest, networkType);
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
     * @return The APN setting. {@code null} if can't find any satisfiable data profile.
     */
    private @Nullable ApnSetting getApnSettingForNetworkRequest(
            @NonNull TelephonyNetworkRequest networkRequest, @NetworkType int networkType) {
        if (!networkRequest.hasAttribute(
                TelephonyNetworkRequest.CAPABILITY_ATTRIBUTE_APN_SETTING)) {
            loge("Network request does not have APN setting attribute.");
            return null;
        }

        // Filter out the data profile that can't satisfy the request.
        // Preferred data profile should be returned in the top of the list.
        List<DataProfile> dataProfiles = mAllDataProfiles.stream()
                .filter(networkRequest::canBeSatisfiedBy)
                // Put the preferred data profile at the top of the list, then the longest time
                // hasn't used data profile will be in the front so all the data profiles can be
                // tried.
                .sorted(Comparator.comparing((DataProfile dp) -> !dp.equals(mPreferredDataProfile))
                        .thenComparingLong(DataProfile::getLastSetupTimestamp))
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

        return dataProfiles.get(0).getApnSetting();
    }

    /**
     * Check if the data profile is the preferred data profile.
     *
     * @param dataProfile The data profile to check.
     * @return {@code true} if the data profile is the preferred data profile.
     */
    public boolean isDataProfilePreferred(@NonNull DataProfile dataProfile) {
        return dataProfile.equals(mPreferredDataProfile);
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
        return getDataProfileForNetworkRequest(networkRequest, networkType) != null;
    }

     /**
     * Check if any preferred data profile exists.
     *
     * @return {@code true} if any preferred data profile exists
     */
    public boolean isAnyPreferredDataProfileExisting() {
        for (DataProfile dataProfile : mAllDataProfiles) {
            if (dataProfile.isPreferred()) return true;
        }
        return false;
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
        apnBuilder.setMvnoType(apn1.getMvnoType());
        apnBuilder.setMvnoMatchData(apn1.getMvnoMatchData());
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
     * Get data profile by APN name and/or traffic descriptor.
     *
     * @param apnName APN name.
     * @param trafficDescriptor Traffic descriptor.
     *
     * @return Data profile by APN name and/or traffic descriptor. Either one of APN name or
     * traffic descriptor should be provided. {@code null} if data profile is not found.
     */
    public @Nullable DataProfile getDataProfile(@Nullable String apnName,
            @Nullable TrafficDescriptor trafficDescriptor) {
        if (apnName == null && trafficDescriptor == null) return null;

        List<DataProfile> dataProfiles = mAllDataProfiles;

        // Check if any existing data profile has the same traffic descriptor.
        if (trafficDescriptor != null) {
            dataProfiles = mAllDataProfiles.stream()
                    .filter(dp -> trafficDescriptor.equals(dp.getTrafficDescriptor()))
                    .collect(Collectors.toList());
        }

        // Check if any existing data profile has the same APN name.
        if (apnName != null) {
            dataProfiles = dataProfiles.stream()
                    .filter(dp -> dp.getApnSetting() != null
                            && (dp.getApnSetting().getApnSetId()
                            == Telephony.Carriers.MATCH_ALL_APN_SET_ID
                            || dp.getApnSetting().getApnSetId() == mPreferredDataProfileSetId))
                    .filter(dp -> apnName.equals(dp.getApnSetting().getApnName()))
                    .collect(Collectors.toList());
        }

        return dataProfiles.isEmpty() ? null : dataProfiles.get(0);
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
        pw.println("Initial attach data profile=" + mInitialAttachDataProfile);
        pw.println("isTetheringDataProfileExisting=" + isTetheringDataProfileExisting(
                TelephonyManager.NETWORK_TYPE_LTE));

        pw.println("Local logs:");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
    }
}
